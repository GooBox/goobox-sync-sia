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

import java.util.Optional;

/**
 * Downloads a cloud name to the local directory.
 */
class DownloadCloudFileTask implements Runnable {

    private static final Logger logger = LogManager.getLogger();

    @NotNull
    private final Context ctx;

    @NotNull
    private final String name;

    DownloadCloudFileTask(@NotNull final Context ctx, @NotNull final String name) {
        this.ctx = ctx;
        this.name = name;
    }

    @Override
    public void run() {

        final Optional<SyncFile> syncFileOpt = DB.get(this.name);
        if (!syncFileOpt.isPresent()) {
            logger.warn("File {} was specified to be downloaded but doesn't exist in the sync DB", this.name);
            return;
        }

        syncFileOpt.ifPresent(syncFile -> {

            if (syncFile.getState() != SyncState.FOR_DOWNLOAD) {
                logger.debug("File {} was enqueued to be downloaded but its status was changed, skipped", this.name);
                return;
            }
            syncFile.getCloudPath().ifPresent(cloudPath -> syncFile.getTemporaryPath().ifPresent(temporaryPath -> {

                final RenterApi api = new RenterApi(this.ctx.apiClient);
                try {

                    logger.info("Downloading {} to {}", cloudPath, syncFile.getLocalPath().orElse(temporaryPath));
                    api.renterDownloadasyncSiapathGet(cloudPath.toString(), temporaryPath.toString());
                    DB.setDownloading(this.name);

                } catch (final ApiException e) {
                    logger.error(
                            "Cannot start downloading name {} to {}: {}",
                            cloudPath, syncFile.getLocalPath().orElse(temporaryPath), APIUtils.getErrorMessage(e));
                    DB.setDownloadFailed(this.name);
                } finally {
                    DB.commit();
                }

            }));

        });

    }

}