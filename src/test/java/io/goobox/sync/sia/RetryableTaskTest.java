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

import org.junit.Test;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RetryableTaskTest {

    private final Exception exception = new Exception("expected exception");

    private class TestTask implements Callable<Void> {
        int counter;
        boolean success;

        @Override
        public Void call() throws Exception {
            if (counter++ == 0) {
                throw exception;
            }
            success = true;
            return null;
        }

    }

    @Test
    public void noException() {

        final Runnable task = new RetryableTask(() -> {
            // Do nothing.
            return null;
        }, e -> {
            fail("unexpected exception");
            return false;
        });
        task.run();

    }

    @Test
    public void recover() {

        final TestTask task = new TestTask();
        new RetryableTask(task, e -> {
            assertEquals(exception, e);
            return true;
        }).run();

        assertEquals(2, task.counter);
        assertTrue(task.success);

    }

    @Test
    public void recoverFailed() {

        final TestTask task = new TestTask();
        new RetryableTask(task, e -> {
            assertEquals(exception, e);
            return false;
        }).run();

        assertEquals(1, task.counter);
        assertFalse(task.success);


    }

}