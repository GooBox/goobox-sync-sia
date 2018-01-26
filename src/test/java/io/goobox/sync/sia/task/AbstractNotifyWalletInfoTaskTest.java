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
import mockit.Expectations;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMockit.class)
public class AbstractNotifyWalletInfoTaskTest {

    private AbstractNotifyWalletInfoTask task;

    @Before
    public void setUp() {
        this.task = new AbstractNotifyWalletInfoTask() {
            @Override
            public void run() {
            }
        };
    }

    @Test
    public void sendEvent() {
        final AbstractNotifyWalletInfoTask.EventType eventType = AbstractNotifyWalletInfoTask.EventType.Error;
        final String message = "some message";

        final Gson gson = new Gson();
        new Expectations(System.out) {{
            System.out.println(gson.toJson(new AbstractNotifyWalletInfoTask.Event(eventType, message)));
        }};
        this.task.sendEvent(eventType, message);
    }

    @Test
    public void sendEventWithoutMessage() {
        final AbstractNotifyWalletInfoTask.EventType eventType = AbstractNotifyWalletInfoTask.EventType.Error;

        final Gson gson = new Gson();
        new Expectations(System.out) {{
            System.out.println(gson.toJson(new AbstractNotifyWalletInfoTask.Event(eventType)));
        }};
        this.task.sendEvent(eventType);
    }

}