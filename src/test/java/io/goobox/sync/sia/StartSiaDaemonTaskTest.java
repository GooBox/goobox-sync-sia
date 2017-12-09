/*
 * Copyright (C) 2017 Junpei Kawamoto
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

import io.goobox.sync.sia.client.ApiException;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ConnectException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class StartSiaDaemonTaskTest {

    @SuppressWarnings("unused")
    @Mocked
    private Thread thread;

    private AppMock appMock;


    @SuppressWarnings("unused")
    class AppMock extends MockUp<App> {
        boolean recoverable = true;
        int startingDaemon = 0;
        Throwable cause = new ConnectException("expected exception");

        @Mock
        App getInstance() {
            return new App();
        }

        @Mock
        void startSiaDaemon() {
            this.startingDaemon++;
        }

        @Mock
        void prepareWallet() throws ApiException {
            if (!recoverable) {
                throw new ApiException(cause);
            }

        }

        @Mock
        void waitSynchronization() {

        }

        @Mock
        void waitContracts() {

        }

    }

    @Before
    public void setUp() {
        appMock = new AppMock();
    }

    @Test
    public void recover() throws InterruptedException {

        new Expectations() {{
            Thread.sleep(App.DefaultSleepTime);
        }};
        final StartSiaDaemonTask task = new StartSiaDaemonTask();
        assertTrue(task.recover(new ApiException(new ConnectException())));
        assertEquals(1, appMock.startingDaemon);

    }

    @Test
    public void recoverFailed() throws InterruptedException {

        this.appMock.recoverable = false;
        new Expectations() {{
            Thread.sleep(App.DefaultSleepTime);
        }};
        final StartSiaDaemonTask task = new StartSiaDaemonTask();
        assertFalse(task.recover(new ApiException(new ConnectException())));
        assertEquals(App.MaxRetry + 1, appMock.startingDaemon);

    }

    @Test
    public void notConnectException() throws InterruptedException {

        final StartSiaDaemonTask task = new StartSiaDaemonTask();
        assertFalse(task.recover(new IOException()));
        assertEquals(0, appMock.startingDaemon);
    }

    @Test
    public void twoDifferentException() throws InterruptedException {

        this.appMock.recoverable = false;
        this.appMock.cause = new IOException();
        new Expectations() {{
            Thread.sleep(App.DefaultSleepTime);
        }};
        final StartSiaDaemonTask task = new StartSiaDaemonTask();
        assertFalse(task.recover(new ApiException(new ConnectException())));
        assertEquals(1, appMock.startingDaemon);

    }

}