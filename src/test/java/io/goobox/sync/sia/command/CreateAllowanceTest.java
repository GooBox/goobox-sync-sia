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

package io.goobox.sync.sia.command;

import io.goobox.sync.common.Utils;
import io.goobox.sync.sia.APIUtils;
import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.SiaDaemon;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import io.goobox.sync.sia.mocks.SystemMock;
import io.goobox.sync.sia.mocks.UtilsMock;
import io.goobox.sync.sia.model.AllowanceInfo;
import io.goobox.sync.sia.task.CreateAllowanceTask;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class CreateAllowanceTest {

    @Mocked
    private CreateAllowanceTask createAllowance;

    private Path cfgPath;
    private Path tempDir;
    private AllowanceInfo allowanceInfo;

    @Before
    public void setUp() throws IOException {
        final double fund = 2234.85;
        final int host = 10;
        final long period = 1234;
        final long renewWindow = 5;

        final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
        allowance.setFunds(APIUtils.toHasting(fund).toString());
        allowance.setHosts(host);
        allowance.setPeriod(period);
        allowance.setRenewwindow(renewWindow);
        allowanceInfo = new AllowanceInfo(allowance);
    }

    /**
     * Creates a temporal directory and sets it as the result of CmdUtils.syncDir().
     *
     * @throws IOException if failed to create a temporary directory.
     */
    @Before
    public void setUpTempSyncDir() throws IOException {
        tempDir = Files.createTempDirectory(null);
        UtilsMock.dataDir = tempDir;
        new UtilsMock();

        cfgPath = tempDir.resolve(App.ConfigFileName);
    }

    /**
     * Deletes the temporary directory.
     *
     * @throws IOException if failed to delete it.
     */
    @After
    public void tearDownTempSyncDir() throws IOException {
        if (tempDir != null && tempDir.toFile().exists()) {
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }

    @Test
    public void withoutFundOption() throws ApiException {
        new Expectations(System.out) {{
            createAllowance = new CreateAllowanceTask((Context) any, null);
            result = createAllowance;

            createAllowance.call();
            result = allowanceInfo;

            System.out.println(allowanceInfo.toString());
        }};
        CreateAllowance.main(new String[]{});
    }

    @Test
    public void withFundOption() throws ApiException {
        final BigInteger fund = BigInteger.valueOf(1234567);
        new Expectations(System.out) {{
            createAllowance = new CreateAllowanceTask((Context) any, fund);
            result = createAllowance;

            createAllowance.call();
            result = allowanceInfo;

            System.out.println(allowanceInfo.toString());
        }};
        CreateAllowance.main(new String[]{"--fund", fund.toString()});
    }

    @Test
    public void withoutRunningSiaDaemon(@Mocked SiaDaemon daemon) throws ApiException, IOException {
        final Config cfg = new Config(cfgPath);
        Deencapsulation.setField(cfg, "dataDir", Utils.getDataDir());
        cfg.save();

        new Expectations(System.out) {{
            // Starting sia daemon.
            new SiaDaemon(cfg);
            result = daemon;
            daemon.start();

            createAllowance = new CreateAllowanceTask((Context) any, null);
            result = createAllowance;

            createAllowance.call();
            result = new ApiException(new ConnectException());
            result = allowanceInfo;

            System.out.println(allowanceInfo.toString());
        }};
        CreateAllowance.main(new String[]{});
    }

    @Test
    public void withHelpOption(@Mocked HelpFormatter formatter) {
        new Expectations() {{
            formatter.printHelp(
                    String.format("%s %s", App.Name, CreateAllowance.CommandName),
                    CreateAllowance.Description, withNotNull(), "", true);
        }};
        CreateAllowance.main(new String[]{"-h"});
        CreateAllowance.main(new String[]{"--help"});
    }

    @Test
    public void withInvalidOption(@Mocked HelpFormatter formatter) {
        new SystemMock();
        new Expectations() {{
            formatter.printHelp(
                    String.format("%s %s", App.Name, CreateAllowance.CommandName),
                    CreateAllowance.Description, withNotNull(), "", true);
        }};
        CreateAllowance.main(new String[]{"--fund", "abcde"});
        assertEquals(1, SystemMock.statusCode);
    }

}