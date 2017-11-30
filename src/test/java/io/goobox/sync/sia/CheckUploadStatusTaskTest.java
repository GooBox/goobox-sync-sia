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
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.storj.Utils;
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
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class CheckUploadStatusTaskTest {

    @SuppressWarnings("unused")
    @Mocked
    private RenterApi api;

    private Path tempDir;
    private Context context;

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
    public void setUp() throws IOException {

        tempDir = Files.createTempDirectory(null);
        UtilsMock.syncDir = tempDir;
        new UtilsMock();

        final Config cfg = new Config();
        cfg.setUserName("testuser");
        this.context = new Context(cfg, null);

    }

    /**
     * Deletes the temporary directory.
     *
     * @throws IOException if failed to delete it.
     */
    @After
    public void tearDown() throws IOException {

        if (tempDir != null && tempDir.toFile().exists()) {
            FileUtils.deleteDirectory(tempDir.toFile());
        }

    }

    @Test
    public void testUploadFile() throws ApiException, IOException {

        final List<InlineResponse20011Files> files = new ArrayList<>();

        final Path remotePath1 = this.context.pathPrefix.resolve("file1");
        final Path localPath1 = Utils.getSyncDir().resolve("file1");
        assertTrue(localPath1.toFile().createNewFile());

        final InlineResponse20011Files file1 = new InlineResponse20011Files();
        file1.setSiapath(remotePath1.toString());
        file1.setLocalpath(localPath1.toString());
        file1.setFilesize(1234L);
        file1.setUploadprogress(new BigDecimal(100));

        DB.addNewFile(localPath1);
        DB.setUploading(localPath1);
        files.add(file1);

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        new CheckUploadStatusTask(this.context).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.SYNCED, DB.get(localPath1).getState());

    }

    @Test
    public void testStillUploadingFile() throws IOException, ApiException {

        final List<InlineResponse20011Files> files = new ArrayList<>();

        final Path remotePath2 = this.context.pathPrefix.resolve("file2");
        final Path localPath2 = Utils.getSyncDir().resolve("file2");
        assertTrue(localPath2.toFile().createNewFile());

        final InlineResponse20011Files file2 = new InlineResponse20011Files();
        file2.setSiapath(remotePath2.toString());
        file2.setLocalpath(localPath2.toString());
        file2.setFilesize(1234L);
        file2.setUploadprogress(new BigDecimal(95.2));
        DB.addNewFile(localPath2);
        DB.setUploading(localPath2);
        files.add(file2);

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        new CheckUploadStatusTask(this.context).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.UPLOADING, DB.get(localPath2).getState());

    }

    /**
     * Since renter/files API returns files stored and being uploaded in the SIA network,
     * results contain files already synced. This test checks CheckUploadStatusTask doesn't modified statuses of such
     * files.
     */
    @Test
    public void testUploadedButSyncedFile() throws InvocationTargetException, NoSuchMethodException, ApiException, IllegalAccessException, IOException {
        this.checkStatusAfterExecution(SyncState.SYNCED, SyncState.SYNCED);
    }

    /**
     * Since renter/files API returns files stored and being uploaded in the SIA network,
     * results contain files to be uploaded. This test checks CheckUploadStatusTask doesn't modified statuses of such
     * files.
     */
    @Test
    public void testToBeUploadedFile() throws IOException, ApiException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this.checkStatusAfterExecution(SyncState.FOR_UPLOAD, SyncState.FOR_UPLOAD);
    }

    /**
     * Test a case that an uploading file is also modified. In this case, the file is marked as MODIFIED and
     * CheckUploadStatusTask doesn't handle it.
     */
    @Test
    public void testUploadingFileModified() throws IOException, ApiException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this.checkStatusAfterExecution(SyncState.MODIFIED, SyncState.MODIFIED);
    }

    /**
     * Test a case that an uploading file is also deleted. In this case, the file is marked as DELETED and
     * CheckUploadStatusTask doesn't handle it.
     */
    @Test
    public void testUploadingFileDeleted() throws NoSuchMethodException, ApiException, IOException, InvocationTargetException, IllegalAccessException {
        this.checkStatusAfterExecution(SyncState.DELETED, SyncState.DELETED);
    }

    @SuppressWarnings("unchecked")
    private void checkStatusAfterExecution(final SyncState before, final SyncState expected)
            throws IOException, NoSuchMethodException, ApiException, InvocationTargetException, IllegalAccessException {

        final List<InlineResponse20011Files> files = new ArrayList<>();
        final String fileName = "test-file";
        final Path remotePath = this.context.pathPrefix.resolve(fileName);
        final Path localPath = Utils.getSyncDir().resolve(fileName);
        assertTrue(localPath.toFile().createNewFile());

        final InlineResponse20011Files file = new InlineResponse20011Files();
        file.setSiapath(remotePath.toString());
        file.setLocalpath(localPath.toString());
        file.setFilesize(1234L);
        file.setUploadprogress(new BigDecimal(100L));
        DB.addNewFile(localPath);

        final SyncFile syncFile = DB.get(localPath);
        Deencapsulation.setField(syncFile, "state", before);

        final Method repo = DB.class.getDeclaredMethod("repo");
        repo.setAccessible(true);
        final ObjectRepository<SyncFile> repository = (ObjectRepository<SyncFile>) repo.invoke(DB.class);
        repository.update(syncFile);

        files.add(file);
        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        new CheckUploadStatusTask(this.context).run();
        assertTrue(DBMock.committed);
        assertEquals(expected, DB.get(localPath).getState());

    }

    @Test
    public void testNotManagedFile() throws IOException, ApiException {

        final List<InlineResponse20011Files> files = new ArrayList<>();

        final Path remotePath = Paths.get("file1");
        final Path localPath = Utils.getSyncDir().resolve("file1");
        assertTrue(localPath.toFile().createNewFile());

        final InlineResponse20011Files file = new InlineResponse20011Files();
        file.setSiapath(remotePath.toString());
        file.setLocalpath(localPath.toString());
        file.setFilesize(1234L);
        file.setUploadprogress(new BigDecimal(100));

        files.add(file);

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        new CheckUploadStatusTask(this.context).run();
        assertTrue(DBMock.committed);
        assertFalse(DB.contains(localPath));

    }

    @Test
    public void testDeletedFromDBFile() throws IOException, ApiException {

        final List<InlineResponse20011Files> files = new ArrayList<>();

        final Path remotePath = this.context.pathPrefix.resolve("file1");
        final Path localPath = Utils.getSyncDir().resolve("file1");
        assertTrue(localPath.toFile().createNewFile());

        final InlineResponse20011Files file = new InlineResponse20011Files();
        file.setSiapath(remotePath.toString());
        file.setLocalpath(localPath.toString());
        file.setFilesize(1234L);
        file.setUploadprogress(new BigDecimal(100));

        files.add(file);

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        new CheckUploadStatusTask(this.context).run();
        assertTrue(DBMock.committed);
        assertFalse(DB.contains(localPath));

    }

}