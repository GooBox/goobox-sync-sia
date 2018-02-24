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
import io.goobox.sync.sia.model.WalletInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class NotifyEmptyFundTask implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(NotifyEmptyFundTask.class);

    @NotNull
    private final Context ctx;

    public NotifyEmptyFundTask(@NotNull Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void run() {
        final GetWalletInfoTask wallet = new GetWalletInfoTask(ctx);
        try {
            final WalletInfo info = wallet.call().getWalletInfo();
            logger.debug("Current balance: {} hastings", info.getBalance());
            if (info.getBalance().equals(BigInteger.ZERO)) {
                App.getInstance().ifPresent(app -> app.notifyEvent(new FundEvent(FundEvent.EventType.NoFunds)));
            }
        } catch (final ApiException e) {
            logger.error(APIUtils.getErrorMessage(e));
            App.getInstance().ifPresent(
                    app -> app.notifyEvent(new FundEvent(FundEvent.EventType.Error, APIUtils.getErrorMessage(e))));
        } catch (final GetWalletInfoTask.WalletException e) {
            logger.error(e.getMessage());
            App.getInstance().ifPresent(
                    app -> app.notifyEvent(new FundEvent(FundEvent.EventType.Error, e.getMessage())));
        }
    }

}
