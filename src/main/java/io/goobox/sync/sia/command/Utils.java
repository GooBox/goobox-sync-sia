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
import io.goobox.sync.sia.client.ApiClient;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class Utils {

    public static final BigDecimal Hasting = new BigDecimal("1000000000000000000000000");

    public static ApiClient getApiClient() {

        final ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://localhost:9980");
        final OkHttpClient httpClient = apiClient.getHttpClient();
        httpClient.setConnectTimeout(0, TimeUnit.MILLISECONDS);
        httpClient.setReadTimeout(0, TimeUnit.MILLISECONDS);

        return apiClient;

    }

}
