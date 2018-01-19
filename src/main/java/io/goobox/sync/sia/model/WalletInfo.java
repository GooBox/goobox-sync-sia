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
import io.goobox.sync.sia.client.api.model.InlineResponse2008;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Financialmetrics;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;

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
    private final int hosts;
    private final long period;
    private final long renewWindow;
    private final long startHeight;
    @NotNull
    private final BigInteger downloadSpending;
    @NotNull
    private final BigInteger uploadSpending;
    @NotNull
    private final BigInteger storageSpending;
    @NotNull
    private final BigInteger contractSpending;

    @SuppressWarnings("SpellCheckingInspection")
    public WalletInfo(@NotNull String address, @NotNull String primarySeed, @NotNull InlineResponse20013 wallet, @NotNull InlineResponse2008 info) {
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
        this.downloadSpending = new BigInteger(spendings.getDownloadspending());
        this.uploadSpending = new BigInteger(spendings.getUploadspending());
        this.storageSpending = new BigInteger(spendings.getStoragespending());
        this.contractSpending = new BigInteger(spendings.getContractspending());
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

    public int getHosts() {
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
    public BigInteger getContractSpending() {
        return contractSpending;
    }

    @NotNull
    public BigInteger getTotalSpending() {
        return this.getDownloadSpending()
                .add(this.getUploadSpending())
                .add(this.getStorageSpending())
                .add(this.getContractSpending());
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
            writer.println(String.format("  download: %s SC", APIUtils.toSiacoin(this.getDownloadSpending())));
            writer.println(String.format("  upload: %s SC", APIUtils.toSiacoin(this.getUploadSpending())));
            writer.println(String.format("  storage: %s SC", APIUtils.toSiacoin(this.getStorageSpending())));
            writer.print(String.format("  contract: %s SC", APIUtils.toSiacoin(this.getContractSpending())));
        }
        return buffer.toString();

    }

}


