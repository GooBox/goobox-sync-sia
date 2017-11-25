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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Downloads a remote file to the local directory.
 */
class DownloadRemoteFileTask implements Runnable {

    private final Context ctx;
    private final SiaFile file;
    private static final Logger logger = LogManager.getLogger();

    DownloadRemoteFileTask(final Context ctx, final SiaFile file) {
        this.ctx = ctx;
        this.file = file;
    }

    @Override
    public void run() {

        logger.info("Downloading {} to {}", this.file.getRemotePath(), this.file.getLocalPath());
        final Path parent = this.file.getLocalPath().getParent();

        try {

            if (!parent.toFile().exists()) {
                logger.trace("Creating directory {}", parent);
                Files.createDirectories(parent);
            }

            final RenterApi api = new RenterApi(this.ctx.apiClient);
            api.renterDownloadasyncSiapathGet(this.file.getRemotePath().toString(), this.file.getLocalPath().toString());

        } catch (IOException e) {
            logger.error("Cannot create directory {}: {}", parent, e.getMessage());
            DB.setDownloadFailed(file);
            DB.commit();
        } catch (ApiException e) {
            logger.error("Cannot start downloading file {} to {}: {}", this.file.getRemotePath(), this.file.getLocalPath(), APIUtils.getErrorMessage(e));
            DB.setDownloadFailed(file);
            DB.commit();
        }

    }

}
