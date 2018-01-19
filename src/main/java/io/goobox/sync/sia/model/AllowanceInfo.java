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
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;

public class AllowanceInfo {

    @NotNull
    private final BigInteger funds;
    private final int hosts;
    private final long period;
    private final long renewWindow;

    public AllowanceInfo(@NotNull final InlineResponse2008SettingsAllowance allowance) {
        this.funds = new BigInteger(allowance.getFunds());
        this.hosts = allowance.getHosts();
        this.period = allowance.getPeriod();
        this.renewWindow = allowance.getRenewwindow();
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

    @Override
    public String toString() {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (final PrintWriter writer = new PrintWriter(buffer)) {
            writer.println("allowance:");
            writer.println(String.format("  funds: %s SC", APIUtils.toSC(this.getFunds())));
            writer.println(String.format("  host: %d", this.getHosts()));
            writer.println(String.format("  period: %d blocks", this.getPeriod()));
            writer.println(String.format("  renew-window: %d blocks", this.getRenewWindow()));
        }
        return buffer.toString();
    }

}
