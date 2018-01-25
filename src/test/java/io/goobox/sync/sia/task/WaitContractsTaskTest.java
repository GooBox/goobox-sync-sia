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

package io.goobox.sync.sia.task;

import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.ApiClient;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.RenterApi;
import io.goobox.sync.sia.client.api.model.InlineResponse2009;
import io.goobox.sync.sia.client.api.model.InlineResponse2009Contracts;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(JMockit.class)
public class WaitContractsTaskTest {

    @Mocked
    private RenterApi renter = new RenterApi();

    private Path configPath;
    private WaitContractsTask task;
    private List<InlineResponse2009Contracts> contracts;

    @Before
    public void setUp() throws IOException {

        this.configPath = Files.createTempFile(null, null);
        this.task = new WaitContractsTask(new Context(new Config(this.configPath), new ApiClient()));
        this.contracts = IntStream.range(0, App.MinContracts + 1).mapToObj(i -> {
            final InlineResponse2009Contracts c = new InlineResponse2009Contracts();
            c.setId(String.valueOf(i));
            c.setNetaddress("aaa-bbb-ccc");
            c.setRenterfunds("1234");
            return c;
        }).collect(Collectors.toList());

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() {
        this.configPath.toFile().delete();
    }

    @SuppressWarnings("unused")
    @Test
    public void testWaitContracts(@Mocked Thread thread) throws ApiException, InterruptedException {

        new Expectations() {{
            // First call: returns not enough contracts.
            final InlineResponse2009 res1 = new InlineResponse2009();
            res1.setContracts(contracts.subList(0, App.MinContracts / 2));

            // Second call: returns enough contracts.
            final InlineResponse2009 res2 = new InlineResponse2009();
            res2.setContracts(contracts);

            renter.renterContractsGet();
            returns(res1, res2);
            Thread.sleep(App.DefaultSleepTime);
        }};
        task.call();

    }

    @Test
    public void testWaitContractsWithoutSleep() throws ApiException {

        new Expectations() {{
            InlineResponse2009 res = new InlineResponse2009();
            res.setContracts(contracts);
            renter.renterContractsGet();
            result = res;
        }};
        task.call();

    }

}