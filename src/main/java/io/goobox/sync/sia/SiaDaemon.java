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

import net.harawata.appdirs.AppDirsFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

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

    private static final String SiaDaemonFolderName = "Sia";
    private static final String SiaDaemonName = "siad";
    private static final Path ConsensusDBPath = Paths.get("consensus", "consensus.db");
    private static final String ConsensusDBURL = "https://consensus.siahub.info/consensus.db";

    private Process process;

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
                String.format("--sia-directory=%s", this.getDataDir()),
                "--modules=cghrtw");
        cmd.redirectErrorStream(true);

        logger.info("Start SIA daemon");
        logger.debug("Execute: {}", String.join(" ", cmd.command()));
        try {
            this.process = cmd.start();
            try (final BufferedReader in = new BufferedReader(new InputStreamReader(this.process.getInputStream()))) {
                in.lines().forEach(logger::debug);
            }
        } catch (IOException e) {
            logger.error("Failed to start SIA daemon: {}", e.getMessage());
        }

    }

    @Override
    public void close() {

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

    @NotNull
    Path getDataDir() {
        final String SiaUI = AppDirsFactory.getInstance().getUserDataDir("Sia-UI", null, "", true);
        return Paths.get(SiaUI, "sia");
    }

    boolean checkAndDownloadConsensusDB() throws IOException {

        // TODO: check sum : https://consensus.siahub.info/sha256sum.txt
        final Path dbPath = this.getDataDir().resolve(ConsensusDBPath);
        if (dbPath.toFile().exists()) {
            return false;
        }

        if (!dbPath.getParent().toFile().exists()) {
            Files.createDirectories(dbPath.getParent());
        }

        final Path tempFile = Files.createTempFile(null, null);
        try {

            final URL url = new URL(ConsensusDBURL);
            final URLConnection conn = url.openConnection();
            try (final InputStream in = conn.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tempFile, dbPath);

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

