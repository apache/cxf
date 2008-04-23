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
package org.apache.cxf.binding.object;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.local.LocalTransportFactory;

public class LocalServerListener implements ServerLifeCycleListener {
    private static final Logger LOG = LogUtils.getL7dLogger(LocalServerListener.class);

    private DestinationFactory destinationFactory;
    private BindingFactory bindingFactory;
    private ObjectBindingConfiguration configuration = new ObjectBindingConfiguration();
    private Bus bus;

    public LocalServerListener(Bus bus,
                               BindingFactory bindingFactory) {
        super();
        this.bindingFactory = bindingFactory;
        this.bus = bus;
    }
    
    public void startServer(Server server) {
        Endpoint endpoint = server.getEndpoint();
        Service service = endpoint.getService();

        // synthesize a new binding
        BindingInfo bi = bindingFactory.createBindingInfo(service, 
                                                          ObjectBindingFactory.BINDING_ID, 
                                                          configuration);
        
        Binding binding = bindingFactory.createBinding(bi);
        
        String uri = "local://" + server.toString();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(uri);
        
        try {
            // Register a new Destination locally for the Server
            Destination destination = getDestinationFactory().getDestination(ei);
            
            destination.setMessageObserver(new OverrideBindingObserver(endpoint, binding, bus));
        } catch (IOException e1) {
            LOG.log(Level.WARNING, "Could not create local destination.", e1);
        }
    }

    public void stopServer(Server server) {
        String uri = "local://" + server.toString();
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(uri);
        
        try {
            Destination destination = getDestinationFactory().getDestination(ei);
            
            destination.shutdown();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not shutdown local destination.", e);
        }
        
    }

    public DestinationFactory getDestinationFactory() {
        if (destinationFactory == null) {
            retrieveDF();
        }
        return destinationFactory;
    }
    
    private synchronized void retrieveDF() {
        if (destinationFactory == null) {
            DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
            try {
                destinationFactory = dfm.getDestinationFactory(LocalTransportFactory.TRANSPORT_ID);
            } catch (BusException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ObjectBindingConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ObjectBindingConfiguration configuration) {
        this.configuration = configuration;
    }

    public static class OverrideBindingObserver extends ChainInitiationObserver {

        private Binding binding;

        public OverrideBindingObserver(Endpoint endpoint, Binding binding, Bus bus) {
            super(endpoint, bus);
            this.binding = binding;
        }

        @Override
        protected Binding getBinding() {
            return binding;
        }
        
    }
}
