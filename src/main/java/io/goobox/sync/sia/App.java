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
import io.goobox.sync.sia.command.Wallet;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.storj.Utils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The goobox-sync-sia App.
 */
public class App {

    /**
     * The number of the minimum required contructs.
     */
    static final int MIN_CONTRACTS = 20;

    /**
     * Default config file name.
     */
    static final String CONFIG_FILE = "goobox.properties";

    private Path configPath;
    private static final Logger logger = LogManager.getLogger();

    /**
     * The main function.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {

        if(args.length != 0 && args[0].equals("wallet")){
            Wallet.main(Arrays.copyOfRange(args, 1, args.length));
            return;
        }

        final Options opts = new Options();
        opts.addOption(null, "reset-db", false, "reset sync DB");
        opts.addOption("h", "help", false, "show this help");
        try {

            final CommandLine cmd = new DefaultParser().parse(opts, args);
            if (cmd.hasOption("h")) {
                final HelpFormatter help = new HelpFormatter();
                help.printHelp("goobox-sync-sia", opts, true);
                System.exit(0);
            }

            if (cmd.hasOption("reset-db")) {

                final File dbFile = Utils.getDataDir().resolve(DB.DatabaseFileName).toFile();
                logger.info("Deleting old sync database {}", dbFile);
                if (dbFile.exists()) {
                    dbFile.delete();
                }

            }

            // Start the app.
            new App().init();

        } catch (ParseException e) {
            logger.error("Failed to parse command line options: {}", e.getMessage());

            final HelpFormatter help = new HelpFormatter();
            help.printHelp("goobox-sync-sia", opts, true);
            System.exit(1);

        }

    }

    /**
     * Initialize the app and starts an event loop.
     */
    private void init() {

        this.configPath = Utils.getDataDir().resolve(CONFIG_FILE);
        final Config cfg = this.loadConfig(this.configPath);

        if (!checkAndCreateSyncDir()) {
            System.exit(1);
        }
        if (!checkAndCreateDataDir()) {
            System.exit(1);
        }

        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://localhost:9980");
        final OkHttpClient httpClient = apiClient.getHttpClient();
        httpClient.setConnectTimeout(0, TimeUnit.MILLISECONDS);
        httpClient.setReadTimeout(0, TimeUnit.MILLISECONDS);

        final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
        final Context ctx = new Context(cfg, apiClient);

        try {

            this.prepareWallet(ctx);
            this.waitSynchronization(ctx);
            this.waitContracts(ctx);

        } catch (ApiException e) {

            this.logger.error("Failed to communicate SIA daemon: {}", APIUtils.getErrorMessage(e));
            System.exit(1);

        }

        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay​(new CheckStateTask(ctx, executor), 0, 30, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay​(new CheckDownloadStatusTask(ctx), 0, 30, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay​(new CheckUploadStatusTask(ctx), 0, 30, TimeUnit.SECONDS);

    }

    /**
     * Load configuration.
     *
     * @param path
     * @return
     */
    private Config loadConfig(final Path path) {

        Config cfg;
        try {
            cfg = Config.load(path);
        } catch (IOException e) {
            this.logger.error("cannot load config file {}: {}", path, e.getMessage());
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
        this.logger.info("Checking if local Goobox sync folder exists: {}", Utils.getSyncDir());
        return checkAndCreateFolder(Utils.getSyncDir());
    }

    /**
     * Create a directory which have Goobox data files if not exists.
     *
     * @return true if the data directory is ready.
     */
    private boolean checkAndCreateDataDir() {
        this.logger.info("Checking if Goobox data folder exists: {}", Utils.getDataDir());
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
                this.logger.info("Folder {} has been created", path);
                return true;
            } catch (IOException e) {
                this.logger.error("Failed to create folder {}: {}", path, e.getMessage());
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

                this.logger.info("Unlocking a wallet");
                api.walletUnlockPost(ctx.config.primarySeed);

            } catch (ApiException e) {
                this.logger.info("Failed to unlock a wallet: {}", APIUtils.getErrorMessage(e));

                try {

                    if (ctx.config.primarySeed != null && !ctx.config.primarySeed.isEmpty()) {

                        // If a primary seed is given but the corresponding wallet doesn't exist,
                        // initialize a wallet with the seed.
                        this.waitSynchronization(ctx);

                        this.logger.info("Initializing a wallet with the given seed");
                        api.walletInitSeedPost("", ctx.config.primarySeed, true, null);

                    } else {

                        // If there is no information about wallets, create a wallet.
                        this.logger.info("Initializing a wallet");
                        final InlineResponse20016 seed = api.walletInitPost("", null, false);
                        ctx.config.primarySeed = seed.getPrimaryseed();
                    }

                    // Try to unlock the wallet, again.
                    this.logger.info("Unlocking a wallet");
                    api.walletUnlockPost(ctx.config.primarySeed);

                } catch (ApiException e1) {
                    // Cannot initialize new wallet.
                    this.logger.error("Cannot initialize new wallet: {}", APIUtils.getErrorMessage(e1));
                    System.exit(1);
                }

                try {
                    ctx.config.save(this.configPath);
                } catch (IOException e1) {
                    this.logger.error("Cannot save configuration: {}, your primary seed is \"{}\"", e1.getMessage(), ctx.config.primarySeed);
                    System.exit(1);
                }

            }

            try {
                final InlineResponse20014 address = api.walletAddressGet();
                this.logger.info("Address of the wallet is {}", address.getAddress());
            } catch (ApiException e) {
                this.logger.error("Cannot get a wallet address: {}", APIUtils.getErrorMessage(e));
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

        this.logger.info("Checking consensus DB");
        final ConsensusApi api = new ConsensusApi(ctx.apiClient);
        while (true) {

            final InlineResponse2006 res = api.consensusGet();
            if (res.getSynced()) {

                this.logger.info("Consensus DB is synchronized");
                break;

            } else {

                this.logger.info("Consensus DB isn't synchronized, wait a minute");
                try {
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException e) {
                    this.logger.trace("Thread {} was interrupted until waiting synchronization: {}", Thread.currentThread().getName(), e.getMessage());
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

        this.logger.info("Checking contracts");
        final RenterApi api = new RenterApi(ctx.apiClient);
        while (true) {

            final int contracts = api.renterContractsGet().getContracts().size();
            if (contracts >= MIN_CONTRACTS) {

                this.logger.info("Sufficient contracts have been signed");
                break;

            } else {

                this.logger.info("Signed contracts aren't enough ({} / {}), wait a minute", contracts, MIN_CONTRACTS);
                try {
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException e) {
                    this.logger.trace("Thread {} was interrupted until waiting contracts: {}", Thread.currentThread().getName(), e.getMessage());
                }

            }

        }

    }

}
