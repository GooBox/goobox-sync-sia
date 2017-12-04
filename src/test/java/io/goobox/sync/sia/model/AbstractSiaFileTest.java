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
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AbstractSiaFileTest {

    private String user;
    private String path;
    private Long created;
    private Path prefix;

    @Before
    public void setUp() {
        this.user = "testuser";
        this.path = "foo/bar.txt";
        this.created = System.currentTimeMillis();
        this.prefix = Paths.get(user, "Goobox");
    }


    @SuppressWarnings("ConstantConditions")
    @Test
    public void cloudPathWithCreationTime() {

        final Path inPath = Paths.get(user, "Goobox", path, String.valueOf(created));
        final SiaFile siaFile = new AbstractSiaFile(inPath.toString(), prefix) {
            @Override
            public long getFileSize() {
                return 0;
            }
        };

        assertEquals(path, siaFile.getName());
        assertEquals(inPath, siaFile.getCloudPath());
        assertEquals(Utils.getSyncDir().resolve(this.path), siaFile.getLocalPath());
        assertEquals(created, siaFile.getCreationTime().get());

    }

    @Test
    public void couldPathWithoutCreationTime() {

        final Path inPath = Paths.get(user, "Goobox", path);
        final SiaFile siaFile = new AbstractSiaFile(inPath.toString(), prefix) {
            @Override
            public long getFileSize() {
                return 0;
            }
        };

        assertEquals(path, siaFile.getName());
        assertEquals(inPath, siaFile.getCloudPath());
        assertEquals(Utils.getSyncDir().resolve(this.path), siaFile.getLocalPath());
        assertFalse(siaFile.getCreationTime().isPresent());

    }

    @Test
    public void integerNameWithoutTimestamp() {

        final String path = "1234567890";
        final Path inPath = Paths.get(user, "Goobox", path);
        final SiaFile siaFile = new AbstractSiaFile(inPath.toString(), prefix) {
            @Override
            public long getFileSize() {
                return 0;
            }
        };

        assertEquals(path, siaFile.getName());
        assertEquals(inPath, siaFile.getCloudPath());
        assertEquals(Utils.getSyncDir().resolve(path), siaFile.getLocalPath());
        assertFalse(siaFile.getCreationTime().isPresent());

    }

}