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
 * Downloads a cloud name to the local directory.
 */
public class DownloadCloudFileTask implements Callable<Void> {

    private static final Logger logger = LoggerFactory.getLogger(DownloadCloudFileTask.class);

    @NotNull
    private final Context ctx;

    @NotNull
    private final String name;

    public DownloadCloudFileTask(@NotNull final Context ctx, @NotNull final String name) {
        this.ctx = ctx;
        this.name = name;
    }

    @Override
    public Void call() throws ApiException {
        logger.trace("Enter call");

        final Optional<SyncFile> syncFileOpt = DB.get(this.name);
        if (!syncFileOpt.isPresent()) {
            logger.warn("File {} was specified to be downloaded but doesn't exist in the sync DB", this.name);
            return null;
        }

        final SyncFile syncFile = syncFileOpt.get();
        if (syncFile.getState() != SyncState.FOR_DOWNLOAD) {
            logger.debug("File {} was enqueued to be downloaded but its status was changed, skipped", this.name);
            return null;
        }

        if (!syncFile.getCloudPath().isPresent() || !syncFile.getTemporaryPath().isPresent()) {
            logger.debug("File {} was enqueued but one of cloud path and temporary path is not given", this.name);
            return null;
        }
        final Path cloudPath = syncFile.getCloudPath().get();
        final Path temporaryPath = syncFile.getTemporaryPath().get();
        final RenterApi api = new RenterApi(this.ctx.getApiClient());
        try {

            logger.info("Downloading {} to {}", cloudPath, syncFile.getLocalPath().orElse(temporaryPath));
            api.renterDownloadasyncSiapathGet(APIUtils.toSlash(cloudPath), APIUtils.toSlash(temporaryPath));
            DB.setDownloading(this.name);

        } catch (final ApiException e) {

            if (e.getCause() instanceof ConnectException) {
                throw e;
            }

            logger.error(
                    "Cannot start downloading name {} to {}: {}",
                    cloudPath, syncFile.getLocalPath().orElse(temporaryPath), APIUtils.getErrorMessage(e));
            DB.setDownloadFailed(this.name);

        } finally {
            App.getInstance().ifPresent(app -> syncFile.getLocalPath().ifPresent(localPath -> app.getOverlayHelper().refresh(localPath)));
            DB.commit();
        }
        return null;

    }

}
