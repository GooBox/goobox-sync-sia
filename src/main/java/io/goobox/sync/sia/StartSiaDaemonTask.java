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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.ConnectException;

public class StartSiaDaemonTask implements RecoveryTask {

    private static final Logger logger = LogManager.getLogger();

    @Override
    public boolean recover(Exception e) {

        if (e instanceof ApiException) {

            final ApiException apiException = (ApiException) e;
            if (apiException.getCause() instanceof ConnectException) {

                final App app = App.getInstance();
                if (app != null) {

                    int retry = 0;
                    while (true) {

                        app.startSiaDaemon();
                        try {

                            Thread.sleep(App.DefaultSleepTime);
                            app.prepareWallet();
                            app.waitSynchronization();
                            app.waitContracts();
                            return true;

                        } catch (ApiException e1) {

                            if (!(e1.getCause() instanceof ConnectException) || retry >= App.MaxRetry) {
                                logger.error("Failed to start SIA daemon");
                                return false;
                            }
                            retry++;

                        } catch (InterruptedException e1) {

                            logger.error("Interrupted while waiting SIA daemon is ready: {}", e1.getMessage());
                            return false;

                        }

                    }

                }

            }

        }

        return false;
    }

}
