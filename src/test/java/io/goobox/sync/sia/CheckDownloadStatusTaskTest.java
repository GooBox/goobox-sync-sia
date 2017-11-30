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
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.sia.model.SiaFileFromDownloadsAPI;
import io.goobox.sync.storj.Utils;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@RunWith(JMockit.class)
public class CheckDownloadStatusTaskTest {

    private static SimpleDateFormat RFC3339 = new SimpleDateFormat("yyyy-MM-dd'T'h:m:ssZ");

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
    public void cleanUpMockDB() {
        DB.close();
    }

    /**
     * Creates a temporal directory and sets it as the result of CmdUtils.syncDir().
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
    public void testWithNullCollection() throws ApiException {

        final InlineResponse20010 downloads = new InlineResponse20010();
        downloads.setDownloads(null);

        new Expectations() {{
            api.renterDownloadsGet();
            result = downloads;
        }};

        final Config cfg = new Config();
        cfg.setUserName("");
        final Context ctx = new Context(cfg, null);
        new CheckDownloadStatusTask(ctx).run();

    }

    @Test
    public void testDownloadedFiles() throws IOException, ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();
        final Path file1RemotePath = this.context.pathPrefix.resolve("file1");
        final Path file1LocalPath = Utils.getSyncDir().resolve("file1");

        // old entry.
        final InlineResponse20010Downloads file1 = new InlineResponse20010Downloads();
        file1.setSiapath(file1RemotePath.toString());
        file1.setDestination(file1LocalPath.toString());
        file1.setFilesize(100L);
        file1.setReceived(100L);
        file1.setStarttime(RFC3339.format(new Date(10000)));
        files.add(file1);

        // new entry.
        final InlineResponse20010Downloads file1new = new InlineResponse20010Downloads();
        file1new.setSiapath(file1RemotePath.toString());
        file1new.setDestination(file1LocalPath.toString());
        file1new.setFilesize(100L);
        file1new.setReceived(100L);
        file1new.setStarttime(RFC3339.format(new Date()));
        files.add(file1new);

        final SiaFile siaFile1 = new SiaFileFromDownloadsAPI(file1new, this.context.pathPrefix);
        DB.addForDownload(siaFile1);
        final byte[] file1Data = "testdata".getBytes();
        Files.write(DB.get(siaFile1).getTemporaryPath(), file1Data);
        DB.setDownloading(siaFile1);

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;
        }};

        new CheckDownloadStatusTask(this.context).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.SYNCED, DB.get(siaFile1).getState());
        assertTrue(file1LocalPath.toFile().exists());
        assertEquals(DigestUtils.sha512Hex(file1Data), DB.get(siaFile1).getLocalDigest());

    }

    @Test
    public void testDownloadingNewerFile() throws IOException, ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();
        final Path remotePath = this.context.pathPrefix.resolve("file");
        final Path localPath = Utils.getSyncDir().resolve("file");

        // old entry.
        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(remotePath.toString());
        file.setDestination(localPath.toString());
        file.setFilesize(100L);
        file.setReceived(100L);
        file.setStarttime(RFC3339.format(new Date(10000)));
        files.add(file);

        // new entry.
        final InlineResponse20010Downloads newFile = new InlineResponse20010Downloads();
        newFile.setSiapath(remotePath.toString());
        newFile.setDestination(localPath.toString());
        newFile.setFilesize(100L);
        newFile.setReceived(50L);
        newFile.setStarttime(RFC3339.format(new Date()));
        files.add(newFile);

        final SiaFile siaFile = new SiaFileFromDownloadsAPI(newFile, this.context.pathPrefix);
        DB.addForDownload(siaFile);
        DB.setDownloading(siaFile);

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;
        }};

        new CheckDownloadStatusTask(this.context).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.DOWNLOADING, DB.get(siaFile).getState());

    }

    @Test
    public void testStillDownloadingFile() throws IOException, ApiException {

        final Path remotePath = this.context.pathPrefix.resolve("file");
        final Path localPath = Utils.getSyncDir().resolve("file");
        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(remotePath.toString());
        file.setDestination(localPath.toString());
        file.setFilesize(100L);
        file.setReceived(50L);
        file.setStarttime(RFC3339.format(new Date()));

        final SiaFile siaFile = new SiaFileFromDownloadsAPI(file, this.context.pathPrefix);
        DB.addForDownload(siaFile);
        DB.setDownloading(siaFile);

        new Expectations() {{
            final List<InlineResponse20010Downloads> files = new LinkedList<>();
            files.add(file);
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;
        }};

        new CheckDownloadStatusTask(this.context).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.DOWNLOADING, DB.get(siaFile).getState());

    }

    /**
     * Since renter/downloads returns files which were downloaded, results may contain a file to be download.
     * This test checks such files are ignored and their state is kept to FOR_DOWNLOAD.
     */
    @Test
    public void testToBeDownloadedFile() throws IOException, ApiException {

        final Path remotePath = this.context.pathPrefix.resolve("file");
        final Path localPath = Utils.getSyncDir().resolve("file");
        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(remotePath.toString());
        file.setDestination(localPath.toString());
        file.setFilesize(100L);
        file.setReceived(50L);
        file.setStarttime(RFC3339.format(new Date()));
        DB.addForDownload(new SiaFileFromDownloadsAPI(file, this.context.pathPrefix));

        new Expectations() {{
            final List<InlineResponse20010Downloads> files = new LinkedList<>();
            files.add(file);
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;
        }};

        new CheckDownloadStatusTask(this.context).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(localPath).getState());

    }

    @Test
    public void testDownloadFilesInSubDirectory() throws IOException, ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();
        final Path name = Paths.get("subdir", "file");
        final Path fileRemotePath = this.context.pathPrefix.resolve(name);
        final Path fileLocalPath = Utils.getSyncDir().resolve(name);
        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(fileRemotePath.toString());
        file.setDestination(fileLocalPath.toString());
        file.setFilesize(100L);
        file.setReceived(100L);
        file.setStarttime(RFC3339.format(new Date()));
        files.add(file);

        final SiaFile siaFile = new SiaFileFromDownloadsAPI(file, this.context.pathPrefix);
        DB.addForDownload(siaFile);
        final byte[] fileData = "testdata".getBytes();
        Files.write(DB.get(siaFile).getTemporaryPath(), fileData);
        DB.setDownloading(siaFile);

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;
        }};

        new CheckDownloadStatusTask(this.context).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.SYNCED, DB.get(siaFile).getState());
        assertTrue(fileLocalPath.toFile().exists());
        assertEquals(DigestUtils.sha512Hex(fileData), DB.get(siaFile).getLocalDigest());

    }

    @Test
    public void testFailedDownloads() throws IOException, ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        final Path file1RemotePath = this.context.pathPrefix.resolve("file1");
        final Path file1LocalPath = Utils.getSyncDir().resolve("file1");
        final InlineResponse20010Downloads file1 = new InlineResponse20010Downloads();
        file1.setSiapath(file1RemotePath.toString());
        file1.setDestination(file1LocalPath.toString());
        file1.setFilesize(100L);
        file1.setReceived(50L);
        file1.setStarttime(RFC3339.format(new Date()));
        file1.setError("expected error");
        files.add(file1);

        assertTrue(file1LocalPath.toFile().createNewFile());
        final SiaFile siaFile = new SiaFileFromDownloadsAPI(file1, this.context.pathPrefix);
        DB.addForDownload(siaFile);
        DB.setDownloading(siaFile);

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;
        }};

        new CheckDownloadStatusTask(this.context).run();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.DOWNLOAD_FAILED, DB.get(Utils.getSyncDir().resolve("file1")).getState());

    }

    /**
     * Since renter/downloads returns files which were downloaded, results may contain a file failed to be download
     * even if the current download doesn't start. This test checks such files are ignored and their state is kept
     * to FOR_DOWNLOAD.
     */
    @Test
    public void testFailedPendingDownloads() throws IOException, ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        final Path file1RemotePath = this.context.pathPrefix.resolve("file1");
        final Path file1LocalPath = Utils.getSyncDir().resolve("file1");
        final InlineResponse20010Downloads file1 = new InlineResponse20010Downloads();
        file1.setSiapath(file1RemotePath.toString());
        file1.setDestination(file1LocalPath.toString());
        file1.setFilesize(100L);
        file1.setReceived(50L);
        file1.setStarttime(RFC3339.format(new Date()));
        file1.setError("expected error");
        files.add(file1);

        final SiaFile siaFile = new SiaFileFromDownloadsAPI(file1, this.context.pathPrefix);
        DB.addForDownload(siaFile);

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;
        }};

        new CheckDownloadStatusTask(this.context).run();
        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(Utils.getSyncDir().resolve("file1")).getState());
        assertTrue(DBMock.committed);

    }

    @Test
    public void testCreationTimeOfDownloadedFile() throws IOException, ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();
        final Date targetDate = new Date(new Date().getTime() - 10000);

        final Path file1RemotePath = this.context.pathPrefix.resolve(Paths.get("file1", String.valueOf(targetDate.getTime())));
        final Path file1LocalPath = Utils.getSyncDir().resolve("file1");
        final InlineResponse20010Downloads file1 = new InlineResponse20010Downloads();
        file1.setSiapath(file1RemotePath.toString());
        file1.setDestination(file1LocalPath.toString());
        file1.setFilesize(100L);
        file1.setReceived(100L);
        file1.setStarttime(RFC3339.format(targetDate));
        files.add(file1);

        final SiaFile siaFile = new SiaFileFromDownloadsAPI(file1, this.context.pathPrefix);
        DB.addForDownload(siaFile);
        DB.setDownloading(siaFile);

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;
        }};

        new CheckDownloadStatusTask(this.context).run();

        assertEquals(SyncState.SYNCED, DB.get(Utils.getSyncDir().resolve("file1")).getState());
        assertEquals(targetDate.getTime() / 1000, new Date(file1LocalPath.toFile().lastModified()).getTime() / 1000);
        assertTrue(DBMock.committed);

    }

    /**
     * Test a case that a file being downloaded is also created/modified in the local directory. In this case, the
     * downloaded file don't have to be copied to the sync dir and should be deleted. CheckStateTask is responsible for
     * solving the conflict between the local and cloud files.
     */
    @Test
    public void testDownloadingFileModified() throws IOException, ApiException {

        final Path remotePath = this.context.pathPrefix.resolve("file");
        final Path localPath = Utils.getSyncDir().resolve("file");
        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(remotePath.toString());
        file.setDestination(localPath.toString());
        file.setFilesize(100L);
        file.setReceived(50L);
        file.setStarttime(RFC3339.format(new Date()));
        DB.addForDownload(new SiaFileFromDownloadsAPI(file, this.context.pathPrefix));

        final String dummyData = "dummey data";
        Files.write(localPath, dummyData.getBytes(), StandardOpenOption.CREATE);
        DB.setModified(localPath);

        new Expectations() {{
            final List<InlineResponse20010Downloads> files = new LinkedList<>();
            files.add(file);
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;
        }};

        new CheckDownloadStatusTask(this.context).run();
        assertTrue(DBMock.committed);

        final SyncFile syncFile = DB.get(localPath);
        assertEquals(SyncState.MODIFIED, syncFile.getState());

    }

    /**
     * As same as testDownloadingFileModified, test a case that the downloaded file is also created/modified.
     */
    @Test
    public void testDownloadedFileModified() throws IOException, ApiException {

        final Path remotePath = this.context.pathPrefix.resolve("file");
        final Path localPath = Utils.getSyncDir().resolve("file");
        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(remotePath.toString());
        file.setDestination(localPath.toString());
        file.setFilesize(100L);
        file.setReceived(100L);
        file.setStarttime(RFC3339.format(new Date()));
        DB.addForDownload(new SiaFileFromDownloadsAPI(file, this.context.pathPrefix));

        final String dummyData = "dummey data";
        Files.write(localPath, dummyData.getBytes(), StandardOpenOption.CREATE);
        DB.setModified(localPath);

        new Expectations() {{
            final List<InlineResponse20010Downloads> files = new LinkedList<>();
            files.add(file);
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;
        }};

        new CheckDownloadStatusTask(this.context).run();
        assertTrue(DBMock.committed);

        final SyncFile syncFile = DB.get(localPath);
        assertEquals(SyncState.MODIFIED, syncFile.getState());
        assertFalse(syncFile.getTemporaryPath().toFile().exists());
        assertArrayEquals(dummyData.getBytes(), Files.readAllBytes(localPath));

    }

    @Test
    public void testParseDate() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final Method parseDateTime = CheckDownloadStatusTask.class.getDeclaredMethod("parseDateTime", String.class);
        parseDateTime.setAccessible(true);

        final String rfc3339 = "2009-11-10T23:00:00Z";
        final DateTime res1 = ((DateTime) parseDateTime.invoke(null, rfc3339)).toDateTime(DateTimeZone.UTC);
        assertEquals(2009, res1.year().get());
        assertEquals(11, res1.monthOfYear().get());
        assertEquals(10, res1.dayOfMonth().get());
        assertEquals(23, res1.hourOfDay().get());
        assertEquals(0, res1.minuteOfHour().get());
        assertEquals(0, res1.secondOfMinute().get());


        final String rfc3339tz = "2017-11-23T14:42:59.864874-05:00";
        final DateTime res2 = (DateTime) parseDateTime.invoke(null, rfc3339tz);
        assertEquals(2017, res2.year().get());
        assertEquals(11, res2.monthOfYear().get());
        assertEquals(23, res2.dayOfMonth().get());
        assertEquals(14, res2.hourOfDay().get());
        assertEquals(42, res2.minuteOfHour().get());
        assertEquals(59, res2.secondOfMinute().get());

    }

}