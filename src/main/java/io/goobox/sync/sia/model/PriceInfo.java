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
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Objects;

public class PriceInfo {

    @NotNull
    private final BigInteger download;
    @NotNull
    private final BigInteger upload;
    @NotNull
    private final BigInteger storage;
    @NotNull
    private final BigInteger contract;

    public PriceInfo(@NotNull final InlineResponse20013 prices) {
        this.download = new BigInteger(prices.getDownloadterabyte());
        this.upload = new BigInteger(prices.getUploadterabyte());
        this.storage = new BigInteger(prices.getStorageterabytemonth());
        this.contract = new BigInteger(prices.getFormcontracts());
    }

    @NotNull
    public BigInteger getDownload() {
        return download;
    }

    @NotNull
    public BigInteger getUpload() {
        return upload;
    }

    @NotNull
    public BigInteger getStorage() {
        return storage;
    }

    @NotNull
    public BigInteger getContract() {
        return contract;
    }

    @Override
    public String toString() {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (final PrintWriter writer = new PrintWriter(buffer)) {
            writer.println("current prices:");
            writer.println(String.format("  download: %s SC/TB", APIUtils.toSiacoin(this.getDownload())));
            writer.println(String.format("  upload: %s SC/TB", APIUtils.toSiacoin(this.getUpload())));
            writer.println(String.format("  storage: %s SC/TB*Month", APIUtils.toSiacoin(this.getStorage())));
            writer.print(String.format("  contract: %s SC/contract", APIUtils.toSiacoin(this.getContract())));
        }
        return buffer.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceInfo priceInfo = (PriceInfo) o;
        return Objects.equals(download, priceInfo.download) &&
                Objects.equals(upload, priceInfo.upload) &&
                Objects.equals(storage, priceInfo.storage) &&
                Objects.equals(contract, priceInfo.contract);
    }

    @Override
    public int hashCode() {
        return Objects.hash(download, upload, storage, contract);
    }
}
