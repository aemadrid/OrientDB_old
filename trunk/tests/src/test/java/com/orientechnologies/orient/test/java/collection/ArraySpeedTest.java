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
package com.orientechnologies.orient.test.java.collection;

public class ArraySpeedTest extends CollectionBaseAbstractSpeedTest {
	private String[]	array;

	public void cycle() {
		for (String item : array) {
			if (item.equals(searchedValue))
				break;
		}
	}

	public void init() {
		array = new String[collectionSize];
		for (int i = 0; i < collectionSize; ++i) {
			array[i] = String.valueOf(i);
		}
	}

	public void deInit() {
		array = null;
	}
}