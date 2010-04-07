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
package com.orientechnologies.orient.core.query.sql;

import java.io.IOException;
import java.util.List;

import com.orientechnologies.orient.core.query.OQueryAbstract;
import com.orientechnologies.orient.core.record.ORecordSchemaAware;
import com.orientechnologies.orient.core.serialization.OBinaryProtocol;
import com.orientechnologies.orient.core.serialization.OSerializableStream;

public abstract class OSQLQuery<T extends ORecordSchemaAware<?>> extends OQueryAbstract<T> implements OSerializableStream {
	public static final String	NAME	= "sql";

	protected String						text;

	public OSQLQuery(String iText) {
		text = iText;
	}

	public OSQLQuery() {
	}

	/**
	 * Delegates to the OQueryExecutor the query execution.
	 */
	public List<T> execute(int iLimit) {
		return database.getStorage().getQueryExecutor(this).execute(this, iLimit);
	}

	/**
	 * Delegates to the OQueryExecutor the query execution.
	 */
	public T executeFirst() {
		return database.getStorage().getQueryExecutor(this).executeFirst(this);
	}

	public String text() {
		return text;
	}

	@Override
	public String toString() {
		return "OSQLQuery [text=" + text + "]";
	}

	public OSerializableStream fromStream(byte[] iStream) throws IOException {
		text = OBinaryProtocol.bytes2string(iStream);
		return this;
	}

	public byte[] toStream() throws IOException {
		return OBinaryProtocol.string2bytes(text);
	}
}