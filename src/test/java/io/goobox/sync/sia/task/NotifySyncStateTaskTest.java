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
import io.goobox.sync.common.overlay.OverlayHelper;
import io.goobox.sync.sia.App;
import io.goobox.sync.sia.db.DB;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Optional;

import static io.goobox.sync.sia.task.NotifySyncStateTask.State;
import static org.junit.Assert.assertEquals;

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
@RunWith(JMockit.class)
public class NotifySyncStateTaskTest {

    @Mocked
    private App app;
    @Mocked
    private OverlayHelper overlayHelper;
    @Mocked
    private DB db;

    private ByteArrayOutputStream out;
    private PrintStream oldOut;

    @Before
    public void setUp() {
        this.out = new ByteArrayOutputStream();
        this.oldOut = System.out;
        System.setOut(new PrintStream(out));
    }

    @After
    public void tearDown() {
        System.setOut(this.oldOut);
    }

    @Test
    public void notifyStartingSynchronization() {

        new Expectations() {{
            App.getInstance();
            result = Optional.of(app);

            app.getOverlayHelper();
            result = overlayHelper;

            overlayHelper.setSynchronizing();
        }};

        final NotifySyncStateTask task = new NotifySyncStateTask();
        System.err.println(this.out.toString());

        final Gson gson = new Gson();
        assertEquals(
                1,
                Arrays.stream(this.out.toString().split("\n"))
                        .map(line -> gson.fromJson(line, NotifySyncStateTask.Event.class))
                        .filter(e -> e.args.newState == State.startSynchronization)
                        .count()
        );
    }

    @Test
    public void notifyIdle() {

        new Expectations() {{
            DB.isSynced();
            result = true;

            App.getInstance();
            result = Optional.of(app);
            times = 2;

            app.getOverlayHelper();
            result = overlayHelper;
            times = 2;

            overlayHelper.setSynchronizing();
            overlayHelper.setOK();
        }};

        final NotifySyncStateTask task = new NotifySyncStateTask();
        task.run();
        System.err.println(this.out.toString());

        final Gson gson = new Gson();
        assertEquals(
                1,
                Arrays.stream(this.out.toString().split("\n"))
                        .map(line -> gson.fromJson(line, NotifySyncStateTask.Event.class))
                        .filter(e -> e.args.newState == State.idle)
                        .count()
        );

    }

    @Test
    public void notifySynchronizing() {

        new Expectations() {{
            DB.isSynced();
            result = false;

            App.getInstance();
            result = Optional.of(app);
            times = 2;

            app.getOverlayHelper();
            result = overlayHelper;
            times = 2;

            overlayHelper.setSynchronizing();
            times = 2;
        }};

        final NotifySyncStateTask task = new NotifySyncStateTask();
        task.run();
        System.err.println(this.out.toString());

        final Gson gson = new Gson();
        assertEquals(
                1,
                Arrays.stream(this.out.toString().split("\n"))
                        .map(line -> gson.fromJson(line, NotifySyncStateTask.Event.class))
                        .filter(e -> e.args.newState == State.synchronizing)
                        .count()
        );

    }


}