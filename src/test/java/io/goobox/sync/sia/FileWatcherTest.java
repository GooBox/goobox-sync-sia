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
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.SystemMock;
import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.FileUtils;
import org.dizitart.no2.objects.ObjectRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
@RunWith(JMockit.class)
public class FileWatcherTest {

    @SuppressWarnings("unused")
    @Mocked
    private DirectoryWatcher watchService;

    private Path tmpDir;
    private long now;
    private String name;
    private Path localPath;

    @Before
    public void setUp() throws IOException {
        new DBMock();
        this.tmpDir = Files.createTempDirectory(null);
        this.now = System.currentTimeMillis();

        this.name = String.format("test-name-%x", this.now);
        this.localPath = this.tmpDir.resolve(this.name);

    }

    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(this.tmpDir.toFile());
    }

    /**
     * Test FileWatcher ignores files modified in the last MinElapsedTime.
     */
    @Test
    public void ignoreRecentModifiedFiles() throws IOException {

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        new Expectations(executor) {{
            executor.scheduleAtFixedRate(withNotNull(), 0, FileWatcher.MinElapsedTime, TimeUnit.MILLISECONDS);
        }};
        new Expectations() {{
            watchService.watchAsync(executor);
        }};

        final FileWatcher watcher = new FileWatcher(this.tmpDir, executor);
        assertTrue(localPath.toFile().createNewFile());

        new SystemMock();

        SystemMock.currentTime = now;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.CREATE, localPath, 0));

        SystemMock.currentTime = now + 100;
        watcher.run();

        SystemMock.currentTime = now + 200;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.MODIFY, localPath, 1));

        SystemMock.currentTime = now + 300;
        watcher.run();

        SystemMock.currentTime = now + 400;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.MODIFY, localPath, 2));

        SystemMock.currentTime = now + 500;
        watcher.run();

        assertFalse(DB.get(name).isPresent());

    }

    /**
     * Create event occurs if a directory is created but such event should be ignored because directories are not
     * maintained in this app.
     */
    @Test
    public void ignoreDirectoryCreated() throws IOException {

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        new Expectations(executor) {{
            executor.scheduleAtFixedRate(withNotNull(), 0, FileWatcher.MinElapsedTime, TimeUnit.MILLISECONDS);
        }};
        new Expectations() {{
            watchService.watchAsync(executor);
        }};

        final FileWatcher watcher = new FileWatcher(this.tmpDir, executor);
        Files.createDirectory(localPath);

        new SystemMock();

        SystemMock.currentTime = now;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.CREATE, localPath, 0));

        final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
        assertFalse(trackingFiles.containsKey(localPath));

    }

    /**
     * Delete event occurs if a directory was deleted but such event should be ignored.
     */
    @Test
    public void ignoreDirectoryDeleted() throws IOException {

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        new Expectations(executor) {{
            executor.scheduleAtFixedRate(withNotNull(), 0, FileWatcher.MinElapsedTime, TimeUnit.MILLISECONDS);
        }};
        new Expectations() {{
            watchService.watchAsync(executor);
        }};

        final FileWatcher watcher = new FileWatcher(this.tmpDir, executor);
        Files.createDirectory(localPath);

        new SystemMock();

        SystemMock.currentTime = now;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.DELETE, localPath, 0));

        assertFalse(DB.get(name).isPresent());

    }

    /**
     * Test FileWatcher adds files not modified in the last MinElapsedTime seconds to the sync DB and marks it MODIFIED.
     */
    @Test
    public void testFoundNewFile() throws IOException {

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        new Expectations(executor) {{
            executor.scheduleAtFixedRate(withNotNull(), 0, FileWatcher.MinElapsedTime, TimeUnit.MILLISECONDS);
        }};
        new Expectations() {{
            watchService.watchAsync(executor);
        }};

        final FileWatcher watcher = new FileWatcher(this.tmpDir, executor);
        assertTrue(localPath.toFile().createNewFile());

        new SystemMock();

        SystemMock.currentTime = now;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.CREATE, localPath, 0));

        SystemMock.currentTime = now + 100;
        watcher.run();

        SystemMock.currentTime = now + 200;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.MODIFY, localPath, 1));

        SystemMock.currentTime = now + 2 * FileWatcher.MinElapsedTime;
        watcher.run();

        assertEquals(SyncState.MODIFIED, DB.get(name).get().getState());
        assertTrue(DBMock.committed);
        final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
        assertFalse(trackingFiles.containsKey(localPath));

    }

    /**
     * When a user modifies a file, the file should be marked as MODIFIED.
     */
    @Test
    public void syncedFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkStatusAfterModifyEvent(SyncState.SYNCED, SyncState.MODIFIED);
    }

    /**
     * When a file of which status is FOR_DOWNLOAD is found in the local directory, the download has to be canceled and
     * the found file is marked as MODIFIED.
     */
    @Test
    public void toBeDownloadedFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkStatusAfterModifyEvent(SyncState.FOR_DOWNLOAD, SyncState.MODIFIED);
    }

    /**
     * When a file of which is now being downloaded is found in the local directory, since the download cannot be
     * canceled, the status of the file will be updated to MODIFIED. CheckDownloadStateTask is responsible for deleting
     * the downloaded file.
     */
    @Test
    public void downloadingFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkStatusAfterModifyEvent(SyncState.DOWNLOADING, SyncState.MODIFIED);
    }

    /**
     * This case happens when a user modified a file to be uploaded while the file is waiting to start uploading.
     * Since metadata of the file is changed, delete the target file from the sync DB and add it as a new file.
     */
    @Test
    public void toBeUploadedFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkStatusAfterModifyEvent(SyncState.FOR_UPLOAD, SyncState.MODIFIED);
    }

    /**
     * This case happens when a user modified a file to be uploaded while the file is being uploaded.
     * Since the upload would be failed, delete the target file from the sync DB and add it as a new file.
     */
    @Test
    public void uploadingFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkStatusAfterModifyEvent(SyncState.UPLOADING, SyncState.MODIFIED);
    }

    /**
     * This case happens when CheckStateTask decided to delete a file from the cloud network but then a user updates the
     * file. In this case, the file shouldn't be deleted from the cloud network. CheckStateTask needs to check the file
     * and in most case the file should be uploaded.
     */
    @Test
    public void beCloudDeleteFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkStatusAfterModifyEvent(SyncState.FOR_CLOUD_DELETE, SyncState.MODIFIED);
    }

    /**
     * This case happens when CheckStateTask decided to delete a local file and then a user updates the file.
     * In this case, the file shouldn't be deleted and its state should be kept to MODIFIED so that CheckStateTask
     * can handle it.
     */
    @Test
    public void toBeLocalDeleteFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkStatusAfterModifyEvent(SyncState.FOR_LOCAL_DELETE, SyncState.MODIFIED);
    }


    private void checkStatusAfterModifyEvent(final SyncState before, final SyncState expected)
            throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        new Expectations(executor) {{
            executor.scheduleAtFixedRate(withNotNull(), 0, FileWatcher.MinElapsedTime, TimeUnit.MILLISECONDS);
        }};
        new Expectations() {{
            watchService.watchAsync(executor);
        }};

        final String dummyData = "this is a sample file body";
        final DirectoryChangeEvent.EventType[] events = new DirectoryChangeEvent.EventType[]{
                DirectoryChangeEvent.EventType.CREATE,
                DirectoryChangeEvent.EventType.MODIFY
        };
        for (DirectoryChangeEvent.EventType event : events) {

            try (final FileWatcher watcher = new FileWatcher(this.tmpDir, executor)) {

                final String name = String.format("test-%s-%x", event, System.currentTimeMillis());
                final Path localPath = this.tmpDir.resolve(name);
                assertTrue(localPath.toFile().createNewFile());
                DB.addNewFile(name, localPath);
                this.updateStatus(name, before);

                new SystemMock();

                Files.write(localPath, dummyData.getBytes());

                SystemMock.currentTime = now;
                watcher.onEvent(new DirectoryChangeEvent(event, localPath, 2));
                SystemMock.currentTime = now + 100;
                watcher.run();

                // in MinElapsedTime, status must not be chanced.
                assertEquals(before, DB.get(name).get().getState());

                SystemMock.currentTime = now + 2 * FileWatcher.MinElapsedTime;
                watcher.run();

                assertEquals(expected, DB.get(name).get().getState());
                final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
                assertFalse(trackingFiles.containsKey(localPath));
                assertTrue(DBMock.committed);

            }
            DBMock.committed = false;

        }

    }

    /**
     * Test FileWatcher ignores files created, modified and deleted in the last MinElapsedTime.
     */
    @Test
    public void testTrackingFileDeleted() throws IOException {

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        new Expectations(executor) {{
            executor.scheduleAtFixedRate(withNotNull(), 0, FileWatcher.MinElapsedTime, TimeUnit.MILLISECONDS);
        }};
        new Expectations() {{
            watchService.watchAsync(executor);
        }};

        final FileWatcher watcher = new FileWatcher(this.tmpDir, executor);
        new SystemMock();

        SystemMock.currentTime = now;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.CREATE, localPath, 0));
        watcher.run();
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.MODIFY, localPath, 1));
        watcher.run();
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.DELETE, localPath, 2));
        SystemMock.currentTime = now + 2 * FileWatcher.MinElapsedTime;
        watcher.run();

        assertFalse(DB.get(name).isPresent());
        final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
        assertFalse(trackingFiles.containsKey(localPath));

    }

    /**
     * Test FileWatcher marks statuses of deleted files `DELETED`.
     */
    @Test
    public void testSyncedFileDeleted() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        this.checkStatusAfterDeleteEvent(SyncState.SYNCED, SyncState.DELETED);
    }

    /**
     * When a file in FOR_UPLOAD status is deleted, the file should be marked as DELETED.
     */
    @Test
    public void testToBeUploadFileDeleted() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        this.checkStatusAfterDeleteEvent(SyncState.FOR_UPLOAD, SyncState.DELETED);
    }

    /**
     * When a file is deleted while it is being uploaded, the file should be marked as DELETED.
     * In this case, CheckUploadStatus would report an upload failed error but it is not problem to ignore the error.
     */
    @Test
    public void testUploadingFileDeleted() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        this.checkStatusAfterDeleteEvent(SyncState.UPLOADING, SyncState.DELETED);
    }

    /**
     * It is possible a file waiting for being downloaded is deleted from the local directory if a user creates and
     * deletes the file while the download is waiting. In this case, the download has to be continued.
     */
    @Test
    public void testToBeDownloadFileDeleted() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        this.checkStatusAfterDeleteEvent(SyncState.FOR_DOWNLOAD, SyncState.FOR_DOWNLOAD);
    }

    /**
     * As same as the case that a file waiting to start download is deleted locally, the download has to be continued.
     */
    @Test
    public void testDownloadingFileDeleted() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        this.checkStatusAfterDeleteEvent(SyncState.DOWNLOADING, SyncState.DOWNLOADING);
    }

    private void checkStatusAfterDeleteEvent(final SyncState before, final SyncState expected)
            throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        new Expectations(executor) {{
            executor.scheduleAtFixedRate(withNotNull(), 0, FileWatcher.MinElapsedTime, TimeUnit.MILLISECONDS);
        }};
        new Expectations() {{
            watchService.watchAsync(executor);
        }};

        // Directory deleted.
        try (final FileWatcher watcher = new FileWatcher(this.tmpDir, executor)) {

            assertTrue(localPath.toFile().createNewFile());
            DB.addNewFile(name, localPath);
            this.updateStatus(name, before);

            watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.DELETE, localPath, 2));

            assertEquals(expected, DB.get(name).get().getState());
            final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
            assertFalse(trackingFiles.containsKey(localPath));
            assertTrue(DBMock.committed);
        }

        DBMock.committed = false;

        // Modified and deleted.
        try (final FileWatcher watcher = new FileWatcher(this.tmpDir, executor)) {

            final String name = String.format("test-file-2-%x", System.currentTimeMillis());
            final Path localPath = this.tmpDir.resolve(name);
            assertTrue(localPath.toFile().createNewFile());
            DB.addNewFile(name, localPath);
            this.updateStatus(name, before);

            new SystemMock();

            SystemMock.currentTime = now;
            watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.MODIFY, localPath, 2));
            SystemMock.currentTime = now + 100;
            watcher.run();
            SystemMock.currentTime = now + 300;
            watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.DELETE, localPath, 2));

            assertEquals(expected, DB.get(name).get().getState());
            final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
            assertFalse(trackingFiles.containsKey(localPath));
            assertTrue(DBMock.committed);

        }

    }

    /**
     * Sometimes, FileWatcher receives modify event for a file but the file is not changed, for example, a user edits
     * the file accidentally and reverts it. In this case, the file should be ignored.
     */
    @Test
    public void testModifiedButNotChangedFile() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        new Expectations(executor) {{
            executor.scheduleAtFixedRate(withNotNull(), 0, FileWatcher.MinElapsedTime, TimeUnit.MILLISECONDS);
        }};
        new Expectations() {{
            watchService.watchAsync(executor);
        }};
        final String dummyData = "this is a sample file body";

        for (DirectoryChangeEvent.EventType event : new DirectoryChangeEvent.EventType[]{DirectoryChangeEvent.EventType.CREATE, DirectoryChangeEvent.EventType.MODIFY}) {

            try (final FileWatcher watcher = new FileWatcher(this.tmpDir, executor)) {

                final String name = String.format("test-%s-%x", event, System.currentTimeMillis());
                final Path localPath = this.tmpDir.resolve(name);
                Files.write(localPath, dummyData.getBytes(), StandardOpenOption.CREATE);
                DB.addNewFile(name, localPath);
                this.updateStatus(name, SyncState.SYNCED);

                new SystemMock();

                SystemMock.currentTime = now;
                watcher.onEvent(new DirectoryChangeEvent(event, localPath, 2));
                SystemMock.currentTime = now + 100;
                watcher.run();

                // in MinElapsedTime, status must not be chanced.
                assertEquals(SyncState.SYNCED, DB.get(name).get().getState());

                SystemMock.currentTime = now + 2 * FileWatcher.MinElapsedTime;
                watcher.run();

                assertEquals(SyncState.SYNCED, DB.get(name).get().getState());
                final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
                assertFalse(trackingFiles.containsKey(localPath));
                assertTrue(DBMock.committed);
            }
            DBMock.committed = false;

        }

    }

    @Test
    public void onEventChecksExcludedFiles() throws IOException {

        final Path target = this.tmpDir.resolve("some-file");
        new Expectations(Utils.class) {{
            Utils.isExcluded(target);
            result = true;
        }};
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try (final FileWatcher watcher = new FileWatcher(this.tmpDir, executor)) {
            watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.CREATE, target, 2));
        }

    }

    @Test
    public void onEventWithOverflow() throws IOException {

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        new Expectations(executor) {{
            executor.scheduleAtFixedRate(withNotNull(), 0, FileWatcher.MinElapsedTime, TimeUnit.MILLISECONDS);
        }};
        new Expectations() {{
            watchService.watchAsync(executor);
        }};

        final FileWatcher watcher = new FileWatcher(this.tmpDir, executor);

        new SystemMock();

        SystemMock.currentTime = now;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.OVERFLOW, null, 0));

        final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
        assertTrue(trackingFiles.isEmpty());

    }

    @SuppressWarnings("unchecked")
    private void updateStatus(final String name, final SyncState newState) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final SyncFile syncFile = DB.get(name).get();
        Deencapsulation.setField(syncFile, "state", newState);

        final Method repo = DB.class.getDeclaredMethod("repo");
        repo.setAccessible(true);
        final ObjectRepository<SyncFile> repository = (ObjectRepository<SyncFile>) repo.invoke(DB.class);
        repository.update(syncFile);

    }

}