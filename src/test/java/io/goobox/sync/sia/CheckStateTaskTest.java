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
import io.goobox.sync.sia.mocks.ExecutorMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.sia.model.SiaFileFromFilesAPI;
import io.goobox.sync.sia.model.SiaPath;
import io.goobox.sync.storj.Utils;
import io.goobox.sync.storj.db.SyncState;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class CheckStateTaskTest {

    @Mocked
    private RenterApi api;

    private Path tempDir;

    @Before
    public void setUpMockDB() {
        new DBMock();
    }

    @After
    public void tearDownMockDB() {
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
        cfg.userName = "testuser";
        final Context ctx = new Context(cfg, null);

        // Test files:
        //   cloud: file1, file2, file3, and file5, file7
        //   local: file1, file2, and file4, file6, file7, file8
        //
        // file1 in cloud is newer than one in local, file2 in local is newer than one in cloud,
        // thus, file1 will be downloaded and file2 will be uploaded.
        //
        // file5 doesn't exist in local directory but in sync db, which means it'll be deleted from the cloud.
        // file6 doesn't exist in cloud directory but in sync db, which means is'll be deleted from the local directory.
        // file7 is not available, which means it's being uploaded, now.
        // file8 doesn't exist in cloud directory but in sync db with upload failed status.
        //
        final List<InlineResponse20011Files> files = new ArrayList<>();
        final Date oldTimeStamp = new Date(100000);
        final Date newTimeStamp = new Date();

        // file1
        final InlineResponse20011Files file1 = new InlineResponse20011Files();
        final Path file1Path = ctx.pathPrefix.resolve(Paths.get("file1", String.valueOf(oldTimeStamp.getTime())));
        file1.setSiapath(file1Path.toString());
        file1.setAvailable(true);
        file1.setFilesize(0L);
        final SiaFile siaFile1 = new SiaFileFromFilesAPI(file1, ctx.pathPrefix);
        final File localFile1 = siaFile1.getLocalPath().toFile();
        localFile1.createNewFile();
        localFile1.setLastModified(oldTimeStamp.getTime());
        DB.setSynced(siaFile1);
        files.add(file1);

        final InlineResponse20011Files file1new = new InlineResponse20011Files();
        final Path file1newPath = ctx.pathPrefix.resolve(Paths.get("file1", String.valueOf(newTimeStamp.getTime())));
        file1new.setSiapath(file1newPath.toString());
        file1new.setAvailable(true);
        file1new.setFilesize(10L);
        files.add(file1new);

        // file2
        final InlineResponse20011Files file2 = new InlineResponse20011Files();
        final Path file2Path = ctx.pathPrefix.resolve(Paths.get("file2", String.valueOf(oldTimeStamp.getTime())));
        file2.setSiapath(file2Path.toString());
        file2.setAvailable(true);
        file2.setFilesize(0L);
        final SiaFile siaFile2 = new SiaFileFromFilesAPI(file2, ctx.pathPrefix);
        final File localFile2 = siaFile2.getLocalPath().toFile();
        localFile2.createNewFile();
        localFile2.setLastModified(oldTimeStamp.getTime());
        files.add(file2);
        DB.setSynced(siaFile2);
        localFile2.setLastModified(newTimeStamp.getTime());

        // file3
        final InlineResponse20011Files file3 = new InlineResponse20011Files();
        final Path file3Path = ctx.pathPrefix.resolve(Paths.get("file3", String.valueOf(newTimeStamp.getTime())));
        file3.setSiapath(file3Path.toString());
        file3.setAvailable(true);
        file3.setFilesize(10L);
        files.add(file3);
        final SiaFile siaFile3 = new SiaFileFromFilesAPI(file3, ctx.pathPrefix);

        // file4
        final Path localFile4 = Utils.getSyncDir().resolve("file4");
        localFile4.toFile().createNewFile();
        localFile4.toFile().setLastModified(newTimeStamp.getTime());

        // file5
        final InlineResponse20011Files file5 = new InlineResponse20011Files();
        final Path file5Path = ctx.pathPrefix.resolve(Paths.get("file5", String.valueOf(newTimeStamp.getTime())));
        file5.setSiapath(file5Path.toString());
        file5.setAvailable(true);
        file5.setFilesize(0L);
        final SiaFile siaFile5 = new SiaFileFromFilesAPI(file5, ctx.pathPrefix);
        final File localFile5 = siaFile5.getLocalPath().toFile();
        localFile5.createNewFile();
        localFile5.setLastModified(newTimeStamp.getTime());
        DB.setSynced(siaFile5);
        localFile5.delete();
        files.add(file5);

        // file6
        final InlineResponse20011Files file6 = new InlineResponse20011Files();
        final Path file6Path = ctx.pathPrefix.resolve(Paths.get("file6", String.valueOf(newTimeStamp.getTime())));
        file6.setSiapath(file6Path.toString());
        file6.setAvailable(true);
        file6.setFilesize(0L);
        final SiaFile siaFile6 = new SiaFileFromFilesAPI(file6, ctx.pathPrefix);
        final File localFile6 = siaFile6.getLocalPath().toFile();
        localFile6.createNewFile();
        localFile6.setLastModified(newTimeStamp.getTime());
        DB.setSynced(siaFile6);

        // file7
        final InlineResponse20011Files file7 = new InlineResponse20011Files();
        final Path file7Path = ctx.pathPrefix.resolve(Paths.get("file7", String.valueOf(newTimeStamp.getTime())));
        file7.setSiapath(file7Path.toString());
        file7.setAvailable(false);
        file7.setFilesize(0L);
        final SiaFile siaFile7 = new SiaFileFromFilesAPI(file7, ctx.pathPrefix);
        final File localFile7 = siaFile7.getLocalPath().toFile();
        localFile7.createNewFile();
        localFile7.setLastModified(oldTimeStamp.getTime());
        DB.addForUpload(siaFile7);
        files.add(file7);

        // file8
        final InlineResponse20011Files file8 = new InlineResponse20011Files();
        final Path file8Path = ctx.pathPrefix.resolve(Paths.get("file8", String.valueOf(oldTimeStamp.getTime())));
        file8.setSiapath(file8Path.toString());
        file8.setAvailable(true);
        file8.setFilesize(0L);
        final SiaFile siaFile8 = new SiaFileFromFilesAPI(file8, ctx.pathPrefix);
        final File localFile8 = siaFile8.getLocalPath().toFile();
        localFile8.createNewFile();
        localFile8.setLastModified(oldTimeStamp.getTime());
        DB.addForUpload(siaFile8);
        DB.setUploadFailed(siaFile8.getLocalPath());
        localFile8.setLastModified(newTimeStamp.getTime());

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(ctx, executor).run();

        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(siaFile1).getState());
        assertEquals(SyncState.FOR_UPLOAD, DB.get(siaFile2).getState());
        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(siaFile3).getState());
        assertEquals(SyncState.FOR_UPLOAD, DB.get(localFile4).getState());
        assertEquals(SyncState.FOR_CLOUD_DELETE, DB.get(siaFile5).getState());
        assertEquals(SyncState.FOR_LOCAL_DELETE, DB.get(siaFile6).getState());
        assertEquals(SyncState.FOR_UPLOAD, DB.get(siaFile7).getState());
        assertEquals(SyncState.FOR_UPLOAD, DB.get(siaFile8).getState());

        for (Runnable cmd : executor.queue) {
            if (cmd instanceof UploadLocalFileTask) {
                final UploadLocalFileTask task = (UploadLocalFileTask) cmd;
                final SiaPath path = new SiaPath(Deencapsulation.getField(task, "remotePath").toString(), ctx.pathPrefix);
                final Date creationTime = Deencapsulation.getField(task, "creationTime");
                assertEquals(String.format("Creation time of %s", path.remotePath), newTimeStamp.getTime() / 1000, creationTime.getTime() / 1000);
            }
        }

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetLocalPaths() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final Config cfg = new Config();
        cfg.userName = "testuser";
        final Context ctx = new Context(cfg, null);

        // Test files:
        // - file1
        // - subdir/file2
        final Path file1 = Utils.getSyncDir().resolve("file1");
        file1.toFile().createNewFile();

        final Path subdir = Utils.getSyncDir().resolve("subdir");
        subdir.toFile().mkdir();

        final Path file2 = subdir.resolve("file2");
        file2.toFile().createNewFile();

        final CheckStateTask target = new CheckStateTask(ctx, new ExecutorMock());

        final Method getLocalPaths = CheckStateTask.class.getDeclaredMethod("getLocalPaths");
        getLocalPaths.setAccessible(true);
        final Set<Path> res = (Set<Path>) getLocalPaths.invoke(target);
        assertEquals(2, res.size());
        assertTrue(res.contains(file1));
        assertTrue(res.contains(file2));
        assertFalse(res.contains(subdir));

    }

}