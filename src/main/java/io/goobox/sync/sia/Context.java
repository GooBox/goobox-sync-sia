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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Context manages config, api client, and a task queue.
 *
 * @author junpei
 */
public class Context {

    public final Config config;
    public final ApiClient apiClient;
    public final Path pathPrefix;

    /**
     * Create a new context with a config object, an API client, and a task queue.
     *
     * @param cfg       Config object
     * @param apiClient API client
     */
    public Context(final Config cfg, final ApiClient apiClient) {
        this.config = cfg;
        this.apiClient = apiClient;
        this.pathPrefix = Paths.get(this.config.getUserName(), "Goobox");
    }

}
