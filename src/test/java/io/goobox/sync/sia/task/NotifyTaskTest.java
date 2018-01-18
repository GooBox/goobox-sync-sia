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

import static io.goobox.sync.sia.task.NotifyTask.State;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
@RunWith(JMockit.class)
public class NotifyTaskTest {

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

        final NotifyTask task = new NotifyTask();
        System.err.println(this.out.toString());

        final Gson gson = new Gson();
        assertEquals(
                1,
                Arrays.stream(this.out.toString().split("\n"))
                        .map(line -> gson.fromJson(line, NotifyTask.Event.class))
                        .filter(e -> e.args.newState == State.startSynchronization)
                        .count()
        );
    }

    @Test
    public void notifyIdle(@Mocked DB db) {

        new Expectations() {{
            DB.isSynced();
            result = true;
        }};

        final NotifyTask task = new NotifyTask();
        task.run();
        System.err.println(this.out.toString());

        final Gson gson = new Gson();
        assertEquals(
                1,
                Arrays.stream(this.out.toString().split("\n"))
                        .map(line -> gson.fromJson(line, NotifyTask.Event.class))
                        .filter(e -> e.args.newState == State.idle)
                        .count()
        );

    }

    @Test
    public void notifySynchronizing(@Mocked DB db) {

        new Expectations() {{
            DB.isSynced();
            result = false;
        }};

        final NotifyTask task = new NotifyTask();
        task.run();
        System.err.println(this.out.toString());

        final Gson gson = new Gson();
        assertEquals(
                1,
                Arrays.stream(this.out.toString().split("\n"))
                        .map(line -> gson.fromJson(line, NotifyTask.Event.class))
                        .filter(e -> e.args.newState == State.synchronizing)
                        .count()
        );

    }


}