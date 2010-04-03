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
package com.orientechnologies.orient.server.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.server.OClientConnection;
import com.orientechnologies.orient.server.OClientConnectionManager;
import com.orientechnologies.orient.server.network.protocol.ONetworkProtocol;

public class OServerNetworkListener {
	private ServerSocket			serverSocket;
	private InetSocketAddress	inboundAddr;
	private volatile int			connectionSerial	= 0;
	private volatile boolean	active						= true;

	public OServerNetworkListener(String iHostName, int iHostPort, Class<? extends ONetworkProtocol> iProtocol) {
		listen(iHostName, iHostPort);

		ONetworkProtocol protocol;
		OClientConnection connection;

		try {
			while (active) {
				try {
					// listen for and accept a client connection to serverSocket
					Socket sock = serverSocket.accept();

					// CREATE A NEW PROTOCOL INSTANCE
					protocol = iProtocol.newInstance();

					// CTEARE THE CLIENT CONNECTION
					connection = new OClientConnection(connectionSerial++, sock, protocol);

					// CONFIGURE THE PROTOCOL FOR THE INCOMING CONNECTION
					protocol.config(sock, connection);

					// EXECUTE THE CONNECTION
					OClientConnectionManager.instance().connect(sock, connection);

				} catch (Throwable e) {
					OLogManager.instance().error(this, "Error on client connection", e);
				} finally {
				}
			}
		} finally {
			try {
				if (serverSocket != null && !serverSocket.isClosed())
					serverSocket.close();
			} catch (IOException ioe) {
			}
		}
	}

	public void shutdown() {
		this.active = false;
	}

	/**
	 * Initialize a server socket for communicating with the client.
	 * 
	 * @param iHostPort
	 * @param iHostName
	 */
	private void listen(String iHostName, int iHostPort) {
		inboundAddr = new InetSocketAddress(iHostName, iHostPort);
		try {
			serverSocket = new java.net.ServerSocket(iHostPort);

			if (serverSocket.isBound()) {
				OLogManager.instance().config(this,
						"Orient Database Server listening connection on " + inboundAddr.getHostName() + ":" + inboundAddr.getPort());
			}
		} catch (SocketException se) {
			OLogManager.instance().error(this, "Unable to create socket", se);
			System.exit(1);
		} catch (IOException ioe) {
			OLogManager.instance().error(this, "Unable to read data from an open socket", ioe);
			System.err.println("Unable to read data from an open socket.");
			System.exit(1);
		}
	}

	public boolean isActive() {
		return active;
	}
}