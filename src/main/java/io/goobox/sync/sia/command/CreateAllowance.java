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

package io.goobox.sync.sia.command;

import io.goobox.sync.sia.APIUtils;
import io.goobox.sync.sia.client.ApiClient;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.WalletApi;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;

/**
 * Creates allowance.
 */
public class CreateAllowance implements Runnable {

    public static final String CommandName = "create-allowance";
    private static final Logger logger = LogManager.getLogger();

    public static void main(final String[] args) {

        final Options opts = new Options();
        opts.addOption("h", "help", false, "show this help");
        opts.addOption(null, "fund", true, "hastings to be allocated if not given allocate current balance");

        BigDecimal fund = null;
        try {

            final CommandLine cmd = new DefaultParser().parse(opts, args);
            if (cmd.hasOption("h")) {
                System.out.println("here");
                final HelpFormatter help = new HelpFormatter();
                help.printHelp(String.format("goobox-sync-sia %s", CommandName), opts, true);
                return;
            }

            if (cmd.hasOption("fund")) {
                fund = new BigDecimal(cmd.getOptionValue("fund"));
            }


        } catch (ParseException | NumberFormatException e) {
            logger.error("Failed to parse command line options: {}", e.getMessage());

            final HelpFormatter help = new HelpFormatter();
            help.printHelp(String.format("goobox-sync-sia %s", CommandName), opts, true);
            System.exit(1);
            return;

        }

        new CreateAllowance(fund).run();

    }

    private BigDecimal fund;

    public CreateAllowance(final BigDecimal fund) {
        this.fund = fund;
    }

    @Override
    public void run() {

        final ApiClient apiClient = Utils.getApiClient();

        try {

            // If fund is null, get current balance.
            if (this.fund == null) {
                final WalletApi wallet = new WalletApi(apiClient);
                this.fund = new BigDecimal(wallet.walletGet().getConfirmedsiacoinbalance());
            }

            // Get current fund and compute updated fund by adding `fund` value.
            final RenterApi renter = new RenterApi(apiClient);
            final BigDecimal currentFund = new BigDecimal(renter.renterGet().getSettings().getAllowance().getFunds());

            // Allocate new fund.
            final BigDecimal newFund = currentFund.add(this.fund).setScale(0, BigDecimal.ROUND_DOWN);
            renter.renterPost(newFund.toString(), null, null, null);

        } catch (ApiException e) {
            logger.error("Failed to access sia daemon: {}", APIUtils.getErrorMessage(e));
        }

    }

}
