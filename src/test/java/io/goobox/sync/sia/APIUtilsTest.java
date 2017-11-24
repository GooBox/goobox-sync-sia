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
import org.junit.Test;

import static org.junit.Assert.*;

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

}