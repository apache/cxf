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

package org.apache.cxf.ws.security.cache;

import java.io.Closeable;
import java.io.IOException;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientLifeCycleListener;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.wss4j.common.cache.ReplayCache;

/**
 *
 */
public class CacheCleanupListener implements ServerLifeCycleListener, ClientLifeCycleListener {

    public CacheCleanupListener() {
    }

    public void clientCreated(Client client) {
    }
    public void startServer(Server server) {
    }

    public void clientDestroyed(Client client) {
        shutdownResources(client.getEndpoint().getEndpointInfo());
    }
    public void stopServer(Server server) {
        shutdownResources(server.getEndpoint().getEndpointInfo());
    }


    protected void shutdownResources(EndpointInfo info) {
        TokenStore ts = (TokenStore)info.getProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
        if (ts instanceof Closeable) {
            close((Closeable)ts);
        }
        ReplayCache rc = (ReplayCache)info.getProperty(SecurityConstants.NONCE_CACHE_INSTANCE);
        if (rc != null) {
            close(rc);
        }
        rc = (ReplayCache)info.getProperty(SecurityConstants.TIMESTAMP_CACHE_INSTANCE);
        if (rc != null) {
            close(rc);
        }
    }

    private void close(Closeable ts) {
        try {
            ts.close();
        } catch (IOException ex) {
            //ignore, we're shutting down and nothing we can do
        }
    }
}
