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

import io.goobox.sync.sia.db.CloudFile;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.storj.Utils;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class DeleteLocalFileTaskTest {

    private Path tempDir;
    private Path localPath;

    @Before
    public void setUp() throws IOException {

        new DBMock();
        tempDir = Files.createTempDirectory(null);
        UtilsMock.syncDir = tempDir;
        new UtilsMock();

        final String name = String.format("file-%x", System.currentTimeMillis());
        localPath = Utils.getSyncDir().resolve(name);
        assertTrue(localPath.toFile().createNewFile());
        DB.setSynced(new CloudFile() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Path getCloudPath() {
                return null;
            }

            @Override
            public long getFileSize() {
                return 0;
            }
        }, localPath);
        DB.setForLocalDelete(localPath);

    }

    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    @Test
    public void deleteExistingFile() throws IOException {

        new DeleteLocalFileTask(localPath).run();
        assertTrue(DBMock.committed);
        assertFalse(DB.contains(localPath));
        assertFalse(localPath.toFile().exists());

    }

    @Test
    public void deleteNotExistingFile() throws IOException {

        // Delete the target file in advance.
        assertTrue(localPath.toFile().delete());
        assertFalse(localPath.toFile().exists());

        new DeleteLocalFileTask(localPath).run();
        assertTrue(DBMock.committed);
        assertFalse(DB.contains(localPath));

    }

    /**
     * A file once marked to be deleted from local directory but it is modified later.
     * In this case, this file must not be deleted and pass to CheckStateTask.
     */
    @Test
    public void toBeDeletedFileModified() throws IOException {

        // Task is enqueued.
        final Runnable task = new DeleteLocalFileTask(localPath);

        // then, the file is modified.
        DB.setModified(localPath);

        // and, the task is executed.
        task.run();

        // check after conditions.
        assertEquals(SyncState.MODIFIED, DB.get(localPath).getState());
        assertTrue(localPath.toFile().exists());

    }

}