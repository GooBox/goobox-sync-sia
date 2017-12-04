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
package io.goobox.sync.sia.model;

import io.goobox.sync.common.Utils;
import io.goobox.sync.sia.client.api.model.InlineResponse20010Downloads;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class SiaFileFromDownloadsAPITest {

    @SuppressWarnings("ConstantConditions")
    @Test
    public void test() {

        final String user = "testuser";
        final String name = "foo/bar.txt";
        final Long created = new Date().getTime();
        final Path prefix = Paths.get(user, "Goobox");
        final Path remotePath = Paths.get(user, "Goobox", name, String.valueOf(created));

        final long fileSize = 12345;
        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(remotePath.toString());
        file.setFilesize(fileSize);
        final SiaFile siaFile = new SiaFileFromDownloadsAPI(file, prefix);

        assertEquals(name, siaFile.getName());
        assertEquals(remotePath, siaFile.getCloudPath());
        assertEquals(Paths.get(Utils.getSyncDir().toString(), name), siaFile.getLocalPath());

        assertEquals(created, siaFile.getCreationTime().get());
        assertEquals(fileSize, siaFile.getFileSize());

    }
}
