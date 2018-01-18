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
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;

public class PriceInfo {

    @NotNull
    private final BigDecimal download;
    @NotNull
    private final BigDecimal upload;
    @NotNull
    private final BigDecimal storage;
    @NotNull
    private final BigDecimal contract;

    public PriceInfo(@NotNull final InlineResponse20012 prices) {
        this.download = new BigDecimal(prices.getDownloadterabyte());
        this.upload = new BigDecimal(prices.getUploadterabyte());
        this.storage = new BigDecimal(prices.getStorageterabytemonth());
        this.contract = new BigDecimal(prices.getFormcontracts());
    }

    @NotNull
    public BigDecimal getDownload() {
        return download;
    }

    @NotNull
    public BigDecimal getUpload() {
        return upload;
    }

    @NotNull
    public BigDecimal getStorage() {
        return storage;
    }

    @NotNull
    public BigDecimal getContract() {
        return contract;
    }

    @Override
    public String toString() {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (final PrintWriter writer = new PrintWriter(buffer)) {
            writer.println("current prices:");
            writer.println(String.format("  download: %s SC/TB", APIUtils.toSC(this.getDownload())));
            writer.println(String.format("  upload: %s SC/TB", APIUtils.toSC(this.getUpload())));
            writer.println(String.format("  storage: %s SC/TB*Month", APIUtils.toSC(this.getStorage())));
            writer.println(String.format("  contract: %s SC/contract", APIUtils.toSC(this.getContract())));
        }
        return buffer.toString();
    }
}
