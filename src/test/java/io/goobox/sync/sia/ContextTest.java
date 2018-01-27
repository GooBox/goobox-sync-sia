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

import io.goobox.sync.sia.client.ApiClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class ContextTest {

    private Path configPath;

    @Before
    public void setUp() throws IOException {
        this.configPath = Files.createTempFile(null, null);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() {
        this.configPath.toFile().delete();
    }

    @Test
    public void test() {

        final Config cfg = new Config(this.configPath.resolve(App.ConfigFileName));
        cfg.setUserName("test-user");
        final ApiClient cli = new ApiClient();

        final Context ctx = new Context(cfg, cli);
        assertEquals(ctx.getConfig(), cfg);
        assertEquals(ctx.getApiClient(), cli);
        assertEquals(ctx.getPathPrefix(), Paths.get(cfg.getUserName(), "Goobox"));

    }

    @Test
    public void getName() {

        final Path wd = Paths.get(".").toAbsolutePath();
        final Config cfg = new Config(this.configPath.resolve(App.ConfigFileName));
        cfg.setSyncDir(wd);

        final Context ctx = new Context(cfg, null);

        final Path name = Paths.get("sub-dir", "some-file");
        assertEquals(name.toString(), ctx.getName(wd.resolve(name)));

    }

    @Test
    public void getLocalPath() {

        final Path wd = Paths.get(".").toAbsolutePath();
        final Config cfg = new Config(this.configPath.resolve(App.ConfigFileName));
        cfg.setSyncDir(wd);

        final Context ctx = new Context(cfg, null);

        final Path name = Paths.get("sub-dir", "some-file");
        assertEquals(wd.resolve(name), ctx.getLocalPath(name.toString()));

    }

}
