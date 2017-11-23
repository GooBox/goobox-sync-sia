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

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

import io.goobox.sync.sia.client.ApiClient;

public class ContextTest {

	@Test
	public void test() {

		final Config cfg = new Config();
		cfg.userName = "test-user";
		final ApiClient cli = new ApiClient();

		final Context ctx = new Context(cfg, cli);
		assertEquals(ctx.config, cfg);
		assertEquals(ctx.apiClient, cli);
		assertEquals(ctx.pathPrefix, Paths.get(cfg.userName, "Goobox"));

	}

}
