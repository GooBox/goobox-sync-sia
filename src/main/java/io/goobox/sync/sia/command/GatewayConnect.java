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
import io.goobox.sync.sia.APIUtils;
import io.goobox.sync.sia.App;
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.client.ApiClient;
import io.goobox.sync.sia.client.ApiException;
import io.goobox.sync.sia.client.api.GatewayApi;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class GatewayConnect implements Runnable {

    public static final String CommandName = "gateway-connect";
    public static final String Description = "Connects the gateway to a peer";
    static final String HelpFormat = "%s %s addr";
    private static final Logger logger = LoggerFactory.getLogger(GatewayConnect.class);

    private final Config cfg;
    private final String addr;

    public static void main(String[] args) {

        final Options opts = new Options();
        opts.addOption("h", "help", false, "show this help");

        try {

            final CommandLine cmd = new DefaultParser().parse(opts, args);
            if (cmd.hasOption("h")) {
                final HelpFormatter help = new HelpFormatter();
                help.printHelp(String.format(HelpFormat, App.Name, CommandName), Description, opts, "", true);
                return;
            }

            // Run this command.
            cmd.getArgList().forEach(addr -> new GatewayConnect(addr).run());

        } catch (final ParseException e) {
            logger.error("Failed to parse command line options: {}", e.getMessage());

            final HelpFormatter help = new HelpFormatter();
            help.printHelp(String.format(HelpFormat, App.Name, CommandName), Description, opts, "", true);
            System.exit(1);
            return;

        }

    }

    GatewayConnect(final String addr) {
        final Path configPath = Utils.getDataDir().resolve(App.ConfigFileName);
        this.cfg = CmdUtils.loadConfig(configPath);
        this.addr = addr;
    }


    @Override
    public void run() {

        logger.info("Connect peer {}", this.addr);
        final ApiClient apiClient = CmdUtils.getApiClient();
        final GatewayApi gateway = new GatewayApi(apiClient);
        try {
            gateway.gatewayConnectNetaddressPost(addr);
        } catch (final ApiException e) {
            logger.error("Failed to connect peer {}: {}", addr, APIUtils.getErrorMessage(e));
        }

    }
}
