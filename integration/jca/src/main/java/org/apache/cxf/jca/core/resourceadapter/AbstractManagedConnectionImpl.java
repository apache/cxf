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
package org.apache.cxf.jca.core.resourceadapter;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jca.core.logging.LoggerHelper;
import org.apache.cxf.jca.cxf.CXFManagedConnectionMetaData;

public abstract class AbstractManagedConnectionImpl implements ManagedConnection {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractManagedConnectionImpl.class);
    protected PrintWriter printWriter;
    protected ConnectionRequestInfo crinfo;
    protected Subject subject;
    protected Set<ConnectionEventListener> connectionEventListeners = 
        new HashSet<ConnectionEventListener>();
    private final AbstractManagedConnectionFactoryImpl managedConnectionFactory;

    public AbstractManagedConnectionImpl(AbstractManagedConnectionFactoryImpl managedFactory, 
        ConnectionRequestInfo crInfo,
        Subject sj) throws ResourceException {
        
        this.managedConnectionFactory = managedFactory;
        this.crinfo = (ConnectionRequestInfo)crInfo;
    }

    
    public void addConnectionEventListener(ConnectionEventListener aListener) {
        LOG.log(Level.FINE, "ADD_EVENT_LISTENER_CALLED", new Object[] {this, aListener});
        connectionEventListeners.add(aListener);
    }

    public void removeConnectionEventListener(ConnectionEventListener aListener) {
        connectionEventListeners.remove(aListener);
    }

    public abstract void associateConnection(Object arg0) throws ResourceException;

    //public abstract LocalTransaction getLocalTransaction() throws ResourceException;

    //public abstract javax.transaction.xa.XAResource getXAResource() throws ResourceException;

    public abstract Object getConnection(Subject aSubject, ConnectionRequestInfo aCrInfo)
        throws ResourceException;

    public abstract boolean isBound();

    public void close(Object closingHandle) throws ResourceException {
        LOG.fine("Closing handle: " + closingHandle);

        ConnectionEvent coEvent = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        coEvent.setConnectionHandle(closingHandle);
        sendEvent(coEvent);
    }

    // going back in the pool
    public void cleanup() throws ResourceException {
        LOG.log(Level.FINE, "CLEANUP_CALLED", new Object[] {this});
    }

    // beging chucked from the pool
    public void destroy() throws ResourceException {
        LOG.log(Level.FINE, "DESTROY_CALLED", new Object[] {this});
        connectionEventListeners = new HashSet<ConnectionEventListener>();
        
        LoggerHelper.deleteLoggingOnWriter();
        if (printWriter != null) {
            printWriter.close();
        }
    }

    public PrintWriter getLogWriter() throws ResourceException {
        return printWriter;
    }

    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new CXFManagedConnectionMetaData();
    }

    public void setLogWriter(PrintWriter aPrintWriter) throws ResourceException {
        printWriter = aPrintWriter;

        if (printWriter != null) {
            LoggerHelper.initializeLoggingOnWriter(printWriter);
        }
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject sj) {
        this.subject = sj;
    }

    protected ConnectionRequestInfo getConnectionRequestInfo() {
        return crinfo;
    }

    protected void setConnectionRequestInfo(ConnectionRequestInfo info) {
        this.crinfo = info;
    }

    protected void sendEvent(ConnectionEvent coEvent) {
        Iterator iter = connectionEventListeners.iterator();

        while (iter.hasNext()) {
            sendEventToListener(coEvent, (ConnectionEventListener)iter.next());
        }
    }

    protected void sendEventToListener(ConnectionEvent coEvent, ConnectionEventListener listener) {
        if (coEvent.getId() == ConnectionEvent.CONNECTION_CLOSED) {
            listener.connectionClosed(coEvent);
            LOG.log(Level.FINE, "CONNECTION_CLOSED_EVENT_FIRED", new Object[] {listener});
        }

        if (coEvent.getId() == ConnectionEvent.LOCAL_TRANSACTION_COMMITTED) {
            listener.localTransactionCommitted(coEvent);
            LOG.log(Level.FINE, "LOCAL_TX_COMMITTED_EVENT_FIRED", new Object[] {listener});
        }

        if (coEvent.getId() == ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK) {
            listener.localTransactionRolledback(coEvent);
            LOG.log(Level.FINE, "LOCAL_TX_ROLLEDBACK_EVENT_FIRED", new Object[] {listener});
        }

        if (coEvent.getId() == ConnectionEvent.LOCAL_TRANSACTION_STARTED) {
            listener.localTransactionStarted(coEvent);
            LOG.log(Level.FINE, "LOCAL_TX_STARTED_EVENT_FIRED", new Object[] {listener});
        }

        if (coEvent.getId() == ConnectionEvent.CONNECTION_ERROR_OCCURRED) {
            listener.connectionErrorOccurred(coEvent);
            LOG.log(Level.FINE, "CTX_ERROR_OCURRED_EVENT_FIRED", new Object[] {listener});
        }
    }

    protected AbstractManagedConnectionFactoryImpl theManagedConnectionFactory() {
        return managedConnectionFactory;
    }

    public String toString() {
        return "[" + getClass().getName() + ":" + hashCode() + ":ManagedConnection[" + crinfo + "]";
    }

    public void error(Exception ex) {
        LOG.warning(ex.toString());
        sendEvent(new ConnectionEvent(this, ConnectionEvent.CONNECTION_ERROR_OCCURRED, ex));
    }
}
