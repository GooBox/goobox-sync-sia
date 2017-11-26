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
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class UploadLocalFileTaskTest {

    @SuppressWarnings("unused")
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

    @Test
    public void testUploadFile() throws IOException, ApiException {

        final Config cfg = new Config();
        cfg.setUserName("testuser");
        cfg.setDataPieces(120);
        cfg.setParityPieces(50);
        final Context ctx = new Context(cfg, null);

        final Path localPath = Utils.getSyncDir().resolve("testfile");
        final Path remotePath = ctx.pathPrefix.resolve("testfile");
        assertTrue(localPath.toFile().createNewFile());
        DB.addForUpload(localPath);
        final Date now = new Date();

        new Expectations() {{
            final Path siaPath = remotePath.resolve(String.valueOf(now.getTime()));
            api.renterUploadSiapathPost(siaPath.toString(), cfg.getDataPieces(), cfg.getParityPieces(), localPath.toString());
        }};

        final SiaFileMock file = new SiaFileMock(localPath);
        file.setRemotePath(remotePath);
        new UploadLocalFileTask(ctx, file, now).run();
        assertEquals(SyncState.UPLOADING, DB.get(localPath).getState());
        assertTrue(DBMock.committed);

    }

    @Test
    public void testUploadFileWithLocalPath() throws IOException, ApiException {

        final Config cfg = new Config();
        cfg.setUserName("testuser");
        cfg.setDataPieces(120);
        cfg.setParityPieces(50);
        final Context ctx = new Context(cfg, null);

        final Path localPath = Utils.getSyncDir().resolve("testfile");
        final Path remotePath = ctx.pathPrefix.resolve("testfile");
        assertTrue(localPath.toFile().createNewFile());
        DB.addForUpload(localPath);
        final Date now = new Date();

        new Expectations() {{
            final Path siaPath = remotePath.resolve(String.valueOf(now.getTime()));
            api.renterUploadSiapathPost(siaPath.toString(), cfg.getDataPieces(), cfg.getParityPieces(), localPath.toString());
        }};

        // Use a local path instead of a SiaFile instance.
        new UploadLocalFileTask(ctx, localPath, now).run();
        assertEquals(SyncState.UPLOADING, DB.get(localPath).getState());
        assertTrue(DBMock.committed);

    }

    @Test
    public void testFailedToUpload() throws ApiException, IOException {

        final Config cfg = new Config();
        cfg.setUserName("testuser");
        cfg.setDataPieces(120);
        cfg.setParityPieces(50);
        final Context ctx = new Context(cfg, null);

        final Path localPath = Utils.getSyncDir().resolve("testfile");
        final Path remotePath = ctx.pathPrefix.resolve("testfile");
        assertTrue(localPath.toFile().createNewFile());
        DB.addForUpload(localPath);
        final Date now = new Date();

        new Expectations() {{
            final Path siaPath = remotePath.resolve(String.valueOf(now.getTime()));
            api.renterUploadSiapathPost(siaPath.toString(), cfg.getDataPieces(), cfg.getParityPieces(), localPath.toString());
            result = new ApiException();
        }};

        new UploadLocalFileTask(ctx, localPath, now).run();
        assertEquals(SyncState.UPLOAD_FAILED, DB.get(localPath).getState());
        assertTrue(DBMock.committed);

    }

//    @Test
//    public void testUploadToSubDirectory() throws IOException, ApiException {
//
//        final Config cfg = new Config();
//        cfg.userName = "testuser";
//        cfg.dataPieces = 120;
//        cfg.parityPieces = 50;
//        final Context ctx = new Context(cfg, null);
//
//        final Path localPath = CmdUtils.getSyncDir().resolve(Paths.get("subdir","testfile"));
//        final Path remotePath = ctx.pathPrefix.resolve(Paths.get("subdir","testfile"));
//        localPath.toFile().createNewFile();
//        DB.addForUpload(localPath);
//
//        new Expectations() {{
//            api.renterUploadSiapathPost(remotePath.toString(), cfg.dataPieces, cfg.parityPieces, localPath.toString());
//        }};
//
//        final SiaFileMock file = new SiaFileMock(localPath);
//        file.setRemotePath(remotePath);
//        new UploadLocalFileTask(ctx, file).run();
//
//    }


}