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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.security.auth.Subject;

import org.apache.cxf.common.logging.LogUtils;



public class DummyManagedConnectionImpl extends AbstractManagedConnectionImpl {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractManagedConnectionImpl.class);
    boolean bound;
    Object con;

    public DummyManagedConnectionImpl(
        AbstractManagedConnectionFactoryImpl managedFactory,
        ConnectionRequestInfo crInfo, Subject subject)
        throws ResourceException {
        super(managedFactory, crInfo, subject);
        // trun off the noise error logger
        LOG.setLevel(Level.OFF);
    }

    public void associateConnection(Object arg0) throws ResourceException {
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        return null;
    }

    public javax.transaction.xa.XAResource getXAResource()
        throws ResourceException {
        return null;
    }

    public Object getConnection(Subject subject, ConnectionRequestInfo crInfo)
        throws ResourceException {
        return con;
    }

    public boolean isBound() {
        return bound;
    }

    public void setBound(boolean b) {
        bound = b;
    }

    // use to indicate invalid
    public void setCon(Object o) {
        con = o;
    }
/*
    public CXFManagedConnectionFactory getManagedConnectionFactory() { 
        return (CXFManagedConnectionFactory)theManagedConnectionFactory();
    } */ 
}
