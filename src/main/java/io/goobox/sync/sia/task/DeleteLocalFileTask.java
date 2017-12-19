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
package io.goobox.sync.sia.task;

import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;


/**
 * DeleteLocalFileTask deletes a given local file.
 *
 * @author junpei
 */
public class DeleteLocalFileTask implements Runnable {

    private static final Logger logger = LogManager.getLogger();

    @NotNull
    private final Context ctx;
    @NotNull
    private final Path localPath;

    public DeleteLocalFileTask(@NotNull final Context ctx, @NotNull final Path localPath) {
        this.ctx = ctx;
        this.localPath = localPath;
    }

    @Override
    public void run() {

        final Optional<SyncFile> syncFileOpt = DB.get(this.ctx.getName(this.localPath));
        if (!syncFileOpt.isPresent()) {
            logger.warn("File {} was deleted from SyncDB", this.localPath);
            return;
        }
        syncFileOpt.ifPresent(syncFile -> {

            if (syncFile.getState() != SyncState.FOR_LOCAL_DELETE) {
                logger.debug("File {} was enqueued to be deleted but its status was changed, skipped", syncFile.getName());
                return;
            }

            logger.info("Deleting local file {}", this.localPath);
            try {

                if (!Files.deleteIfExists(this.localPath)) {
                    logger.warn("File {} doesn't exist", this.localPath);
                }
                DB.remove(this.ctx.getName(this.localPath));
                DB.commit();

            } catch (IOException e) {
                logger.error("Cannot delete local file {}: {}", this.localPath, e.getMessage());
            }

        });

    }

}
