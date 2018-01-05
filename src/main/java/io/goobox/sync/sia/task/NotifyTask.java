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
import io.goobox.sync.sia.db.DB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NotifyTask implements Runnable {

    private static Logger logger = LogManager.getLogger();

    public enum EventType {
        // Notify when consensus DB is synchronized and start Goobox's synchronization.
        StartSynchronization,
        // Notify all files are synchronized.
        Synchronized,
        // Notify some files are being synchronized.
        Synchronizing
    }

    public class Schema {
        EventType eventType;
        String message;

        public Schema(final EventType type, String message) {
            this.eventType = type;
            this.message = message;
        }

        public Schema(final EventType type) {
            this(type, "");
        }

    }

    private final Gson gson = new Gson();

    public NotifyTask() {
        System.out.println(this.gson.toJson(new Schema(EventType.StartSynchronization)));
    }

    @Override
    public void run() {
        logger.traceEntry();

        EventType e;
        if (DB.isSynced()) {
            e = EventType.Synchronized;
        } else {
            e = EventType.Synchronizing;
        }
        System.out.println(this.gson.toJson(new Schema(e)));

    }

}
