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
import io.goobox.sync.sia.client.api.model.InlineResponse20010;
import io.goobox.sync.sia.client.api.model.InlineResponse20010Downloads;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.sia.model.SiaFileFromDownloadsAPI;
import io.goobox.sync.storj.Utils;
import io.goobox.sync.storj.db.SyncState;
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
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;


@RunWith(JMockit.class)
public class CheckDownloadStatusTaskTest {

    @Mocked
    private RenterApi api;

    private Path tempDir;

    private static SimpleDateFormat RFC3339 = new SimpleDateFormat("yyyy-MM-dd'T'h:m:ssZ");

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
    public void testWithNullCollection() throws ApiException {

        final InlineResponse20010 downloads = new InlineResponse20010();
        downloads.setDownloads(null);

        new Expectations() {{
            api.renterDownloadsGet();
            result = downloads;
        }};

        final Config cfg = new Config();
        final Context ctx = new Context(cfg, null);
        new CheckDownloadStatusTask(ctx).run();

    }

    @Test
    public void testDownloadingFiles() throws IOException, ApiException {

        final Config cfg = new Config();
        cfg.userName = "testuser";
        final Context ctx = new Context(cfg, null);

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        // Test files are
        // - one finished file which has two entries (file1)
        // - one downloading file which also has one old finished entry (file2)
        // - one downloading file (file3)

        {
            // file 1, old entry.
            final Path file1RemotePath = ctx.pathPrefix.resolve("file1");
            final Path file1LocalPath = Utils.getSyncDir().resolve("file1");
            final InlineResponse20010Downloads file1 = new InlineResponse20010Downloads();
            file1.setSiapath(file1RemotePath.toString());
            file1.setDestination(file1LocalPath.toString());
            file1.setFilesize(100L);
            file1.setReceived(100L);
            file1.setStarttime(RFC3339.format(new Date(10000)));
            files.add(file1);

            // file 1, new entry.
            final InlineResponse20010Downloads file1new = new InlineResponse20010Downloads();
            file1new.setSiapath(file1RemotePath.toString());
            file1new.setDestination(file1LocalPath.toString());
            file1new.setFilesize(100L);
            file1new.setReceived(100L);
            file1new.setStarttime(RFC3339.format(new Date()));
            files.add(file1new);

            file1LocalPath.toFile().createNewFile();
            DB.addForDownload(new SiaFileFromDownloadsAPI(file1new, ctx.pathPrefix));
        }


        {
            // file 2, old entry.
            final Path file2RemotePath = ctx.pathPrefix.resolve("file2");
            final Path file2LocalPath = Utils.getSyncDir().resolve("file2");
            final InlineResponse20010Downloads file2 = new InlineResponse20010Downloads();
            file2.setSiapath(file2RemotePath.toString());
            file2.setDestination(file2LocalPath.toString());
            file2.setFilesize(100L);
            file2.setReceived(100L);
            file2.setStarttime(RFC3339.format(new Date(10000)));
            files.add(file2);

            // file 2, new entry.
            final InlineResponse20010Downloads file2new = new InlineResponse20010Downloads();
            file2new.setSiapath(file2RemotePath.toString());
            file2new.setDestination(file2LocalPath.toString());
            file2new.setFilesize(100L);
            file2new.setReceived(50L);
            file2new.setStarttime(RFC3339.format(new Date()));
            files.add(file2new);

            file2LocalPath.toFile().createNewFile();
            DB.addForDownload(new SiaFileFromDownloadsAPI(file2new, ctx.pathPrefix));
        }

        {
            // file 3.
            final Path file3RemotePath = ctx.pathPrefix.resolve("file3");
            final Path file3LocalPath = Utils.getSyncDir().resolve("file3");
            final InlineResponse20010Downloads file3 = new InlineResponse20010Downloads();
            file3.setSiapath(file3RemotePath.toString());
            file3.setDestination(file3LocalPath.toString());
            file3.setFilesize(100L);
            file3.setReceived(50L);
            file3.setStarttime(RFC3339.format(new Date()));
            files.add(file3);

            file3LocalPath.toFile().createNewFile();
            DB.addForDownload(new SiaFileFromDownloadsAPI(file3, ctx.pathPrefix));
        }

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;
        }};

        new CheckDownloadStatusTask(ctx).run();

        assertEquals(SyncState.SYNCED, DB.get(Utils.getSyncDir().resolve("file1")).getState());
        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(Utils.getSyncDir().resolve("file2")).getState());
        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(Utils.getSyncDir().resolve("file3")).getState());

    }

    @Test
    public void testFailedDownloads() throws IOException, ApiException {

        final Config cfg = new Config();
        cfg.userName = "testuser";
        final Context ctx = new Context(cfg, null);

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        final Path file1RemotePath = ctx.pathPrefix.resolve("file1");
        final Path file1LocalPath = Utils.getSyncDir().resolve("file1");
        final InlineResponse20010Downloads file1 = new InlineResponse20010Downloads();
        file1.setSiapath(file1RemotePath.toString());
        file1.setDestination(file1LocalPath.toString());
        file1.setFilesize(100L);
        file1.setReceived(50L);
        file1.setStarttime(RFC3339.format(new Date()));
        file1.setError("expected error");
        files.add(file1);

        file1LocalPath.toFile().createNewFile();
        DB.addForDownload(new SiaFileFromDownloadsAPI(file1, ctx.pathPrefix));

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;
        }};

        new CheckDownloadStatusTask(ctx).run();
        assertEquals(SyncState.DOWNLOAD_FAILED, DB.get(Utils.getSyncDir().resolve("file1")).getState());

    }

    @Test
    public void testCreationTimeOfDownloadedFile() throws IOException, ApiException {

        final Config cfg = new Config();
        cfg.userName = "testuser";
        final Context ctx = new Context(cfg, null);

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        final Date targetDate = new Date(new Date().getTime() - 10000);

        final Path file1RemotePath = ctx.pathPrefix.resolve(Paths.get("file1", String.valueOf(targetDate.getTime())));
        final Path file1LocalPath = Utils.getSyncDir().resolve("file1");
        final InlineResponse20010Downloads file1 = new InlineResponse20010Downloads();
        file1.setSiapath(file1RemotePath.toString());
        file1.setDestination(file1LocalPath.toString());
        file1.setFilesize(100L);
        file1.setReceived(100L);
        file1.setStarttime(RFC3339.format(targetDate));
        files.add(file1);

        file1LocalPath.toFile().createNewFile();
        DB.addForDownload(new SiaFileFromDownloadsAPI(file1, ctx.pathPrefix));

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;
        }};

        new CheckDownloadStatusTask(ctx).run();

        assertEquals(SyncState.SYNCED, DB.get(Utils.getSyncDir().resolve("file1")).getState());
        assertEquals(targetDate.getTime() / 1000, new Date(file1LocalPath.toFile().lastModified()).getTime() / 1000);

    }

}