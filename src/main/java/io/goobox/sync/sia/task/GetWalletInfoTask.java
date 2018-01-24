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
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.WalletApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20012;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse20016;
import io.goobox.sync.sia.client.api.model.InlineResponse2008;
import io.goobox.sync.sia.model.PriceInfo;
import io.goobox.sync.sia.model.WalletInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.Callable;

public class GetWalletInfoTask implements Callable<GetWalletInfoTask.InfoPair> {

    private final Logger logger = LoggerFactory.getLogger(GetWalletInfoTask.class);

    public static class InfoPair {
        @NotNull
        private final WalletInfo walletInfo;
        @NotNull
        private final PriceInfo priceInfo;

        public InfoPair(@NotNull final WalletInfo walletInfo, @NotNull final PriceInfo priceInfo) {
            this.walletInfo = walletInfo;
            this.priceInfo = priceInfo;
        }

        @NotNull
        public WalletInfo getWalletInfo() {
            return walletInfo;
        }

        @NotNull
        public PriceInfo getPriceInfo() {
            return priceInfo;
        }
    }

    public static class WalletException extends Exception {
        public WalletException(final String msg) {
            super(msg);
        }
    }

    @NotNull
    private final Context ctx;
    private final boolean force;

    public GetWalletInfoTask(@NotNull final Context ctx, final boolean force) {
        this.ctx = ctx;
        this.force = force;
    }

    public GetWalletInfoTask(@NotNull final Context ctx) {
        this(ctx, false);
    }

    @Override
    public InfoPair call() throws ApiException, WalletException {

        final WalletApi walletApi = new WalletApi(this.ctx.apiClient);

        logger.info("Retrieving the wallet information");
        final InlineResponse20013 wallet = walletApi.walletGet();
        if (!wallet.getUnlocked()) {

            try {

                logger.info("Unlocking the wallet");
                walletApi.walletUnlockPost(this.ctx.getConfig().getPrimarySeed());

            } catch (final ApiException e) {

                if (e.getCause() instanceof ConnectException) {
                    throw e;
                }


                if (this.ctx.getConfig().getPrimarySeed().isEmpty()) {

                    // Create a new wallet.
                    logger.info("Initializing the wallet");
                    try {

                        final InlineResponse20016 seed = walletApi.walletInitPost(null, null, this.force);
                        this.ctx.getConfig().setPrimarySeed(seed.getPrimaryseed());
                        this.ctx.getConfig().save();

                    } catch (final ApiException e1) {

                        logger.error("Cannot initialize the wallet: {}", APIUtils.getErrorMessage(e1));
                        throw new WalletException("Cannot initialize the wallet. It may be locked with another seed");

                    } catch (IOException e1) {

                        logger.error("Failed to save the wallet information: {}", e1.getMessage());
                        throw new WalletException("Cannot save the wallet information");

                    }

                } else {
                    logger.info("Initializing a wallet with the given primary seed");
                    final WaitSynchronizationTask waitSynchronization = new WaitSynchronizationTask(this.ctx);
                    waitSynchronization.call();
                    walletApi.walletInitSeedPost(null, this.ctx.getConfig().getPrimarySeed(), true, null);
                }

                if (!walletApi.walletGet().getUnlocked()) {
                    logger.info("Retrying to unlock the wallet");
                    walletApi.walletUnlockPost(this.ctx.getConfig().getPrimarySeed());
                }

            }

        }

        final RenterApi renter = new RenterApi(this.ctx.apiClient);
        final InlineResponse2008 info = renter.renterGet();
        final WalletInfo walletInfo = new WalletInfo(
                walletApi.walletAddressGet().getAddress(), this.ctx.getConfig().getPrimarySeed(), wallet, info);

        final InlineResponse20012 prices = renter.renterPricesGet();
        final PriceInfo priceInfo = new PriceInfo(prices);

        return new InfoPair(walletInfo, priceInfo);

    }


}
