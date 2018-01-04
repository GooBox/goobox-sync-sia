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
import mockit.Deencapsulation;
import mockit.integration.junit4.JMockit;
import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

@RunWith(JMockit.class)
public class ConfigTest {

    private String userName;
    private String primarySeed;
    private Path syncDir;
    private Path dataDir;
    private int dataPieces;
    private int parityPieces;

    private Path tmpPath;

    @Before
    public void setUp() throws IOException {
        tmpPath = Files.createTempFile(null, null);
    }

    @After
    public void tearDown() {
        if (tmpPath != null && tmpPath.toFile().exists()) {
            assertTrue(tmpPath.toFile().delete());
        }
    }

    @Test
    public void load() throws IOException {

        this.userName = "testuser@sample.com";
        this.primarySeed = "a b c d e f g";
        this.syncDir = Paths.get("sync-dir");
        this.dataDir = Paths.get("data-dir");
        this.dataPieces = 5;
        this.parityPieces = 12;

        final File tmpFile = tmpPath.toFile();
        final BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile, true));
        writer.write(this.getPropertiesString());
        writer.flush();
        writer.close();

        final Config cfg = Config.load(tmpPath);
        assertEquals(userName, cfg.getUserName());
        assertEquals(primarySeed, cfg.getPrimarySeed());
        assertEquals(syncDir.toAbsolutePath(), cfg.getSyncDir());
        assertEquals(dataDir.toAbsolutePath(), cfg.getDataDir());
        assertEquals(dataPieces, cfg.getDataPieces());
        assertEquals(parityPieces, cfg.getParityPieces());

    }

    @Test
    public void loadWithInvalidParityPieces() throws IOException {

        this.userName = "testuser@sample.com";
        this.primarySeed = "a b c d e f g";
        this.dataPieces = 5;
        this.parityPieces = 1;

        final File tmpFile = tmpPath.toFile();
        final BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile, true));
        writer.write(this.getPropertiesString());
        writer.flush();
        writer.close();

        final Config cfg = Config.load(tmpPath);
        assertEquals(userName, cfg.getUserName());
        assertEquals(primarySeed, cfg.getPrimarySeed());
        assertEquals(dataPieces, cfg.getDataPieces());
        assertEquals(Config.DefaultParityPieces, cfg.getParityPieces());

    }

    @Test
    public void loadWithoutSyncFolderOption() throws IOException {

        this.userName = "testuser@sample.com";
        this.primarySeed = "a b c d e f g";
        this.syncDir = null;
        this.dataDir = null;
        this.dataPieces = 5;
        this.parityPieces = 12;

        final File tmpFile = tmpPath.toFile();
        final BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile, true));
        writer.write(this.getPropertiesString());
        writer.flush();
        writer.close();

        final Config cfg = Config.load(tmpPath);
        assertEquals(userName, cfg.getUserName());
        assertEquals(primarySeed, cfg.getPrimarySeed());
        assertEquals(Utils.getSyncDir().toAbsolutePath(), cfg.getSyncDir());
        assertEquals(Utils.getDataDir().toAbsolutePath(), cfg.getDataDir());
        assertEquals(dataPieces, cfg.getDataPieces());
        assertEquals(parityPieces, cfg.getParityPieces());

    }

    @Test
    public void save() throws IOException {

        final Path syncDir = Paths.get("sync-dir");
        final Path dataDir = Paths.get("data-dir");
        final Config cfg = new Config();
        cfg.setUserName("testuser@sample.com");
        cfg.setPrimarySeed("a b c d e f g");
        cfg.setDataPieces(5);
        cfg.setParityPieces(12);
        cfg.setSyncDir(syncDir);
        Deencapsulation.setField(cfg, "dataDir", dataDir);

        cfg.save(tmpPath);
        Deencapsulation.setField(cfg, "syncDir", syncDir.toAbsolutePath());
        Deencapsulation.setField(cfg, "dataDir", dataDir.toAbsolutePath());

        assertEquals(cfg, Config.load(tmpPath));

        // Absolute path has to be written.
        assumeFalse(SystemUtils.IS_OS_WINDOWS);
        assertTrue(Files.readAllLines(tmpPath).stream().anyMatch(line -> line.contains(syncDir.toAbsolutePath().toString())));
        assertTrue(Files.readAllLines(tmpPath).stream().anyMatch(line -> line.contains(dataDir.toAbsolutePath().toString())));

    }

    @Test
    public void overwrite() throws IOException {

        final Config cfg = new Config();
        cfg.setUserName("testuser@sample.com");
        cfg.setPrimarySeed("a b c d e f g");
        cfg.setDataPieces(5);
        cfg.setParityPieces(12);
        cfg.setSyncDir(Paths.get("sync-dir"));
        Deencapsulation.setField(cfg, "dataDir", Paths.get("data-dir").toAbsolutePath());

        final BufferedWriter writer = new BufferedWriter(new FileWriter(tmpPath.toFile(), true));
        writer.write("write random dummy data");
        writer.flush();
        writer.close();

        cfg.save(tmpPath);
        assertEquals(cfg, Config.load(tmpPath));

    }

    private String getPropertiesString() {

        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final PrintWriter writer = new PrintWriter(buf);

        writer.println(String.format("%s = %s", Config.UserName, userName));
        writer.println(String.format("%s = %s", Config.PrimarySeed, primarySeed));
        writer.println(String.format("%s = %s", Config.DataPieces, dataPieces));
        writer.println(String.format("%s = %s", Config.ParityPieces, parityPieces));

        if (syncDir != null) {
            writer.println(String.format("%s = %s", Config.SyncDir, syncDir));
        }

        if (dataDir != null) {
            writer.println(String.format("%s = %s", Config.DataDir, dataDir));
        }

        writer.flush();
        System.out.println(buf.toString());
        return buf.toString();

    }

    @Test
    public void setSyncDir() throws IOException {

        final Path syncDir = Paths.get("sync-dir");
        final Config cfg = new Config();
        cfg.setUserName("testuser@sample.com");
        cfg.setPrimarySeed("a b c d e f g");
        cfg.setDataPieces(5);
        cfg.setParityPieces(12);
        cfg.setSyncDir(syncDir);

        cfg.save(tmpPath);

        final Config res = Config.load(tmpPath);
        assertEquals(cfg, res);
        assertEquals(syncDir.toAbsolutePath(), res.getSyncDir());

    }

}
