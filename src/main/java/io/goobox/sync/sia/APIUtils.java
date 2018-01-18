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
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class APIUtils {

    /**
     * Defines 1 SC in hastings.
     */
    private static final BigDecimal Hasting = new BigDecimal("1000000000000000000000000");

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
     * Convert SC to hastings.
     *
     * @param sc in double.
     * @return a big decimal representing the give sc in hastings.
     */
    public static BigDecimal toHastings(final double sc) {
        return new BigDecimal(sc).multiply(Hasting);
    }

    /**
     * Convert hastings to SC.
     *
     * @param hastings in BigDecimal
     * @return a big decimal representing the given hastings in sc.
     */
    public static BigDecimal toSC(final BigDecimal hastings) {
        return hastings.divide(Hasting, 4, RoundingMode.HALF_UP);
    }

    /**
     * Convert hastings to SC.
     *
     * @param hastings in String
     * @return a big decimal representing the given hastings in sc.
     */
    public static BigDecimal toSC(final String hastings) {
        return toSC(new BigDecimal(hastings));
    }

}
