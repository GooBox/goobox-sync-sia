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

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class ConfigTest {

	String userName;
	String primarySeed;
	int dataPieces;
	int parityPieces;
	
	@Test
	public void testLoad() throws IOException {

		this.userName = "testuser@sample.com";
		this.primarySeed = "a b c d e f g";
		this.dataPieces = 5;
		this.parityPieces = 12;

		File tmpFile = null;
		try {
		
			Path tmpPath = Files.createTempFile(null, null);
			tmpFile = tmpPath.toFile();
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile, true));
			writer.write(this.getPropertiesString());
			writer.flush();
			writer.close();
			
			Config cfg = Config.load(tmpPath);
			assertEquals(cfg.userName, userName);
			assertEquals(cfg.primarySeed, primarySeed);
			assertEquals(cfg.dataPieces, dataPieces);
			assertEquals(cfg.parityPieces, parityPieces);

		} finally {
			if(tmpFile != null && tmpFile.exists()) {
				tmpFile.delete();
			}
		}
		
	}
	
	@Test
	public void testLoadWithInvalidParityPieces() throws IOException {

		this.userName = "testuser@sample.com";
		this.primarySeed = "a b c d e f g";
		this.dataPieces = 5;
		this.parityPieces = 1;

		File tmpFile = null;
		try {
		
			Path tmpPath = Files.createTempFile(null, null);
			tmpFile = tmpPath.toFile();
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile, true));
			writer.write(this.getPropertiesString());
			writer.flush();
			writer.close();
			
			Config cfg = Config.load(tmpPath);
			assertEquals(cfg.userName, userName);
			assertEquals(cfg.primarySeed, primarySeed);
			assertEquals(cfg.dataPieces, dataPieces);
			assertEquals(cfg.parityPieces, 12);

		} finally {
			if(tmpFile != null && tmpFile.exists()) {
				tmpFile.delete();
			}
		}
	
	
	}
	
	@Test
	public void testSave() throws IOException {
		
		Config cfg = new Config();
		cfg.userName = "testuser@sample.com";
		cfg.primarySeed = "a b c d e f g";
		cfg.dataPieces = 5;
		cfg.parityPieces = 12;
		
		Path tmpPath = null;
		try {
		
			tmpPath = Files.createTempFile(null, null);
			cfg.save(tmpPath);
			
			Config cfg2 = Config.load(tmpPath);
			assertEquals(cfg2.userName, cfg.userName);
			assertEquals(cfg2.primarySeed, cfg.primarySeed);
			assertEquals(cfg2.dataPieces, cfg.dataPieces);
			assertEquals(cfg2.parityPieces, cfg.parityPieces);
		
		} finally {
			if(tmpPath != null && tmpPath.toFile().exists()) {
				tmpPath.toFile().delete();
			}
		}
		
	}
	
	@Test
	public void testOverwrite() throws IOException {
		
		Config cfg = new Config();
		cfg.userName = "testuser@sample.com";
		cfg.primarySeed = "a b c d e f g";
		cfg.dataPieces = 5;
		cfg.parityPieces = 12;
		
		Path tmpPath = null;
		try {
		
			tmpPath = Files.createTempFile(null, null);
			BufferedWriter writer = new BufferedWriter(new FileWriter(tmpPath.toFile(), true));
			writer.write("write random dummy data");
			writer.flush();
			writer.close();
			
			cfg.save(tmpPath);
			
			Config cfg2 = Config.load(tmpPath);
			assertEquals(cfg2.userName, cfg.userName);
			assertEquals(cfg2.primarySeed, cfg.primarySeed);
			assertEquals(cfg2.dataPieces, cfg.dataPieces);
			assertEquals(cfg2.parityPieces, cfg.parityPieces);
		
		} finally {
			if(tmpPath != null && tmpPath.toFile().exists()) {
				tmpPath.toFile().delete();
			}
		}

	}
	
	public String getPropertiesString() {

		StringBuffer buf = new StringBuffer();
		buf.append("username = ");
		buf.append(userName);
		buf.append("\n");
		
		buf.append("primary-seed = ");
		buf.append(primarySeed);
		buf.append("\n");
		
		buf.append("data-pieces = ");
		buf.append(dataPieces);
		buf.append("\n");
		
		buf.append("parity-pieces = ");
		buf.append(parityPieces);
		return buf.toString();
		
	}

}
