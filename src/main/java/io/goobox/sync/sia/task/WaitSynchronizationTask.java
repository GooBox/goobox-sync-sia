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

import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.ConsensusApi;
import io.goobox.sync.sia.client.api.model.InlineResponse2006;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class WaitSynchronizationTask implements Callable<Void> {

    private final Logger logger = LoggerFactory.getLogger(WaitSynchronizationTask.class);

    @NotNull
    private final Context ctx;

    public WaitSynchronizationTask(@NotNull final Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public Void call() throws ApiException {

        logger.info("Checking consensus DB");
        final ConsensusApi api = new ConsensusApi(this.ctx.getApiClient());
        while (true) {

            final InlineResponse2006 res = api.consensusGet();
            if (res.isSynced()) {

                logger.info("Consensus DB is synchronized");
                return null;

            } else {

                logger.info("Consensus DB isn't synchronized (block height: {}), wait a minute", res.getHeight());
                try {
                    Thread.sleep(App.DefaultSleepTime);
                } catch (final InterruptedException e) {
                    logger.trace("Thread was interrupted until waiting synchronization: {}", e.getMessage());
                }

            }

        }

    }

}
