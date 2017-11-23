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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20010;
import io.goobox.sync.sia.client.api.model.InlineResponse20010Downloads;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.model.SiaFileFromDownloadsAPI;
import io.goobox.sync.storj.db.SyncState;

/**
 * CheckDownloadStatusTask requests current downloading status to siad and prints it.
 *
 * @author junpei
 */
public class CheckDownloadStatusTask implements Runnable {

    private final Context ctx;
    private static final Logger logger = LogManager.getLogger();

    private static final SimpleDateFormat RFC3339 = new SimpleDateFormat("yyyy-MM-dd'T'h:m:ssZ");


    public CheckDownloadStatusTask(final Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run() {

        this.logger.info("Checking download status");
        final RenterApi api = new RenterApi(this.ctx.apiClient);
        try {

            final InlineResponse20010 downloads = api.renterDownloadsGet();
            int nFiles = 0;

            for (InlineResponse20010Downloads rawFile : getRecentDownloads(downloads.getDownloads())) {

                final SiaFileFromDownloadsAPI file = new SiaFileFromDownloadsAPI(rawFile, this.ctx.pathPrefix);
                if (!file.getRemotePath().startsWith(this.ctx.pathPrefix)) {
                    // This file isn't managed by Goobox.
                    this.logger.debug("Found remote file {} but it's not managed by Goobox", file.getRemotePath());
                    continue;
                }

                final String err = file.getError();
                if (err != null && !err.isEmpty()) {

                    // TODO: Error handling.
                    this.logger.error("Failed to download {}: {}", file.getName(), err);
                    if (DB.contains(file)) {
                        final SyncFile syncFile = DB.get(file);
                        if (syncFile.getState() == SyncState.FOR_DOWNLOAD) {
                            DB.setDownloadFailed(file);
                        }
                    }

                } else if (file.getFileSize() == file.getReceived()) {

                    // This file has been downloaded.
                    this.logger.debug("File {} has been downloaded", file.getRemotePath());
                    if(file.getCreationTime() != 0){
                        file.getLocalPath().toFile().setLastModified(file.getCreationTime());
                    }
                    if (DB.contains(file)) {
                        final SyncFile syncFile = DB.get(file);
                        if (syncFile.getState() == SyncState.FOR_DOWNLOAD) {
                            try {
                                DB.setSynced(file);
                            } catch (IOException e) {
                                logger.error("Failed to set status: {}", e.getMessage());
                            }
                        }
                    }

                } else {

                    this.logger.debug("Still downloading {} ({} / {})", file.getName(), file.getReceived(),
                            file.getFileSize());
                    ++nFiles;

                }

            }
            this.logger.info("Downloading {} files", nFiles);

        } catch (ApiException e) {

            this.logger.error("Failed to retrieve downloading files: {}", APIUtils.getErrorMessage(e));

        }

    }

    /**
     * Checks the collection has duplicated entries, and returns a list consisting of newer entries. .
     */
    private static Collection<InlineResponse20010Downloads> getRecentDownloads(final Collection<InlineResponse20010Downloads> list) {

        if (list == null) {
            return new ArrayList<>();
        }

        final Map<String, InlineResponse20010Downloads> map = new HashMap<>();
        for (InlineResponse20010Downloads file : list) {

            if (map.containsKey(file.getSiapath())) {

                try {

                    final Date prev = RFC3339.parse(map.get(file.getSiapath()).getStarttime());
                    final Date curr = RFC3339.parse(file.getStarttime());
                    if (prev.before(curr)) {
                        map.put(file.getSiapath(), file);
                    }

                } catch (ParseException e) {
                    logger.error("Failed to parse the start date of {}: {}", file.getSiapath(), e.getMessage());
                    map.put(file.getSiapath(), file);
                }

            } else {
                map.put(file.getSiapath(), file);
            }

        }
        return map.values();

    }

}
