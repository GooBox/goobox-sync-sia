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

package io.goobox.sync.sia.command;

import io.goobox.sync.common.Utils;
import io.goobox.sync.sia.APIUtils;
import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.SiaDaemon;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.task.GetWalletInfoTask;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.nio.file.Path;

/**
 * Wallet command shows wallet information.
 */
public final class Wallet implements Runnable {

    public static final String CommandName = "wallet";
    public static final String Description = "Show your wallet information";
    private static final Logger logger = LoggerFactory.getLogger(Wallet.class);

    public static void main(String[] args) {

        final Options opts = new Options();
        opts.addOption(null, "force", false, "force initialize a wallet if not exists");
        opts.addOption("h", "help", false, "show this help");

        boolean force;
        try {

            final CommandLine cmd = new DefaultParser().parse(opts, args);
            if (cmd.hasOption("h")) {
                final HelpFormatter help = new HelpFormatter();
                help.printHelp(String.format("%s %s", App.Name, CommandName), Description, opts, "", true);
                return;
            }
            force = cmd.hasOption("force");

        } catch (final ParseException e) {
            logger.error("Failed to parse command line options: {}", e.getMessage());

            final HelpFormatter help = new HelpFormatter();
            help.printHelp(String.format("%s %s", App.Name, CommandName), Description, opts, "", true);
            System.exit(1);
            return;

        }

        // Run this command.
        new Wallet(force).run();

    }

    @NotNull
    private final Path configPath;
    @NotNull
    private final Config cfg;
    @Nullable
    private SiaDaemon daemon = null;
    private boolean force = false;

    public Wallet() {
        configPath = Utils.getDataDir().resolve(App.ConfigFileName);
        this.cfg = APIUtils.loadConfig(configPath);
    }

    public Wallet(final boolean force) {
        this();
        this.force = force;
    }

    @Override
    public void run() {

        final GetWalletInfoTask task = new GetWalletInfoTask(new Context(this.cfg, APIUtils.getApiClient()), this.force);
        int retry = 0;
        while (true) {

            try {
                final GetWalletInfoTask.InfoPair pair = task.call();
                System.out.println(pair.getWalletInfo().toString());
                System.out.println(pair.getPriceInfo().toString());
                break;
            } catch (final ApiException e) {

                if (retry >= App.MaxRetry) {
                    logger.error("Failed to communicate with the sia daemon: {}", APIUtils.getErrorMessage(e));
                    System.exit(1);
                    return;
                }

                if (e.getCause() instanceof ConnectException) {

                    logger.info("Failed to access sia daemon: {}", APIUtils.getErrorMessage(e));
                    if (daemon == null) {
                        daemon = new SiaDaemon(cfg.getDataDir().resolve("sia"));
                        Runtime.getRuntime().addShutdownHook(new Thread(() -> daemon.close()));

                        logger.info("Starting a sia daemon");
                        daemon.start();
                    }
                    logger.info("Waiting for the sia daemon to get ready");

                } else {
                    logger.warn("Failed to get wallet information: {}", APIUtils.getErrorMessage(e));
                }

                try {
                    if (retry == 0) {
                        Thread.sleep(5000);
                    } else {
                        Thread.sleep(App.DefaultSleepTime);
                    }
                    retry++;
                } catch (final InterruptedException e1) {
                    logger.error("Interrupted while waiting for preparing a wallet: {}", e1.getMessage());
                    break;
                }

            } catch (final NullPointerException e) {

                logger.error("sia daemon returns invalid responses: {}", e.getMessage());
                break;

            } catch (final GetWalletInfoTask.WalletException e) {
                System.out.println(String.format("error: %s", e.getMessage()));
                break;
            }

        }

        if (daemon != null) {
            try {
                daemon.close();
                daemon.join();
            } catch (InterruptedException e) {
                logger.error("Interrupted while closing the sia daemon: {}", e.getMessage());
            }
        }

    }

}
