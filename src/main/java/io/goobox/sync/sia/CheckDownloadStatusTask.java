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
import io.goobox.sync.sia.client.api.model.InlineResponse20010;
import io.goobox.sync.sia.client.api.model.InlineResponse20010Downloads;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.model.SiaFileFromDownloadsAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * CheckDownloadStatusTask requests current downloading status to siad and prints it.
 *
 * @author junpei
 */
class CheckDownloadStatusTask implements Runnable {

    @NotNull
    private final Context ctx;
    private static final Logger logger = LogManager.getLogger();

    CheckDownloadStatusTask(@NotNull final Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CheckDownloadStatusTask that = (CheckDownloadStatusTask) o;

        return ctx.equals(that.ctx);
    }

    @Override
    public int hashCode() {
        return ctx.hashCode();
    }

    @Override
    public void run() {

        logger.info("Checking download status");
        final RenterApi api = new RenterApi(this.ctx.apiClient);
        try {

            final InlineResponse20010 downloads = api.renterDownloadsGet();
            int nFiles = 0;

            for (InlineResponse20010Downloads rawFile : getRecentDownloads(downloads.getDownloads())) {

                final SiaFileFromDownloadsAPI file = new SiaFileFromDownloadsAPI(rawFile, this.ctx.pathPrefix);
                if (!file.getRemotePath().startsWith(this.ctx.pathPrefix) || !DB.contains(file)) {
                    logger.debug("Found remote file {} but it's not managed by Goobox", file.getRemotePath());
                    continue;
                }

                final SyncFile syncFile = DB.get(file);
                if (syncFile.getState() != SyncState.DOWNLOADING) {
                    logger.debug("Found remote file {} but it's not being downloaded", file.getRemotePath());
                    continue;
                }

                final String err = file.getError();
                if (err != null && !err.isEmpty()) {

                    logger.error("Failed to download {}: {}", file.getName(), err);
                    DB.setDownloadFailed(file);

                } else if (file.getFileSize() == file.getReceived()) {

                    // This file has been downloaded.
                    logger.info("File {} has been downloaded", file.getRemotePath());
                    if (file.getCreationTime() != 0) {
                        final boolean success = file.getLocalPath().toFile().setLastModified(file.getCreationTime());
                        if (!success) {
                            logger.debug("Failed to update the time stamp of {}", file.getLocalPath());
                        }
                    }

                    try {
                        DB.setSynced(file);
                    } catch (IOException e) {
                        logger.error("Failed to set status: {}", e.getMessage());
                        DB.setDownloadFailed(file);
                    }

                } else {

                    logger.debug("Still downloading {} ({} / {})", file.getName(), file.getReceived(), file.getFileSize());
                    ++nFiles;

                }

            }
            logger.info("Downloading {} files", nFiles);

        } catch (ApiException e) {

            logger.error("Failed to retrieve downloading files: {}", APIUtils.getErrorMessage(e));

        } finally {

            DB.commit();

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

                    final DateTime prev = parseDateTime(map.get(file.getSiapath()).getStarttime());
                    final DateTime curr = parseDateTime(file.getStarttime());
                    if (prev.isBefore(curr)) {
                        map.put(file.getSiapath(), file);
                    }

                } catch (IllegalArgumentException e) {
                    logger.error("Failed to parse the start date of {}: {}", file.getSiapath(), e.getMessage());
                    map.put(file.getSiapath(), file);
                }

            } else {
                map.put(file.getSiapath(), file);
            }

        }
        return map.values();

    }

    /**
     * Parse the given string to a date object.
     *
     * @param input string representing a date time in RFC3339 format.
     * @return a date object.
     */
    private static DateTime parseDateTime(final String input) throws IllegalArgumentException {
        return ISODateTimeFormat.dateTimeParser().parseDateTime(input);
    }

}
