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
import io.goobox.sync.sia.client.api.model.InlineResponse20014;
import io.goobox.sync.sia.client.api.model.InlineResponse2008;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Financialmetrics;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class WalletInfo {

    @NotNull
    private final String address;
    @NotNull
    private final String primarySeed;
    @NotNull
    private final BigInteger balance;
    @NotNull
    private final BigInteger unconfirmedDelta;
    @NotNull
    private final BigInteger funds;
    private final long hosts;
    private final long period;
    private final long renewWindow;
    private final long startHeight;
    @NotNull
    private final BigInteger contractFees;
    @NotNull
    private final BigInteger downloadSpending;
    @NotNull
    private final BigInteger uploadSpending;
    @NotNull
    private final BigInteger storageSpending;
    @NotNull
    private final BigInteger totalAllocated;

    @SuppressWarnings("SpellCheckingInspection")
    public WalletInfo(@NotNull String address, @NotNull String primarySeed, @NotNull InlineResponse20014 wallet, @NotNull InlineResponse2008 info) {
        this.address = address;
        this.primarySeed = primarySeed;
        this.balance = new BigInteger(wallet.getConfirmedsiacoinbalance());
        this.unconfirmedDelta = new BigInteger(wallet.getUnconfirmedincomingsiacoins())
                .subtract(new BigInteger(wallet.getUnconfirmedoutgoingsiacoins()));

        final InlineResponse2008SettingsAllowance allowance = info.getSettings().getAllowance();
        this.funds = new BigInteger(allowance.getFunds());
        this.hosts = allowance.getHosts();
        this.period = allowance.getPeriod();
        this.renewWindow = allowance.getRenewwindow();
        this.startHeight = Long.valueOf(info.getCurrentperiod());

        final InlineResponse2008Financialmetrics spendings = info.getFinancialmetrics();
        this.contractFees = new BigInteger(spendings.getContractfees());
        this.downloadSpending = new BigInteger(spendings.getDownloadspending());
        this.uploadSpending = new BigInteger(spendings.getUploadspending());
        this.storageSpending = new BigInteger(spendings.getStoragespending());
        this.totalAllocated = new BigInteger(spendings.getTotalallocated());
    }

    @NotNull
    public String getAddress() {
        return address;
    }

    @NotNull
    public String getPrimarySeed() {
        return primarySeed;
    }

    @NotNull
    public BigInteger getBalance() {
        return balance;
    }

    @NotNull
    public BigInteger getUnconfirmedDelta() {
        return unconfirmedDelta;
    }

    @NotNull
    public BigInteger getFunds() {
        return funds;
    }

    public long getHosts() {
        return hosts;
    }

    public long getPeriod() {
        return period;
    }

    public long getRenewWindow() {
        return renewWindow;
    }

    public long getStartHeight() {
        return startHeight;
    }

    @NotNull
    public BigInteger getContractFees() {
        return contractFees;
    }

    @NotNull
    public BigInteger getDownloadSpending() {
        return downloadSpending;
    }

    @NotNull
    public BigInteger getUploadSpending() {
        return uploadSpending;
    }

    @NotNull
    public BigInteger getStorageSpending() {
        return storageSpending;
    }

    @NotNull
    public BigInteger getTotalAllocated() {
        return totalAllocated;
    }

    @NotNull
    public BigInteger getTotalSpending() {
        return this.getDownloadSpending()
                .add(this.getUploadSpending())
                .add(this.getStorageSpending())
                .add(this.getContractFees());
    }

    @Override
    public String toString() {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (final PrintWriter writer = new PrintWriter(buffer)) {

            writer.println(String.format("wallet address: %s", this.getAddress()));
            writer.println(String.format("primary seed: %s", this.getPrimarySeed()));

            writer.println(String.format("balance: %s SC", APIUtils.toSiacoin(this.getBalance())));
            writer.println(String.format("unconfirmed delta: %s SC", APIUtils.toSiacoin(this.getUnconfirmedDelta())));

            writer.println("allowance:");
            writer.println(String.format("  funds: %s SC", APIUtils.toSiacoin(this.getFunds())));
            writer.println(String.format("  hosts: %d", this.getHosts()));
            writer.println(String.format("  period: %d", this.getPeriod()));
            writer.println(String.format("  renew window: %d", this.getRenewWindow()));
            writer.println(String.format("  start height: %s", this.getStartHeight()));

            writer.println("current spending:");
            writer.println(String.format("  download: %.4f SC", APIUtils.toSiacoin(this.getDownloadSpending())));
            writer.println(String.format("  upload: %.4f SC", APIUtils.toSiacoin(this.getUploadSpending())));
            writer.println(String.format("  storage: %.4f SC", APIUtils.toSiacoin(this.getStorageSpending())));
            writer.print(String.format("  contract fees: %.4f SC", APIUtils.toSiacoin(this.getContractFees())));
        }
        return buffer.toString();

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WalletInfo that = (WalletInfo) o;
        return hosts == that.hosts &&
                period == that.period &&
                renewWindow == that.renewWindow &&
                startHeight == that.startHeight &&
                Objects.equals(address, that.address) &&
                Objects.equals(primarySeed, that.primarySeed) &&
                Objects.equals(balance, that.balance) &&
                Objects.equals(unconfirmedDelta, that.unconfirmedDelta) &&
                Objects.equals(funds, that.funds) &&
                Objects.equals(contractFees, that.contractFees) &&
                Objects.equals(downloadSpending, that.downloadSpending) &&
                Objects.equals(uploadSpending, that.uploadSpending) &&
                Objects.equals(storageSpending, that.storageSpending) &&
                Objects.equals(totalAllocated, that.totalAllocated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                address,
                primarySeed,
                balance,
                unconfirmedDelta,
                funds,
                hosts,
                period,
                renewWindow,
                startHeight,
                contractFees,
                downloadSpending,
                uploadSpending,
                storageSpending,
                totalAllocated);
    }
}


