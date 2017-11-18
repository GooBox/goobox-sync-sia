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

import io.goobox.sync.sia.client.api.model.InlineResponse20010Downloads;
import io.goobox.sync.storj.Utils;

public class SiaFileFromDownloadsAPITest {

	@Test
	public void test() {

		final String user = "testuser";
		final String name = "foo/bar.txt";
		final long created = new Date().getTime();
		final Path prefix = Paths.get(user, "Goobox");
		final Path remotePath = Paths.get(user, "Goobox", name, String.valueOf(created));

		final int filesize = 12345;
		final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
		file.setSiapath(remotePath.toString());
		file.setFilesize(filesize);
		final SiaFile siaFile = new SiaFileFromDownloadsAPI(file, prefix);

		assertEquals(siaFile.getName(), name);
		assertEquals(siaFile.getRemotePath(), remotePath);
		assertEquals(siaFile.getLocalPath(), Paths.get(Utils.getSyncDir().toString(), name));
		assertEquals(siaFile.getSiaPath(), new SiaPath(remotePath.toString(), prefix));

		assertEquals(siaFile.getCreationTime(), created);
		assertEquals(siaFile.getFileSize(), filesize);

	}
}
