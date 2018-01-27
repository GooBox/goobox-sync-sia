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
import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20010;
import io.goobox.sync.sia.client.api.model.InlineResponse20010Downloads;
import io.goobox.sync.sia.db.CloudFile;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
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
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
@RunWith(JMockit.class)
public class CheckDownloadStateTaskTest {

    private static SimpleDateFormat RFC3339 = new SimpleDateFormat("yyyy-MM-dd'T'h:m:ssZ");

    @Mocked
    private App app;

    @Mocked
    private OverlayHelper overlayHelper;

    @Mocked
    private RenterApi api;

    private Path tmpDir;
    private Context ctx;
    private String name;
    private Path localPath;
    private SyncFile syncFile;

    @Before
    public void setUp() throws IOException {

        new DBMock();
        this.tmpDir = Files.createTempDirectory(null);

        final Config cfg = new Config(this.tmpDir.resolve(App.ConfigFileName));
        Deencapsulation.setField(cfg, "userName", "test-user");
        Deencapsulation.setField(cfg, "syncDir", this.tmpDir.toAbsolutePath());
        this.ctx = new Context(cfg, null);

        this.name = String.format("file-%x", System.currentTimeMillis());
        final Path cloudPath = this.ctx.getPathPrefix().resolve(this.name);
        this.localPath = this.tmpDir.resolve(this.name);
        DB.addForDownload(new CloudFile() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Path getCloudPath() {
                return cloudPath;
            }

            @Override
            public long getFileSize() {
                return 1234L;
            }
        }, localPath);
        this.syncFile = DB.get(this.name).get();

    }

    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(this.tmpDir.toFile());
    }

    @Test
    public void apiReturnsNullCollection() throws ApiException {

        final InlineResponse20010 downloads = new InlineResponse20010();
        downloads.setDownloads(null);

        new Expectations() {{
            api.renterDownloadsGet();
            result = downloads;
        }};

        new CheckDownloadStateTask(this.ctx).call();

    }

    @Test
    public void downloadedFiles() throws IOException, ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        // old entry.
        final InlineResponse20010Downloads oldFile = new InlineResponse20010Downloads();
        oldFile.setSiapath(syncFile.getCloudPath().get().toString());
        oldFile.setDestination("some temporary path");
        oldFile.setFilesize(syncFile.getCloudSize().get());
        oldFile.setReceived(syncFile.getCloudSize().get());
        oldFile.setStarttime(RFC3339.format(new Date(10000)));
        files.add(oldFile);

        // new entry.
        final InlineResponse20010Downloads newFile = new InlineResponse20010Downloads();
        newFile.setSiapath(syncFile.getCloudPath().get().toString());
        newFile.setDestination(syncFile.getTemporaryPath().get().toString());
        newFile.setFilesize(syncFile.getCloudSize().get());
        newFile.setReceived(syncFile.getCloudSize().get());
        newFile.setStarttime(RFC3339.format(new Date()));
        files.add(newFile);

        final byte[] data = "test-data".getBytes();
        Files.write(syncFile.getTemporaryPath().get(), data);
        DB.setDownloading(syncFile.getName());

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);

            app.getOverlayHelper();
            result = overlayHelper;

            overlayHelper.refresh(localPath);
        }};

        new CheckDownloadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.SYNCED, DB.get(syncFile.getName()).get().getState());
        assertTrue(localPath.toFile().exists());
        assertArrayEquals(data, Files.readAllBytes(localPath));
        assertEquals(DigestUtils.sha512Hex(data), DB.get(syncFile.getName()).get().getLocalDigest().get());

    }

    @Test
    public void downloadingNewerFile() throws ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        // old entry.
        final InlineResponse20010Downloads oldFile = new InlineResponse20010Downloads();
        oldFile.setSiapath(syncFile.getCloudPath().get().toString());
        oldFile.setDestination("some temporary path");
        oldFile.setFilesize(syncFile.getCloudSize().get());
        oldFile.setReceived(syncFile.getCloudSize().get());
        oldFile.setStarttime(RFC3339.format(new Date(10000)));
        files.add(oldFile);

        // new entry.
        final InlineResponse20010Downloads newFile = new InlineResponse20010Downloads();
        newFile.setSiapath(syncFile.getCloudPath().get().toString());
        newFile.setDestination(syncFile.getTemporaryPath().get().toString());
        newFile.setFilesize(syncFile.getCloudSize().get());
        newFile.setReceived(syncFile.getCloudSize().get() / 2);
        newFile.setStarttime(RFC3339.format(new Date()));
        files.add(newFile);

        DB.setDownloading(syncFile.getName());

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;

            App.getInstance();
            times = 0;
        }};

        new CheckDownloadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.DOWNLOADING, DB.get(syncFile.getName()).get().getState());
        assertFalse(localPath.toFile().exists());

    }

    @Test
    public void stillDownloadingFile() throws ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(syncFile.getCloudPath().get().toString());
        file.setDestination(syncFile.getTemporaryPath().get().toString());
        file.setFilesize(syncFile.getCloudSize().get());
        file.setReceived(syncFile.getCloudSize().get() / 2);
        file.setStarttime(RFC3339.format(new Date()));
        files.add(file);

        DB.setDownloading(syncFile.getName());

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;

            App.getInstance();
            times = 0;
        }};

        new CheckDownloadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.DOWNLOADING, DB.get(syncFile.getName()).get().getState());
        assertFalse(localPath.toFile().exists());

    }

    /**
     * Since renter/downloads returns files which were downloaded, results may contain a file to be download.
     * This test checks such files are ignored and their state is kept to FOR_DOWNLOAD.
     */
    @Test
    public void toBeDownloadedFile() throws ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(syncFile.getCloudPath().get().toString());
        file.setDestination(syncFile.getTemporaryPath().get().toString());
        file.setFilesize(syncFile.getCloudSize().get());
        file.setReceived(syncFile.getCloudSize().get() / 2);
        file.setStarttime(RFC3339.format(new Date()));
        files.add(file);

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;

            App.getInstance();
            times = 0;
        }};

        new CheckDownloadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(syncFile.getName()).get().getState());
        assertFalse(localPath.toFile().exists());

    }

    @Test
    public void downloadFilesInSubDirectory() throws IOException, ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();
        final Path name = Paths.get("sub-dir", "file");
        final Path cloudPath = this.ctx.getPathPrefix().resolve(name);
        final Path localPath = this.tmpDir.resolve(name);
        final CloudFile cloudFile = new CloudFile() {
            @Override
            public String getName() {
                return name.toString();
            }

            @Override
            public Path getCloudPath() {
                return cloudPath;
            }

            @Override
            public long getFileSize() {
                return 100L;
            }
        };
        DB.addForDownload(cloudFile, localPath);

        final SyncFile syncFile = DB.get(name.toString()).get();
        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(cloudPath.toString());
        file.setDestination(syncFile.getTemporaryPath().get().toString());
        file.setFilesize(syncFile.getCloudSize().get());
        file.setReceived(syncFile.getCloudSize().get());
        file.setStarttime(RFC3339.format(new Date()));
        files.add(file);

        final byte[] fileData = "test-data".getBytes();
        Files.write(syncFile.getTemporaryPath().get(), fileData);
        DB.setDownloading(syncFile.getName());

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);

            app.getOverlayHelper();
            result = overlayHelper;

            overlayHelper.refresh(localPath);
        }};

        new CheckDownloadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.SYNCED, DB.get(syncFile.getName()).get().getState());
        assertTrue(localPath.toFile().exists());
        assertEquals(DigestUtils.sha512Hex(fileData), DB.get(syncFile.getName()).get().getLocalDigest().get());

    }

    @Test
    public void failedDownloads() throws ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        final InlineResponse20010Downloads file1 = new InlineResponse20010Downloads();
        file1.setSiapath(syncFile.getCloudPath().get().toString());
        file1.setDestination(syncFile.getTemporaryPath().get().toString());
        file1.setFilesize(syncFile.getCloudSize().get());
        file1.setReceived(syncFile.getCloudSize().get() / 2);
        file1.setStarttime(RFC3339.format(new Date()));
        file1.setError("expected error");
        files.add(file1);

        DB.setDownloading(syncFile.getName());

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);

            app.getOverlayHelper();
            result = overlayHelper;

            overlayHelper.refresh(localPath);
        }};

        new CheckDownloadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.DOWNLOAD_FAILED, DB.get(name).get().getState());
        assertFalse(localPath.toFile().exists());

    }

    /**
     * Since renter/downloads returns files which were downloaded, results may contain a file failed to be download
     * even if the current download doesn't start. This test checks such files are ignored and their state is kept
     * to FOR_DOWNLOAD.
     */
    @Test
    public void failedPendingDownloads() throws ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(syncFile.getCloudPath().get().toString());
        file.setDestination(syncFile.getTemporaryPath().get().toString());
        file.setFilesize(syncFile.getCloudSize().get());
        file.setReceived(syncFile.getCloudSize().get() / 2);
        file.setStarttime(RFC3339.format(new Date()));
        file.setError("expected error");
        files.add(file);

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;

            App.getInstance();
            times = 0;
        }};

        new CheckDownloadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.FOR_DOWNLOAD, DB.get(name).get().getState());
        assertFalse(localPath.toFile().exists());

    }

    @Test
    public void creationTimeOfDownloadedFile() throws IOException, ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();
        final long targetDate = System.currentTimeMillis();

        final String name = String.format("file-%x", System.currentTimeMillis());
        final Path cloudPath = this.ctx.getPathPrefix().resolve(name).resolve(String.valueOf(targetDate));
        final Path localPath = this.tmpDir.resolve(name);

        final CloudFile cloudFile = new CloudFile() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Path getCloudPath() {
                return cloudPath;
            }

            @Override
            public long getFileSize() {
                return 100L;
            }
        };
        DB.addForDownload(cloudFile, localPath);
        DB.setDownloading(cloudFile.getName());

        final SyncFile syncFile = DB.get(name).get();
        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(cloudPath.toString());
        //noinspection ConstantConditions
        file.setDestination(syncFile.getTemporaryPath().get().toString());
        file.setFilesize(100L);
        file.setReceived(100L);
        file.setStarttime(RFC3339.format(targetDate));
        files.add(file);

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;

            App.getInstance();
            result = Optional.of(app);

            app.getOverlayHelper();
            result = overlayHelper;

            overlayHelper.refresh(localPath);
        }};

        new CheckDownloadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);
        assertEquals(SyncState.SYNCED, DB.get(name).get().getState());
        assertEquals(targetDate / 1000, localPath.toFile().lastModified() / 1000);
        assertTrue(localPath.toFile().exists());

    }

    /**
     * Test a case that a file being downloaded is also created/modified in the local directory. In this case, the
     * downloaded file don't have to be copied to the sync dir and should be deleted. CheckStateTask is responsible for
     * solving the conflict between the local and cloud files.
     */
    @Test
    public void downloadingFileModified() throws IOException, ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(syncFile.getCloudPath().get().toString());
        file.setDestination(syncFile.getTemporaryPath().get().toString());
        file.setFilesize(syncFile.getCloudSize().get());
        file.setReceived(syncFile.getCloudSize().get() / 2);
        file.setStarttime(RFC3339.format(new Date()));
        files.add(file);
        DB.setDownloading(syncFile.getName());

        final String dummyData = "dummy data";
        Files.write(localPath, dummyData.getBytes(), StandardOpenOption.CREATE);
        DB.setModified(name, localPath);

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;

            App.getInstance();
            times = 0;
        }};

        new CheckDownloadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);

        assertEquals(SyncState.MODIFIED, DB.get(name).get().getState());
        assertArrayEquals(dummyData.getBytes(), Files.readAllBytes(localPath));

    }

    /**
     * As same as testDownloadingFileModified, test a case that the downloaded file is also created/modified.
     * <p>
     * This test is related to issue #18.
     */
    @Test
    public void downloadedFileModified() throws IOException, ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(syncFile.getCloudPath().get().resolve(
                String.valueOf(System.currentTimeMillis() + 10000)).toString());
        file.setDestination(syncFile.getTemporaryPath().get().toString());
        file.setFilesize(syncFile.getCloudSize().get());
        file.setReceived(syncFile.getCloudSize().get());
        file.setStarttime(RFC3339.format(new Date()));
        files.add(file);

        DB.setDownloading(syncFile.getName());

        final String dummyData = "dummy data";
        Files.write(localPath, dummyData.getBytes(), StandardOpenOption.CREATE);
        DB.setModified(name, localPath);
        assertTrue(localPath.toFile().setLastModified(System.currentTimeMillis() + 20000));

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;

            App.getInstance();
            times = 0;
        }};

        new CheckDownloadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);

        assertEquals(SyncState.MODIFIED, DB.get(name).get().getState());
        assertFalse(
                syncFile.getTemporaryPath().get().toString(),
                syncFile.getTemporaryPath().get().toFile().exists());
        assertArrayEquals(dummyData.getBytes(), Files.readAllBytes(localPath));

        final String conflictedFileName = String.format(
                "%s (%s's conflicted copy %s)",
                localPath.getFileName().toString(),
                System.getProperty("user.name"),
                ISODateTimeFormat.date().print(System.currentTimeMillis()));
        assertTrue(localPath.getParent().resolve(conflictedFileName).toFile().exists());

    }

    /**
     * As same as testDownloadingFileModified, test a case that the downloaded file is also created/modified.
     * <p>
     * This test is related to issue #18.
     */
    @Test
    public void downloadedFileInSubDirModified() throws IOException, ApiException {

        final List<InlineResponse20010Downloads> files = new LinkedList<>();

        final Path name = Paths.get("sub-dir", "file");
        final Path cloudPath = this.ctx.getPathPrefix().resolve(name).resolve(
                String.valueOf(System.currentTimeMillis() + 10000));
        final Path localPath = this.tmpDir.resolve(name);
        final CloudFile cloudFile = new CloudFile() {
            @Override
            public String getName() {
                return name.toString();
            }

            @Override
            public Path getCloudPath() {
                return cloudPath;
            }

            @Override
            public long getFileSize() {
                return 100L;
            }
        };
        DB.addForDownload(cloudFile, localPath);

        final SyncFile syncFile = DB.get(name.toString()).get();

        final InlineResponse20010Downloads file = new InlineResponse20010Downloads();
        file.setSiapath(syncFile.getCloudPath().get().toString());
        file.setDestination(syncFile.getTemporaryPath().get().toString());
        file.setFilesize(syncFile.getCloudSize().get());
        file.setReceived(syncFile.getCloudSize().get());
        file.setStarttime(RFC3339.format(new Date()));
        files.add(file);

        DB.setDownloading(syncFile.getName());

        final String dummyData = "dummy data";
        final Path parent = localPath.getParent();
        if (!parent.toFile().exists()) {
            Files.createDirectories(parent);
        }
        Files.write(localPath, dummyData.getBytes(), StandardOpenOption.CREATE);
        DB.setModified(name.toString(), localPath);
        assertTrue(localPath.toFile().setLastModified(System.currentTimeMillis() + 20000));

        new Expectations() {{
            final InlineResponse20010 res = new InlineResponse20010();
            res.setDownloads(files);
            api.renterDownloadsGet();
            result = res;

            App.getInstance();
            times = 0;
        }};

        new CheckDownloadStateTask(this.ctx).call();
        assertTrue(DBMock.committed);

        assertEquals(SyncState.MODIFIED, DB.get(name.toString()).get().getState());
        assertFalse(
                syncFile.getTemporaryPath().get().toString(),
                syncFile.getTemporaryPath().get().toFile().exists());
        assertArrayEquals(dummyData.getBytes(), Files.readAllBytes(localPath));

        final String conflictedFileName = String.format(
                "%s (%s's conflicted copy %s)",
                localPath.getFileName().toString(),
                System.getProperty("user.name"),
                ISODateTimeFormat.date().print(System.currentTimeMillis()));
        assertTrue(localPath.getParent().resolve(conflictedFileName).toFile().exists());

    }

    @Test
    public void parseDate() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final Method parseDateTime = CheckDownloadStateTask.class.getDeclaredMethod("parseDateTime", String.class);
        parseDateTime.setAccessible(true);

        final String rfc3339 = "2009-11-10T23:00:00Z";
        final DateTime res1 = ((DateTime) parseDateTime.invoke(null, rfc3339)).toDateTime(DateTimeZone.UTC);
        assertEquals(2009, res1.year().get());
        assertEquals(11, res1.monthOfYear().get());
        assertEquals(10, res1.dayOfMonth().get());
        assertEquals(23, res1.hourOfDay().get());
        assertEquals(0, res1.minuteOfHour().get());
        assertEquals(0, res1.secondOfMinute().get());


        final String rfc3339tz = "2017-11-23T14:42:59.864874-07:00";
        final DateTime res2 = ((DateTime) parseDateTime.invoke(null, rfc3339tz)).toDateTime(DateTimeZone.UTC);
        assertEquals(2017, res2.year().get());
        assertEquals(11, res2.monthOfYear().get());
        assertEquals(23, res2.dayOfMonth().get());
        assertEquals(21, res2.hourOfDay().get());
        assertEquals(42, res2.minuteOfHour().get());
        assertEquals(59, res2.secondOfMinute().get());

    }

}