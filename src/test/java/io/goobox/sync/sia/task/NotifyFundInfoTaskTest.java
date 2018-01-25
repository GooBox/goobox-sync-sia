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
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiClient;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.model.InlineResponse20012;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse2008;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Financialmetrics;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Settings;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import io.goobox.sync.sia.command.CreateAllowance;
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
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class NotifyFundInfoTaskTest {

    private final int hosts = 30;
    private final long period = 6000;
    private final long renewWindow = 1000;

    @Mocked
    private GetWalletInfoTask walletInfoTask;
    @Mocked
    private CreateAllowance createAllowanceCmd;

    private Context ctx;
    private PriceInfo priceInfo;
    private WalletInfo walletInfo;
    private ByteArrayOutputStream out;
    private PrintStream oldOut;

    private Gson gson = new Gson();

    @SuppressWarnings("SpellCheckingInspection")
    @Before
    public void setUp() {
        this.ctx = new Context(new Config(), new ApiClient());
        this.walletInfoTask = new GetWalletInfoTask(this.ctx);

        this.out = new ByteArrayOutputStream();
        this.oldOut = System.out;
        System.setOut(new PrintStream(out));

        this.walletInfo = this.createWalletInfo();

        final double downloadPrice = 1234.5;
        final double uploadPrice = 1234.5;
        final double storagePrice = 12345.6;
        final double contractPrice = 1.123;

        final InlineResponse20012 prices = new InlineResponse20012();
        prices.setDownloadterabyte(APIUtils.toHasting(downloadPrice).toString());
        prices.setUploadterabyte(APIUtils.toHasting(uploadPrice).toString());
        prices.setStorageterabytemonth(APIUtils.toHasting(storagePrice).toString());
        prices.setFormcontracts(APIUtils.toHasting(contractPrice).toString());
        this.priceInfo = new PriceInfo(prices);
    }

    @After
    public void tearDown() {
        System.setOut(this.oldOut);
    }

    @Test
    public void constructorChecksRemainingFunds() throws GetWalletInfoTask.WalletException, ApiException {
        new Expectations() {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
            result = pair;
        }};
        new NotifyFundInfoTask(this.ctx);
        assertTrue(out.toString().isEmpty());
    }

    @Test
    public void constructorNotifiesEmptyFunds() throws GetWalletInfoTask.WalletException, ApiException {
        Deencapsulation.setField(walletInfo, "balance", BigInteger.ZERO);
        new Expectations() {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
            result = pair;
        }};
        new NotifyFundInfoTask(this.ctx);

        final String output = this.out.toString();
        System.err.println(output);

        NotifyFundInfoTask.Event event = gson.fromJson(output, NotifyFundInfoTask.Event.class);
        assertEquals("walletInfo", event.method);
        assertEquals(NotifyFundInfoTask.EventType.NoFunds, event.args.eventType);
    }

    @Test
    public void constructorHandlesAPIError(@SuppressWarnings("unused") @Mocked APIUtils utils) throws GetWalletInfoTask.WalletException, ApiException {
        final String err = "expected error";
        new Expectations() {{
            APIUtils.getErrorMessage(withAny(new ApiException()));
            result = err;

            walletInfoTask.call();
            result = new ApiException();
        }};
        new NotifyFundInfoTask(this.ctx);

        final String output = this.out.toString();
        System.err.println(output);

        NotifyFundInfoTask.Event event = gson.fromJson(output, NotifyFundInfoTask.Event.class);
        assertEquals("walletInfo", event.method);
        assertEquals(NotifyFundInfoTask.EventType.Error, event.args.eventType);
        assertEquals(err, event.args.message);
    }

    @Test
    public void constructorHandlesWalletException() throws GetWalletInfoTask.WalletException, ApiException {
        final String err = "expected error";
        new Expectations() {{
            walletInfoTask.call();
            result = new GetWalletInfoTask.WalletException(err);
        }};
        new NotifyFundInfoTask(this.ctx);

        final String output = this.out.toString();
        System.err.println(output);

        NotifyFundInfoTask.Event event = gson.fromJson(output, NotifyFundInfoTask.Event.class);
        assertEquals("walletInfo", event.method);
        assertEquals(NotifyFundInfoTask.EventType.Error, event.args.eventType);
        assertEquals(err, event.args.message);
    }

    @Test
    public void runChecksRemainingFunds() throws GetWalletInfoTask.WalletException, ApiException {
        new Expectations() {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
            result = pair;
        }};
        final NotifyFundInfoTask task = new NotifyFundInfoTask(this.ctx);

        final BigInteger threshold = priceInfo.getContract().multiply(BigInteger.valueOf(App.MinContracts));
        Deencapsulation.setField(
                walletInfo, "funds",
                threshold.add(walletInfo.getTotalSpending()).multiply(BigInteger.valueOf(2)));

        new Expectations() {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
            result = pair;
        }};
        task.run();

        final String output = this.out.toString();
        System.err.println(output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void runNotifyInsufficientFunds() throws GetWalletInfoTask.WalletException, ApiException {
        new Expectations() {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
            result = pair;
        }};
        final NotifyFundInfoTask task = new NotifyFundInfoTask(this.ctx);

        final BigInteger threshold = priceInfo.getContract().multiply(BigInteger.valueOf(App.MinContracts));
        Deencapsulation.setField(
                walletInfo, "funds",
                threshold.add(walletInfo.getTotalSpending()).divide(BigInteger.valueOf(2)));

        new Expectations() {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
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
    public void autoAllocation() throws GetWalletInfoTask.WalletException, ApiException {
        // Threshold: contract fee * number of min. contracts + current spending.
        final BigInteger threshold = priceInfo.getContract()
                .multiply(BigInteger.valueOf(App.MinContracts))
                .add(walletInfo.getTotalSpending());
        // Set funds half of sufficient amount.
        final BigInteger funds = threshold.divide(BigInteger.valueOf(2));
        // Set balance double of sufficient amount
        final BigInteger balance = threshold.multiply(BigInteger.valueOf(2));
        Deencapsulation.setField(walletInfo, "funds", funds);
        Deencapsulation.setField(walletInfo, "balance", balance);

        new Expectations() {{
            // Constructor and the first call in run.
            final GetWalletInfoTask.InfoPair pairBefore = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);

            // after allocation.
            final WalletInfo newWalletInfo = createWalletInfo();
            Deencapsulation.setField(newWalletInfo, "funds", balance);
            Deencapsulation.setField(newWalletInfo, "balance", balance);
            final GetWalletInfoTask.InfoPair pairAfter = new GetWalletInfoTask.InfoPair(newWalletInfo, priceInfo);

            walletInfoTask.call();
            returns(pairBefore, pairBefore, pairAfter);
        }};
        final NotifyFundInfoTask task = new NotifyFundInfoTask(this.ctx, true);

        new Expectations() {{
            final InlineResponse2008SettingsAllowance info = new InlineResponse2008SettingsAllowance();
            info.setFunds(balance.toString());
            info.setHosts(hosts);
            info.setPeriod(period);
            info.setRenewwindow(renewWindow);
            final AllowanceInfo allowanceInfo = new AllowanceInfo(info);
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

    /**
     * If autoAllocate is true but the current balance is not bigger than the funds,
     * do not create a new allowance with the current balance.
     */
    @Test
    public void skipAllocation() throws GetWalletInfoTask.WalletException, ApiException {
        new Expectations() {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
            result = pair;
            times = 2;
        }};
        final NotifyFundInfoTask task = new NotifyFundInfoTask(this.ctx, true);

        final BigInteger threshold = priceInfo.getContract().multiply(BigInteger.valueOf(App.MinContracts));
        final BigInteger newFunds = threshold.add(walletInfo.getTotalSpending()).multiply(BigInteger.valueOf(2));
        Deencapsulation.setField(walletInfo, "funds", newFunds);
        Deencapsulation.setField(walletInfo, "balance", newFunds);

        task.run();

        final String output = this.out.toString();
        assertTrue(output.isEmpty());
    }

    @Test
    public void handleAPIError(@SuppressWarnings("unused") @Mocked APIUtils utils) throws GetWalletInfoTask.WalletException, ApiException {
        new Expectations() {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
            result = pair;
        }};
        final NotifyFundInfoTask task = new NotifyFundInfoTask(this.ctx);

        final BigInteger threshold = priceInfo.getContract().multiply(BigInteger.valueOf(App.MinContracts));
        Deencapsulation.setField(
                walletInfo, "funds",
                threshold.add(walletInfo.getTotalSpending()).multiply(BigInteger.valueOf(2)));

        final String err = "expected error";
        new Expectations() {{
            APIUtils.getErrorMessage(withAny(new ApiException()));
            result = err;

            walletInfoTask.call();
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
    public void handleWalletException() throws GetWalletInfoTask.WalletException, ApiException {
        new Expectations() {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
            result = pair;
        }};
        final NotifyFundInfoTask task = new NotifyFundInfoTask(this.ctx);

        final BigInteger threshold = priceInfo.getContract().multiply(BigInteger.valueOf(App.MinContracts));
        Deencapsulation.setField(
                walletInfo, "funds",
                threshold.add(walletInfo.getTotalSpending()).multiply(BigInteger.valueOf(2)));

        final String err = "expected error";
        new Expectations() {{
            walletInfoTask.call();
            result = new GetWalletInfoTask.WalletException(err);
        }};
        task.run();

        final String output = this.out.toString();
        System.err.println(output);

        NotifyFundInfoTask.Event event = gson.fromJson(output, NotifyFundInfoTask.Event.class);
        assertEquals("walletInfo", event.method);
        assertEquals(NotifyFundInfoTask.EventType.Error, event.args.eventType);
        assertEquals(err, event.args.message);
    }

    private WalletInfo createWalletInfo() {
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

        final InlineResponse20013 wallet = new InlineResponse20013();
        wallet.setConfirmedsiacoinbalance(APIUtils.toHasting(balance).toString());
        wallet.setUnconfirmedincomingsiacoins(APIUtils.toHasting(income).toString());
        wallet.setUnconfirmedoutgoingsiacoins(APIUtils.toHasting(outcome).toString());

        final InlineResponse2008 info = new InlineResponse2008();
        final InlineResponse2008Settings settings = new InlineResponse2008Settings();
        final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
        allowance.setFunds(APIUtils.toHasting(balance).toString());
        allowance.setHosts(hosts);
        allowance.setPeriod(period);
        allowance.setRenewwindow(renewWindow);
        settings.setAllowance(allowance);
        info.setSettings(settings);
        //noinspection SpellCheckingInspection
        final InlineResponse2008Financialmetrics spending = new InlineResponse2008Financialmetrics();
        spending.setDownloadspending(APIUtils.toHasting(downloadSpending).toString());
        spending.setUploadspending(APIUtils.toHasting(uploadSpending).toString());
        spending.setStoragespending(APIUtils.toHasting(storageSpending).toString());
        spending.setContractspending(APIUtils.toHasting(contractSpending).toString());
        info.setFinancialmetrics(spending);
        info.setCurrentperiod(String.valueOf(currentPeriod));

        return new WalletInfo(address, primarySeed, wallet, info);
    }

}