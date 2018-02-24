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
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.ConsensusApi;
import io.goobox.sync.sia.client.api.model.InlineResponse2006;
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

@RunWith(JMockit.class)
public class WaitSynchronizationTaskTest {

    @Mocked
    private ConsensusApi consensus = new ConsensusApi();

    private Path configPath;
    private WaitSynchronizationTask task;

    @Before
    public void setUp() throws IOException {
        this.configPath = Files.createTempFile(null, null);
        this.task = new WaitSynchronizationTask(new Context(new Config(this.configPath)));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @After
    public void tearDown() {
        this.configPath.toFile().delete();
    }

    @SuppressWarnings("unused")
    @Test
    public void testWaitSynchronization(@Mocked Thread thread) throws InterruptedException, ApiException {

        new Expectations() {{
            final InlineResponse2006 res1 = new InlineResponse2006();
            res1.setSynced(false);

            final InlineResponse2006 res2 = new InlineResponse2006();
            res2.setSynced(true);

            consensus.consensusGet();
            returns(res1, res2);
            Thread.sleep(App.DefaultSleepTime);

        }};
        task.call();

    }

    @Test
    public void testWaitSynchronizationWithoutSleep() throws ApiException {

        new Expectations() {{
            final InlineResponse2006 res = new InlineResponse2006();
            res.setSynced(true);
            consensus.consensusGet();
            result = res;
        }};
        task.call();

    }

}