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
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.model.SiaFileFromFilesAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.util.Optional;
import java.util.concurrent.Callable;

class CheckUploadStateTask implements Callable<Void> {

    private static final Logger logger = LogManager.getLogger();
    private static final BigDecimal Completed = new BigDecimal(100);

    @NotNull
    private final Context ctx;

    CheckUploadStateTask(@NotNull final Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public Void call() throws ApiException {

        logger.info("Checking upload status");
        final RenterApi api = new RenterApi(this.ctx.apiClient);
        try {

            final InlineResponse20011 res = api.renterFilesGet();
            if (res.getFiles() == null) {
                return null;
            }

            res.getFiles().forEach(item -> {

                final SiaFileFromFilesAPI siaFile = new SiaFileFromFilesAPI(item, this.ctx.pathPrefix);
                final Optional<SyncFile> syncFileOpt = DB.get(siaFile);

                if (!siaFile.getCloudPath().startsWith(this.ctx.pathPrefix) || !syncFileOpt.isPresent()) {
                    logger.debug("Found remote file {} but it's not managed by Goobox", siaFile.getCloudPath());
                    return;
                }

                syncFileOpt.ifPresent(syncFile -> {

                    if (syncFile.getState() != SyncState.UPLOADING) {
                        logger.debug("Found remote file {} but it's not being uploaded", siaFile.getCloudPath());
                        return;
                    }

                    if (siaFile.getUploadProgress().compareTo(Completed) >= 0) {
                        logger.info("File {} has been uploaded", siaFile.getLocalPath());
                        try {
                            DB.setSynced(siaFile, siaFile.getLocalPath());
                        } catch (final IOException e) {
                            logger.error("Failed to update the sync db: {}", e.getMessage());
                            DB.setUploadFailed(this.ctx.getName(siaFile.getLocalPath()));
                        }
                    } else {
                        logger.info(
                                "File {} is now being uploaded ({}%)", siaFile.getName(),
                                siaFile.getUploadProgress().setScale(3, BigDecimal.ROUND_HALF_UP));
                    }

                });

            });

        } catch (final ApiException e) {
            if (e.getCause() instanceof ConnectException) {
                throw e;
            }
            logger.error("Failed to retrieve uploading status: {}", APIUtils.getErrorMessage(e));
        } finally {
            DB.commit();
        }
        return null;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CheckUploadStateTask that = (CheckUploadStateTask) o;

        return ctx.equals(that.ctx);
    }

    @Override
    public int hashCode() {
        return ctx.hashCode();
    }

}
