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
import io.goobox.sync.sia.client.api.model.InlineResponse20011Files;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.sia.model.SiaFileFromFilesAPI;
import io.goobox.sync.storj.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Checks changes in both cloud directory and local directory, and create tasks to handle them.
 *
 * @author junpei
 */
class CheckStateTask implements Runnable {

    private static final Logger logger = LogManager.getLogger();

    @NotNull
    private final Context ctx;
    @NotNull
    private final Executor executor;

    CheckStateTask(@NotNull final Context ctx, @NotNull final Executor executor) {
        this.ctx = ctx;
        this.executor = executor;
    }

    @Override
    public void run() {

        logger.info("Checking for changes");
        final RenterApi api = new RenterApi(this.ctx.apiClient);
        final Set<String> processedFiles = new HashSet<>();
        try {

            logger.trace("Processing files stored in the cloud netwrok");
            for (final SiaFile file : this.takeNewestFiles(api.renterFilesGet().getFiles())) {

                try {

                    if (DB.contains(file)) {

                        final SyncFile syncFile = DB.get(file);
                        switch (syncFile.getState()) {
                            case SYNCED:

                                // This file was synced.
                                if (file.getCreationTime() > syncFile.getLocalModificationTime().orElse(0L)) {

                                    // The cloud file was updated, and it will be downloaded.
                                    logger.info("Cloud file {} is going to be downloaded", file.getName());
                                    this.enqueueForDownload(file);

                                }
                                break;

                            case MODIFIED:

                                // This file has been modified.
                                if (file.getCreationTime() < syncFile.getLocalModificationTime().orElse(0L)) {

                                    // The newer local file will be uploaded.
                                    // Even if the file in cloud was also modified, i.e. there is conflict,
                                    // the cloud network supports versioning and the conflict can be solved.
                                    logger.info("Local file {} is going to be uploaded", file.getName());
                                    this.enqueueForUpload(file.getLocalPath());

                                } else {

                                    // Both cloud and local files are modified and it is a conflict.
                                    // The cloud file should be downloaded and the local file should be renamed and kept it
                                    // too.
                                    logger.warn("Conflict detected: file {} is modified in both cloud and local", file.getName());
                                    DB.setConflict(file, file.getLocalPath());

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
                            default:
                                logger.debug("File {} ({}) is skipped", file.getName(), syncFile.getState());
                                break;

                        }


                    } else {

                        // The file is found in the cloud network but doesn't exist in the local DB.
                        logger.debug("New file {} is found in the cloud storage", file.getName());
                        if (file.getLocalPath().toFile().exists()) {

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

                } catch (IOException e) {
                    logger.error("Failed to handle file {}: {}", file.getName(), e.getMessage());
                }

                processedFiles.add(file.getName());

            }

            logger.trace("Processing files stored only in the local directory and modified");
            for (final SyncFile syncFile : DB.getModifiedFiles()) {
                if (processedFiles.contains(syncFile.getName())) {
                    continue;
                }
                // This file is not stored in the cloud network and modified from the local directory.
                // It should be uploaded.
                try {
                    logger.info("Local file {} is going to be uploaded", syncFile.getName());
                    this.enqueueForUpload(Utils.getSyncDir().resolve(syncFile.getName()));
                } catch (IOException e) {
                    logger.error("Failed to upload {}: {}", syncFile.getName(), e.getMessage());
                }
                processedFiles.add(syncFile.getName());
            }

            logger.trace("Processing files stored only in the local directory but deleted");
            for (final SyncFile syncFile : DB.getDeletedFiles()) {
                if (processedFiles.contains(syncFile.getName())) {
                    continue;
                }
                // This file exist in neither the cloud network nor the local directory, but in the sync DB.
                // It should be deleted from the DB.
                logger.debug("Remove deleted file {} from the sync DB", syncFile.getName());
                DB.remove(Utils.getSyncDir().resolve(syncFile.getName()));
                processedFiles.add(syncFile.getName());
            }

            logger.trace("Processing files stored only in the local directory but marked as synced");
            for (final SyncFile syncFile : DB.getSyncedFiles()) {
                if (processedFiles.contains(syncFile.getName())) {
                    continue;
                }
                // This file has been synced but now exists only in the local directory.
                // It means this file was deleted from the cloud network by another client.
                // This file should be deleted from the local directory, too.
                logger.info("Local file {} is going to be deleted since it was deleted from the cloud storage", syncFile.getName());
                this.enqueueForLocalDelete(Utils.getSyncDir().resolve(syncFile.getName()));
                processedFiles.add(syncFile.getName());
            }

        } catch (ApiException e) {
            logger.error("Failed to retrieve files stored in the SIA network", APIUtils.getErrorMessage(e));
        } finally {

            DB.commit();

        }

    }

    /**
     * Takes only newest files managed by Goobox from a given file collection.
     *
     * @param files returned by renterFilesGet.
     * @return a collection of SiaFile instances.
     */
    private Collection<SiaFile> takeNewestFiles(final Collection<InlineResponse20011Files> files) {

        // Key: file name, Value: file object.
        final Map<String, SiaFile> fileMap = new HashMap<>();
        if (files != null) {
            for (InlineResponse20011Files file : files) {

                if (!file.getAvailable()) {
                    // This file is still being uploaded.
                    logger.debug("Found remote file {} but it's not available", file.getSiapath());
                    continue;
                }

                final SiaFile siaFile = new SiaFileFromFilesAPI(file, this.ctx.pathPrefix);
                if (!siaFile.getCloudPath().startsWith(this.ctx.pathPrefix)) {
                    // This file isn't managed by Goobox.
                    logger.debug("Found remote file {} but it's not managed by Goobox", siaFile.getCloudPath());
                    continue;
                }

                if (fileMap.containsKey(siaFile.getName())) {

                    final SiaFile prev = fileMap.get(siaFile.getName());
                    if (siaFile.getCreationTime() > prev.getCreationTime()) {
                        logger.debug("Found newer version of remote file {} created at {}", siaFile.getName(),
                                siaFile.getCreationTime());
                        fileMap.put(siaFile.getName(), siaFile);
                    } else {
                        logger.debug("Found older version of remote file {} created at {} but ignored",
                                siaFile.getName(), siaFile.getCreationTime());
                    }

                } else {
                    logger.debug("Found remote file {} created at {}", siaFile.getName(),
                            siaFile.getCreationTime());
                    fileMap.put(siaFile.getName(), siaFile);
                }

            }
        }
        return fileMap.values();

    }

    /**
     * Enqueues a file represented by the given local path to be uploaded.
     *
     * @param localPath to the file to be uploaded.
     * @throws IOException if failed to access the local file.
     */
    private void enqueueForUpload(final Path localPath) throws IOException {

        final Path name = Utils.getSyncDir().relativize(localPath);
        final Path cloudPath = this.ctx.pathPrefix.resolve(name).resolve(String.valueOf(localPath.toFile().lastModified()));
        DB.setForUpload(localPath, cloudPath);
        this.executor.execute(new UploadLocalFileTask(this.ctx, localPath));

    }

    /**
     * Enqueue a file represented by the given SiaFile object to be download.
     *
     * @param file to be downloaded.
     */
    private void enqueueForDownload(final SiaFile file) throws IOException {

        DB.addForDownload(file, file.getLocalPath());
        this.executor.execute(new DownloadCloudFileTask(this.ctx, file.getName()));

    }

    /**
     * Enqueue a file to be deleted from the cloud network.
     *
     * @param file to be deleted from the cloud network.
     */
    private void enqueueForCloudDelete(final SiaFile file) {

        DB.setForCloudDelete(file);
        this.executor.execute(new DeleteCloudFileTask(this.ctx, file.getName()));

    }

    /**
     * Enqueue a file to be deleted from the local directory.
     *
     * @param localPath to the file to be deleted from the local directory.
     */
    private void enqueueForLocalDelete(final Path localPath) {

        DB.setForLocalDelete(localPath);
        this.executor.execute(new DeleteLocalFileTask(localPath));

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
