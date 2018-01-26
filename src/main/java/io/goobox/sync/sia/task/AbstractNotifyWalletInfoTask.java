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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractNotifyWalletInfoTask implements Runnable {

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
        final EventType eventType;
        @Nullable
        final String message;

        Args(@NotNull final EventType eventType, @Nullable final String message) {
            this.eventType = eventType;
            this.message = message;
        }
    }

    static class Event {
        @SuppressWarnings("unused")
        final String method = "walletInfo";
        @NotNull
        final Args args;

        Event(@NotNull final EventType eventType, @Nullable final String message) {
            this.args = new Args(eventType, message);
        }

        Event(@NotNull final EventType eventType) {
            this(eventType, null);
        }
    }

    private final Gson gson = new Gson();

    void sendEvent(@NotNull final EventType eventType, @Nullable final String message) {
        System.out.println(this.gson.toJson(new Event(eventType, message)));
    }

    void sendEvent(@NotNull final EventType eventType) {
        this.sendEvent(eventType, null);
    }

}
