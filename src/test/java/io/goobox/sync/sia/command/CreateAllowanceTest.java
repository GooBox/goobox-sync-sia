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

package io.goobox.sync.sia.command;

import io.goobox.sync.common.Utils;
import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.SiaDaemon;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.WalletApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse2008;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Settings;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import io.goobox.sync.sia.mocks.SystemMock;
import io.goobox.sync.sia.mocks.UtilsMock;
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
import java.math.BigDecimal;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class CreateAllowanceTest {

    @SuppressWarnings("unused")
    @Mocked
    private WalletApi wallet;

    @SuppressWarnings("unused")
    @Mocked
    private RenterApi renter;

    private Path tempDir;

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

        final double balance = 12345.02;
        final double fund = 2234.85;

        new Expectations() {{

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setConfirmedsiacoinbalance(new BigDecimal(balance).multiply(CmdUtils.Hasting).toString());
            res1.setUnlocked(true);
            wallet.walletGet();
            result = res1;

            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(new BigDecimal(fund).multiply(CmdUtils.Hasting).toString());
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            settings.setAllowance(allowance);
            final InlineResponse2008 res2 = new InlineResponse2008();
            res2.setSettings(settings);
            renter.renterGet();
            result = res2;

            final BigDecimal newFund = new BigDecimal(balance).add(new BigDecimal(fund)).
                    multiply(CmdUtils.Hasting).
                    setScale(0, BigDecimal.ROUND_DOWN);
            renter.renterPost(newFund.toString(), null, null, null);

        }};

        CreateAllowance.main(new String[]{});

    }

    @Test
    public void withFundOption() throws ApiException {

        final double param = 12345.02;
        final double fund = 2234.85;

        new Expectations() {{

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setUnlocked(true);
            wallet.walletGet();
            result = res1;

            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(new BigDecimal(fund).multiply(CmdUtils.Hasting).toString());
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            settings.setAllowance(allowance);
            final InlineResponse2008 res2 = new InlineResponse2008();
            res2.setSettings(settings);
            renter.renterGet();
            result = res2;

            final BigDecimal newFund = new BigDecimal(param).add(new BigDecimal(fund)).
                    multiply(CmdUtils.Hasting).
                    setScale(0, BigDecimal.ROUND_DOWN);
            renter.renterPost(newFund.toString(), null, null, null);

        }};

        CreateAllowance.main(new String[]{"--fund", new BigDecimal(param).multiply(CmdUtils.Hasting).toString()});

    }

    @Test
    public void withLockedWalletWithoutFundOption() throws ApiException, IOException {

        final double balance = 12345.02;
        final double fund = 2234.85;

        final Config cfg = new Config();
        Deencapsulation.setField(cfg, "userName", "testuser@sample.com");
        Deencapsulation.setField(cfg, "primarySeed", "a b c d e f g");
        Deencapsulation.setField(cfg, "dataPieces", 5);
        Deencapsulation.setField(cfg, "parityPieces", 12);
        cfg.save(Utils.getDataDir().resolve(CmdUtils.ConfigFileName));

        new Expectations() {{

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setConfirmedsiacoinbalance(new BigDecimal(balance).multiply(CmdUtils.Hasting).toString());
            res1.setUnlocked(false);
            wallet.walletGet();
            result = res1;

            wallet.walletUnlockPost(cfg.getPrimarySeed());

            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(new BigDecimal(fund).multiply(CmdUtils.Hasting).toString());
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            settings.setAllowance(allowance);
            final InlineResponse2008 res2 = new InlineResponse2008();
            res2.setSettings(settings);
            renter.renterGet();
            result = res2;

            final BigDecimal newFund = new BigDecimal(balance).add(new BigDecimal(fund)).
                    multiply(CmdUtils.Hasting).
                    setScale(0, BigDecimal.ROUND_DOWN);
            renter.renterPost(newFund.toString(), null, null, null);

        }};

        CreateAllowance.main(new String[]{});

    }

    @Test
    public void withLockedWalletAndFundOption() throws ApiException, IOException {

        final double param = 12345.02;
        final double fund = 2234.85;
        final Config cfg = new Config();
        Deencapsulation.setField(cfg, "userName", "testuser@sample.com");
        Deencapsulation.setField(cfg, "primarySeed", "a b c d e f g");
        Deencapsulation.setField(cfg, "dataPieces", 5);
        Deencapsulation.setField(cfg, "parityPieces", 12);
        cfg.save(Utils.getDataDir().resolve(CmdUtils.ConfigFileName));

        new Expectations() {{

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setUnlocked(false);
            wallet.walletGet();
            result = res1;

            wallet.walletUnlockPost(cfg.getPrimarySeed());

            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(new BigDecimal(fund).multiply(CmdUtils.Hasting).toString());
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            settings.setAllowance(allowance);
            final InlineResponse2008 res2 = new InlineResponse2008();
            res2.setSettings(settings);
            renter.renterGet();
            result = res2;

            final BigDecimal newFund = new BigDecimal(param).add(new BigDecimal(fund)).
                    multiply(CmdUtils.Hasting).
                    setScale(0, BigDecimal.ROUND_DOWN);
            renter.renterPost(newFund.toString(), null, null, null);

        }};

        CreateAllowance.main(new String[]{"--fund", new BigDecimal(param).multiply(CmdUtils.Hasting).toString()});

    }

    @Test
    public void withoutRunningSiaDaemon(@Mocked SiaDaemon daemon) throws ApiException {

        final double balance = 12345.02;
        final double fund = 2234.85;

        new Expectations() {{

            // Starting SIA daemon.
            new SiaDaemon(Utils.getDataDir().resolve("sia"));
            result = daemon;
            daemon.start();

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setConfirmedsiacoinbalance(new BigDecimal(balance).multiply(CmdUtils.Hasting).toString());
            res1.setUnlocked(true);
            wallet.walletGet();
            result = new ApiException(new ConnectException());
            result = res1;

            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(new BigDecimal(fund).multiply(CmdUtils.Hasting).toString());
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            settings.setAllowance(allowance);
            final InlineResponse2008 res2 = new InlineResponse2008();
            res2.setSettings(settings);
            renter.renterGet();
            result = res2;

            final BigDecimal newFund = new BigDecimal(balance).add(new BigDecimal(fund)).
                    multiply(CmdUtils.Hasting).
                    setScale(0, BigDecimal.ROUND_DOWN);
            renter.renterPost(newFund.toString(), null, null, null);

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