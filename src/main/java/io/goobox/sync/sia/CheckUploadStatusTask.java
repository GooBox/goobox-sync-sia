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
import io.goobox.sync.sia.model.SiaFileFromFilesAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;

public class CheckUploadStatusTask implements Runnable {

    private final Context ctx;
    private static final Logger logger = LogManager.getLogger();
    private static final BigDecimal Completed = new BigDecimal(100);

    public CheckUploadStatusTask(final Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run() {

        this.logger.info("Checking upload status");
        final RenterApi api = new RenterApi(this.ctx.apiClient);
        try {

            final InlineResponse20011 res = api.renterFilesGet();
            if (res.getFiles() == null) {
                return;
            }

            int nFiles = 0;
            for (InlineResponse20011Files item : res.getFiles()) {

                final SiaFileFromFilesAPI file = new SiaFileFromFilesAPI(item, this.ctx.pathPrefix);
                if (!file.getRemotePath().startsWith(this.ctx.pathPrefix)) {
                    logger.debug("Found remote file {} but it's not managed by Goobox", file.getRemotePath());
                    continue;
                }

                logger.debug("Found remote file {}", file.getRemotePath());
                if (file.getUploadProgress().compareTo(Completed) >= 0) {
                    logger.debug("File {} has been uploaded", file.getLocalPath());
                    try {
                        DB.setSynced(file);
                    } catch (IOException e) {
                        logger.error("Failed to update the sync db: {}", e.getMessage());
                    }
                } else {
                    logger.info(
                            "File {} is now being uploaded ({}%)", file.getName(),
                            file.getUploadProgress().setScale(3, BigDecimal.ROUND_HALF_UP));
                    ++nFiles;
                }

            }

            if (nFiles != 0) {
                logger.info("Uploading {} files", nFiles);
            }

            DB.commit();

        } catch (ApiException e) {
            logger.error("Failed to retrieve uploaing status: {}", APIUtils.getErrorMessage(e));
        }

    }

}
