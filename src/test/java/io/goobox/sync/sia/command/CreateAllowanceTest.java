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

import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.WalletApi;
import io.goobox.sync.sia.client.api.model.InlineResponse20013;
import io.goobox.sync.sia.client.api.model.InlineResponse2008;
import io.goobox.sync.sia.client.api.model.InlineResponse2008Settings;
import io.goobox.sync.sia.client.api.model.InlineResponse2008SettingsAllowance;
import io.goobox.sync.sia.mocks.UtilsMock;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

public class CreateAllowanceTest {

    @Mocked
    private WalletApi wallet;

    @Mocked
    private RenterApi renter;

    private Path tempDir;

    /**
     * Creates a temporal directory and sets it as the result of Utils.syncDir().
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
    public void testWithoutFundOption() throws ApiException {

        final double balance = 12345.02;
        final double fund = 2234.85;

        new Expectations() {{

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setConfirmedsiacoinbalance(new BigDecimal(balance).multiply(Utils.Hasting).toString());
            res1.setUnlocked(true);
            wallet.walletGet();
            result = res1;

            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(new BigDecimal(fund).multiply(Utils.Hasting).toString());
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            settings.setAllowance(allowance);
            final InlineResponse2008 res2 = new InlineResponse2008();
            res2.setSettings(settings);
            renter.renterGet();
            result = res2;

            final BigDecimal newFund = new BigDecimal(balance).add(new BigDecimal(fund)).
                    multiply(Utils.Hasting).
                    setScale(0, BigDecimal.ROUND_DOWN);
            renter.renterPost(newFund.toString(), null, null, null);

        }};

        CreateAllowance.main(new String[]{});

    }

    @Test
    public void testWithFundOption() throws ApiException {

        final double param = 12345.02;
        final double fund = 2234.85;

        new Expectations() {{

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setUnlocked(true);
            wallet.walletGet();
            result = res1;

            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(new BigDecimal(fund).multiply(Utils.Hasting).toString());
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            settings.setAllowance(allowance);
            final InlineResponse2008 res2 = new InlineResponse2008();
            res2.setSettings(settings);
            renter.renterGet();
            result = res2;

            final BigDecimal newFund = new BigDecimal(param).add(new BigDecimal(fund)).
                    multiply(Utils.Hasting).
                    setScale(0, BigDecimal.ROUND_DOWN);
            renter.renterPost(newFund.toString(), null, null, null);

        }};

        CreateAllowance.main(new String[]{"--fund", new BigDecimal(param).multiply(Utils.Hasting).toString()});

    }

    @Test
    public void testWithLockedWalletWithoutFundOption() throws ApiException, IOException {

        final double balance = 12345.02;
        final double fund = 2234.85;

        final Config cfg = new Config();
        cfg.userName = "testuser@sample.com";
        cfg.primarySeed = "a b c d e f g";
        cfg.dataPieces = 5;
        cfg.parityPieces = 12;
        cfg.includeHiddenFiles = true;
        cfg.save(io.goobox.sync.storj.Utils.getDataDir().resolve(Utils.ConfigFileName));

        new Expectations() {{

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setConfirmedsiacoinbalance(new BigDecimal(balance).multiply(Utils.Hasting).toString());
            res1.setUnlocked(false);
            wallet.walletGet();
            result = res1;

            wallet.walletUnlockPost(cfg.primarySeed);

            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(new BigDecimal(fund).multiply(Utils.Hasting).toString());
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            settings.setAllowance(allowance);
            final InlineResponse2008 res2 = new InlineResponse2008();
            res2.setSettings(settings);
            renter.renterGet();
            result = res2;

            final BigDecimal newFund = new BigDecimal(balance).add(new BigDecimal(fund)).
                    multiply(Utils.Hasting).
                    setScale(0, BigDecimal.ROUND_DOWN);
            renter.renterPost(newFund.toString(), null, null, null);

        }};

        CreateAllowance.main(new String[]{});

    }

    @Test
    public void testWithLockedWalletAndFundOption() throws ApiException, IOException {

        final double param = 12345.02;
        final double fund = 2234.85;
        final Config cfg = new Config();
        cfg.userName = "testuser@sample.com";
        cfg.primarySeed = "a b c d e f g";
        cfg.dataPieces = 5;
        cfg.parityPieces = 12;
        cfg.includeHiddenFiles = true;
        cfg.save(io.goobox.sync.storj.Utils.getDataDir().resolve(Utils.ConfigFileName));

        new Expectations() {{

            final InlineResponse20013 res1 = new InlineResponse20013();
            res1.setUnlocked(false);
            wallet.walletGet();
            result = res1;

            wallet.walletUnlockPost(cfg.primarySeed);

            final InlineResponse2008SettingsAllowance allowance = new InlineResponse2008SettingsAllowance();
            allowance.setFunds(new BigDecimal(fund).multiply(Utils.Hasting).toString());
            final InlineResponse2008Settings settings = new InlineResponse2008Settings();
            settings.setAllowance(allowance);
            final InlineResponse2008 res2 = new InlineResponse2008();
            res2.setSettings(settings);
            renter.renterGet();
            result = res2;

            final BigDecimal newFund = new BigDecimal(param).add(new BigDecimal(fund)).
                    multiply(Utils.Hasting).
                    setScale(0, BigDecimal.ROUND_DOWN);
            renter.renterPost(newFund.toString(), null, null, null);

        }};

        CreateAllowance.main(new String[]{"--fund", new BigDecimal(param).multiply(Utils.Hasting).toString()});

    }


    @Test
    public void testWithInvalidOption(@Mocked HelpFormatter formatter, @Mocked System system) {

        new Expectations() {{
            formatter.printHelp(String.format("goobox-sync-sia %s", CreateAllowance.CommandName), withNotNull(), true);
            System.exit(1);
        }};
        CreateAllowance.main(new String[]{"--fund", "abcde"});

    }

}