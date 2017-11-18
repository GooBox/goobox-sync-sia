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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.squareup.okhttp.OkHttpClient;

import io.goobox.sync.sia.client.ApiClient;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.WalletApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse20014;
import io.goobox.sync.sia.client.api.model.InlineResponse20016;
import io.goobox.sync.storj.TaskExecutor;
import io.goobox.sync.storj.Utils;

public class App {

	static final String CONFIG_FILE = "goobox.properties";

	private Logger logger = LogManager.getLogger();

	public static void main(String[] args) {
		new App().init();
	}

	private void init() {

		final Config cfg = this.loadConfig(CONFIG_FILE);
		if (!checkAndCreateSyncDir()) {
			System.exit(1);
		}

		if (!checkAndCreateDataDir()) {
			System.exit(1);
		}

		final ApiClient apiClient = new ApiClient();
		apiClient.setBasePath("http://localhost:9980");
		final OkHttpClient httpClient = apiClient.getHttpClient();
		httpClient.setConnectTimeout(0, TimeUnit.MILLISECONDS);
		httpClient.setReadTimeout(0, TimeUnit.MILLISECONDS);

		this.prepareWallet(cfg, apiClient);

		BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
		tasks.add(new CheckConsensusTask(new Context(cfg, apiClient, tasks)));
		new TaskExecutor(tasks).start();

	}

	/**
	 * Load configuration.
	 * 
	 * @param filename
	 * @return
	 */
	private Config loadConfig(final String filename) {

		Config cfg;
		try {
			cfg = Config.load(CONFIG_FILE);
		} catch (IOException e) {
			System.err.printf("cannot load the config file: %s\n", e.getMessage());
			cfg = new Config();
		}
		return cfg;

	}

	private boolean checkAndCreateSyncDir() {
		this.logger.info("Checking if local Goobox sync folder exists: {}", Utils.getSyncDir());
		return checkAndCreateFolder(Utils.getSyncDir());
	}

	private boolean checkAndCreateDataDir() {
		this.logger.info("Checking if Goobox data folder exists: {}", Utils.getDataDir());
		return checkAndCreateFolder(Utils.getDataDir());
	}

	private boolean checkAndCreateFolder(Path path) {
		if (Files.exists(path)) {
			return true;
		} else {
			try {
				Files.createDirectory(path);
				this.logger.info("Folder {} has been created", path);
				return true;
			} catch (IOException e) {
				this.logger.error("Failed to create folder {}: {}", path, e.getMessage());
				return false;
			}
		}
	}

	/**
	 * Prepare the wallet.
	 * 
	 * If no wallets have been created, it'll initialize a wallet.
	 * 
	 * @throws ApiException
	 */
	private void prepareWallet(final Config cfg, final ApiClient apiClient) {

		final WalletApi api = new WalletApi(apiClient);
		InlineResponse20013 wallet = null;
		try {
			wallet = api.walletGet();
		} catch (ApiException e) {
			this.logger.error("siad seems not running: {}", e.getMessage());
			System.exit(1);
		}

		if (!wallet.getUnlocked()) {

			try {

				this.logger.info("Unlocking a wallet");
				api.walletUnlockPost(cfg.primarySeed);

			} catch (ApiException e) {

				this.logger.info("Initializing a wallet");
				try {

					final InlineResponse20016 seed = api.walletInitPost("", null, false);
					cfg.primarySeed = seed.getPrimaryseed();

					api.walletUnlockPost(cfg.primarySeed);
					cfg.save(CONFIG_FILE);

				} catch (ApiException e1) {
					// Cannot initialize new wallet.
					e1.printStackTrace();
					System.exit(1);
				} catch (IOException e1) {
					// Cannot save the configuration.
					e1.printStackTrace();
					System.exit(1);
				}

			}

			try {
				InlineResponse20014 addr = api.walletAddressGet();
				this.logger.info("Address of the wallet is {}", addr.getAddress());
			} catch (ApiException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

}
