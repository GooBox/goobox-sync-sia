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

package io.goobox.sync.sia.task;

import com.google.gson.Gson;
import io.goobox.sync.sia.APIUtils;
import io.goobox.sync.sia.App;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.model.InlineResponse20012;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse2008;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Financialmetrics;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Settings;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import io.goobox.sync.sia.command.CreateAllowance;
import io.goobox.sync.sia.command.Wallet;
import io.goobox.sync.sia.model.AllowanceInfo;
import io.goobox.sync.sia.model.PriceInfo;
import io.goobox.sync.sia.model.WalletInfo;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
@RunWith(JMockit.class)
public class NotifyFundInfoTaskTest {

    private final int hosts = 30;
    private final long period = 6000;
    private final long renewWindow = 1000;

    private PriceInfo priceInfo;
    private WalletInfo walletInfo;

    @Mocked
    private Wallet walletCmd;
    @Mocked
    private CreateAllowance createAllowanceCmd;

    private ByteArrayOutputStream out;
    private PrintStream oldOut;

    private Gson gson = new Gson();

    @SuppressWarnings("SpellCheckingInspection")
    @Before
    public void setUp() {
        this.out = new ByteArrayOutputStream();
        this.oldOut = System.out;
        System.setOut(new PrintStream(out));

        final String address = "01234567890123456789";
        final String primarySeed = "sample primary seed";
        final double balance = 12345.02;
        final double income = 10;
        final double outcome = 15;
        final long currentPeriod = 3000;
        final double downloadSpending = 1.2345;
        final double uploadSpending = 0.223;
        final double storageSpending = 2.3;
        final double contractSpending = 0.001;
        final double downloadPrice = 1234.5;
        final double uploadPrice = 1234.5;
        final double storagePrice = 12345.6;
        final double contractPrice = 1.123;

        final InlineResponse20013 wallet = new InlineResponse20013();
        wallet.setConfirmedsiacoinbalance(APIUtils.toHastings(balance).toString());
        wallet.setUnconfirmedincomingsiacoins(APIUtils.toHastings(income).toString());
        wallet.setUnconfirmedoutgoingsiacoins(APIUtils.toHastings(outcome).toString());

        final InlineResponse2008 info = new InlineResponse2008();
        final InlineResponse2008Settings settings = new InlineResponse2008Settings();
        final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
        allowance.setFunds(APIUtils.toHastings(balance).toString());
        allowance.setHosts(hosts);
        allowance.setPeriod(period);
        allowance.setRenewwindow(renewWindow);
        settings.setAllowance(allowance);
        info.setSettings(settings);
        final InlineResponse2008Financialmetrics spending = new InlineResponse2008Financialmetrics();
        spending.setDownloadspending(APIUtils.toHastings(downloadSpending).toString());
        spending.setUploadspending(APIUtils.toHastings(uploadSpending).toString());
        spending.setStoragespending(APIUtils.toHastings(storageSpending).toString());
        spending.setContractspending(APIUtils.toHastings(contractSpending).toString());
        info.setFinancialmetrics(spending);
        info.setCurrentperiod(String.valueOf(currentPeriod));

        this.walletInfo = new WalletInfo(address, primarySeed, wallet, info);

        final InlineResponse20012 prices = new InlineResponse20012();
        prices.setDownloadterabyte(APIUtils.toHastings(downloadPrice).toString());
        prices.setUploadterabyte(APIUtils.toHastings(uploadPrice).toString());
        prices.setStorageterabytemonth(APIUtils.toHastings(storagePrice).toString());
        prices.setFormcontracts(APIUtils.toHastings(contractPrice).toString());
        this.priceInfo = new PriceInfo(prices);
    }

    @After
    public void tearDown() {
        System.setOut(this.oldOut);
    }

    @Test
    public void constructorChecksRemainingFunds() throws Wallet.WalletException, ApiException {
        new Expectations() {{
            final Wallet.InfoPair pair = new Wallet.InfoPair(walletInfo, priceInfo);
            walletCmd.call();
            result = pair;
        }};
        new NotifyFundInfoTask();
        assertTrue(out.toString().isEmpty());
    }

