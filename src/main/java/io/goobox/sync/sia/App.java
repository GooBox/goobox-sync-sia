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
import io.goobox.sync.sia.client.ApiClient;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.ConsensusApi;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.WalletApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse20014;
import io.goobox.sync.sia.client.api.model.InlineResponse20016;
import io.goobox.sync.sia.client.api.model.InlineResponse2006;
import io.goobox.sync.sia.command.CmdUtils;
import io.goobox.sync.sia.command.CreateAllowance;
import io.goobox.sync.sia.command.Wallet;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncState;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The goobox-sync-sia App.
 */
public class App {

    // Constants.
    public static final String Name;
    public static final String Description = "Sync app for Sia";

    static {
        String name = "goobox-sync-sia";
        if (SystemUtils.IS_OS_WINDOWS) {
            name = String.format("%s.bat", name);
        }
        Name = name;
    }

    /**
     * Version information.
     */
    public static final String Version = "0.0.10";

    /**
     * The number of the minimum required contructs.
     */
    static final int MinContracts = 20;

    /**
     * Default sleep time to wait synchronization and signing contracts.
     */
    public static final long DefaultSleepTime = 60 * 1000;

    /**
     * Default config file name.
     */
    public static final String ConfigFileName = "goobox.properties";

    /**
     * The number of worker threads.
     */
    private static final int WorkerThreadSize = 2;

    /**
     * How many times retrying to start SIA daemon.
     */
    public static final int MaxRetry = 60 * 24;

    private static final Logger logger = LogManager.getLogger();

    // Static fields.
    private static App app;

    // Instance fields.
    @NotNull
    private final Path configPath;
    @NotNull
    private final Config cfg;
    @NotNull
    private final Context ctx;

    @Nullable
    private SiaDaemon daemon;

