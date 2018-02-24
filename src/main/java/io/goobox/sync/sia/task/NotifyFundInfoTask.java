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
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.model.AllowanceInfo;
import io.goobox.sync.sia.model.PriceInfo;
import io.goobox.sync.sia.model.WalletInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.math.RoundingMode;

public class NotifyFundInfoTask implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(NotifyFundInfoTask.class);

    /**
     * If the funds are lower than the amount to cover the minimum sets of contracts,
     * this task notifying the user.
     */
    public static int MinContractSets = 2;

    @NotNull
    private final Context ctx;
    private final boolean autoAllocate;

    public NotifyFundInfoTask(@NotNull final Context ctx, final boolean autoAllocate) {
        this.ctx = ctx;
        this.autoAllocate = autoAllocate;
    }

    @SuppressWarnings("WeakerAccess")
    public NotifyFundInfoTask(@NotNull final Context ctx) {
        this(ctx, false);
    }

    @Override
    public void run() {
        final GetWalletInfoTask wallet = new GetWalletInfoTask(this.ctx);
        try {

            final GetWalletInfoTask.InfoPair pair = wallet.call();
            WalletInfo info = pair.getWalletInfo();
            logger.info(
                    "Current balance = {} H, funds = {} H", info.getBalance(), info.getFunds());

            final PriceInfo prices = pair.getPriceInfo();
            if (this.autoAllocate) {
                if (info.getBalance().compareTo(info.getFunds()) > 0) {
                    final CreateAllowanceTask createAllowance = new CreateAllowanceTask(this.ctx);
                    final AllowanceInfo allowance = createAllowance.call();

                    App.getInstance().ifPresent(app -> app.notifyEvent(new FundEvent(
                            FundEvent.EventType.Allocated,
                            String.format("Allocated %d SC", APIUtils.toSiacoin(
                                    allowance.getFunds()).setScale(0, RoundingMode.HALF_UP).toBigInteger()))));

                    info = wallet.call().getWalletInfo();
                    logger.info("Current balance = {} H, funds = {} H", info.getBalance(), info.getFunds());
                }
            }

            final BigInteger remaining = info.getFunds().subtract(info.getTotalSpending());
            final BigInteger threshold = prices.getContract().multiply(BigInteger.valueOf(NotifyFundInfoTask.MinContractSets));
            if (remaining.compareTo(threshold) < 0) {
                App.getInstance().ifPresent(app -> app.notifyEvent(new FundEvent(
                        FundEvent.EventType.InsufficientFunds,
                        String.format(
                                "Should have more than %d SC",
                                APIUtils.toSiacoin(threshold).setScale(0, RoundingMode.UP).toBigInteger()))));
            }

        } catch (final ApiException e) {

            logger.error(APIUtils.getErrorMessage(e));
            App.getInstance().ifPresent(app -> app.notifyEvent(
                    new FundEvent(FundEvent.EventType.Error, APIUtils.getErrorMessage(e))));

        } catch (final GetWalletInfoTask.WalletException e) {

            logger.error(e.getMessage());
            App.getInstance().ifPresent(app -> app.notifyEvent(
                    new FundEvent(FundEvent.EventType.Error, e.getMessage())));

        }
    }
}
