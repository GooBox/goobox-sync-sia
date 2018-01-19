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
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.WalletApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20012;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse20014;
import io.goobox.sync.sia.client.api.model.InlineResponse20016;
import io.goobox.sync.sia.client.api.model.InlineResponse2008;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Financialmetrics;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Settings;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import io.goobox.sync.sia.mocks.SystemMock;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.cli.HelpFormatter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("SpellCheckingInspection")
@RunWith(JMockit.class)
public class WalletTest {

    // Wallet command should print
    // (from /wallet/address)
    // - the wallet address
    // (from /wallet)
    // - confirmed balance (in SC)
    // - confirmed delta (in SC)
    // (from /renter)
    // - allowance
    //   - funds (in SC)
    //   - hosts
    //   - period
    //   - renew window
    //   - current period
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
    private final String address = "01234567890123456789";
    private final double balance = 12345.02;
    private final double income = 10;
    private final double outcome = 15;
    private final double funds = 1234;
    private final int hosts = 30;
    private final long period = 6000;
    private final long renewWindow = 1000;
    private final long currentPeriod = 3000;
    private final double downloadSpending = 1.2345;
    private final double uploadSpending = 0.223;
    private final double storageSpending = 2.3;
    private final double contractSpending = 0.001;
    private final double downloadPrice = 1234.5;
    private final double uploadPrice = 1234.5;
    private final double storagePrice = 12345.6;
    private final double contractPrice = 1.123;
    private final String primarySeed = "sample primary seed";

    private Wallet cmd;
    private ByteArrayOutputStream out;
    private PrintStream oldOut;

