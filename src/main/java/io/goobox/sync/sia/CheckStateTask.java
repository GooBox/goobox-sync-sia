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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20011;
import io.goobox.sync.sia.client.api.model.InlineResponse20011Files;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.util.SiaFile;
import io.goobox.sync.sia.util.SiaFileFromFilesAPI;
import io.goobox.sync.storj.Utils;
import io.goobox.sync.storj.db.SyncState;

/**
 * 
 * @author junpei
 *
 */
public class CheckStateTask implements Runnable {

	private final Context ctx;
	private final Logger logger = LogManager.getLogger();

	public CheckStateTask(final Context ctx) {
		this.ctx = ctx;
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

						this.logger.debug("File {} was deleted from the local directory", file.getName());
						// The file exists in the DB but not in the local directory.

						// TODO: Currently disabled because it is out of one way sync.
						// DB.setForCloudDelete(file);
						// api.renterDeleteSiapathPost(file.getSiapath());

					} else {

						try {

							SyncFile syncFile = DB.get(file);
							final boolean cloudChanged = syncFile.getState() != SyncState.UPLOAD_FAILED
									&& syncFile.getRemoteCreatedTime() != file.getCreationTime();
							final boolean localChanged = syncFile.getState() != SyncState.DOWNLOAD_FAILED
									&& syncFile.getLocalModifiedTime() != Files.getLastModifiedTime(file.getLocalPath())
											.toMillis();
							if (cloudChanged && localChanged) {
								// both local and cloud has been changed - conflict
								DB.setConflict(file);
							} else if (cloudChanged) {

								if (syncFile.getState().isConflict()) {
									// the file has been in conflict before - keep the conflict
									DB.setConflict(file);
								} else {
									// download
									// TODO: create sub directories.
									this.logger.info("Downloading {} to {}", file.getRemotePath(), file.getLocalPath());
									DB.addForDownload(file);
									api.renterDownloadasyncSiapathGet(file.getRemotePath().toString(),
											file.getLocalPath().toString());
								}

							} else if (localChanged) {

								if (syncFile.getState().isConflict()) {
									// the file has been in conflict before - keep the conflict
									DB.setConflict(file);
								} else {

									// TODO: Currently disabled because it is out of one way sync.
									// upload
									// DB.addForUpload(file);
									// api.renterUploadSiapathPost(file.path.siaPath.toString(),
									// this.ctx.config.dataPieces, this.ctx.config.parityPieces,
									// file.path.localPath.toString());
								}

							} else {
								// no change - do nothing
							}

						} catch (IOException e) {
							e.printStackTrace();
						}

					}

				} else {
					// The file doesn't exist in the local DB.
					this.logger.debug("New file {} is found in the cloud storage", file.getName());

					if (!file.getLocalPath().toFile().exists()) {
						// The file also doesn't exist in the local directory.

						// TODO: create sub directories.
						this.logger.info("Downloading {} to {}", file.getRemotePath(), file.getLocalPath());
						DB.addForDownload(file);
						api.renterDownloadasyncSiapathGet(file.getRemotePath().toString(),
								file.getLocalPath().toString());

					} else {
						// The file exists in the local directory.

						// check if local and cloud file are same
						if (file.getFileSize() == Files.size(file.getLocalPath())) {
							DB.setSynced(file);
						} else {
							DB.setConflict(file);
						}

					}
				}

				localPaths.remove(file.getLocalPath());

			}

			// Process local files without cloud counterpart
			for (Path absPath : localPaths) {

				final Path path = Utils.getSyncDir().relativize(absPath);
				if (DB.contains(path)) {

					DB.setForLocalDelete(path);
					this.ctx.tasks.add(new DeleteLocalFileTask(absPath));

					// TODO: Update here to support two way sync.
					// SyncFile syncFile = DB.get(path);
					// if (syncFile.getState().isSynced()) {
					//
					// DB.setForLocalDelete(path);
					// this.ctx.tasks.add(new DeleteLocalFileTask(path));
					//
					// } else if (syncFile.getState() == SyncState.UPLOAD_FAILED
					// && syncFile.getLocalModifiedTime() !=
					// Files.getLastModifiedTime(path).toMillis()) {
					//
					// // TODO: Currently disabled because it is out of one way sync.
					// // DB.addForUpload(path);
					// // tasks.add(new UploadFileTask(gooboxBucket, path));
					//
					// }

				} else {

					// TODO: Currently disabled because it is out of one way sync.
					// DB.addForUpload(path);
					// tasks.add(new UploadFileTask(gooboxBucket, path));

				}

			}

		} catch (ApiException e) {
			
			this.logger.error("Failed to retreive files stored in the SIA network", APIUtils.getErrorMessage(e));

		} catch (IOException e) {

			this.logger.catching(e);

		}

		DB.commit();
		if (this.ctx.tasks.size() < 2) {
			// Sleep some time
			this.ctx.tasks.add(new SleepTask());
		}
		// Add itself to the queueAdd itself to the queue
		this.ctx.tasks.add(this);

	}

	/**
	 * Takes only newest files managed by Goobox from a given file collection.
	 * 
	 * @param files
	 * @return
	 */
	private Collection<SiaFile> takeNewestFiles(final Collection<InlineResponse20011Files> files) {

		// Key: remote path, Value: file object.
		final Map<String, SiaFile> filemap = new HashMap<String, SiaFile>();
		if (files != null) {
			for (InlineResponse20011Files file : files) {

				if (!file.getAvailable()) {
					// This file is still being uploaded.
					this.logger.trace("File {} is not available", file.getSiapath());
					continue;
				}

				final SiaFile siaFile = new SiaFileFromFilesAPI(file, this.ctx.pathPrefix);
				if (!siaFile.getRemotePath().startsWith(this.ctx.pathPrefix)) {
					// This file isn't managed by Goobox.
					this.logger.trace("File {} is not managed by Goobox", siaFile.getRemotePath());
					continue;
				}

				if (filemap.containsKey(siaFile.getName())) {

					final SiaFile prev = filemap.get(siaFile.getName());
					if (siaFile.getCreationTime() > prev.getCreationTime()) {
						this.logger.trace("Found newer version of remote file {} created at {}", siaFile.getName(),
								siaFile.getCreationTime());
						filemap.put(siaFile.getName(), siaFile);
					} else {
						this.logger.trace("Found older version of remote file {} created at {} but ignored",
								siaFile.getName(), siaFile.getCreationTime());
					}

				} else {
					this.logger.trace("Found remote file {} created at {}", siaFile.getName(),
							siaFile.getCreationTime());
					filemap.put(siaFile.getName(), siaFile);
				}

			}
		}
		return filemap.values();

	}

	/**
	 * Returns a list of paths representing local files in the sync directory.
	 * 
	 * The returned paths include sub directories.
	 * 
	 * @return
	 */
	private Set<Path> getLocalPaths() {

		final Set<Path> paths = new HashSet<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Utils.getSyncDir())) {
			for (Path path : stream) {
				this.logger.trace("Found local file {}", path);
				paths.add(path);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return paths;
	}

}
