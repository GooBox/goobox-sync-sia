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
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.SiaFileMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.storj.Utils;
import io.goobox.sync.storj.db.SyncState;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

@RunWith(JMockit.class)
public class DownloadRemoteFileTaskTest {

    @Mocked
    private RenterApi api;

    private Path tempDir;

    @Before
    public void setUpMockDB() {
        new DBMock();
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

    @Test
    public void testDownloadFile() throws IOException, ApiException {

        final Config cfg = new Config();
        cfg.userName = "testuser";
        final Context ctx = new Context(cfg, null);

        final Path remotePath = ctx.pathPrefix.resolve("testfile");
        final Path localPath = Utils.getSyncDir().resolve("testfile");
        localPath.toFile().delete();

        final SiaFileMock file = new SiaFileMock(localPath);
        file.setRemotePath(remotePath);

        new Expectations() {{
            api.renterDownloadasyncSiapathGet(remotePath.toString(), file.getLocalPath().toString());
        }};

        new DownloadRemoteFileTask(ctx, file).run();

    }

    /**
     * This test checks DownloadRemoteFileTask creates not existing parent directories.
     *
     * @throws IOException if failed file handlings
     * @throws ApiException if API call raises some error
     */
    @Test
    public void testDownloadToNotExistingDir() throws IOException, ApiException {

        final Config cfg = new Config();
        cfg.userName = "testuser";
        final Context ctx = new Context(cfg, null);

        final Path remotePath = ctx.pathPrefix.resolve("subdir/testfile");

        final Path parent = Files.createTempDirectory(null);
        final Path localPath = Utils.getSyncDir().resolve("subdir/testfile");
        assertFalse(localPath.getParent().toFile().exists());

        final SiaFileMock file = new SiaFileMock(localPath);
        file.setRemotePath(remotePath);

        new Expectations() {{
            api.renterDownloadasyncSiapathGet(remotePath.toString(), localPath.toString());
        }};

        new DownloadRemoteFileTask(ctx, file).run();
        assertTrue(localPath.getParent().toFile().exists());

    }

    @Test
    public void testHandlingOfApiException() throws IOException, ApiException {

        final Config cfg = new Config();
        cfg.userName = "testuser";
        final Context ctx = new Context(cfg, null);

        final Path remotePath = ctx.pathPrefix.resolve("testfile");
        final Path localPath = Utils.getSyncDir().resolve("testfile");

        final SiaFileMock file = new SiaFileMock(localPath);
        file.setRemotePath(remotePath);
        DB.addForDownload(file);

        new Expectations() {{
            api.renterDownloadasyncSiapathGet(remotePath.toString(), localPath.toString());
            result = new ApiException("expected exception");
        }};

        new DownloadRemoteFileTask(ctx, file).run();
        assertEquals(DB.get(file).getState(), SyncState.DOWNLOAD_FAILED);

    }

    @Test
    public void testHandlingOfIOException(@Mocked Files files) throws IOException {

        final Config cfg = new Config();
        cfg.userName = "testuser";
        final Context ctx = new Context(cfg, null);

        final Path remotePath = ctx.pathPrefix.resolve("subdir/testfile");
        final Path localPath = Utils.getSyncDir().resolve("subdir/testfile");
        assertFalse(localPath.getParent().toFile().exists());

        final SiaFileMock file = new SiaFileMock(localPath);
        file.setRemotePath(remotePath);
        DB.addForDownload(file);

        new Expectations() {{
            Files.createDirectories(localPath.getParent());
            result = new IOException("expected exception");
        }};

        new DownloadRemoteFileTask(ctx, file).run();
        assertEquals(DB.get(file).getState(), SyncState.DOWNLOAD_FAILED);

    }

}