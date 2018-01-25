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
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.command.CreateAllowance;
import io.goobox.sync.sia.model.AllowanceInfo;
import io.goobox.sync.sia.model.PriceInfo;
import io.goobox.sync.sia.model.WalletInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class NotifyFundInfoTask extends AbstractNotifyWalletInfoTask {

    private static Logger logger = LoggerFactory.getLogger(NotifyFundInfoTask.class);

    @NotNull
    private final Context ctx;
    private final boolean autoAllocate;
    private final Gson gson = new Gson();

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
                    final CreateAllowance createAllowance = new CreateAllowance(null);
                    final AllowanceInfo allowance = createAllowance.call();
                    this.sendEvent(
                            EventType.Allocated,
                            String.format("Allocated %.4f SC", APIUtils.toSiacoin(allowance.getFunds()))
                    );
                    info = wallet.call().getWalletInfo();
                    logger.info("Current balance = {} H, funds = {} H", info.getBalance(), info.getFunds());
                }
            }

            final BigInteger remaining = info.getFunds().subtract(info.getTotalSpending());
            final BigInteger threshold = prices.getContract().multiply(BigInteger.valueOf(App.MinContracts));
            if (remaining.compareTo(threshold) < 0) {
                this.sendEvent(
                        EventType.InsufficientFunds,
                        String.format("Should have more than %.4f SC", APIUtils.toSiacoin(threshold))
                );
            }

        } catch (final ApiException e) {

            logger.error(APIUtils.getErrorMessage(e));
            this.sendEvent(EventType.Error, APIUtils.getErrorMessage(e));

        } catch (final GetWalletInfoTask.WalletException e) {

            logger.error(e.getMessage());
            this.sendEvent(EventType.Error, e.getMessage());

        }
    }
}
