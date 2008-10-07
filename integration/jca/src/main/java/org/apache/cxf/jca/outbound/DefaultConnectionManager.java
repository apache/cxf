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
package org.apache.cxf.jca.outbound;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import org.apache.cxf.common.logging.LogUtils;

/**
 * Default Connection Manager which does not support connection pool.  
 * Connection will be destroyed upon closing by application.
 */
public class DefaultConnectionManager implements ConnectionManager, ConnectionEventListener {

    private static final long serialVersionUID = -8931949400870739450L;
    private static final Logger LOG = LogUtils.getL7dLogger(DefaultConnectionManager.class);


    /* -------------------- ConnectionManager Methods ----------------------------
     */
    public Object allocateConnection(ManagedConnectionFactory mcf,
            ConnectionRequestInfo cxRequestInfo) throws ResourceException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("allocateConnection cxRequestInfo = " + cxRequestInfo);
        }

        ManagedConnection mc = mcf.createManagedConnection(null, cxRequestInfo);
        mc.addConnectionEventListener(this);
        return mc.getConnection(null, cxRequestInfo);
    }

    /* -------------------- ConnectionEventListener Methods -----------------------
     */
    public void connectionClosed(ConnectionEvent event) {
        try {
            ((ManagedConnection)event.getSource()).destroy();
        } catch (ResourceException e) {
            LOG.log(Level.SEVERE, "Failed to destroy connection.", e);
        }
    }

    public void connectionErrorOccurred(ConnectionEvent event) {
        try {
            ((ManagedConnection)event.getSource()).destroy();
        } catch (ResourceException e) {
            LOG.log(Level.SEVERE, "Failed to destroy connection.", e);
        }
    }

    public void localTransactionCommitted(ConnectionEvent event) {
    }

    public void localTransactionRolledback(ConnectionEvent event) {
    }

    public void localTransactionStarted(ConnectionEvent event) {
    }

}
