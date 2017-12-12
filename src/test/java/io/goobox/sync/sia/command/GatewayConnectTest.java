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

import io.goobox.sync.sia.App;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.GatewayApi;
import io.goobox.sync.sia.mocks.SystemMock;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.commons.cli.HelpFormatter;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class GatewayConnectTest {

    @Test
    public void run(@Mocked GatewayApi gateway) throws ApiException {

        final String addr = "123.456.789.012:9999";
        final Runnable cmd = new GatewayConnect(addr);

        new Expectations() {{
            gateway.gatewayConnectNetaddressPost(addr);
        }};

        cmd.run();

    }


    @Test
    public void withHelpOption(@Mocked HelpFormatter formatter) {

        new Expectations() {{
            formatter.printHelp(
                    String.format(GatewayConnect.HelpFormat, App.Name, GatewayConnect.CommandName),
                    GatewayConnect.Description, withNotNull(), "", true);
        }};
        GatewayConnect.main(new String[]{"-h"});
        GatewayConnect.main(new String[]{"--help"});

    }

    @Test
    public void withInvalidOption(@Mocked HelpFormatter formatter) {

        new SystemMock();
        new Expectations() {{
            formatter.printHelp(
                    String.format(GatewayConnect.HelpFormat, App.Name, GatewayConnect.CommandName),
                    GatewayConnect.Description, withNotNull(), "", true);
        }};
        GatewayConnect.main(new String[]{"--some-flag", "abcde"});
        assertEquals(1, SystemMock.statusCode);

    }

}