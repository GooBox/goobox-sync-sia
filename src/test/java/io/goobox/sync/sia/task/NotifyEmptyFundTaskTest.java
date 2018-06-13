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
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse20014;
import io.goobox.sync.sia.client.api.model.InlineResponse2008;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Financialmetrics;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Settings;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(JMockit.class)
public class NotifyEmptyFundTaskTest {

    @Mocked
    private GetWalletInfoTask walletInfoTask;

    @Mocked
    private App app = new App();

    private Path configPath;
    private NotifyEmptyFundTask task;
    private PriceInfo priceInfo;
    private WalletInfo walletInfo;

    @Before
    public void setUp() throws IOException {
        this.configPath = Files.createTempFile(null, null);
        this.task = new NotifyEmptyFundTask(new Context(new Config(this.configPath)));

        final String address = "01234567890123456789";
        final String primarySeed = "sample primary seed";
        final long hosts = 30;
        final long period = 6000;
        final long renewWindow = 1000;
        final double balance = 12345.02;
        final double income = 10;
        final double outcome = 15;
        final long currentPeriod = 3000;
        final double contractFees = 245.6;
        final double downloadSpending = 1.2345;
        final double uploadSpending = 0.223;
        final double storageSpending = 2.3;
        final double totalAllocated = 0.001;

        final InlineResponse20014 wallet = new InlineResponse20014();
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
        spending.setContractfees(APIUtils.toHasting(contractFees).toString());
        spending.setDownloadspending(APIUtils.toHasting(downloadSpending).toString());
        spending.setUploadspending(APIUtils.toHasting(uploadSpending).toString());
        spending.setStoragespending(APIUtils.toHasting(storageSpending).toString());
        spending.setTotalallocated(APIUtils.toHasting(totalAllocated).toString());
        info.setFinancialmetrics(spending);
        info.setCurrentperiod(String.valueOf(currentPeriod));

        this.walletInfo = new WalletInfo(address, primarySeed, wallet, info);

        final double downloadPrice = 1234.5;
        final double uploadPrice = 1234.5;
        final double storagePrice = 12345.6;
        final double contractPrice = 1.123;

        final InlineResponse20013 prices = new InlineResponse20013();
        prices.setDownloadterabyte(APIUtils.toHasting(downloadPrice).toString());
        prices.setUploadterabyte(APIUtils.toHasting(uploadPrice).toString());
        prices.setStorageterabytemonth(APIUtils.toHasting(storagePrice).toString());
        prices.setFormcontracts(APIUtils.toHasting(contractPrice).toString());
        this.priceInfo = new PriceInfo(prices);
    }

    @After
    public void tearDown() {
        this.configPath.toFile().delete();
    }

    @Test
    public void checksRemainingFunds() throws GetWalletInfoTask.WalletException, ApiException {
        new Expectations(this.task) {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
            result = pair;

            App.getInstance();
            result = Optional.of(app);
            times = 0;
        }};
        this.task.run();
    }

    @Test
    public void notifiesEmptyFunds() throws GetWalletInfoTask.WalletException, ApiException {
        Deencapsulation.setField(walletInfo, "balance", BigInteger.ZERO);
        new Expectations(this.task) {{
            final GetWalletInfoTask.InfoPair pair = new GetWalletInfoTask.InfoPair(walletInfo, priceInfo);
            walletInfoTask.call();
            result = pair;

            App.getInstance();
            result = Optional.of(app);
            app.notifyEvent(new FundEvent(FundEvent.EventType.NoFunds));
        }};
        this.task.run();
    }

    @Test
    public void handlesAPIError(@SuppressWarnings("unused") @Mocked APIUtils utils) throws GetWalletInfoTask.WalletException, ApiException {
        final String err = "expected error";
        new Expectations(this.task) {{
            APIUtils.getErrorMessage(withAny(new ApiException()));
            result = err;

            walletInfoTask.call();
            result = new ApiException();

            App.getInstance();
            result = Optional.of(app);
            app.notifyEvent(new FundEvent(FundEvent.EventType.Error, err));
        }};
        this.task.run();
    }

    @Test
    public void handlesWalletException() throws GetWalletInfoTask.WalletException, ApiException {
        final String err = "expected error";
        new Expectations(this.task) {{
            walletInfoTask.call();
            result = new GetWalletInfoTask.WalletException(err);

            App.getInstance();
            result = Optional.of(app);
            app.notifyEvent(new FundEvent(FundEvent.EventType.Error, err));
        }};
        this.task.run();
    }

}