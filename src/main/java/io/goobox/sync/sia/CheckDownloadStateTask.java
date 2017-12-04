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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Requests current downloading status to siad and prints it.
 *
 * @author junpei
 */
class CheckDownloadStateTask implements Runnable {

    private static final Logger logger = LogManager.getLogger();

    @NotNull
    private final Context ctx;

    CheckDownloadStateTask(@NotNull final Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run() {

        logger.info("Checking download status");
        final RenterApi api = new RenterApi(this.ctx.apiClient);
        try {

            int nFiles = 0;
            for (final InlineResponse20010Downloads remoteFile : getRecentDownloads(api.renterDownloadsGet().getDownloads())) {

                final SiaFileFromDownloadsAPI file = new SiaFileFromDownloadsAPI(remoteFile, this.ctx.pathPrefix);
                final Optional<SyncFile> syncFileOpt = DB.get(file);

                if (!file.getCloudPath().startsWith(this.ctx.pathPrefix) || !syncFileOpt.isPresent()) {
                    logger.trace("Found remote file {} but it's not managed by Goobox", file.getCloudPath());
                    continue;
                }

                final SyncFile syncFile = syncFileOpt.get();
                if (syncFile.getState() == SyncState.MODIFIED || syncFile.getState() == SyncState.DELETED) {

                    logger.debug("Found cloud file {} is also modified/deleted in the local directory", file.getName());
                    final String err = file.getError();
                    if (err != null && !err.isEmpty()) {
                        logger.error("Failed to download {}: {}", file.getName(), err);
                    }
                    if (file.getFileSize() == file.getReceived()) {
                        syncFile.getLocalPath().ifPresent(localPath -> syncFile.getTemporaryPath().ifPresent(temporaryPath -> {

                            try {

                                final String conflictedFileName = String.format(
                                        "%s (%s's conflicted copy %s)",
                                        localPath.getFileName(),
                                        System.getProperty("user.name"),
                                        ISODateTimeFormat.date().print(System.currentTimeMillis()));

                                final Path parent = localPath.getParent();
                                if (!parent.toFile().exists()) {
                                    Files.createDirectories(parent);
                                }
                                logger.info("Save conflicted copy to {}", parent.resolve(conflictedFileName));
                                Files.move(temporaryPath, parent.resolve(conflictedFileName));

                            } catch (IOException e) {
                                logger.warn("Failed to delete a temporary file {}: {}", temporaryPath, e.getMessage());
                            }

                        }));
                    }
                    continue;

                } else if (syncFile.getState() != SyncState.DOWNLOADING) {
                    logger.debug("Found remote file {} but it's not being downloaded", file.getCloudPath());
                    continue;
                }

                final String err = file.getError();
                if (err != null && !err.isEmpty()) {

                    logger.error("Failed to download {}: {}", file.getName(), err);
                    DB.setDownloadFailed(file.getName());

                } else if (file.getFileSize() == file.getReceived()) {

                    syncFile.getLocalPath().ifPresent(localPath -> syncFile.getTemporaryPath().ifPresent(temporaryPath -> {

                        // This file has been downloaded.
                        logger.info("File {} has been downloaded", file.getName());

                        try {

                            // Move the file from the temporary directory to the desired place.
                            final Path parentDir = localPath.getParent();
                            if (!parentDir.toFile().exists()) {
                                Files.createDirectories(parentDir);
                            }
                            Files.move(temporaryPath, localPath);

                            if (file.getCreationTime() != 0) {
                                final boolean success = file.getLocalPath().toFile().setLastModified(file.getCreationTime());
                                if (!success) {
                                    logger.debug("Failed to update the time stamp of {}", localPath);
                                }
                            }

                            DB.setSynced(file, file.getLocalPath());

                        } catch (IOException e) {
                            logger.error("Failed post process of {}: {}", localPath, e.getMessage());
                            DB.setDownloadFailed(file.getName());
                        }

                    }));

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CheckDownloadStateTask that = (CheckDownloadStateTask) o;

        return ctx.equals(that.ctx);
    }

    @Override
    public int hashCode() {
        return ctx.hashCode();
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