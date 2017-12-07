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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    public static final String CommandName = "goobox-sync-sia";
    public static final String Description = "Sync app for Sia";

    /**
     * Version information.
     */
    public static final String Version = "0.0.6";

    /**
     * The number of the minimum required contructs.
     */
    static final int MinContracts = 20;

    /**
     * Default sleep time to wait synchronization and signing contracts.
     */
    static final long DefaultSleepTime = 60 * 1000;

    /**
     * Default config file name.
     */
    static final String ConfigFileName = "goobox.properties";

    /**
     * The number of worker threads.
     */
    private static final int WorkerThreadSize = 2;

    static final int MaxRetry = 20;

    private static final Logger logger = LogManager.getLogger();

    private Path configPath;

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
            new App().init();

        } catch (ParseException e) {

            logger.error("Failed to parse command line options: {}", e.getMessage());
            App.printHelp(opts);
            System.exit(1);

        } catch (IOException e) {

            logger.error("Failed to start this application: {}", e.getMessage());
            System.exit(1);

        }

    }

    public synchronized void startSiaDaemon() {

        if (this.daemon == null) {

            this.daemon = new SiaDaemon();
            try {
                this.daemon.checkAndDownloadConsensusDB();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> this.daemon.close()));
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

        this.configPath = Utils.getDataDir().resolve(ConfigFileName);
        final Config cfg = this.loadConfig(this.configPath);

        if (!checkAndCreateSyncDir()) {
            System.exit(1);
            return;
        }
        if (!checkAndCreateDataDir()) {
            System.exit(1);
            return;
        }

        final ApiClient apiClient = CmdUtils.getApiClient();
        final Context ctx = new Context(cfg, apiClient);

        int retry = 0;
        while (true) {

            try {

                this.prepareWallet(ctx);
                this.waitSynchronization(ctx);
                this.waitContracts(ctx);
                break;

            } catch (ApiException e) {

                if (!(e.getCause() instanceof ConnectException) || retry >= App.MaxRetry) {
                    logger.error("Failed to communicate SIA daemon: {}", APIUtils.getErrorMessage(e));
                    System.exit(1);
                    return;
                }
                this.startSiaDaemon();
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

        this.synchronizeModifiedFiles(Utils.getSyncDir());
        this.synchronizeDeletedFiles();

        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(WorkerThreadSize);
        this.resumeTasks(ctx, executor);
        executor.scheduleWithFixedDelay(new CheckStateTask(ctx, executor), 0, 60, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(new CheckDownloadStateTask(ctx), 30, 60, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(new CheckUploadStateTask(ctx), 45, 60, TimeUnit.SECONDS);
        new FileWatcher(Utils.getSyncDir(), executor);

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
        logger.info("Checking if local Goobox sync folder exists: {}", Utils.getSyncDir());
        return checkAndCreateFolder(Utils.getSyncDir());
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
     * @param ctx context object.
     * @throws ApiException when an error occurs in Wallet API.
     */
    private void prepareWallet(final Context ctx) throws ApiException {

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
                        this.waitSynchronization(ctx);

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

                } catch (ApiException e1) {
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
     * @param ctx context object.
     * @throws ApiException when an error occurs in Consensus API.
     */
    private void waitSynchronization(final Context ctx) throws ApiException {

        logger.info("Checking consensus DB");
        final ConsensusApi api = new ConsensusApi(ctx.apiClient);
        while (true) {

            final InlineResponse2006 res = api.consensusGet();
            if (res.getSynced()) {

                logger.info("Consensus DB is synchronized");
                break;

            } else {

                logger.info("Consensus DB isn't synchronized, wait a minute");
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
     * @param ctx context object.
     * @throws ApiException when an error occurs in Renter API.
     */
    private void waitContracts(final Context ctx) throws ApiException {

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
            executor.execute(new UploadLocalFileTask(ctx, localPath));
        }));

        logger.info("Resume pending downloads if exist");
        DB.getFiles(SyncState.FOR_DOWNLOAD).forEach(syncFile -> {
            logger.info("File {} is going to be downloaded", syncFile.getName());
            executor.execute(new DownloadCloudFileTask(ctx, syncFile.getName()));
        });

        logger.info("Resume pending deletes from the cloud network if exist");
        DB.getFiles(SyncState.FOR_CLOUD_DELETE).forEach(syncFile -> {
            logger.info("File {} is going to be deleted from the cloud network", syncFile.getName());
            executor.execute(new DeleteCloudFileTask(ctx, syncFile.getName()));
        });

        logger.info("Resume pending deletes from the local directory if exist");
        DB.getFiles(SyncState.FOR_LOCAL_DELETE).forEach(syncFile -> syncFile.getLocalPath().ifPresent(localPath -> {
            logger.info("File {} is going to be deleted from the local directory", syncFile.getName());
            executor.execute(new DeleteLocalFileTask(localPath));
        }));

    }

    private void synchronizeModifiedFiles(final Path rootDir) {
        logger.debug("Checking modified files in {}", rootDir);

        try {

            Files.list(rootDir).filter(path -> !Utils.isExcluded(path)).forEach(path -> {

                if (path.toFile().isDirectory()) {
                    synchronizeModifiedFiles(path);
                    return;
                }

                final boolean modified = DB.get(path).flatMap(syncFile -> syncFile.getLocalDigest().map(digest -> {

                    try {
                        final String currentDigest = DigestUtils.sha512Hex(new FileInputStream(path.toFile()));
                        return !digest.equals(currentDigest);
                    } catch (IOException e) {
                        logger.error("Failed to read {}: {}", path, e.getMessage());
                        return false;
                    }

                })).orElse(true);

                if (modified) {
                    try {
                        logger.debug("File {} has been modified", path);
                        DB.setModified(path);
                    } catch (IOException e) {
                        logger.error("Failed to update state of {}: {}", path, e.getMessage());
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
                DB.setDeleted(localPath);
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
        help.printHelp(CommandName, Description, opts, builder.toString(), true);

    }

}
