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

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobox.sync.storj.db.DB;

/**
 * DeleteLocalFileTask deletes a given local file.
 * 
 * @author junpei
 *
 */
public class DeleteLocalFileTask implements Runnable {

	private final Path path;
	private final Logger logger = LogManager.getLogger();

	public DeleteLocalFileTask(final Path path) {
		this.path = path;
	}

	@Override
	public void run() {

		this.logger.info("Deleting local file {}", this.path);
		try {
			boolean success = Files.deleteIfExists(this.path);
			if (success) {
				DB.remove(this.path);
				DB.commit();
			} else {
				this.logger.warn("File {} doesn't exist", this.path);
			}
		} catch (Exception e) {
			this.logger.error("Cannot delete local file {}: {}", this.path, e.getMessage());
		}

	}

}
