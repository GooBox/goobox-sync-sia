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
import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.sia.model.SiaFileFromFilesAPI;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class DeleteRemoteFileTaskTest {

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
    public void test() throws IOException, ApiException {

        final Config cfg = new Config();
        cfg.setUserName("testuser");
        final Context ctx = new Context(cfg, null);

        final List<InlineResponse20011Files> files = new ArrayList<>();

        final Path localPath = Utils.getSyncDir().resolve("testfile");
        final Path remotePath = ctx.pathPrefix.resolve("testfile");

        final InlineResponse20011Files file1 = new InlineResponse20011Files();
        file1.setSiapath(remotePath.resolve(String.valueOf(new Date(10000).getTime())).toString());
        file1.setLocalpath(localPath.toString());
        file1.setAvailable(true);
        files.add(file1);

        final InlineResponse20011Files file2 = new InlineResponse20011Files();
        file2.setSiapath(remotePath.resolve(String.valueOf(new Date(20000).getTime())).toString());
        file2.setLocalpath(localPath.toString());
        file2.setAvailable(true);
        files.add(file2);

        final InlineResponse20011Files file3 = new InlineResponse20011Files();
        file3.setSiapath(remotePath.resolve(String.valueOf(new Date(30000).getTime())).toString());
        file3.setLocalpath(localPath.toString());
        file3.setAvailable(true);
        file3.setFilesize(1234L);
        files.add(file3);

        final SiaFile file = new SiaFileFromFilesAPI(file3, ctx.pathPrefix);
        assertTrue(localPath.toFile().createNewFile());
        DB.setSynced(file);
        assertTrue(localPath.toFile().delete());


        new Expectations() {{
            final InlineResponse20011 list = new InlineResponse20011();
            list.setFiles(files);
            api.renterFilesGet();
            result = list;
            api.renterDeleteSiapathPost(remotePath.resolve(String.valueOf(new Date(10000).getTime())).toString());
            api.renterDeleteSiapathPost(remotePath.resolve(String.valueOf(new Date(20000).getTime())).toString());
            api.renterDeleteSiapathPost(remotePath.resolve(String.valueOf(new Date(30000).getTime())).toString());
        }};

        new DeleteRemoteFileTask(ctx, file).run();
        assertFalse(DB.contains(file));
        assertTrue(DBMock.committed);

    }

    @Test
    public void testNoFiles() throws IOException, ApiException {

        final Config cfg = new Config();
        cfg.setUserName("testuser");
        final Context ctx = new Context(cfg, null);

        final Path localPath = Utils.getSyncDir().resolve("testfile");
        final Path remotePath = ctx.pathPrefix.resolve("testfile");
        final InlineResponse20011Files file3 = new InlineResponse20011Files();
        file3.setSiapath(remotePath.resolve(String.valueOf(new Date(30000).getTime())).toString());
        file3.setLocalpath(localPath.toString());
        file3.setAvailable(true);
        file3.setFilesize(1234L);
        final SiaFile file = new SiaFileFromFilesAPI(file3, ctx.pathPrefix);

        new Expectations() {{
            final InlineResponse20011 list = new InlineResponse20011();
            list.setFiles(null);
            api.renterFilesGet();
            result = list;
        }};

        new DeleteRemoteFileTask(ctx, file).run();

    }

}