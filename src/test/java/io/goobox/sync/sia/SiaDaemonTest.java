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

import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import static org.junit.Assert.*;


@RunWith(JMockit.class)
public class SiaDaemonTest {

    private Path dataDir;
    private Path tempDir;
    private SiaDaemon daemon;

    @Before
    public void setUp() throws IOException {
        dataDir = Files.createTempDirectory(null);
        tempDir = Files.createTempDirectory(null);
        daemon = new SiaDaemon(dataDir);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(dataDir.toFile());
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    @Test
    public void getDaemonPath() {

        class Fixture {
            private final String wd;
            private final Path result;

            private Fixture(String wd, Path result) {
                this.wd = wd;
                this.result = result;
            }
        }

        final Fixture[] workingDirectories = {
                new Fixture("/Users/someuser/somewhere/", Paths.get("/Users/someuser/somewhere/sia/siad")),
                new Fixture("/Users/someuser/somewhere/bin", Paths.get("/Users/someuser/somewhere/sia/siad")),
        };

        for (Fixture fixture : workingDirectories) {
            new Expectations(SystemUtils.class) {{
                SystemUtils.getUserDir();
                result = fixture.wd;
            }};

            String cmd = fixture.result.toString();
            if (SystemUtils.IS_OS_WINDOWS) {
                cmd += ".exe";
            }
            assertEquals(cmd, daemon.getDaemonPath().toString());
        }

    }

    /**
     * checkAndDownloadConsensusDB checks {DataDIR}/consensus/consensus.db, and if not exists download it from
     * https://consensus.siahub.info/consensus.db with a check sum in https://consensus.siahub.info/sha256sum.txt
     */
    @Test
    public void checkAndDownloadConsensusDB() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        // Case 1: consensus.db doesn't exist.
        Path consensusDB = dataDir.resolve(Paths.get("consensus", "consensus.db"));
        assertFalse(consensusDB.toFile().exists());

        final List<String> dummyData = new ArrayList<>();
        dummyData.add("abcdefg");
        dummyData.add("012345");
        final Path dummyPath = Files.createTempFile(tempDir, null, null);
        Files.write(dummyPath, dummyData);

        final String checkSum = DigestUtils.sha256Hex(Files.readAllBytes(dummyPath));
        new Expectations(daemon) {{
            daemon.getCheckSum();
            result = Optional.of(checkSum);
        }};

        final URLConnection conn = dummyPath.toUri().toURL().openConnection();

        class URLMock extends MockUp<URL> {
            @SuppressWarnings("unused")
            @Mock
            public URLConnection openConnection() {
                return conn;
            }
        }
        new URLMock();

        new Expectations(conn) {{
            conn.setRequestProperty("User-Agent", SiaDaemon.DefaultUserAgent);
            conn.setRequestProperty("Accept-Encoding", "identity");
        }};

        assertTrue(daemon.checkAndDownloadConsensusDB());

        assertTrue(consensusDB.toFile().exists());
        final List<String> contents = Files.readAllLines(consensusDB);
        assertEquals(dummyData.size(), contents.size());
        for (int i = 0; i != dummyData.size(); ++i) {
            assertEquals(dummyData.get(i), contents.get(i));
        }

    }

    @Test
    public void checkAndDownloadConsensusDBWithExistingDBFile() throws IOException {

        // Case 2: consensus.db exists and the sile size is bigger than the threshold.
        Path consensusDB = dataDir.resolve(Paths.get("consensus", "consensus.db"));
        assertFalse(consensusDB.toFile().exists());
        Files.createDirectories(consensusDB.getParent());
        assertTrue(consensusDB.toFile().createNewFile());

        @SuppressWarnings("unused")
        class FileMock extends MockUp<File> {
            @Mock
            public long length() {
                return SiaDaemon.ConsensusDBThreshold * 2L;
            }
        }
        new FileMock();

        assertFalse(daemon.checkAndDownloadConsensusDB());

    }

    @Test
    public void checkAndDownloadConsensusDBOverwritesIncompleteDB() throws IOException {

        // Case 3: consensus.db exists but the file size is less than the threshold.
        Path consensusDB = dataDir.resolve(Paths.get("consensus", "consensus.db"));
        Files.createDirectories(consensusDB.getParent());
        assertTrue(consensusDB.toFile().createNewFile());

        final List<String> dummyData = new ArrayList<>();
        dummyData.add("abcdefg");
        dummyData.add("012345");
        final Path dummyPath = Files.createTempFile(tempDir, null, null);
        Files.write(dummyPath, dummyData);

        final String checkSum = DigestUtils.sha256Hex(Files.readAllBytes(dummyPath));
        new Expectations(daemon) {{
            daemon.getCheckSum();
            result = Optional.of(checkSum);
        }};

        final URLConnection conn = dummyPath.toUri().toURL().openConnection();

        class URLMock extends MockUp<URL> {
            @SuppressWarnings("unused")
            @Mock
            public URLConnection openConnection() {
                return conn;
            }
        }
        new URLMock();

        assertTrue(daemon.checkAndDownloadConsensusDB());

        assertTrue(consensusDB.toFile().exists());
        final List<String> contents = Files.readAllLines(consensusDB);
        assertEquals(dummyData.size(), contents.size());
        for (int i = 0; i != dummyData.size(); ++i) {
            assertEquals(dummyData.get(i), contents.get(i));
        }

    }

