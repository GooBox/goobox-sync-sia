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
package io.goobox.sync.sia;

import io.goobox.sync.common.Utils;
import io.goobox.sync.sia.client.ApiClient;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.command.CreateAllowance;
import io.goobox.sync.sia.command.DumpDB;
import io.goobox.sync.sia.command.GatewayConnect;
import io.goobox.sync.sia.command.Wallet;
import io.goobox.sync.sia.db.DB;
import io.goobox.sync.sia.db.SyncState;
import io.goobox.sync.sia.task.CheckDownloadStateTask;
import io.goobox.sync.sia.task.CheckStateTask;
import io.goobox.sync.sia.task.CheckUploadStateTask;
import io.goobox.sync.sia.task.DeleteCloudFileTask;
import io.goobox.sync.sia.task.DeleteLocalFileTask;
import io.goobox.sync.sia.task.DownloadCloudFileTask;
import io.goobox.sync.sia.task.GetWalletInfoTask;
import io.goobox.sync.sia.task.NotifyEmptyFundTask;
import io.goobox.sync.sia.task.NotifyFundInfoTask;
import io.goobox.sync.sia.task.NotifySyncStateTask;
import io.goobox.sync.sia.task.UploadLocalFileTask;
import io.goobox.sync.sia.task.WaitContractsTask;
import io.goobox.sync.sia.task.WaitSynchronizationTask;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The goobox-sync-sia App.
 */
public final class App implements Callable<Integer> {

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
    public static final String Version = "0.0.17";

    /**
     * The number of the minimum required contracts.
     */
    public static final int MinContracts = 20;

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
    static final int WorkerThreadSize = 3;

    /**
     * How many times retrying to start a sia daemon.
     */
    public static final int MaxRetry = 60 * 24;

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    // Static fields.
    private static App app;

    // Instance fields.
    @NotNull
    private final Path configPath;
    @NotNull
    private final Config cfg;
    @NotNull
    private final Context ctx;

    /**
     * If true, this app outputs events to stdout so that the GUI app can obtain them.
     */
    private boolean outputEvents = false;

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
                    CreateAllowance.main(Arrays.copyOfRange(args, 1, args.length));
                    return;

                case GatewayConnect.CommandName:
                    GatewayConnect.main(Arrays.copyOfRange(args, 1, args.length));
                    return;

