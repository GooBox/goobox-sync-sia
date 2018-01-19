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
import io.goobox.sync.sia.SiaDaemon;
import io.goobox.sync.sia.client.ApiClient;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.WalletApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20012;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse20016;
import io.goobox.sync.sia.client.api.model.InlineResponse2008;
import io.goobox.sync.sia.model.PriceInfo;
import io.goobox.sync.sia.model.WalletInfo;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Wallet command shows wallet information.
 */
public final class Wallet implements Runnable, Callable<Wallet.InfoPair> {

    public static final String CommandName = "wallet";
    public static final String Description = "Show your wallet information";
    private static final Logger logger = LogManager.getLogger();

    public static class InfoPair {
        @NotNull
        private final WalletInfo walletInfo;
        @NotNull
        private final PriceInfo priceInfo;

        public InfoPair(@NotNull final WalletInfo walletInfo, @NotNull final PriceInfo priceInfo) {
            this.walletInfo = walletInfo;
            this.priceInfo = priceInfo;
        }

        @NotNull
        public WalletInfo getWalletInfo() {
            return walletInfo;
        }

        @NotNull
        public PriceInfo getPriceInfo() {
            return priceInfo;
        }
    }

    public class WalletException extends Exception {
        WalletException(final String msg) {
            super(msg);
        }
    }

    @NotNull
    private final Path configPath;
    @NotNull
    private final Config cfg;
    @Nullable
    private SiaDaemon daemon = null;

    public static void main(String[] args) {

        final Options opts = new Options();
        opts.addOption("h", "help", false, "show this help");
        try {

            final CommandLine cmd = new DefaultParser().parse(opts, args);
            if (cmd.hasOption("h")) {
                final HelpFormatter help = new HelpFormatter();
                help.printHelp(String.format("%s %s", App.Name, CommandName), Description, opts, "", true);
                return;
            }

        } catch (ParseException e) {
            logger.error("Failed to parse command line options: {}", e.getMessage());

            final HelpFormatter help = new HelpFormatter();
            help.printHelp(String.format("%s %s", App.Name, CommandName), Description, opts, "", true);
            System.exit(1);
            return;

        }

        // Run this command.
        new Wallet().run();

    }

    public Wallet() {
        configPath = Utils.getDataDir().resolve(App.ConfigFileName);
        this.cfg = CmdUtils.loadConfig(configPath);
    }

    /**
     * Wallet command print wallet and coin spending information.
     * <p>
     * The printed information is:
     * (from /wallet/address)
     * - the wallet address
     * (from /wallet)
     * - confirmed balance (in SC)
     * - confirmed delta (in SC)
     * (from /renter)
     * - allowance
     * - funds (in SC)
     * - hosts
     * - period
     * - renew window
     * - current period
     * - current spending
     * - download
     * - storage
     * - upload
     * - form contract
     * (from /renter/prices)
     * - current prices
     * - download
     * - storage
     * - upload
     * - form contract
     */
    @Override
    public InfoPair call() throws ApiException, WalletException {

        final ApiClient apiClient = CmdUtils.getApiClient();
        final WalletApi walletApi = new WalletApi(apiClient);
        final InlineResponse20013 wallet = walletApi.walletGet();
        if (!wallet.getUnlocked()) {

            try {

                logger.info("Unlocking the wallet");
                walletApi.walletUnlockPost(cfg.getPrimarySeed());

            } catch (final ApiException e) {

                if (e.getCause() instanceof ConnectException) {
                    throw e;
                }

                if (!cfg.getPrimarySeed().isEmpty()) {
                    logger.error("Primary seed is given but the corresponding wallet is not found");
                    throw new WalletException("cannot find the wallet");
                }

                // Create a new wallet.
                logger.info("No wallets are found, initializing a wallet");
                final InlineResponse20016 seed = walletApi.walletInitPost(null, null, false);
                cfg.setPrimarySeed(seed.getPrimaryseed());
                try {
                    cfg.save(configPath);
                } catch (IOException e1) {
                    logger.error("Failed to save the wallet information");
                    throw new WalletException("cannot save the wallet");
                }

                if (!walletApi.walletGet().getUnlocked()) {
                    logger.info("Unlocking the wallet again");
                    walletApi.walletUnlockPost(cfg.getPrimarySeed());
                }

            }

        }

        final RenterApi renter = new RenterApi(apiClient);
        final InlineResponse2008 info = renter.renterGet();
        final WalletInfo walletInfo = new WalletInfo(
                walletApi.walletAddressGet().getAddress(), cfg.getPrimarySeed(), wallet, info);

        final InlineResponse20012 prices = renter.renterPricesGet();
        final PriceInfo priceInfo = new PriceInfo(prices);

        return new InfoPair(walletInfo, priceInfo);

    }

    @Override
    public void run() {

        int retry = 0;
        while (true) {

            try {
                final InfoPair pair = this.call();
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

            } catch (final WalletException e) {
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
