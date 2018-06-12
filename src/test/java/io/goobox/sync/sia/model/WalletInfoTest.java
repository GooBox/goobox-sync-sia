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
import io.goobox.sync.sia.client.api.model.InlineResponse2008Settings;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WalletInfoTest {

    private final String address = "01234567890123456789";
    private final String primarySeed = "sample primary seed";
    private final double balance = 12345.02;
    private final double income = 10;
    private final double outcome = 15;
    private final double funds = 1234;
    private final long hosts = 30;
    private final long period = 6000;
    private final long renewWindow = 1000;
    private final long currentPeriod = 3000;
    private final double downloadSpending = 1.2345;
    private final double uploadSpending = 0.223;
    private final double storageSpending = 2.3;
    private final double contractSpending = 0.001;
    private WalletInfo walletInfo;

    @SuppressWarnings("SpellCheckingInspection")
    @Before
    public void setUp() {

        final InlineResponse20014 wallet = new InlineResponse20014();
        wallet.setConfirmedsiacoinbalance(APIUtils.toHasting(balance).toString());
        wallet.setUnconfirmedincomingsiacoins(APIUtils.toHasting(income).toString());
        wallet.setUnconfirmedoutgoingsiacoins(APIUtils.toHasting(outcome).toString());

        final InlineResponse2008 info = new InlineResponse2008();
        final InlineResponse2008Settings settings = new InlineResponse2008Settings();
        final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
        allowance.setFunds(APIUtils.toHasting(funds).toString());
        allowance.setHosts(hosts);
        allowance.setPeriod(period);
        allowance.setRenewwindow(renewWindow);
        settings.setAllowance(allowance);
        info.setSettings(settings);
        final InlineResponse2008Financialmetrics spending = new InlineResponse2008Financialmetrics();
        spending.setDownloadspending(APIUtils.toHasting(downloadSpending).toString());
        spending.setUploadspending(APIUtils.toHasting(uploadSpending).toString());
        spending.setStoragespending(APIUtils.toHasting(storageSpending).toString());
        spending.setContractspending(APIUtils.toHasting(contractSpending).toString());
        info.setFinancialmetrics(spending);
        info.setCurrentperiod(String.valueOf(currentPeriod));

        this.walletInfo = new WalletInfo(address, primarySeed, wallet, info);

    }

    @Test
    public void getAddress() {
        assertEquals(address, walletInfo.getAddress());
    }

    @Test
    public void getPrimarySeed() {
        assertEquals(primarySeed, walletInfo.getPrimarySeed());
    }

    @Test
    public void getBalance() {
        assertEquals(APIUtils.toHasting(balance), walletInfo.getBalance());
    }

    @Test
    public void getUnconfirmedDelta() {
        assertEquals(APIUtils.toHasting(income - outcome), walletInfo.getUnconfirmedDelta());
    }

    @Test
    public void getFunds() {
        assertEquals(APIUtils.toHasting(funds), walletInfo.getFunds());
    }

    @Test
    public void getHosts() {
        assertEquals(hosts, walletInfo.getHosts());
    }

    @Test
    public void getPeriod() {
        assertEquals(period, walletInfo.getPeriod());
    }

    @Test
    public void getRenewWindow() {
        assertEquals(renewWindow, walletInfo.getRenewWindow());
    }

    @Test
    public void getStartHeight() {
        assertEquals(currentPeriod, walletInfo.getStartHeight());
    }

    @Test
    public void getDownloadSpending() {
        assertEquals(APIUtils.toHasting(downloadSpending), walletInfo.getDownloadSpending());
    }

    @Test
    public void getUploadSpending() {
        assertEquals(APIUtils.toHasting(uploadSpending), walletInfo.getUploadSpending());
    }

    @Test
    public void getStorageSpending() {
        assertEquals(APIUtils.toHasting(storageSpending), walletInfo.getStorageSpending());
    }

    @Test
    public void getContractSpending() {
        assertEquals(APIUtils.toHasting(contractSpending), walletInfo.getContractSpending());
    }

    @Test
    public void getTotalSpending() {
        assertEquals(
                APIUtils.toHasting(downloadSpending)
                        .add(APIUtils.toHasting(uploadSpending))
                        .add(APIUtils.toHasting(storageSpending))
                        .add(APIUtils.toHasting(contractSpending)),
                walletInfo.getTotalSpending());
    }

    @Test
    public void dump() {

        final String outputs = walletInfo.toString();
        assertTrue(
                String.format("wallet address: %s", address),
                outputs.contains(String.format("wallet address: %s", address)));
        assertTrue(
                String.format("primary seed: %s", primarySeed),
                outputs.contains(String.format("primary seed: %s", primarySeed)));
        assertTrue(
                String.format("balance: %.4f SC", balance),
                outputs.contains(String.format("balance: %.4f SC", balance)));
        assertTrue(
                String.format("unconfirmed delta: %.4f SC", income - outcome),
                outputs.contains(String.format("unconfirmed delta: %.4f SC", income - outcome)));
        assertTrue(
                String.format("funds: %.4f SC", funds),
                outputs.contains(String.format("funds: %.4f SC", funds)));
        assertTrue(
                String.format("hosts: %d", hosts),
                outputs.contains(String.format("hosts: %d", hosts)));
        assertTrue(
                String.format("period: %d", period),
                outputs.contains(String.format("period: %d", period)));
        assertTrue(
                String.format("renew window: %d", renewWindow),
                outputs.contains(String.format("renew window: %d", renewWindow)));
        assertTrue(
                String.format("start height: %d", currentPeriod),
                outputs.contains(String.format("start height: %d", currentPeriod)));
        assertTrue(
                String.format("download: %.4f SC", downloadSpending),
                outputs.contains(String.format("download: %.4f SC", downloadSpending)));
        assertTrue(
                String.format("upload: %.4f SC", uploadSpending),
                outputs.contains(String.format("upload: %.4f SC", uploadSpending)));
        assertTrue(
                String.format("storage: %.4f SC", storageSpending),
                outputs.contains(String.format("storage: %.4f SC", storageSpending)));
        assertTrue(
                String.format("contract: %.4f SC", contractSpending),
                outputs.contains(String.format("contract: %.4f SC", contractSpending)));
    }
}