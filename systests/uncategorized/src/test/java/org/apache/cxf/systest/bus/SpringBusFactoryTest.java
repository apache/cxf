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

package org.apache.cxf.systest.bus;


import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.cxf.wsdl.WSDLManager;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringBusFactoryTest extends Assert {
    
    @Test
    public void testKnownExtensions() throws BusException {
        Bus bus = new SpringBusFactory().createBus();
        assertNotNull(bus);
        
        checkBindingExtensions(bus);
        
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        assertNotNull("No destination factory manager", dfm);
        ConduitInitiatorManager cim = bus.getExtension(ConduitInitiatorManager.class);
        assertNotNull("No conduit initiator manager", cim);
        
        checkTransportFactories(bus);
        checkOtherCoreExtensions(bus);
        //you should include instumentation extenstion to get the instrumentation manager 
        assertNotNull("No instrumentation manager", bus.getExtension(InstrumentationManager.class));
    }
    
    @Test
    public void testLoadBusWithServletApplicationContext() throws BusException {
        ClassPathXmlApplicationContext ctx = 
            new ClassPathXmlApplicationContext(new String[] {"/org/apache/cxf/systest/bus/servlet.xml"});
        Bus bus = new SpringBusFactory(ctx).createBus();
        checkBindingExtensions(bus);
        checkHTTPTransportFactories(bus);
        checkOtherCoreExtensions(bus);
    }
    
    private void checkBindingExtensions(Bus bus) throws BusException {
        BindingFactoryManager bfm = bus.getExtension(BindingFactoryManager.class);  
        assertNotNull("No binding factory manager", bfm);
        assertNotNull("binding factory not available", 
                      bfm.getBindingFactory("http://schemas.xmlsoap.org/wsdl/soap/"));
        try {
            bfm.getBindingFactory("http://cxf.apache.org/unknown");
        } catch (BusException ex) {
            // expected
        }
    }
    
    private void checkOtherCoreExtensions(Bus bus) throws BusException {
        assertNotNull("No wsdl manager", bus.getExtension(WSDLManager.class));
        assertNotNull("No phase manager", bus.getExtension(PhaseManager.class));
        assertNotNull("No workqueue manager", bus.getExtension(WorkQueueManager.class));
        assertNotNull("No lifecycle manager", bus.getExtension(BusLifeCycleManager.class));
        assertNotNull("No service registry", bus.getExtension(ServerRegistry.class));
        
    }
    
    private void checkHTTPTransportFactories(Bus bus) throws BusException {
        ConduitInitiatorManager cim = bus.getExtension(ConduitInitiatorManager.class);
        assertNotNull("No conduit initiator manager", cim);
        
        assertNotNull("conduit initiator not available", 
                      cim.getConduitInitiator("http://schemas.xmlsoap.org/wsdl/soap/http"));
        assertNotNull("conduit initiator not available", 
                      cim.getConduitInitiator("http://schemas.xmlsoap.org/wsdl/http/"));
        assertNotNull("conduit initiator not available", 
                      cim.getConduitInitiator("http://cxf.apache.org/transports/http/configuration"));
        
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        assertNotNull("No destination factory manager", dfm);
        
        assertNotNull("destination factory not available", 
                      dfm.getDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/"));
        assertNotNull("destination factory not available", 
                      dfm.getDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/http"));
        assertNotNull("destination factory not available", 
                      dfm.getDestinationFactory("http://schemas.xmlsoap.org/wsdl/http/"));
        assertNotNull("destination factory not available", 
                      dfm.getDestinationFactory("http://cxf.apache.org/transports/http/configuration"));
    }
    
    private void checkTransportFactories(Bus bus) throws BusException {
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        assertNotNull("No destination factory manager", dfm);
        ConduitInitiatorManager cim = bus.getExtension(ConduitInitiatorManager.class);
        assertNotNull("No conduit initiator manager", cim);
        
        try {
            cim.getConduitInitiator("http://cxf.apache.org/unknown");
        } catch (BusException ex) {
            // expected
        }
      
        try {
            dfm.getDestinationFactory("http://cxf.apache.org/unknown");
        } catch (BusException ex) {
            // expected
        }       
        
        // not sure that we need this - Dan Diephouse
        //assertNotNull("conduit initiator not available", 
        //cim.getConduitInitiator("http://schemas.xmlsoap.org/wsdl/soap/"));
         
        
        assertNotNull("conduit initiator not available", 
                      cim.getConduitInitiator("http://cxf.apache.org/bindings/xformat"));
        assertNotNull("conduit initiator not available", 
                      cim.getConduitInitiator("http://cxf.apache.org/transports/jms"));
        assertNotNull("conduit initiator not available", 
                      cim.getConduitInitiator("http://cxf.apache.org/transports/jms/configuration"));
        
        

        assertNotNull("destination factory not available", 
                      dfm.getDestinationFactory("http://cxf.apache.org/bindings/xformat"));
        assertNotNull("destination factory not available", 
                      dfm.getDestinationFactory("http://cxf.apache.org/transports/jms"));
        assertNotNull("destination factory not available", 
                      dfm.getDestinationFactory("http://cxf.apache.org/transports/jms/configuration"));
        
    }
    
}
