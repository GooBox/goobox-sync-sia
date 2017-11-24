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
package io.goobox.sync.sia.db;

import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.storj.db.SyncState;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.dizitart.no2.objects.Id;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

public class SyncFile implements Serializable {

    /**
     * Identifier of a file; remote path without prefix and time stamp.
     */
    @Id
    private String name;

    private long remoteCreatedTime;

    private long remoteSize;

    private long localModifiedTime;

    private long localSize;

    private SyncState state;

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public long getRemoteCreatedTime() {
        return this.remoteCreatedTime;
    }

    public void setRemoteCreatedTime(final long remoteCreatedTime) {
        this.remoteCreatedTime = remoteCreatedTime;
    }

    public long getRemoteSize() {
        return this.remoteSize;
    }

    public void setRemoteSize(final long remoteSize) {
        this.remoteSize = remoteSize;
    }

    public long getLocalModifiedTime() {
        return this.localModifiedTime;
    }

    public void setLocalModifiedTime(final long localModifiedTime) {
        this.localModifiedTime = localModifiedTime;
    }

    public long getLocalSize() {
        return this.localSize;
    }

    public void setLocalSize(final long localSize) {
        this.localSize = localSize;
    }

    public SyncState getState() {
        return this.state;
    }

    public void setState(final SyncState state) {
        this.state = state;
    }

    public void setCloudData(final SiaFile file) {
        this.setName(file.getName());
        this.setRemoteCreatedTime(file.getCreationTime());
        this.setRemoteSize(file.getFileSize());
    }

    public void setLocalData(Path path) throws IOException {
        setLocalModifiedTime(Files.getLastModifiedTime(path).toMillis());
        setLocalSize(Files.size(path));
    }

    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }

}
