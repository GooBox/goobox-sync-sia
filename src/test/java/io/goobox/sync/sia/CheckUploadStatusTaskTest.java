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
import io.goobox.sync.sia.client.api.model.InlineResponse20011;
import io.goobox.sync.sia.client.api.model.InlineResponse20011Files;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.sia.model.SiaFileFromFilesAPI;
import io.goobox.sync.storj.Utils;
import io.goobox.sync.storj.db.SyncState;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CheckUploadStatusTaskTest {

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
    public void test() throws ApiException, IOException {

        final Config cfg = new Config();
        cfg.setUserName("testuser");
        final Context ctx = new Context(cfg, null);

        // Test files:
        // - file1: finished uploading
        // - file2: still uploading
        // - file3: uploaded but already synced
        final List<InlineResponse20011Files> files = new ArrayList<>();

        final Path remotePath1 = ctx.pathPrefix.resolve("file1");
        final Path localPath1 = Utils.getSyncDir().resolve("file1");
        localPath1.toFile().createNewFile();
        final InlineResponse20011Files file1 = new InlineResponse20011Files();
        file1.setSiapath(remotePath1.toString());
        file1.setLocalpath(localPath1.toString());
        file1.setFilesize(1234L);
        file1.setUploadprogress(new BigDecimal(100));
        DB.addForUpload(new SiaFileFromFilesAPI(file1, ctx.pathPrefix));
        files.add(file1);

        final Path remotePath2 = ctx.pathPrefix.resolve("file2");
        final Path localPath2 = Utils.getSyncDir().resolve("file2");
        localPath2.toFile().createNewFile();
        final InlineResponse20011Files file2 = new InlineResponse20011Files();
        file2.setSiapath(remotePath2.toString());
        file2.setLocalpath(localPath2.toString());
        file2.setFilesize(1234L);
        file2.setUploadprogress(new BigDecimal(95.2));
        DB.addForUpload(new SiaFileFromFilesAPI(file2, ctx.pathPrefix));
        files.add(file2);

        final Path remotePath3 = ctx.pathPrefix.resolve("file3");
        final Path localPath3 = Utils.getSyncDir().resolve("file3");
        localPath3.toFile().createNewFile();
        final InlineResponse20011Files file3 = new InlineResponse20011Files();
        file3.setSiapath(remotePath3.toString());
        file3.setLocalpath(localPath3.toString());
        file3.setFilesize(1234L);
        file3.setUploadprogress(new BigDecimal(100));
        DB.setSynced(new SiaFileFromFilesAPI(file3, ctx.pathPrefix));
        files.add(file3);

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        new CheckUploadStatusTask(ctx).run();
        assertEquals(SyncState.SYNCED, DB.get(localPath1).getState());
        assertEquals(SyncState.FOR_UPLOAD, DB.get(localPath2).getState());
        assertEquals(SyncState.SYNCED, DB.get(localPath3).getState());
        assertTrue(DBMock.committed);

    }

}