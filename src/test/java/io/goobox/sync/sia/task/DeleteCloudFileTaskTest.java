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
import io.goobox.sync.sia.client.api.model.FileInfo;
import io.goobox.sync.sia.client.api.model.InlineResponse20012;
import io.goobox.sync.sia.db.CloudFile;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.APIUtilsMock;
import io.goobox.sync.sia.mocks.DBMock;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class DeleteCloudFileTaskTest {

    @SuppressWarnings("unused")
    @Mocked
    private RenterApi api;

    private Path tmpDir;
    private Context ctx;
    private String name;
    private Path localPath;
    private Path cloudPath;

    @Before
    public void setUp() throws IOException {

        new DBMock();
        this.tmpDir = Files.createTempDirectory(null);

        final Config cfg = new Config(this.tmpDir.resolve(App.ConfigFileName));
        Deencapsulation.setField(cfg, "userName", "test-user");
        Deencapsulation.setField(cfg, "syncDir", this.tmpDir.toAbsolutePath());
        this.ctx = new Context(cfg);

        this.name = String.format("test-file-%x", System.currentTimeMillis());
        this.localPath = this.tmpDir.resolve(this.name);
        Files.createFile(localPath);
        this.cloudPath = this.ctx.getPathPrefix().resolve(this.name);

        APIUtilsMock.toSlashPaths.clear();
        new APIUtilsMock();

    }

    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(tmpDir.toFile());
    }

    @Test
    public void deleteCloudFiles() throws IOException, ApiException {

        final List<FileInfo> files = new ArrayList<>();
        final List<String> siaPaths = new ArrayList<>();

        IntStream.range(1, 3).forEach(i -> {

            final FileInfo file = new FileInfo();
            final String siaPath = APIUtils.toSlash(cloudPath.resolve(String.valueOf(new Date(i * 10000).getTime())));
            file.setSiapath(siaPath);
            file.setLocalpath(localPath.toString());
            file.setAvailable(true);
            siaPaths.add(siaPath);
            files.add(file);

        });

        final CloudFile cloudFile = new CloudFile() {
            @NotNull
            @Override
            public String getName() {
                return name;
            }

            @NotNull
            @Override
            public Path getCloudPath() {
                return Paths.get(siaPaths.get(siaPaths.size() - 1));
            }

            @Override
            public long getFileSize() {
                return 0;
            }
        };
        DB.setSynced(cloudFile, this.localPath);
        Files.deleteIfExists(localPath);
        DB.setForCloudDelete(cloudFile);

        new Expectations() {{
            final InlineResponse20012 list = new InlineResponse20012();
            list.setFiles(files);
            api.renterFilesGet();
            result = list;
            siaPaths.forEach(siaPath -> {
                try {
                    api.renterDeleteSiapathPost(siaPath);
                } catch (ApiException e) {
                    throw new RuntimeException(e);
                }
            });
        }};

        APIUtilsMock.toSlashPaths.clear();

        new DeleteCloudFileTask(this.ctx, this.name).call();
        assertTrue(DBMock.committed);
        assertFalse(DB.get(this.name).isPresent());

        assertEquals(siaPaths.size(), APIUtilsMock.toSlashPaths.size());

        for (int i = 0; i != siaPaths.size(); i++) {
            assertEquals(Paths.get(siaPaths.get(i)), APIUtilsMock.toSlashPaths.get(i));
        }

    }

    @Test
    public void deleteNotExistingFile() throws IOException, ApiException {

        final FileInfo file = this.createCloudFile(new Date(30000), 1234);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        DB.setSynced(siaFile, this.localPath);
        DB.setForCloudDelete(siaFile);

        new Expectations() {{
            final InlineResponse20012 list = new InlineResponse20012();
            list.setFiles(Collections.emptyList());
            api.renterFilesGet();
            result = list;
        }};

        new DeleteCloudFileTask(this.ctx, siaFile.getName()).call();

    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void toBeCloudDeleteFileModified() throws Exception {

        final FileInfo file = this.createCloudFile(new Date(30000), 1235);
        final SiaFile siaFile = new SiaFileFromFilesAPI(this.ctx, file);
        DB.setSynced(siaFile, this.localPath);
        DB.setForCloudDelete(siaFile);

        // expecting the api won't be called.
        new Expectations() {{
            final InlineResponse20012 list = new InlineResponse20012();
            list.setFiles(Collections.singletonList(file));
            api.renterFilesGet();
            result = list;
            times = 0;
        }};

        // a delete task is enqueued.
        final Callable<Void> task = new DeleteCloudFileTask(this.ctx, siaFile.getName());

        // the target is modified.
        DB.setModified(this.name, this.localPath);

        // the task is executed.
        task.call();

        // check after conditions.
        assertEquals(SyncState.MODIFIED, DB.get(this.name).get().getState());

    }

    private FileInfo createCloudFile(final Date lastModified, final long size) {

        final FileInfo file = new FileInfo();
        file.setSiapath(this.cloudPath.resolve(String.valueOf(lastModified.getTime())).toString());
        file.setLocalpath(this.localPath.toString());
        file.setAvailable(true);
        file.setFilesize(size);
        return file;

    }

}