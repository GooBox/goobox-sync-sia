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

import com.sun.istack.internal.NotNull;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/**
 * Config defines information stored in a config file.
 *
 * @author junpei
 */
public class Config {

    private static final String UserName = "username";
    private static final String PrimarySeed = "primary-seed";
    private static final String DataPieces = "data-pieces";
    private static final String ParityPieces = "parity-pieces";
    private static final String IncludeHiddenFiles = "include-hidden-files";
    private static final Logger logger = LogManager.getLogger();

    /**
     * user name.
     */
    private String userName;

    /**
     * primary seed.
     */
    private String primarySeed;

    /**
     * The number of data pieces to use when erasure coding the file.
     */
    private int dataPieces;

    /**
     * The number of parity pieces to use when erasure coding the file. Total
     * redundancy of the file is (datapieces+paritypieces)/datapieces. Minimum
     * required: 12
     */
    private int parityPieces;

    /**
     * if true, sync hidden files, too.
     */
    private boolean includeHiddenFiles;

    @NotNull
    public String getUserName() {
        return userName;
    }

    void setUserName(@NotNull String userName) {
        this.userName = userName;
    }

    @NotNull
    public String getPrimarySeed() {
        return primarySeed;
    }

    void setPrimarySeed(@NotNull String primarySeed) {
        this.primarySeed = primarySeed;
    }

    public int getDataPieces() {
        return dataPieces;
    }

    void setDataPieces(int dataPieces) {
        this.dataPieces = dataPieces;
    }

    public int getParityPieces() {
        return parityPieces;
    }

    void setParityPieces(int parityPieces) {
        this.parityPieces = parityPieces;
    }

    public boolean isIncludeHiddenFiles() {
        return includeHiddenFiles;
    }

    void setIncludeHiddenFiles(boolean includeHiddenFiles) {
        this.includeHiddenFiles = includeHiddenFiles;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Config config = (Config) o;

        if (dataPieces != config.dataPieces) return false;
        if (parityPieces != config.parityPieces) return false;
        if (includeHiddenFiles != config.includeHiddenFiles) return false;
        if (!userName.equals(config.userName)) return false;
        return primarySeed.equals(config.primarySeed);
    }

    @Override
    public int hashCode() {
        int result = userName.hashCode();
        result = 31 * result + primarySeed.hashCode();
        result = 31 * result + dataPieces;
        result = 31 * result + parityPieces;
        result = 31 * result + (includeHiddenFiles ? 1 : 0);
        return result;
    }

    /**
     * Save this configurations to the given file.
     *
     * @param path to the config file.
     * @throws IOException if failed to write a file.
     */
    public void save(@NotNull final Path path) throws IOException {

        final Properties props = new Properties();
        props.setProperty(UserName, this.userName);
        props.setProperty(PrimarySeed, this.primarySeed);
        props.setProperty(DataPieces, String.valueOf(this.dataPieces));
        props.setProperty(ParityPieces, String.valueOf(this.parityPieces));
        props.setProperty(IncludeHiddenFiles, String.valueOf(this.includeHiddenFiles));

        try (final BufferedWriter output = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            props.store(output, "");
        }

    }

    /**
     * Load configurations from the given file.
     *
     * @param path to the config file.
     * @return a Config object.
     * @throws IOException if failed to read a file.
     */
    public static Config load(@NotNull final Path path) throws IOException {

        logger.info("Loading config file {}", path);
        final Properties props = new Properties();
        try (final InputStream in = Files.newInputStream(path)) {
            props.load(in);
        }

        final Config cfg = new Config();
        cfg.userName = props.getProperty(UserName, "");
        cfg.primarySeed = props.getProperty(PrimarySeed, "");

        try {
            cfg.dataPieces = Integer.valueOf(props.getProperty(DataPieces, "1"));
        } catch (NumberFormatException e) {
            logger.warn("Invalid data pieces {}", props.getProperty(DataPieces));
            cfg.dataPieces = 1;
        }

        try {
            cfg.parityPieces = Integer.valueOf(props.getProperty(ParityPieces, "12"));
            if (cfg.parityPieces < 12) {
                logger.warn("Invalid parity pieces {}, minimum 12 pieces are required", cfg.parityPieces);
                cfg.parityPieces = 12;
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid parity pieces {}", props.getProperty(ParityPieces));
            cfg.parityPieces = 12;
        }

        cfg.includeHiddenFiles = Boolean.valueOf(props.getProperty(IncludeHiddenFiles, "false"));

        logger.info(
                "Sync configuration: data pieces = {}, parity pieces = {}, include hidden files = {}",
                cfg.dataPieces, cfg.parityPieces, cfg.includeHiddenFiles);
        return cfg;

    }

}
