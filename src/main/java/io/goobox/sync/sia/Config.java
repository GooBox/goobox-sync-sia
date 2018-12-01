/*
 * Copyright (C) 2017-2018 Junpei Kawamoto
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
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Properties;

/**
 * Config defines information stored in a config file.
 *
 * @author junpei
 */
@SuppressWarnings("WeakerAccess")
public class Config {

    static final Logger logger = LoggerFactory.getLogger(Config.class);

    static final String UserName = "username";
    static final String PrimarySeed = "primary-seed";
    static final String SyncDir = "sync-folder";
    static final String DataDir = "data-folder";
    static final String DataPieces = "data-pieces";
    static final String ParityPieces = "parity-pieces";
    static final String DisableAutoAllocation = "disable-auto-allocation";
    @SuppressWarnings("SpellCheckingInspection")
    static final String SiadApiAddress = "siad-api-address";
    @SuppressWarnings("SpellCheckingInspection")
    static final String SiadGatewayAddress = "siad-gateway-address";
    static final String SiaApiPassword = "sia-api-password";

    static final int MinimumParityPieces = 12;
    static final String DefaultApiAddress = "127.0.0.1:9983";
    static final String DefaultGatewayAddress = ":9984";

    /**
     * Path to this config file.
     */
    @NotNull
    private final Path filePath;

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
     * Path to the directory where sia daemon data files are stored.
     */
    @NotNull
    private Path dataDir;

    /**
     * The number of data pieces to use when erasure coding the file.
     * According to https://github.com/NebulousLabs/Sia/blob/80cb4bdf63ba45227e62613694d553d09e95bc9f/node/api/renter.go#L536
     * it can be null.
     */
    @Nullable
    private Long dataPieces;

    /**
     * The number of parity pieces to use when erasure coding the file. Total
     * redundancy of the file is (datapieces+paritypieces)/datapieces. Minimum
     * required: 12
     * <p>
     * According to https://github.com/NebulousLabs/Sia/blob/80cb4bdf63ba45227e62613694d553d09e95bc9f/node/api/renter.go#L536
     * it can be null.
     */
    @Nullable
    private Long parityPieces;

    /**
     * If true, disable auto funds allocation;
     */
    private boolean disableAutoAllocation;

    /**
     * Which host:port the API server listens on.
     */
    @NotNull
    private String siadApiAddress;

    /**
     * Which port the gateway listens on.
     */
    @NotNull
    private String siadGatewayAddress;

    @NotNull
    private String siaApiPassword;

    /**
     * Create a config object associated with a given path.
     * <p>
     * Note that this constructor doesn't load the given file.
     *
     * @param filePath to be used to save the configuration.
     */
    public Config(@NotNull Path filePath) {
        this.filePath = filePath;
        this.userName = "";
        this.primarySeed = "";
        this.syncDir = Utils.getSyncDir().toAbsolutePath();
        this.dataDir = Utils.getDataDir().toAbsolutePath();
        this.dataPieces = null;
        this.parityPieces = null;
        this.disableAutoAllocation = false;
        this.siadApiAddress = DefaultApiAddress;
        this.siadGatewayAddress = DefaultGatewayAddress;
        this.siaApiPassword = RandomStringUtils.randomAlphabetic(32);
    }

