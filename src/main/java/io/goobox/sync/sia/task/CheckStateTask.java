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
import io.goobox.sync.sia.RetryableTask;
import io.goobox.sync.sia.StartSiaDaemonTask;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20011Files;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.sia.model.SiaFileFromFilesAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Checks changes in both cloud directory and local directory, and create tasks to handle them.
 *
 * @author junpei
 */
public class CheckStateTask implements Callable<Void> {

    private static final Logger logger = LoggerFactory.getLogger(CheckStateTask.class);

    @NotNull
    private final Context ctx;
    @NotNull
    private final Executor executor;

    public CheckStateTask(@NotNull final Context ctx, @NotNull final Executor executor) {
        this.ctx = ctx;
        this.executor = executor;
    }

    @Override
    public Void call() throws ApiException {

        logger.info("Checking for changes");
        final RenterApi api = new RenterApi(this.ctx.getApiClient());
        try {

            logger.debug("Processing files found in the cloud network");
            final Set<String> processedFiles = this.takeNewestFiles(api.renterFilesGet().getFiles())
                    .stream()
                    .map(this::processCloudFile)
                    .collect(Collectors.toSet());
            this.processLocalFile(processedFiles);

        } catch (final ApiException e) {
            if (e.getCause() instanceof ConnectException) {
                throw e;
            }
            logger.error("Failed to retrieve files stored in sia network: {}", APIUtils.getErrorMessage(e));
        } finally {
            DB.commit();
        }
        return null;

    }

    /**
     * Takes only newest files managed by Goobox from a given file collection.
     *
     * @param files returned by renterFilesGet.
     * @return a collection of SiaFile instances.
     */
    @NotNull
    private Collection<SiaFileFromFilesAPI> takeNewestFiles(@Nullable final Collection<InlineResponse20011Files> files) {

        if (files == null) {
            return Collections.emptyList();
        }

        // Key: file name, Value: file object.
        final Map<String, SiaFileFromFilesAPI> fileMap = new HashMap<>();
        files.stream().filter(InlineResponse20011Files::isAvailable).forEach(file -> {

            final SiaFileFromFilesAPI siaFile = new SiaFileFromFilesAPI(this.ctx, file);
            if (!siaFile.getCloudPath().startsWith(this.ctx.getPathPrefix())) {
                // This file isn't managed by Goobox.
                logger.trace(
                        "Found remote file {} but it's not managed by Goobox (not starts with {})",
                        siaFile.getCloudPath(),
                        this.ctx.getPathPrefix());
                return;
            }

            if (fileMap.containsKey(siaFile.getName())) {

                final SiaFile prev = fileMap.get(siaFile.getName());
                if (siaFile.getCreationTime().orElse(0L) > prev.getCreationTime().orElse(0L)) {
                    logger.debug("Found newer version of remote file {} created at {}", siaFile.getName(),
                            siaFile.getCreationTime());
                    fileMap.put(siaFile.getName(), siaFile);
                } else {
                    logger.debug("Found older version of remote file {} created at {} but ignored",
                            siaFile.getName(), siaFile.getCreationTime());
                }

            } else {
                logger.trace("Found remote file {} created at {}", siaFile.getName(),
                        siaFile.getCreationTime());
                fileMap.put(siaFile.getName(), siaFile);
            }

        });
        return fileMap.values();

    }

