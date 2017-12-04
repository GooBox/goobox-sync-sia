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
import io.goobox.sync.sia.client.api.model.InlineResponse20012;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Financialmetrics;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;

/**
 * Wallet command shows wallet information.
 */
public class Wallet implements Runnable {

    public static final String CommandName = "wallet";
    public static final String Description = "Show your wallet information";
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {

        final Options opts = new Options();
        opts.addOption("h", "help", false, "show this help");
        try {

            final CommandLine cmd = new DefaultParser().parse(opts, args);
            if (cmd.hasOption("h")) {
                final HelpFormatter help = new HelpFormatter();
                help.printHelp(String.format("goobox-sync-sia %s", CommandName), Description, opts, "", true);
                return;
            }

        } catch (ParseException e) {
            logger.error("Failed to parse command line options: {}", e.getMessage());

            final HelpFormatter help = new HelpFormatter();
            help.printHelp(String.format("goobox-sync-sia %s", CommandName), Description, opts, "", true);
            System.exit(1);
            return;

        }

        // Run this command.
        new Wallet().run();

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
    public void run() {

        final ApiClient apiClient = CmdUtils.getApiClient();

        try {

            final WalletApi wallet = new WalletApi(apiClient);
            System.out.println(String.format("wallet address: %s", wallet.walletAddressGet().getAddress()));

            final InlineResponse20013 balances = wallet.walletGet();
            System.out.println(String.format(
                    "balance: %s SC",
                    new BigDecimal(balances.getConfirmedsiacoinbalance()).
                            divide(CmdUtils.Hasting, 4, BigDecimal.ROUND_HALF_UP)));
            System.out.println(String.format(
                    "unconfirmed delta: %s SC",
                    new BigDecimal(balances.getUnconfirmedincomingsiacoins()).
                            subtract(new BigDecimal(balances.getUnconfirmedoutgoingsiacoins())).
                            divide(CmdUtils.Hasting, 4, BigDecimal.ROUND_HALF_UP)));

            final RenterApi renter = new RenterApi(apiClient);
            final InlineResponse2008Financialmetrics spendings = renter.renterGet().getFinancialmetrics();
            System.out.println("current spending:");
            System.out.println(String.format(
                    "  download: %s SC",
                    new BigDecimal(spendings.getDownloadspending()).
                            divide(CmdUtils.Hasting, 4, BigDecimal.ROUND_HALF_UP)));
            System.out.println(String.format(
                    "  upload: %s SC",
                    new BigDecimal(spendings.getUploadspending()).
                            divide(CmdUtils.Hasting, 4, BigDecimal.ROUND_HALF_UP)));
            System.out.println(String.format(
                    "  storage: %s SC",
                    new BigDecimal(spendings.getStoragespending()).
                            divide(CmdUtils.Hasting, 4, BigDecimal.ROUND_HALF_UP)));
            System.out.println(String.format(
                    "  fee: %s SC",
                    new BigDecimal(spendings.getContractspending()).
                            divide(CmdUtils.Hasting, 4, BigDecimal.ROUND_HALF_UP)));

            final InlineResponse20012 prices = renter.renterPricesGet();
            System.out.println("current prices:");
            System.out.println(String.format(
                    "  download: %s SC/TB",
                    new BigDecimal(prices.getDownloadterabyte()).
                            divide(CmdUtils.Hasting, 4, BigDecimal.ROUND_HALF_UP)));
            System.out.println(String.format(
                    "  upload: %s SC/TB",
                    new BigDecimal(prices.getUploadterabyte()).
                            divide(CmdUtils.Hasting, 4, BigDecimal.ROUND_HALF_UP)));
            System.out.println(String.format(
                    "  storage: %s SC/TB*Month",
                    new BigDecimal(prices.getStorageterabytemonth()).
                            divide(CmdUtils.Hasting, 4, BigDecimal.ROUND_HALF_UP)));
            System.out.println(String.format(
                    "  fee: %s SC",
                    new BigDecimal(prices.getFormcontracts()).
                            divide(CmdUtils.Hasting, 4, BigDecimal.ROUND_HALF_UP)));

        } catch (ApiException e) {
            logger.error("Failed to access sia daemon: {}", APIUtils.getErrorMessage(e));
        } catch (NullPointerException e) {
            logger.error("sia daemon returns invalid responses: {}", e.getMessage());
        }

    }

}
