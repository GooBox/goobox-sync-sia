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
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.model.StandardError;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;

import java.math.BigDecimal;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class APIUtilsTest {

    private Gson gson = new Gson();

    @Test
    public void testGetErrorMessage() {

        final String errMsg = "test error message";
        final StandardError err = new StandardError();
        err.setMessage(errMsg);

        final ApiException e = new ApiException(501, "", null, gson.toJson(err));
        assertEquals(errMsg, APIUtils.getErrorMessage(e));

    }

    @Test
    public void testGetErrorMessageWithEmptyBody() {

        final String anotherMsg = "another msg";
        final ApiException e = new ApiException(501, anotherMsg, null, "");
        assertEquals(anotherMsg, APIUtils.getErrorMessage(e));

    }

    @Test
    public void testGetErrorMessageWithNullBody() {

        final ApiException e = new ApiException();
        assertEquals(null, APIUtils.getErrorMessage(e));

    }

    @Test
    public void testGetErrorMessageWtihBrokenJsonObject() {

        final String anotherMsg = "{ this is not: json}";
        final ApiException e = new ApiException(501, "", null, anotherMsg);
        assertEquals(anotherMsg, APIUtils.getErrorMessage(e));

    }

    @Test
    public void toSlash() {
        assertEquals("/path/to/somefile", APIUtils.toSlash(Paths.get("/path", "to\\somefile")));
    }

    @Test
    public void fromSlash() {
        assertEquals(Paths.get("path"), APIUtils.fromSlash("path"));
        assertEquals(Paths.get("path", "to", "somefile"), APIUtils.fromSlash("path/to/somefile"));
    }

    @Test
    public void fromSlashOfAbsolutePaths() {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);
        assertEquals(Paths.get("/path"), APIUtils.fromSlash("/path"));
        assertEquals(Paths.get("/path", "to", "somefile"), APIUtils.fromSlash("/path/to/somefile"));
    }

    @Test
    public void toHasting() {
        final double input = 123.45;
        assertEquals(BigDecimal.valueOf(input).multiply(APIUtils.Hasting).toBigInteger(), APIUtils.toHasting(input));
    }

    @Test
    public void toSiacoinFromBigInteger() {
        final double input = 123.45;
        assertEquals(input, APIUtils.toSiacoin(APIUtils.toHasting(input)).doubleValue(), 0.0001);
    }

    @Test
    public void toSiacoinFromString() {
        final double input = 123.45;
        assertEquals(input, APIUtils.toSiacoin(APIUtils.toHasting(input).toString()).doubleValue(), 0.0001);
    }

}