    @Test
    public void checkAndDownloadConsensusDBMismatchCheckSum() throws IOException {

        Path consensusDB = dataDir.resolve(Paths.get("consensus", "consensus.db"));
        Files.createDirectories(consensusDB.getParent());

        final List<String> dummyData = new ArrayList<>();
        dummyData.add("abcdefg");
        dummyData.add("012345");
        final Path dummyPath = Files.createTempFile(tempDir, null, null);
        Files.write(dummyPath, dummyData);

        new Expectations(daemon) {{
            daemon.getCheckSum();
            result = Optional.of("12345");
            times = SiaDaemon.MaxRetry;
        }};

        final URLConnection conn = dummyPath.toUri().toURL().openConnection();

        class URLMock extends MockUp<URL> {
            @SuppressWarnings("unused")
            @Mock
            public URLConnection openConnection() {
                return conn;
            }
        }
        new URLMock();

        new Expectations(conn) {{
            conn.setRequestProperty("User-Agent", SiaDaemon.DefaultUserAgent);
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.connect();
            conn.getInputStream();
            returns(
                    new FileInputStream(dummyPath.toFile()),
                    new FileInputStream(dummyPath.toFile()),
                    new FileInputStream(dummyPath.toFile()),
                    new FileInputStream(dummyPath.toFile()),
                    new FileInputStream(dummyPath.toFile())
            );
        }};

        assertFalse(daemon.checkAndDownloadConsensusDB());
        assertFalse(consensusDB.toFile().exists());

    }


    @SuppressWarnings("unused")
    @Test
    public void run(@Mocked ProcessBuilder builder, @Mocked Process proc) throws IOException {

        final Path daemonPath = Paths.get(System.getProperty("java.io.tmpdir"), "daemon");
        new Expectations(daemon) {{
            daemon.getDaemonPath();
            result = daemonPath;
        }};

        final PipedOutputStream out = new PipedOutputStream();
        final PipedInputStream in = new PipedInputStream(out);

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    out.write("test output".getBytes());
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    fail(e.getMessage());
                }
            }
        }, 3000);

        new Expectations() {{
            final ProcessBuilder cmd = new ProcessBuilder(daemonPath.toString(),
                    "--api-addr=127.0.0.1:9980",
                    "--host-addr=:9982",
                    "--rpc-addr=:9981",
                    String.format("--sia-directory=%s", dataDir),
                    "--modules=cgrtw");
            cmd.redirectErrorStream(true);
            cmd.start();
            result = proc;
            proc.getInputStream();
            result = in;
        }};

        daemon.run();

    }

    @Test
    public void runAndClose() throws IOException {

        final Path daemonPath = Paths.get(System.getProperty("java.io.tmpdir"), "daemon");
        new Expectations(daemon) {{
            daemon.getDaemonPath();
            result = daemonPath;
        }};

        final ProcessBuilder dummyProc = new ProcessBuilder("cat");
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                daemon.close();
            }
        }, 3000);

        new Expectations(ProcessBuilder.class) {{
            new ProcessBuilder(daemonPath.toString(),
                    "--api-addr=127.0.0.1:9980",
                    "--host-addr=:9982",
                    "--rpc-addr=:9981",
                    String.format("--sia-directory=%s", dataDir),
                    "--modules=cgrtw");
            result = dummyProc;
        }};

        daemon.run();
        assertTrue(daemon.isClosed());

    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Test
    public void getCheckSum() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {

        final String checkSum = "abcdefghijklmn";
        final List<String> testData = new ArrayList<>();
        testData.add("012345 another.db");
        testData.add(String.format("%s %s", checkSum, "consensus.db"));
        testData.add("012345 consensus.db");

        final Path dummyPath = Files.createTempFile(tempDir, null, null);
        Files.write(dummyPath, testData);

        final URLConnection conn = dummyPath.toUri().toURL().openConnection();

        @SuppressWarnings("unused")
        class URLMock extends MockUp<URL> {

            @Mock
            public void $init(String url) {
                assertEquals(SiaDaemon.CheckSumURL, url);
            }

            @Mock
            public URLConnection openConnection() {
                return conn;
            }
        }
        new URLMock();

        new Expectations(conn) {{
            conn.setRequestProperty("User-Agent", SiaDaemon.DefaultUserAgent);
            conn.setRequestProperty("Accept-Encoding", "identity");
        }};

        final Optional<String> res = this.daemon.getCheckSum();
        assertEquals(checkSum, res.get());

    }

}