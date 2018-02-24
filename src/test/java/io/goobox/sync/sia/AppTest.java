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
import io.goobox.sync.common.overlay.OverlayHelper;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.command.CreateAllowance;
import io.goobox.sync.sia.command.DumpDB;
import io.goobox.sync.sia.command.GatewayConnect;
import io.goobox.sync.sia.command.Wallet;
import io.goobox.sync.sia.db.CloudFile;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.ExecutorMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.sia.task.CheckDownloadStateTask;
import io.goobox.sync.sia.task.CheckStateTask;
import io.goobox.sync.sia.task.CheckUploadStateTask;
import io.goobox.sync.sia.task.DeleteCloudFileTask;
import io.goobox.sync.sia.task.DeleteLocalFileTask;
import io.goobox.sync.sia.task.DownloadCloudFileTask;
import io.goobox.sync.sia.task.GetWalletInfoTask;
import io.goobox.sync.sia.task.NotifyEmptyFundTask;
import io.goobox.sync.sia.task.NotifyFundInfoTask;
import io.goobox.sync.sia.task.UploadLocalFileTask;
import io.goobox.sync.sia.task.WaitContractsTask;
import io.goobox.sync.sia.task.WaitSynchronizationTask;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.dizitart.no2.objects.ObjectRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class AppTest {

    private Path tmpDir;
    private Context ctx;

    @Before
    public void setUp() throws IOException {

        new DBMock();
        UtilsMock.dataDir = Files.createTempDirectory("data");
        UtilsMock.syncDir = Files.createTempDirectory("sync");
        new UtilsMock();

        this.tmpDir = UtilsMock.syncDir;
        final Config cfg = new Config(UtilsMock.dataDir.resolve(App.ConfigFileName));
        cfg.setUserName("test-user");
        cfg.setSyncDir(this.tmpDir);
        cfg.save();
        this.ctx = new Context(cfg);

    }

    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(UtilsMock.dataDir.toFile());
        try {
            FileUtils.deleteDirectory(this.tmpDir.toFile());
        } catch (IOException e) {
            System.err.println("Cannot delete sync folder: " + e.getMessage());
        }
    }

    /**
     * Test App.main without any options invokes app.call.
     */
    @Test
    public void testMain() throws IOException {
        new Expectations(App.class) {{
            final App app = new App();
            result = app;
            app.call();
        }};
        App.main(new String[]{});
    }

    /**
     * Test App.main with reset-db flag deletes the sync db if exists.
     */
    @Test
    public void testMainWithResetDB() throws IOException {

        new Expectations(App.class) {{
            final App app = new App();
            result = app;
            app.call();
            result = 0;
        }};

        try {
            final File dbFile = Utils.getDataDir().resolve(DB.DatabaseFileName).toFile();
            assertTrue(dbFile.createNewFile());
            App.main(new String[]{"--reset-db"});
            assertFalse("check database files exists", dbFile.exists());
        } finally {
            FileUtils.deleteDirectory(UtilsMock.dataDir.toFile());
        }

    }

    /**
     * Test App.main with sync-dir flag updates cfg.syncDir.
     */
    @Test
    public void testMainWithSyncDir() throws IOException {

        new Expectations(App.class) {{
            final App app = new App(tmpDir);
            result = app;
            app.call();
            result = 0;
        }};
        App.main(new String[]{"--sync-dir", this.tmpDir.toString()});

    }

    /**
     * Test App.main with output-events flag sets app.outputEvents = true.
     */
    @Test
    public void testMainWithOutputEvents() throws IOException {

        new Expectations(App.class) {{
            final App app = new App();
            result = app;
            app.enableOutputEvents();
            app.call();
            result = 0;
        }};
        App.main(new String[]{"--output-events"});

    }

    @Test
    public void testMainWithHelp() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        new Expectations(App.class) {{
            final Method printHelp = App.class.getDeclaredMethod("printHelp", Options.class);
            printHelp.setAccessible(true);
            printHelp.invoke(App.class, withAny(new Options()));
        }};
        App.main(new String[]{"-h"});
        App.main(new String[]{"--help"});

    }

    @Test
    public void testMainWithVersion() {

        new Expectations(System.out) {{
            System.out.println(String.format("Version %s", App.Version));
            times = 2;
        }};
        App.main(new String[]{"-v"});
        App.main(new String[]{"--version"});

    }

    @SuppressWarnings("unused")
    @Test
    public void testMainWithIllegalOptions()
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        new Expectations(App.class) {{
            final Method printHelp = App.class.getDeclaredMethod("printHelp", Options.class);
            printHelp.setAccessible(true);
            printHelp.invoke(App.class, withAny(new Options()));
        }};
        new Expectations(System.class) {{
            System.exit(1);
        }};
        App.main(new String[]{"--aaaaa"});

    }

    @Test
    public void testPrintHelp(@Mocked HelpFormatter help) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (final PrintWriter writer = new PrintWriter(buffer)) {
            writer.println();
            writer.println("Commands:");
            writer.print(" ");
            writer.println(Wallet.CommandName);
            writer.print("   ");
            writer.println(Wallet.Description);
            writer.print(" ");
            writer.println(CreateAllowance.CommandName);
            writer.print("   ");
            writer.println(CreateAllowance.Description);
            writer.print(" ");
            writer.println(GatewayConnect.CommandName);
            writer.print("   ");
            writer.println(GatewayConnect.Description);
            writer.print(" ");
            writer.println(DumpDB.CommandName);
            writer.print("   ");
            writer.println(DumpDB.Description);
        }

        final Options opt = new Options();
        new Expectations() {{
            help.printHelp(App.Name, App.Description, opt, buffer.toString(), true);
        }};

        final Method printHelp = App.class.getDeclaredMethod("printHelp", Options.class);
        printHelp.setAccessible(true);
        printHelp.invoke(App.class, opt);

    }

    @SuppressWarnings("unused")
    @Test
    public void testWithWalletCommand(@Mocked Wallet cmd) {

        final String[] args = new String[]{Wallet.CommandName, "a", "b", "c"};
        new Expectations() {{
            Wallet.main(Arrays.copyOfRange(args, 1, args.length));
        }};
        App.main(args);

    }

    @SuppressWarnings("unused")
    @Test
    public void testCreateAllowanceCommand(@Mocked CreateAllowance cmd) {

        final String[] args = new String[]{CreateAllowance.CommandName, "x", "y", "z"};
        new Expectations() {{
            CreateAllowance.main(Arrays.copyOfRange(args, 1, args.length));
        }};
        App.main(args);

    }

    @SuppressWarnings("unused")
    @Test
    public void testGatewayConnectCommand(@Mocked GatewayConnect cmd) {

        final String[] args = new String[]{GatewayConnect.CommandName, "x", "y", "z"};
        new Expectations() {{
            GatewayConnect.main(Arrays.copyOfRange(args, 1, args.length));
        }};
        App.main(args);

    }

    @Test
    public void testConstructor() {
        new Expectations(APIUtils.class) {{
            APIUtils.loadConfig(Utils.getDataDir().resolve(App.ConfigFileName));
            result = ctx.getConfig();

            APIUtils.getApiClient(ctx.getConfig());
            result = ctx.getApiClient();
        }};

        final App app = new App();
        final Context ctx = app.getContext();
        assertEquals(this.ctx.getConfig(), ctx.getConfig());
        assertEquals(this.ctx.getApiClient(), ctx.getApiClient());

        final OverlayHelper overlayHelper = Deencapsulation.getField(app, "overlayHelper");
        assertEquals(this.ctx.getConfig().getSyncDir(), Deencapsulation.getField(overlayHelper, "syncDir"));
    }

    @Test
    public void testConstructorWithSyncDir() {
        new Expectations(APIUtils.class) {{
            APIUtils.loadConfig(Utils.getDataDir().resolve(App.ConfigFileName));
            result = ctx.getConfig();

            APIUtils.getApiClient(ctx.getConfig());
            result = ctx.getApiClient();
        }};

        final App app = new App(this.tmpDir);
        final Context ctx = app.getContext();
        assertEquals(this.ctx.getConfig(), ctx.getConfig());
        assertEquals(this.ctx.getApiClient(), ctx.getApiClient());
        assertEquals(this.tmpDir, ctx.getConfig().getSyncDir());

        final OverlayHelper overlayHelper = Deencapsulation.getField(app, "overlayHelper");
        assertEquals(this.tmpDir, Deencapsulation.getField(overlayHelper, "syncDir"));
    }

    /**
     * Calls checkAndCreateSyncDir and checkAndCreateDataDir. Then runs GetWalletInfoTask, WaitSynchronizationTask,
     * and WaitContractsTask. After that, runs synchronizeModifiedFiles and synchronizeDeletedFiles.
     * Finally, registers CheckStateTask, CheckDownloadStateTask, CheckUploadStateTask, and FileWatcher.
     */
    @Test
    public void testCall() throws GetWalletInfoTask.WalletException, ApiException, IOException {

        final App app = new App();
        Deencapsulation.setField(app, "ctx", this.ctx);

        new Expectations(app) {{
            app.checkAndCreateSyncDir();
            result = true;
            app.checkAndCreateDataDir();
            result = true;
        }};

        new Expectations(GetWalletInfoTask.class, WaitSynchronizationTask.class, NotifyEmptyFundTask.class, WaitContractsTask.class) {{
            final GetWalletInfoTask getWalletInfoTask = new GetWalletInfoTask(ctx);
            getWalletInfoTask.call();
            result = null;

            final WaitSynchronizationTask waitSynchronizationTask = new WaitSynchronizationTask(ctx);
            waitSynchronizationTask.call();
            result = null;

            final WaitContractsTask waitContractsTask = new WaitContractsTask(ctx);
            waitContractsTask.call();
            result = null;
        }};

        new Expectations(app) {{
            app.synchronizeModifiedFiles(ctx.getConfig().getSyncDir());
            app.synchronizeDeletedFiles();
        }};

        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(App.WorkerThreadSize);
        final StartSiaDaemonTask startSiaDaemonTask = new StartSiaDaemonTask();
        final CheckStateTask checkStateTask = new CheckStateTask(ctx, executor);
        final CheckDownloadStateTask checkDownloadStateTask = new CheckDownloadStateTask(ctx);
        final CheckUploadStateTask checkUploadStateTask = new CheckUploadStateTask(ctx);
        new Expectations(Executors.class) {{
            Executors.newScheduledThreadPool(App.WorkerThreadSize);
            result = executor;
        }};

        final OverlayHelper overlayHelper = Deencapsulation.getField(app, "overlayHelper");
        new Expectations(app, overlayHelper, executor, StartSiaDaemonTask.class,
                CheckStateTask.class, CheckDownloadStateTask.class, CheckUploadStateTask.class, FileWatcher.class) {{

            app.resumeTasks(ctx, executor);

            new StartSiaDaemonTask();
            result = startSiaDaemonTask;

            new CheckStateTask(ctx, executor);
            result = checkStateTask;
            new RetryableTask(checkStateTask, startSiaDaemonTask);
            executor.scheduleWithFixedDelay((RetryableTask) any, 0, 60, TimeUnit.SECONDS);

            new CheckDownloadStateTask(ctx);
            result = checkDownloadStateTask;
            new RetryableTask(checkDownloadStateTask, startSiaDaemonTask);
            executor.scheduleWithFixedDelay((RetryableTask) any, 30, 60, TimeUnit.SECONDS);

            new CheckUploadStateTask(ctx);
            result = checkUploadStateTask;
            new RetryableTask(checkUploadStateTask, startSiaDaemonTask);
            executor.scheduleWithFixedDelay((RetryableTask) any, 45, 60, TimeUnit.SECONDS);

            new FileWatcher(ctx.getConfig().getSyncDir(), executor);

            overlayHelper.setSynchronizing();
        }};

        new Expectations(System.class) {{
            System.out.println(io.goobox.sync.sia.SyncState.startSynchronization.toJson());
        }};

        app.call();

    }

    @Test
    public void testCallWithOutputEvents() throws IOException, GetWalletInfoTask.WalletException, ApiException {

        final App app = new App();
        Deencapsulation.setField(app, "ctx", this.ctx);
        Deencapsulation.setField(app, "outputEvents", true);

        new Expectations(app) {{
            app.checkAndCreateSyncDir();
            result = true;
            app.checkAndCreateDataDir();
            result = true;
        }};

        new Expectations(GetWalletInfoTask.class, WaitSynchronizationTask.class, NotifyEmptyFundTask.class, WaitContractsTask.class) {{
            final GetWalletInfoTask getWalletInfoTask = new GetWalletInfoTask(ctx);
            getWalletInfoTask.call();
            result = null;

            final WaitSynchronizationTask waitSynchronizationTask = new WaitSynchronizationTask(ctx);
            waitSynchronizationTask.call();
            result = null;

            final NotifyEmptyFundTask notifyEmptyFundTask = new NotifyEmptyFundTask(ctx);
            notifyEmptyFundTask.run();

            final WaitContractsTask waitContractsTask = new WaitContractsTask(ctx);
            waitContractsTask.call();
            result = null;
        }};

        new Expectations(app) {{
            app.synchronizeModifiedFiles(ctx.getConfig().getSyncDir());
            app.synchronizeDeletedFiles();
        }};

        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(App.WorkerThreadSize);
        final StartSiaDaemonTask startSiaDaemonTask = new StartSiaDaemonTask();
        final CheckStateTask checkStateTask = new CheckStateTask(ctx, executor);
        final CheckDownloadStateTask checkDownloadStateTask = new CheckDownloadStateTask(ctx);
        final CheckUploadStateTask checkUploadStateTask = new CheckUploadStateTask(ctx);
        new Expectations(Executors.class) {{
            Executors.newScheduledThreadPool(App.WorkerThreadSize);
            result = executor;
        }};

        final OverlayHelper overlayHelper = Deencapsulation.getField(app, "overlayHelper");
        new Expectations(
                app, executor, overlayHelper, StartSiaDaemonTask.class, CheckStateTask.class, CheckDownloadStateTask.class,
                CheckUploadStateTask.class, NotifyFundInfoTask.class, FileWatcher.class) {{

            app.resumeTasks(ctx, executor);

            new StartSiaDaemonTask();
            result = startSiaDaemonTask;

            new CheckStateTask(ctx, executor);
            result = checkStateTask;
            new RetryableTask(checkStateTask, startSiaDaemonTask);
            executor.scheduleWithFixedDelay((RetryableTask) any, 0, 60, TimeUnit.SECONDS);

            new CheckDownloadStateTask(ctx);
            result = checkDownloadStateTask;
            new RetryableTask(checkDownloadStateTask, startSiaDaemonTask);
            executor.scheduleWithFixedDelay((RetryableTask) any, 30, 60, TimeUnit.SECONDS);

            new CheckUploadStateTask(ctx);
            result = checkUploadStateTask;
            new RetryableTask(checkUploadStateTask, startSiaDaemonTask);
            executor.scheduleWithFixedDelay((RetryableTask) any, 45, 60, TimeUnit.SECONDS);

            new NotifyFundInfoTask(ctx, true);
            executor.scheduleWithFixedDelay((Runnable) any, 0, 1, TimeUnit.HOURS);

            new FileWatcher(ctx.getConfig().getSyncDir(), executor);

            overlayHelper.setSynchronizing();
        }};

        new Expectations(System.class) {{
            System.out.println(io.goobox.sync.sia.SyncState.startSynchronization.toJson());
        }};

        app.call();

    }

    @Test
    public void testCallWithOutputEventsAndDisableAutoAllocation() throws IOException, GetWalletInfoTask.WalletException, ApiException {

        final App app = new App();
        Deencapsulation.setField(app, "ctx", this.ctx);
        Deencapsulation.setField(app, "outputEvents", true);
        this.ctx.getConfig().setDisableAutoAllocation(true);

        new Expectations(app) {{
            app.checkAndCreateSyncDir();
            result = true;
            app.checkAndCreateDataDir();
            result = true;
        }};

        new Expectations(GetWalletInfoTask.class, WaitSynchronizationTask.class, NotifyEmptyFundTask.class, WaitContractsTask.class) {{
            final GetWalletInfoTask getWalletInfoTask = new GetWalletInfoTask(ctx);
            getWalletInfoTask.call();
            result = null;

            final WaitSynchronizationTask waitSynchronizationTask = new WaitSynchronizationTask(ctx);
            waitSynchronizationTask.call();
            result = null;

            final NotifyEmptyFundTask notifyEmptyFundTask = new NotifyEmptyFundTask(ctx);
            notifyEmptyFundTask.run();

            final WaitContractsTask waitContractsTask = new WaitContractsTask(ctx);
            waitContractsTask.call();
            result = null;
        }};

        new Expectations(app) {{
            app.synchronizeModifiedFiles(ctx.getConfig().getSyncDir());
            app.synchronizeDeletedFiles();
        }};

        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(App.WorkerThreadSize);
        final StartSiaDaemonTask startSiaDaemonTask = new StartSiaDaemonTask();
        final CheckStateTask checkStateTask = new CheckStateTask(ctx, executor);
        final CheckDownloadStateTask checkDownloadStateTask = new CheckDownloadStateTask(ctx);
        final CheckUploadStateTask checkUploadStateTask = new CheckUploadStateTask(ctx);
        new Expectations(Executors.class) {{
            Executors.newScheduledThreadPool(App.WorkerThreadSize);
            result = executor;
        }};

        final OverlayHelper overlayHelper = Deencapsulation.getField(app, "overlayHelper");
        new Expectations(
                app, executor, overlayHelper, StartSiaDaemonTask.class, CheckStateTask.class, CheckDownloadStateTask.class,
                CheckUploadStateTask.class, NotifyFundInfoTask.class, FileWatcher.class) {{

            app.resumeTasks(ctx, executor);

            new StartSiaDaemonTask();
            result = startSiaDaemonTask;

            new CheckStateTask(ctx, executor);
            result = checkStateTask;
            new RetryableTask(checkStateTask, startSiaDaemonTask);
            executor.scheduleWithFixedDelay((RetryableTask) any, 0, 60, TimeUnit.SECONDS);

            new CheckDownloadStateTask(ctx);
            result = checkDownloadStateTask;
            new RetryableTask(checkDownloadStateTask, startSiaDaemonTask);
            executor.scheduleWithFixedDelay((RetryableTask) any, 30, 60, TimeUnit.SECONDS);

            new CheckUploadStateTask(ctx);
            result = checkUploadStateTask;
            new RetryableTask(checkUploadStateTask, startSiaDaemonTask);
            executor.scheduleWithFixedDelay((RetryableTask) any, 45, 60, TimeUnit.SECONDS);

            new NotifyFundInfoTask(ctx, false);
            executor.scheduleWithFixedDelay((Runnable) any, 0, 1, TimeUnit.HOURS);

            new FileWatcher(ctx.getConfig().getSyncDir(), executor);

            overlayHelper.setSynchronizing();
        }};

        app.call();

    }

    /**
     * This test simulates the scenario that trying to start a sia daemon couple of times but finally cannot do it.
     */
    @Test
    public void testCallWithApiException(@SuppressWarnings("unused") @Mocked Thread thread)
            throws GetWalletInfoTask.WalletException, ApiException, IOException, InterruptedException {

        final App app = new App();
        Deencapsulation.setField(app, "ctx", this.ctx);

        new Expectations(app) {{
            app.checkAndCreateSyncDir();
            result = true;
            app.checkAndCreateDataDir();
            result = true;
        }};

        new Expectations(GetWalletInfoTask.class, WaitSynchronizationTask.class, NotifyEmptyFundTask.class, WaitContractsTask.class) {{
            final GetWalletInfoTask getWalletInfoTask = new GetWalletInfoTask(ctx);
            result = getWalletInfoTask;
            getWalletInfoTask.call();
            result = new ApiException();
            times = App.MaxRetry + 1;
            Thread.sleep(App.DefaultSleepTime);
            times = App.MaxRetry;
        }};

        assertEquals(Integer.valueOf(1), app.call());

    }

    /**
     * This test simulates the scenario that GetWalletInfoTask throws a WalletException.
     */
    @Test
    public void testCallWithWalletException() throws GetWalletInfoTask.WalletException, ApiException, IOException {

        final App app = new App();
        Deencapsulation.setField(app, "ctx", this.ctx);

        new Expectations(app) {{
            app.checkAndCreateSyncDir();
            result = true;
            app.checkAndCreateDataDir();
            result = true;
        }};

        new Expectations(GetWalletInfoTask.class, WaitSynchronizationTask.class, NotifyEmptyFundTask.class, WaitContractsTask.class) {{
            final GetWalletInfoTask getWalletInfoTask = new GetWalletInfoTask(ctx);
            result = getWalletInfoTask;
            getWalletInfoTask.call();
            result = new GetWalletInfoTask.WalletException("expected error");
        }};

        assertEquals(Integer.valueOf(1), app.call());

    }

    @Test
    public void testCheckAndCreateSyncDir() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        class AppMock extends MockUp<App> {
            private Path arg;

            @SuppressWarnings("unused")
            @Mock
            private boolean checkAndCreateFolder(Path path) {
                arg = path;
                return true;
            }
        }

        final AppMock mock = new AppMock();
        final App app = new App(this.tmpDir);
        final Method checkAndCreateSyncDir = App.class.getDeclaredMethod("checkAndCreateSyncDir");
        checkAndCreateSyncDir.setAccessible(true);
        checkAndCreateSyncDir.invoke(app);

        assertEquals(this.tmpDir, mock.arg);

    }

    @Test
    public void testCheckAndCreateDataDir() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        class AppMock extends MockUp<App> {
            private Path arg;

            @SuppressWarnings("unused")
            @Mock
            private boolean checkAndCreateFolder(Path path) {
                arg = path;
                return true;
            }
        }

        final AppMock mock = new AppMock();
        final App app = new App();
        final Method checkAndCreateDataDir = App.class.getDeclaredMethod("checkAndCreateDataDir");
        checkAndCreateDataDir.setAccessible(true);
        checkAndCreateDataDir.invoke(app);

        assertEquals(Utils.getDataDir(), mock.arg);

    }

    @Test
    public void testCheckAndCreateFolderWithExistingFolder() throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        final Path tmpDir = Files.createTempDirectory(null);
        try {

            final Path target = tmpDir.resolve("test-dir");
            assertFalse(target.toFile().exists());

            final App app = new App();
            final Method checkAndCreateFolder = App.class.getDeclaredMethod("checkAndCreateFolder", Path.class);
            checkAndCreateFolder.setAccessible(true);
            final boolean res = (boolean) checkAndCreateFolder.invoke(app, target);
            assertTrue(res);
            assertTrue(target.toFile().exists());

        } finally {
            FileUtils.deleteDirectory(tmpDir.toFile());
        }

    }

    @Test
    public void testCheckAndCreateFolderWithNotExistingFolder() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final Path tmpDir = Files.createTempDirectory(null);
        try {
            assertTrue(tmpDir.toFile().exists());


            final App app = new App();
            final Method checkAndCreateFolder = App.class.getDeclaredMethod("checkAndCreateFolder", Path.class);
            checkAndCreateFolder.setAccessible(true);
            final boolean res = (boolean) checkAndCreateFolder.invoke(app, tmpDir);
            assertTrue(res);
            assertTrue(tmpDir.toFile().exists());

        } finally {
            FileUtils.deleteDirectory(tmpDir.toFile());
        }

    }

    @Test
    public void synchronizeNewFile() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {

        final Path name = Paths.get("sub-dir", String.format("file-%x", System.currentTimeMillis()));
        final Path localPath = this.tmpDir.resolve(name);
        Files.createDirectories(localPath.getParent());
        assertTrue(localPath.toFile().createNewFile());

        invokeSynchronizeModifiedFiles();

        assertTrue(DB.get(name.toString()).isPresent());
        assertEquals(SyncState.MODIFIED, DB.get(name.toString()).map(SyncFile::getState).orElse(null));

    }

    @Test
    public void synchronizeNotModifiedFile() throws InvocationTargetException, IllegalAccessException, IOException, NoSuchMethodException {

        final String dummyData = "sample test";
        final Path name = Paths.get("sub-dir", String.format("file-%x", System.currentTimeMillis()));
        final Path localPath = this.tmpDir.resolve(name);
        Files.createDirectories(localPath.getParent());
        Files.write(localPath, dummyData.getBytes(), StandardOpenOption.CREATE);
        DB.setSynced(new CloudFile() {
            @NotNull
            @Override
            public String getName() {
                return name.toString();
            }

            @NotNull
            @Override
            public Path getCloudPath() {
                return ctx.getPathPrefix().resolve(name);
            }

            @Override
            public long getFileSize() {
                return 0;
            }
        }, localPath);

        invokeSynchronizeModifiedFiles();

        assertTrue(DB.get(name.toString()).isPresent());
        assertEquals(SyncState.SYNCED, DB.get(name.toString()).map(SyncFile::getState).orElse(null));

    }

    @Test
    public void synchronizeModifiedFile() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        final String dummyData = "sample test";
        final Path name = Paths.get("sub-dir", String.format("file-%x", System.currentTimeMillis()));
        final Path localPath = this.tmpDir.resolve(name);
        Files.createDirectories(localPath.getParent());
        Files.write(localPath, dummyData.getBytes(), StandardOpenOption.CREATE);
        DB.setSynced(new CloudFile() {
            @NotNull
            @Override
            public String getName() {
                return name.toString();
            }

            @NotNull
            @Override
            public Path getCloudPath() {
                return ctx.getPathPrefix().resolve(name);
            }

            @Override
            public long getFileSize() {
                return 0;
            }
        }, localPath);

        Files.write(localPath, dummyData.getBytes(), StandardOpenOption.APPEND);

        invokeSynchronizeModifiedFiles();

        assertTrue(DB.get(name.toString()).isPresent());
        assertEquals(SyncState.MODIFIED, DB.get(name.toString()).map(SyncFile::getState).orElse(null));

    }

    private void invokeSynchronizeModifiedFiles() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        final App app = new App(this.tmpDir);
        final Method synchronizeModifiedFiles = App.class.getDeclaredMethod("synchronizeModifiedFiles", Path.class);
        synchronizeModifiedFiles.setAccessible(true);
        synchronizeModifiedFiles.invoke(app, this.tmpDir);
    }

    @Test
    public void synchronizeDeletedFile() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        final String dummyData = "sample test";
        final Path name = Paths.get("sub-dir", String.format("file-%x", System.currentTimeMillis()));
        final Path localPath = this.tmpDir.resolve(name);
        Files.createDirectories(localPath.getParent());
        Files.write(localPath, dummyData.getBytes(), StandardOpenOption.CREATE);
        DB.setSynced(new CloudFile() {
            @NotNull
            @Override
            public String getName() {
                return name.toString();
            }

            @NotNull
            @Override
            public Path getCloudPath() {
                return ctx.getPathPrefix().resolve(name);
            }

            @Override
            public long getFileSize() {
                return 0;
            }
        }, localPath);

        assertTrue(localPath.toFile().delete());

        final App app = new App(this.tmpDir);
        final Method synchronizeDeletedFiles = App.class.getDeclaredMethod("synchronizeDeletedFiles");
        synchronizeDeletedFiles.setAccessible(true);
        synchronizeDeletedFiles.invoke(app);

        assertTrue(DB.get(name.toString()).isPresent());
        assertEquals(SyncState.DELETED, DB.get(name.toString()).map(SyncFile::getState).orElse(null));

    }

    @Test
    public void resumeToBeUploadedFile() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkResumeTasks(SyncState.FOR_UPLOAD, UploadLocalFileTask.class);
    }

    @Test
    public void resumeToBeDownloadedFile() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkResumeTasks(SyncState.FOR_DOWNLOAD, DownloadCloudFileTask.class);
    }

    @Test
    public void resumeToBeCloudDeletedFile() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkResumeTasks(SyncState.FOR_CLOUD_DELETE, DeleteCloudFileTask.class);
    }

    @Test
    public void resumeToBeLocalDeletedFile() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        this.checkResumeTasks(SyncState.FOR_LOCAL_DELETE, DeleteLocalFileTask.class);
    }

    /**
     * Enqueued tasks might not be executed because this app is closed. The app should check files of which state
     * starts with FOR_ and re-enqueues related tasks before starting other tasks.
     */
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private void checkResumeTasks(final SyncState state, final Class<?> cmdClass)
            throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final String name = String.format("test-%s", state);
        final Path localPath = this.tmpDir.resolve(name);
        assertTrue(localPath.toFile().createNewFile());

        DB.setSynced(new CloudFile() {
            @NotNull
            @Override
            public String getName() {
                return name;
            }

            @NotNull
            @Override
            public Path getCloudPath() {
                return ctx.getPathPrefix().resolve(name);
            }

            @Override
            public long getFileSize() {
                return 0;
            }
        }, localPath);

        final SyncFile syncFile = DB.get(name).get();
        final Method setState = SyncFile.class.getDeclaredMethod("setState", SyncState.class);
        setState.setAccessible(true);
        setState.invoke(syncFile, state);

        final Method repo = DB.class.getDeclaredMethod("repo");
        repo.setAccessible(true);
        final ObjectRepository<SyncFile> repository = (ObjectRepository<SyncFile>) repo.invoke(DB.class);
        repository.update(syncFile);

        final ExecutorMock executor = new ExecutorMock();

        final App app = new App();
        final Method resumeTasks = App.class.getDeclaredMethod("resumeTasks", Context.class, Executor.class);
        resumeTasks.setAccessible(true);
        resumeTasks.invoke(app, this.ctx, executor);

        final Runnable task = executor.queue.get(0);
        System.out.println(task);

        final Class<?> klass;
        if (task instanceof RetryableTask) {
            klass = Deencapsulation.getField(task, "task").getClass();
        } else {
            klass = task.getClass();
        }
        assertEquals(cmdClass, klass);

    }

    @Test
    public void startSiaDaemon(@Mocked SiaDaemon daemon) throws IOException {

        final Config cfg = ctx.getConfig();
        new Expectations() {{
            new SiaDaemon(cfg);
            result = daemon;
            daemon.checkAndDownloadConsensusDB();
            daemon.start();
        }};

        final App app = new App();
        assertEquals(null, (SiaDaemon) Deencapsulation.getField(app, "daemon"));
        app.startSiaDaemon();

    }

    @Test
    public void restartSiaDaemon(@Mocked SiaDaemon daemon) throws IOException {

        new Expectations() {{
            daemon.isClosed();
            result = true;
            daemon.checkAndDownloadConsensusDB();
            daemon.start();
        }};

        final App app = new App();
        Deencapsulation.setField(app, "daemon", daemon);
        app.startSiaDaemon();

    }

    @Test
    public void notStartSiaDaemon(@Mocked SiaDaemon daemon) throws IOException {

        new Expectations() {{
            daemon.isClosed();
            result = false;
            daemon.checkAndDownloadConsensusDB();
            times = 0;
            daemon.start();
            times = 0;
        }};

        final App app = new App();
        Deencapsulation.setField(app, "daemon", daemon);
        app.startSiaDaemon();

    }

    @Test
    public void refreshOverlayIconWithSyncedDB(@SuppressWarnings("unused") @Mocked DB db) {

        final App app = new App();
        app.enableOutputEvents();

        final OverlayHelper overlayHelper = Deencapsulation.getField(app, "overlayHelper");
        new Expectations(overlayHelper, System.class) {{
            overlayHelper.refresh(tmpDir);
            DB.isSynced();
            result = true;
            overlayHelper.setOK();

            System.out.println(io.goobox.sync.sia.SyncState.idle.toJson());
        }};
        app.refreshOverlayIcon(tmpDir);

    }

    @Test
    public void refreshOverlayIconWithSynchronizingDB(@SuppressWarnings("unused") @Mocked DB db) {

        final App app = new App();
        app.enableOutputEvents();

        final OverlayHelper overlayHelper = Deencapsulation.getField(app, "overlayHelper");
        new Expectations(overlayHelper, System.class) {{
            overlayHelper.refresh(tmpDir);
            DB.isSynced();
            result = false;
            overlayHelper.setSynchronizing();

            System.out.println(io.goobox.sync.sia.SyncState.synchronizing.toJson());
        }};
        app.refreshOverlayIcon(tmpDir);

    }

}