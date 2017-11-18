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
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.model.InlineResponse2009;

/**
 * CheckContractsTask checks there are at least 20 contracts.
 * 
 * At least 20 contracts are required to upload files to SIA network.
 * 
 * @author junpei
 *
 */
public class CheckContractsTask implements Runnable {

	static final int MIN_CONTRACTS = 20;

	private final Context ctx;
	private final Logger logger = LogManager.getLogger();

	public CheckContractsTask(final Context ctx) {
		this.ctx = ctx;
	}

	@Override
	public void run() {

		this.logger.info("Checking contracts");
		final RenterApi api = new RenterApi(this.ctx.apiClient);
		try {

			final InlineResponse2009 contracts = api.renterContractsGet();
			if (contracts.getContracts().size() >= MIN_CONTRACTS) {

				// Start checking state.
				this.ctx.tasks.add(new CheckStateTask(this.ctx));
				this.ctx.tasks.add(new CheckDownloadTask(this.ctx));

			} else {

				this.logger.info("Not enough contracts have been signed yet");
				// Sleep some time to wait that siad signs enough contracts.
				this.ctx.tasks.add(new SleepTask());
				// Check the number of contracts again.
				this.ctx.tasks.add(this);

			}

		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
