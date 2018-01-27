/*
 * Copyright (C) 2017-2018 Junpei Kawamoto
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
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Context manages config, api client, and a task queue.
 *
 * @author junpei
 */
public class Context {

    @NotNull
    private final Config config;

    @Nullable
    private final ApiClient apiClient;

    @NotNull
    private final Path pathPrefix;

    /**
     * Create a new context with a config object, an API client, and a task queue.
     *
     * @param cfg       Config object
     * @param apiClient API client
     */
    public Context(@NotNull final Config cfg, @Nullable final ApiClient apiClient) {
        this.config = cfg;
        this.apiClient = apiClient;
        this.pathPrefix = Paths.get(this.config.getUserName(), "Goobox");
    }

    /**
     * Returns the file name from a given local path.
     *
     * @param localPath of the file
     * @return the name used in goobox for the given file.
     */
    @NotNull
    public String getName(@NotNull final Path localPath) {
        return this.config.getSyncDir().relativize(localPath).toString();
    }

    /**
     * Returns the local path for a given named file.
     *
     * @param name of the file.
     * @return local path to the file.
     */
    @NotNull
    public Path getLocalPath(@NotNull final String name) {
        return this.config.getSyncDir().resolve(name);
    }

    /**
     * Returns the config object in this context.
     *
     * @return the config object
     */
    @NotNull
    public Config getConfig() {
        return config;
    }

    /**
     * Returns the api client in this context.
     *
     * @return the client object.
     */
    @Nullable
    public ApiClient getApiClient() {
        return apiClient;
    }

    /**
     * Returns the prefix of cloud paths in this context.
     *
     * @return the path prefix.
     */
    @NotNull
    public Path getPathPrefix() {
        return pathPrefix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Context context = (Context) o;

        return config.equals(context.config) &&
                (apiClient != null ? apiClient.equals(context.apiClient) : context.apiClient == null) && pathPrefix.equals(context.pathPrefix);
    }

    @Override
    public int hashCode() {
        int result = config.hashCode();
        result = 31 * result + (apiClient != null ? apiClient.hashCode() : 0);
        result = 31 * result + pathPrefix.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }

}
