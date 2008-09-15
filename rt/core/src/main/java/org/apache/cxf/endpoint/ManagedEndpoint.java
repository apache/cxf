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

package org.apache.cxf.endpoint;

import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.annotation.ManagedAttribute;
import org.apache.cxf.management.annotation.ManagedOperation;
import org.apache.cxf.management.annotation.ManagedResource;

@ManagedResource(componentName = "Endpoint", 
                 description = "Responsible for managing server instances.")

public class ManagedEndpoint implements ManagedComponent, ServerLifeCycleListener {

    private Bus bus;
    private Endpoint endpoint;
    private Server server;
    private enum State { CREATED, STARTED, STOPPED };
    private State state = State.CREATED;
    
    public ManagedEndpoint(Bus b, Endpoint ep, Server s) {
        bus = b;
        endpoint = ep;
        server = s;
    }

    @ManagedOperation        
    public void start() {
        if (state == State.STARTED) {
            return;
        }
        ServerLifeCycleManager mgr = bus.getExtension(ServerLifeCycleManager.class);
        if (mgr != null) {
            mgr.registerListener(this);
        }
        server.start();
    }
    
    @ManagedOperation
    public void stop() {
        server.stop();
    }

    @ManagedAttribute(description = "Address Attribute", currencyTimeLimit = 60)
    public String getAddress() {
        return endpoint.getEndpointInfo().getAddress();
    }
    
    @ManagedAttribute(description = "TransportId Attribute", currencyTimeLimit = 60)
    public String getTransportId() {
        return endpoint.getEndpointInfo().getAddress();
    }
    
    @ManagedAttribute(description = "Server State")
    public String getState() {
        return state.toString();
    }
        
    public ObjectName getObjectName() throws JMException {
        String busId = bus.getId();
        StringBuffer buffer = new StringBuffer();
        buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME + ":");
        buffer.append(ManagementConstants.BUS_ID_PROP + "=" + busId + ",");
        buffer.append(ManagementConstants.TYPE_PROP + "=" + "Bus.Service.Endpoint,");
       

        String serviceName = ObjectName.quote(endpoint.getService().getName().toString());
        buffer.append(ManagementConstants.SERVICE_NAME_PROP + "=" + serviceName + ",");
        String endpointName = ObjectName.quote(endpoint.getEndpointInfo().getName().getLocalPart());
        buffer.append(ManagementConstants.PORT_NAME_PROP + "=" + endpointName);
        
        //Use default domain name of server
        return new ObjectName(buffer.toString());
    }

    public void startServer(Server s) {
        if (server.equals(s)) {
            state = State.STARTED;            
        }
    }

    public void stopServer(Server s) {
        if (server.equals(s)) {
            state = State.STOPPED;
            // unregister server to avoid the memory leak
            ServerLifeCycleManager mgr = bus.getExtension(ServerLifeCycleManager.class);
            if (mgr != null) {
                mgr.unRegisterListener(this);                
            }
        }
    }
}
