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
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiClient;
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
import io.goobox.sync.sia.model.PriceInfo;
import io.goobox.sync.sia.model.WalletInfo;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class GetWalletInfoTaskTest {

    private final String address = "01234567890123456789";
    private final String primarySeed = "sample primary seed";

    @Mocked
    private WalletApi wallet = new WalletApi();
    @Mocked
    private RenterApi renter = new RenterApi();

    private Path configPath;
    private Context ctx;
    private GetWalletInfoTask task;
    private InlineResponse20013 walletGetResponse;
    private InlineResponse2008 renterGetResponse;
    private InlineResponse20012 renterPriceGetResponse;

    @Before
    public void setUp() throws IOException {

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

        this.configPath = Files.createTempFile(null, null);
        final Config cfg = new Config(this.configPath);
        cfg.setPrimarySeed(primarySeed);
        new Expectations(cfg) {{
            cfg.save();
            minTimes = 0;
        }};
        ctx = new Context(cfg, new ApiClient());
        task = new GetWalletInfoTask(ctx);

        walletGetResponse = new InlineResponse20013();
        walletGetResponse.setUnlocked(false);
        walletGetResponse.setConfirmedsiacoinbalance(APIUtils.toHasting(balance).toString());
        walletGetResponse.setUnconfirmedincomingsiacoins(APIUtils.toHasting(income).toString());
        walletGetResponse.setUnconfirmedoutgoingsiacoins(APIUtils.toHasting(outcome).toString());

        renterGetResponse = new InlineResponse2008();
        final InlineResponse2008Settings settings = new InlineResponse2008Settings();
        final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
        allowance.setFunds(APIUtils.toHasting(funds).toString());
        allowance.setHosts(hosts);
        allowance.setPeriod(period);
        allowance.setRenewwindow(renewWindow);
        settings.setAllowance(allowance);
        renterGetResponse.setSettings(settings);
        //noinspection SpellCheckingInspection
        final InlineResponse2008Financialmetrics spending = new InlineResponse2008Financialmetrics();
        spending.setDownloadspending(APIUtils.toHasting(downloadSpending).toString());
        spending.setUploadspending(APIUtils.toHasting(uploadSpending).toString());
        spending.setStoragespending(APIUtils.toHasting(storageSpending).toString());
        spending.setContractspending(APIUtils.toHasting(contractSpending).toString());
        renterGetResponse.setFinancialmetrics(spending);
        renterGetResponse.setCurrentperiod(String.valueOf(currentPeriod));

        renterPriceGetResponse = new InlineResponse20012();
        renterPriceGetResponse.setDownloadterabyte(APIUtils.toHasting(downloadPrice).toString());
        renterPriceGetResponse.setUploadterabyte(APIUtils.toHasting(uploadPrice).toString());
        renterPriceGetResponse.setStorageterabytemonth(APIUtils.toHasting(storagePrice).toString());
        renterPriceGetResponse.setFormcontracts(APIUtils.toHasting(contractPrice).toString());

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() {
        this.configPath.toFile().delete();
    }

    /**
     * This test simulates the scenario that the wallet was initialized ant unlocked.
     */
    @Test
    public void withInitializedWallet() throws ApiException, GetWalletInfoTask.WalletException {

        new Expectations() {{
            walletGetResponse.setUnlocked(true);
            wallet.walletGet();
            result = walletGetResponse;

            final InlineResponse20014 res2 = new InlineResponse20014();
            res2.setAddress(address);
            wallet.walletAddressGet();
            result = res2;

            renter.renterGet();
            result = renterGetResponse;

            renter.renterPricesGet();
            result = renterPriceGetResponse;
        }};

        final GetWalletInfoTask.InfoPair res = task.call();
        assertEquals(new WalletInfo(address, primarySeed, walletGetResponse, renterGetResponse), res.getWalletInfo());
        assertEquals(new PriceInfo(renterPriceGetResponse), res.getPriceInfo());

    }

    /**
     * This test simulates the scenario that the wallet was initialized but want't unlocked.
     */
    @Test
    public void withLockedWallet() throws ApiException, GetWalletInfoTask.WalletException {

        new Expectations() {{
            wallet.walletGet();
            result = walletGetResponse;

            wallet.walletUnlockPost(primarySeed);

            final InlineResponse20014 res2 = new InlineResponse20014();
            res2.setAddress(address);
            wallet.walletAddressGet();
            result = res2;

            renter.renterGet();
            result = renterGetResponse;

            renter.renterPricesGet();
            result = renterPriceGetResponse;
        }};

        final GetWalletInfoTask.InfoPair res = task.call();
        assertEquals(new WalletInfo(address, primarySeed, walletGetResponse, renterGetResponse), res.getWalletInfo());
        assertEquals(new PriceInfo(renterPriceGetResponse), res.getPriceInfo());

    }

    /**
     * This test simulates the scenario that the wallet isn't initialized but the primary seed is given.
     */
    @SuppressWarnings("unused")
    @Test
    public void withoutInitializedWallet(@Mocked WaitSynchronizationTask waitSynchronization) throws ApiException, GetWalletInfoTask.WalletException {

        new Expectations() {{
            wallet.walletGet();
            result = walletGetResponse;

            wallet.walletUnlockPost(primarySeed);
            result = new ApiException();
            result = null;

            final WaitSynchronizationTask waitSynchronization = new WaitSynchronizationTask(ctx);
            waitSynchronization.call();

            wallet.walletInitPost("", null, true);
            wallet.walletInitSeedPost("", primarySeed, true, null);

            final InlineResponse20014 res2 = new InlineResponse20014();
            res2.setAddress(address);
            wallet.walletAddressGet();
            result = res2;

            renter.renterGet();
            result = renterGetResponse;

            renter.renterPricesGet();
            result = renterPriceGetResponse;
        }};

        final GetWalletInfoTask.InfoPair res = task.call();
        assertEquals(new WalletInfo(address, primarySeed, walletGetResponse, renterGetResponse), res.getWalletInfo());
        assertEquals(new PriceInfo(renterPriceGetResponse), res.getPriceInfo());

    }

    /**
     * This test simulates the scenario that the wallet isn't initialized and no primary seed is given.
     */
    @Test
    public void noWalletExists() throws ApiException, GetWalletInfoTask.WalletException, IOException {

        final Config cfg = this.ctx.getConfig();
        cfg.setPrimarySeed("");

        new Expectations(cfg) {{
            wallet.walletGet();
            result = walletGetResponse;

            wallet.walletUnlockPost("");
            result = new ApiException();

            final InlineResponse20016 seed = new InlineResponse20016();
            seed.setPrimaryseed(primarySeed);
            wallet.walletInitPost("", null, false);
            result = seed;

            wallet.walletUnlockPost(primarySeed);

            final InlineResponse20014 res2 = new InlineResponse20014();
            res2.setAddress(address);
            wallet.walletAddressGet();
            result = res2;

            renter.renterGet();
            result = renterGetResponse;

            renter.renterPricesGet();
            result = renterPriceGetResponse;

            cfg.save();
        }};


        final GetWalletInfoTask.InfoPair res = task.call();
        assertEquals(new WalletInfo(address, primarySeed, walletGetResponse, renterGetResponse), res.getWalletInfo());
        assertEquals(new PriceInfo(renterPriceGetResponse), res.getPriceInfo());

    }

    /**
     * This test simulates the scenario that the wallet was initialized and want't unlocked, but primary seed isn't given.
     * In this case, a wallet exception must be thrown.
     */
    @Test(expected = GetWalletInfoTask.WalletException.class)
    public void withoutPrimarySeed() throws ApiException, GetWalletInfoTask.WalletException {

        new Expectations() {{
            wallet.walletGet();
            result = walletGetResponse;

            wallet.walletUnlockPost("");
            result = new ApiException();

            wallet.walletInitPost("", null, false);
            result = new ApiException("Wallet is already initialized with a primary seed");
        }};

        this.ctx.getConfig().setPrimarySeed("");
        task.call();
    }

    /**
     * This test simulates the scenario that the wallet was initialized with a seed but the user wants to overwrite it
     * and creates a new wallet.
     */
    @Test
    public void forceInitializeWallet() throws ApiException, GetWalletInfoTask.WalletException, IOException {

        task = new GetWalletInfoTask(ctx, true);
        final Config cfg = this.ctx.getConfig();
        cfg.setPrimarySeed("");

        new Expectations() {{
            wallet.walletGet();
            result = walletGetResponse;

            wallet.walletUnlockPost("");
            result = new ApiException();

            final InlineResponse20016 seed = new InlineResponse20016();
            seed.setPrimaryseed(primarySeed);
            wallet.walletInitPost("", null, true);
            result = seed;

            wallet.walletUnlockPost(primarySeed);

            final InlineResponse20014 res2 = new InlineResponse20014();
            res2.setAddress(address);
            wallet.walletAddressGet();
            result = res2;

            renter.renterGet();
            result = renterGetResponse;

            renter.renterPricesGet();
            result = renterPriceGetResponse;

            cfg.save();
        }};

        final GetWalletInfoTask.InfoPair res = task.call();
        assertEquals(new WalletInfo(address, primarySeed, walletGetResponse, renterGetResponse), res.getWalletInfo());
        assertEquals(new PriceInfo(renterPriceGetResponse), res.getPriceInfo());

    }

}