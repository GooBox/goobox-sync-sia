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
import io.goobox.sync.sia.model.SiaFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Downloads a cloud file to the local directory.
 */
class DownloadCloudFileTask implements Runnable {

    private final Context ctx;
    private final SiaFile file;
    private static final Logger logger = LogManager.getLogger();

    DownloadCloudFileTask(final Context ctx, final SiaFile file) {
        this.ctx = ctx;
        this.file = file;
    }

    @Override
    public void run() {

        if (!DB.contains(this.file)) {
            logger.warn("File {} was specified to be downloaded but removed from the sync DB", this.file.getName());
            return;
        }

        final SyncFile syncFile = DB.get(this.file);
        if (syncFile.getState() != SyncState.FOR_DOWNLOAD) {
            logger.debug("File {} was enqueued to be downloaded but its status was changed, skipped", this.file);
            return;
        }

        final RenterApi api = new RenterApi(this.ctx.apiClient);
        try {

            logger.info("Downloading {} to {}", this.file.getRemotePath(), this.file.getLocalPath());
            api.renterDownloadasyncSiapathGet(this.file.getRemotePath().toString(), syncFile.getTemporaryPath().toString());
            DB.setDownloading(file);

        } catch (ApiException e) {

            logger.error(
                    "Cannot start downloading file {} to {}: {}",
                    this.file.getRemotePath(), this.file.getLocalPath(), APIUtils.getErrorMessage(e));
            DB.setDownloadFailed(file);

        } finally {

            DB.commit();

        }

    }

}
