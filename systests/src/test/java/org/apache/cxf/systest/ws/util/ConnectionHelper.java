/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.systest.ws.util;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

/**
 * 
 */
public final class ConnectionHelper {

    private ConnectionHelper() {
    }
    
    public static void setKeepAliveConnection(Object proxy, boolean keepAlive) {
        setKeepAliveConnection(proxy, keepAlive, true);
    }

    public static void setKeepAliveConnection(Object proxy, boolean keepAlive, boolean force) {
        if (force || "HP-UX".equals(System.getProperty("os.name"))
            || "Windows XP".equals(System.getProperty("os.name"))) {
            Client client = ClientProxy.getClient(proxy);
            HTTPConduit hc = (HTTPConduit)client.getConduit();
            HTTPClientPolicy cp = hc.getClient();
            cp.setConnection(keepAlive ? ConnectionType.KEEP_ALIVE : ConnectionType.CLOSE);
        }
    }

    public static boolean isKeepAliveConnection(Object proxy) {
        Client client = ClientProxy.getClient(proxy);
        HTTPConduit hc = (HTTPConduit)client.getConduit();
        HTTPClientPolicy cp = hc.getClient();
        return cp.getConnection() == ConnectionType.KEEP_ALIVE;
    }
}