    /**
     * Handles the given cloud file and returns the name.
     *
     * @param file to be processed
     * @return the name of the processed file
     */
    @NotNull
    private String processCloudFile(@NotNull SiaFileFromFilesAPI file) {

        try {

            final Optional<SyncFile> syncFileOpt = DB.get(file);
            if (syncFileOpt.isPresent()) {

                final SyncFile syncFile = syncFileOpt.get();
                switch (syncFile.getState()) {
                    case SYNCED:

                        // This file was synced.
                        if (file.getCreationTime().orElse(0L) > syncFile.getLocalModificationTime().orElse(0L)) {
                            // The cloud file was updated, and it will be downloaded.
                            logger.info("Cloud file {} is going to be downloaded", file.getName());
                            this.enqueueForDownload(file);
                        }
                        break;

                    case MODIFIED:

                        // This file has been modified.
                        final long remoteCreationTime = file.getCreationTime().orElse(0L);
                        final long localModificationTime = syncFile.getLocalModificationTime().orElse(0L);
                        if (remoteCreationTime == localModificationTime) {

                            logger.info("File {} was marked as modified but cloud/local files are same", file.getName());
                            DB.setSynced(file, file.getLocalPath());
                            App.getInstance().ifPresent(app -> app.refreshOverlayIcon(file.getLocalPath()));

                        } else if (remoteCreationTime < localModificationTime) {

                            // The newer local file will be uploaded.
                            // Even if the file in cloud was also modified, i.e. there is conflict,
                            // the cloud network supports versioning and the conflict can be solved.
                            logger.info("Local file {} is going to be uploaded", file.getName());
                            this.enqueueForUpload(file.getLocalPath());

                        } else {

                            // Both cloud and local files are modified and it is a conflict.
                            // The cloud file should be downloaded and the local file should be renamed and kept it
                            // too.
                            logger.debug("Conflict detected: file {} is modified in both cloud and local", file.getName());
                            logger.info("Cloud file {} is going to be downloaded", file.getName());
                            this.enqueueForDownload(file);

                        }
                        break;

                    case DELETED:

                        // Since this file has been deleted from the local directory.
                        // it will be deleted from the cloud network, too.
                        logger.info("Remote file {} is going to be deleted", file.getName());
                        this.enqueueForCloudDelete(file);
                        break;

                    case UPLOAD_FAILED:

                        logger.info("Retry to upload file {}", file.getName());
                        this.enqueueForUpload(file.getLocalPath());
                        break;

                    case DOWNLOAD_FAILED:
                        if (Files.exists(file.getLocalPath())) {
                            final long localTimeStamp = Files.getLastModifiedTime(file.getLocalPath()).toMillis();
                            final long cloudTimeStamp = file.getCreationTime().orElse(0L);
                            if (localTimeStamp >= cloudTimeStamp) {
                                logger.info("File {} is marked as {} but then the local file is modified", syncFile.getName(), syncFile.getState());
                                DB.setModified(syncFile.getName(), file.getLocalPath());
                                App.getInstance().ifPresent(app -> app.refreshOverlayIcon(file.getLocalPath()));
                                break;
                            }
                        }
                        logger.info("File {} is marked as {}, retry to download it", syncFile.getName(), syncFile.getState());
                        this.enqueueForDownload(file);
                        break;

                    case CONFLICT:
                        // TODO: Handle this case.
                        logger.warn("File {} is marked as {}", file.getName(), syncFile.getState());
                        break;

                    case FOR_DOWNLOAD:
                    case DOWNLOADING:
                    case FOR_UPLOAD:
                    case UPLOADING:
                    case FOR_LOCAL_DELETE:
                    case FOR_CLOUD_DELETE:
                    default:
                        logger.debug("File {} is marked as {}", file.getName(), syncFile.getState());
                        break;

                }

            } else {

                logger.debug("Cloud file {} is not found in the sync DB", file.getName());
                if (Files.exists(file.getLocalPath())) {

                    // The file exists in the local directory but not in the sync DB.
                    // It means the file still invokes modify event e.g. still being copied etc.
                    // This kind of file should be pended until the modify events end and the file is added to the
                    // sync DB.
                    logger.debug("The file {} is also found in the local storage, pending", file.getName());

                } else {

                    // The file also doesn't exist in the local directory.
                    logger.info("Remote file {} is going to be downloaded", file.getName());
                    this.enqueueForDownload(file);

                }

            }

        } catch (final IOException e) {
            logger.error("Failed to handle file {}: {}", file.getName(), e.getMessage());
        }
        return file.getName();

    }

    /**
     * Enqueues a file represented by the given local path to be uploaded.
     *
     * @param localPath to the file to be uploaded.
     * @throws IOException if failed to access the local file.
     */
    private void enqueueForUpload(@NotNull final Path localPath) throws IOException {

        final Path name = this.ctx.getConfig().getSyncDir().relativize(localPath);

        long lastModifiedTime;
        try {
            lastModifiedTime = Files.getLastModifiedTime(localPath).toMillis();
        } catch (final IOException e) {
            logger.error("Failed to get the time stamp of {}: {}", localPath, e.getMessage());
            lastModifiedTime = System.currentTimeMillis();
        }
        final Path cloudPath = this.ctx.getPathPrefix().resolve(name).resolve(Long.toString(lastModifiedTime));
        try {
            DB.setForUpload(this.ctx.getName(localPath), localPath, cloudPath);
            App.getInstance().ifPresent(app -> app.refreshOverlayIcon(localPath));
            executor.execute(new RetryableTask(new UploadLocalFileTask(ctx, localPath), new StartSiaDaemonTask()));
        } catch (final IOException e) {
            if (Files.exists(localPath)) {
                throw e;
            }
            logger.info("File {} was deleted", name);
            // For now, marks the file was deleted, next round of CheckStateTask will update it to FOR_CLOUD_DELETE, etc.
            DB.setDeleted(this.ctx.getName(localPath));
        }

    }

