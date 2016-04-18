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

package org.apache.cxf.transport.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.servlet.ServletDestinationFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.http.AddressType;

/**
 *
 */
@NoJSR250Annotations
public class HTTPTransportFactory 
    extends AbstractTransportFactory 
    implements ConduitInitiator, DestinationFactory {
    

    public static final List<String> DEFAULT_NAMESPACES 
        = Arrays.asList(
            "http://cxf.apache.org/transports/http",
            "http://cxf.apache.org/transports/http/configuration",
            "http://schemas.xmlsoap.org/wsdl/http",
            "http://schemas.xmlsoap.org/wsdl/http/"
        );
        
    private static final Logger LOG = LogUtils.getL7dLogger(HTTPTransportFactory.class);
    
    /**
     * This constant holds the prefixes served by this factory.
     */
    private static final Set<String> URI_PREFIXES = new HashSet<String>();
    static {
        URI_PREFIXES.add("http://");
        URI_PREFIXES.add("https://");
    }

    protected DestinationRegistry registry;
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock r = lock.readLock();
    private final Lock w = lock.writeLock();
    
    public HTTPTransportFactory() {
        this(new DestinationRegistryImpl());
    }
    
    public HTTPTransportFactory(DestinationRegistry registry) {
        this(DEFAULT_NAMESPACES, registry);
    }
    
    protected HTTPTransportFactory(List<String> transportIds, DestinationRegistry registry) {
        super(transportIds);
        if (registry == null) {
            registry = new DestinationRegistryImpl();
        }
        this.registry = registry;
    }
    
    public DestinationRegistry getRegistry() {
        return registry;
    }
    
    public void setRegistry(DestinationRegistry newRegistry) {
        w.lock();
        try {
            if (registry.getDestinations().isEmpty()) {
                this.registry = newRegistry;
            } else {
                String m = new org.apache.cxf.common.i18n.Message("CANNOT_CHANGE_REGISTRY_ALREADY_IN_USE",
                                                                  LOG).toString();
                LOG.log(Level.SEVERE, m);
                throw new RuntimeException(m);
            }
        } finally {
            w.unlock();
        }
    }
    
    /**
     * This call is used by CXF ExtensionManager to inject the activationNamespaces
     * @param ans The transport ids.
     */
    public void setActivationNamespaces(Collection<String> ans) {
        setTransportIds(new ArrayList<String>(ans));
    }

    public EndpointInfo createEndpointInfo(
        ServiceInfo serviceInfo, 
        BindingInfo b, 
        List<?>     ees
    ) {
        if (ees != null) {
            for (Iterator<?> itr = ees.iterator(); itr.hasNext();) {
                Object extensor = itr.next();
    
                if (extensor instanceof AddressType) {
                    final AddressType httpAdd = (AddressType)extensor;
    
                    EndpointInfo info = 
                        new HttpEndpointInfo(serviceInfo, 
                                "http://schemas.xmlsoap.org/wsdl/http/");
                    info.setAddress(httpAdd.getLocation());
                    info.addExtensor(httpAdd);
                    return info;
                }
            }
        }
        
        HttpEndpointInfo hei = new HttpEndpointInfo(serviceInfo, 
            "http://schemas.xmlsoap.org/wsdl/http/");
        AddressType at = new AddressType();
        hei.addExtensor(at);
        
        return hei;
    }

    public void createPortExtensors(EndpointInfo ei, Service service) {
        // TODO
    }

    public Set<String> getUriPrefixes() {
        return URI_PREFIXES;
    }

    /**
     * This call uses the Configurer from the bus to configure
     * a bean.
     * 
     * @param bean
     */
    protected void configure(Bus b, Object bean) {
        configure(b, bean, null, null);
    }
    
    protected void configure(Bus bus, Object bean, String name, String extraName) {
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(name, bean);
            if (extraName != null) {
                configurer.configureBean(extraName, bean);
            }
        }
    }
    
    private static class HttpEndpointInfo extends EndpointInfo {
        AddressType saddress;
        HttpEndpointInfo(ServiceInfo serv, String trans) {
            super(serv, trans);
        }
        public void setAddress(String s) {
            super.setAddress(s);
            if (saddress != null) {
                saddress.setLocation(s);
            }
        }

        public void addExtensor(Object el) {
            super.addExtensor(el);
            if (el instanceof AddressType) {
                saddress = (AddressType)el;
            }
        }
    }    
    
    /**
     * This call creates a new HTTPConduit for the endpoint. It is equivalent
     * to calling getConduit without an EndpointReferenceType.
     */
    public Conduit getConduit(EndpointInfo endpointInfo, Bus bus) throws IOException {
        return getConduit(endpointInfo, endpointInfo.getTarget(), bus);
    }

    /**
     * This call creates a new HTTP Conduit based on the EndpointInfo and
     * EndpointReferenceType.
     * TODO: What are the formal constraints on EndpointInfo and 
     * EndpointReferenceType values?
     */
    public Conduit getConduit(
            EndpointInfo endpointInfo,
            EndpointReferenceType target,
            Bus bus
    ) throws IOException {
        
        HTTPConduitFactory factory = findFactory(endpointInfo, bus);
        HTTPConduit conduit = null;
        if (factory != null) {
            conduit = factory.createConduit(this, bus, endpointInfo, target);
        }
        if (conduit == null) {
            conduit = new URLConnectionHTTPConduit(bus, endpointInfo, target);
        }

        // Spring configure the conduit.  
        String address = conduit.getAddress();
        if (address != null && address.indexOf('?') != -1) {
            address = address.substring(0, address.indexOf('?'));
        }
        HTTPConduitConfigurer c1 = bus.getExtension(HTTPConduitConfigurer.class);
        if (c1 != null) {
            c1.configure(conduit.getBeanName(), address, conduit);
        }
        configure(bus, conduit, conduit.getBeanName(), address);
        conduit.finalizeConfig();
        return conduit;
    }
    
    protected HTTPConduitFactory findFactory(EndpointInfo endpointInfo, Bus bus) {
        HTTPConduitFactory f = endpointInfo.getProperty(HTTPConduitFactory.class.getName(), HTTPConduitFactory.class);
        if (f == null) {
            f = bus.getExtension(HTTPConduitFactory.class);
        }
        return f;
    }
    public Destination getDestination(EndpointInfo endpointInfo, Bus bus) throws IOException {
        if (endpointInfo == null) {
            throw new IllegalArgumentException("EndpointInfo cannot be null");
        }
        r.lock();
        try {
            synchronized (registry) {
                AbstractHTTPDestination d = registry.getDestinationForPath(endpointInfo.getAddress());
                if (d == null) {
                    HttpDestinationFactory jettyFactory = bus.getExtension(HttpDestinationFactory.class);
                    String addr = endpointInfo.getAddress();
                    if (jettyFactory == null && addr != null && addr.startsWith("http")) {
                        String m = 
                            new org.apache.cxf.common.i18n.Message("NO_HTTP_DESTINATION_FACTORY_FOUND",
                                                                   LOG).toString();
                        LOG.log(Level.SEVERE, m);
                        throw new IOException(m);
                    }
                    HttpDestinationFactory factory = null;
                    if (jettyFactory != null && (addr == null || addr.startsWith("http"))) {
                        factory = jettyFactory;
                    } else {
                        factory = new ServletDestinationFactory();
                    }
                    
                    d = factory.createDestination(endpointInfo, bus, registry);
                    registry.addDestination(d);
                    configure(bus, d);
                    d.finalizeConfig();
                }
                return d;
            }
        } finally {
            r.unlock();
        }
    }

}