    /**
     * The main function.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {

        if (args.length != 0) {
            // Checking sub commands.
            switch (args[0]) {
                case Wallet.CommandName:
                    Wallet.main(Arrays.copyOfRange(args, 1, args.length));
                    return;

                case CreateAllowance.CommandName:
                    CreateAllowance.main((Arrays.copyOfRange(args, 1, args.length)));
                    return;

            }
        }

        final Options opts = new Options();
        opts.addOption(null, "reset-db", false, "reset sync DB");
        opts.addOption("h", "help", false, "show this help");
        opts.addOption("v", "version", false, "print version");
        try {

            final CommandLine cmd = new DefaultParser().parse(opts, args);
            if (cmd.hasOption("h")) {
                App.printHelp(opts);
                return;
            }

            if (cmd.hasOption("v")) {
                System.out.println(String.format("Version %s", App.Version));
                return;
            }

            if (cmd.hasOption("reset-db")) {

                final File dbFile = Utils.getDataDir().resolve(DB.DatabaseFileName).toFile();
                logger.info("Deleting old sync database {}", dbFile);
                if (dbFile.exists()) {
                    if (!dbFile.delete()) {
                        logger.error("Cannot delete old sync database");
                    }
                }

            }

            // Start the app.
            App.app = new App();
            App.app.init();

        } catch (ParseException e) {

            logger.error("Failed to parse command line options: {}", e.getMessage());
            App.printHelp(opts);
            System.exit(1);

        } catch (IOException e) {

            logger.error("Failed to start this application: {}", e.getMessage());
            System.exit(1);

        }

    }

    public App() {

        this.configPath = Utils.getDataDir().resolve(ConfigFileName);
        this.cfg = this.loadConfig(this.configPath);

        final ApiClient apiClient = CmdUtils.getApiClient();
        this.ctx = new Context(cfg, apiClient);

    }

    @Nullable
    public static App getInstance() {
        return App.app;
    }

    synchronized void startSiaDaemon() {

        if (this.daemon == null || this.daemon.isClosed()) {

            this.daemon = new SiaDaemon(this.cfg.getDataDir().resolve("sia"));
            try {
                this.daemon.checkAndDownloadConsensusDB();
                Runtime.getRuntime().addShutdownHook(new Thread(this.daemon::close));

                logger.info("Starting SIA daemon");
                this.daemon.start();
            } catch (IOException e) {
                logger.error("Failed to start SIA daemon: {}", e.getMessage());
            }

        }

    }

    /**
     * Initialize the app and starts an event loop.
     */
    private void init() throws IOException {

        if (!checkAndCreateSyncDir()) {
            System.exit(1);
            return;
        }
        if (!checkAndCreateDataDir()) {
            System.exit(1);
            return;
        }


        int retry = 0;
        while (true) {

            try {

                this.prepareWallet();
                this.waitSynchronization();
                this.waitContracts();
                break;

            } catch (final ApiException e) {

                if (retry >= App.MaxRetry) {
                    logger.error("Failed to communicate SIA daemon: {}", APIUtils.getErrorMessage(e));
                    System.exit(1);
                    return;
                }

                if (e.getCause() instanceof ConnectException) {
                    logger.info("SIA daemon is not running: {}", APIUtils.getErrorMessage(e));
                    this.startSiaDaemon();
                }
                retry++;

                logger.info("Waiting SIA daemon starts");
                try {
                    Thread.sleep(DefaultSleepTime);
                } catch (InterruptedException e1) {
                    logger.error("Interrupted while waiting SIA daemon starts: {}", e1.getMessage());
                    System.exit(1);
                    return;
                }

            }

        }

        this.synchronizeModifiedFiles(this.ctx.config.getSyncDir());
        this.synchronizeDeletedFiles();

        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(WorkerThreadSize);
        this.resumeTasks(ctx, executor);
        executor.scheduleWithFixedDelay(
                new RetryableTask(new CheckStateTask(ctx, executor), new StartSiaDaemonTask()),
                0, 60, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(
                new RetryableTask(new CheckDownloadStateTask(ctx), new StartSiaDaemonTask()),
                30, 60, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(
                new RetryableTask(new CheckUploadStateTask(ctx), new StartSiaDaemonTask()),
                45, 60, TimeUnit.SECONDS);
        new FileWatcher(this.ctx.config.getSyncDir(), executor);

    }

    /**
     * Load configuration.
     *
     * @param path to the config file.
     * @return a Config object.
     */
    private Config loadConfig(final Path path) {

        Config cfg;
        try {
            cfg = Config.load(path);
        } catch (IOException e) {
            logger.error("cannot load config file {}: {}", path, e.getMessage());
            cfg = new Config();
        }
        return cfg;

    }

    /**
     * Creates a directory which will be synchronized with cloud storage if not exists.
     *
     * @return true if the synchronizing directory is ready.
     */
    private boolean checkAndCreateSyncDir() {
        logger.info("Checking if local Goobox sync folder exists: {}", this.ctx.config.getSyncDir());
        return checkAndCreateFolder(this.ctx.config.getSyncDir());
    }

    /**
     * Create a directory which have Goobox data files if not exists.
     *
     * @return true if the data directory is ready.
     */
    private boolean checkAndCreateDataDir() {
        logger.info("Checking if Goobox data folder exists: {}", Utils.getDataDir());
        return checkAndCreateFolder(Utils.getDataDir());
    }

    /**
     * Create a folder represented by the given path if not exists.
     *
     * @param path represents a folder to be checked and created.
     * @return true if the target folder has been existed or created.
     */
    private boolean checkAndCreateFolder(Path path) {
        if (Files.exists(path)) {
            return true;
        } else {
            try {
                Files.createDirectory(path);
                logger.info("Folder {} has been created", path);
                return true;
            } catch (IOException e) {
                logger.error("Failed to create folder {}: {}", path, e.getMessage());
                return false;
            }
        }
    }

    /**
     * Prepare the wallet.
     * <p>
     * If no wallets have been created, it'll initialize a wallet.
     *
     * @throws ApiException when an error occurs in Wallet API.
     */
    void prepareWallet() throws ApiException {

        final WalletApi api = new WalletApi(ctx.apiClient);
        final InlineResponse20013 wallet = api.walletGet();

        if (!wallet.getUnlocked()) {

            try {

                logger.info("Unlocking a wallet");
                api.walletUnlockPost(ctx.config.getPrimarySeed());

            } catch (ApiException e) {
                logger.info("Failed to unlock the wallet: {}", APIUtils.getErrorMessage(e));

                try {

                    if (!ctx.config.getPrimarySeed().isEmpty()) {

                        // If a primary seed is given but the corresponding wallet doesn't exist,
                        // initialize a wallet with the seed.
                        this.waitSynchronization();

                        logger.info("Initializing a wallet with the given seed");
                        api.walletInitSeedPost("", ctx.config.getPrimarySeed(), true, null);

                    } else {

                        // If there is no information about wallets, create a wallet.
                        logger.info("Initializing a wallet");
                        final InlineResponse20016 seed = api.walletInitPost("", null, false);
                        ctx.config.setPrimarySeed(seed.getPrimaryseed());

                        try {
                            ctx.config.save(this.configPath);
                        } catch (IOException e1) {
                            logger.error("Cannot save configuration: {}, your primary seed is \"{}\"", e1.getMessage(), ctx.config.getPrimarySeed());
                            System.exit(1);
                        }

                    }

                    // Try to unlock the wallet, again.
                    logger.info("Unlocking a wallet");
                    api.walletUnlockPost(ctx.config.getPrimarySeed());

                } catch (final ApiException e1) {
                    // Cannot initialize new wallet.
                    logger.error("Cannot initialize new wallet: {}", APIUtils.getErrorMessage(e1));
                    System.exit(1);
                }

            }

            try {
                final InlineResponse20014 address = api.walletAddressGet();
                logger.info("Address of the wallet is {}", address.getAddress());
            } catch (ApiException e) {
                logger.error("Cannot get a wallet address: {}", APIUtils.getErrorMessage(e));
            }

        }

    }

    /**
     * Wait until the consensus DB is synced.
     *
     * @throws ApiException when an error occurs in Consensus API.
     */
    void waitSynchronization() throws ApiException {

        logger.info("Checking consensus DB");
        final ConsensusApi api = new ConsensusApi(ctx.apiClient);
        while (true) {

            final InlineResponse2006 res = api.consensusGet();
            if (res.getSynced()) {

                logger.info("Consensus DB is synchronized");
                break;

            } else {

                logger.info("Consensus DB isn't synchronized (block height: {}), wait a minute", res.getHeight());
                try {
                    Thread.sleep(DefaultSleepTime);
                } catch (InterruptedException e) {
                    logger.trace("Thread {} was interrupted until waiting synchronization: {}", Thread.currentThread().getName(), e.getMessage());
                }

            }

        }

    }

    /**
     * Wait until minimum required contracts are signed.
     *
     * @throws ApiException when an error occurs in Renter API.
     */
    void waitContracts() throws ApiException {

        logger.info("Checking contracts");
        final RenterApi api = new RenterApi(ctx.apiClient);
        while (true) {

            final int contracts = api.renterContractsGet().getContracts().size();
            if (contracts >= MinContracts) {

                logger.info("Sufficient contracts have been signed");
                break;

            } else {

                logger.info("Signed contracts aren't enough ({} / {}), wait a minute", contracts, MinContracts);
                try {
                    Thread.sleep(DefaultSleepTime);
                } catch (InterruptedException e) {
                    logger.trace("Thread {} was interrupted until waiting contracts: {}", Thread.currentThread().getName(), e.getMessage());
                }

            }

        }

    }

    private void resumeTasks(final Context ctx, final Executor executor) {

        logger.info("Resume pending uploads if exist");
        DB.getFiles(SyncState.FOR_UPLOAD).forEach(syncFile -> syncFile.getLocalPath().ifPresent(localPath -> {
            logger.info("File {} is going to be uploaded", syncFile.getName());
            executor.execute(new RetryableTask(new UploadLocalFileTask(ctx, localPath), new StartSiaDaemonTask()));
        }));

        logger.info("Resume pending downloads if exist");
        DB.getFiles(SyncState.FOR_DOWNLOAD).forEach(syncFile -> {
            logger.info("File {} is going to be downloaded", syncFile.getName());
            executor.execute(new RetryableTask(new DownloadCloudFileTask(ctx, syncFile.getName()), new StartSiaDaemonTask()));
        });

        logger.info("Resume pending deletes from the cloud network if exist");
        DB.getFiles(SyncState.FOR_CLOUD_DELETE).forEach(syncFile -> {
            logger.info("File {} is going to be deleted from the cloud network", syncFile.getName());
            executor.execute(new RetryableTask(new DeleteCloudFileTask(ctx, syncFile.getName()), new StartSiaDaemonTask()));
        });

        logger.info("Resume pending deletes from the local directory if exist");
        DB.getFiles(SyncState.FOR_LOCAL_DELETE).forEach(syncFile -> syncFile.getLocalPath().ifPresent(localPath -> {
            logger.info("File {} is going to be deleted from the local directory", syncFile.getName());
            executor.execute(new DeleteLocalFileTask(ctx, localPath));
        }));

    }

    private void synchronizeModifiedFiles(final Path rootDir) {
        logger.debug("Checking modified files in {}", rootDir);

        try {

            Files.list(rootDir).filter(localPath -> !Utils.isExcluded(localPath)).forEach(localPath -> {

                if (localPath.toFile().isDirectory()) {
                    synchronizeModifiedFiles(localPath);
                    return;
                }

                final String name = ctx.getName(localPath);
                final boolean modified = DB.get(name).flatMap(syncFile -> syncFile.getLocalDigest().map(digest -> {

                    try {
                        final String currentDigest = DigestUtils.sha512Hex(new FileInputStream(localPath.toFile()));
                        return !digest.equals(currentDigest);
                    } catch (IOException e) {
                        logger.error("Failed to read {}: {}", localPath, e.getMessage());
                        return false;
                    }

                })).orElse(true);

                if (modified) {
                    try {
                        logger.debug("File {} has been modified", localPath);
                        DB.setModified(name, localPath);
                    } catch (IOException e) {
                        logger.error("Failed to update state of {}: {}", localPath, e.getMessage());
                    }
                }

            });

        } catch (IOException e) {
            logger.error("Failed to list files in {}: {}", rootDir, e.getMessage());
        }

    }

    private void synchronizeDeletedFiles() {
        logger.debug("Checking deleted files");

        DB.getFiles(SyncState.SYNCED).forEach(syncFile -> syncFile.getLocalPath().ifPresent(localPath -> {

            if (!localPath.toFile().exists()) {
                logger.debug("File {} has been deleted", localPath);
                DB.setDeleted(ctx.getName(localPath));
            }

        }));

    }

    @SuppressWarnings("StringBufferReplaceableByString")
    private static void printHelp(final Options opts) {

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

        final HelpFormatter help = new HelpFormatter();
        help.printHelp(Name, Description, opts, builder.toString(), true);

    }

}
