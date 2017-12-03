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

import io.goobox.sync.common.Utils;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

    private Path tempDir;
    private Config config;
    private Context context;
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

        tempDir = Files.createTempDirectory(null);
        UtilsMock.syncDir = tempDir;
        new UtilsMock();

        this.config = new Config();
        this.config.setUserName("test-user");
        this.config.setDataPieces(120);
        this.config.setParityPieces(50);
        this.context = new Context(this.config, null);

        this.localPath = Utils.getSyncDir().resolve("test-file");
        this.cloudPath = this.context.pathPrefix.resolve("test-file").resolve(String.valueOf(System.currentTimeMillis())).toAbsolutePath();
        assertTrue(localPath.toFile().createNewFile());
        DB.addNewFile(localPath);
        DB.setForUpload(this.localPath, this.cloudPath);

    }

    /**
     * Deletes the temporary directory.
     *
     * @throws IOException if failed to delete it.
     */
    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    @Test
    public void uploadFile() throws IOException, ApiException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        new Expectations() {{
            api.renterUploadSiapathPost(cloudPath.toString(), config.getDataPieces(), config.getParityPieces(), localPath.toString());
        }};
        new UploadLocalFileTask(this.context, localPath).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.UPLOADING, DB.get(localPath).get().getState());

    }

    @Test
    public void failedToUpload() throws ApiException, IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        new Expectations() {{
            api.renterUploadSiapathPost(cloudPath.toString(), config.getDataPieces(), config.getParityPieces(), localPath.toString());
            result = new ApiException();
        }};
        new UploadLocalFileTask(this.context, localPath).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.UPLOAD_FAILED, DB.get(localPath).get().getState());

    }

    /**
     * Test a case that a file is modified while it is waiting to start uploading.
     * In this case, upload should be canceled and delegate CheckStateTask to decide uploading the new file or not.
     */
    @Test
    public void toBeUploadedFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException, ApiException {

        // Expecting the api won't be called.
        new Expectations() {{
            api.renterUploadSiapathPost(cloudPath.toString(), config.getDataPieces(), config.getParityPieces(), localPath.toString());
            times = 0;
        }};

        // Create a upload local file task.
        final UploadLocalFileTask task = new UploadLocalFileTask(this.context, localPath);

        // then, the target file is modified.
        DB.setModified(localPath);

        // and, the task is executed.
        task.run();

        // check after conditions.
        assertFalse(DBMock.committed);
        assertEquals(SyncState.MODIFIED, DB.get(localPath).get().getState());

    }

    /**
     * Test a cast that a file is deleted while it is waiting to start uploading.
     * In this case, upload must be canceled.
     */
    @Test
    public void toBeUploadedFileDeleted() throws NoSuchMethodException, IOException, IllegalAccessException, InvocationTargetException, ApiException {

        // Expecting the api won't be called.
        new Expectations() {{
            api.renterUploadSiapathPost(cloudPath.toString(), config.getDataPieces(), config.getParityPieces(), localPath.toString());
            times = 0;
        }};

        // Create a upload local file task.
        final UploadLocalFileTask task = new UploadLocalFileTask(this.context, localPath);

        // then, the target file is modified.
        DB.setDeleted(localPath);

        // and, the task is executed.
        task.run();

        // check after conditions.
        assertFalse(DBMock.committed);
        assertEquals(SyncState.DELETED, DB.get(localPath).get().getState());

    }

}