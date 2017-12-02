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
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.sia.model.SiaFileFromFilesAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Deletes a given file from the cloud network and sync DB.
 */
class DeleteCloudFileTask implements Runnable {

    private static final Logger logger = LogManager.getLogger();

    @NotNull
    private final Context ctx;

    @NotNull
    private final String name;

    DeleteCloudFileTask(@NotNull final Context ctx, @NotNull final String name) {
        this.ctx = ctx;
        this.name = name;
    }

    @Override
    public void run() {

        if (!DB.contains(this.name)) {
            logger.warn("File {} was deleted from SyncDB", this.name);
            return;
        }

        final SyncFile syncFile = DB.get(this.name);
        if (syncFile.getState() != SyncState.FOR_CLOUD_DELETE) {
            logger.debug("File {} was enqueued to be deleted but its status was changed, skipped", syncFile.getName());
            return;
        }

        final RenterApi api = new RenterApi(this.ctx.apiClient);
        try {

            final InlineResponse20011 files = api.renterFilesGet();
            if (files.getFiles() == null) {

                logger.warn("No files exist in the cloud storage");

            } else {

                for (final InlineResponse20011Files file : files.getFiles()) {

                    final SiaFile siaFile = new SiaFileFromFilesAPI(file, this.ctx.pathPrefix);
                    if (!siaFile.getCloudPath().startsWith(this.ctx.pathPrefix)) {
                        continue;
                    }

                    if (siaFile.getName().equals(this.name)) {
                        logger.info("Delete file {}", siaFile.getCloudPath());
                        try {
                            api.renterDeleteSiapathPost(siaFile.getCloudPath().toString());
                        } catch (final ApiException e) {
                            logger.error(
                                    "Failed to delete remote file {}: {}",
                                    siaFile.getCloudPath(), APIUtils.getErrorMessage(e));
                        }
                    }

                }

            }
            DB.remove(this.name);
            DB.commit();

        } catch (final ApiException e) {
            logger.error("Failed to delete remote file {}: {}", this.name, APIUtils.getErrorMessage(e));
        }

    }

}
