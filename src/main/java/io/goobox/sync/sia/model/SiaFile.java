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

import java.nio.file.Path;

/**
 * SiaFile defines an API representing a file stored in SIA network.
 *
 * @author junpei
 */
public interface SiaFile {

    /**
     * Returns the name of this file.
     */
    public String getName();

    /**
     * Returns the path where this file located in the blockchain.
     */
    public Path getRemotePath();

    /**
     * Returns the path where this file located in a local computer.
     */
    public Path getLocalPath();

    /**
     * Returns the creation time of this file.
     */
    public long getCreationTime();

    /**
     * Returns the byte size of this file.
     */
    public long getFileSize();

    /**
     * Returns a SiaPath object.
     */
    public SiaPath getSiaPath();

}