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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Config defines information stored in a config file.
 * 
 * @author junpei
 *
 */
public class Config {

	/**
	 * user name.
	 */
	String userName;

	/**
	 * primary seed.
	 */
	String primarySeed;

	/**
	 * The number of data pieces to use when erasure coding the file.
	 */
	int dataPieces;

	/**
	 * The number of parity pieces to use when erasure coding the file. Total
	 * redundancy of the file is (datapieces+paritypieces)/datapieces. Minimum
	 * required: 12
	 */
	int parityPieces;

	private static Logger logger = LogManager.getLogger();

	/**
	 * Save this configurations to the given file.
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void save(final Path path) throws IOException {

		final Properties props = new Properties();
		props.setProperty("username", this.userName);
		props.setProperty("primary-seed", this.primarySeed);
		props.setProperty("data-pieces", String.valueOf(this.dataPieces));
		props.setProperty("parity-pieces", String.valueOf(this.parityPieces));

		try (final BufferedWriter output = Files.newBufferedWriter(path,
				StandardOpenOption.CREATE_NEW)) {
			props.store(output, "");
		}

	}

	/**
	 * Load configurations from the given file.
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static Config load(final Path path) throws IOException {

		logger.info("Loading config file {}", path);
		final Properties props = new Properties();
		try (final InputStream in = Files.newInputStream(path)) {
			props.load(in);
		}

		final Config cfg = new Config();
		cfg.userName = props.getProperty("username");
		cfg.primarySeed = props.getProperty("primary-seed");

		try {
			cfg.dataPieces = Integer.valueOf(props.getProperty("data-pieces"));
		} catch (NumberFormatException e) {
			logger.warn("Invalid data pieces {}, use 1 instead", props.getProperty("data-pieces"));
			cfg.dataPieces = 1;
		}

		try {
			cfg.parityPieces = Integer.valueOf(props.getProperty("parity-pieces"));
			if (cfg.parityPieces < 12) {
				logger.warn("Invalid parity pieces {}, use 12 instead", cfg.parityPieces);
				cfg.parityPieces = 12;
			}
		} catch (NumberFormatException e) {
			logger.warn("Invalid parity pieces {}, use 12 instead", props.getProperty("parity-pieces"));
			cfg.parityPieces = 12;
		}

		return cfg;

	}

}
