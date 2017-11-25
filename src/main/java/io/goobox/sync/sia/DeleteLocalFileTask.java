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

import io.goobox.sync.sia.db.DB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * DeleteLocalFileTask deletes a given local file.
 *
 * @author junpei
 */
class DeleteLocalFileTask implements Runnable {

    private final Path path;
    private static final Logger logger = LogManager.getLogger();

    DeleteLocalFileTask(final Path path) {
        this.path = path;
    }

    @Override
    public void run() {

        logger.info("Deleting local file {}", this.path);
        try {

            final boolean success = Files.deleteIfExists(this.path);
            if (!success) {
                logger.warn("File {} doesn't exist", this.path);
            }
            DB.remove(this.path);
            DB.commit();

        } catch (IOException e) {
            logger.error("Cannot delete local file {}: {}", this.path, e.getMessage());
        }

    }

}
