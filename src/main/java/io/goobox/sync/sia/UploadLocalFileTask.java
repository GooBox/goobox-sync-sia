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

import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Uploads a given local file to cloud storage with a given remote path.
 */
class UploadLocalFileTask implements Runnable {

    private static final Logger logger = LogManager.getLogger();

    @NotNull
    private final Context ctx;

    @NotNull
    private final Path localPath;


    UploadLocalFileTask(@NotNull final Context ctx, @NotNull final Path localPath) {
        this.ctx = ctx;
        this.localPath = localPath;
    }

    @Override
    public void run() {

        final Optional<SyncFile> syncFileOpt = DB.get(this.localPath);
        if (!syncFileOpt.isPresent()) {
            logger.warn("File {} was deleted from SyncDB", this.localPath);
            return;
        }
        syncFileOpt.ifPresent(syncFile -> {

            if (syncFile.getState() != SyncState.FOR_UPLOAD) {
                logger.debug("File {} was enqueued to be uploaded but its status was changed, skipped", this.localPath);
                return;
            }
            syncFile.getCloudPath().ifPresent(cloudPath -> {

                final RenterApi api = new RenterApi(this.ctx.apiClient);
                try {

                    api.renterUploadSiapathPost(
                            cloudPath.toString(),
                            this.ctx.config.getDataPieces(), this.ctx.config.getParityPieces(),
                            this.localPath.toString());
                    DB.setUploading(this.localPath);

                } catch (ApiException e) {
                    logger.error("Failed to upload {}: {}", this.localPath, APIUtils.getErrorMessage(e));
                    DB.setUploadFailed(this.localPath);
                } finally {
                    DB.commit();
                }

            });

        });

    }

}
