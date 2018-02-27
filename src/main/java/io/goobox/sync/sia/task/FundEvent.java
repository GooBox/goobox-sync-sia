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

package io.goobox.sync.sia.task;

import com.google.gson.Gson;
import io.goobox.sync.sia.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FundEvent implements Event {

    public enum EventType {
        // Notify when the funds are 0.
        NoFunds,
        // Notify when the funds are not enough.
        InsufficientFunds,
        // Notify when the creating allowance successes.
        Allocated,
        // Notify when an error occurs.
        Error
    }

    static class Args {
        @NotNull
        final FundEvent.EventType eventType;
        @Nullable
        final String message;

        Args(@NotNull final FundEvent.EventType eventType, @Nullable final String message) {
            this.eventType = eventType;
            this.message = message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Args args = (Args) o;
            return eventType == args.eventType &&
                    Objects.equals(message, args.message);
        }

        @Override
        public int hashCode() {

            return Objects.hash(eventType, message);
        }
    }

    @SuppressWarnings("unused")
    final String method = "walletInfo";
    @NotNull
    final FundEvent.Args args;

    FundEvent(@NotNull final FundEvent.EventType eventType, @Nullable final String message) {
        this.args = new FundEvent.Args(eventType, message);
    }

    FundEvent(@NotNull final FundEvent.EventType eventType) {
        this(eventType, null);
    }

    @NotNull
    @Override
    public String toJson() {
        final Gson gson = new Gson();
        return gson.toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FundEvent fundEvent = (FundEvent) o;
        return Objects.equals(method, fundEvent.method) &&
                Objects.equals(args, fundEvent.args);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, args);
    }

}
