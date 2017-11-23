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
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.sia.model.SiaFileFromFilesAPI;
import io.goobox.sync.storj.Utils;
import io.goobox.sync.storj.db.SyncState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * @author junpei
 */
public class CheckStateTask implements Runnable {

    private final Context ctx;
    private final Executor executor;
    private final Logger logger = LogManager.getLogger();

    public CheckStateTask(final Context ctx, final Executor executor) {
        this.ctx = ctx;
        this.executor = executor;
    }

    @Override
    public void run() {

        this.logger.info("Checking for changes");
        final RenterApi api = new RenterApi(this.ctx.apiClient);
        try {

            final Set<Path> localPaths = this.getLocalPaths();
            final InlineResponse20011 files = api.renterFilesGet();
            for (SiaFile file : this.takeNewestFiles(files.getFiles())) {

                if (DB.contains(file)) {

                    if (!file.getLocalPath().toFile().exists()) {
                        // The file exists in the DB but not in the local directory.
                        this.logger.info("Remote file {} is going to be deleted", file.getName());
                        DB.setForCloudDelete(file);
                        this.executor.execute(new DeleteRemoteFileTask(this.ctx, file));

                    } else {

                        try {

                            SyncFile syncFile = DB.get(file);
                            final boolean cloudChanged = syncFile.getState() != SyncState.UPLOAD_FAILED
                                    && syncFile.getRemoteCreatedTime() != file.getCreationTime();
                            final boolean localChanged = syncFile.getState() != SyncState.DOWNLOAD_FAILED
                                    && syncFile.getLocalModifiedTime() != Files.getLastModifiedTime(file.getLocalPath()).toMillis();
                            this.logger.trace("Status of {}: cloudChanged = {}, localChanged = {}", file.getName(), cloudChanged, localChanged);
                            if (cloudChanged && localChanged) {
                                // both local and cloud has been changed - conflict
                                this.logger.warn("File {} is conflict", file.getLocalPath());
                                DB.setConflict(file);
                            } else if (cloudChanged) {

                                if (syncFile.getState().isConflict()) {
                                    // the file has been in conflict before - keep the conflict
                                    this.logger.warn("File {} is conflict", file.getLocalPath());
                                    DB.setConflict(file);
                                } else {

                                    // download
                                    this.logger.info("Remote file {} is going to be downloaded", file.getName());
                                    DB.addForDownload(file);
                                    this.executor.execute(new DownloadRemoteFileTask(ctx, file));

                                }

                            } else if (localChanged) {

                                if (syncFile.getState().isConflict()) {
                                    // the file has been in conflict before - keep the conflict
                                    this.logger.warn("File {} is conflict", file.getLocalPath());
                                    DB.setConflict(file);
                                } else {

                                    // upload
                                    this.logger.info("Local file {} is going to be uploaded", file.getName());
                                    DB.addForUpload(file);
                                    final Date created = new Date(file.getLocalPath().toFile().lastModified());
                                    this.executor.execute(new UploadLocalFileTask(this.ctx, file, created));

                                }

                            } else {
                                // no change - do nothing
                            }

                        } catch (IOException e) {
                            this.logger.error("Failed to access {}: {}", file.getLocalPath(), e.getMessage());
                        }

                    }

                } else {
                    // The file doesn't exist in the local DB.
                    this.logger.debug("New file {} is found in the cloud storage", file.getName());

                    if (!file.getLocalPath().toFile().exists()) {
                        // The file also doesn't exist in the local directory.

                        this.logger.info("Remote file {} is going to be downloaded", file.getName());
                        DB.addForDownload(file);
                        this.executor.execute(new DownloadRemoteFileTask(ctx, file));

                    } else {
                        // The file exists in the local directory.

                        // check if local and cloud file are same
                        if (file.getFileSize() == Files.size(file.getLocalPath())) {
                            this.logger.debug("File {} exists in the local sync folder", file.getName());
                            DB.setSynced(file);
                        } else {
                            this.logger.warn("File {} is conflict", file.getLocalPath());
                            DB.setConflict(file);
                        }

                    }
                }

                localPaths.remove(file.getLocalPath());

            }

            // Process local files without cloud counterpart
            for (Path localPath : localPaths) {

                if (DB.contains(localPath)) {

                    SyncFile syncFile = DB.get(localPath);
                    if (syncFile.getState().isSynced()) {

                        this.logger.info("Local file {} is going to be deleted", localPath);
                        DB.setForLocalDelete(localPath);
                        this.executor.execute(new DeleteLocalFileTask(localPath));

                    } else if (syncFile.getState() == SyncState.UPLOAD_FAILED
                            && syncFile.getLocalModifiedTime() !=
                            Files.getLastModifiedTime(localPath).toMillis()) {

                        this.logger.info("Local file {} is going to be uploaded", localPath);
                        DB.addForUpload(localPath);
                        final Date created = new Date(localPath.toFile().lastModified());
                        this.executor.execute(new UploadLocalFileTask(this.ctx, localPath, created));

                    }

                } else {

                    this.logger.info("Local file {} is going to be uploaded", localPath);
                    // TODO:
                    DB.addForUpload(localPath);
                    final Date created = new Date(localPath.toFile().lastModified());
                    this.executor.execute(new UploadLocalFileTask(this.ctx, localPath, created));

                }

            }

        } catch (ApiException e) {

            this.logger.error("Failed to retrieve files stored in the SIA network", APIUtils.getErrorMessage(e));

        } catch (IOException e) {

            this.logger.catching(e);

        }

        DB.commit();

    }

