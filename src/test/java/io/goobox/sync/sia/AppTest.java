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
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.storj.Utils;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    public void setUp() {

        final Config cfg = new Config();
        cfg.setUserName("test-user");
        this.ctx = new Context(cfg, null);

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
        UtilsMock.dataDir = Files.createTempDirectory(null);
        try {

            final Path db = UtilsMock.dataDir.resolve(DB.DatabaseFileName);
            assertTrue(db.toFile().createNewFile());
            new UtilsMock();

            App.main(new String[]{"--reset-db"});
            assertTrue("check app is initialized", mock.initialized);
            assertFalse("check database files exists", db.toFile().exists());

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
        builder.append("\nSubcommands:\n");
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
    public void testInit(@Mocked ScheduledThreadPoolExecutor executor, @Mocked CmdUtils utils, @Mocked FileWatcher watcher)
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
            private void prepareWallet(Context context) {
                assertEquals(ctx, context);
                this.preparedWallet = true;
            }

            @Mock
            private void waitSynchronization(Context context) {
                assertEquals(ctx, context);
                this.waitedSynchronization = true;
            }

            @Mock
            private void waitContracts(Context context) {
                assertEquals(ctx, context);
                this.waitedContracts = true;
            }

        }

        // Enqueue basic tasks.
        new Expectations() {{
            executor.scheduleWithFixedDelay(withAny(new CheckStateTask(ctx, executor)), 0L, 60, TimeUnit.SECONDS);
            executor.scheduleWithFixedDelay(new CheckDownloadStatusTask(ctx), 30, 60, TimeUnit.SECONDS);
            executor.scheduleWithFixedDelay(new CheckUploadStatusTask(ctx), 45, 60, TimeUnit.SECONDS);
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

        final Method init = App.class.getDeclaredMethod("prepareWallet", Context.class);
        init.setAccessible(true);
        init.invoke(new App(), ctx);

    }

    @Test
    public void testPrepareWalletWithUnlockedWallet() throws ApiException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        new Expectations() {{
            final InlineResponse20013 res = new InlineResponse20013();
            res.setUnlocked(true);
            wallet.walletGet();
            result = res;
        }};

        final Method init = App.class.getDeclaredMethod("prepareWallet", Context.class);
        init.setAccessible(true);
        init.invoke(new App(), ctx);

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

            final Method init = App.class.getDeclaredMethod("prepareWallet", Context.class);
            init.setAccessible(true);
            init.invoke(app, ctx);

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
        final Method init = App.class.getDeclaredMethod("prepareWallet", Context.class);
        init.setAccessible(true);
        init.invoke(app, ctx);

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
        final Method waitSynchronization = App.class.getDeclaredMethod("waitSynchronization", Context.class);
        waitSynchronization.setAccessible(true);
        waitSynchronization.invoke(app, this.ctx);

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
        final Method waitSynchronization = App.class.getDeclaredMethod("waitSynchronization", Context.class);
        waitSynchronization.setAccessible(true);
        waitSynchronization.invoke(app, this.ctx);

    }

    @SuppressWarnings("unused")
    @Test
    public void testWaitContracts(@Mocked Thread thread)
            throws ApiException, InterruptedException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final List<InlineResponse2009Contracts> contracts = new ArrayList<>();
        for (int i = 0; i != App.MinContracts + 1; ++i) {
            final InlineResponse2009Contracts c = new InlineResponse2009Contracts();
            c.setId(String.valueOf(i));
            c.setNetaddress("aaa-bbb-ccc");
            c.setRenterfunds("1234");
            contracts.add(c);
        }

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
        final Method waitContracts = App.class.getDeclaredMethod("waitContracts", Context.class);
        waitContracts.setAccessible(true);
        waitContracts.invoke(app, this.ctx);

    }

    @Test
    public void testWaitContractsWithoutSleep() throws ApiException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        final List<InlineResponse2009Contracts> contracts = new ArrayList<>();
        for (int i = 0; i != App.MinContracts + 1; ++i) {
            final InlineResponse2009Contracts c = new InlineResponse2009Contracts();
            c.setId(String.valueOf(i));
            c.setNetaddress("aaa-bbb-ccc");
            c.setRenterfunds("1234");
            contracts.add(c);
        }

        new Expectations() {{
            InlineResponse2009 res = new InlineResponse2009();
            res.setContracts(contracts);
            renter.renterContractsGet();
            result = res;
        }};

        final App app = new App();
        final Method waitContracts = App.class.getDeclaredMethod("waitContracts", Context.class);
        waitContracts.setAccessible(true);
        waitContracts.invoke(app, this.ctx);

    }

}