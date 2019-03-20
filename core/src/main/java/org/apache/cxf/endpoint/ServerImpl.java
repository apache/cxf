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

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.logging.RegexLoggingFilter;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.MultipleEndpointObserver;

public class ServerImpl implements Server {
    private static final Logger LOG = LogUtils.getL7dLogger(ServerImpl.class);

    protected final Endpoint endpoint;
    protected final Bus bus;
    protected final BindingFactory bindingFactory;

    private Destination destination;
    private ServerRegistry serverRegistry;
    private ServerLifeCycleManager slcMgr;
    private InstrumentationManager iMgr;
    private ManagedEndpoint mep;
    private boolean stopped = true;

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

        destination = destinationFactory.getDestination(ei, bus);
        String wantFilter = ei.getAddress();
        
        if (wantFilter != null && wantFilter.startsWith("jms")) {
            RegexLoggingFilter filter = new RegexLoggingFilter();
            filter.setPattern("jms(.*?)password=+([^ ]+)");
            filter.setGroup(2);
            wantFilter = filter.filter(wantFilter).toString();
        }
        LOG.info("Setting the server's publish address to be " + wantFilter);
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
        if (!stopped) {
            return;
        }
        LOG.fine("Server is starting.");

        bindingFactory.addListener(destination, endpoint);

        // register the active server to run
        if (null != serverRegistry) {
            LOG.fine("register the server to serverRegistry ");
            serverRegistry.register(this);
        }
        if (slcMgr == null) {
            slcMgr = bus.getExtension(ServerLifeCycleManager.class);
            if (slcMgr != null && mep != null) {
                slcMgr.registerListener(mep);
            }
        }
        if (slcMgr != null) {
            slcMgr.startServer(this);
        }
        stopped = false;
    }

    public boolean isStopped() {
        return stopped;
    }
    public boolean isStarted() {
        return !stopped;
    }

    public void stop() {
        if (stopped) {
            return;
        }

        LOG.fine("Server is stopping.");

        for (Closeable c : endpoint.getCleanupHooks()) {
            try {
                c.close();
            } catch (IOException e) {
                //ignore
            }
        }
        if (slcMgr != null) {
            slcMgr.stopServer(this);
        }

        MessageObserver mo = getDestination().getMessageObserver();
        if (mo instanceof MultipleEndpointObserver) {
            ((MultipleEndpointObserver)mo).getEndpoints().remove(endpoint);
            if (((MultipleEndpointObserver)mo).getEndpoints().isEmpty()) {
                getDestination().setMessageObserver(null);
            }
        } else {
            getDestination().setMessageObserver(null);
        }
        stopped = true;
    }

    public void destroy() {
        stop();
        // we should shutdown the destination here
        getDestination().shutdown();

        if (null != serverRegistry) {
            LOG.fine("unregister the server to serverRegistry ");
            serverRegistry.unregister(this);
        }

        if (iMgr != null) {
            try {
                iMgr.unregister(mep);
            } catch (JMException jmex) {
                LOG.log(Level.WARNING, "Unregistering ManagedEndpoint failed.", jmex);
            }
            iMgr = null;
        }

    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

}