    /**
     * Takes only newest files managed by Goobox from a given file collection.
     *
     * @param files
     * @return
     */
    private Collection<SiaFile> takeNewestFiles(final Collection<InlineResponse20011Files> files) {

        // Key: file name, Value: file object.
        final Map<String, SiaFile> filemap = new HashMap<String, SiaFile>();
        if (files != null) {
            for (InlineResponse20011Files file : files) {

                if (!file.getAvailable()) {
                    // This file is still being uploaded.
                    this.logger.debug("Found remote file {} but it's not available", file.getSiapath());
                    continue;
                }

                final SiaFile siaFile = new SiaFileFromFilesAPI(file, this.ctx.pathPrefix);
                if (!siaFile.getRemotePath().startsWith(this.ctx.pathPrefix)) {
                    // This file isn't managed by Goobox.
                    this.logger.debug("Found remote file {} but it's not managed by Goobox", siaFile.getRemotePath());
                    continue;
                }

                if (filemap.containsKey(siaFile.getName())) {

                    final SiaFile prev = filemap.get(siaFile.getName());
                    if (siaFile.getCreationTime() > prev.getCreationTime()) {
                        this.logger.debug("Found newer version of remote file {} created at {}", siaFile.getName(),
                                siaFile.getCreationTime());
                        filemap.put(siaFile.getName(), siaFile);
                    } else {
                        this.logger.debug("Found older version of remote file {} created at {} but ignored",
                                siaFile.getName(), siaFile.getCreationTime());
                    }

                } else {
                    this.logger.debug("Found remote file {} created at {}", siaFile.getName(),
                            siaFile.getCreationTime());
                    filemap.put(siaFile.getName(), siaFile);
                }

            }
        }
        return filemap.values();

    }

    /**
     * Returns a list of paths representing local files in the sync directory.
     * <p>
     * The returned paths include sub directories.
     *
     * @return
     */
    private Set<Path> getLocalPaths() {

        final Set<Path> paths = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Utils.getSyncDir())) {
            for (Path path : stream) {
                this.logger.debug("Found local file {}", path);
                paths.add(path);
            }
        } catch (IOException e) {
            this.logger.error("Failed to list files: {}", e.getMessage());

        }
        return paths;
    }

}
