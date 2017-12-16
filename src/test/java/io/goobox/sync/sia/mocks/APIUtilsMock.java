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

import io.goobox.sync.sia.APIUtils;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class APIUtilsMock extends MockUp<APIUtils> {

    public static final List<Path> toSlashPaths = new ArrayList<>();
    public static final List<String> fromSlashPaths = new ArrayList<>();

    @Mock
    public static String toSlash(Invocation invocation, Path path) {
        toSlashPaths.add(path);
        return invocation.proceed();
    }

    @Mock
    public static Path fromSlash(Invocation invocation, String path) {
        fromSlashPaths.add(path);
        return invocation.proceed();
    }

}
 