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

package io.goobox.sync.sia.command;

import com.squareup.okhttp.OkHttpClient;
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.client.ApiClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class Utils {

    /**
     * Default config file name.
     */
    static final String ConfigFileName = "goobox.properties";

    /**
     * Defines 1 SC in hastings.
     */
    static final BigDecimal Hasting = new BigDecimal("1000000000000000000000000");

    private static final Logger logger = LogManager.getLogger();

    /**
     * Creates an API client.
     *
     * @return an ApiClient object.
     */
    static ApiClient getApiClient() {

        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://localhost:9980");
        final OkHttpClient httpClient = apiClient.getHttpClient();
        httpClient.setConnectTimeout(0, TimeUnit.MILLISECONDS);
        httpClient.setReadTimeout(0, TimeUnit.MILLISECONDS);

        return apiClient;

    }

    /**
     * Load configuration.
     *
     * @param path file path to a configuration file.
     * @return a Config object.
     */
    static Config loadConfig(final Path path) {

        Config cfg;
        try {
            cfg = Config.load(path);
        } catch (IOException e) {
            logger.error("cannot load config file {}: {}", path, e.getMessage());
            cfg = new Config();
        }
        return cfg;

    }


}
