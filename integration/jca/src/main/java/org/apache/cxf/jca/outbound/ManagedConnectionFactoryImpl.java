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

import java.io.PrintWriter;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.work.WorkManager;
import javax.security.auth.Subject;

import org.apache.commons.lang.ObjectUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jca.core.logging.LoggerHelper;
import org.apache.cxf.jca.cxf.ResourceAdapterImpl;

public class ManagedConnectionFactoryImpl implements ManagedConnectionFactory, 
    ResourceAdapterAssociation {

    private static final long serialVersionUID = -5294527634981120642L;
    private static final Logger LOG = LogUtils.getL7dLogger(ManagedConnectionFactoryImpl.class);
    
    private String busConfigURL;
    private PrintWriter printWriter;
    private ResourceAdapter resourceAdapter;
    
    private ConnectionManager defaultConnectionManager = 
        new DefaultConnectionManager();

    static {
        // first use of log, default init if necessary
        LoggerHelper.init();
    }
   
    /* --------------------------------------------------------------------
     *                           Bean Properties
     */
    public void setBusConfigURL(String busConfigURL) {
        this.busConfigURL = busConfigURL;
    }

    public String getBusConfigURL() {
        return busConfigURL;
    }
    
  
    /* --------------------------------------------------------------------
     *                    ManagedConnectionFactory methods
     */
    public Object createConnectionFactory() throws ResourceException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Create connection factory for unmanaged connections");
        }
        return new ConnectionFactoryImpl(this, defaultConnectionManager);
    }
    
    public Object createConnectionFactory(ConnectionManager connMgr) 
        throws ResourceException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Create connection factory by app server connMgr " + connMgr);
        }
        return new ConnectionFactoryImpl(this, 
                connMgr == null ? defaultConnectionManager : connMgr);
    }

    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connReqInfo) 
        throws ResourceException {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Create managed connection subject=" + subject + "connReqInfo="
                    + connReqInfo);
        }
        return new ManagedConnectionImpl(this, connReqInfo, subject);
    }
    
    // hashCode method is required by JCA 1.5 because on properties
    public int hashCode() {
        int retval = 0;

        if (busConfigURL != null) {
            retval += busConfigURL.hashCode();
        }
        
        return retval;
    }
    
    // equals method is required by JCA 1.5 because on properties
    public boolean equals(Object o) {
        if (!this.getClass().isAssignableFrom(o.getClass())) {
            return false;
        }
        
        ManagedConnectionFactoryImpl that = (ManagedConnectionFactoryImpl)o;
       
        if (!ObjectUtils.equals(that.getBusConfigURL(), busConfigURL)) {
            return false;
        }
        
        return true;
    }
    
    public PrintWriter getLogWriter() throws ResourceException {
        return printWriter;
    }

    public void setLogWriter(final PrintWriter aPrintWriter) throws ResourceException {
        if (aPrintWriter == null) {
            throw new IllegalArgumentException("NULL_LOG_WRITER");
        }

        printWriter = aPrintWriter;
        LoggerHelper.initializeLoggingOnWriter(printWriter);
    }
    
    public ManagedConnection matchManagedConnections(Set mcs, Subject subject, 
            ConnectionRequestInfo reqInfo) throws ResourceException {

        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("match connections: set=" + mcs + ", subject=" + subject
                    + " reqInfo=" + reqInfo);
        }

        // find the first managed connection that matches the bus and request info
        Iterator iter = mcs.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (!(obj instanceof ManagedConnectionImpl)) {
                continue;
            }
            
            ManagedConnectionImpl mc = (ManagedConnectionImpl)obj;
             
            if (!ObjectUtils.equals(busConfigURL,
                    mc.getManagedConnectionFactoryImpl().getBusConfigURL())) {
                continue;
            }
            
            if (!ObjectUtils.equals(reqInfo, mc.getRequestInfo())) {
                continue;
            }
            
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("found matched connection " + mc);
            }
            return mc;
        }
        return null;
    }
    
    /* --------------------------------------------------------------------
     *                      ResourceAdapterAssociation methods
     */
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        resourceAdapter = ra;
    }

    /* --------------------------------------------------------------------
     *                      public methods
     */
    public WorkManager getWorkManager() {
        if (resourceAdapter instanceof ResourceAdapterImpl) {
            return ((ResourceAdapterImpl)resourceAdapter).getBootstrapContext()
                .getWorkManager();
        } else {
            return null;
        }
    }

}
