/*
 * Copyright (C) 2017-2018 Junpei Kawamoto
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

import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.model.FileInfo;
import io.goobox.sync.sia.client.api.model.InlineResponse20011;
import io.goobox.sync.sia.db.CloudFile;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.ExecutorMock;
import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.sia.model.SiaFileFromFilesAPI;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
@RunWith(JMockit.class)
public class CheckStateTaskTest {

    @Mocked
    private App app;

    @Mocked
    private RenterApi api;

    private Path tmpDir;
    private Context ctx;
    private String name;
    private Date oldTimeStamp;
    private Date newTimeStamp;

    @Before
    public void setUp() throws IOException {
        new DBMock();
        this.tmpDir = Files.createTempDirectory(null);

        final Config cfg = new Config(this.tmpDir.resolve(App.ConfigFileName));
        Deencapsulation.setField(cfg, "userName", "test-user");
        Deencapsulation.setField(cfg, "syncDir", this.tmpDir.toAbsolutePath());
        this.ctx = new Context(cfg);

        this.name = String.format("file-%x", System.currentTimeMillis());
        this.oldTimeStamp = new Date(100000);
        this.newTimeStamp = new Date();
    }

    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(this.tmpDir.toFile());
    }

    /**
     * A file already synced but modified in the cloud network will be downloaded.
     * <p>
     * Target file condiion: cloud yes, local yes, db yes.
     */
    @Test
    public void cloudFileModified() throws IOException, ApiException {

        final FileInfo file = this.createCloudFile(oldTimeStamp, true, 0);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        final Path localPath = siaFile.getLocalPath();
        Files.createFile(localPath);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(oldTimeStamp.getTime()));
        DB.setSynced(siaFile, siaFile.getLocalPath());

        final List<FileInfo> files = Arrays.asList(
                file, this.createCloudFile(newTimeStamp, true, 10)
        );

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);
            app.refreshOverlayIcon(localPath);
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
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

        final FileInfo file = this.createCloudFile(oldTimeStamp, true, 0);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        final Path localPath = siaFile.getLocalPath();
        Files.createFile(localPath);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(oldTimeStamp.getTime()));
        DB.setSynced(siaFile, localPath);

        Files.setLastModifiedTime(localPath, FileTime.fromMillis(newTimeStamp.getTime()));
        Files.write(localPath, "new data".getBytes());
        DB.setModified(name, localPath);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(file));
            api.renterFilesGet();
            result = res;

            App.getInstance();
            times = 0;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.FOR_UPLOAD, DB.get(siaFile).get().getState());

        final Path expected = this.ctx.getPathPrefix().resolve(Paths.get(name)).resolve(String.valueOf(newTimeStamp.getTime()));
        assertEquals(expected.getParent(), DB.get(siaFile).get().getCloudPath().get().getParent());

        final long time1 = Long.valueOf(expected.getFileName().toString());
        final long time2 = Long.valueOf(DB.get(siaFile).get().getCloudPath().get().getFileName().toString());
        assertTrue(String.format("time1 = %d, time2 = %d", time1, time2), Math.abs(time1 - time2) < 2000);

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

        final FileInfo file = this.createCloudFile(newTimeStamp, true, 10);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(file));
            api.renterFilesGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);
            app.refreshOverlayIcon(siaFile.getLocalPath());
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
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

        final Path localPath = this.tmpDir.resolve(name);
        Files.createFile(localPath);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(newTimeStamp.getTime()));
        DB.addNewFile(name, localPath);
        DB.commit();

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.emptyList());
            api.renterFilesGet();
            result = res;

            App.getInstance();
            times = 0;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
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

        final FileInfo file = this.createCloudFile(newTimeStamp, true, 0);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        final Path localPath = siaFile.getLocalPath();
        Files.createFile(localPath);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(oldTimeStamp.getTime()));

        DB.addNewFile(name, localPath);
        DB.setDeleted(name);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(file));
            api.renterFilesGet();
            result = res;

            // Since the local file has been deleted, don't need to update the overlay icon.
            App.getInstance();
            times = 0;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
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

        final Path localPath = this.tmpDir.resolve(name);
        Files.createFile(localPath);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(oldTimeStamp.getTime()));

        DB.addNewFile(name, localPath);
        DB.setSynced(new CloudFile() {
            @NotNull
            @Override
            public String getName() {
                return name;
            }

            @NotNull
            @Override
            public Path getCloudPath() {
                return ctx.getPathPrefix().resolve(name);
            }

            @Override
            public long getFileSize() {
                return 0;
            }
        }, localPath);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.emptyList());
            api.renterFilesGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);
            app.refreshOverlayIcon(localPath);
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
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

        final FileInfo file = this.createCloudFile(newTimeStamp, false, 0);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        final Path localPath = siaFile.getLocalPath();
        Files.createFile(localPath);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(oldTimeStamp.getTime()));

        DB.addNewFile(name, localPath);
        DB.setForUpload(name, localPath, Paths.get(file.getSiapath()));

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(file));
            api.renterFilesGet();
            result = res;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
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

        final Path localPath = this.tmpDir.resolve("file");
        Files.createFile(localPath);
        DB.addNewFile(name, localPath);

        Files.deleteIfExists(localPath);
        DB.setDeleted(name);
        DB.commit();

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.emptyList());
            api.renterFilesGet();
            result = res;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
        assertTrue(DBMock.committed);
        assertFalse(DB.get(name).isPresent());
        assertEquals(0, executor.queue.size());

    }

    /**
     * A file its state is UPLOAD_FAILED will be enqueued to be uploaded again.
     * <p>
     * Target file condition: cloud no, local yes, db yes.
     */
    @Test
    public void uploadFailed() throws IOException, ApiException {

        final FileInfo file = this.createCloudFile(oldTimeStamp, true, 0);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        final Path localPath = siaFile.getLocalPath();
        Files.createFile(localPath);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(oldTimeStamp.getTime()));

        DB.addNewFile(name, localPath);
        DB.setUploadFailed(name);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(newTimeStamp.getTime()));

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(file));
            api.renterFilesGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);
            app.refreshOverlayIcon(localPath);
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
        assertTrue(DBMock.committed);

        assertEquals(SyncState.FOR_UPLOAD, DB.get(siaFile).get().getState());

        // Check enqueued task.
        final Callable<Void> task = Deencapsulation.getField(executor.queue.get(0), "task");
        assertTrue(task instanceof UploadLocalFileTask);
        assertEquals(localPath, Deencapsulation.getField(task, "localPath"));

    }

    /**
     * Test the case where uploading a file failed and the file is not available yet. In this case,
     * the file looks only existing in local but might be in a contract. Thus, before restarting
     */
    @Test
    public void uploadFailedAndNoAvailableCloudFile() throws IOException, ApiException {

        final FileInfo file = this.createCloudFile(oldTimeStamp, true, 0);
        file.setAvailable(false);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        final Path localPath = siaFile.getLocalPath();
        Files.createFile(localPath);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(oldTimeStamp.getTime()));

        DB.addNewFile(name, localPath);
        DB.setUploadFailed(name);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(newTimeStamp.getTime()));

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(file));
            api.renterFilesGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);
            app.refreshOverlayIcon(localPath);
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
        assertTrue(DBMock.committed);

        assertEquals(SyncState.FOR_UPLOAD, DB.get(siaFile).get().getState());

        // Check enqueued task.
        final Callable<Void> task = Deencapsulation.getField(executor.queue.get(0), "task");
        assertTrue(task instanceof UploadLocalFileTask);
        assertEquals(localPath, Deencapsulation.getField(task, "localPath"));

    }

    /**
     * Test the case where uploading a file failed and the file is not included in /renter/files. In this case,
     * the file looks only existing in local but might be in a contract. Thus, before restarting
     */
    @Test
    public void uploadFailedAndNoCloudFile() throws IOException, ApiException {

        final FileInfo file = this.createCloudFile(oldTimeStamp, true, 0);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        final Path localPath = siaFile.getLocalPath();
        Files.createFile(localPath);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(oldTimeStamp.getTime()));

        DB.addNewFile(name, localPath);
        DB.setUploadFailed(name);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(newTimeStamp.getTime()));

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.emptyList());
            api.renterFilesGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);
            app.refreshOverlayIcon(localPath);
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
        assertTrue(DBMock.committed);

        assertEquals(SyncState.FOR_UPLOAD, DB.get(siaFile).get().getState());

        // Check enqueued task.
        final Callable<Void> task = Deencapsulation.getField(executor.queue.get(0), "task");
        assertTrue(task instanceof UploadLocalFileTask);
        assertEquals(localPath, Deencapsulation.getField(task, "localPath"));

    }

    @Test
    public void toBeDownloadedFileModified() throws IOException, ApiException {

        final FileInfo file = this.createCloudFile(newTimeStamp, true, 0);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        final Path localPath = siaFile.getLocalPath();
        Files.createFile(localPath);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(oldTimeStamp.getTime()));

        DB.addNewFile(name, localPath);
        DB.setModified(name, localPath);
        assertEquals(oldTimeStamp.getTime(), (long) DB.get(name).get().getLocalModificationTime().get());
        DB.commit();

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(file));
            api.renterFilesGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);
            app.refreshOverlayIcon(localPath);
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(name).get().getState());

        // Check enqueued task.
        final Callable<Void> task = Deencapsulation.getField(executor.queue.get(0), "task");
        assertTrue(task instanceof DownloadCloudFileTask);
        assertEquals(siaFile.getName(), Deencapsulation.getField(task, "name"));

    }

    /**
     * Test case that a file marked as modified but already deleted.
     */
    @Test
    public void modifiedFileIsDeleted() throws ApiException, IOException {

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.emptyList());
            api.renterFilesGet();
            result = res;

            // Since the local file has been deleted, don't need to update the overlay icon.
            App.getInstance();
            times = 0;
        }};

        final Path localPath = tmpDir.resolve(name);
        Files.createFile(localPath);
        DB.addNewFile(name, localPath);
        DB.setModified(name, localPath);

        Files.deleteIfExists(localPath);
        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();

        assertTrue(DB.get(name).isPresent());
        assertEquals(SyncState.DELETED, DB.get(name).get().getState());
        assertEquals(0, executor.queue.size());

    }

    @Test
    public void toBeUploadedFileIsDeleted() throws IOException, ApiException {

        final FileInfo file = this.createCloudFile(oldTimeStamp, true, 0);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        final Path localPath = siaFile.getLocalPath();
        Files.createFile(localPath);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(oldTimeStamp.getTime()));
        DB.setSynced(siaFile, localPath);

        Files.setLastModifiedTime(localPath, FileTime.fromMillis(newTimeStamp.getTime()));
        Files.write(localPath, "new data".getBytes());
        DB.setModified(name, localPath);
        Files.deleteIfExists(localPath);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(file));
            api.renterFilesGet();
            result = res;

            // Since the local file has been deleted, don't need to update the overlay icon.
            App.getInstance();
            times = 0;
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
        assertTrue(DBMock.committed);
        assertTrue(DB.get(name).isPresent());
        assertEquals(SyncState.DELETED, DB.get(name).get().getState());
        assertEquals(0, executor.queue.size());

    }

    /**
     * Test the case where a file is marked as modified but the last modification times of local/cloud files are same.
     * In this case, just setting the sync state to Synced.
     */
    @Test
    public void modifiedButTimeStampIsSame() throws IOException, ApiException {

        final FileInfo file = this.createCloudFile(oldTimeStamp, true, 0);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        final Path localPath = siaFile.getLocalPath();
        Files.createFile(localPath);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(oldTimeStamp.getTime()));
        DB.setModified(name, localPath);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(file));
            api.renterFilesGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);
            app.refreshOverlayIcon(localPath);
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
        assertTrue(DBMock.committed);
        assertTrue(DB.get(name).isPresent());
        assertEquals(SyncState.SYNCED, DB.get(name).get().getState());
        assertEquals(0, executor.queue.size());

    }

    /**
     * Test the case where downloading a file failed, and the cloud file is still newer than the corresponding
     * local file. In this case, retry to download that file.
     */
    @Test
    public void downloadFailedButCloudFileIsStillNewer() throws IOException, ApiException {

        final FileInfo file = this.createCloudFile(newTimeStamp, true, 0);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        final Path localPath = siaFile.getLocalPath();
        Files.createFile(localPath);
        Files.setLastModifiedTime(localPath, FileTime.fromMillis(oldTimeStamp.getTime()));
        DB.setModified(name, localPath);
        DB.setDownloadFailed(name);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(file));
            api.renterFilesGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);
            app.refreshOverlayIcon(localPath);
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
        assertTrue(DBMock.committed);
        assertTrue(DB.get(name).isPresent());
        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(name).get().getState());
        assertEquals(1, executor.queue.size());

    }

    /**
     * Test the case where downloading a file failed, and the corresponding local file is not found. In this csse,
     * retry to download the file.
     */
    @Test
    public void downloadFailedAndLocalFileIsNotFound() throws ApiException, IOException {

        final FileInfo file = this.createCloudFile(newTimeStamp, true, 0);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        final Path localPath = siaFile.getLocalPath();
        DB.addForDownload(siaFile, localPath);
        DB.setDownloadFailed(name);

        DB.commit();
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(file));
            api.renterFilesGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);
            app.refreshOverlayIcon(localPath);
        }};

        final ExecutorMock executor = new ExecutorMock();
        new CheckStateTask(this.ctx, executor).call();
        assertTrue(DBMock.committed);
        assertTrue(DB.get(name).isPresent());
        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(name).get().getState());
        assertEquals(1, executor.queue.size());

    }

    private FileInfo createCloudFile(final Date timeStamp, final boolean availability, final long fileSize) {
        final FileInfo file = new FileInfo();
        final Path remotePath = this.ctx.getPathPrefix().resolve(Paths.get(name, String.valueOf(timeStamp.getTime())));
        file.setSiapath(remotePath.toString());
        file.setAvailable(availability);
        file.setFilesize(fileSize);
        return file;
    }

}