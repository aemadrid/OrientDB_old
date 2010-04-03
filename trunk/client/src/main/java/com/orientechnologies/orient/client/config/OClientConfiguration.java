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
package com.orientechnologies.orient.client.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.orientechnologies.orient.core.config.OStorageEntryConfiguration;

@XmlRootElement(name = "orient-client")
public class OClientConfiguration {
	public int												connectionTimeout			= 5000;
	public int												connectionRetry				= 5;
	public long												connectionRetryDelay	= 500;

	public List<OStorageEntryConfiguration>	properties						= new ArrayList<OStorageEntryConfiguration>();
}