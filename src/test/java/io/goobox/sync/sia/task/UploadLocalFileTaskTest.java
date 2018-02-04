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

import io.goobox.sync.common.overlay.OverlayHelper;
import io.goobox.sync.sia.APIUtils;
import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import mockit.Deencapsulation;
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
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
@RunWith(JMockit.class)
public class UploadLocalFileTaskTest {

    @Mocked
    private App app;

    @Mocked
    private OverlayHelper overlayHelper;

    @Mocked
    private RenterApi api;

    private Path tmpDir;
    private Config cfg;
    private Context context;
    private String name;
    private Path localPath;
    private Path cloudPath;
    private String slashedLocalPath;
    private String slashedCloudPath;

    /**
     * Creates a temporal directory and sets it as the result of CmdUtils.syncDir().
     *
     * @throws IOException if failed to create a temporary directory.
     */
    @Before
    public void setUp() throws IOException {

        new DBMock();
        this.tmpDir = Files.createTempDirectory(null);

        this.cfg = new Config(this.tmpDir.resolve(App.ConfigFileName));
        Deencapsulation.setField(this.cfg, "userName", "test-user");
        Deencapsulation.setField(this.cfg, "dataPieces", 120);
        Deencapsulation.setField(this.cfg, "parityPieces", 50);
        Deencapsulation.setField(this.cfg, "syncDir", this.tmpDir.toAbsolutePath());
        this.context = new Context(this.cfg, null);

        this.name = String.format("test-file-%x", System.currentTimeMillis());
        this.localPath = this.tmpDir.resolve(this.name);
        this.cloudPath = this.context.getPathPrefix().resolve(this.name).resolve(String.valueOf(System.currentTimeMillis()));
        Files.createFile(this.localPath);
        DB.addNewFile(this.name, this.localPath);
        DB.setForUpload(this.name, this.localPath, this.cloudPath);

        this.slashedLocalPath = APIUtils.toSlash(this.localPath);
        this.slashedCloudPath = APIUtils.toSlash(this.cloudPath);

    }

    /**
     * Deletes the temporary directory.
     *
     * @throws IOException if failed to delete it.
     */
    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(this.tmpDir.toFile());
    }

    @Test
    public void uploadFile() throws ApiException {

        new Expectations(APIUtils.class) {{
            APIUtils.toSlash(localPath);
            result = slashedLocalPath;

            APIUtils.toSlash(cloudPath);
            result = slashedCloudPath;

            api.renterUploadSiapathPost(slashedCloudPath, cfg.getDataPieces(), cfg.getParityPieces(), slashedLocalPath);

            App.getInstance();
            result = Optional.of(app);

            app.getOverlayHelper();
            result = overlayHelper;

            overlayHelper.refresh(localPath);
        }};
        new UploadLocalFileTask(this.context, this.localPath).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.UPLOADING, DB.get(this.name).get().getState());

    }

    /**
     * Test the case where after retrying to upload the files MaxRetry times, set UPLOAD_FAILED.
     */
    @Test
    public void failedToUpload() throws ApiException {

        new Expectations(APIUtils.class) {{
            APIUtils.toSlash(cloudPath);
            result = slashedCloudPath;

            APIUtils.toSlash(localPath);
            result = slashedLocalPath;

            api.renterUploadSiapathPost(slashedCloudPath, cfg.getDataPieces(), cfg.getParityPieces(), slashedLocalPath);
            result = new ApiException();
            times = UploadLocalFileTask.MaxRetry;

            App.getInstance();
            result = Optional.of(app);

            app.getOverlayHelper();
            result = overlayHelper;

            overlayHelper.refresh(localPath);
        }};
        new UploadLocalFileTask(this.context, this.localPath).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.UPLOAD_FAILED, DB.get(this.name).get().getState());

    }

    /**
     * Test the case where a file is modified while it is waiting to start uploading.
     * In this case, upload should be canceled and delegate CheckStateTask to decide uploading the new file or not.
     */
    @Test
    public void toBeUploadedFileModified() throws IOException, ApiException {

        // Expecting the api won't be called.
        new Expectations() {{
            api.renterUploadSiapathPost(slashedCloudPath, cfg.getDataPieces(), cfg.getParityPieces(), slashedLocalPath);
            times = 0;

            App.getInstance();
            times = 0;
        }};

        // Create a upload local file task.
        final UploadLocalFileTask task = new UploadLocalFileTask(this.context, this.localPath);

        // then, the target file is modified.
        DB.setModified(this.name, this.localPath);

        // and, the task is executed.
        task.call();

        // check after conditions.
        assertEquals(SyncState.MODIFIED, DB.get(this.name).get().getState());

    }

    /**
     * Test the cast where a file is deleted while it is waiting to start uploading.
     * In this case, upload must be canceled.
     */
    @Test
    public void toBeUploadedFileDeleted() throws ApiException {

        // Expecting the api won't be called.
        new Expectations() {{
            api.renterUploadSiapathPost(slashedCloudPath, cfg.getDataPieces(), cfg.getParityPieces(), slashedLocalPath);
            times = 0;

            App.getInstance();
            times = 0;
        }};

        // Create a upload local file task.
        final UploadLocalFileTask task = new UploadLocalFileTask(this.context, this.localPath);

        // then, the target file is modified.
        DB.setDeleted(this.name);

        // and, the task is executed.
        task.call();

        // check after conditions.
        assertFalse(DBMock.committed);
        assertEquals(SyncState.DELETED, DB.get(this.name).get().getState());

    }

    /**
     * Test the case where an exception thrown because an old broken file occupies the sia path where the new uploading
     * file will be stored. In this case, delete the old file and retry to upload.
     */
    @Test
    public void retryUpload() throws ApiException {

        new Expectations(APIUtils.class) {{
            APIUtils.toSlash(localPath);
            result = slashedLocalPath;

            APIUtils.toSlash(cloudPath);
            result = slashedCloudPath;

            api.renterUploadSiapathPost(slashedCloudPath, cfg.getDataPieces(), cfg.getParityPieces(), slashedLocalPath);
            // At first time, the API call throws an exception.
            result = new ApiException();
            result = null;

            api.renterDeleteSiapathPost(slashedCloudPath);

            App.getInstance();
            result = Optional.of(app);

            app.getOverlayHelper();
            result = overlayHelper;

            overlayHelper.refresh(localPath);
        }};
        new UploadLocalFileTask(this.context, this.localPath).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.UPLOADING, DB.get(this.name).get().getState());

    }


}