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
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class SiaDaemon extends Thread implements Closeable {

    private static final Logger logger = LogManager.getLogger();

    private static final String SiaDaemonFolderName = "sia";
    private static final String SiaDaemonName = "siad";
    private static final Path ConsensusDBPath = Paths.get("consensus", "consensus.db");
    private static final String ConsensusDBURL = "https://consensus.siahub.info/consensus.db";

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

        logger.info("Starting SIA daemon");
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
            } catch (InterruptedException e) {
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
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            cmd = cmd + ".exe";
        }
        final Path wd = Paths.get(System.getProperty("user.dir"));
        if (wd.getFileName().toString().equals("bin")) {
            return wd.getParent().resolve(SiaDaemonFolderName).resolve(cmd);
        }
        return wd.resolve(SiaDaemonFolderName).resolve(cmd);

    }

    boolean checkAndDownloadConsensusDB() throws IOException {

        // TODO: check sum : https://consensus.siahub.info/sha256sum.txt
        final Path dbPath = this.dataDir.resolve(ConsensusDBPath);
        if (dbPath.toFile().exists() && dbPath.toFile().length() > ConsensusDBThreshold) {
            return false;
        }

        if (!dbPath.getParent().toFile().exists()) {
            Files.createDirectories(dbPath.getParent());
        }

        final Path tempFile = Files.createTempFile(null, null);
        try {

            final URL url = new URL(ConsensusDBURL);
            final URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            conn.connect();

            final Thread th = new Thread(() -> {
                try {
                    while (tempFile.toFile().exists()) {
                        logger.info("Downloading consensus database... ({} MB)", tempFile.toFile().length() / 1000000);
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException e) {
                    logger.error("Interrupted while downloading consensus database");
                }
            });
            th.start();

            try (final InputStream in = conn.getInputStream()) {
                Files.copy(new BufferedInputStream(in), tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tempFile, dbPath, StandardCopyOption.REPLACE_EXISTING);
            th.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (tempFile.toFile().exists()) {
                if (tempFile.toFile().delete()) {
                    logger.warn("Failed to delete a temporary file {}", tempFile);
                }
            }
        }

        return true;

    }

}

