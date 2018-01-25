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

package io.goobox.sync.sia.command;

import io.goobox.sync.sia.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class CmdUtils {

    /**
     * Default config file name.
     */
    static final String ConfigFileName = "goobox.properties";

    private static final Logger logger = LoggerFactory.getLogger(CmdUtils.class);

    /**
     * Load configuration.
     *
     * @param path file path to a configuration file.
     * @return a Config object.
     */
    static Config loadConfig(final Path path) {

        Config cfg;
        try {
            cfg = Config.load(path);
        } catch (IOException e) {
            logger.warn("Failed to read config file {}: {}", path, e.getMessage());
            logger.info("Loading the default configuration");
            cfg = new Config();
            cfg.setFilePath(path);
        }
        return cfg;

    }

}
