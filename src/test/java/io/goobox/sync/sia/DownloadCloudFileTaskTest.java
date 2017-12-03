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
import io.goobox.sync.sia.db.CloudFile;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.UtilsMock;
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
public class DownloadCloudFileTaskTest {

    @SuppressWarnings("unused")
    @Mocked
    private RenterApi api;

    private Path tempDir;

    private Context context;
    private String name;
    private Path remotePath;
    private Path localPath;

    @Before
    public void setUp() throws IOException {

        new DBMock();

        tempDir = Files.createTempDirectory(null);
        UtilsMock.syncDir = tempDir;
        new UtilsMock();

        final Config cfg = new Config();
        cfg.setUserName("test-user");
        this.context = new Context(cfg, null);

        this.name = String.format("test-file-%x", System.currentTimeMillis());
        this.remotePath = this.context.pathPrefix.resolve(this.name).toAbsolutePath();
        this.localPath = Utils.getSyncDir().resolve(this.name);

        DB.addForDownload(new CloudFile() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Path getCloudPath() {
                return remotePath;
            }

            @Override
            public long getFileSize() {
                return System.currentTimeMillis();
            }
        }, this.localPath);

    }

    @After
    public void tearDown() throws IOException {

        DB.close();
        if (tempDir != null && tempDir.toFile().exists()) {
            FileUtils.deleteDirectory(tempDir.toFile());
        }

    }

    /**
     * Test it starts downloading a given file from a given URL to a given temporary path.
     *
     * @throws ApiException if API calls return an error.
     */
    @Test
    public void downloadFile() throws ApiException {

        new Expectations() {{
            //noinspection ConstantConditions
            api.renterDownloadasyncSiapathGet(remotePath.toString(), DB.get(name).getTemporaryPath().get().toString());
        }};
        new DownloadCloudFileTask(this.context, this.name).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.DOWNLOADING, DB.get(this.name).getState());

    }

    /**
     * If a not exising file name is given, it should be ignored.
     *
     * @throws ApiException if API calls return an error.
     */
    @Test
    public void downloadNotExistingFile() throws ApiException {

        new Expectations() {{
            //noinspection ConstantConditions
            api.renterDownloadasyncSiapathGet(remotePath.toString(), DB.get(name).getTemporaryPath().get().toString());
            times = 0;
        }};
        new DownloadCloudFileTask(this.context, "not-existing-name").run();
        assertFalse(DBMock.committed);

    }

    /**
     * Test if API calls return errors, it sets the status of a given file to DOWNLOAD_FAILED.
     *
     * @throws ApiException if API calls return errors.
     */
    @Test
    public void handleApiException() throws ApiException {

        new Expectations() {{
            //noinspection ConstantConditions
            api.renterDownloadasyncSiapathGet(remotePath.toString(), DB.get(name).getTemporaryPath().get().toString());
            result = new ApiException("expected exception");
        }};
        new DownloadCloudFileTask(this.context, this.name).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.DOWNLOAD_FAILED, DB.get(this.name).getState());

    }

    /**
     * Test a case that a file to be downloaded is also modified. In this case, the download has to be canceled.
     * CheckStateTask will check this file is uploaded, downloaded, or synced.
     */
    @Test
    public void toBeDownloadedFileModified() throws ApiException, IOException {

        // Expecting the api won't be called.
        new Expectations() {{
            //noinspection ConstantConditions
            api.renterDownloadasyncSiapathGet(remotePath.toString(), DB.get(name).getTemporaryPath().toString());
            times = 0;
        }};

        // A download remote file task is created (enqueued).
        final DownloadCloudFileTask task = new DownloadCloudFileTask(this.context, this.name);

        // The same file is created/modified.
        assertTrue(this.localPath.toFile().createNewFile());
        DB.setModified(this.localPath);

        // then, the task is executed.
        task.run();

        assertFalse(DBMock.committed);
        assertEquals(SyncState.MODIFIED, DB.get(this.name).getState());

    }

}