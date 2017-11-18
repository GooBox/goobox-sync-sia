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
package io.goobox.sync.sia.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobox.sync.storj.Utils;

/**
 * SiaPath represents a file path in SIA and provides utility methods.
 * 
 * In Goobox, a file path in SIA, i.e. sia path, must follow the format below:
 * 
 * <username>/Goobox/<filepath>/<UNIX time>
 * 
 * where filepath allows any relative path.
 * 
 * @author junpei
 *
 */
public class SiaPath {
	
	private final Logger logger = LogManager.getLogger();

	/**
	 * file path identifying a file in SIA's blockchain.
	 */
	public final Path siaPath;

	/**
	 * file path identifying a uploaded file; it doesn't have any prefix and
	 * timestamp.
	 */
	public final Path remotePath;

	/**
	 * file path representing a file associated with this remote file.
	 */
	public final Path localPath;
	public final long created;

	public SiaPath(final String siaPath, final Path prefix) {

		this.siaPath = Paths.get(siaPath);

		Path withoutTimestamp = this.siaPath;
		long created = 0;
		if (this.siaPath.getNameCount() - prefix.getNameCount() != 1) {
			try {
				created = Long.parseLong(this.siaPath.getFileName().toString());
				withoutTimestamp = this.siaPath.getParent();
			} catch (NumberFormatException e) {
				this.logger.warn("siapath {} doesn't have its creation time", siaPath);
			}
		}
		this.created = created;

		this.remotePath = prefix.relativize(withoutTimestamp);
		this.localPath = Utils.getSyncDir().resolve(remotePath);

	}

	/**
	 * Checks this path starts with the given path.
	 * 
	 * @param p
	 * @return
	 */
	public boolean startsWith(final Path p) {
		return this.siaPath.startsWith(p);
	}

	/**
	 * Returns File object which the local path represents.
	 * 
	 * @return
	 */
	public File getLocalFile() {
		return this.localPath.toFile();
	}

	@Override
	public String toString() {
		return new ReflectionToStringBuilder(this).toString();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if(!(obj instanceof SiaPath)) {
			return false;
		}
		final SiaPath p = (SiaPath)obj;
		return this.siaPath.equals(p.siaPath);
	}

}
