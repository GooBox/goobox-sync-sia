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

package io.goobox.sync.sia.task;

import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.db.CloudFile;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class DeleteLocalFileTaskTest {

    private Path tmpDir;
    private Context ctx;
    private String name;
    private Path localPath;

    @Before
    public void setUp() throws IOException {

        new DBMock();
        this.tmpDir = Files.createTempDirectory(null);

        final Config cfg = new Config();
        Deencapsulation.setField(cfg, "syncDir", this.tmpDir.toAbsolutePath());
        this.ctx = new Context(cfg, null);

        this.name = String.format("file-%x", System.currentTimeMillis());
        this.localPath = this.tmpDir.resolve(this.name);
        assertTrue(this.localPath.toFile().createNewFile());
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
        }, this.localPath);
        DB.setForLocalDelete(this.name);

    }

    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(this.tmpDir.toFile());
    }

    @Test
    public void deleteExistingFile() {

        new DeleteLocalFileTask(this.ctx, this.localPath).run();
        assertTrue(DBMock.committed);
        assertFalse(DB.get(this.name).isPresent());
        assertFalse(this.localPath.toFile().exists());

    }

    @Test
    public void deleteNotExistingFile() {

        // Delete the target file in advance.
        assertTrue(this.localPath.toFile().delete());
        assertFalse(this.localPath.toFile().exists());

        new DeleteLocalFileTask(this.ctx, this.localPath).run();
        assertTrue(DBMock.committed);
        assertFalse(DB.get(this.name).isPresent());

    }

    /**
     * A file once marked to be deleted from local directory but it is modified later.
     * In this case, this file must not be deleted and pass to CheckStateTask.
     */
    @SuppressWarnings("ConstantConditions")
    @Test
    public void toBeDeletedFileModified() throws IOException {

        // Task is enqueued.
        final Runnable task = new DeleteLocalFileTask(this.ctx, this.localPath);

        // then, the file is modified.
        DB.setModified(this.name, this.localPath);

        // and, the task is executed.
        task.run();

        // check after conditions.
        assertEquals(SyncState.MODIFIED, DB.get(this.name).get().getState());
        assertTrue(this.localPath.toFile().exists());

    }

}