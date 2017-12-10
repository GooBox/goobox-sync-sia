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
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.model.StandardError;

public class APIUtils {

    /**
     * Parse error massages in an APIException.
     */
    public static String getErrorMessage(final ApiException e) {

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

}
