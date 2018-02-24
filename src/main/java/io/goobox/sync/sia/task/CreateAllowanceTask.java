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

import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.WalletApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import io.goobox.sync.sia.model.AllowanceInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

public class CreateAllowanceTask implements Callable<AllowanceInfo> {

    private final Logger logger = LoggerFactory.getLogger(CreateAllowanceTask.class);

    static final int AllocationPeriod;
    @Nullable
    static final Integer NHosts;
    @Nullable
    static final Integer RenewWindow;

    static {

        final ResourceBundle bundle = ResourceBundle.getBundle("allowance");
        final int blockMonth = Integer.valueOf(bundle.getString("block-month"));
        final int allowanceMonths = Integer.valueOf(bundle.getString("allowance-months"));
        AllocationPeriod = blockMonth * allowanceMonths;

        if (bundle.containsKey("nhosts")) {
            NHosts = Integer.valueOf(bundle.getString("nhosts"));
        } else {
            NHosts = null;
        }

        if (bundle.containsKey("renew-window")) {
            RenewWindow = Integer.valueOf(bundle.getString("renew-window"));
        } else {
            RenewWindow = null;
        }

    }

    @NotNull
    private Context ctx;
    @Nullable
    private BigInteger fund;

    public CreateAllowanceTask(@NotNull Context ctx, @Nullable BigInteger fund) {
        this.ctx = ctx;
        this.fund = fund;
    }

    public CreateAllowanceTask(@NotNull Context ctx) {
        this(ctx, null);
    }

    @Override
    public AllowanceInfo call() throws ApiException {

        final WalletApi wallet = new WalletApi(this.ctx.getApiClient());
        final InlineResponse20013 walletInfo = wallet.walletGet();

        // If the wallet is locked, unlock it first.
        if (!walletInfo.isUnlocked()) {
            logger.info("Unlocking the wallet");
            wallet.walletUnlockPost(this.ctx.getConfig().getPrimarySeed());
        }

        // If fund is null, get current balance.
        final RenterApi renter = new RenterApi(this.ctx.getApiClient());
        if (this.fund == null) {
            // Allocating the current balance.
            this.fund = new BigInteger(walletInfo.getConfirmedsiacoinbalance());
        }

        // Allocate new fund.
        logger.info("Allocating {} hastings", this.fund);
        renter.renterPost(this.fund.toString(), NHosts, AllocationPeriod, RenewWindow);

        final InlineResponse2008SettingsAllowance allowance = renter.renterGet().getSettings().getAllowance();
        return new AllowanceInfo(allowance);

    }

}
