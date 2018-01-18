/*
 * Copyright (C) 2017-2018 Junpei Kawamoto
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

package io.goobox.sync.sia.model;

import io.goobox.sync.sia.APIUtils;
import io.goobox.sync.sia.client.api.model.InlineResponse20012;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PriceInfoTest {

    private final double downloadPrice = 1234.5;
    private final double uploadPrice = 1234.5;
    private final double storagePrice = 12345.6;
    private final double contractPrice = 1.123;
    private PriceInfo priceInfo;

    @Before
    public void setUp() {
        final InlineResponse20012 info = new InlineResponse20012();
        info.setDownloadterabyte(APIUtils.toHastings(downloadPrice).toString());
        info.setUploadterabyte(APIUtils.toHastings(uploadPrice).toString());
        info.setStorageterabytemonth(APIUtils.toHastings(storagePrice).toString());
        info.setFormcontracts(APIUtils.toHastings(contractPrice).toString());
        priceInfo = new PriceInfo(info);
    }

    @Test
    public void getDownload() {
        assertEquals(APIUtils.toHastings(downloadPrice), priceInfo.getDownload());
    }

    @Test
    public void getUpload() {
        assertEquals(APIUtils.toHastings(uploadPrice), priceInfo.getUpload());
    }

    @Test
    public void getStorage() {
        assertEquals(APIUtils.toHastings(storagePrice), priceInfo.getStorage());
    }

    @Test
    public void getContract() {
        assertEquals(APIUtils.toHastings(contractPrice), priceInfo.getContract());
    }

    @Test
    public void dump() {
        final String outputs = priceInfo.toString();
        assertTrue(
                String.format("download: %.4f SC/TB", downloadPrice),
                outputs.contains(String.format("download: %.4f SC/TB", downloadPrice)));
        assertTrue(
                String.format("upload: %.4f SC/TB", uploadPrice),
                outputs.contains(String.format("upload: %.4f SC/TB", uploadPrice)));
        assertTrue(
                String.format("storage: %.4f SC/TB", storagePrice),
                outputs.contains(String.format("storage: %.4f SC/TB*Month", storagePrice)));
        assertTrue(
                String.format("contract: %.4f SC", contractPrice),
                outputs.contains(String.format("contract: %.4f SC", contractPrice)));
    }
}