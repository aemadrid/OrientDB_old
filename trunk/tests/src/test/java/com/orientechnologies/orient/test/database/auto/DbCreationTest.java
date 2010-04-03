/*
 * Copyright 1999-2010 Luca Garulli (l.garulli--at--orientechnologies.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.test.database.auto;

import java.io.IOException;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.orientechnologies.orient.client.admin.OServerAdmin;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordFlat;

@Test(groups = "db")
public class DbCreationTest {
	public static final String			STORAGE_MODE	= "csv";

	private String									url;
	private ODatabaseRecordFlat	database;

	@Parameters(value = "url")
	public DbCreationTest(String iURL) {
		url = iURL;
	}

	public void testDbCreation() throws IOException {
		if (url.startsWith("remote"))
			new OServerAdmin(url).connect().createDatabase("admin", "admin", STORAGE_MODE).close();
		else {
			database = new ODatabaseRecordFlat(url);
			database.create(STORAGE_MODE);
			database.close();
		}
	}

	@Test(dependsOnMethods = { "testDbCreation" })
	public void testDbOpen() {
		database = new ODatabaseRecordFlat(url);
		database.open("admin", "admin");
		database.close();
	}
}