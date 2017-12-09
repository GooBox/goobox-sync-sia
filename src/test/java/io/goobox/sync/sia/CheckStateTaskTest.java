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
import io.goobox.sync.sia.client.api.model.InlineResponse20011;
import io.goobox.sync.sia.client.api.model.InlineResponse20011Files;
import io.goobox.sync.sia.db.CloudFile;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.ExecutorMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.sia.model.SiaFileFromFilesAPI;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
@RunWith(JMockit.class)
public class CheckStateTaskTest {

    @SuppressWarnings("unused")
    @Mocked
    private RenterApi api;

    private Path tempDir;
    private Context context;
    private String name;
    private Date oldTimeStamp;
    private Date newTimeStamp;

    @Before
    public void setUp() throws IOException {

        new DBMock();

        tempDir = Files.createTempDirectory(null);
        UtilsMock.syncDir = tempDir;
        new UtilsMock();

        final Config cfg = new Config();
        cfg.setUserName("test-user");
        this.context = new Context(cfg, null);

        this.name = String.format("file-%x", System.currentTimeMillis());
        this.oldTimeStamp = new Date(100000);
        this.newTimeStamp = new Date();

    }

    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    /**
     * A file already synced but modified in the cloud network will be downloaded.
     * <p>
     * Target file condiion: cloud yes, local yes, db yes.
     */
    @Test
    public void cloudFileModified() throws IOException, ApiException {

        final List<InlineResponse20011Files> files = new ArrayList<>();

        final InlineResponse20011Files file = new InlineResponse20011Files();
        final Path remotePath = this.context.pathPrefix.resolve(Paths.get(name, String.valueOf(oldTimeStamp.getTime())));
        file.setSiapath(remotePath.toString());
        file.setAvailable(true);
        file.setFilesize(0L);

        final SiaFile siaFile = new SiaFileFromFilesAPI(this.context, file);
        final File localFile = siaFile.getLocalPath().toFile();
        assertTrue(localFile.createNewFile());
        assertTrue(localFile.setLastModified(oldTimeStamp.getTime()));
        DB.setSynced(siaFile, siaFile.getLocalPath());
        files.add(file);

        final InlineResponse20011Files newerFile = new InlineResponse20011Files();
        final Path newerRemotePath = this.context.pathPrefix.resolve(Paths.get(name, String.valueOf(newTimeStamp.getTime())));
        newerFile.setSiapath(newerRemotePath.toString());
        newerFile.setAvailable(true);
        newerFile.setFilesize(10L);
        files.add(newerFile);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.context, executor).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(siaFile).get().getState());
        assertTrue(DB.get(siaFile).get().getTemporaryPath().get().startsWith(Paths.get(System.getProperty("java.io.tmpdir"))));

        // Check enqueued task.
        final Callable<Void> task = Deencapsulation.getField(executor.queue.get(0), "task");
        assertTrue(task instanceof DownloadCloudFileTask);
        final String enqueuedName = Deencapsulation.getField(task, "name");
        assertEquals(siaFile.getName(), enqueuedName);

    }

    /**
     * A file already synced but modified in the local directory will be uploaded.
     * <p>
     * Target file condition: cloud yes, local yes, db yes (MODIFIED)
     */
    @Test
    public void syncedFileModified() throws IOException, ApiException {

        final List<InlineResponse20011Files> files = new ArrayList<>();
        final InlineResponse20011Files file = new InlineResponse20011Files();
        final Path remotePath = this.context.pathPrefix.resolve(Paths.get(name, String.valueOf(oldTimeStamp.getTime())));
        file.setSiapath(remotePath.toString());
        file.setAvailable(true);
        file.setFilesize(0L);
        files.add(file);

        final SiaFile siaFile = new SiaFileFromFilesAPI(this.context, file);
        final Path localPath = siaFile.getLocalPath();
        final File localFile = localPath.toFile();
        assertTrue(localFile.createNewFile());
        assertTrue(localFile.setLastModified(oldTimeStamp.getTime()));
        DB.setSynced(siaFile, localPath);

        assertTrue(localFile.setLastModified(newTimeStamp.getTime()));
        Files.write(localPath, "new data".getBytes());
        DB.setModified(name, localPath);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.context, executor).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.FOR_UPLOAD, DB.get(siaFile).get().getState());

        final Path expected = this.context.pathPrefix.resolve(Paths.get(name)).resolve(String.valueOf(newTimeStamp.getTime()));
        assertEquals(expected.getParent(), DB.get(siaFile).get().getCloudPath().get().getParent());

        final long time1 = Long.valueOf(expected.getFileName().toString());
        final long time2 = Long.valueOf(DB.get(siaFile).get().getCloudPath().get().getFileName().toString());
        assertTrue(Math.abs(time1 - time2) < 1000);

        // Check enqueued task.
        final Callable<Void> task = Deencapsulation.getField(executor.queue.get(0), "task");
        assertTrue(task instanceof UploadLocalFileTask);
        assertEquals(localPath, Deencapsulation.getField(task, "localPath"));

    }

    /**
     * A file found in the cloud network but not in the local directory will be downloaded.
     * <p>
     * Target file condition: cloud yes, local no, db no.
     */
    @Test
    public void newCloudFile() throws ApiException {

        final List<InlineResponse20011Files> files = new ArrayList<>();
        final InlineResponse20011Files file = new InlineResponse20011Files();
        final Path remotePath = this.context.pathPrefix.resolve(Paths.get(name, String.valueOf(newTimeStamp.getTime())));
        file.setSiapath(remotePath.toString());
        file.setAvailable(true);
        file.setFilesize(10L);
        files.add(file);

        final SiaFile siaFile = new SiaFileFromFilesAPI(this.context, file);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};


        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.context, executor).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(siaFile).get().getState());
        assertTrue(DB.get(siaFile).get().getTemporaryPath().get().startsWith(Paths.get(System.getProperty("java.io.tmpdir"))));

        // Check enqueued task.
        final Callable<Void> task = Deencapsulation.getField(executor.queue.get(0), "task");
        assertTrue(task instanceof DownloadCloudFileTask);
        assertEquals(siaFile.getName(), Deencapsulation.getField(task, "name"));

    }

    /**
     * A file found in the local directory but not in the cloud network will be uploaded.
     * <p>
     * Target file condition: cloud no, local yes, db yes (MODIFIED)
     */
    @Test
    public void newLocalFile() throws IOException, ApiException {

        final Path localPath = Utils.getSyncDir().resolve(name);
        final File localFile = localPath.toFile();
        assertTrue(localFile.createNewFile());
        assertTrue(localFile.setLastModified(newTimeStamp.getTime()));

        DB.addNewFile(name, localPath);
        DB.commit();

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(new ArrayList<>());
            api.renterFilesGet();
            result = res;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.context, executor).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.FOR_UPLOAD, DB.get(name).get().getState());

        // Check enqueued task.
        final Callable<Void> task = Deencapsulation.getField(executor.queue.get(0), "task");
        assertTrue(task instanceof UploadLocalFileTask);
        assertEquals(localPath, Deencapsulation.getField(task, "localPath"));

    }

    /**
     * A file of which state is DELETED will be deleted from the cloud network.
     * <p>
     * Target file condition: cloud yes, local no, db yes.
     */
    @Test
    public void toBeDeletedFromCloudFile() throws IOException, ApiException {

        final List<InlineResponse20011Files> files = new ArrayList<>();
        final InlineResponse20011Files file = new InlineResponse20011Files();
        final Path remotePath = this.context.pathPrefix.resolve(Paths.get(name, String.valueOf(newTimeStamp.getTime())));
        file.setSiapath(remotePath.toString());
        file.setAvailable(true);
        file.setFilesize(0L);

        final SiaFile siaFile = new SiaFileFromFilesAPI(this.context, file);
        final Path localPath = siaFile.getLocalPath();
        final File localFile = siaFile.getLocalPath().toFile();
        assertTrue(localFile.createNewFile());
        assertTrue(localFile.setLastModified(oldTimeStamp.getTime()));

        DB.addNewFile(name, localPath);
        DB.setDeleted(name);
        files.add(file);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.context, executor).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.FOR_CLOUD_DELETE, DB.get(siaFile).get().getState());

        // Check enqueued task.
        final Callable<Void> task = Deencapsulation.getField(executor.queue.get(0), "task");
        assertTrue(task instanceof DeleteCloudFileTask);
        assertEquals(siaFile.getName(), Deencapsulation.getField(task, "name"));

    }

    /**
     * A file not existing in cloud directory but in local and sync db means the file was deleted from another app.
     * The file will be deleted from the local directory.
     * <p>
     * Target file condition: cloud no, local yes, db yes.
     */
    @Test
    public void toBeDeletedFromLocalFile() throws IOException, ApiException {

        final Path localPath = Utils.getSyncDir().resolve(name);
        final File localFile = localPath.toFile();
        assertTrue(localFile.createNewFile());
        assertTrue(localFile.setLastModified(oldTimeStamp.getTime()));

        DB.addNewFile(name, localPath);
        DB.setSynced(new CloudFile() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Path getCloudPath() {
                return null;
            }

            @Override
            public long getFileSize() {
                return 0;
            }
        }, localPath);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            final List<InlineResponse20011Files> files = new ArrayList<>();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.context, executor).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.FOR_LOCAL_DELETE, DB.get(name).get().getState());

        // Check enqueued task.
        final Runnable task = executor.queue.get(0);
        assertTrue(task instanceof DeleteLocalFileTask);
        assertEquals(localPath, Deencapsulation.getField(task, "localPath"));

    }


    /**
     * A file sill being uploaded will not be handled by CheckStateTask.
     * <p>
     * Target file condition: cloud yes (not available), local yes, db yes.
     */
    @Test
    public void uploadingFile() throws IOException, ApiException {

        final List<InlineResponse20011Files> files = new ArrayList<>();
        final InlineResponse20011Files file = new InlineResponse20011Files();
        final Path cloudPath = this.context.pathPrefix.resolve(Paths.get(name, String.valueOf(newTimeStamp.getTime())));
        file.setSiapath(cloudPath.toString());
        file.setAvailable(false);
        file.setFilesize(0L);

        final SiaFile siaFile = new SiaFileFromFilesAPI(this.context, file);
        final Path localPath = siaFile.getLocalPath();
        final File localFile = siaFile.getLocalPath().toFile();
        assertTrue(localFile.createNewFile());
        assertTrue(localFile.setLastModified(oldTimeStamp.getTime()));

        DB.addNewFile(name, localPath);
        DB.setForUpload(name, localPath, cloudPath);
        files.add(file);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.context, executor).call();
        assertTrue(DBMock.committed);

        // Check enqueued task.
        assertEquals(SyncState.FOR_UPLOAD, DB.get(siaFile).get().getState());
        assertEquals(0, executor.queue.size());

    }

    /**
     * Test case that a file which existed only the local directory is deleted.
     * In this case the sync DB deletes its entry.
     */
    @Test
    public void deletedLocalFile() throws ApiException, IOException {

        final Path localPath = Utils.getSyncDir().resolve("file");
        assertTrue(localPath.toFile().createNewFile());
        DB.addNewFile(name, localPath);

        assertTrue(localPath.toFile().delete());
        DB.setDeleted(name);
        DB.commit();

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(new ArrayList<>());
            api.renterFilesGet();
            result = res;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.context, executor).call();
        assertTrue(DBMock.committed);
        assertFalse(DB.get(name).isPresent());
        assertEquals(0, executor.queue.size());

    }

    /**
     * A file its state is UPLOAD_FAILED will be enqueued to be uploaded again.
     * <p>
     * Target file condition: cloud no, local yes, db yes.
     */
    @Ignore
    @Test
    public void uploadFailedFile() throws IOException, ApiException {

        final List<InlineResponse20011Files> files = new ArrayList<>();
        final InlineResponse20011Files file = new InlineResponse20011Files();
        final Path remotePath = this.context.pathPrefix.resolve(Paths.get(name, String.valueOf(oldTimeStamp.getTime())));
        file.setSiapath(remotePath.toString());
        file.setAvailable(true);
        file.setFilesize(0L);

        final SiaFile siaFile = new SiaFileFromFilesAPI(this.context, file);
        final Path localPath = siaFile.getLocalPath();
        final File localFile = localPath.toFile();
        assertTrue(localFile.createNewFile());
        assertTrue(localFile.setLastModified(oldTimeStamp.getTime()));

        DB.addNewFile(name, localPath);
        DB.setUploadFailed(name);
        assertTrue(localFile.setLastModified(newTimeStamp.getTime()));

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.context, executor).call();
        assertTrue(DBMock.committed);

        assertEquals(SyncState.FOR_UPLOAD, DB.get(siaFile).get().getState());

        // Check enqueued task.
        final Callable<Void> task = Deencapsulation.getField(executor.queue.get(0), "task");
        assertTrue(task instanceof UploadLocalFileTask);
        assertEquals(localPath, Deencapsulation.getField(task, "localPath"));

    }

    @Test
    public void toBeDownloadedFileModified() throws IOException, ApiException {

        final List<InlineResponse20011Files> files = new ArrayList<>();
        final InlineResponse20011Files file = new InlineResponse20011Files();
        final Path remotePath = this.context.pathPrefix.resolve(Paths.get(name, String.valueOf(newTimeStamp.getTime())));
        file.setSiapath(remotePath.toString());
        file.setAvailable(true);
        file.setFilesize(0L);
        files.add(file);

        final SiaFile siaFile = new SiaFileFromFilesAPI(this.context, file);
        final Path localPath = siaFile.getLocalPath();
        assertTrue(localPath.toFile().createNewFile());
        assertTrue(localPath.toFile().setLastModified(this.oldTimeStamp.getTime()));

        DB.addNewFile(name, localPath);
        DB.setModified(name, localPath);
        assertEquals(oldTimeStamp.getTime(), (long) DB.get(name).get().getLocalModificationTime().get());
        DB.commit();

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.context, executor).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(name).get().getState());

        // Check enqueued task.
        final Callable<Void> task = Deencapsulation.getField(executor.queue.get(0), "task");
        assertTrue(task instanceof DownloadCloudFileTask);
        assertEquals(siaFile.getName(), Deencapsulation.getField(task, "name"));

    }

}