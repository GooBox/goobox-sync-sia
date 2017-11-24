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

import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.WalletApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20012;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse20014;
import io.goobox.sync.sia.client.api.model.InlineResponse2008;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Financialmetrics;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.cli.HelpFormatter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;

import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class WalletTest {


    private ByteArrayOutputStream out;
    private PrintStream oldOut;

    @Before
    public void setUp() {
        out = new ByteArrayOutputStream();
        oldOut = System.out;
        System.setOut(new PrintStream(out));
    }

    @After
    public void tearDown() {
        System.setOut(oldOut);
    }

    @Test
    public void test(@Mocked WalletApi wallet, @Mocked RenterApi renter) throws ApiException {

        // Wallet command should print
        // (from /wallet/address)
        // - the wallet address
        // (from /wallet)
        // - confirmed balance (in SC)
        // - confirmed delta (in SC)
        // (from /renter)
        // - current spending
        //   - download
        //   - storage
        //   - upload
        //   - form contract
        // (from /renter/prices)
        // - current prices
        //   - download
        //   - storage
        //   - upload
        //   - form contract

        final String address = "01234567890123456789";
        final double balance = 12345.02;
        final double income = 10;
        final double outcome = 15;
        final double downloadSpending = 1.2345;
        final double uploadSpending = 0.223;
        final double storageSpending = 2.3;
        final double contractSpending = 0.001;
        final double downloadPrice = 1234.5;
        final double uploadPrice = 1234.5;
        final double storagePrice = 12345.6;
        final double contractPrice = 1.123;

        new Expectations() {{

            final InlineResponse20014 res1 = new InlineResponse20014();
            res1.setAddress(address);
            wallet.walletAddressGet();
            result = res1;

            final InlineResponse20013 res2 = new InlineResponse20013();
            res2.setConfirmedsiacoinbalance(new BigDecimal(balance).multiply(Wallet.Hasting).toString());
            res2.setUnconfirmedincomingsiacoins(new BigDecimal(income).multiply(Wallet.Hasting).toString());
            res2.setUnconfirmedoutgoingsiacoins(new BigDecimal(outcome).multiply(Wallet.Hasting).toString());
            wallet.walletGet();
            result = res2;

            final InlineResponse2008 res3 = new InlineResponse2008();
            final InlineResponse2008Financialmetrics spending = new InlineResponse2008Financialmetrics();
            spending.setDownloadspending(new BigDecimal(downloadSpending).multiply(Wallet.Hasting).toString());
            spending.setUploadspending(new BigDecimal(uploadSpending).multiply(Wallet.Hasting).toString());
            spending.setStoragespending(new BigDecimal(storageSpending).multiply(Wallet.Hasting).toString());
            spending.setContractspending(new BigDecimal(contractSpending).multiply(Wallet.Hasting).toString());
            res3.setFinancialmetrics(spending);
            renter.renterGet();
            result = res3;

            final InlineResponse20012 res4 = new InlineResponse20012();
            res4.setDownloadterabyte(new BigDecimal(downloadPrice).multiply(Wallet.Hasting).toString());
            res4.setUploadterabyte(new BigDecimal(uploadPrice).multiply(Wallet.Hasting).toString());
            res4.setStorageterabytemonth(new BigDecimal(storagePrice).multiply(Wallet.Hasting).toString());
            res4.setFormcontracts(new BigDecimal(contractPrice).multiply(Wallet.Hasting).toString());
            renter.renterPricesGet();
            result = res4;

        }};

        Wallet.main(new String[]{});

        final String outputs = out.toString();
        System.err.println(outputs);
        assertTrue(
                String.format("wallet address: %s", address),
                outputs.contains(String.format("wallet address: %s", address)));
        assertTrue(
                String.format("balance: %.4f SC", balance),
                outputs.contains(String.format("balance: %.4f SC", balance)));
        assertTrue(
                String.format("unconfirmed delta: %.4f SC", income - outcome),
                outputs.contains(String.format("unconfirmed delta: %.4f SC", income - outcome)));
        assertTrue(
                String.format("download: %.4f SC", downloadSpending),
                outputs.contains(String.format("download: %.4f SC", downloadSpending)));
        assertTrue(
                String.format("upload: %.4f SC", uploadSpending),
                outputs.contains(String.format("upload: %.4f SC", uploadSpending)));
        assertTrue(
                String.format("storage: %.4f SC", storageSpending),
                outputs.contains(String.format("storage: %.4f SC", storageSpending)));
        assertTrue(
                String.format("fee: %.4f SC", contractSpending),
                outputs.contains(String.format("fee: %.4f SC", contractSpending)));
        assertTrue(
                String.format("download: %.4f SC/TB", downloadPrice),
                outputs.contains(String.format("download: %.4f SC/TB", downloadPrice)));
        assertTrue(
                String.format("upload: %.4f SC/TB", uploadPrice),
                outputs.contains(String.format("upload: %.4f SC/TB", uploadPrice)));
        assertTrue(
                String.format("storage: %.4f SC/TB", storagePrice),
                outputs.contains(String.format("storage: %.4f SC/TB*Month", storagePrice)));
        assertTrue(
                String.format("fee: %.4f SC", contractPrice),
                outputs.contains(String.format("fee: %.4f SC", contractPrice)));

    }

    @Test
    public void testHelpOption(@Mocked HelpFormatter formatter) {

        new Expectations() {{
            formatter.printHelp("goobox-sync-sia wallet", withNotNull(), true);
        }};
        Wallet.main(new String[]{"-h"});

    }

    @Test
    public void testLongHelpOption(@Mocked HelpFormatter formatter) {

        new Expectations() {{
            formatter.printHelp("goobox-sync-sia wallet", withNotNull(), true);
        }};
        Wallet.main(new String[]{"--help"});

    }

    @Test
    public void testInvalidOption(@Mocked HelpFormatter formatter, @Mocked System system) {

        new Expectations() {{
            formatter.printHelp("goobox-sync-sia wallet", withNotNull(), true);
            system.exit(1);
        }};
        Wallet.main(new String[]{"-something"});

    }


}