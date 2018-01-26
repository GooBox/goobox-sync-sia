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
import io.goobox.sync.sia.task.GetWalletInfoTask;
import io.goobox.sync.sia.task.WaitContractsTask;
import io.goobox.sync.sia.task.WaitSynchronizationTask;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.ConnectException;
import java.util.Optional;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class StartSiaDaemonTaskTest {

    @SuppressWarnings("unused")
    @Mocked
    private Thread thread;

    @SuppressWarnings("unused")
    @Mocked
    private GetWalletInfoTask getWalletInfoTask;

    @SuppressWarnings("unused")
    @Mocked
    private WaitSynchronizationTask waitSynchronizationTask;

    @SuppressWarnings("unused")
    @Mocked
    private WaitContractsTask waitContractsTask;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void recover() throws InterruptedException, GetWalletInfoTask.WalletException, ApiException {

        final App app = new App();
        final Context ctx = app.getContext();
        new Expectations(App.class) {{

            Thread.sleep(App.DefaultSleepTime);

            App.getInstance();
            result = Optional.of(app);
            app.startSiaDaemon();

            app.getContext();
            result = ctx;

            final GetWalletInfoTask getWalletInfoTask = new GetWalletInfoTask(ctx);
            result = getWalletInfoTask;
            getWalletInfoTask.call();
            result = null;

            final WaitSynchronizationTask waitSynchronizationTask = new WaitSynchronizationTask(ctx);
            result = waitSynchronizationTask;
            waitSynchronizationTask.call();
            result = null;

            final WaitContractsTask waitContractsTask = new WaitContractsTask(ctx);
            result = waitContractsTask;
            waitContractsTask.call();
            result = null;

        }};
        final StartSiaDaemonTask task = new StartSiaDaemonTask();
        assertTrue(task.recover(new ApiException(new ConnectException())));

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void recoverFailedAfterMaxRetry() throws InterruptedException, GetWalletInfoTask.WalletException, ApiException {

        final App app = new App();
        final Context ctx = app.getContext();
        new Expectations(App.class) {{

            Thread.sleep(App.DefaultSleepTime);

            App.getInstance();
            result = Optional.of(app);
            app.startSiaDaemon();

            app.getContext();
            result = ctx;

            final GetWalletInfoTask getWalletInfoTask = new GetWalletInfoTask(ctx);
            result = getWalletInfoTask;
            getWalletInfoTask.call();
            result = new ApiException(new ConnectException());
            times = App.MaxRetry + 1;

        }};
        final StartSiaDaemonTask task = new StartSiaDaemonTask();
        assertFalse(task.recover(new ApiException(new ConnectException())));

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void recoverFailedWithApiException() throws InterruptedException, GetWalletInfoTask.WalletException, ApiException {

        final App app = new App();
        final Context ctx = app.getContext();
        new Expectations(App.class) {{

            Thread.sleep(App.DefaultSleepTime);

            App.getInstance();
            result = Optional.of(app);
            app.startSiaDaemon();

            app.getContext();
            result = ctx;

            final GetWalletInfoTask getWalletInfoTask = new GetWalletInfoTask(ctx);
            result = getWalletInfoTask;
            getWalletInfoTask.call();
            result = new ApiException();

        }};
        final StartSiaDaemonTask task = new StartSiaDaemonTask();
        assertFalse(task.recover(new ApiException(new ConnectException())));

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void recoverFailedWithWalletException() throws InterruptedException, GetWalletInfoTask.WalletException, ApiException {

        final App app = new App();
        final Context ctx = app.getContext();
        new Expectations(App.class) {{

            Thread.sleep(App.DefaultSleepTime);

            App.getInstance();
            result = Optional.of(app);
            app.startSiaDaemon();

            app.getContext();
            result = ctx;

            final GetWalletInfoTask getWalletInfoTask = new GetWalletInfoTask(ctx);
            result = getWalletInfoTask;
            getWalletInfoTask.call();
            result = new GetWalletInfoTask.WalletException("expected error");

        }};
        final StartSiaDaemonTask task = new StartSiaDaemonTask();
        assertFalse(task.recover(new ApiException(new ConnectException())));

    }

    @Test
    public void notConnectException() {

        final StartSiaDaemonTask task = new StartSiaDaemonTask();
        assertFalse(task.recover(new ApiException()));

    }

}