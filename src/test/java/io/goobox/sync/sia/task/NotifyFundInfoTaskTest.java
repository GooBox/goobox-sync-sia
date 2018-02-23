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

import java.io.IOException;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;

@RunWith(JMockit.class)
public class NotifyFundInfoTaskTest {

    private final int hosts = 30;
    private final long period = 6000;
    private final long renewWindow = 1000;

    @Mocked
    private GetWalletInfoTask walletInfoTask;
    @Mocked
    private CreateAllowanceTask createAllowanceTask;

    private Path configPath;
    private Context ctx;
    private NotifyFundInfoTask task;

    private PriceInfo priceInfo;
    private WalletInfo walletInfo;

    @SuppressWarnings("SpellCheckingInspection")
    @Before
    public void setUp() throws IOException {
        this.configPath = Files.createTempFile(null, null);
        this.ctx = new Context(new Config(this.configPath), new ApiClient());
        this.task = new NotifyFundInfoTask(this.ctx);

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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() {
        this.configPath.toFile().delete();
    }

    @Test
    public void checksRemainingFunds() throws GetWalletInfoTask.WalletException, ApiException {
        final BigInteger threshold = priceInfo.getContract().multiply(BigInteger.valueOf(NotifyFundInfoTask.MinContractSets));
        Deencapsulation.setField(
                walletInfo, "funds",
                threshold.add(walletInfo.getTotalSpending()).multiply(BigInteger.valueOf(2)));

        new Expectations(this.task) {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
            result = pair;

            task.sendEvent((AbstractNotifyWalletInfoTask.EventType) any, anyString);
            times = 0;
        }};
        task.run();
    }

    @Test
    public void notifyInsufficientFunds() throws GetWalletInfoTask.WalletException, ApiException {
        final BigInteger threshold = priceInfo.getContract().multiply(BigInteger.valueOf(NotifyFundInfoTask.MinContractSets));
        Deencapsulation.setField(
                walletInfo, "funds",
                threshold.add(walletInfo.getTotalSpending()).divide(BigInteger.valueOf(2)));

        new Expectations(this.task) {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
            result = pair;

            task.sendEvent(
                    AbstractNotifyWalletInfoTask.EventType.InsufficientFunds,
                    String.format(
                            "Should have more than %d SC",
                            APIUtils.toSiacoin(threshold).setScale(0, RoundingMode.UP).toBigInteger()));
        }};
        task.run();
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

        new Expectations(task) {{
            final InlineResponse2008SettingsAllowance info = new InlineResponse2008SettingsAllowance();
            info.setFunds(balance.toString());
            info.setHosts(hosts);
            info.setPeriod(period);
            info.setRenewwindow(renewWindow);
            final AllowanceInfo allowanceInfo = new AllowanceInfo(info);
            createAllowanceTask = new CreateAllowanceTask(ctx);
            createAllowanceTask.call();
            result = allowanceInfo;

            task.sendEvent(
                    AbstractNotifyWalletInfoTask.EventType.Allocated,
                    String.format("Allocated %d SC", APIUtils.toSiacoin(
                            allowanceInfo.getFunds()).setScale(0, RoundingMode.HALF_UP).toBigInteger()));
        }};
        task.run();

    }

    /**
     * If autoAllocate is true but the current balance is not bigger than the funds,
     * do not create a new allowance with the current balance.
     */
    @Test
    public void skipAllocation() throws GetWalletInfoTask.WalletException, ApiException {
        final NotifyFundInfoTask task = new NotifyFundInfoTask(this.ctx, true);
        final BigInteger threshold = priceInfo.getContract().multiply(BigInteger.valueOf(NotifyFundInfoTask.MinContractSets));
        final BigInteger newFunds = threshold.add(walletInfo.getTotalSpending()).multiply(BigInteger.valueOf(2));
        Deencapsulation.setField(walletInfo, "funds", newFunds);
        Deencapsulation.setField(walletInfo, "balance", newFunds);

        new Expectations(task) {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
            result = pair;

            task.sendEvent((AbstractNotifyWalletInfoTask.EventType) any, anyString);
            times = 0;
        }};
        task.run();
    }

    @Test
    public void handleAPIError(@SuppressWarnings("unused") @Mocked APIUtils utils) throws GetWalletInfoTask.WalletException, ApiException {
        final BigInteger threshold = priceInfo.getContract().multiply(BigInteger.valueOf(NotifyFundInfoTask.MinContractSets));
        Deencapsulation.setField(
                walletInfo, "funds",
                threshold.add(walletInfo.getTotalSpending()).multiply(BigInteger.valueOf(2)));

        final String err = "expected error";
        new Expectations(this.task) {{
            APIUtils.getErrorMessage(withAny(new ApiException()));
            result = err;

            walletInfoTask.call();
            result = new ApiException();

            task.sendEvent(AbstractNotifyWalletInfoTask.EventType.Error, err);
        }};
        task.run();
    }

    @Test
    public void handleWalletException() throws GetWalletInfoTask.WalletException, ApiException {
        final BigInteger threshold = priceInfo.getContract().multiply(BigInteger.valueOf(NotifyFundInfoTask.MinContractSets));
        Deencapsulation.setField(
                walletInfo, "funds",
                threshold.add(walletInfo.getTotalSpending()).multiply(BigInteger.valueOf(2)));

        final String err = "expected error";
        new Expectations(this.task) {{
            walletInfoTask.call();
            result = new GetWalletInfoTask.WalletException(err);

            task.sendEvent(AbstractNotifyWalletInfoTask.EventType.Error, err);
        }};
        task.run();
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