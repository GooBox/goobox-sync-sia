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
import java.math.BigDecimal;

public class WalletInfo {

    @NotNull
    private String address;
    @NotNull
    private String primarySeed;
    @NotNull
    private BigDecimal balance;
    @NotNull
    private BigDecimal unconfirmedDelta;
    @NotNull
    private BigDecimal funds;
    private int hosts;
    private long period;
    private long renewWindow;
    private long startHeight;
    @NotNull
    private BigDecimal downloadSpending;
    @NotNull
    private BigDecimal uploadSpending;
    @NotNull
    private BigDecimal storageSpending;
    @NotNull
    private BigDecimal contractSpending;

    @SuppressWarnings("SpellCheckingInspection")
    public WalletInfo(@NotNull String address, @NotNull String primarySeed, @NotNull InlineResponse20013 wallet, @NotNull InlineResponse2008 info) {
        this.address = address;
        this.primarySeed = primarySeed;
        this.balance = new BigDecimal(wallet.getConfirmedsiacoinbalance());
        this.unconfirmedDelta = new BigDecimal(wallet.getUnconfirmedincomingsiacoins())
                .subtract(new BigDecimal(wallet.getUnconfirmedoutgoingsiacoins()));

        final InlineResponse2008SettingsAllowance allowance = info.getSettings().getAllowance();
        this.funds = new BigDecimal(allowance.getFunds());
        this.hosts = allowance.getHosts();
        this.period = allowance.getPeriod();
        this.renewWindow = allowance.getRenewwindow();
        this.startHeight = Long.valueOf(info.getCurrentperiod());

        final InlineResponse2008Financialmetrics spendings = info.getFinancialmetrics();
        this.downloadSpending = new BigDecimal(spendings.getDownloadspending());
        this.uploadSpending = new BigDecimal(spendings.getUploadspending());
        this.storageSpending = new BigDecimal(spendings.getStoragespending());
        this.contractSpending = new BigDecimal(spendings.getContractspending());
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
    public BigDecimal getBalance() {
        return balance;
    }

    @NotNull
    public BigDecimal getUnconfirmedDelta() {
        return unconfirmedDelta;
    }

    @NotNull
    public BigDecimal getFunds() {
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
    public BigDecimal getDownloadSpending() {
        return downloadSpending;
    }

    @NotNull
    public BigDecimal getUploadSpending() {
        return uploadSpending;
    }

    @NotNull
    public BigDecimal getStorageSpending() {
        return storageSpending;
    }

    @NotNull
    public BigDecimal getContractSpending() {
        return contractSpending;
    }

    @Override
    public String toString() {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (final PrintWriter writer = new PrintWriter(buffer)) {

            writer.println(String.format("wallet address: %s", this.getAddress()));
            writer.println(String.format("primary seed: %s", this.getPrimarySeed()));

            writer.println(String.format("balance: %s SC", APIUtils.toSC(this.getBalance())));
            writer.println(String.format("unconfirmed delta: %s SC", APIUtils.toSC(this.getUnconfirmedDelta())));

            writer.println("allowance:");
            writer.println(String.format("  funds: %s SC", APIUtils.toSC(this.getFunds())));
            writer.println(String.format("  hosts: %d", this.getHosts()));
            writer.println(String.format("  period: %d", this.getPeriod()));
            writer.println(String.format("  renew window: %d", this.getRenewWindow()));
            writer.println(String.format("  start height: %s", this.getStartHeight()));

            writer.println("current spending:");
            writer.println(String.format("  download: %s SC", APIUtils.toSC(this.getDownloadSpending())));
            writer.println(String.format("  upload: %s SC", APIUtils.toSC(this.getUploadSpending())));
            writer.println(String.format("  storage: %s SC", APIUtils.toSC(this.getStorageSpending())));
            writer.println(String.format("  contract: %s SC", APIUtils.toSC(this.getContractSpending())));
        }
        return buffer.toString();

    }

}


