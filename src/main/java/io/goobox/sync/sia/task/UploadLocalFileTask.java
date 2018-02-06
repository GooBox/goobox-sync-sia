/*
 * Copyright (C) 2017-2018 Junpei Kawamoto
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

import io.goobox.sync.sia.APIUtils;
import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Uploads a given local file to cloud storage with a given remote path.
 */
public class UploadLocalFileTask implements Callable<Void> {

    private static final Logger logger = LoggerFactory.getLogger(UploadLocalFileTask.class);

    static final int MaxRetry = 2;

    @NotNull
    private final Context ctx;

    @NotNull
    private final Path localPath;


    public UploadLocalFileTask(@NotNull final Context ctx, @NotNull final Path localPath) {
        this.ctx = ctx;
        this.localPath = localPath;
    }

    @Override
    public Void call() throws ApiException {
        logger.trace("Enter call");

        final Optional<SyncFile> syncFileOpt = DB.get(this.ctx.getName(this.localPath));
        if (!syncFileOpt.isPresent()) {
            logger.warn("File {} was deleted from SyncDB", this.localPath);
            return null;
        }

        final SyncFile syncFile = syncFileOpt.get();
        if (syncFile.getState() != SyncState.FOR_UPLOAD) {
            logger.debug("File {} was enqueued to be uploaded but its status was changed, skipped", syncFile.getName());
            return null;
        }

        if (!syncFile.getCloudPath().isPresent()) {
            logger.debug("File {} was enqueued but it doesn't have the cloud path", syncFile.getName());
            return null;
        }

        final RenterApi api = new RenterApi(this.ctx.getApiClient());
        final String slashedCloudPath = APIUtils.toSlash(syncFile.getCloudPath().get());
        final String slashedLocalPath = APIUtils.toSlash(this.localPath);
        for (int i = 0; i != MaxRetry; i++) {

            try {

                api.renterUploadSiapathPost(
                        slashedCloudPath,
                        this.ctx.getConfig().getDataPieces(),
                        this.ctx.getConfig().getParityPieces(),
                        slashedLocalPath);
                DB.setUploading(this.ctx.getName(this.localPath));
                break;

            } catch (final ApiException e) {

                if (e.getCause() instanceof ConnectException) {
                    throw e;
                }
                logger.error("Failed to upload {}: {}", this.localPath, APIUtils.getErrorMessage(e));
                DB.setUploadFailed(this.ctx.getName(this.localPath));

            }

            try {
                api.renterDeleteSiapathPost(slashedCloudPath);
            } catch (final ApiException e) {
                logger.error("Failed to delete {}: {}", slashedCloudPath, APIUtils.getErrorMessage(e));
            }

        }

        App.getInstance().ifPresent(app -> app.getOverlayHelper().refresh(this.localPath));
        DB.commit();
        return null;

    }

}
