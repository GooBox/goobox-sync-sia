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
import io.goobox.sync.sia.client.api.model.InlineResponse20011;
import io.goobox.sync.sia.client.api.model.InlineResponse20011Files;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.sia.model.SiaFileFromFilesAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Deletes a given file from the cloud network and sync DB.
 */
class DeleteCloudFileTask implements Runnable {

    private final Context ctx;
    private final SiaFile target;

    private static final Logger logger = LogManager.getLogger();

    DeleteCloudFileTask(final Context ctx, final SiaFile file) {
        this.ctx = ctx;
        this.target = file;
    }

    @Override
    public void run() {

        final RenterApi api = new RenterApi(this.ctx.apiClient);
        try {

            final InlineResponse20011 files = api.renterFilesGet();
            if (files.getFiles() == null) {
                logger.warn("No files exist in the cloud storage");
                return;
            }

            for (InlineResponse20011Files file : files.getFiles()) {

                final SiaFile siaFile = new SiaFileFromFilesAPI(file, this.ctx.pathPrefix);
                if (!siaFile.getRemotePath().startsWith(this.ctx.pathPrefix)) {
                    continue;
                }

                if (siaFile.getName().equals(this.target.getName())) {
                    logger.debug("Delete file {}", siaFile.getName());
                    api.renterDeleteSiapathPost(siaFile.getRemotePath().toString());
                }

            }
            DB.remove(this.target);
            DB.commit();

        } catch (ApiException e) {
            logger.error("Failed to delete remote file {}: {}", this.target.getRemotePath(), APIUtils.getErrorMessage(e));
        }

    }

}
