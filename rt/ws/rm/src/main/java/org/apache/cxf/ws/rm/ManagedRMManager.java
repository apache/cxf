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

package org.apache.cxf.ws.rm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.JMException;
import javax.management.ObjectName;

//import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.management.annotation.ManagedAttribute;
import org.apache.cxf.management.annotation.ManagedOperation;
import org.apache.cxf.management.annotation.ManagedResource;

/**
 * The ManagedRMManager is a JMX managed bean for RMManager.
 *
 */
@ManagedResource(componentName = "RMManager", 
                 description = "Responsible for managing RMEndpoints.")
public class ManagedRMManager implements ManagedComponent {

    private RMManager manager;
    
    public ManagedRMManager(RMManager manager) {
        this.manager = manager;
    }
    
    /* (non-Javadoc)
     * @see org.apache.cxf.management.ManagedComponent#getObjectName()
     */
    public ObjectName getObjectName() throws JMException {
        return RMUtils.getManagedObjectName(manager);
    }

    @ManagedOperation       
    public void shutdown() {
        manager.shutdown();
    }
    
    @ManagedOperation
    public String[] getEndpointIdentifiers() {
        Set<String> identifiers = new HashSet<String>();
        //FIXME find this method for 2.5
//        for (Endpoint ep : manager.getReliableEndpointsMap().keySet()) {
        for (Endpoint ep : getReliableEndpointsMap().keySet()) {
            identifiers.add(RMUtils.getEndpointIdentifier(ep, manager.getBus()));
        }
        return identifiers.toArray(new String[identifiers.size()]);
    }

    //TODO see the comment above
    private Map<Endpoint, RMEndpoint> getReliableEndpointsMap() {
        Map<Endpoint, RMEndpoint> epmap = new HashMap<Endpoint, RMEndpoint>();
        for (ProtocolVariation pv : manager.getEndpointMaps().keySet()) {
            epmap.putAll(manager.getEndpointMaps().get(pv));
        }
        return epmap;
    }
    
    @ManagedAttribute(description = "Using Store")
    public boolean isUsingStore() {
        return manager.getStore() != null;
    }
}
