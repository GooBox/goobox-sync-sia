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

package io.goobox.sync.sia.db;

import mockit.Deencapsulation;
import org.apache.commons.codec.digest.DigestUtils;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectRepository;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.dizitart.no2.objects.filters.ObjectFilters.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SyncFileTest {

    private String name;
    private Path cloudPath;
    private Long cloudSize;
    private Path localPath;
    private Long localModificationTime;
    private Long localSize;
    private String localDigest;
    private Path temporaryPath;
    private SyncState state;

    @Before
    public void setUp() {
        name = String.format("file-%x", System.currentTimeMillis());
        cloudPath = Paths.get(String.format("cloud-%x", System.currentTimeMillis()));
        cloudSize = System.currentTimeMillis();
        localPath = Paths.get(String.format("local-%x", System.currentTimeMillis())).toAbsolutePath();
        localModificationTime = System.currentTimeMillis();
        localSize = System.currentTimeMillis();
        localDigest = String.format("%x", System.currentTimeMillis());
        temporaryPath = Paths.get(String.format("temp-%x", System.currentTimeMillis())).toAbsolutePath();
        state = SyncState.SYNCED;
    }

    @Test
    public void serialize() throws IOException {

        final SyncFile syncFile = new SyncFile();
        syncFile.setName(name);
        syncFile.setCloudPath(cloudPath);
        Deencapsulation.setField(syncFile, "cloudSize", cloudSize);
        syncFile.setLocalPath(localPath);
        Deencapsulation.setField(syncFile, "localModificationTime", localModificationTime);
        Deencapsulation.setField(syncFile, "localSize", localSize);
        Deencapsulation.setField(syncFile, "localDigest", localDigest);
        syncFile.setTemporaryPath(temporaryPath);
        syncFile.setState(state);
        this.checkDeserializeFile(syncFile);

    }

    @Test
    public void serializeWithNull() throws IOException {

        final SyncFile syncFile = new SyncFile();
        syncFile.setName(name);
        syncFile.setState(state);

        this.checkDeserializeFile(syncFile);

    }

    private void checkDeserializeFile(final SyncFile syncFile) throws IOException {

        final File tmpFile = Files.createTempFile(null, null).toFile();
        try {

            // Open database and store a sync file.
            try (final Nitrite db = Nitrite.builder().compressed().filePath(tmpFile).openOrCreate()) {
                final ObjectRepository<SyncFile> repository = db.getRepository(SyncFile.class);
                repository.insert(syncFile);
            }

            // Reopen the database and retrieve the stored sync file.
            try (final Nitrite db = Nitrite.builder().compressed().filePath(tmpFile).openOrCreate()) {
                final ObjectRepository<SyncFile> repository = db.getRepository(SyncFile.class);
                final SyncFile res = repository.find(eq("name", syncFile.getName())).firstOrDefault();
                assertEquals(syncFile, res);
            }

        } finally {
            assertTrue(tmpFile.delete());
        }

    }

    @Test
    public void getCloudCreationTime() {

        final Long now = System.currentTimeMillis();
        final Path cloudPath = Paths.get("cloud", String.valueOf(now));
        final SyncFile syncFile = new SyncFile();
        Deencapsulation.setField(syncFile, "cloudPath", cloudPath.toString());
        assertEquals(now, syncFile.getCloudCreationTime());

    }

    @Test
    public void getCloudCreationTimeWithNullCloudPath() {

        final SyncFile syncFile = new SyncFile();
        Deencapsulation.setField(syncFile, "cloudPath", cloudPath.toString());
        assertEquals(null, syncFile.getCloudCreationTime());

    }

    @Test
    public void getCloudCreationTimeWithInvalidCreationTime() {

        final Path cloudPath = Paths.get("cloud", "123456X");
        final SyncFile syncFile = new SyncFile();
        Deencapsulation.setField(syncFile, "cloudPath", cloudPath.toString());
        assertEquals(null, syncFile.getCloudCreationTime());

    }

    @Test
    public void setCloudData() {

        final Long now = System.currentTimeMillis();
        final Path cloudPath = Paths.get("cloud", String.valueOf(now)).toAbsolutePath();

        final SyncFile syncFile = new SyncFile();
        syncFile.setCloudData(new CloudFile() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Path getCloudPath() {
                return cloudPath;
            }

            @Override
            public long getFileSize() {
                return cloudSize;
            }
        });

        assertEquals(cloudPath, syncFile.getCloudPath());
        assertEquals(now, syncFile.getCloudCreationTime());
        assertEquals(cloudSize, syncFile.getCloudSize());

    }

    @Test
    public void setLocalData() throws IOException {

        final Path localPath = Files.createTempFile(null, null);
        try {

            Files.write(localPath, name.getBytes());

            final SyncFile syncFile = new SyncFile();
            syncFile.setLocalData(localPath);

            assertEquals(localPath, syncFile.getLocalPath());
            assertEquals((Long) Files.getLastModifiedTime(localPath).toMillis(), syncFile.getLocalModificationTime());
            assertEquals((Long) Files.size(localPath), syncFile.getLocalSize());
            final String digest = DigestUtils.sha512Hex(new FileInputStream(localPath.toFile()));
            assertEquals(digest, syncFile.getLocalDigest());

        } finally {
            Files.deleteIfExists(localPath);
        }

    }

}