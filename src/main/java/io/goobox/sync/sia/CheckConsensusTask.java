/*
 * Copyright (C) 2017 Junpei Kawamoto
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
package io.goobox.sync.sia;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.ConsensusApi;
import io.goobox.sync.sia.client.api.model.InlineResponse2006;

/**
 * CheckConsensusTask checks the local block chain.
 *
 */
public class CheckConsensusTask implements Runnable {

	private final Context ctx;

	private final Logger logger = LogManager.getLogger();

	/**
	 * Create a check consensus task.
	 * 
	 * @param ctx
	 *            context
	 */
	public CheckConsensusTask(final Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public void run() {

		this.logger.info("Checking siad is synchronized");
		final ConsensusApi api = new ConsensusApi(this.ctx.apiClient);
		try {

			InlineResponse2006 res = api.consensusGet();
			if (res.getSynced()) {

				// Check enough contracts have been signed.
				this.ctx.tasks.add(new CheckContractsTask(this.ctx));

			} else {

				this.logger.info("siad isn't synchronized");
				// Sleep some time
				this.ctx.tasks.add(new SleepTask());
				// Add itself to the queue
				this.ctx.tasks.add(this);

			}

		} catch (ApiException e) {
			this.logger.error("siad seems not running: {}", e.getMessage());
			System.exit(1);
		}

	}

}
