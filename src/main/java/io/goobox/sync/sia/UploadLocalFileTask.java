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
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.storj.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * Uploads a given local file to cloud storage with a given remote path.
 */
class UploadLocalFileTask implements Runnable {

    private final Context ctx;
    private final Path localPath;

    private static final Logger logger = LogManager.getLogger();

    UploadLocalFileTask(final Context ctx, final Path localPath) {
        this.ctx = ctx;
        this.localPath = localPath;
    }

    @Override
    public void run() {

        if (!DB.contains(localPath)) {
            logger.warn("File {} was deleted from SyncDB", this.localPath);
            return;
        }

        if (DB.get(localPath).getState() != SyncState.FOR_UPLOAD) {
            logger.debug("File {} was enqueued to be uploaded but its status was changed, skipped", this.localPath);
            return;
        }

        final RenterApi api = new RenterApi(this.ctx.apiClient);
        try {
            final Path remotePath = this.ctx.pathPrefix.resolve(Utils.getSyncDir().relativize(localPath));
            final long creationTime = this.localPath.toFile().lastModified();
            final Path siaPath = remotePath.resolve(String.valueOf(creationTime));
            api.renterUploadSiapathPost(
                    siaPath.toString(),
                    this.ctx.config.getDataPieces(), this.ctx.config.getParityPieces(),
                    this.localPath.toString());
            DB.setUploading(this.localPath);
        } catch (ApiException e) {
            logger.error("Failed to upload {}: {}", this.localPath, APIUtils.getErrorMessage(e));
            DB.setUploadFailed(this.localPath);
        } finally {
            DB.commit();
        }

    }

}
