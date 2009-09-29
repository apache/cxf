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
package org.apache.cxf.transport.http_osgi;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.http.AbstractHTTPDestination;


public class OsgiDestination extends AbstractHTTPDestination {

    static final Logger LOG = LogUtils.getL7dLogger(OsgiDestination.class);

    private static final long serialVersionUID = 1L;

    final OsgiDestinationRegistryIntf factory;
    final String path;

    /**
     * Constructor, allowing substitution of configuration.
     *
     * @param b the associated Bus
     * @param ci the associated conduit initiator
     * @param ei the endpoint info of the destination
     * @param fact the transport factory
     * @param p the path
     * @throws IOException
     */
    public OsgiDestination(Bus b,
                           EndpointInfo ei,
                           OsgiDestinationRegistryIntf fact,
                           String p)
        throws IOException {
        // would add the default port to the address
        super(b, null, ei, false);
        factory = fact;
        path = p;
    }


    protected Logger getLogger() {
        return LOG;
    }

    protected Bus getBus() {
        return bus;
    }


    protected void doMessage(MessageImpl inMessage) throws IOException {
        try {

            setHeaders(inMessage);

            inMessage.setDestination(this);

            incomingObserver.onMessage(inMessage);

        } finally {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Finished servicing http request on thread: " + Thread.currentThread());
            }
        }
    }

    @Override
    public void shutdown() {
        factory.removeDestination(path);
        super.shutdown();
    }

    public MessageObserver getMessageObserver() {
        return this.incomingObserver;
    }

    public EndpointInfo getEndpointInfo() {
        return endpointInfo;
    }
}
