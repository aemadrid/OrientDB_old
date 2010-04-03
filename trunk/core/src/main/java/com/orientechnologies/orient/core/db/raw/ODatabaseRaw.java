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
package com.orientechnologies.orient.core.db.raw;

import java.io.IOException;
import java.util.Collection;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.cache.OCacheRecord;
import com.orientechnologies.orient.core.config.OStorageLogicalClusterConfiguration;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabaseComplex;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.intent.OIntent;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.storage.impl.logical.OClusterLogical;

@SuppressWarnings("unchecked")
public class ODatabaseRaw implements ODatabase {
	private static volatile int	serialId	= 0;

	protected int								id;
	protected OStorage					storage;
	protected STATUS						status;

	private ODatabaseRecord<?>	databaseOwner;

	public enum STATUS {
		OPEN, CLOSED
	}

	public ODatabaseRaw(final String iURL) {
		try {
			storage = Orient.instance().getStorage(iURL);
			id = serialId++;
			status = STATUS.CLOSED;
		} catch (Throwable t) {
			OLogManager.instance().error(this, "Error on opening database '" + iURL + "'", t, ODatabaseException.class);
		}
	}

	public <DB extends ODatabase> DB open(final String iUserName, final String iUserPassword) {
		try {
			if (status == STATUS.OPEN)
				throw new IllegalStateException("Database " + getName() + " is already open");

			storage.open(getId(), iUserName, iUserPassword);

			status = STATUS.OPEN;

			// OPEN LOGICAL CLUSTERS IF ANY
			for (OStorageLogicalClusterConfiguration lc : storage.getConfiguration().logicalClusters) {
				storage.registerLogicalCluster(new OClusterLogical(databaseOwner, lc.name, lc.id, lc.map));
			}

		} catch (Exception e) {
			OLogManager.instance().error(this, "Can't open database", e, ODatabaseException.class);
		}
		return (DB) this;
	}

	public <DB extends ODatabase> DB create(final String iStorageMode) {
		try {
			storage.create(iStorageMode);
			status = STATUS.OPEN;
		} catch (Exception e) {
			OLogManager.instance().error(this, "Can't create database", e, ODatabaseException.class);
		}
		return (DB) this;
	}

	public boolean exists() {
		if (status == STATUS.OPEN)
			return true;

		return storage.exists();
	}

	public long countClusterElements(final String iClusterName) {
		return storage.count(getClusterIdByName(iClusterName));
	}

	public long countClusterElements(final int iClusterId) {
		return storage.count(iClusterId);
	}

	public long countClusterElements(final int[] iClusterIds) {
		return storage.count(iClusterIds);
	}

	public Object[] read(final int iClusterId, final long iPosition) {
		try {

			final String recId = ORecordId.generateString(iClusterId, iPosition);

			// SEARCH IT IN CACHE
			Object[] result = getCache().findRecord(recId);
			if (result != null)
				return result;

			result = storage.readRecord(id, iClusterId, iPosition);

			getCache().addRecord(recId, result);

			return result;

		} catch (Throwable t) {
			OLogManager.instance().error(this,
					"Error on retrieving record #" + iPosition + " in cluster '" + storage.getPhysicalClusterNameById(iClusterId) + "'", t,
					ODatabaseException.class);
		}
		return null;
	}

	public long save(final int iClusterId, final long iPosition, final byte[] iContent, final int iVersion) {
		try {
			if (iPosition == ORID.CLUSTER_POS_INVALID)
				return storage.createRecord(iClusterId, iContent);

			int newVersion = storage.updateRecord(id, iClusterId, iPosition, iContent, iVersion);
			if (newVersion > -1)
				return newVersion * -1 - 2;

		} catch (Throwable t) {
			OLogManager.instance().error(this, "Error on saving record in cluster id: " + iClusterId + ", position: " + iPosition, t,
					ODatabaseException.class);
		}
		return ORID.CLUSTER_POS_INVALID;
	}

	public void delete(final String iClusterName, final long iPosition, final int iVersion) {
		delete(getClusterIdByName(iClusterName), iPosition, iVersion);
	}

	public void delete(final int iClusterId, final long iPosition, final int iVersion) {
		try {
			storage.deleteRecord(id, iClusterId, iPosition, iVersion);
		} catch (Throwable t) {
			OLogManager.instance().error(this,
					"Error on deleting record #" + iPosition + " in cluster '" + storage.getPhysicalClusterNameById(iClusterId) + "'", t,
					ODatabaseException.class);
		}
	}

	public OStorage getStorage() {
		return storage;
	}

	public boolean isClosed() {
		return status == STATUS.CLOSED;
	}

	public String getName() {
		return storage != null ? storage.getName() : "<no-name>";
	}

	@Override
	public void finalize() {
		close();
	}

	public void close() {
		if (status != STATUS.OPEN)
			return;

		if (storage != null)
			storage.removeUser();

		status = STATUS.CLOSED;
	}

	public int getId() {
		return id;
	}

	public int getClusterIdByName(final String iClusterName) {
		// SEARCH IT BETWEEN PHYSICAL CLUSTERS
		return storage.getClusterIdByName(iClusterName);
	}

	public String getClusterNameById(final int iClusterId) {
		if (iClusterId == -1)
			return null;

		// PHIYSICAL CLUSTER
		return storage.getPhysicalClusterNameById(iClusterId);
	}

	public int addLogicalCluster(final String iClusterName, final int iPhyClusterContainerId) {
		try {
			return storage.addLogicalCluster(new OClusterLogical(databaseOwner, iClusterName));

		} catch (IOException e) {
			throw new ODatabaseException("Error on adding logical cluster: " + iClusterName, e);
		}
	}

	public int addPhysicalCluster(String iClusterName, String iClusterFileName, int iStartSize) {
		return storage.addClusterSegment(iClusterName, iClusterFileName, iStartSize);
	}

	public int addDataSegment(String iSegmentName, String iSegmentFileName) {
		return storage.addDataSegment(iSegmentName, iSegmentFileName);
	}

	public Collection<String> getClusterNames() {
		return storage.getClusterNames();
	}

	public OCacheRecord getCache() {
		return storage.getCache();
	}

	public int getDefaultClusterId() {
		return storage.getClusterIdByName(OStorage.DEFAULT_SEGMENT);
	}

	public void declareIntent(final OIntent iIntent, final Object... iParams) {
		ODatabaseComplex<?> ownerDb = databaseOwner;

		while (ownerDb.getDatabaseOwner() != null && ownerDb.getDatabaseOwner() != ownerDb)
			ownerDb = ownerDb.getDatabaseOwner();

		iIntent.activate(ownerDb, iParams);
	}

	public <DB extends ODatabase> DB checkSecurity(String iResource, int iOperation) {
		// TODO
		// System.out.println("checkSecurity for [" + iResource + "], operation: " + iOperation);

		return null;
	}

	public ODatabaseRaw setOwner(final ODatabaseRecord<?> iOwner) {
		databaseOwner = iOwner;
		return this;
	}
}