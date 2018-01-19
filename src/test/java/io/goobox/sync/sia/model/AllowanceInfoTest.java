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
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class AllowanceInfoTest {

    private final double funds = 2234.85;
    private final int hosts = 10;
    private final long period = 1234;
    private final long renewWindow = 5;
    private AllowanceInfo allowanceInfo;

    @Before
    public void setUp() {
        final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
        allowance.setFunds(APIUtils.toHasting(funds).toString());
        allowance.setHosts(hosts);
        allowance.setPeriod(period);
        allowance.setRenewwindow(renewWindow);
        this.allowanceInfo = new AllowanceInfo(allowance);
    }

    @Test
    public void getFunds() {
        assertEquals(APIUtils.toHasting(funds), allowanceInfo.getFunds());
    }

    @Test
    public void getHosts() {
        assertEquals(hosts, allowanceInfo.getHosts());
    }

    @Test
    public void getPeriod() {
        assertEquals(period, allowanceInfo.getPeriod());
    }

    @Test
    public void getRenewWindow() {
        assertEquals(renewWindow, allowanceInfo.getRenewWindow());
    }

    @Test
    public void dump() {
        final String output = allowanceInfo.toString();
        System.err.println(output);
        assertTrue(
                String.format("funds: %.4f SC", funds),
                output.contains(String.format("funds: %.4f SC", funds)));
        assertTrue(
                String.format("host: %d", hosts),
                output.contains(String.format("host: %d", hosts)));
        assertTrue(
                String.format("period: %d blocks", period),
                output.contains(String.format("period: %d blocks", period)));
        assertTrue(
                String.format("renew-window: %d blocks", renewWindow),
                output.contains(String.format("renew-window: %d blocks", renewWindow)));
    }

}