    @Test
    public void constructorNotifiesEmptyFunds() throws Wallet.WalletException, ApiException {
        Deencapsulation.setField(walletInfo, "balance", BigDecimal.ZERO);
        new Expectations() {{
            final Wallet.InfoPair pair = new Wallet.InfoPair(walletInfo, priceInfo);
            walletCmd.call();
            result = pair;
        }};
        new NotifyFundInfoTask();

        final String output = this.out.toString();
        System.err.println(output);

        NotifyFundInfoTask.Event event = gson.fromJson(output, NotifyFundInfoTask.Event.class);
        assertEquals("walletInfo", event.method);
        assertEquals(NotifyFundInfoTask.EventType.NoFunds, event.args.eventType);
    }

    @Test
    public void constructorHandlesAPIError(@Mocked APIUtils utils) throws Wallet.WalletException, ApiException {
        final String err = "expected error";
        new Expectations() {{
            APIUtils.getErrorMessage(withAny(new ApiException()));
            result = err;

            walletCmd.call();
            result = new ApiException();
        }};
        new NotifyFundInfoTask();

        final String output = this.out.toString();
        System.err.println(output);

        NotifyFundInfoTask.Event event = gson.fromJson(output, NotifyFundInfoTask.Event.class);
        assertEquals("walletInfo", event.method);
        assertEquals(NotifyFundInfoTask.EventType.Error, event.args.eventType);
        assertEquals(err, event.args.message);
    }

    @Test
    public void constructorHandlesWalletException() throws Wallet.WalletException, ApiException {
        final String err = "expected error";
        new Expectations() {{
            walletCmd.call();
            result = new Wallet.WalletException(err);
        }};
        new NotifyFundInfoTask();

        final String output = this.out.toString();
        System.err.println(output);

        NotifyFundInfoTask.Event event = gson.fromJson(output, NotifyFundInfoTask.Event.class);
        assertEquals("walletInfo", event.method);
        assertEquals(NotifyFundInfoTask.EventType.Error, event.args.eventType);
        assertEquals(err, event.args.message);
    }