    @Before
    public void setUp() throws IOException {
        out = new ByteArrayOutputStream();
        oldOut = System.out;
        System.setOut(new PrintStream(out));

        cmd = new Wallet();
        Deencapsulation.setField(cmd, "configPath", Files.createTempFile(null, null));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() {
        System.setOut(oldOut);
        final Path configPath = Deencapsulation.getField(cmd, "configPath");
        if (configPath.toFile().exists()) {
            configPath.toFile().delete();
        }
    }

    @Test
    public void withInitializedWallet(@Mocked WalletApi wallet, @Mocked RenterApi renter) throws ApiException {

        new Expectations() {{

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setUnlocked(true);
            res1.setConfirmedsiacoinbalance(APIUtils.toHastings(balance).toString());
            res1.setUnconfirmedincomingsiacoins(APIUtils.toHastings(income).toString());
            res1.setUnconfirmedoutgoingsiacoins(APIUtils.toHastings(outcome).toString());
            wallet.walletGet();
            result = res1;

            final InlineResponse20014 res2 = new InlineResponse20014();
            res2.setAddress(address);
            wallet.walletAddressGet();
            result = res2;

            final InlineResponse2008 res3 = new InlineResponse2008();
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(APIUtils.toHastings(funds).toString());
            allowance.setHosts(hosts);
            allowance.setPeriod(period);
            allowance.setRenewwindow(renewWindow);
            settings.setAllowance(allowance);
            res3.setSettings(settings);
            final InlineResponse2008Financialmetrics spending = new InlineResponse2008Financialmetrics();
            spending.setDownloadspending(APIUtils.toHastings(downloadSpending).toString());
            spending.setUploadspending(APIUtils.toHastings(uploadSpending).toString());
            spending.setStoragespending(APIUtils.toHastings(storageSpending).toString());
            spending.setContractspending(APIUtils.toHastings(contractSpending).toString());
            res3.setFinancialmetrics(spending);
            res3.setCurrentperiod(String.valueOf(currentPeriod));
            renter.renterGet();
            result = res3;

            final InlineResponse20012 res4 = new InlineResponse20012();
            res4.setDownloadterabyte(APIUtils.toHastings(downloadPrice).toString());
            res4.setUploadterabyte(APIUtils.toHastings(uploadPrice).toString());
            res4.setStorageterabytemonth(APIUtils.toHastings(storagePrice).toString());
            res4.setFormcontracts(APIUtils.toHastings(contractPrice).toString());
            renter.renterPricesGet();
            result = res4;

        }};

        final Config cfg = new Config();
        cfg.setPrimarySeed(primarySeed);
        Deencapsulation.setField(cmd, "cfg", cfg);

        cmd.run();

        final String outputs = out.toString();
        System.err.println(outputs);
        this.checkOutput(outputs);

    }

    @Test
    public void withLockedWallet(@Mocked WalletApi wallet, @Mocked RenterApi renter) throws ApiException {

        new Expectations() {{

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setUnlocked(false);
            res1.setConfirmedsiacoinbalance(APIUtils.toHastings(balance).toString());
            res1.setUnconfirmedincomingsiacoins(APIUtils.toHastings(income).toString());
            res1.setUnconfirmedoutgoingsiacoins(APIUtils.toHastings(outcome).toString());
            wallet.walletGet();
            result = res1;

            wallet.walletUnlockPost(primarySeed);

            final InlineResponse20014 res2 = new InlineResponse20014();
            res2.setAddress(address);
            wallet.walletAddressGet();
            result = res2;

            final InlineResponse2008 res3 = new InlineResponse2008();
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(APIUtils.toHastings(funds).toString());
            allowance.setHosts(hosts);
            allowance.setPeriod(period);
            allowance.setRenewwindow(renewWindow);
            settings.setAllowance(allowance);
            res3.setSettings(settings);
            final InlineResponse2008Financialmetrics spending = new InlineResponse2008Financialmetrics();
            spending.setDownloadspending(APIUtils.toHastings(downloadSpending).toString());
            spending.setUploadspending(APIUtils.toHastings(uploadSpending).toString());
            spending.setStoragespending(APIUtils.toHastings(storageSpending).toString());
            spending.setContractspending(APIUtils.toHastings(contractSpending).toString());
            res3.setFinancialmetrics(spending);
            res3.setCurrentperiod(String.valueOf(currentPeriod));
            renter.renterGet();
            result = res3;

            final InlineResponse20012 res4 = new InlineResponse20012();
            res4.setDownloadterabyte(APIUtils.toHastings(downloadPrice).toString());
            res4.setUploadterabyte(APIUtils.toHastings(uploadPrice).toString());
            res4.setStorageterabytemonth(APIUtils.toHastings(storagePrice).toString());
            res4.setFormcontracts(APIUtils.toHastings(contractPrice).toString());
            renter.renterPricesGet();
            result = res4;

        }};

        final Config cfg = new Config();
        cfg.setPrimarySeed(primarySeed);
        Deencapsulation.setField(cmd, "cfg", cfg);
        cmd.run();

        final String outputs = out.toString();
        System.err.println(outputs);
        this.checkOutput(outputs);

    }

    @Test
    public void withoutInitializedWallet(@Mocked WalletApi wallet, @Mocked RenterApi renter) throws ApiException {

        Deencapsulation.setField(cmd, "cfg", new Config());
        new Expectations() {{

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setUnlocked(false);
            res1.setConfirmedsiacoinbalance(APIUtils.toHastings(balance).toString());
            res1.setUnconfirmedincomingsiacoins(APIUtils.toHastings(income).toString());
            res1.setUnconfirmedoutgoingsiacoins(APIUtils.toHastings(outcome).toString());
            wallet.walletGet();
            result = res1;

            wallet.walletUnlockPost("");
            result = new ApiException();

            final InlineResponse20016 seed = new InlineResponse20016();
            seed.setPrimaryseed(primarySeed);
            wallet.walletInitPost(null, null, false);
            result = seed;

            wallet.walletUnlockPost(primarySeed);

            final InlineResponse20014 res2 = new InlineResponse20014();
            res2.setAddress(address);
            wallet.walletAddressGet();
            result = res2;

            final InlineResponse2008 res3 = new InlineResponse2008();
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(APIUtils.toHastings(funds).toString());
            allowance.setHosts(hosts);
            allowance.setPeriod(period);
            allowance.setRenewwindow(renewWindow);
            settings.setAllowance(allowance);
            res3.setSettings(settings);
            final InlineResponse2008Financialmetrics spending = new InlineResponse2008Financialmetrics();
            spending.setDownloadspending(APIUtils.toHastings(downloadSpending).toString());
            spending.setUploadspending(APIUtils.toHastings(uploadSpending).toString());
            spending.setStoragespending(APIUtils.toHastings(storageSpending).toString());
            spending.setContractspending(APIUtils.toHastings(contractSpending).toString());
            res3.setFinancialmetrics(spending);
            res3.setCurrentperiod(String.valueOf(currentPeriod));
            renter.renterGet();
            result = res3;

            final InlineResponse20012 res4 = new InlineResponse20012();
            res4.setDownloadterabyte(APIUtils.toHastings(downloadPrice).toString());
            res4.setUploadterabyte(APIUtils.toHastings(uploadPrice).toString());
            res4.setStorageterabytemonth(APIUtils.toHastings(storagePrice).toString());
            res4.setFormcontracts(APIUtils.toHastings(contractPrice).toString());
            renter.renterPricesGet();
            result = res4;

        }};

        cmd.run();

        final String outputs = out.toString();
        System.err.println(outputs);
        this.checkOutput(outputs);

    }

    @Test
    public void withoutRunningSiaDaemon(
            @Mocked WalletApi wallet, @Mocked RenterApi renter, @Mocked SiaDaemon daemon) throws ApiException {

        final Config cfg = new Config();
        cfg.setPrimarySeed(primarySeed);
        Deencapsulation.setField(cmd, "cfg", cfg);

        new Expectations(cmd) {{

            // Starting sia daemon.
            new SiaDaemon(Utils.getDataDir().resolve("sia"));
            result = daemon;
            daemon.start();

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setUnlocked(true);
            res1.setConfirmedsiacoinbalance(APIUtils.toHastings(balance).toString());
            res1.setUnconfirmedincomingsiacoins(APIUtils.toHastings(income).toString());
            res1.setUnconfirmedoutgoingsiacoins(APIUtils.toHastings(outcome).toString());
            wallet.walletGet();
            // At the first attempt, ApiException will be thrown because no sia daemon running.
            result = new ApiException(new ConnectException());
            result = res1;

            final InlineResponse20014 res2 = new InlineResponse20014();
            res2.setAddress(address);
            wallet.walletAddressGet();
            result = res2;

            final InlineResponse2008 res3 = new InlineResponse2008();
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(APIUtils.toHastings(funds).toString());
            allowance.setHosts(hosts);
            allowance.setPeriod(period);
            allowance.setRenewwindow(renewWindow);
            settings.setAllowance(allowance);
            res3.setSettings(settings);
            final InlineResponse2008Financialmetrics spending = new InlineResponse2008Financialmetrics();
            spending.setDownloadspending(APIUtils.toHastings(downloadSpending).toString());
            spending.setUploadspending(APIUtils.toHastings(uploadSpending).toString());
            spending.setStoragespending(APIUtils.toHastings(storageSpending).toString());
            spending.setContractspending(APIUtils.toHastings(contractSpending).toString());
            res3.setFinancialmetrics(spending);
            res3.setCurrentperiod(String.valueOf(currentPeriod));
            renter.renterGet();
            result = res3;

            final InlineResponse20012 res4 = new InlineResponse20012();
            res4.setDownloadterabyte(APIUtils.toHastings(downloadPrice).toString());
            res4.setUploadterabyte(APIUtils.toHastings(uploadPrice).toString());
            res4.setStorageterabytemonth(APIUtils.toHastings(storagePrice).toString());
            res4.setFormcontracts(APIUtils.toHastings(contractPrice).toString());
            renter.renterPricesGet();
            result = res4;

        }};

        cmd.run();

        final String outputs = out.toString();
        System.err.println(outputs);
        this.checkOutput(outputs);

    }

    private void checkOutput(final String outputs) {
        assertTrue(
                String.format("wallet address: %s", address),
                outputs.contains(String.format("wallet address: %s", address)));
        assertTrue(
                String.format("primary seed: %s", primarySeed),
                outputs.contains(String.format("primary seed: %s", primarySeed)));
        assertTrue(
                String.format("balance: %.4f SC", balance),
                outputs.contains(String.format("balance: %.4f SC", balance)));
        assertTrue(
                String.format("unconfirmed delta: %.4f SC", income - outcome),
                outputs.contains(String.format("unconfirmed delta: %.4f SC", income - outcome)));
        assertTrue(
                String.format("funds: %.4f SC", funds),
                outputs.contains(String.format("funds: %.4f SC", funds)));
        assertTrue(
                String.format("hosts: %d", hosts),
                outputs.contains(String.format("hosts: %d", hosts)));
        assertTrue(
                String.format("period: %d", period),
                outputs.contains(String.format("period: %d", period)));
        assertTrue(
                String.format("renew window: %d", renewWindow),
                outputs.contains(String.format("renew window: %d", renewWindow)));
        assertTrue(
                String.format("start height: %d", currentPeriod),
                outputs.contains(String.format("start height: %d", currentPeriod)));
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
                String.format("contract: %.4f SC", contractSpending),
                outputs.contains(String.format("contract: %.4f SC", contractSpending)));
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
                String.format("contract: %.4f SC", contractPrice),
                outputs.contains(String.format("contract: %.4f SC", contractPrice)));
    }

    /**
     * If walletApi.walletUnlockPost throws an exception, which means there are no wallet,
     * but the config has a primary seed, output error message "cannot find the wallet".
     */
    @Test
    public void cannotFindWallet(@Mocked WalletApi wallet) throws ApiException {

        final Config cfg = new Config();
        cfg.setPrimarySeed(primarySeed);
        Deencapsulation.setField(cmd, "cfg", cfg);

        new Expectations() {{
            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setUnlocked(false);
            wallet.walletGet();
            result = res1;

            wallet.walletUnlockPost(primarySeed);
            result = new ApiException();
        }};

        cmd.run();

        final String outputs = out.toString();
        System.err.println(outputs);
        assertTrue(outputs.contains("error: cannot find the wallet"));

    }

    /**
     * If cfg.save throws IOException, output error message "cannot save the wallet".
     */
    @Test
    public void cannotSaveWallet(@Mocked WalletApi wallet) throws ApiException, IOException {

        final Config cfg = new Config();
        Deencapsulation.setField(cmd, "cfg", cfg);
        new Expectations(cfg) {{
            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setUnlocked(false);
            wallet.walletGet();
            result = res1;

            wallet.walletUnlockPost("");
            result = new ApiException();

            final InlineResponse20016 seed = new InlineResponse20016();
            seed.setPrimaryseed(primarySeed);
            wallet.walletInitPost(null, null, false);
            result = seed;

            cfg.save(Deencapsulation.getField(cmd, "configPath"));
            result = new IOException();
        }};

        cmd.run();

        final String outputs = out.toString();
        System.err.println(outputs);
        assertTrue(outputs.contains("error: cannot save the wallet"));

    }

    @Test
    public void forceInitializeWallet(@Mocked WalletApi wallet, @Mocked RenterApi renter, @Mocked CmdUtils utils) throws ApiException {

        new Expectations() {{
            CmdUtils.loadConfig(withAny(Paths.get("")));
            result = new Config();

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setUnlocked(false);
            res1.setConfirmedsiacoinbalance(APIUtils.toHastings(balance).toString());
            res1.setUnconfirmedincomingsiacoins(APIUtils.toHastings(income).toString());
            res1.setUnconfirmedoutgoingsiacoins(APIUtils.toHastings(outcome).toString());
            wallet.walletGet();
            result = res1;

            wallet.walletUnlockPost("");
            result = new ApiException();

            final InlineResponse20016 seed = new InlineResponse20016();
            seed.setPrimaryseed(primarySeed);
            wallet.walletInitPost(null, null, true);
            result = seed;

            wallet.walletUnlockPost(primarySeed);

            final InlineResponse20014 res2 = new InlineResponse20014();
            res2.setAddress(address);
            wallet.walletAddressGet();
            result = res2;

            final InlineResponse2008 res3 = new InlineResponse2008();
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(APIUtils.toHastings(funds).toString());
            allowance.setHosts(hosts);
            allowance.setPeriod(period);
            allowance.setRenewwindow(renewWindow);
            settings.setAllowance(allowance);
            res3.setSettings(settings);
            final InlineResponse2008Financialmetrics spending = new InlineResponse2008Financialmetrics();
            spending.setDownloadspending(APIUtils.toHastings(downloadSpending).toString());
            spending.setUploadspending(APIUtils.toHastings(uploadSpending).toString());
            spending.setStoragespending(APIUtils.toHastings(storageSpending).toString());
            spending.setContractspending(APIUtils.toHastings(contractSpending).toString());
            res3.setFinancialmetrics(spending);
            res3.setCurrentperiod(String.valueOf(currentPeriod));
            renter.renterGet();
            result = res3;

            final InlineResponse20012 res4 = new InlineResponse20012();
            res4.setDownloadterabyte(APIUtils.toHastings(downloadPrice).toString());
            res4.setUploadterabyte(APIUtils.toHastings(uploadPrice).toString());
            res4.setStorageterabytemonth(APIUtils.toHastings(storagePrice).toString());
            res4.setFormcontracts(APIUtils.toHastings(contractPrice).toString());
            renter.renterPricesGet();
            result = res4;

        }};

        Wallet.main(new String[]{"--force"});

        final String outputs = out.toString();
        System.err.println(outputs);
        this.checkOutput(outputs);

    }

    @Test
    public void helpOption(@Mocked HelpFormatter formatter) {

        new Expectations() {{
            formatter.printHelp(
                    String.format("%s %s", App.Name, Wallet.CommandName),
                    Wallet.Description, withNotNull(), "", true);
        }};
        Wallet.main(new String[]{"-h"});

    }

    @Test
    public void longHelpOption(@Mocked HelpFormatter formatter) {

        new Expectations() {{
            formatter.printHelp(
                    String.format("%s %s", App.Name, Wallet.CommandName),
                    Wallet.Description, withNotNull(), "", true);
        }};
        Wallet.main(new String[]{"--help"});

    }

    @Test
    public void invalidOption(@Mocked HelpFormatter formatter) {

        new SystemMock();
        new Expectations() {{
            formatter.printHelp(
                    String.format("%s %s", App.Name, Wallet.CommandName),
                    Wallet.Description, withNotNull(), "", true);
        }};
        Wallet.main(new String[]{"-something"});
        assertEquals(1, SystemMock.statusCode);

    }

}