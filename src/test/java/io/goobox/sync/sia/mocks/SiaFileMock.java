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

import io.goobox.sync.sia.model.SiaFile;
import io.goobox.sync.sia.model.SiaPath;
import io.goobox.sync.storj.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SiaFileMock implements SiaFile as a simple structure.
 */
public class SiaFileMock implements SiaFile {

    private String name;
    private Path remotePath;
    private Path localPath;
    private long creationTime;
    private long fileSize;
    private SiaPath siaPath;

    public SiaFileMock(final Path localPath) {

        System.out.println(Utils.getSyncDir());
        System.out.println(localPath);

        this.name = Utils.getSyncDir().relativize(localPath).toString();
        this.localPath = localPath;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Path getRemotePath() {
        return this.remotePath;
    }

    @Override
    public Path getLocalPath() {
        return this.localPath;
    }

    @Override
    public long getCreationTime() {
        return this.creationTime;
    }

    @Override
    public long getFileSize() {
        return this.fileSize;
    }

    @Override
    public SiaPath getSiaPath() {
        return this.siaPath;
    }

    public void setRemotePath(Path remotePath) {
        this.remotePath = remotePath;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setSiaPath(SiaPath siaPath) {
        this.siaPath = siaPath;
    }

}
