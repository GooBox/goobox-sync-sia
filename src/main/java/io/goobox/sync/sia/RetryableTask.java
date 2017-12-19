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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

@SuppressWarnings("SpellCheckingInspection")
public class RetryableTask implements Runnable {

    private static Logger logger = LogManager.getLogger();

    @NotNull
    private final Callable<Void> task;
    @NotNull
    private final RecoveryTask recover;

    public RetryableTask(@NotNull final Callable<Void> task, @NotNull final RecoveryTask recover) {
        this.task = task;
        this.recover = recover;
    }

    @Override
    public void run() {

        boolean retry = true;
        while (retry) {

            try {
                this.task.call();
                return;
            } catch (Exception e) {
                retry = this.recover.recover(e);
            }

        }
        logger.warn("Failed to recover {}", this.task.getClass().getName());

    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RetryableTask that = (RetryableTask) o;

        if (!task.equals(that.task)) return false;
        return recover.equals(that.recover);
    }

    @Override
    public int hashCode() {
        int result = task.hashCode();
        result = 31 * result + recover.hashCode();
        return result;
    }
}
