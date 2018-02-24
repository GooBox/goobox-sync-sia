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

package io.goobox.sync.sia;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

public enum SyncState {

    // Notify when consensus DB is synchronized and start Goobox's synchronization.
    startSynchronization,
    // Notify all files are synchronized.
    idle,
    // Notify some files are being synchronized.
    synchronizing;

    public class Args {
        @NotNull
        final SyncState newState;

        Args(@NotNull final SyncState state) {
            this.newState = state;
        }
    }

    public class Event {
        @SuppressWarnings("unused")
        final String method = "syncState";
        @NotNull
        final SyncState.Args args;

        Event(@NotNull final SyncState state) {
            this.args = new SyncState.Args(state);
        }
    }

    private final Gson gson = new Gson();

    public String toJson() {
        return gson.toJson(new Event(this));
    }

}
