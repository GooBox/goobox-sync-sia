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
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.APIUtilsMock;
import io.goobox.sync.sia.mocks.DBMock;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
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

@SuppressWarnings("ConstantConditions")
@RunWith(JMockit.class)
public class UploadLocalFileTaskTest {

    @SuppressWarnings("unused")
    @Mocked
    private RenterApi api;

    private Path tmpDir;
    private Config cfg;
    private Context context;
    private String name;
    private Path localPath;
    private Path cloudPath;

    /**
     * Creates a temporal directory and sets it as the result of CmdUtils.syncDir().
     *
     * @throws IOException if failed to create a temporary directory.
     */
    @Before
    public void setUp() throws IOException {

        new DBMock();
        this.tmpDir = Files.createTempDirectory(null);

        this.cfg = new Config();
        Deencapsulation.setField(this.cfg, "userName", "test-user");
        Deencapsulation.setField(this.cfg, "dataPieces", 120);
        Deencapsulation.setField(this.cfg, "parityPieces", 50);
        Deencapsulation.setField(this.cfg, "syncDir", this.tmpDir.toAbsolutePath());
        this.context = new Context(this.cfg, null);

        this.name = String.format("test-file-%x", System.currentTimeMillis());
        this.localPath = this.tmpDir.resolve(this.name);
        this.cloudPath = this.context.pathPrefix.resolve(this.name).resolve(String.valueOf(System.currentTimeMillis()));
        assertTrue(this.localPath.toFile().createNewFile());
        DB.addNewFile(this.name, this.localPath);
        DB.setForUpload(this.name, this.localPath, this.cloudPath);

        APIUtilsMock.toSlashPaths.clear();
        new APIUtilsMock();

    }

    /**
     * Deletes the temporary directory.
     *
     * @throws IOException if failed to delete it.
     */
    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(this.tmpDir.toFile());
    }

    @Test
    public void uploadFile() throws ApiException {

        new Expectations() {{
            api.renterUploadSiapathPost(toSlash(cloudPath), cfg.getDataPieces(), cfg.getParityPieces(), toSlash(localPath));
        }};
        new UploadLocalFileTask(this.context, this.localPath).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.UPLOADING, DB.get(this.name).get().getState());

        // check toSlash is used.
        assertEquals(2, APIUtilsMock.toSlashPaths.size());
        assertEquals(this.cloudPath, APIUtilsMock.toSlashPaths.get(0));
        assertEquals(this.localPath, APIUtilsMock.toSlashPaths.get(1));

    }

    @Test
    public void failedToUpload() throws ApiException {

        new Expectations() {{
            api.renterUploadSiapathPost(toSlash(cloudPath), cfg.getDataPieces(), cfg.getParityPieces(), toSlash(localPath));
            result = new ApiException();
        }};
        new UploadLocalFileTask(this.context, this.localPath).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.UPLOAD_FAILED, DB.get(this.name).get().getState());

        // check toSlash is used.
        assertEquals(2, APIUtilsMock.toSlashPaths.size());
        assertEquals(this.cloudPath, APIUtilsMock.toSlashPaths.get(0));
        assertEquals(this.localPath, APIUtilsMock.toSlashPaths.get(1));

    }

    /**
     * Test a case that a file is modified while it is waiting to start uploading.
     * In this case, upload should be canceled and delegate CheckStateTask to decide uploading the new file or not.
     */
    @Test
    public void toBeUploadedFileModified() throws IOException, ApiException {

        // Expecting the api won't be called.
        new Expectations() {{
            api.renterUploadSiapathPost(toSlash(cloudPath), cfg.getDataPieces(), cfg.getParityPieces(), toSlash(localPath));
            times = 0;
        }};

        // Create a upload local file task.
        final UploadLocalFileTask task = new UploadLocalFileTask(this.context, this.localPath);

        // then, the target file is modified.
        DB.setModified(this.name, this.localPath);

        // and, the task is executed.
        task.call();

        // check after conditions.
        assertEquals(SyncState.MODIFIED, DB.get(this.name).get().getState());

        // check toSlash is used.
        assertEquals(0, APIUtilsMock.toSlashPaths.size());

    }

    /**
     * Test a cast that a file is deleted while it is waiting to start uploading.
     * In this case, upload must be canceled.
     */
    @Test
    public void toBeUploadedFileDeleted() throws ApiException {

        // Expecting the api won't be called.
        new Expectations() {{
            api.renterUploadSiapathPost(toSlash(cloudPath), cfg.getDataPieces(), cfg.getParityPieces(), toSlash(localPath));
            times = 0;
        }};

        // Create a upload local file task.
        final UploadLocalFileTask task = new UploadLocalFileTask(this.context, this.localPath);

        // then, the target file is modified.
        DB.setDeleted(this.name);

        // and, the task is executed.
        task.call();

        // check after conditions.
        assertFalse(DBMock.committed);
        assertEquals(SyncState.DELETED, DB.get(this.name).get().getState());

        // check toSlash is used.
        assertEquals(0, APIUtilsMock.toSlashPaths.size());

    }

    private String toSlash(final Path path) {
        return path.toString().replace("\\", "/");
    }

}