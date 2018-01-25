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
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import io.goobox.sync.sia.model.AllowanceInfo;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ConnectException;
import java.util.concurrent.Callable;

/**
 * Creates allowance.
 */
public final class CreateAllowance implements Runnable, Callable<AllowanceInfo> {

    public static final String CommandName = "create-allowance";
    public static final String Description = "Create allowance";

    static final int DefaultPeriod = 4320;

    private static final Logger logger = LoggerFactory.getLogger(CreateAllowance.class);

    @NotNull
    private final Config cfg;
    @Nullable
    private BigDecimal fund;
    @Nullable
    private SiaDaemon daemon = null;

    public static void main(final String[] args) {

        final Options opts = new Options();
        opts.addOption("h", "help", false, "show this help");
        opts.addOption(null, "fund", true, "hastings to be allocated if not given allocate current balance");

        BigDecimal fund = null;
        try {

            final CommandLine cmd = new DefaultParser().parse(opts, args);
            if (cmd.hasOption("h")) {
                final HelpFormatter help = new HelpFormatter();
                help.printHelp(
                        String.format("%s %s", App.Name, CreateAllowance.CommandName),
                        Description, opts, "", true);
                return;
            }

            if (cmd.hasOption("fund")) {
                fund = new BigDecimal(cmd.getOptionValue("fund"));
            }


        } catch (ParseException | NumberFormatException e) {
            logger.error("Failed to parse command line options: {}", e.getMessage());

            final HelpFormatter help = new HelpFormatter();
            help.printHelp(
                    String.format("%s %s", App.Name, CreateAllowance.CommandName),
                    Description, opts, "", true);
            System.exit(1);
            return;

        }

        new CreateAllowance(fund).run();

    }

    /**
     * Create a create allowance task.
     *
     * @param fund to be allocated, if null, all current balance will be allocated.
     */
    @SuppressWarnings("WeakerAccess")
    public CreateAllowance(@Nullable final BigDecimal fund) {
        this.fund = fund;
        this.cfg = CmdUtils.loadConfig(Utils.getDataDir().resolve(CmdUtils.ConfigFileName));
    }

    @Override
    public AllowanceInfo call() throws ApiException {

        final ApiClient apiClient = APIUtils.getApiClient();
        final WalletApi wallet = new WalletApi(apiClient);
        final InlineResponse20013 walletInfo = wallet.walletGet();

        // If the wallet is locked, unlock it first.
        if (!walletInfo.getUnlocked()) {
            logger.info("Unlocking the wallet");
            wallet.walletUnlockPost(cfg.getPrimarySeed());
        }

        // If fund is null, get current balance.
        final RenterApi renter = new RenterApi(apiClient);
        if (this.fund == null) {
            // Allocating the current balance.
            this.fund = new BigDecimal(walletInfo.getConfirmedsiacoinbalance());
        }

        // Allocate new fund.
        logger.info("Allocating {} hastings", this.fund);
        renter.renterPost(this.fund.setScale(0, RoundingMode.DOWN).toString(), null, DefaultPeriod, null);

        final InlineResponse2008SettingsAllowance allowance = renter.renterGet().getSettings().getAllowance();
        return new AllowanceInfo(allowance);

    }

    @Override
    public void run() {

        int retry = 0;
        while (true) {

            try {

                System.out.println(this.call().toString());
                break;

            } catch (final ApiException e) {

                if (retry >= App.MaxRetry) {
                    logger.error("Failed to communicate with a sia daemon: {}", APIUtils.getErrorMessage(e));
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
                    logger.warn("Failed to allocate funds: {}", APIUtils.getErrorMessage(e));
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

            }

        }

        if (daemon != null) {
            try {
                daemon.close();
                daemon.join();
            } catch (final InterruptedException e) {
                logger.error("Interrupted while closing the sia daemon: {}", e.getMessage());
            }
        }

    }

}
