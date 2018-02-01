/*
 * Copyright (C) 2018 Junpei Kawamoto
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
import io.goobox.sync.sia.App;
import io.goobox.sync.sia.db.DB;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifySyncStateTask implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(NotifySyncStateTask.class);

    public enum State {
        // Notify when consensus DB is synchronized and start Goobox's synchronization.
        startSynchronization,
        // Notify all files are synchronized.
        idle,
        // Notify some files are being synchronized.
        synchronizing
    }

    public class Args {
        @NotNull
        final State newState;

        Args(@NotNull final State state) {
            this.newState = state;
        }
    }

    public class Event {
        @SuppressWarnings("unused")
        final String method = "syncState";
        @NotNull
        final Args args;

        Event(@NotNull final State state) {
            this.args = new Args(state);
        }
    }

    private final Gson gson = new Gson();

    public NotifySyncStateTask() {
        App.getInstance().ifPresent(app -> app.getOverlayHelper().setSynchronizing());
        this.sendState(State.startSynchronization);
    }

    @Override
    public void run() {
        logger.trace("Enter run");

        if (DB.isSynced()) {
            App.getInstance().ifPresent(app -> app.getOverlayHelper().setOK());
            this.sendState(State.idle);
        } else {
            App.getInstance().ifPresent(app -> app.getOverlayHelper().setSynchronizing());
            this.sendState(State.synchronizing);
        }

    }

    private void sendState(final State e) {
        System.out.println(this.gson.toJson(new Event(e)));
    }

}
