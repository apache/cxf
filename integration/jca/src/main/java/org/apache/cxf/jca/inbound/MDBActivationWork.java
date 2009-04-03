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
package org.apache.cxf.jca.inbound;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.EndpointUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

/**
 *
 * MDBActivationWork is a type of {@link Work} that is executed by 
 * {@link javax.resource.spi.work.WorkManager}.  MDBActivationWork
 * starts an CXF service endpoint to accept inbound calls for
 * the JCA connector.
 * 
 */
public class MDBActivationWork implements Work {
    
    private static final Logger LOG = LogUtils.getL7dLogger(MDBActivationWork.class);

    private MDBActivationSpec spec;
    private MessageEndpointFactory endpointFactory;

    private Map<String, InboundEndpoint> endpoints;

    public MDBActivationWork(MDBActivationSpec spec, 
            MessageEndpointFactory endpointFactory, 
            Map<String, InboundEndpoint> endpoints) {
        this.spec = spec;
        this.endpointFactory = endpointFactory;
        this.endpoints = endpoints;
    }

    public void release() {

    }

    /**
     * Performs the work
     */
    public void run() {
        MDBInvoker invoker = createInvoker();
        MessageEndpoint mep = invoker.getMessageEndpoint();
        if (mep == null) {
            return;            
        }
        
        ClassLoader savedClassLoader = null;

        try {
            savedClassLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader classLoader = mep.getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            activate(invoker, classLoader);
        } finally {
            invoker.releaseEndpoint(mep);
            if (savedClassLoader != null) {
                Thread.currentThread().setContextClassLoader(savedClassLoader);
            }
        }
    }
    
    /**
     * @param endpoint
     * @param classLoader 
     */
    private void activate(MDBInvoker invoker, ClassLoader classLoader) {
        Class<?> serviceClass = null;
        if (spec.getServiceInterfaceClass() != null) {
            try {
                serviceClass = Class.forName(spec.getServiceInterfaceClass(),
                        false, classLoader);
            } catch (ClassNotFoundException e) {
                LOG.severe("Failed to activate service endpoint " 
                        + spec.getDisplayName() 
                        + " due to unable to endpoint listener.");
                return;
            } 
        }
        
        Bus bus = null;
        if (spec.getBusConfigLocation() != null) {
            URL url = classLoader.getResource(spec.getBusConfigLocation());
            if (url == null) {
                LOG.warning("Unable to get bus configuration from " 
                        + spec.getBusConfigLocation());
            } else {    
                bus = new SpringBusFactory().createBus(url);
            }
        }
        
        if (bus == null) {
            bus = BusFactory.getDefaultBus();
        }

        Server server = createServer(bus, serviceClass, invoker);
        
        if (server == null) {
            LOG.severe("Failed to create CXF facade service endpoint.");
            return;
        }
        
        server.start();
        
        // save the server for clean up later
        endpoints.put(spec.getDisplayName(), new InboundEndpoint(server, invoker));
    }


    private Server createServer(Bus bus, Class<?> serviceClass, MDBInvoker invoker) {

        // create server bean factory
        ServerFactoryBean factory = null;
        if (serviceClass != null && EndpointUtils.hasWebServiceAnnotation(serviceClass)) {
            factory = new JaxWsServerFactoryBean();
        } else {
            factory = new ServerFactoryBean();
        }
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Creating a server using " + factory.getClass().getName());
        }
        
        if (serviceClass != null) {
            factory.setServiceClass(serviceClass);
        }
        
        if (spec.getWsdlLocation() != null) {   
            factory.setWsdlLocation(spec.getWsdlLocation());
        }
        
        if (spec.getAddress() != null) {
            factory.setAddress(spec.getAddress());
        }
         
        factory.setBus(bus);
       
        if (spec.getEndpointName() != null) {
            factory.setEndpointName(QName.valueOf(spec.getEndpointName()));
        }
        
        if (spec.getSchemaLocations() != null) {
            factory.setSchemaLocations(getListOfString(spec.getSchemaLocations()));
        }
        
        if (spec.getServiceName() != null) {
            factory.setServiceName(QName.valueOf(spec.getServiceName()));
        }
              
        factory.setInvoker(invoker);

        // Don't start the server yet
        factory.setStart(false);
        
        Server retval = null;
        if (factory instanceof JaxWsServerFactoryBean) {
            retval = createServerFromJaxwsEndpoint((JaxWsServerFactoryBean)factory);
        } else {
            retval = factory.create();
        }
        
        return retval;
    }

    /*
     * Creates a server from EndpointImpl so that jaxws-endpoint config can be injected.
     */
    private Server createServerFromJaxwsEndpoint(JaxWsServerFactoryBean factory) {
        
        EndpointImpl endpoint = new EndpointImpl(factory.getBus(), null, factory);
        
        endpoint.setWsdlLocation(factory.getWsdlURL());
        endpoint.setImplementorClass(factory.getServiceClass());
        endpoint.setEndpointName(factory.getEndpointName());
        endpoint.setServiceName(factory.getServiceName());
        endpoint.setInvoker(factory.getInvoker());
        endpoint.setSchemaLocations(factory.getSchemaLocations());
        
        return endpoint.getServer(factory.getAddress());
    }

    /**
     * @param str
     * @return
     */
    private List<String> getListOfString(String str) {
        if (str == null) {
            return null;
        }
        
        return Arrays.asList(str.split(","));
    }

    /**
     * @param endpoint
     * @return
     */
    private MDBInvoker createInvoker() {
        MDBInvoker answer = null;
        if (spec instanceof DispatchMDBActivationSpec) {
            answer = new DispatchMDBInvoker(endpointFactory, 
                    ((DispatchMDBActivationSpec)spec).getTargetBeanJndiName());
        } else {
            answer = new MDBInvoker(endpointFactory);
        }
        return answer;
    }

}
