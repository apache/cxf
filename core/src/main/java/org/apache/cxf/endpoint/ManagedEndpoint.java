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
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.annotation.ManagedAttribute;
import org.apache.cxf.management.annotation.ManagedOperation;
import org.apache.cxf.management.annotation.ManagedResource;

@ManagedResource(componentName = "Endpoint", 
                 description = "Responsible for managing server instances.")

public class ManagedEndpoint implements ManagedComponent, ServerLifeCycleListener {
    public static final String ENDPOINT_NAME = "managed.endpoint.name";
    public static final String SERVICE_NAME = "managed.service.name";

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
    
    @ManagedOperation
    public void destroy() {
        server.destroy();
    }

    @ManagedAttribute(description = "Address Attribute", currencyTimeLimit = 60)
    public String getAddress() {
        return endpoint.getEndpointInfo().getAddress();
    }
    
    @ManagedAttribute(description = "TransportId Attribute", currencyTimeLimit = 60)
    public String getTransportId() {
        return endpoint.getEndpointInfo().getTransportId();
    }
    
    @ManagedAttribute(description = "Server State")
    public String getState() {
        return state.toString();
    }
        
    public ObjectName getObjectName() throws JMException {
        String busId = bus.getId();
        StringBuilder buffer = new StringBuilder();
        buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME).append(':');
        buffer.append(ManagementConstants.BUS_ID_PROP).append('=').append(busId).append(',');
        buffer.append(ManagementConstants.TYPE_PROP).append('=').append("Bus.Service.Endpoint,");
       

        String serviceName = (String)endpoint.get(SERVICE_NAME);
        if (StringUtils.isEmpty(serviceName)) {
            serviceName = endpoint.getService().getName().toString();
        }
        serviceName = ObjectName.quote(serviceName);
        buffer.append(ManagementConstants.SERVICE_NAME_PROP).append('=').append(serviceName).append(',');
        
        
        String endpointName = (String)endpoint.get(ENDPOINT_NAME);
        if (StringUtils.isEmpty(endpointName)) {
            endpointName = endpoint.getEndpointInfo().getName().getLocalPart();
        }
        endpointName = ObjectName.quote(endpointName);
        buffer.append(ManagementConstants.PORT_NAME_PROP).append('=').append(endpointName).append(',');
        // Added the instance id to make the ObjectName unique
        buffer.append(ManagementConstants.INSTANCE_ID_PROP).append('=').append(endpoint.hashCode());
        
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
