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
import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20010;
import io.goobox.sync.sia.client.api.model.InlineResponse20010Downloads;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.util.SiaFileFromDownloadsAPI;
import io.goobox.sync.storj.db.SyncState;

/**
 * CheckDownloadTask requests current downloading status to siad and prints it.
 * 
 * @author junpei
 *
 */
public class CheckDownloadTask implements Runnable {

	private final Context ctx;
	private final Logger logger = LogManager.getLogger();

	public CheckDownloadTask(final Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public void run() {

		this.logger.info("Checking downloading status");
		final RenterApi api = new RenterApi(this.ctx.apiClient);
		try {

			final InlineResponse20010 downloads = api.renterDownloadsGet();
			int nFiles = 0;

			for (InlineResponse20010Downloads rawFile : checkNullCollection(downloads.getDownloads())) {

				final SiaFileFromDownloadsAPI file = new SiaFileFromDownloadsAPI(rawFile, this.ctx.pathPrefix);
				if (!file.getRemotePath().startsWith(this.ctx.pathPrefix)) {
					// This file isn't managed by Goobox.
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
					if (DB.contains(file)) {
						final SyncFile syncFile = DB.get(file);
						if (syncFile.getState() == SyncState.FOR_DOWNLOAD) {
							try {
								DB.setSynced(file);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
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
			
			this.logger.error("Failed to retreive downloading files: {}", APIUtils.getErrorMessage(e));

		}

		// Enqueue this task.
		this.ctx.tasks.add(this);

	}

	/**
	 * Check the given collection is null and if so, returns an empty collection.
	 */
	private static <T> Collection<T> checkNullCollection(final Collection<T> list) {
		if(list == null) {
			return new ArrayList<T>();
		}
		return list;
	}

}
