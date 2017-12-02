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
    private Path cloudPath;
    private Path localPath;


    @Before
    public void setUp() throws IOException {

        new DBMock();

        tempDir = Files.createTempDirectory(null);
        UtilsMock.syncDir = tempDir;
        new UtilsMock();

        final Config cfg = new Config();
        cfg.setUserName("test-user");
        this.context = new Context(cfg, null);

        final String name = String.format("file-%x", System.currentTimeMillis());
        this.cloudPath = this.context.pathPrefix.resolve(name).resolve(String.valueOf(System.currentTimeMillis()));
        this.localPath = Utils.getSyncDir().resolve(name);
        assertTrue(this.localPath.toFile().createNewFile());

    }

    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    @Test
    public void uploadFile() throws ApiException, IOException {

        final List<InlineResponse20011Files> files = new ArrayList<>();

        final InlineResponse20011Files file1 = new InlineResponse20011Files();
        file1.setSiapath(cloudPath.toString());
        file1.setLocalpath(localPath.toString());
        file1.setFilesize(1234L);
        file1.setUploadprogress(new BigDecimal(100));

        DB.addNewFile(localPath);
        DB.setUploading(localPath);
        files.add(file1);

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        new CheckUploadStatusTask(this.context).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.SYNCED, DB.get(localPath).getState());

    }

    @Test
    public void stillUploadingFile() throws IOException, ApiException {

        final List<InlineResponse20011Files> files = new ArrayList<>();

        final InlineResponse20011Files file2 = new InlineResponse20011Files();
        file2.setSiapath(cloudPath.toString());
        file2.setLocalpath(localPath.toString());
        file2.setFilesize(1234L);
        file2.setUploadprogress(new BigDecimal(95.2));
        DB.addNewFile(localPath);
        DB.setUploading(localPath);
        files.add(file2);

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(files);
            api.renterFilesGet();
            result = res;
        }};

        new CheckUploadStatusTask(this.context).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.UPLOADING, DB.get(localPath).getState());

    }

    /**
     * Since renter/files API returns files stored and being uploaded in the SIA network,
     * results contain files already synced. This test checks CheckUploadStatusTask doesn't modified statuses of such
     * files.
     */
    @Test
    public void uploadedButSyncedFile() throws InvocationTargetException, NoSuchMethodException, ApiException, IllegalAccessException, IOException {
        this.checkStatusAfterExecution(SyncState.SYNCED, SyncState.SYNCED);
    }

    /**
     * Since renter/files API returns files stored and being uploaded in the SIA network,
     * results contain files to be uploaded. This test checks CheckUploadStatusTask doesn't modified statuses of such
     * files.
     */
    @Test
    public void toBeUploadedFile() throws IOException, ApiException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this.checkStatusAfterExecution(SyncState.FOR_UPLOAD, SyncState.FOR_UPLOAD);
    }

    /**
     * Test a case that an uploading file is also modified. In this case, the file is marked as MODIFIED and
     * CheckUploadStatusTask doesn't handle it.
     */
    @Test
    public void uploadingFileModified() throws IOException, ApiException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this.checkStatusAfterExecution(SyncState.MODIFIED, SyncState.MODIFIED);
    }

    /**
     * Test a case that an uploading file is also deleted. In this case, the file is marked as DELETED and
     * CheckUploadStatusTask doesn't handle it.
     */
    @Test
    public void uploadingFileDeleted() throws NoSuchMethodException, ApiException, IOException, InvocationTargetException, IllegalAccessException {
        this.checkStatusAfterExecution(SyncState.DELETED, SyncState.DELETED);
    }

    @SuppressWarnings("unchecked")
    private void checkStatusAfterExecution(final SyncState before, final SyncState expected)
            throws IOException, NoSuchMethodException, ApiException, InvocationTargetException, IllegalAccessException {

        final List<InlineResponse20011Files> files = new ArrayList<>();

        final InlineResponse20011Files file = new InlineResponse20011Files();
        file.setSiapath(cloudPath.toString());
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
    public void notManagedFile() throws IOException, ApiException {

        final List<InlineResponse20011Files> files = new ArrayList<>();

        final InlineResponse20011Files file = new InlineResponse20011Files();
        file.setSiapath(cloudPath.toString());
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
    public void deletedFromDBFile() throws IOException, ApiException {

        final List<InlineResponse20011Files> files = new ArrayList<>();

        final InlineResponse20011Files file = new InlineResponse20011Files();
        file.setSiapath(cloudPath.toString());
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