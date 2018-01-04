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

import io.goobox.sync.common.Utils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/**
 * Config defines information stored in a config file.
 *
 * @author junpei
 */
@SuppressWarnings("WeakerAccess")
public class Config {

    static final String UserName = "username";
    static final String PrimarySeed = "primary-seed";
    static final String SyncDir = "sync-folder";
    static final String DataDir = "data-folder";
    static final String DataPieces = "data-pieces";
    static final String ParityPieces = "parity-pieces";
    static final Logger logger = LogManager.getLogger();

    static final int DefaultDataPieces = 10;
    static final int DefaultParityPieces = 20;
    static final int MinimumParityPieces = 12;

    /**
     * User name.
     */
    private String userName;

    /**
     * Primary seed.
     */
    private String primarySeed;

    /**
     * Path to the directory where synchronising files are stored.
     */
    @NotNull
    private Path syncDir;

    /**
     * Path to the directory where SIA daemon data files are stored.
     */
    @NotNull
    private Path dataDir;

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

    public Config() {
        this.userName = "";
        this.primarySeed = "";
        this.syncDir = Utils.getSyncDir().toAbsolutePath();
        this.dataDir = Utils.getDataDir().toAbsolutePath();
    }

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

    public void setPrimarySeed(@NotNull String primarySeed) {
        this.primarySeed = primarySeed;
    }

    @NotNull
    public Path getSyncDir() {
        return syncDir;
    }

    void setSyncDir(@NotNull Path path) {
        this.syncDir = path.toAbsolutePath();
    }

    @NotNull
    public Path getDataDir() {
        return dataDir;
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

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Config config = (Config) o;

        if (dataPieces != config.dataPieces) return false;
        if (parityPieces != config.parityPieces) return false;
        if (userName != null ? !userName.equals(config.userName) : config.userName != null) return false;
        if (primarySeed != null ? !primarySeed.equals(config.primarySeed) : config.primarySeed != null) return false;
        if (!syncDir.equals(config.syncDir)) return false;
        return dataDir.equals(config.dataDir);
    }

    @Override
    public int hashCode() {
        int result = userName != null ? userName.hashCode() : 0;
        result = 31 * result + (primarySeed != null ? primarySeed.hashCode() : 0);
        result = 31 * result + syncDir.hashCode();
        result = 31 * result + dataDir.hashCode();
        result = 31 * result + dataPieces;
        result = 31 * result + parityPieces;
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
        props.setProperty(SyncDir, this.syncDir.toAbsolutePath().toString());
        props.setProperty(DataDir, this.dataDir.toAbsolutePath().toString());
        props.setProperty(DataPieces, String.valueOf(this.dataPieces));
        props.setProperty(ParityPieces, String.valueOf(this.parityPieces));

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
        if (!path.toFile().exists()) {
            throw new IOException(String.format("file %s doesn't exist", path));
        }

        final Properties props = new Properties();
        try (final InputStream in = Files.newInputStream(path)) {
            props.load(in);
        }

        final Config cfg = new Config();
        cfg.userName = props.getProperty(UserName, "");
        cfg.primarySeed = props.getProperty(PrimarySeed, "");

        if (props.getProperty(SyncDir) != null) {
            cfg.syncDir = Paths.get(props.getProperty(SyncDir)).toAbsolutePath();
        } else {
            cfg.syncDir = Utils.getSyncDir().toAbsolutePath();
        }

        if (props.getProperty(DataDir) != null) {
            cfg.dataDir = Paths.get(props.getProperty(DataDir)).toAbsolutePath();
        } else {
            cfg.dataDir = Utils.getDataDir().toAbsolutePath();
        }

        try {
            cfg.dataPieces = Integer.valueOf(props.getProperty(DataPieces, String.valueOf(DefaultDataPieces)));
        } catch (NumberFormatException e) {
            logger.warn("Invalid data pieces {}", props.getProperty(DataPieces));
            cfg.dataPieces = DefaultDataPieces;
        }

        try {
            cfg.parityPieces = Integer.valueOf(props.getProperty(ParityPieces, String.valueOf(DefaultParityPieces)));
            if (cfg.parityPieces < MinimumParityPieces) {
                logger.warn("Invalid parity pieces {}, minimum {} pieces are required", cfg.parityPieces, MinimumParityPieces);
                cfg.parityPieces = DefaultParityPieces;
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid parity pieces {}", props.getProperty(ParityPieces));
            cfg.parityPieces = DefaultParityPieces;
        }

        logger.info("Sync directory: {}", cfg.getSyncDir());
        logger.info(
                "Sync configuration: data pieces = {}, parity pieces = {}",
                cfg.dataPieces, cfg.parityPieces);
        return cfg;

    }

}