    /**
     * Enqueue a file represented by the given SiaFile object to be download.
     *
     * @param file to be downloaded.
     */
    private void enqueueForDownload(@NotNull final SiaFile file) throws IOException {

        DB.addForDownload(file, file.getLocalPath());
        if (Files.exists(file.getLocalPath())) {
            App.getInstance().ifPresent(app -> app.refreshOverlayIcon(file.getLocalPath()));
        }
        this.executor.execute(new RetryableTask(new DownloadCloudFileTask(this.ctx, file.getName()), new StartSiaDaemonTask()));

    }

    /**
     * Enqueue a file to be deleted from the cloud network.
     *
     * @param file to be deleted from the cloud network.
     */
    private void enqueueForCloudDelete(@NotNull final SiaFile file) {

        DB.setForCloudDelete(file);
        this.executor.execute(new RetryableTask(new DeleteCloudFileTask(this.ctx, file.getName()), new StartSiaDaemonTask()));

    }

    /**
     * Enqueue a file to be deleted from the local directory.
     *
     * @param localPath to the file to be deleted from the local directory.
     */
    private void enqueueForLocalDelete(@NotNull final Path localPath) {

        DB.setForLocalDelete(this.ctx.getName(localPath));
        App.getInstance().ifPresent(app -> app.refreshOverlayIcon(localPath));
        this.executor.execute(new DeleteLocalFileTask(this.ctx, localPath));

    }

    private void processLocalFile(Set<String> processedFiles) {

        logger.debug("Processing files stored only in the local directory and modified");
        DB.getFiles(SyncState.MODIFIED)
                .filter(syncFile -> !processedFiles.contains(syncFile.getName()))
                .map(syncFile -> {
                    // This file is not stored in the cloud network and modified from the local directory.
                    // It should be uploaded.
                    try {
                        logger.info("Local file {} is going to be uploaded", syncFile.getName());
                        this.enqueueForUpload(this.ctx.getConfig().getSyncDir().resolve(syncFile.getName()));
                    } catch (final IOException e) {
                        logger.error("Failed to upload {}: {}", syncFile.getName(), e.getMessage());
                        DB.setUploadFailed(syncFile.getName());
                    }
                    return syncFile.getName();
                })
                .forEach(processedFiles::add);

        logger.debug("Processing files stored only in the local directory but deleted");
        DB.getFiles(SyncState.DELETED)
                .filter(syncFile -> !processedFiles.contains(syncFile.getName()))
                .map(syncFile -> {
                    // This file exist in neither the cloud network nor the local directory, but in the sync DB.
                    // It should be deleted from the DB.
                    logger.debug("Remove deleted file {} from the sync DB", syncFile.getName());
                    DB.remove(syncFile.getName());
                    return syncFile.getName();
                })
                .forEach(processedFiles::add);

        logger.debug("Processing files stored only in the local directory but marked as synced");
        DB.getFiles(SyncState.SYNCED)
                .filter(syncFile -> !processedFiles.contains(syncFile.getName()))
                .map(syncFile -> {
                    // This file has been synced but now exists only in the local directory.
                    // It means this file was deleted from the cloud network by another client.
                    // This file should be deleted from the local directory, too.
                    logger.info("Local file {} is going to be deleted since it was deleted from the cloud storage", syncFile.getName());
                    this.enqueueForLocalDelete(this.ctx.getConfig().getSyncDir().resolve(syncFile.getName()));
                    return syncFile.getName();
                })
                .forEach(processedFiles::add);

        logger.debug("Processing files filed to be uploaded");
        DB.getFiles(SyncState.UPLOAD_FAILED)
                .filter(syncFile -> !processedFiles.contains(syncFile.getName()))
                .map(syncFile -> {
                    // This file is marked as filed to upload and don't exist in the available file list.
                    // This file should be uploaded again.
                    try {
                        logger.info("Retry to upload file {}", syncFile.getName());
                        this.enqueueForUpload(this.ctx.getConfig().getSyncDir().resolve(syncFile.getName()));
                    } catch (final IOException e) {
                        logger.error("Failed to upload {}: {}", syncFile.getName(), e.getMessage());
                    }
                    return syncFile.getName();
                })
                .forEach(processedFiles::add);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CheckStateTask that = (CheckStateTask) o;
        return ctx.equals(that.ctx) && executor.equals(that.executor);
    }

    @Override
    public int hashCode() {
        int result = ctx.hashCode();
        result = 31 * result + executor.hashCode();
        return result;
    }

}
