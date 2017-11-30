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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.dizitart.no2.objects.Id;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SyncFile implements Serializable {

    /**
     * Identifier of a file; remote path without prefix and time stamp.
     */
    @Id
    private String name;

    /**
     * Creation time of the remote file.
     */
    private long remoteCreatedTime;

    /**
     * File size of the remote file.
     */
    private long remoteSize;

    /**
     * Last modified time of the local file.
     */
    private long localModifiedTime;

    /**
     * File size of the local file.
     */
    private long localSize;

    /**
     * Hex string of sha512 digest of the local file body.
     * <p>
     * It is used to detect renaming files.
     */
    private String localDigest;

    /**
     * Temporary path to store file during its download.
     */
    private String temporaryPath;

    /**
     * Sync status of this file.
     */
    private SyncState state;

    /**
     * Only classes in the same package can instantiation of SyncFile.
     */
    SyncFile() {
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public long getRemoteCreatedTime() {
        return this.remoteCreatedTime;
    }

    void setRemoteCreatedTime(final long remoteCreatedTime) {
        this.remoteCreatedTime = remoteCreatedTime;
    }

    public long getRemoteSize() {
        return this.remoteSize;
    }

    void setRemoteSize(final long remoteSize) {
        this.remoteSize = remoteSize;
    }

    public long getLocalModifiedTime() {
        return this.localModifiedTime;
    }

    private void setLocalModifiedTime(final long localModifiedTime) {
        this.localModifiedTime = localModifiedTime;
    }

    public long getLocalSize() {
        return this.localSize;
    }

    private void setLocalSize(final long localSize) {
        this.localSize = localSize;
    }

    public String getLocalDigest() {
        return localDigest;
    }

    private void setLocalDigest(String localDigest) {
        this.localDigest = localDigest;
    }

    public Path getTemporaryPath() {
        return Paths.get(this.temporaryPath);
    }

    void setTemporaryPath(Path temporaryPath) {
        if (temporaryPath != null) {
            this.temporaryPath = temporaryPath.toString();
        }
    }

    public SyncState getState() {
        return this.state;
    }

    /**
     * Sets new state. It must be called from DB.
     *
     * @param state to be set.
     */
    void setState(final SyncState state) {
        this.state = state;
    }

    public void setCloudData(final SiaFile file) {
        this.setName(file.getName());
        this.setRemoteCreatedTime(file.getCreationTime());
        this.setRemoteSize(file.getFileSize());
    }

    public void setLocalData(Path path) throws IOException {
        this.setLocalModifiedTime(Files.getLastModifiedTime(path).toMillis());
        this.setLocalSize(Files.size(path));
        this.setLocalDigest(DigestUtils.sha512Hex(new FileInputStream(path.toFile())));
    }

    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }

}
