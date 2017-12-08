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
package io.goobox.sync.sia.model;

import io.goobox.sync.common.Utils;
import io.goobox.sync.sia.Config;
import io.goobox.sync.sia.Context;
import io.goobox.sync.sia.client.api.model.InlineResponse20011Files;
import mockit.Deencapsulation;
import org.junit.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class SiaFileFromFilesAPITest {

    @SuppressWarnings("ConstantConditions")
    @Test
    public void test() {

        final String user = "test-user";
        final String name = "foo/bar.txt";
        final Long created = new Date().getTime();
        final Path remotePath = Paths.get(user, "Goobox", name, String.valueOf(created));

        final Config cfg = new Config();
        Deencapsulation.setField(cfg, "userName", user);
        final Context ctx = new Context(cfg, null);

        final long fileSize = 12345;
        final InlineResponse20011Files file = new InlineResponse20011Files();
        file.setSiapath(remotePath.toString());
        file.setFilesize(fileSize);
        file.setAvailable(false);
        file.setUploadprogress(new BigDecimal(24.5));
        final SiaFileFromFilesAPI siaFile = new SiaFileFromFilesAPI(ctx, file);

        assertEquals(name, siaFile.getName());
        assertEquals(remotePath, siaFile.getCloudPath());
        assertEquals(Paths.get(Utils.getSyncDir().toString(), name), siaFile.getLocalPath());
        assertEquals(created, siaFile.getCreationTime().get());
        assertEquals(fileSize, siaFile.getFileSize());

        assertEquals(false, siaFile.getAvailable());
        assertEquals(new BigDecimal(24.5), siaFile.getUploadProgress());
    }

}
