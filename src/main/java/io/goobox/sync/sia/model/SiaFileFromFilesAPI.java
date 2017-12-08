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

import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.api.model.InlineResponse20011Files;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

/**
 * SiaFileFromDownloadsAPI is a SiaFile which wraps a result of /renter/files.
 */
public class SiaFileFromFilesAPI extends AbstractSiaFile {

    /**
     * File object returned by /renter/files.
     */
    @NotNull
    private final InlineResponse20011Files rawFile;


    /**
     * Create a new SiaFile object with a path prefix.
     */
    public SiaFileFromFilesAPI(@NotNull final Context ctx, @NotNull final InlineResponse20011Files file) {
        super(ctx, file.getSiapath());
        this.rawFile = file;
    }

    @Override
    public long getFileSize() {
        return this.rawFile.getFilesize();
    }

    public boolean getAvailable() {
        return this.rawFile.getAvailable();
    }

    @NotNull
    public BigDecimal getUploadProgress() {
        return this.rawFile.getUploadprogress();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SiaFileFromFilesAPI that = (SiaFileFromFilesAPI) o;

        return rawFile.equals(that.rawFile);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + rawFile.hashCode();
        return result;
    }

}
