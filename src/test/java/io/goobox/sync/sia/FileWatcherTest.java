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

import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.SystemMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.storj.Utils;
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
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class FileWatcherTest {

    @SuppressWarnings("unused")
    @Mocked
    private DirectoryWatcher watchService;

    private Path tmpDir;
    private long now;

    @Before
    public void setUp() throws IOException {
        this.tmpDir = Files.createTempDirectory(null);
        UtilsMock.syncDir = this.tmpDir;
        new UtilsMock();
        this.now = System.currentTimeMillis();
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(this.tmpDir.toFile());
    }

    @Before
    public void setUpMockDB() {
        new DBMock();
    }

    @After
    public void tearDownMockDB() {
        DB.close();
    }

    /**
     * Test FileWatcher ignores files modified in the last MinElapsedTime.
     */
    @Test
    public void testIgnoreRecentModifiedFiles() throws IOException {

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        new Expectations(executor) {{
            executor.scheduleAtFixedRate(withNotNull(), 0, FileWatcher.MinElapsedTime, TimeUnit.MILLISECONDS);
        }};
        new Expectations() {{
            watchService.watchAsync(executor);
        }};

        final FileWatcher watcher = new FileWatcher(Utils.getSyncDir(), executor);
        final Path target = Utils.getSyncDir().resolve("test-file");
        assertTrue(target.toFile().createNewFile());

        new SystemMock();

        SystemMock.currentTime = now;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.CREATE, target, 0));

        SystemMock.currentTime = now + 100;
        watcher.run();

        SystemMock.currentTime = now + 200;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.MODIFY, target, 1));

        SystemMock.currentTime = now + 300;
        watcher.run();

        SystemMock.currentTime = now + 400;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.MODIFY, target, 2));

        SystemMock.currentTime = now + 500;
        watcher.run();

        assertFalse(DB.contains(target));

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

        final FileWatcher watcher = new FileWatcher(Utils.getSyncDir(), executor);
        final Path target = Utils.getSyncDir().resolve("test-file");
        assertTrue(target.toFile().createNewFile());

        new SystemMock();

        SystemMock.currentTime = now;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.CREATE, target, 0));

        SystemMock.currentTime = now + 100;
        watcher.run();

        SystemMock.currentTime = now + 200;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.MODIFY, target, 1));

        SystemMock.currentTime = now + 2 * FileWatcher.MinElapsedTime;
        watcher.run();

        assertEquals(SyncState.MODIFIED, DB.get(target).getState());
        assertTrue(DBMock.committed);
        final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
        assertFalse(trackingFiles.containsKey(target));

    }

    /**
     * When a user modifies a file, the file should be marked as MODIFIED.
     */
    @Test
    public void testSyncedFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkStatusAfterModifyEvent(SyncState.SYNCED, SyncState.MODIFIED);
    }

    /**
     * When a file of which status is FOR_DOWNLOAD is found in the local directory, the download has to be canceled and
     * the found file is marked as MODIFIED.
     */
    @Test
    public void testToBeDownloadedFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkStatusAfterModifyEvent(SyncState.FOR_DOWNLOAD, SyncState.MODIFIED);
    }

    /**
     * When a file of which is now being downloaded is found in the local directory, since the download cannot be
     * canceled, the status of the file will be updated to MODIFIED. CheckDownloadStatusTask is responsible for deleting
     * the downloaded file.
     */
    @Test
    public void testDownloadingFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkStatusAfterModifyEvent(SyncState.DOWNLOADING, SyncState.MODIFIED);
    }

    /**
     * This case happens when a user modified a file to be uploaded while the file is waiting to start uploading.
     * Since metadata of the file is changed, delete the target file from the sync DB and add it as a new file.
     */
    @Test
    public void testToBeUploadedFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkStatusAfterModifyEvent(SyncState.FOR_UPLOAD, SyncState.MODIFIED);
    }

    /**
     * This case happens when a user modified a file to be uploaded while the file is being uploaded.
     * Since the upload would be failed, delete the target file from the sync DB and add it as a new file.
     */
    @Test
    public void testUploadingFileModified() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkStatusAfterModifyEvent(SyncState.UPLOADING, SyncState.MODIFIED);
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

        // Create event.
        try (final FileWatcher watcher = new FileWatcher(Utils.getSyncDir(), executor)) {
            final Path target = Utils.getSyncDir().resolve("test-file1");
            assertTrue(target.toFile().createNewFile());
            DB.addNewFile(target);
            this.updateStatus(target, before);

            new SystemMock();

            SystemMock.currentTime = now;
            watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.CREATE, target, 2));
            SystemMock.currentTime = now + 100;
            watcher.run();

            // in MinElapsedTime, status must not be chanced.
            assertEquals(before, DB.get(target).getState());

            SystemMock.currentTime = now + 2 * FileWatcher.MinElapsedTime;
            watcher.run();

            assertEquals(expected, DB.get(target).getState());
            final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
            assertFalse(trackingFiles.containsKey(target));
            assertTrue(DBMock.committed);
        }

        DBMock.committed = false;

        // Modify event.
        try (final FileWatcher watcher = new FileWatcher(Utils.getSyncDir(), executor)) {
            final Path target = Utils.getSyncDir().resolve("test-file2");
            assertTrue(target.toFile().createNewFile());
            DB.addNewFile(target);
            this.updateStatus(target, before);

            new SystemMock();

            SystemMock.currentTime = now;
            watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.MODIFY, target, 2));
            SystemMock.currentTime = now + 100;
            watcher.run();

            // in MinElapsedTime, status must not be chanced.
            assertEquals(before, DB.get(target).getState());

            SystemMock.currentTime = now + 2 * FileWatcher.MinElapsedTime;
            watcher.run();

            assertEquals(expected, DB.get(target).getState());
            final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
            assertFalse(trackingFiles.containsKey(target));
            assertTrue(DBMock.committed);
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

        final FileWatcher watcher = new FileWatcher(Utils.getSyncDir(), executor);
        final Path target = Utils.getSyncDir().resolve("test-file");

        new SystemMock();

        SystemMock.currentTime = now;
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.CREATE, target, 0));
        watcher.run();
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.MODIFY, target, 1));
        watcher.run();
        watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.DELETE, target, 2));
        SystemMock.currentTime = now + 2 * FileWatcher.MinElapsedTime;
        watcher.run();

        assertFalse(DB.contains(target));
        final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
        assertFalse(trackingFiles.containsKey(target));

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
        try (final FileWatcher watcher = new FileWatcher(Utils.getSyncDir(), executor)) {
            final Path target = Utils.getSyncDir().resolve("test-file1");
            assertTrue(target.toFile().createNewFile());
            DB.addNewFile(target);
            this.updateStatus(target, before);

            watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.DELETE, target, 2));

            assertEquals(expected, DB.get(target).getState());
            final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
            assertFalse(trackingFiles.containsKey(target));
            assertTrue(DBMock.committed);
        }

        DBMock.committed = false;

        // Modified and deleted.
        try (final FileWatcher watcher = new FileWatcher(Utils.getSyncDir(), executor)) {
            final Path target = Utils.getSyncDir().resolve("test-file2");
            assertTrue(target.toFile().createNewFile());
            DB.addNewFile(target);
            this.updateStatus(target, before);

            new SystemMock();

            SystemMock.currentTime = now;
            watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.MODIFY, target, 2));
            SystemMock.currentTime = now + 100;
            watcher.run();
            SystemMock.currentTime = now + 300;
            watcher.onEvent(new DirectoryChangeEvent(DirectoryChangeEvent.EventType.DELETE, target, 2));

            assertEquals(expected, DB.get(target).getState());
            final Map<Path, Long> trackingFiles = Deencapsulation.getField(watcher, "trackingFiles");
            assertFalse(trackingFiles.containsKey(target));
            assertTrue(DBMock.committed);
        }

    }

    @SuppressWarnings("unchecked")
    private void updateStatus(final Path localPath, final SyncState newState) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final SyncFile syncFile = DB.get(localPath);
        Deencapsulation.setField(syncFile, "state", newState);

        final Method repo = DB.class.getDeclaredMethod("repo");
        repo.setAccessible(true);
        final ObjectRepository<SyncFile> repository = (ObjectRepository<SyncFile>) repo.invoke(DB.class);
        repository.update(syncFile);

    }

}