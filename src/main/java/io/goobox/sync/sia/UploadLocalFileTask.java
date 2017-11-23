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
import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.storj.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Date;

/**
 * Uploads a given local file to cloud storage with a given remote path.
 */
public class UploadLocalFileTask implements Runnable {

    private final Context ctx;
    private final Path localPath;
    private final Path remotePath;
    private final Date creationTime;

    private static final Logger logger = LogManager.getLogger();

    public UploadLocalFileTask(final Context ctx, final SiaFile file, final Date creationTime) {
        this.ctx = ctx;
        this.localPath = file.getLocalPath();
        this.remotePath = file.getRemotePath();
        this.creationTime = creationTime;
    }

    public UploadLocalFileTask(final Context ctx, final Path localPath, final Date creationTime) {
        this.ctx = ctx;
        this.localPath = localPath;
        this.remotePath = this.ctx.pathPrefix.resolve(Utils.getSyncDir().relativize(localPath));
        this.creationTime = creationTime;
    }

    @Override
    public void run() {

        final RenterApi api = new RenterApi(this.ctx.apiClient);
        try {
            final Path siaPath = this.remotePath.resolve(String.valueOf(this.creationTime.getTime()));
            api.renterUploadSiapathPost(
                    siaPath.toString(),
                    this.ctx.config.dataPieces, this.ctx.config.parityPieces,
                    this.localPath.toString());
        } catch (ApiException e) {
            logger.error("Failed to upload {}: {}", this.localPath, APIUtils.getErrorMessage(e));
            DB.setUploadFailed(this.localPath);
        }

    }

}
