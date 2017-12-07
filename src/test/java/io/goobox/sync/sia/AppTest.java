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

import com.squareup.okhttp.OkHttpClient;
import io.goobox.sync.common.Utils;
import io.goobox.sync.sia.client.ApiClient;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.ConsensusApi;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.WalletApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse20014;
import io.goobox.sync.sia.client.api.model.InlineResponse20016;
import io.goobox.sync.sia.client.api.model.InlineResponse2006;
import io.goobox.sync.sia.client.api.model.InlineResponse2009;
import io.goobox.sync.sia.client.api.model.InlineResponse2009Contracts;
import io.goobox.sync.sia.command.CmdUtils;
import io.goobox.sync.sia.command.CreateAllowance;
import io.goobox.sync.sia.command.Wallet;
import io.goobox.sync.sia.db.CloudFile;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncFile;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.mocks.DBMock;
import io.goobox.sync.sia.mocks.ExecutorMock;
import io.goobox.sync.sia.mocks.UtilsMock;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class AppTest {

    @SuppressWarnings("unused")
    @Mocked
    private WalletApi wallet;

    @SuppressWarnings("unused")
    @Mocked
    private ConsensusApi consensus;

    @SuppressWarnings("unused")
    @Mocked
    private RenterApi renter;

    private Context ctx;

    @Before
    public void setUp() throws IOException {

        new DBMock();

        final Config cfg = new Config();
        cfg.setUserName("test-user");
        this.ctx = new Context(cfg, null);

        UtilsMock.dataDir = Files.createTempDirectory(null);
        UtilsMock.syncDir = Files.createTempDirectory(null);
        new UtilsMock();

    }

    @After
    public void tearDown() throws IOException {
        DB.close();
        FileUtils.deleteDirectory(UtilsMock.dataDir.toFile());
        FileUtils.deleteDirectory(UtilsMock.syncDir.toFile());
    }

    @Test
    public void testMain() {

        class AppMock extends MockUp<App> {
            private boolean initialized = false;

            @SuppressWarnings("unused")
            @Mock
            private void init() {
                this.initialized = true;
            }
        }

        final AppMock mock = new AppMock();
        App.main(new String[]{});
        assertTrue(mock.initialized);

    }

    @Test
    public void testMainWithResetDB() throws IOException {

        class AppMock extends MockUp<App> {
            private boolean initialized = false;

            @SuppressWarnings("unused")
            @Mock
            private void init() {
                this.initialized = true;
            }
        }

        final AppMock mock = new AppMock();
        try {

            final File dbFile = Utils.getDataDir().resolve(DB.DatabaseFileName).toFile();
            assertTrue(dbFile.createNewFile());

            App.main(new String[]{"--reset-db"});
            assertTrue("check app is initialized", mock.initialized);
            assertFalse("check database files exists", dbFile.exists());

        } finally {

            FileUtils.deleteDirectory(UtilsMock.dataDir.toFile());

        }

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

        final StringBuilder builder = new StringBuilder();
        builder.append("\nCommands:\n");
        builder.append(" ");
        builder.append(Wallet.CommandName);
        builder.append("\n  ");
        builder.append(Wallet.Description);
        builder.append("\n ");
        builder.append(CreateAllowance.CommandName);
        builder.append("\n  ");
        builder.append(CreateAllowance.Description);

        final Options opt = new Options();
        new Expectations() {{
            help.printHelp(App.CommandName, App.Description, opt, builder.toString(), true);
        }};

        final Method printHelp = App.class.getDeclaredMethod("printHelp", Options.class);
        printHelp.setAccessible(true);
        printHelp.invoke(App.class, opt);

    }

    @SuppressWarnings("unused")
    @Test
    public void testMainWithWalletCommand(@Mocked Wallet cmd) {

        final String[] args = new String[]{Wallet.CommandName, "a", "b", "c"};
        new Expectations() {{
            Wallet.main(Arrays.copyOfRange(args, 1, args.length));
        }};
        App.main(args);

    }

    @SuppressWarnings("unused")
    @Test
    public void testMainWithCreateAllowanceCommand(@Mocked CreateAllowance cmd) {

        final String[] args = new String[]{CreateAllowance.CommandName, "x", "y", "z"};
        new Expectations() {{
            CreateAllowance.main(Arrays.copyOfRange(args, 1, args.length));
        }};
        App.main(args);

    }

    @SuppressWarnings("unused")
    @Test
    public void testInit(@Mocked CmdUtils utils, @Mocked FileWatcher watcher)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, IOException {

        final Config cfg = new Config();
        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://localhost:9980");
        final OkHttpClient httpClient = apiClient.getHttpClient();
        httpClient.setConnectTimeout(0, TimeUnit.MILLISECONDS);
        httpClient.setReadTimeout(0, TimeUnit.MILLISECONDS);
        final Context ctx = new Context(cfg, apiClient);

        new Expectations() {{
            CmdUtils.getApiClient();
            result = apiClient;
        }};

        class AppMock extends MockUp<App> {
            private boolean checkedSyncDir = false;
            private boolean checkedDataDir = false;
            private boolean preparedWallet = false;
            private boolean waitedSynchronization = false;
            private boolean waitedContracts = false;
            private boolean calledResumeTasks = false;
            private boolean calledSynchronizeModifiedFiles = false;
            private boolean calledSynchronizeDeletedFiles = false;

            @Mock
            private Config loadConfig(Path path) {
                assertEquals(Utils.getDataDir().resolve(App.ConfigFileName), path);
                return cfg;
            }

            @Mock
            private boolean checkAndCreateSyncDir() {
                this.checkedSyncDir = true;
                return true;
            }

            @Mock
            private boolean checkAndCreateDataDir() {
                this.checkedDataDir = true;
                return true;
            }

            @Mock
            void prepareWallet() {
                this.preparedWallet = true;
            }

            @Mock
            void waitSynchronization() {
                this.waitedSynchronization = true;
            }

            @Mock
            void waitContracts() {
                this.waitedContracts = true;
            }

            @Mock
            private void resumeTasks(final Context context, final Executor executor) {
                assertEquals(ctx, context);
                this.calledResumeTasks = true;
            }

            @Mock
            private void synchronizeModifiedFiles(Path root) {
                this.calledSynchronizeModifiedFiles = true;
            }

            @Mock
            private void synchronizeDeletedFiles() {
                this.calledSynchronizeDeletedFiles = true;
            }

        }

        class ScheduledThreadPoolExecutorMock extends MockUp<ScheduledThreadPoolExecutor> {
            private List<Runnable> queue = new ArrayList<>();

            @Mock
            void scheduleWithFixedDelay(Runnable task, long start, long delay, TimeUnit unit) {
                queue.add(task);
            }
        }
        ScheduledThreadPoolExecutorMock executorMock = new ScheduledThreadPoolExecutorMock();


        // Enqueue basic tasks.
        new Expectations() {{
            new FileWatcher(Utils.getSyncDir(), withNotNull());
        }};

        final AppMock mock = new AppMock();
        final App app = new App();
        final Method init = App.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(app);

        assertTrue(mock.checkedSyncDir);
        assertTrue(mock.checkedDataDir);
        assertTrue(mock.preparedWallet);
        assertTrue(mock.waitedSynchronization);
        assertTrue(mock.waitedContracts);
        assertTrue(mock.calledSynchronizeModifiedFiles);
        assertTrue(mock.calledSynchronizeDeletedFiles);
        assertTrue(mock.calledResumeTasks);

        assertTrue(Deencapsulation.getField(executorMock.queue.get(0), "task") instanceof CheckStateTask);
        assertTrue(Deencapsulation.getField(executorMock.queue.get(1), "task") instanceof CheckDownloadStateTask);
        assertTrue(Deencapsulation.getField(executorMock.queue.get(2), "task") instanceof CheckUploadStateTask);

    }

    @SuppressWarnings("unused")
    @Test
    public void initWithApiException(@Mocked Thread thread)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InterruptedException {

        final Config cfg = new Config();
        new Expectations() {{
            CmdUtils.getApiClient();
            result = null;
            Thread.sleep(App.DefaultSleepTime);
        }};

        new Expectations(System.class) {{
            System.exit(1);
        }};

        class AppMock extends MockUp<App> {
            private boolean checkedSyncDir = false;
            private boolean checkedDataDir = false;
            private int prepareWalletCalled = 0;
            private int startSiaDaemonCalled = 0;

            @Mock
            private Config loadConfig(Path path) {
                assertEquals(Utils.getDataDir().resolve(App.ConfigFileName), path);
                return cfg;
            }

            @Mock
            private boolean checkAndCreateSyncDir() {
                this.checkedSyncDir = true;
                return true;
            }

            @Mock
            private boolean checkAndCreateDataDir() {
                this.checkedDataDir = true;
                return true;
            }

            @Mock
            void prepareWallet() throws ApiException {
                this.prepareWalletCalled++;
                throw new ApiException(new ConnectException("expected exception"));
            }

            @Mock
            public synchronized void startSiaDaemon() {
                this.startSiaDaemonCalled++;
            }

        }

        final AppMock mock = new AppMock();
        final App app = new App();
        final Method init = App.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(app);

        assertTrue(mock.checkedSyncDir);
        assertTrue(mock.checkedDataDir);
        assertEquals(App.MaxRetry + 1, mock.prepareWalletCalled);
        assertEquals(App.MaxRetry, mock.startSiaDaemonCalled);

    }

    @Test
    public void testLoadConfig() throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        final Config cfg = new Config();
        cfg.setUserName("testuser@sample.com");
        cfg.setPrimarySeed("a b c d e f g");
        cfg.setDataPieces(5);
        cfg.setParityPieces(12);
        cfg.setIncludeHiddenFiles(true);

        final Path tmpFile = Files.createTempFile(null, null);
        try {

            cfg.save(tmpFile);

            final App app = new App();
            final Method loadConfig = App.class.getDeclaredMethod("loadConfig", Path.class);
            loadConfig.setAccessible(true);
            final Config res = (Config) loadConfig.invoke(app, tmpFile);
            assertEquals(cfg, res);

        } finally {

            final File file = tmpFile.toFile();
            if (file.exists()) {
                assertTrue(file.delete());
            }

        }

    }

    @Test
    public void testLoadConfigWithoutConfigFile() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, IOException {

        final Path tmpFile = Files.createTempFile(null, null);
        assertTrue(tmpFile.toFile().delete());

        final App app = new App();
        final Method loadConfig = App.class.getDeclaredMethod("loadConfig", Path.class);
        loadConfig.setAccessible(true);
        final Config res = (Config) loadConfig.invoke(app, tmpFile);
        assertEquals(new Config(), res);

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
        final App app = new App();
        final Method checkAndCreateSyncDir = App.class.getDeclaredMethod("checkAndCreateSyncDir");
        checkAndCreateSyncDir.setAccessible(true);
        checkAndCreateSyncDir.invoke(app);

        assertEquals(Utils.getSyncDir(), mock.arg);

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
    public void testPrepareWallet() throws ApiException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        ctx.config.setPrimarySeed("abcdefg");
        final String address = "012345567890";

        new Expectations() {{
            final InlineResponse20013 res = new InlineResponse20013();
            res.setUnlocked(false);
            wallet.walletGet();
            result = res;

            wallet.walletUnlockPost(ctx.config.getPrimarySeed());
        }};

        new Expectations() {{
            final InlineResponse20014 res = new InlineResponse20014();
            res.setAddress(address);
            wallet.walletAddressGet();
            result = res;
        }};

        final Method init = App.class.getDeclaredMethod("prepareWallet");
        init.setAccessible(true);
        final App app = new App();
        Deencapsulation.setField(app, "ctx", ctx);
        init.invoke(app);

    }

    @Test
    public void testPrepareWalletWithUnlockedWallet() throws ApiException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        new Expectations() {{
            final InlineResponse20013 res = new InlineResponse20013();
            res.setUnlocked(true);
            wallet.walletGet();
            result = res;
        }};

        final Method init = App.class.getDeclaredMethod("prepareWallet");
        init.setAccessible(true);
        final App app = new App();
        Deencapsulation.setField(app, "ctx", ctx);
        init.invoke(app);

    }

    @Test
    public void testPrepareWalletWithUninitializedWallet() throws ApiException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {

        final String primarySeed = "1234567890";
        final String address = "012345567890";

        new Expectations() {{
            final InlineResponse20013 res = new InlineResponse20013();
            res.setUnlocked(false);
            wallet.walletGet();
            result = res;
        }};

        new Expectations() {{
            wallet.walletUnlockPost(ctx.config.getPrimarySeed());
            result = new ApiException();
        }};

        new Expectations() {{
            final InlineResponse20016 seed = new InlineResponse20016();
            seed.setPrimaryseed(primarySeed);
            wallet.walletInitPost("", null, false);
            result = seed;
        }};

        new Expectations() {{
            wallet.walletUnlockPost(primarySeed);
        }};

        new Expectations() {{
            final InlineResponse20014 res = new InlineResponse20014();
            res.setAddress(address);
            wallet.walletAddressGet();
            result = res;
        }};

        final Path tmpFile = Files.createTempFile(null, null);
        try {

            final App app = new App();
            Deencapsulation.setField(app, "configPath", tmpFile);
            Deencapsulation.setField(app, "ctx", ctx);

            final Method init = App.class.getDeclaredMethod("prepareWallet");
            init.setAccessible(true);
            init.invoke(app);

            final Config cfg = Config.load(tmpFile);
            assertEquals(primarySeed, cfg.getPrimarySeed());

        } finally {

            assertTrue(tmpFile.toFile().delete());

        }

    }

    @Test
    public void testPrepareWalletWithImportingWallet() throws ApiException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final String primarySeed = "abcdefg";
        final String address = "012345567890";
        ctx.config.setPrimarySeed(primarySeed);

        new Expectations() {{
            final InlineResponse20013 res = new InlineResponse20013();
            res.setUnlocked(false);
            wallet.walletGet();
            result = res;
        }};

        new Expectations() {{
            wallet.walletUnlockPost(primarySeed);
            result = new ApiException("expected error");
            result = null;

        }};

        new Expectations() {{
            final InlineResponse2006 res2 = new InlineResponse2006();
            res2.setSynced(true);
            consensus.consensusGet();
            result = res2;
        }};

        new Expectations() {{
            wallet.walletInitSeedPost("", ctx.config.getPrimarySeed(), true, null);
        }};

        new Expectations() {{
            final InlineResponse20014 res3 = new InlineResponse20014();
            res3.setAddress(address);
            wallet.walletAddressGet();
            result = res3;
        }};

        final App app = new App();
        final Method init = App.class.getDeclaredMethod("prepareWallet");
        init.setAccessible(true);
        Deencapsulation.setField(app, "ctx", ctx);
        init.invoke(app);

    }


    @SuppressWarnings("unused")
    @Test
    public void testWaitSynchronization(@Mocked Thread thread)
            throws ApiException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        new Expectations() {{
            final InlineResponse2006 res1 = new InlineResponse2006();
            res1.setSynced(false);

            final InlineResponse2006 res2 = new InlineResponse2006();
            res2.setSynced(true);

            consensus.consensusGet();
            returns(res1, res2);
            Thread.sleep(App.DefaultSleepTime);

        }};

        final App app = new App();
        Deencapsulation.setField(app, "ctx", ctx);
        final Method waitSynchronization = App.class.getDeclaredMethod("waitSynchronization");
        waitSynchronization.setAccessible(true);
        waitSynchronization.invoke(app);

    }

    @Test
    public void testWaitSynchronizationWithoutSleet() throws ApiException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        new Expectations() {{
            final InlineResponse2006 res = new InlineResponse2006();
            res.setSynced(true);
            consensus.consensusGet();
            result = res;
        }};

        final App app = new App();
        Deencapsulation.setField(app, "ctx", ctx);
        final Method waitSynchronization = App.class.getDeclaredMethod("waitSynchronization");
        waitSynchronization.setAccessible(true);
        waitSynchronization.invoke(app);

    }

    @SuppressWarnings("unused")
    @Test
    public void testWaitContracts(@Mocked Thread thread)
            throws ApiException, InterruptedException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final List<InlineResponse2009Contracts> contracts = IntStream.range(0, App.MinContracts + 1).mapToObj(i -> {
            final InlineResponse2009Contracts c = new InlineResponse2009Contracts();
            c.setId(String.valueOf(i));
            c.setNetaddress("aaa-bbb-ccc");
            c.setRenterfunds("1234");
            return c;
        }).collect(Collectors.toList());

        new Expectations() {{
            // First call: returns not enough contracts.
            final InlineResponse2009 res1 = new InlineResponse2009();
            res1.setContracts(contracts.subList(0, App.MinContracts / 2));

            // Second call: returns enough contracts.
            final InlineResponse2009 res2 = new InlineResponse2009();
            res2.setContracts(contracts);

            renter.renterContractsGet();
            returns(res1, res2);
            Thread.sleep(App.DefaultSleepTime);

        }};

        final App app = new App();
        Deencapsulation.setField(app, "ctx", ctx);
        final Method waitContracts = App.class.getDeclaredMethod("waitContracts");
        waitContracts.setAccessible(true);
        waitContracts.invoke(app);

    }

    @Test
    public void testWaitContractsWithoutSleep() throws ApiException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        final List<InlineResponse2009Contracts> contracts = IntStream.range(0, App.MinContracts + 1).mapToObj(i -> {
            final InlineResponse2009Contracts c = new InlineResponse2009Contracts();
            c.setId(String.valueOf(i));
            c.setNetaddress("aaa-bbb-ccc");
            c.setRenterfunds("1234");
            return c;
        }).collect(Collectors.toList());

        new Expectations() {{
            InlineResponse2009 res = new InlineResponse2009();
            res.setContracts(contracts);
            renter.renterContractsGet();
            result = res;
        }};

        final App app = new App();
        Deencapsulation.setField(app, "ctx", ctx);
        final Method waitContracts = App.class.getDeclaredMethod("waitContracts");
        waitContracts.setAccessible(true);
        waitContracts.invoke(app);

    }

    @Test
    public void synchronizeNewFile() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {

        final Path localPath = Utils.getSyncDir().resolve("sub-dir").resolve(String.format("file-%x", System.currentTimeMillis()));
        Files.createDirectories(localPath.getParent());
        assertTrue(localPath.toFile().createNewFile());

        invokeSynchronizeModifiedFiles();

        assertTrue(DB.get(localPath).isPresent());
        assertEquals(SyncState.MODIFIED, DB.get(localPath).map(SyncFile::getState).orElse(null));

    }

    @Test
    public void synchronizeNotModifiedFile() throws InvocationTargetException, IllegalAccessException, IOException, NoSuchMethodException {

        final String dummyData = "sample test";
        final Path name = Paths.get("sub-dir", String.format("file-%x", System.currentTimeMillis()));
        final Path localPath = Utils.getSyncDir().resolve(name);
        Files.createDirectories(localPath.getParent());
        Files.write(localPath, dummyData.getBytes(), StandardOpenOption.CREATE);
        DB.setSynced(new CloudFile() {
            @Override
            public String getName() {
                return name.toString();
            }

            @Override
            public Path getCloudPath() {
                return null;
            }

            @Override
            public long getFileSize() {
                return 0;
            }
        }, localPath);

        invokeSynchronizeModifiedFiles();

        assertTrue(DB.get(localPath).isPresent());
        assertEquals(SyncState.SYNCED, DB.get(localPath).map(SyncFile::getState).orElse(null));

    }

    @Test
    public void synchronizeModifiedFile() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        final String dummyData = "sample test";
        final Path name = Paths.get("sub-dir", String.format("file-%x", System.currentTimeMillis()));
        final Path localPath = Utils.getSyncDir().resolve(name);
        Files.createDirectories(localPath.getParent());
        Files.write(localPath, dummyData.getBytes(), StandardOpenOption.CREATE);
        DB.setSynced(new CloudFile() {
            @Override
            public String getName() {
                return name.toString();
            }

            @Override
            public Path getCloudPath() {
                return null;
            }

            @Override
            public long getFileSize() {
                return 0;
            }
        }, localPath);

        Files.write(localPath, dummyData.getBytes(), StandardOpenOption.APPEND);

        invokeSynchronizeModifiedFiles();

        assertTrue(DB.get(localPath).isPresent());
        assertEquals(SyncState.MODIFIED, DB.get(localPath).map(SyncFile::getState).orElse(null));

    }

    private void invokeSynchronizeModifiedFiles() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        final App app = new App();
        final Method synchronizeModifiedFiles = App.class.getDeclaredMethod("synchronizeModifiedFiles", Path.class);
        synchronizeModifiedFiles.setAccessible(true);
        synchronizeModifiedFiles.invoke(app, Utils.getSyncDir());
    }

    @Test
    public void synchronizeDeletedFile() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        final String dummyData = "sample test";
        final Path name = Paths.get("sub-dir", String.format("file-%x", System.currentTimeMillis()));
        final Path localPath = Utils.getSyncDir().resolve(name);
        Files.createDirectories(localPath.getParent());
        Files.write(localPath, dummyData.getBytes(), StandardOpenOption.CREATE);
        DB.setSynced(new CloudFile() {
            @Override
            public String getName() {
                return name.toString();
            }

            @Override
            public Path getCloudPath() {
                return null;
            }

            @Override
            public long getFileSize() {
                return 0;
            }
        }, localPath);

        assertTrue(localPath.toFile().delete());

        final App app = new App();
        final Method synchronizeDeletedFiles = App.class.getDeclaredMethod("synchronizeDeletedFiles");
        synchronizeDeletedFiles.setAccessible(true);
        synchronizeDeletedFiles.invoke(app);

        assertTrue(DB.get(localPath).isPresent());
        assertEquals(SyncState.DELETED, DB.get(localPath).map(SyncFile::getState).orElse(null));

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
        final Path localPath = Utils.getSyncDir().resolve(name);
        assertTrue(localPath.toFile().createNewFile());

        DB.setSynced(new CloudFile() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Path getCloudPath() {
                return null;
            }

            @Override
            public long getFileSize() {
                return 0;
            }
        }, localPath);

        final SyncFile syncFile = DB.get(localPath).get();
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

        new Expectations() {{
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

}