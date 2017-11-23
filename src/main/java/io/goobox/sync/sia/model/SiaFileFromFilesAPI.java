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

import java.math.BigDecimal;
import java.nio.file.Path;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import io.goobox.sync.sia.client.api.model.InlineResponse20011Files;

/**
 * SiaFileFromDownloadsAPI is a SiaFile which wraps a result of /renter/files.
 */
public class SiaFileFromFilesAPI implements SiaFile {

    /**
     * SiaPath object.
     */
    public final SiaPath siaPath;

    /**
     * File object returned by /renter/files.
     */
    public final InlineResponse20011Files rawFile;


    /**
     * Create a new SiaFile object with a path prefix.
     *
     * @param file
     * @param prefix
     */
    public SiaFileFromFilesAPI(final InlineResponse20011Files file, final Path prefix) {
        this.siaPath = new SiaPath(file.getSiapath(), prefix);
        this.rawFile = file;
    }

    @Override
    public String getName() {
        return this.siaPath.remotePath.toString();
    }

    @Override
    public Path getRemotePath() {
        return this.siaPath.siaPath;
    }

    @Override
    public Path getLocalPath() {
        return this.siaPath.localPath;
    }

    @Override
    public long getCreationTime() {
        return this.siaPath.created;
    }

    @Override
    public long getFileSize() {
        return this.rawFile.getFilesize();
    }

    public boolean getAvailable(){
        return this.rawFile.getAvailable();
    }

    public BigDecimal getUploadProgress(){
        return this.rawFile.getUploadprogress();
    }

    @Override
    public SiaPath getSiaPath() {
        return this.siaPath;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof SiaFileFromFilesAPI)) {
            return false;
        }
        final SiaFileFromFilesAPI c = (SiaFileFromFilesAPI) obj;
        return this.siaPath.equals(c.siaPath) && this.rawFile.equals(c.rawFile);
    }

    @Override
    public int hashCode() {
        return this.siaPath.hashCode() + this.rawFile.hashCode();
    }

}
