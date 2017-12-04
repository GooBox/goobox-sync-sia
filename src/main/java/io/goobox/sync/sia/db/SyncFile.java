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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.dizitart.no2.objects.Id;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@SuppressWarnings("WeakerAccess")
public class SyncFile implements Serializable {

    /**
     * Identifier of a file; remote path without prefix and time stamp.
     */
    @Id
    private String name;

    /**
     * Path to the cloud file. It can be null if the cloud file doesn't exist.
     * <p>
     * Note: this path must be a relative because it will be used in a request URL. However, serialize algorithm treats
     * this path as a absolute path with the current directory if this variable's class is Path.
     * Thus, it must be declared as a string and getter/setter convert.
     */
    @Nullable
    private String cloudPath;

    /**
     * File size of the cloud file.
     */
    @Nullable
    private Long cloudSize;

    /**
     * Path to the local file. It can be null if the local file doesn't exist.
     */
    @Nullable
    private Path localPath;

    /**
     * Last modification time of the local file.
     */
    @Nullable
    private Long localModificationTime;

    /**
     * File size of the local file.
     */
    @Nullable
    private Long localSize;

    /**
     * Hex string of sha512 digest of the local file body.
     * <p>
     * It is used to detect renaming files.
     */
    @Nullable
    private String localDigest;

    /**
     * Temporary path to store file during its download.
     */
    @Nullable
    private Path temporaryPath;

    /**
     * Sync status of this file.
     */
    private SyncState state;

    /**
     * Only classes in the same package can instantiation of SyncFile.
     */
    SyncFile() {
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public Optional<Path> getCloudPath() {
        if (this.cloudPath == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(Paths.get(this.cloudPath));
    }

    public Optional<Long> getCloudCreationTime() {
        try {
            return this.getCloudPath().map(cloudPath -> Long.valueOf(cloudPath.getFileName().toString()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public Optional<Long> getCloudSize() {
        return Optional.ofNullable(this.cloudSize);
    }

    public Optional<Long> getLocalModificationTime() {
        return Optional.ofNullable(this.localModificationTime);
    }

    public Optional<Path> getLocalPath() {
        return Optional.ofNullable(this.localPath);
    }

    public Optional<Long> getLocalSize() {
        return Optional.ofNullable(this.localSize);
    }

    public Optional<String> getLocalDigest() {
        return Optional.ofNullable(localDigest);
    }

    public Optional<Path> getTemporaryPath() {
        return Optional.ofNullable(this.temporaryPath);
    }

    @NotNull
    public SyncState getState() {
        return this.state;
    }

    void setName(@NotNull final String name) {
        this.name = name;
    }

    public void setCloudPath(@Nullable final Path cloudPath) {
        if (cloudPath != null) {
            this.cloudPath = cloudPath.toString();
        }
    }

    private void setCloudSize(@Nullable final Long cloudSize) {
        this.cloudSize = cloudSize;
    }

    public void setLocalPath(@Nullable final Path localPath) {
        this.localPath = localPath;
    }

    private void setLocalModificationTime(@Nullable final Long localModificationTime) {
        this.localModificationTime = localModificationTime;
    }

    private void setLocalSize(@Nullable final Long localSize) {
        this.localSize = localSize;
    }

    private void setLocalDigest(@Nullable String localDigest) {
        this.localDigest = localDigest;
    }

    void setTemporaryPath(@Nullable Path temporaryPath) {
        this.temporaryPath = temporaryPath;
    }

    /**
     * Sets new state. It must be called from DB.
     *
     * @param state to be set.
     */
    void setState(@NotNull final SyncState state) {
        this.state = state;
    }

    void setCloudData(@NotNull final CloudFile file) {
        this.setCloudPath(file.getCloudPath());
        this.setCloudSize(file.getFileSize());
    }

    void setLocalData(@NotNull final Path localPath) throws IOException {
        this.setLocalPath(localPath);
        this.setLocalModificationTime(Files.getLastModifiedTime(localPath).toMillis());
        this.setLocalSize(Files.size(localPath));
        this.setLocalDigest(DigestUtils.sha512Hex(new FileInputStream(localPath.toFile())));
    }

    @NotNull
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SyncFile syncFile = (SyncFile) o;

        if (!name.equals(syncFile.name)) return false;
        if (cloudPath != null ? !cloudPath.equals(syncFile.cloudPath) : syncFile.cloudPath != null) return false;
        if (cloudSize != null ? !cloudSize.equals(syncFile.cloudSize) : syncFile.cloudSize != null) return false;
        if (localPath != null ? !localPath.equals(syncFile.localPath) : syncFile.localPath != null) return false;
        if (localModificationTime != null ? !localModificationTime.equals(syncFile.localModificationTime) : syncFile.localModificationTime != null)
            return false;
        if (localSize != null ? !localSize.equals(syncFile.localSize) : syncFile.localSize != null) return false;
        if (localDigest != null ? !localDigest.equals(syncFile.localDigest) : syncFile.localDigest != null)
            return false;
        if (temporaryPath != null ? !temporaryPath.equals(syncFile.temporaryPath) : syncFile.temporaryPath != null)
            return false;
        return state == syncFile.state;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (cloudPath != null ? cloudPath.hashCode() : 0);
        result = 31 * result + (cloudSize != null ? cloudSize.hashCode() : 0);
        result = 31 * result + (localPath != null ? localPath.hashCode() : 0);
        result = 31 * result + (localModificationTime != null ? localModificationTime.hashCode() : 0);
        result = 31 * result + (localSize != null ? localSize.hashCode() : 0);
        result = 31 * result + (localDigest != null ? localDigest.hashCode() : 0);
        result = 31 * result + (temporaryPath != null ? temporaryPath.hashCode() : 0);
        result = 31 * result + state.hashCode();
        return result;
    }

}
