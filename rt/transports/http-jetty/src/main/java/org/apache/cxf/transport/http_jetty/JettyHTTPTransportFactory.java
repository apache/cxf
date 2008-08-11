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
package org.apache.cxf.transport.http_jetty;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.imageio.IIOException;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.AbstractHTTPTransportFactory;

public class JettyHTTPTransportFactory extends AbstractHTTPTransportFactory
    implements DestinationFactory {

    private Map<String, JettyHTTPDestination> destinations = 
        new ConcurrentHashMap<String, JettyHTTPDestination>();
    
    public JettyHTTPTransportFactory() {
        super();
    }
    
    @Resource(name = "cxf")
    public void setBus(Bus b) {
        super.setBus(b);
    }

    @PostConstruct
    public void finalizeConfig() {
        if (null == bus) {
            return;
        }
        
        if (getTransportIds() == null) {
            setTransportIds(new ArrayList<String>(activationNamespaces));
        }
        if (activationNamespaces == null) {
            activationNamespaces = getTransportIds();
        }
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        if (null != dfm && null != activationNamespaces) {
            for (String ns : activationNamespaces) {
                dfm.registerDestinationFactory(ns, this);
            }
        }

        // This call will register the server engine factory
        // with the Bus.
        getJettyHTTPServerEngineFactory();
    }
    
    /**
     * This method returns the Jetty HTTP Server Engine Factory.
     */
    protected JettyHTTPServerEngineFactory getJettyHTTPServerEngineFactory() {
        // We have got to *always* get this off the bus, because it may have 
        // been overridden by Spring Configuration initially.
        // Spring Configuration puts it on the correct bus.
        JettyHTTPServerEngineFactory serverEngineFactory =
                getBus().getExtension(JettyHTTPServerEngineFactory.class);
        // If it's not there, then create it and register it.
        // Spring may override it later, but we need it here for default
        // with no spring configuration.
        if (serverEngineFactory == null) {
            serverEngineFactory = new JettyHTTPServerEngineFactory();
            serverEngineFactory.setBus(getBus());
            serverEngineFactory.finalizeConfig();
        }
        return serverEngineFactory;
    }
    
    public Destination getDestination(EndpointInfo endpointInfo) 
        throws IOException {
        
        String addr = endpointInfo.getAddress();
        JettyHTTPDestination destination = addr == null ? null : destinations.get(addr);
        if (destination == null) {
            destination = createDestination(endpointInfo);
        }
           
        return destination;
    }
    
    private synchronized JettyHTTPDestination createDestination(
        EndpointInfo endpointInfo
    ) throws IOException {
        
        String addr = endpointInfo.getAddress();
        JettyHTTPDestination destination = addr == null ? null : destinations.get(addr);
        if (destination == null) {
            destination = 
                new JettyHTTPDestination(getBus(), this, endpointInfo);
            
            destinations.put(endpointInfo.getAddress(), destination);
            
            configure(destination);
            try {
                destination.finalizeConfig();
            } catch (GeneralSecurityException ex) {
                throw new IIOException("JSSE Security Exception ", ex);
            }
        }
        return destination;
    }
    
    /**
     * This function removes the destination for a particular endpoint.
     */
    void removeDestination(EndpointInfo ei) {
        destinations.remove(ei.getAddress());
    }
}
