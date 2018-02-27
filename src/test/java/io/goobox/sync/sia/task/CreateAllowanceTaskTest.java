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
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.WalletApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse2008;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Settings;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import io.goobox.sync.sia.model.AllowanceInfo;
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

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class CreateAllowanceTaskTest {

    @Mocked
    private WalletApi wallet;
    @Mocked
    private RenterApi renter;

    private final double balance = 12345.02;

    private Path configPath;
    private Context ctx;
    private InlineResponse20013 walletGetResponse;
    private InlineResponse2008 renterGetResponse;
    private InlineResponse2008SettingsAllowance allowance;

    @Before
    public void setUp() throws IOException {
        this.configPath = Files.createTempFile(null, null);
        this.ctx = new Context(new Config(this.configPath.resolve(App.ConfigFileName)));

        final double fund = 2234.85;
        final int host = 10;
        final long period = 1234;
        final long renewWindow = 5;

        walletGetResponse = new InlineResponse20013();
        walletGetResponse.setConfirmedsiacoinbalance(APIUtils.toHasting(balance).toString());
        walletGetResponse.setUnlocked(true);

        allowance = new InlineResponse2008SettingsAllowance();
        allowance.setFunds(APIUtils.toHasting(fund).toString());
        allowance.setHosts(host);
        allowance.setPeriod(period);
        allowance.setRenewwindow(renewWindow);

        renterGetResponse = new InlineResponse2008();
        final InlineResponse2008Settings settings = new InlineResponse2008Settings();
        settings.setAllowance(allowance);
        renterGetResponse.setSettings(settings);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() {
        this.configPath.toFile().delete();
    }

    @Test
    public void withFundOption() throws ApiException {
        new Expectations() {{
            wallet.walletGet();
            result = walletGetResponse;

            renter.renterGet();
            result = renterGetResponse;

            final BigInteger newFund = APIUtils.toHasting(balance);
            renter.renterPost(
                    newFund.toString(),
                    CreateAllowanceTask.NHosts,
                    CreateAllowanceTask.AllocationPeriod,
                    CreateAllowanceTask.RenewWindow);
        }};
        final CreateAllowanceTask task = new CreateAllowanceTask(this.ctx);
        assertEquals(new AllowanceInfo(allowance), task.call());
    }

    @Test
    public void withoutFundOption() throws ApiException {
        final BigInteger fund = BigInteger.valueOf(1234567);
        new Expectations() {{
            wallet.walletGet();
            result = walletGetResponse;

            renter.renterGet();
            result = renterGetResponse;

            renter.renterPost(
                    fund.toString(),
                    CreateAllowanceTask.NHosts,
                    CreateAllowanceTask.AllocationPeriod,
                    CreateAllowanceTask.RenewWindow);
        }};
        final CreateAllowanceTask task = new CreateAllowanceTask(this.ctx, fund);
        assertEquals(new AllowanceInfo(allowance), task.call());
    }

    @Test
    public void withLockedWalletWithoutFundOption() throws ApiException {
        final Config cfg = this.ctx.getConfig();
        cfg.setPrimarySeed("a b c d e f g");
        new Expectations() {{
            walletGetResponse.setUnlocked(false);
            wallet.walletGet();
            result = walletGetResponse;

            wallet.walletUnlockPost(cfg.getPrimarySeed());

            renter.renterGet();
            result = renterGetResponse;

            final BigInteger newFund = APIUtils.toHasting(balance);
            renter.renterPost(
                    newFund.toString(),
                    CreateAllowanceTask.NHosts,
                    CreateAllowanceTask.AllocationPeriod,
                    CreateAllowanceTask.RenewWindow);
        }};
        final CreateAllowanceTask task = new CreateAllowanceTask(this.ctx);
        assertEquals(new AllowanceInfo(allowance), task.call());
    }

    @Test
    public void withLockedWalletAndFundOption() throws ApiException {
        final Config cfg = this.ctx.getConfig();
        cfg.setPrimarySeed("a b c d e f g");
        final BigInteger fund = BigInteger.valueOf(1234567);
        new Expectations() {{
            walletGetResponse.setUnlocked(false);
            wallet.walletGet();
            result = walletGetResponse;

            wallet.walletUnlockPost(cfg.getPrimarySeed());

            renter.renterGet();
            result = renterGetResponse;

            renter.renterPost(
                    fund.toString(),
                    CreateAllowanceTask.NHosts,
                    CreateAllowanceTask.AllocationPeriod,
                    CreateAllowanceTask.RenewWindow);
        }};
        final CreateAllowanceTask task = new CreateAllowanceTask(this.ctx, fund);
        assertEquals(new AllowanceInfo(allowance), task.call());
    }

}