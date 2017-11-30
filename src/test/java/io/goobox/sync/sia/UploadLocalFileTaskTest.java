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

import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.storj.Utils;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.FileUtils;
import org.dizitart.no2.objects.ObjectRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class UploadLocalFileTaskTest {

    @SuppressWarnings("unused")
    @Mocked
    private RenterApi api;

    private Path tempDir;
    private Config config;
    private Context context;

    @Before
    public void setUpMockDB() {
        new DBMock();
    }

    @After
    public void cleanUp() {
        DB.close();
    }

    /**
     * Creates a temporal directory and sets it as the result of CmdUtils.syncDir().
     *
     * @throws IOException if failed to create a temporary directory.
     */
    @Before
    public void setUpTempSyncDir() throws IOException {

        tempDir = Files.createTempDirectory(null);
        UtilsMock.syncDir = tempDir;
        new UtilsMock();

    }

    /**
     * Deletes the temporary directory.
     *
     * @throws IOException if failed to delete it.
     */
    @After
    public void tearDownTempSyncDir() throws IOException {

        if (tempDir != null && tempDir.toFile().exists()) {
            FileUtils.deleteDirectory(tempDir.toFile());
        }

    }

    @Before
    public void setUp() {

        this.config = new Config();
        this.config.setUserName("testuser");
        this.config.setDataPieces(120);
        this.config.setParityPieces(50);
        this.context = new Context(this.config, null);

    }

    @Test
    public void testUploadFile() throws IOException, ApiException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        final Path localPath = Utils.getSyncDir().resolve("testfile");
        final Path remotePath = this.context.pathPrefix.resolve("testfile");
        this.addForUpload(localPath);
        assertTrue(localPath.toFile().setLastModified(new Date().getTime()));

        new Expectations() {{
            final Path siaPath = remotePath.resolve(String.valueOf(localPath.toFile().lastModified()));
            api.renterUploadSiapathPost(siaPath.toString(), config.getDataPieces(), config.getParityPieces(), localPath.toString());
        }};

        // Use a local path instead of a SiaFile instance.
        new UploadLocalFileTask(this.context, localPath).run();
        assertEquals(SyncState.UPLOADING, DB.get(localPath).getState());
        assertTrue(DBMock.committed);

    }

    @Test
    public void testFailedToUpload() throws ApiException, IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        final Path localPath = Utils.getSyncDir().resolve("testfile");
        final Path remotePath = this.context.pathPrefix.resolve("testfile");
        this.addForUpload(localPath);
        assertTrue(localPath.toFile().setLastModified(new Date().getTime()));

        new Expectations() {{
            final Path siaPath = remotePath.resolve(String.valueOf(localPath.toFile().lastModified()));
            api.renterUploadSiapathPost(siaPath.toString(), config.getDataPieces(), config.getParityPieces(), localPath.toString());
            result = new ApiException();
        }};

        new UploadLocalFileTask(this.context, localPath).run();
        assertEquals(SyncState.UPLOAD_FAILED, DB.get(localPath).getState());
        assertTrue(DBMock.committed);

    }

    /**
     * Test a case that a file is modified while it is waiting to start uploading.
     * In this case, upload should be canceled and delegate CheckStateTask to decide uploading the new file or not.
     */
    @Test
    public void testToBeUploadedFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException, ApiException {

        final Path localPath = Utils.getSyncDir().resolve("testfile");
        final Path remotePath = this.context.pathPrefix.resolve("testfile");
        this.addForUpload(localPath);
        assertTrue(localPath.toFile().setLastModified(new Date().getTime()));

        // Expecting the api won't be called.
        new Expectations() {{
            final Path siaPath = remotePath.resolve(String.valueOf(localPath.toFile().lastModified()));
            api.renterUploadSiapathPost(siaPath.toString(), config.getDataPieces(), config.getParityPieces(), localPath.toString());
            times = 0;
        }};

        // Create a upload local file task.
        final UploadLocalFileTask task = new UploadLocalFileTask(this.context, localPath);

        // then, the target file is modified.
        DB.setModified(localPath);

        // and, the task is executed.
        task.run();

        assertEquals(SyncState.MODIFIED, DB.get(localPath).getState());
        assertFalse(DBMock.committed);

    }

    /**
     * Test a cast that a file is deleted while it is waiting to start uploading.
     * In this case, upload must be canceled.
     */
    @Test
    public void testToBeUploadedFileDeleted() throws NoSuchMethodException, IOException, IllegalAccessException, InvocationTargetException, ApiException {

        final Path localPath = Utils.getSyncDir().resolve("testfile");
        final Path remotePath = this.context.pathPrefix.resolve("testfile");
        this.addForUpload(localPath);
        assertTrue(localPath.toFile().setLastModified(new Date().getTime()));

        // Expecting the api won't be called.
        new Expectations() {{
            final Path siaPath = remotePath.resolve(String.valueOf(localPath.toFile().lastModified()));
            api.renterUploadSiapathPost(siaPath.toString(), config.getDataPieces(), config.getParityPieces(), localPath.toString());
            times = 0;
        }};

        // Create a upload local file task.
        final UploadLocalFileTask task = new UploadLocalFileTask(this.context, localPath);

        // then, the target file is modified.
        DB.setDeleted(localPath);

        // and, the task is executed.
        task.run();

        assertEquals(SyncState.DELETED, DB.get(localPath).getState());
        assertFalse(DBMock.committed);

    }

    private void addForUpload(final Path localPath) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.setState(localPath, SyncState.FOR_UPLOAD);
    }

    @SuppressWarnings("unchecked")
    private void setState(final Path localPath, final SyncState state) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        if (!localPath.toFile().exists()) {
            assertTrue(localPath.toFile().createNewFile());
        }
        DB.addNewFoundFile(localPath);
        final SyncFile syncFile = DB.get(localPath);
        Deencapsulation.setField(syncFile, "state", state);

        final Method repo = DB.class.getDeclaredMethod("repo");
        repo.setAccessible(true);
        final ObjectRepository<SyncFile> repository = (ObjectRepository<SyncFile>) repo.invoke(DB.class);
        repository.update(syncFile);

    }


}