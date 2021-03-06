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
import io.goobox.sync.sia.client.api.model.InlineResponse20012;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.model.SiaFileFromFilesAPI;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

public class CheckUploadStateTask implements Callable<Void> {

    private static final Logger logger = LoggerFactory.getLogger(CheckUploadStateTask.class);
    private static final BigDecimal Completed = new BigDecimal(100);

    @NotNull
    private final Context ctx;

    public CheckUploadStateTask(@NotNull final Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public Void call() throws ApiException {

        logger.info("Checking upload status");
        final RenterApi api = new RenterApi(this.ctx.getApiClient());
        try {

            final InlineResponse20012 res = api.renterFilesGet();
            if (res.getFiles() == null) {
                return null;
            }

            res.getFiles()
                    .stream()
                    .map(file -> new SiaFileFromFilesAPI(this.ctx, file))
                    .filter(siaFile -> siaFile.getCloudPath().startsWith(this.ctx.getPathPrefix()))
                    .forEach(siaFile -> {

                        final Optional<SyncFile> syncFileOpt = DB.get(siaFile);
                        if (syncFileOpt.isPresent()) {

                            final SyncFile syncFile = syncFileOpt.get();
                            if (syncFile.getState() == SyncState.SYNCED) {
                                logger.trace("Skip processing {} because it has been already synced", syncFile.getName());
                                return;
                            } else if (syncFile.getState() != SyncState.UPLOADING) {
                                logger.debug("Cancel uploading {} because it's not marked as UPLOADING: {}", syncFile.getName(), syncFile.getState());
                                try {
                                    api.renterDeleteSiapathPost(APIUtils.toSlash(siaFile.getCloudPath()));
                                } catch (final ApiException e) {
                                    logger.error("Failed to delete {}: {}", syncFile.getName(), APIUtils.getErrorMessage(e));
                                }
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
                                App.getInstance().ifPresent(app -> app.refreshOverlayIcon(siaFile.getLocalPath()));
                            } else {
                                final BigDecimal progress = siaFile.getUploadProgress().setScale(3, RoundingMode.HALF_UP);
                                logger.info("File {} is now being uploaded ({}%)", siaFile.getName(), progress);
                            }

                        } else {

                            logger.debug("Cancel uploading {} because it's deleted from the sync DB", siaFile.getName());
                            try {
                                api.renterDeleteSiapathPost(APIUtils.toSlash(siaFile.getCloudPath()));
                            } catch (final ApiException e) {
                                logger.error("Failed to delete {}: {}", siaFile.getName(), APIUtils.getErrorMessage(e));
                            }

                        }

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
        return Objects.equals(ctx, that.ctx);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctx);
    }

}
