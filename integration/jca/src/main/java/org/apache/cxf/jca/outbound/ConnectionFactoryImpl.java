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

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

/**
 * Implement ConnectionFactory that delegate allocation of connection
 * to {@link ConnectionManager}.
 */
public class ConnectionFactoryImpl implements CXFConnectionFactory {

    private static final long serialVersionUID = -9185852866781165233L;
    private ManagedConnectionFactory mcf;
    private ConnectionManager connectionManager;
    private Reference reference;

    public ConnectionFactoryImpl(ManagedConnectionFactory mcf,
            ConnectionManager connectionManager) {
        this.mcf = mcf;
        this.connectionManager = connectionManager;
    }

    public void setReference(Reference reference) {
        this.reference = reference; 
    }

    public Reference getReference() throws NamingException {
        return reference;
    }

    public CXFConnection getConnection(CXFConnectionSpec spec) throws ResourceException {
        return (CXFConnection) connectionManager.allocateConnection(mcf, spec);
    }

}
