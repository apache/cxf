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
package org.apache.cxf.jca.cxf;


import java.io.Serializable;
import java.util.ResourceBundle;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.connector.CXFConnectionFactory;
import org.apache.cxf.connector.CXFConnectionParam;
import org.apache.cxf.jca.core.resourceadapter.ResourceAdapterInternalException;

public class ConnectionFactoryImpl implements CXFConnectionFactory, 
                                              Referenceable, 
                                              Serializable {
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(ConnectionFactoryImpl.class);
    private ManagedConnectionFactory managedConnectionFactory;
    private ConnectionManager connectionManager;
    private Reference reference;

    public ConnectionFactoryImpl(ManagedConnectionFactory aMCF, ConnectionManager aCM) {
        managedConnectionFactory = aMCF;
        connectionManager = aCM;
    }

    public void setReference(Reference ref) {
        reference = ref;
    }

    public Reference getReference() throws NamingException {
        return reference;
    }
    
  

    public Object getBus() { 
        return ((ManagedConnectionFactoryImpl)managedConnectionFactory).getBus();
    }

    public Object getConnection(CXFConnectionParam param) throws ResourceException {
        
        if (param.getInterface() == null) {
            throw new ResourceAdapterInternalException(new Message("INTERFACE_IS_NULL", BUNDLE).toString());
        }
        
        if (!param.getInterface().isInterface()) {
            throw new ResourceAdapterInternalException(new Message("IS_NOT_AN_INTERFACE", 
                                                                   BUNDLE, param.getInterface()).toString());
        }
        
        CXFConnectionRequestInfo reqInfo = (CXFConnectionRequestInfo) param;
        
        if (connectionManager == null) {
            // non-managed, null Subject
            ManagedConnection connection = managedConnectionFactory.createManagedConnection(null, reqInfo);
            return connection.getConnection(null, reqInfo);
        } else {
            return connectionManager.allocateConnection(managedConnectionFactory, reqInfo);
        }
    }


}