    @NotNull
    public Path getFilePath() {
        return filePath;
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

    @Nullable
    public Long getDataPieces() {
        return dataPieces;
    }

    void setDataPieces(@Nullable Long dataPieces) {
        this.dataPieces = dataPieces;
    }

    @Nullable
    public Long getParityPieces() {
        return parityPieces;
    }

    void setParityPieces(@Nullable Long parityPieces) {
        this.parityPieces = parityPieces;
    }

    void setDisableAutoAllocation(boolean disableAutoAllocation) {
        this.disableAutoAllocation = disableAutoAllocation;
    }

    public boolean isDisableAutoAllocation() {
        return disableAutoAllocation;
    }

    @NotNull
    public String getSiadApiAddress() {
        return siadApiAddress;
    }

    public void setSiadApiAddress(@NotNull String siadApiAddress) {
        this.siadApiAddress = siadApiAddress;
    }

    @NotNull
    public String getSiadGatewayAddress() {
        return siadGatewayAddress;
    }

    public void setSiadGatewayAddress(@NotNull String siadGatewayAddress) {
        this.siadGatewayAddress = siadGatewayAddress;
    }

    @NotNull
    public String getSiaApiPassword() {
        return siaApiPassword;
    }

    public void setSiaApiPassword(@NotNull String siaApiPassword) {
        this.siaApiPassword = siaApiPassword;
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
        return disableAutoAllocation == config.disableAutoAllocation &&
                Objects.equals(filePath, config.filePath) &&
                Objects.equals(userName, config.userName) &&
                Objects.equals(primarySeed, config.primarySeed) &&
                Objects.equals(syncDir, config.syncDir) &&
                Objects.equals(dataDir, config.dataDir) &&
                Objects.equals(dataPieces, config.dataPieces) &&
                Objects.equals(parityPieces, config.parityPieces) &&
                Objects.equals(siadApiAddress, config.siadApiAddress) &&
                Objects.equals(siadGatewayAddress, config.siadGatewayAddress) &&
                Objects.equals(siaApiPassword, config.siaApiPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                filePath, userName, primarySeed, syncDir, dataDir, dataPieces,
                parityPieces, disableAutoAllocation, siadApiAddress, siadGatewayAddress, siaApiPassword);
    }

    /**
     * Save this configurations.
     *
     * @throws IOException if failed to write a file.
     */
    public void save() throws IOException {

        final Properties props = new Properties();
        if (!this.userName.isEmpty()) {
            props.setProperty(UserName, this.userName);
        }
        props.setProperty(PrimarySeed, this.primarySeed);
        props.setProperty(SyncDir, this.syncDir.toAbsolutePath().toString());
        props.setProperty(DataDir, this.dataDir.toAbsolutePath().toString());
        if (this.dataPieces != null) {
            props.setProperty(DataPieces, String.valueOf(this.dataPieces));
        }
        if (this.parityPieces != null) {
            props.setProperty(ParityPieces, String.valueOf(this.parityPieces));
        }
        props.setProperty(DisableAutoAllocation, String.valueOf(this.disableAutoAllocation));

        if (!this.siadApiAddress.equals(DefaultApiAddress)) {
            props.setProperty(SiadApiAddress, this.siadApiAddress);
        }
        if (!this.siadGatewayAddress.equals(DefaultGatewayAddress)) {
            props.setProperty(SiadGatewayAddress, this.siadGatewayAddress);
        }
        props.setProperty(SiaApiPassword, this.getSiaApiPassword());

        try (final BufferedWriter output = Files.newBufferedWriter(this.filePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            props.store(output, "");
        }

    }

    /**
     * Load configurations from the given file.
     *
     * @param filePath to the config file.
     * @return a Config object.
     * @throws IOException if failed to read a file.
     */
    public static Config load(@NotNull final Path filePath) throws IOException {

        logger.info("Loading config file {}", filePath);
        if (!filePath.toFile().exists()) {
            throw new IOException(String.format("file %s doesn't exist", filePath));
        }

        final Properties props = new Properties();
        try (final InputStream in = Files.newInputStream(filePath)) {
            props.load(in);
        }

        final Config cfg = new Config(filePath);
        cfg.setUserName(props.getProperty(UserName, ""));
        cfg.setPrimarySeed(props.getProperty(PrimarySeed, ""));

        if (props.getProperty(SyncDir) != null) {
            cfg.setSyncDir(Paths.get(props.getProperty(SyncDir)));
        }
        if (props.getProperty(DataDir) != null) {
            cfg.dataDir = Paths.get(props.getProperty(DataDir)).toAbsolutePath();
        }

        final String dataPieces = props.getProperty(DataPieces);
        if (dataPieces != null) {
            try {
                cfg.setDataPieces(Long.valueOf(dataPieces));
            } catch (final NumberFormatException e) {
                logger.warn("Invalid data pieces {}", dataPieces);
            }
        }
        final String parityPieces = props.getProperty(ParityPieces);
        try {
            final long value = Long.valueOf(parityPieces);
            if (value >= MinimumParityPieces) {
                cfg.setParityPieces(value);
            } else {
                logger.warn("Invalid parity pieces {}, minimum {} pieces are required", value, MinimumParityPieces);
            }
        } catch (final NumberFormatException e) {
            logger.warn("Invalid parity pieces {}", parityPieces);
        }
        if (cfg.getDataPieces() == null || cfg.getParityPieces() == null) {
            cfg.setDataPieces(null);
            cfg.setParityPieces(null);
        }

        cfg.setDisableAutoAllocation(Boolean.parseBoolean(props.getProperty(DisableAutoAllocation, "false")));

        final String apiAddress = props.getProperty(SiadApiAddress);
        if (apiAddress != null) {
            cfg.setSiadApiAddress(apiAddress);
        }
        final String gatewayAddress = props.getProperty(SiadGatewayAddress);
        if (gatewayAddress != null) {
            cfg.setSiadGatewayAddress(gatewayAddress);
        }

        final String siaApiPassword = props.getProperty(SiaApiPassword);
        if (siaApiPassword != null) {
            cfg.setSiaApiPassword(siaApiPassword);
        }

        logger.info("Sync directory: {}", cfg.getSyncDir());
        logger.info(
                "Sync configuration: data pieces = {}, parity pieces = {}",
                cfg.dataPieces, cfg.parityPieces);
        return cfg;

    }

}
