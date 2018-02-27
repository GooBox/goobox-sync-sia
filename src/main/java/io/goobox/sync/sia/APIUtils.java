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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.squareup.okhttp.OkHttpClient;
import io.goobox.sync.sia.client.ApiClient;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.model.StandardError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class APIUtils {

    private static final Logger logger = LoggerFactory.getLogger(APIUtils.class);

    /**
     * Defines 1 SC in hastings.
     */
    static final BigDecimal Hasting = new BigDecimal("1000000000000000000000000");

    /**
     * Load configuration.
     *
     * @param configFilePath to the config file.
     * @return a Config object.
     */
    @NotNull
    public static Config loadConfig(@NotNull final Path configFilePath) {

        Config cfg;
        try {
            cfg = Config.load(configFilePath);
        } catch (IOException e) {
            logger.warn("Failed to read config file {}: {}", configFilePath, e.getMessage());
            logger.info("Loading the default configuration");
            cfg = new Config(configFilePath);
        }
        return cfg;

    }

    /**
     * Creates an API client.
     *
     * @return an ApiClient object.
     */
    @NotNull
    public static ApiClient getApiClient(@NotNull Config cfg) {

        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(String.format("http://%s", cfg.getSiadApiAddress()));
        final OkHttpClient httpClient = apiClient.getHttpClient();
        httpClient.setConnectTimeout(0, TimeUnit.MILLISECONDS);
        httpClient.setReadTimeout(0, TimeUnit.MILLISECONDS);
        return apiClient;

    }

    /**
     * Parse error massages in an APIException.
     */
    @Nullable
    public static String getErrorMessage(@NotNull final ApiException e) {

        final String body = e.getResponseBody();
        if (body == null || body.isEmpty()) {
            return e.getMessage();
        }

        final Gson gson = new Gson();
        try {
            final StandardError err = gson.fromJson(body, StandardError.class);
            return err.getMessage();
        } catch (final JsonSyntaxException e1) {
            return body;
        }

    }

    @NotNull
    public static String toSlash(@NotNull final Path path) {
        return path.toString().replace("\\", "/");
    }

    @NotNull
    public static Path fromSlash(@NotNull final String path) {

        final String[] components = path.split("/");
        if (components.length < 2) {
            return Paths.get(path);
        }
        if (components[0].isEmpty()) {
            components[0] = "/";
        }

        return Paths.get(components[0], Arrays.copyOfRange(components, 1, components.length));

    }

    /**
     * Convert Siacoin to hastings.
     *
     * @param siacoin in double.
     * @return a big decimal representing the give sc in hastings.
     */
    @NotNull
    public static BigInteger toHasting(final double siacoin) {
        return BigDecimal.valueOf(siacoin).multiply(Hasting).toBigInteger();
    }

    /**
     * Convert hastings to Siacoin.
     *
     * @param hastings in BigInteger
     * @return a big decimal representing the given hastings in sc.
     */
    @NotNull
    public static BigDecimal toSiacoin(@NotNull final BigInteger hastings) {
        return new BigDecimal(hastings).divide(Hasting, 4, RoundingMode.HALF_UP);
    }

    /**
     * Convert hastings to Siacoin.
     *
     * @param hastings in String
     * @return a big decimal representing the given hastings in sc.
     */
    @NotNull
    public static BigDecimal toSiacoin(@NotNull final String hastings) {
        return toSiacoin(new BigInteger(hastings));
    }

}
