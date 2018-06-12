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

import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.api.model.InlineResponse20010Downloads;
import mockit.Deencapsulation;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SiaFileFromDownloadsAPITest {

    private Path tmpDir;
    private String user;
    private Context ctx;

    @Before
    public void setUp() throws IOException {

        this.tmpDir = Files.createTempDirectory(null);

        this.user = "test-user";
        final Config cfg = new Config(this.tmpDir.resolve(App.ConfigFileName));
        Deencapsulation.setField(cfg, "userName", this.user);
        Deencapsulation.setField(cfg, "syncDir", this.tmpDir.toAbsolutePath());
        this.ctx = new Context(cfg);

    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(this.tmpDir.toFile());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void test() {

        final String name = Paths.get("foo", "bar.txt").toString();
        final Long created = new Date().getTime();
        final Path remotePath = Paths.get(user, "Goobox", name, String.valueOf(created));

        final long fileSize = 12345;
        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(remotePath.toString());
        file.setLength(fileSize);
        file.setCompleted(true);
        final SiaFileFromDownloadsAPI siaFile = new SiaFileFromDownloadsAPI(ctx, file);

        assertEquals(name, siaFile.getName());
        assertEquals(remotePath, siaFile.getCloudPath());
        assertEquals(this.tmpDir.resolve(name), siaFile.getLocalPath());

        assertEquals(created, siaFile.getCreationTime().get());
        assertEquals(fileSize, siaFile.getFileSize());
        assertTrue(siaFile.isCompleted());

    }
}