                case DumpDB.CommandName:
                    DumpDB.main(Arrays.copyOfRange(args, 1, args.length));
                    return;

            }
        }

        final Options opts = new Options();
        opts.addOption(null, "reset-db", false, "reset sync DB");
        opts.addOption(null, "sync-dir", true, "set the sync dir");
        opts.addOption(null, "output-events", false, "output events for the GUI app");
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

            if (cmd.hasOption("sync-dir")) {
                App.app = new App(Paths.get(cmd.getParsedOptionValue("sync-dir").toString()));
            } else {
                App.app = new App();
            }

            if (cmd.hasOption("output-events")) {
                App.app.setOutputEvents(true);
            }

            // Start the app.
            System.exit(App.app.call());

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

        final ApiClient apiClient = APIUtils.getApiClient();
        this.ctx = new Context(cfg, apiClient);

    }

    public App(final Path syncDir) {
        this();
        logger.info("Overwrite the sync directory: {}", syncDir);
        this.cfg.setSyncDir(syncDir);
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

                logger.info("Starting a sia daemon");
                this.daemon.start();
            } catch (IOException e) {
                logger.error("Failed to start the sia daemon: {}", e.getMessage());
            }

        }

    }

    void setOutputEvents(boolean outputEvents) {
        this.outputEvents = outputEvents;
    }

    /**
     * Initialize the app and starts an event loop.
     */
    public Integer call() throws IOException {

        if (!checkAndCreateSyncDir()) {
            return 1;
        }
        if (!checkAndCreateDataDir()) {
            return 1;
        }

        int retry = 0;
        while (true) {

            try {

                final GetWalletInfoTask getWalletInfo = new GetWalletInfoTask(this.ctx);
                getWalletInfo.call();

                final WaitSynchronizationTask waitSynchronizationTask = new WaitSynchronizationTask(this.ctx);
                waitSynchronizationTask.call();

                if (this.outputEvents) {
                    final NotifyEmptyFundTask notifyEmptyFundTask = new NotifyEmptyFundTask(this.ctx);
                    notifyEmptyFundTask.run();
                }

                final WaitContractsTask waitContractsTask = new WaitContractsTask(this.ctx);
                waitContractsTask.call();
                break;

            } catch (final ApiException e) {

                if (retry >= App.MaxRetry) {
                    logger.error("Failed to communicate with the sia daemon: {}", APIUtils.getErrorMessage(e));
                    return 1;
                }

                if (e.getCause() instanceof ConnectException) {
                    logger.info("Sia daemon is not running: {}", APIUtils.getErrorMessage(e));
                    this.startSiaDaemon();
                }
                retry++;

                logger.info("Waiting the daemon starts");
                try {
                    Thread.sleep(DefaultSleepTime);
                } catch (InterruptedException e1) {
                    logger.error("Interrupted while waiting for the sia daemon to start: {}", e1.getMessage());
                    return 1;
                }

            } catch (GetWalletInfoTask.WalletException e) {
                e.printStackTrace();
                return 1;
            }

        }

        this.synchronizeModifiedFiles(this.ctx.config.getSyncDir());
        this.synchronizeDeletedFiles();

        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(WorkerThreadSize);
        this.resumeTasks(ctx, executor);

        final RecoveryTask startSiaDaemonTask = new StartSiaDaemonTask();
        executor.scheduleWithFixedDelay(
                new RetryableTask(new CheckStateTask(ctx, executor), startSiaDaemonTask),
                0, 60, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(
                new RetryableTask(new CheckDownloadStateTask(ctx), startSiaDaemonTask),
                30, 60, TimeUnit.SECONDS);
        executor.scheduleWithFixedDelay(
                new RetryableTask(new CheckUploadStateTask(ctx), startSiaDaemonTask),
                45, 60, TimeUnit.SECONDS);
        if (this.outputEvents) {
            executor.scheduleWithFixedDelay(new NotifySyncStateTask(), 0, 60, TimeUnit.SECONDS);
            executor.scheduleWithFixedDelay(
                    new NotifyFundInfoTask(ctx, !this.ctx.getConfig().isDisableAutoAllocation()), 0, 1, TimeUnit.HOURS);
        }
        new FileWatcher(this.ctx.config.getSyncDir(), executor);
        return 0;

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
    boolean checkAndCreateSyncDir() {
        logger.info("Checking if local Goobox sync folder exists: {}", this.ctx.config.getSyncDir());
        return checkAndCreateFolder(this.ctx.config.getSyncDir());
    }

    /**
     * Create a directory which have Goobox data files if not exists.
     *
     * @return true if the data directory is ready.
     */
    boolean checkAndCreateDataDir() {
        logger.info("Checking if Goobox data folder exists: {}", this.ctx.config.getDataDir());
        return checkAndCreateFolder(this.ctx.config.getDataDir());
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

    void resumeTasks(final Context ctx, final Executor executor) {

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

    void synchronizeModifiedFiles(final Path rootDir) {
        logger.debug("Checking modified files in {}", rootDir);

        try {

            Files.list(rootDir).filter(localPath -> !Utils.isExcluded(localPath)).forEach(localPath -> {

                if (localPath.toFile().isDirectory()) {
                    synchronizeModifiedFiles(localPath);
                    return;
                }

                final String name = ctx.getName(localPath);
                final boolean modified = DB.get(name).flatMap(syncFile -> syncFile.getLocalDigest().map(digest -> {

                    try (final FileInputStream in = new FileInputStream(localPath.toFile())) {
                        final String currentDigest = DigestUtils.sha512Hex(in);
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

    void synchronizeDeletedFiles() {
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

        final HelpFormatter help = new HelpFormatter();
        help.printHelp(Name, Description, opts, buffer.toString(), true);

    }

}
