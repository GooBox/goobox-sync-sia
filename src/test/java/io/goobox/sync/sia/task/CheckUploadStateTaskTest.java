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

import io.goobox.sync.sia.APIUtils;
import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20011;
import io.goobox.sync.sia.client.api.model.InlineResponse20011Files;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.FileUtils;
import org.dizitart.no2.objects.ObjectRepository;
import org.jetbrains.annotations.NotNull;
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
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
@RunWith(JMockit.class)
public class CheckUploadStateTaskTest {

    @Mocked
    private RenterApi renterApi;

    private Path tmpDir;
    private Context ctx;
    private String name;
    private Path cloudPath;
    private Path localPath;

    @Before
    public void setUp() throws IOException {

        new DBMock();
        this.tmpDir = Files.createTempDirectory(null);

        final Config cfg = new Config(this.tmpDir.resolve(App.ConfigFileName));
        Deencapsulation.setField(cfg, "userName", "test-user");
        Deencapsulation.setField(cfg, "syncDir", this.tmpDir.toAbsolutePath());
        this.ctx = new Context(cfg);

        this.name = String.format("file-%x", System.currentTimeMillis());
        this.cloudPath = this.ctx.getPathPrefix().resolve(this.name).resolve(String.valueOf(System.currentTimeMillis()));
        this.localPath = this.tmpDir.resolve(this.name);
        Files.createFile(localPath);

    }

    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(this.tmpDir.toFile());
    }

    @Test
    public void uploadFile(@Mocked App app) throws ApiException, IOException {

        DB.addNewFile(name, localPath);
        DB.setUploading(name);

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(
                    createCloudFile(1234, 100)
            ));
            renterApi.renterFilesGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);
            app.refreshOverlayIcon(localPath);
        }};

        new CheckUploadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.SYNCED, DB.get(this.name).get().getState());

    }

    @Test
    public void stillUploadingFile() throws IOException, ApiException {

        DB.addNewFile(this.name, this.localPath);
        DB.setUploading(this.name);

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(
                    createCloudFile(1234, 95.2)
            ));
            renterApi.renterFilesGet();
            result = res;
        }};

        new CheckUploadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.UPLOADING, DB.get(this.name).get().getState());

    }

    /**
     * Since renter/files API returns files stored and being uploaded in the sia network,
     * results contain files already synced. This test checks CheckUploadStateTask doesn't modified statuses of such
     * files.
     */
    @Test
    public void uploadedButSyncedFile() throws InvocationTargetException, NoSuchMethodException, ApiException, IllegalAccessException, IOException {
        this.checkStatusAfterExecution(SyncState.SYNCED, SyncState.SYNCED);
    }

    /**
     * Since renter/files API returns files stored and being uploaded in the sia network,
     * results contain files to be uploaded. This test checks CheckUploadStateTask doesn't modified statuses of such
     * files.
     */
    @Test
    public void toBeUploadedFile() throws IOException, ApiException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this.checkStatusAfterExecution(SyncState.FOR_UPLOAD, SyncState.FOR_UPLOAD);
    }

    /**
     * Test the case where an uploading file is also modified. In this case, the file is marked as MODIFIED and
     * CheckUploadStateTask doesn't handle it.
     */
    @Test
    public void uploadingFileModified() throws IOException, ApiException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this.checkStatusAfterExecution(SyncState.MODIFIED, SyncState.MODIFIED);
    }

    /**
     * Test the case where an uploading file is also deleted. In this case, the file should be marked as DELETED and
     * the associated cloud file should deleted.
     */
    @Test
    public void uploadingFileDeleted() throws NoSuchMethodException, ApiException, IOException, InvocationTargetException, IllegalAccessException {

        final String slashedCloudPath = APIUtils.toSlash(cloudPath);
        new Expectations(APIUtils.class) {{
            APIUtils.toSlash(cloudPath);
            result = slashedCloudPath;
            renterApi.renterDeleteSiapathPost(slashedCloudPath);
        }};

        this.checkStatusAfterExecution(SyncState.DELETED, SyncState.DELETED);

    }

    @SuppressWarnings("unchecked")
    private void checkStatusAfterExecution(final SyncState before, final SyncState expected)
            throws IOException, NoSuchMethodException, ApiException, InvocationTargetException, IllegalAccessException {

        DB.addNewFile(this.name, this.localPath);

        final SyncFile syncFile = DB.get(name).get();
        Deencapsulation.setField(syncFile, "state", before);

        final Method repo = DB.class.getDeclaredMethod("repo");
        repo.setAccessible(true);
        final ObjectRepository<SyncFile> repository = (ObjectRepository<SyncFile>) repo.invoke(DB.class);
        repository.update(syncFile);

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(
                    createCloudFile(1234L, 100)
            ));
            renterApi.renterFilesGet();
            result = res;
        }};

        new CheckUploadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);
        assertEquals(expected, DB.get(this.name).get().getState());

    }

    @Test
    public void notManagedFile() throws ApiException {

        new Expectations() {{
            final InlineResponse20011 res = new InlineResponse20011();
            res.setFiles(Collections.singletonList(
                    createCloudFile(1234L, 100)
            ));
            renterApi.renterFilesGet();
            result = res;
        }};

        new CheckUploadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);
        assertFalse(DB.get(this.name).isPresent());

    }

    @NotNull
    private InlineResponse20011Files createCloudFile(long fileSize, double progress) {
        final InlineResponse20011Files file = new InlineResponse20011Files();
        file.setSiapath(this.cloudPath.toString());
        file.setLocalpath(this.localPath.toString());
        file.setFilesize(fileSize);
        file.setUploadprogress(new BigDecimal(progress));
        return file;
    }

}