    @Test
    public void runChecksRemainingFunds() throws Wallet.WalletException, ApiException {
        new Expectations() {{
            final Wallet.InfoPair pair = new Wallet.InfoPair(walletInfo, priceInfo);
            walletCmd.call();
            result = pair;
        }};
        final NotifyFundInfoTask task = new NotifyFundInfoTask();

        final BigDecimal threshold = priceInfo.getContract().multiply(new BigDecimal(App.MinContracts));
        Deencapsulation.setField(
                walletInfo, "funds",
                threshold.add(walletInfo.getTotalSpending()).multiply(new BigDecimal(1.1)));

        new Expectations() {{
            final Wallet.InfoPair pair = new Wallet.InfoPair(walletInfo, priceInfo);
            walletCmd.call();
            result = pair;
        }};
        task.run();

        final String output = this.out.toString();
        System.err.println(output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void runNotifyInsufficientFunds() throws Wallet.WalletException, ApiException {
        new Expectations() {{
            final Wallet.InfoPair pair = new Wallet.InfoPair(walletInfo, priceInfo);
            walletCmd.call();
            result = pair;
        }};
        final NotifyFundInfoTask task = new NotifyFundInfoTask();

        final BigDecimal threshold = priceInfo.getContract().multiply(new BigDecimal(App.MinContracts));
        Deencapsulation.setField(
                walletInfo, "funds",
                threshold.add(walletInfo.getTotalSpending()).multiply(new BigDecimal(0.8)));

        new Expectations() {{
            final Wallet.InfoPair pair = new Wallet.InfoPair(walletInfo, priceInfo);
            walletCmd.call();
            result = pair;
        }};
        task.run();

        final String output = this.out.toString();
        System.err.println(output);

        NotifyFundInfoTask.Event event = gson.fromJson(output, NotifyFundInfoTask.Event.class);
        assertEquals("walletInfo", event.method);
        assertEquals(NotifyFundInfoTask.EventType.InsufficientFunds, event.args.eventType);
    }

    /**
     * If autoAllocate is true and the current balance is bigger than the funds,
     * create a new allowance with the current balance.
     */
    @Test
    public void autoAllocate() throws Wallet.WalletException, ApiException {
        new Expectations() {{
            final Wallet.InfoPair pair = new Wallet.InfoPair(walletInfo, priceInfo);
            walletCmd.call();
            result = pair;
            times = 2;
        }};
        final NotifyFundInfoTask task = new NotifyFundInfoTask(true);

        final BigDecimal threshold = priceInfo.getContract().multiply(new BigDecimal(App.MinContracts));
        final BigDecimal newFunds = threshold.add(walletInfo.getTotalSpending()).multiply(new BigDecimal(1.1));
        final BigDecimal newBalance = newFunds.multiply(new BigDecimal(2));
        Deencapsulation.setField(walletInfo, "funds", newFunds);
        Deencapsulation.setField(walletInfo, "balance", newBalance);

        final InlineResponse2008SettingsAllowance info = new InlineResponse2008SettingsAllowance();
        info.setFunds(newBalance.toString());
        info.setHosts(hosts);
        info.setPeriod(period);
        info.setRenewwindow(renewWindow);
        final AllowanceInfo allowanceInfo = new AllowanceInfo(info);
        new Expectations() {{
            final Wallet.InfoPair pair = new Wallet.InfoPair(walletInfo, priceInfo);
            walletCmd.call();
            result = pair;
            createAllowanceCmd = new CreateAllowance(null);
            createAllowanceCmd.call();
            result = allowanceInfo;
        }};
        task.run();

        final String output = this.out.toString();
        System.err.println(output);

        NotifyFundInfoTask.Event event = gson.fromJson(output, NotifyFundInfoTask.Event.class);
        assertEquals("walletInfo", event.method);
        assertEquals(NotifyFundInfoTask.EventType.Allocated, event.args.eventType);
    }

    @Test
    public void handleAPIError(@Mocked APIUtils utils) throws Wallet.WalletException, ApiException {
        new Expectations() {{
            final Wallet.InfoPair pair = new Wallet.InfoPair(walletInfo, priceInfo);
            walletCmd.call();
            result = pair;
        }};
        final NotifyFundInfoTask task = new NotifyFundInfoTask();

        final BigDecimal threshold = priceInfo.getContract().multiply(new BigDecimal(App.MinContracts));
        Deencapsulation.setField(
                walletInfo, "funds",
                threshold.add(walletInfo.getTotalSpending()).multiply(new BigDecimal(1.1)));

        final String err = "expected error";
        new Expectations() {{
            APIUtils.getErrorMessage(withAny(new ApiException()));
            result = err;

            walletCmd.call();
            result = new ApiException();
        }};
        task.run();

        final String output = this.out.toString();
        System.err.println(output);

        NotifyFundInfoTask.Event event = gson.fromJson(output, NotifyFundInfoTask.Event.class);
        assertEquals("walletInfo", event.method);
        assertEquals(NotifyFundInfoTask.EventType.Error, event.args.eventType);
        assertEquals(err, event.args.message);
    }

    @Test
    public void handleWalletException() throws Wallet.WalletException, ApiException {
        new Expectations() {{
            final Wallet.InfoPair pair = new Wallet.InfoPair(walletInfo, priceInfo);
            walletCmd.call();
            result = pair;
        }};
        final NotifyFundInfoTask task = new NotifyFundInfoTask();

        final BigDecimal threshold = priceInfo.getContract().multiply(new BigDecimal(App.MinContracts));
        Deencapsulation.setField(
                walletInfo, "funds",
                threshold.add(walletInfo.getTotalSpending()).multiply(new BigDecimal(1.1)));

        final String err = "expected error";
        new Expectations() {{
            walletCmd.call();
            result = new Wallet.WalletException(err);
        }};
        task.run();

        final String output = this.out.toString();
        System.err.println(output);

        NotifyFundInfoTask.Event event = gson.fromJson(output, NotifyFundInfoTask.Event.class);
        assertEquals("walletInfo", event.method);
        assertEquals(NotifyFundInfoTask.EventType.Error, event.args.eventType);
        assertEquals(err, event.args.message);
    }

}