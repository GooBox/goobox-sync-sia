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
import io.goobox.sync.sia.db.CloudFile;
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
public class DownloadCloudFileTaskTest {

    @SuppressWarnings("unused")
    @Mocked
    private RenterApi api;

    private Path tmpDir;

    private Context ctx;
    private String name;
    private Path remotePath;
    private Path localPath;

    @Before
    public void setUp() throws IOException {

        new DBMock();
        this.tmpDir = Files.createTempDirectory(null);

        final Config cfg = new Config();
        Deencapsulation.setField(cfg, "userName", "test-user");
        Deencapsulation.setField(cfg, "syncDir", this.tmpDir.toAbsolutePath());
        this.ctx = new Context(cfg, null);

        this.name = String.format("test-file-%x", System.currentTimeMillis());
        this.remotePath = this.ctx.pathPrefix.resolve(this.name);
        this.localPath = this.tmpDir.resolve(this.name);

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

        APIUtilsMock.toSlashPaths.clear();
        new APIUtilsMock();

    }

    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(this.tmpDir.toFile());
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
            api.renterDownloadasyncSiapathGet(toSlash(remotePath), toSlash(DB.get(name).get().getTemporaryPath().get()));
        }};
        new DownloadCloudFileTask(this.ctx, this.name).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.DOWNLOADING, DB.get(this.name).get().getState());

        assertEquals(2, APIUtilsMock.toSlashPaths.size());
        assertEquals(this.remotePath, APIUtilsMock.toSlashPaths.get(0));
        assertEquals(DB.get(this.name).get().getTemporaryPath().get(), APIUtilsMock.toSlashPaths.get(1));

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
            api.renterDownloadasyncSiapathGet(toSlash(remotePath), toSlash(DB.get(name).get().getTemporaryPath().get()));
            times = 0;
        }};
        new DownloadCloudFileTask(this.ctx, "not-existing-name").call();
        assertFalse(DBMock.committed);

        assertEquals(0, APIUtilsMock.toSlashPaths.size());

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
            api.renterDownloadasyncSiapathGet(toSlash(remotePath), toSlash(DB.get(name).get().getTemporaryPath().get()));
            result = new ApiException("expected exception");
        }};
        new DownloadCloudFileTask(this.ctx, this.name).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.DOWNLOAD_FAILED, DB.get(this.name).get().getState());

        assertEquals(2, APIUtilsMock.toSlashPaths.size());
        assertEquals(this.remotePath, APIUtilsMock.toSlashPaths.get(0));
        assertEquals(DB.get(this.name).get().getTemporaryPath().get(), APIUtilsMock.toSlashPaths.get(1));

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
            api.renterDownloadasyncSiapathGet(toSlash(remotePath), toSlash(DB.get(name).get().getTemporaryPath().get()));
            times = 0;
        }};

        // A download remote file task is created (enqueued).
        final DownloadCloudFileTask task = new DownloadCloudFileTask(this.ctx, this.name);

        // The same file is created/modified.
        assertTrue(this.localPath.toFile().createNewFile());
        DB.setModified(this.name, this.localPath);

        // then, the task is executed.
        task.call();

        // assertFalse(DBMock.committed);
        assertEquals(SyncState.MODIFIED, DB.get(this.name).get().getState());

        assertEquals(0, APIUtilsMock.toSlashPaths.size());

    }

    private String toSlash(final Path path) {
        return path.toString().replace("\\", "/");
    }

}