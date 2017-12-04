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

package io.goobox.sync.sia.model;

import io.goobox.sync.common.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public abstract class AbstractSiaFile implements SiaFile {

    private static final Logger logger = LogManager.getLogger();

    @NotNull
    private final Path name;
    @NotNull
    private final Path cloudPath;
    @NotNull
    private final Path localPath;
    @Nullable
    private final Long creationTime;

    AbstractSiaFile(@NotNull final String cloudPath, @NotNull final Path prefix) {

        this.cloudPath = Paths.get(cloudPath);

        Path withoutTimestamp = this.cloudPath;
        Long created = null;
        if (this.cloudPath.getNameCount() - prefix.getNameCount() != 1) {
            try {
                created = Long.parseLong(this.cloudPath.getFileName().toString());
                withoutTimestamp = this.cloudPath.getParent();
            } catch (NumberFormatException e) {
                logger.debug("siapath {} doesn't have its creation time", cloudPath);
            }
        }
        this.creationTime = created;

        this.name = prefix.relativize(withoutTimestamp);
        this.localPath = Utils.getSyncDir().resolve(this.name);

    }

    @NotNull
    @Override
    public String getName() {
        return this.name.toString();
    }

    @NotNull
    @Override
    public Path getCloudPath() {
        return this.cloudPath;
    }

    @NotNull
    @Override
    public Path getLocalPath() {
        return this.localPath;
    }

    @Override
    public Optional<Long> getCreationTime() {
        return Optional.ofNullable(this.creationTime);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractSiaFile that = (AbstractSiaFile) o;

        if (!name.equals(that.name)) return false;
        if (!cloudPath.equals(that.cloudPath)) return false;
        if (!localPath.equals(that.localPath)) return false;
        return creationTime != null ? creationTime.equals(that.creationTime) : that.creationTime == null;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + cloudPath.hashCode();
        result = 31 * result + localPath.hashCode();
        result = 31 * result + (creationTime != null ? creationTime.hashCode() : 0);
        return result;
    }

}
