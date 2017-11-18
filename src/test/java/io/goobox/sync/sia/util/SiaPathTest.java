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

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.junit.Test;

import io.goobox.sync.storj.Utils;

public class SiaPathTest {

	@Test
	public void testValidSiaPath() {

		final String user = "testuser";
		final String path = "foo/bar.txt";
		final long created = new Date().getTime();
		final Path prefix = Paths.get(user, "Goobox");
		final Path inPath = Paths.get(user, "Goobox", path, String.valueOf(created));
		final SiaPath siaPath = new SiaPath(inPath.toString(), prefix);

		assertEquals(siaPath.siaPath, inPath);
		assertEquals(siaPath.created, created);
		assertEquals(siaPath.localPath, Paths.get(Utils.getSyncDir().toString(), path));
		assertEquals(siaPath.remotePath, Paths.get(path));

	}

	@Test
	public void testInvalidSiaPath() {

		final String user = "testuser";
		final String path = "foo/bar.txt";
		final Path prefix = Paths.get(user, "Goobox");
		final Path inPath = Paths.get(user, "Goobox", path);
		final SiaPath siaPath = new SiaPath(inPath.toString(), prefix);

		assertEquals(siaPath.siaPath, inPath);
		assertEquals(siaPath.created, 0);
		assertEquals(siaPath.localPath, Paths.get(Utils.getSyncDir().toString(), path));
		assertEquals(siaPath.remotePath, Paths.get(path));
		assertEquals(siaPath.getLocalFile(), Paths.get(Utils.getSyncDir().toString(), path).toFile());

	}

	@Test
	public void testIntegerNameWithoutTimestamp() {

		final String user = "testuser";
		final String path = "1234567890";
		final Path prefix = Paths.get(user, "Goobox");
		final Path inPath = Paths.get(user, "Goobox", path);
		final SiaPath siaPath = new SiaPath(inPath.toString(), prefix);

		assertEquals(siaPath.siaPath, inPath);
		assertEquals(siaPath.created, 0);
		assertEquals(siaPath.localPath, Paths.get(Utils.getSyncDir().toString(), path));
		assertEquals(siaPath.remotePath, Paths.get(path));
		assertEquals(siaPath.getLocalFile(), Paths.get(Utils.getSyncDir().toString(), path).toFile());

	}

}
