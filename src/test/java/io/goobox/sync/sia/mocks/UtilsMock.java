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

import io.goobox.sync.storj.Utils;
import mockit.Mock;
import mockit.MockUp;

import java.nio.file.Path;

public class UtilsMock extends MockUp<Utils> {

    public static Path syncDir;
    public static Path dataDir;

    @Mock
    public static Path getSyncDir() {
        return syncDir;
    }

    @Mock
    public static Path getDataDir() {
        return dataDir;
    }

}
