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
import io.goobox.sync.sia.command.CreateAllowance;
import io.goobox.sync.sia.command.Wallet;
import io.goobox.sync.sia.model.AllowanceInfo;
import io.goobox.sync.sia.model.PriceInfo;
import io.goobox.sync.sia.model.WalletInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public class NotifyFundInfoTask implements Runnable {

    private static Logger logger = LogManager.getLogger();

    public enum EventType {
        // Notify when the funds are 0.
        NoFunds,
        // Notify when the funds are not enough.
        InsufficientFunds,
        // Notify when the creating allowance successes.
        Allocated,
        // Notify when an error occurs.
        Error
    }

    public class Args {
        @NotNull
        final EventType eventType;
        @Nullable
        final String message;

        Args(@NotNull final EventType eventType, @Nullable final String message) {
            this.eventType = eventType;
            this.message = message;
        }
    }

    public class Event {
        @SuppressWarnings("unused")
        final String method = "walletInfo";
        @NotNull
        final Args args;

        Event(@NotNull final EventType eventType, @Nullable final String message) {
            this.args = new Args(eventType, message);
        }

        Event(@NotNull final EventType eventType) {
            this(eventType, null);
        }
    }

    private final boolean autoAllocate;
    private final Gson gson = new Gson();

    public NotifyFundInfoTask(final boolean autoAllocate) {
        final Wallet wallet = new Wallet();
        try {
            final WalletInfo info = wallet.call().getWalletInfo();
            if (info.getBalance().equals(BigDecimal.ZERO)) {
                System.out.println(this.gson.toJson(new Event(EventType.NoFunds)));
            }
        } catch (final ApiException e) {
            logger.error(APIUtils.getErrorMessage(e));
            System.out.println(this.gson.toJson(new Event(
                    EventType.Error,
                    APIUtils.getErrorMessage(e)))
            );
        } catch (final Wallet.WalletException e) {
            logger.error(e);
            System.out.println(this.gson.toJson(new Event(
                    EventType.Error,
                    e.getMessage()))
            );
        }
        this.autoAllocate = autoAllocate;
    }

    public NotifyFundInfoTask() {
        this(false);
    }

    @Override
    public void run() {
        final Wallet wallet = new Wallet();
        try {

            final Wallet.InfoPair pair = wallet.call();
            final WalletInfo info = pair.getWalletInfo();
            final PriceInfo prices = pair.getPriceInfo();
            if (this.autoAllocate) {
                final CreateAllowance createAllowance = new CreateAllowance(null);
                final AllowanceInfo allowance = createAllowance.call();
                System.out.println(this.gson.toJson(new Event(
                        EventType.Allocated,
                        String.format("Allocated %.4f SC", APIUtils.toSC(allowance.getFunds()))))
                );
            }

            final BigDecimal remaining = info.getFunds().subtract(info.getTotalSpending());
            final BigDecimal threshold = prices.getContract().multiply(new BigDecimal(App.MinContracts));
            if (remaining.compareTo(threshold) < 0) {
                System.out.println(this.gson.toJson(new Event(
                        EventType.InsufficientFunds,
                        String.format("Should have more than %.4f SC", APIUtils.toSC(threshold))))
                );
            }

        } catch (final ApiException e) {
            logger.error(APIUtils.getErrorMessage(e));
            System.out.println(this.gson.toJson(new Event(
                    EventType.Error,
                    APIUtils.getErrorMessage(e)))
            );
        } catch (final Wallet.WalletException e) {
            logger.error(e);
            System.out.println(this.gson.toJson(new Event(
                    EventType.Error,
                    e.getMessage()))
            );
        }
    }
}
