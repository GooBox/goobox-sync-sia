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

import io.goobox.sync.common.Utils;
import io.goobox.sync.sia.APIUtils;
import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20010Downloads;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.model.SiaFileFromDownloadsAPI;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Requests current downloading status to siad and prints it.
 *
 * @author junpei
 */
public class CheckDownloadStateTask implements Callable<Void> {

    private static final Logger logger = LoggerFactory.getLogger(CheckDownloadStateTask.class);

    @NotNull
    private final Context ctx;

    public CheckDownloadStateTask(@NotNull final Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public Void call() throws ApiException {

        logger.info("Checking download status");
        final RenterApi api = new RenterApi(this.ctx.getApiClient());
        try {
            getRecentDownloads(api.renterDownloadsGet().getDownloads())
                    .stream()
                    .map(remoteFile -> new SiaFileFromDownloadsAPI(this.ctx, remoteFile))
                    .forEach(this::handleFile);
        } catch (final ApiException e) {
            if (e.getCause() instanceof ConnectException) {
                throw e;
            }
            logger.error("Failed to retrieve downloading files: {}", APIUtils.getErrorMessage(e));
        } finally {
            DB.commit();
        }
        return null;

    }

    /**
     * Checks the collection has duplicated entries, and returns a list consisting of newer entries. .
     */
    @NotNull
    private Collection<InlineResponse20010Downloads> getRecentDownloads(@Nullable final Collection<InlineResponse20010Downloads> list) {

        if (list == null) {
            return new ArrayList<>();
        }

        final Map<String, InlineResponse20010Downloads> map = new HashMap<>();
        list.forEach(file -> {

            if (map.containsKey(file.getSiapath())) {

                try {
                    final DateTime prev = parseDateTime(map.get(file.getSiapath()).getStarttime());
                    final DateTime curr = parseDateTime(file.getStarttime());
                    if (prev.isBefore(curr)) {
                        map.put(file.getSiapath(), file);
                    }
                } catch (final IllegalArgumentException e) {
                    logger.error("Failed to parse the start date of {}: {}", file.getSiapath(), e.getMessage());
                    map.put(file.getSiapath(), file);
                }

            } else {
                map.put(file.getSiapath(), file);
            }

        });
        return map.values();

    }

    private void handleFile(@NotNull final SiaFileFromDownloadsAPI file) {

        final Optional<SyncFile> syncFileOpt = DB.get(file);
        if (!file.getCloudPath().startsWith(this.ctx.getPathPrefix()) || !syncFileOpt.isPresent()) {
            logger.trace(
                    "Found remote file {} but it's not managed by Goobox (not starts with {})",
                    file.getCloudPath(),
                    this.ctx.getPathPrefix());
            return;
        }

        syncFileOpt.ifPresent(syncFile -> {

            final String err = file.getError();
            if (err != null && !err.isEmpty()) {
                logger.error("Failed to download {}: {}", file.getName(), err);
                if (syncFile.getState() == SyncState.DOWNLOADING) {
                    DB.setDownloadFailed(file.getName());
                    App.getInstance().ifPresent(app -> syncFile.getLocalPath().ifPresent(localPath -> app.getOverlayHelper().refresh(localPath)));
                }
                return;
            }
            if (file.getFileSize() != file.getReceived()) {
                logger.debug("Still downloading {} ({}B / {}B)", file.getName(), file.getReceived(), file.getFileSize());
                return;
            }

            syncFile.getTemporaryPath().ifPresent(tempPath -> syncFile.getLocalPath().ifPresent(localPath -> {

                // If temporary path is not set, it means file is not being downloaded.
                if (!tempPath.toFile().exists()) {
                    logger.trace("Temporal downloaded file {} doesn't exist", tempPath);
                    return;
                }

                try {

                    final Path parent = localPath.getParent();
                    if (!parent.toFile().exists()) {
                        logger.debug("Create directories for {}", localPath);
                        Files.createDirectories(parent);
                    }

                    // If local file doesn't exist, it means there are no conflict.
                    if (!localPath.toFile().exists()) {
                        logger.info("New file {} has been downloaded", file.getName());
                        Files.move(tempPath, localPath, StandardCopyOption.REPLACE_EXISTING);
                        syncFile.getCloudCreationTime().ifPresent(cloudCreationTime -> {
                            if (localPath.toFile().setLastModified(cloudCreationTime)) {
                                logger.error("Failed to set timestamp of {}", file.getName());
                            }
                        });
                        DB.setSynced(file, file.getLocalPath());
                        App.getInstance().ifPresent(app -> app.getOverlayHelper().refresh(file.getLocalPath()));
                        return;
                    }

                    // Both local file and downloaded file exist, and solve the conflict.
                    final long cloudCreationTime = file.getCreationTime().orElse(0L);
                    final long localCreationTime = localPath.toFile().lastModified();
                    final long syncTime = syncFile.getLocalModificationTime().orElse(0L);
                    logger.trace(
                            "name = {}, cloudCreationTime = {}, localCreationTime = {}, syncTime = {}",
                            syncFile.getName(), cloudCreationTime, localCreationTime, syncTime);
                    if (cloudCreationTime > localCreationTime) {

                        logger.info("File {} has been downloaded", file.getName());
                        if (localCreationTime > syncTime) {
                            final Path conflictedCopy = Utils.conflictedCopyPath(localPath);
                            Files.move(localPath, conflictedCopy, StandardCopyOption.REPLACE_EXISTING);
                            logger.debug("Conflicted copy of {} has been created", file.getName());
                        }
                        Files.move(tempPath, localPath, StandardCopyOption.REPLACE_EXISTING);
                        if (localPath.toFile().setLastModified(cloudCreationTime)) {
                            logger.error("Failed to set timestamp of {}", file.getName());
                        }
                        if (syncFile.getState() == SyncState.DOWNLOADING) {
                            DB.setSynced(file, file.getLocalPath());
                            App.getInstance().ifPresent(app -> app.getOverlayHelper().refresh(file.getLocalPath()));
                        }

                    } else if (cloudCreationTime < localCreationTime) {

                        if (cloudCreationTime >= syncTime) {
                            final Path conflictedCopy = Utils.conflictedCopyPath(localPath);
                            Files.move(tempPath, conflictedCopy, StandardCopyOption.REPLACE_EXISTING);
                            logger.info("Conflicted copy of {} has been created", file.getName());
                            if (conflictedCopy.toFile().setLastModified(cloudCreationTime)) {
                                logger.error("Failed to set timestamp of {}", conflictedCopy);
                            }
                        } else {
                            logger.trace("Found cloud file was created before last sync time.");
                        }

                    } else if (cloudCreationTime > syncTime) {

                        String cloudDigest;
                        try (final FileInputStream in = new FileInputStream(tempPath.toFile())) {
                            cloudDigest = DigestUtils.sha512Hex(in);
                        }
                        String localDigest;
                        try (final FileInputStream in = new FileInputStream(localPath.toFile())) {
                            localDigest = DigestUtils.sha512Hex(in);
                        }

                        if (!cloudDigest.equals(localDigest)) {
                            logger.info("Conflicted copy of {} has been created", file.getName());
                            final Path conflictedCopy = Utils.conflictedCopyPath(localPath);
                            Files.move(tempPath, conflictedCopy, StandardCopyOption.REPLACE_EXISTING);
                            if (conflictedCopy.toFile().setLastModified(cloudCreationTime)) {
                                logger.error("Failed to set timestamp of {}", conflictedCopy);
                            }
                        } else {
                            logger.error("Downloaded cloud file is same as the corresponding local file {}", file.getName());
                        }

                    } else {
                        logger.trace("File {} has not been changed", file.getName());
                    }

                } catch (final IOException e) {
                    logger.error("Failed post process of downloading {}: {}", file.getName(), e.getLocalizedMessage());
                    if (syncFile.getState() == SyncState.DOWNLOADING) {
                        DB.setDownloadFailed(file.getName());
                        App.getInstance().ifPresent(app -> app.getOverlayHelper().refresh(file.getLocalPath()));
                    }
                }

            }));

        });

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
