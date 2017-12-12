/*
 * Copyright (C) 2017 Junpei Kawamoto
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.goobox.sync.sia;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SiaDaemon extends Thread implements Closeable {

    private static final Logger logger = LogManager.getLogger();

    private static final String SiaDaemonFolderName = "sia";
    private static final String SiaDaemonName = "siad";
    private static final Path ConsensusDBPath = Paths.get("consensus", "consensus.db");
    private static final String ConsensusDBURL = "https://consensus.siahub.info/consensus.db";
    static final String CheckSumURL = "https://consensus.siahub.info/sha256sum.txt";
    static final String DefaultUserAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";

    static final int MaxRetry = 5;

    /**
     * Threshold file size of the consensus.db. (2GB)
     * If current file size is smaller than this, the database snapshot will be downloaded.
     */
    static final long ConsensusDBThreshold = 2L * 1024L * 1024L * 1024L;

    @NotNull
    private final Path dataDir;
    @Nullable
    private Process process;

    public SiaDaemon(@NotNull final Path dataDir) {
        this.dataDir = dataDir.toAbsolutePath();
    }

    /**
     * Start SIA daemon. This method blocks until the child process ends.
     */
    @Override
    public void run() {

        final ProcessBuilder cmd = new ProcessBuilder(
                this.getDaemonPath().toString(),
                "--api-addr=127.0.0.1:9980",
                "--host-addr=:9982",
                "--rpc-addr=:9981",
                String.format("--sia-directory=%s", this.dataDir),
                "--modules=cgrtw");
        cmd.redirectErrorStream(true);

        logger.debug("Execute: {}", String.join(" ", cmd.command()));
        try {
            this.process = cmd.start();
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(this.process.getInputStream()))) {
                in.lines().forEach(logger::debug);
            }
        } catch (IOException e) {
            logger.error("Failed to start SIA daemon: {}", e.getMessage());
        }
        synchronized (this) {
            this.process = null;
        }

    }

    @Override
    public synchronized void close() {

        if (this.process != null) {
            logger.info("Closing SIA daemon");
            this.process.destroy();
            try {
                this.process.waitFor();
            } catch (final InterruptedException e) {
                logger.warn("Interrupted while waiting SIA daemon ends: {}", e.getMessage());
            }
        }

    }

    @SuppressWarnings("WeakerAccess")
    public boolean isClosed() {
        return this.process == null;
    }

    @NotNull
    Path getDaemonPath() {

        String cmd = SiaDaemonName;
        if (SystemUtils.IS_OS_WINDOWS) {
            cmd = cmd + ".exe";
        }

        final Path wd = SystemUtils.getUserDir().toPath();
        if (wd.getFileName().toString().equals("bin")) {
            return wd.getParent().resolve(SiaDaemonFolderName).resolve(cmd);
        }
        return wd.resolve(SiaDaemonFolderName).resolve(cmd);

    }

    boolean checkAndDownloadConsensusDB() throws IOException {

        final Path dbPath = this.dataDir.resolve(ConsensusDBPath);
        if (dbPath.toFile().exists() && dbPath.toFile().length() > ConsensusDBThreshold) {
            return false;
        }

        if (!dbPath.getParent().toFile().exists()) {
            Files.createDirectories(dbPath.getParent());
        }

        int attempt = 0;
        while (attempt < MaxRetry) {
            attempt++;

            final Optional<String> checkSumOpt = this.getCheckSum();
            if (!checkSumOpt.isPresent()) {
                logger.warn("The bootstrap DB is not available, skipped");
                return false;
            }

            final Path tempFile = Files.createTempFile(null, null);
            final ExecutorService executor = Executors.newFixedThreadPool(2);
            try {


                final URL url = new URL(ConsensusDBURL);
                final URLConnection conn = url.openConnection();
                conn.setRequestProperty("User-Agent", DefaultUserAgent);
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.connect();

                executor.submit(() -> {
                    try {
                        while (tempFile.toFile().exists()) {
                            logger.info("Downloading consensus database... ({} MB)", tempFile.toFile().length() / 1000000);
                            Thread.sleep(5000);
                        }
                    } catch (final InterruptedException e) {
                        logger.error("Interrupted while downloading consensus database: {}", e.getMessage());
                    }
                });

                final PipedInputStream pipedIn = new PipedInputStream();
                final PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
                @SuppressWarnings("unchecked") final Future checkSumFuture = executor.submit((Callable) () -> DigestUtils.sha256Hex(pipedIn));

                try (final InputStream in = new TeeInputStream(conn.getInputStream(), pipedOut, true)) {
                    Files.copy(new BufferedInputStream(in), tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                if (!checkSumOpt.get().equals(checkSumFuture.get())) {
                    logger.warn("The check sum of the downloaded bootstrap DB doesn't match, retry");
                    continue;
                }

                Files.move(tempFile, dbPath, StandardCopyOption.REPLACE_EXISTING);
                return true;

            } catch (final InterruptedException e) {
                logger.error("Interrupted while downloading the bootstrap DB: {}", e.getMessage());
            } catch (final ExecutionException e) {
                logger.error("Failed to compute the check sum of the bootstrap DB: {}", e.getMessage());
            } finally {

                if (tempFile.toFile().exists()) {
                    if (tempFile.toFile().delete()) {
                        logger.warn("Failed to delete temporary file {}", tempFile);
                    }
                }
                executor.shutdown();
                try {
                    executor.awaitTermination(60, TimeUnit.SECONDS);
                } catch (final InterruptedException e) {
                    logger.error("Interrupted while preparing the bootstrap DB: {}", e.getMessage());
                }

            }

        }

        logger.warn("Cannot download the bootstrap DB");
        return false;

    }

    /**
     * Download the checksum of the consensus.db.
     *
     * @return an optional object of the checksum string.
     */
    Optional<String> getCheckSum() {

        try {

            final URL url = new URL(CheckSumURL);
            final URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", DefaultUserAgent);
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.connect();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            return reader.lines().
                    filter((line) -> line.contains(ConsensusDBPath.getFileName().toString())).
                    map((line) -> line.split(" ")).
                    filter((items) -> items.length > 0).
                    map((items) -> items[0]).
                    findFirst();

        } catch (final MalformedURLException e) {
            logger.error("The URL of the check sum file is invalid: {}", e.getMessage());
        } catch (final IOException e) {
            logger.error("Failed to receive the checksum of consensus.db: {}", e.getMessage());
        }

        return Optional.empty();

    }

}

