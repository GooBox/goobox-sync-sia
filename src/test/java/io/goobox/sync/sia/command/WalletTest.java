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

import io.goobox.sync.sia.APIUtils;
import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.SiaDaemon;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.model.InlineResponse20012;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse2008;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Financialmetrics;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Settings;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import io.goobox.sync.sia.mocks.SystemMock;
import io.goobox.sync.sia.model.PriceInfo;
import io.goobox.sync.sia.model.WalletInfo;
import io.goobox.sync.sia.task.GetWalletInfoTask;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.cli.HelpFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.ConnectException;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("SpellCheckingInspection")
@RunWith(JMockit.class)
public class WalletTest {

    private Config cfg;
    private Wallet cmd;
    private WalletInfo walletInfo;
    private PriceInfo priceInfo;

    @Before
    public void setUp() {
        cfg = new Config();
        cmd = new Wallet();
        Deencapsulation.setField(cmd, "cfg", cfg);

        final String address = "01234567890123456789";
        final double balance = 12345.02;
        final double income = 10;
        final double outcome = 15;
        final double funds = 1234;
        final int hosts = 30;
        final long period = 6000;
        final long renewWindow = 1000;
        final long currentPeriod = 3000;
        final double downloadSpending = 1.2345;
        final double uploadSpending = 0.223;
        final double storageSpending = 2.3;
        final double contractSpending = 0.001;
        final double downloadPrice = 1234.5;
        final double uploadPrice = 1234.5;
        final double storagePrice = 12345.6;
        final double contractPrice = 1.123;
        final String primarySeed = "sample primary seed";

        final InlineResponse20013 walletGetResponse = new InlineResponse20013();
        walletGetResponse.setUnlocked(false);
        walletGetResponse.setConfirmedsiacoinbalance(APIUtils.toHasting(balance).toString());
        walletGetResponse.setUnconfirmedincomingsiacoins(APIUtils.toHasting(income).toString());
        walletGetResponse.setUnconfirmedoutgoingsiacoins(APIUtils.toHasting(outcome).toString());

        final InlineResponse2008 renterGetResponse = new InlineResponse2008();
        final InlineResponse2008Settings settings = new InlineResponse2008Settings();
        final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
        allowance.setFunds(APIUtils.toHasting(funds).toString());
        allowance.setHosts(hosts);
        allowance.setPeriod(period);
        allowance.setRenewwindow(renewWindow);
        settings.setAllowance(allowance);
        renterGetResponse.setSettings(settings);
        final InlineResponse2008Financialmetrics spending = new InlineResponse2008Financialmetrics();
        spending.setDownloadspending(APIUtils.toHasting(downloadSpending).toString());
        spending.setUploadspending(APIUtils.toHasting(uploadSpending).toString());
        spending.setStoragespending(APIUtils.toHasting(storageSpending).toString());
        spending.setContractspending(APIUtils.toHasting(contractSpending).toString());
        renterGetResponse.setFinancialmetrics(spending);
        renterGetResponse.setCurrentperiod(String.valueOf(currentPeriod));

        final InlineResponse20012 renterPriceGetResponse = new InlineResponse20012();
        renterPriceGetResponse.setDownloadterabyte(APIUtils.toHasting(downloadPrice).toString());
        renterPriceGetResponse.setUploadterabyte(APIUtils.toHasting(uploadPrice).toString());
        renterPriceGetResponse.setStorageterabytemonth(APIUtils.toHasting(storagePrice).toString());
        renterPriceGetResponse.setFormcontracts(APIUtils.toHasting(contractPrice).toString());

        walletInfo = new WalletInfo(address, primarySeed, walletGetResponse, renterGetResponse);
        priceInfo = new PriceInfo(renterPriceGetResponse);
    }

    /**
     * Wallet runs GetWalletInfoTask and prints retrieved information.
     */
    @Test
    public void outputWalletInformation(@Mocked final GetWalletInfoTask task) throws GetWalletInfoTask.WalletException, ApiException {

        new Expectations(System.out) {{
            task.call();
            result = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            System.out.println(walletInfo.toString());
            System.out.println(priceInfo.toString());
        }};
        cmd.run();

    }

    /**
     * This test simulates the senario that the SiaDaemon isn't running and GetWalletInfoTask throws an ApiException
     * with a ConnectException. Then, Wallet starts a SiaDaemon and retries GetWalletInfoTask.
     */
    @Test
    public void withoutRunningSiaDaemon(@Mocked GetWalletInfoTask task, @Mocked SiaDaemon daemon) throws ApiException, GetWalletInfoTask.WalletException {

        new Expectations(System.out) {{
            task.call();
            // At the first time, GetWalletInfoTask throws an ApiException because no SiaDaemon running.
            result = new ApiException(new ConnectException());
            // After starting a SiaDaemon, returns the correct result.
            result = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);

            // Starting sia daemon.
            new SiaDaemon(cfg.getDataDir().resolve("sia"));
            result = daemon;
            daemon.start();

            System.out.println(walletInfo.toString());
            System.out.println(priceInfo.toString());
        }};
        cmd.run();

    }


    /**
     * If GetWalletInfoTask throws an exception, output the error message.
     */
    @Test
    public void walletException(@Mocked GetWalletInfoTask task) throws ApiException, GetWalletInfoTask.WalletException {

        final String error = "error message";
        new Expectations(System.out) {{
            task.call();
            result = new GetWalletInfoTask.WalletException(error);
            System.out.println(String.format("error: %s", error));
        }};
        cmd.run();

    }

    @Test
    public void forceOption(@SuppressWarnings("unused") @Mocked GetWalletInfoTask task) throws GetWalletInfoTask.WalletException, ApiException {

        new Expectations(cmd) {{
            cmd = new Wallet(true);
            GetWalletInfoTask task = new GetWalletInfoTask((Context) any, true);
            task.call();
            result = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
        }};
        Wallet.main(new String[]{"--force"});
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