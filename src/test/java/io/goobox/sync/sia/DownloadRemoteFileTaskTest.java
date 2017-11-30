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
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.SiaFileMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.storj.Utils;
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

@RunWith(JMockit.class)
public class DownloadRemoteFileTaskTest {

    @SuppressWarnings("unused")
    @Mocked
    private RenterApi api;

    private Path tempDir;

    private Context context;
    private Path remotePath;
    private SiaFile file;

    @Before
    public void setUpMockDB() throws IOException {

        new DBMock();

        final Config cfg = new Config();
        cfg.setUserName("testuser");
        this.context = new Context(cfg, null);

        this.remotePath = this.context.pathPrefix.resolve("testfile");

        final Path localPath = Utils.getSyncDir().resolve("testfile");
        final SiaFileMock file = new SiaFileMock(localPath);
        file.setRemotePath(this.remotePath);
        this.file = file;

        DB.addForDownload(file);

    }

    @After
    public void cleanUp() {
        DB.close();
    }

    /**
     * Creates a temporal directory and sets it as the result of Utils.syncDir().
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

    /**
     * Test it starts downloading a given file from a given URL to a given temporary path.
     *
     * @throws ApiException if API calls return errors.
     */
    @Test
    public void testDownloadFile() throws ApiException {

        new Expectations() {{
            api.renterDownloadasyncSiapathGet(remotePath.toString(), DB.get(file).getTemporaryPath().toString());
        }};
        new DownloadRemoteFileTask(this.context, this.file).run();
        assertEquals(SyncState.DOWNLOADING, DB.get(this.file).getState());
        assertTrue(DBMock.committed);

    }

    /**
     * Test if API calls return errors, it sets the status of a given file to DOWNLOAD_FAILED.
     *
     * @throws ApiException if API calls return errors.
     */
    @Test
    public void testHandlingOfApiException() throws ApiException {

        new Expectations() {{
            api.renterDownloadasyncSiapathGet(remotePath.toString(), DB.get(file).getTemporaryPath().toString());
            result = new ApiException("expected exception");
        }};
        new DownloadRemoteFileTask(this.context, this.file).run();
        assertEquals(SyncState.DOWNLOAD_FAILED, DB.get(this.file).getState());
        assertTrue(DBMock.committed);

    }

    /**
     * Test a case that a file to be downloaded is also modified. In this case, the download has to be canceled.
     * CheckStateTask will check this file is uploaded, downloaded, or synced.
     */
    @Test
    public void testToBeDownloadedFileModified() throws ApiException, IOException {

        // Expecting the api won't be called.
        new Expectations() {{
            api.renterDownloadasyncSiapathGet(remotePath.toString(), DB.get(file).getTemporaryPath().toString());
            times = 0;
        }};

        // A download remote file task is created (enqueued).
        final DownloadRemoteFileTask task = new DownloadRemoteFileTask(this.context, this.file);

        // The same file is created/modified.
        assertTrue(this.file.getLocalPath().toFile().createNewFile());
        DB.setModified(this.file.getLocalPath());

        // then, the task is executed.
        task.run();

        assertEquals(SyncState.MODIFIED, DB.get(this.file).getState());
        assertFalse(DBMock.committed);

    }

}