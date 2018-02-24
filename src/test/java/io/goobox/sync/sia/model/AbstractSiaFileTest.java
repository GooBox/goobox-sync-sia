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
import io.goobox.sync.sia.mocks.APIUtilsMock;
import mockit.Deencapsulation;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(JMockit.class)
public class AbstractSiaFileTest {

    private Path tmpDir;
    private String user;
    private String path;
    private Long created;
    private Context ctx;

    @Before
    public void setUp() throws IOException {
        this.tmpDir = Files.createTempDirectory(null);

        this.user = "testuser";
        this.path = Paths.get("foo", "bar.txt").toString();
        this.created = System.currentTimeMillis();

        final Config cfg = new Config(this.tmpDir.resolve(App.ConfigFileName));
        Deencapsulation.setField(cfg, "userName", this.user);
        Deencapsulation.setField(cfg, "syncDir", this.tmpDir.toAbsolutePath());
        this.ctx = new Context(cfg);

        APIUtilsMock.fromSlashPaths.clear();
        new APIUtilsMock();
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(this.tmpDir.toFile());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void cloudPathWithCreationTime() {

        final Path inPath = Paths.get(this.user, "Goobox", this.path, String.valueOf(this.created));
        final SiaFile siaFile = new AbstractSiaFile(this.ctx, inPath.toString()) {
            @Override
            public long getFileSize() {
                return 0;
            }
        };

        assertEquals(this.path, siaFile.getName());
        assertEquals(inPath, siaFile.getCloudPath());
        assertEquals(this.tmpDir.resolve(this.path), siaFile.getLocalPath());
        assertEquals(this.created, siaFile.getCreationTime().get());

        assertEquals(1, APIUtilsMock.fromSlashPaths.size());
        assertEquals(inPath.toString(), APIUtilsMock.fromSlashPaths.get(0));

    }

    @Test
    public void couldPathWithoutCreationTime() {

        final Path inPath = Paths.get(this.user, "Goobox", this.path);
        final SiaFile siaFile = new AbstractSiaFile(this.ctx, inPath.toString()) {
            @Override
            public long getFileSize() {
                return 0;
            }
        };

        assertEquals(this.path, siaFile.getName());
        assertEquals(inPath, siaFile.getCloudPath());
        assertEquals(this.tmpDir.resolve(this.path), siaFile.getLocalPath());
        assertFalse(siaFile.getCreationTime().isPresent());

        assertEquals(1, APIUtilsMock.fromSlashPaths.size());
        assertEquals(inPath.toString(), APIUtilsMock.fromSlashPaths.get(0));

    }

    @Test
    public void integerNameWithoutTimestamp() {

        final String path = "1234567890";
        final Path inPath = Paths.get(this.user, "Goobox", path);
        final SiaFile siaFile = new AbstractSiaFile(this.ctx, inPath.toString()) {
            @Override
            public long getFileSize() {
                return 0;
            }
        };

        assertEquals(path, siaFile.getName());
        assertEquals(inPath, siaFile.getCloudPath());
        assertEquals(this.tmpDir.resolve(path), siaFile.getLocalPath());
        assertFalse(siaFile.getCreationTime().isPresent());

        assertEquals(1, APIUtilsMock.fromSlashPaths.size());
        assertEquals(inPath.toString(), APIUtilsMock.fromSlashPaths.get(0));

    }

}