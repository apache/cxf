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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.MultipleEndpointObserver;

public class ServerImpl implements Server {
    private static final Logger LOG = LogUtils.getL7dLogger(ServerImpl.class);    
    private Destination destination;
    private Endpoint endpoint;
    private ServerRegistry serverRegistry;
    private Bus bus;
    private ServerLifeCycleManager slcMgr;
    private InstrumentationManager iMgr;
    private BindingFactory bindingFactory;
    private MessageObserver messageObserver;
    private ManagedEndpoint mep;

    public ServerImpl(Bus bus, 
                      Endpoint endpoint, 
                      DestinationFactory destinationFactory, 
                      MessageObserver observer) throws BusException, IOException {
        this.endpoint = endpoint;
        this.bus = bus;
        this.messageObserver = observer;
        
        initDestination(destinationFactory);
    }
    
    public ServerImpl(Bus bus, 
                      Endpoint endpoint, 
                      DestinationFactory destinationFactory, 
                      BindingFactory bindingFactory) throws BusException, IOException {
        this.endpoint = endpoint;
        this.bus = bus;
        this.bindingFactory = bindingFactory;
        
        initDestination(destinationFactory);
    }

    private void initDestination(DestinationFactory destinationFactory) throws BusException, IOException {
        EndpointInfo ei = endpoint.getEndpointInfo();
        
        //Treat local transport as a special case, transports loaded by transportId can be replaced
        //by local transport when the publishing address is a local transport protocol. 
        //Of course its not an ideal situation here to use a hard-coded prefix. To be refactored.
        if (destinationFactory == null) {
            if (ei.getAddress() != null && ei.getAddress().indexOf("local://") != -1) {
                destinationFactory = bus.getExtension(DestinationFactoryManager.class)
                    .getDestinationFactoryForUri(ei.getAddress());
            }

            if (destinationFactory == null) {
                destinationFactory = bus.getExtension(DestinationFactoryManager.class)
                    .getDestinationFactory(ei.getTransportId());
            }
        }
            
        destination = destinationFactory.getDestination(ei);
        LOG.info("Setting the server's publish address to be " + ei.getAddress());
        serverRegistry = bus.getExtension(ServerRegistry.class);
        
        mep = new ManagedEndpoint(bus, endpoint, this);
        
        slcMgr = bus.getExtension(ServerLifeCycleManager.class);
        if (slcMgr != null) {
            slcMgr.registerListener(mep);
        }
        
        iMgr = bus.getExtension(InstrumentationManager.class);        
        if (iMgr != null) {   
            try {
                iMgr.register(mep);
            } catch (JMException jmex) {
                LOG.log(Level.WARNING, "Registering ManagedEndpoint failed.", jmex);
            }
        }
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public void start() {     
        LOG.fine("Server is starting.");
        
        if (messageObserver != null) {
            destination.setMessageObserver(messageObserver);
        } else {
            bindingFactory.addListener(destination, endpoint);
        }
        
        // register the active server to run
        if (null != serverRegistry) {
            LOG.fine("register the server to serverRegistry ");
            serverRegistry.register(this);
        }
        
        if (slcMgr != null) {
            slcMgr.startServer(this);
        }
    }

    public void stop() {
        LOG.fine("Server is stopping.");
        
        if (slcMgr != null) {
            slcMgr.stopServer(this);
        }
        
        if (null != serverRegistry) {
            LOG.fine("unregister the server to serverRegistry ");
            serverRegistry.unregister(this);
        }

        MessageObserver mo = getDestination().getMessageObserver();
        if (mo instanceof MultipleEndpointObserver) {
            ((MultipleEndpointObserver)mo).getEndpoints().remove(endpoint);
            if (!((MultipleEndpointObserver)mo).getEndpoints().isEmpty()) {
                return;
            }
        }
        getDestination().setMessageObserver(null);
        getDestination().shutdown();
    }
    
    public void destroy() {
        stop();
        
        if (iMgr != null) {   
            try {
                iMgr.unregister(mep);
            } catch (JMException jmex) {
                LOG.log(Level.WARNING, "Unregistering ManagedEndpoint failed.", jmex);
            }
        }
        
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public MessageObserver getMessageObserver() {
        return messageObserver;
    }

    public void setMessageObserver(MessageObserver messageObserver) {
        this.messageObserver = messageObserver;
    }
    
}
