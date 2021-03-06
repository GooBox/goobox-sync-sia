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

package io.goobox.sync.sia.mocks;

import io.goobox.sync.sia.db.DB;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import org.dizitart.no2.Nitrite;

public class DBMock extends MockUp<DB> {

    public static boolean committed;

    @SuppressWarnings("unused")
    @Mock
    private Nitrite open() {
        committed = false;
        return Nitrite.builder().compressed().openOrCreate();
    }

    @SuppressWarnings("unused")
    @Mock
    public synchronized static void commit(Invocation invocation) {
        committed = true;
        invocation.proceed();
    }
}
