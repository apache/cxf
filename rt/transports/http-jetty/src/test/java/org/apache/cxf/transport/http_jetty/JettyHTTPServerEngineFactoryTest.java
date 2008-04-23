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

import java.net.URL;

import junit.framework.Assert;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;



public class JettyHTTPServerEngineFactoryTest
        extends Assert {
    Bus bus;
    
    @BeforeClass
    public static void classUp() {
        // Get rid of any notion of a default bus set by other 
        // rogue tests.
        BusFactory.setDefaultBus(null);
    }
    
    @AfterClass
    public static void classDown() {
        // Clean up.
        BusFactory.setDefaultBus(null);
    }
    
    @After
    public void tearDown() {
        if (bus != null) {
            bus.shutdown(false);
            bus = null;
        }
    }
    
    /**
     * This test makes sure that a default Spring initialized bus will 
     * have the JettyHTTPServerEngineFactory (absent of <httpj:engine-factory>
     * configuration.
     */
    @Test
    public void testMakeSureTransportFactoryHasEngineFactory() {
        bus = BusFactory.getDefaultBus(true);
        
        assertNotNull("Cannot get bus", bus);
        
        try {
            // Make sure we got the Transport Factory.
            DestinationFactoryManager destFM = 
                bus.getExtension(DestinationFactoryManager.class);
            assertNotNull("Cannot get DestinationFactoryManager", destFM);
            DestinationFactory destF = 
                destFM.getDestinationFactory(
                        "http://schemas.xmlsoap.org/soap/http");
            assertNotNull("No DestinationFactory", destF);
            assertTrue(JettyHTTPTransportFactory.class.isInstance(destF));
        } catch (BusException e) {
            fail("Asserting Transport Factory" + e);
        }
        // And the JettyHTTPServerEngineFactory should be there.
        JettyHTTPServerEngineFactory factory =
            bus.getExtension(JettyHTTPServerEngineFactory.class);
        assertNotNull("EngineFactory is not configured.", factory);
    }
    
    /**
     * This test makes sure that with a <httpj:engine-factory bus="cxf">
     * that the bus is configured with the rightly configured Jetty
     * HTTP Server Engine Factory.  Port 1234 should have be configured
     * for TLS.
     */
    @Test
    public void testMakeSureTransportFactoryHasEngineFactoryConfigured() {
        
        // This file configures the factory to configure
        // port 1234 with default TLS.
        
        URL config = getClass().getResource("server-engine-factory.xml");
        
        bus = new SpringBusFactory().createBus(config, true);
        
        JettyHTTPServerEngineFactory factory =
            bus.getExtension(JettyHTTPServerEngineFactory.class);
        
        assertNotNull("EngineFactory is not configured.", factory);
        
        // The Engine for port 1234 should be configured for TLS.
        // This will throw an error if it is not.
        JettyHTTPServerEngine engine = null;
        try {
            engine = factory.createJettyHTTPServerEngine(1234, "https");
        } catch (Exception e) {
            fail("Error getting factory" + e);
        }
        assertNotNull("Engine is not available.", engine);
        assertEquals(1234, engine.getPort());
        assertEquals("Not https", "https", engine.getProtocol());
        
        try {
            engine = factory.createJettyHTTPServerEngine(1234, "http");
            fail("The engine's protocol should be https");
        } catch (Exception e) {
            // expect the exception
        }
    }
    
    @Test
    public void testAnInvalidConfiguresfile() {
        
        // This file configures the factory to configure
        // port 1234 with default TLS.
        
        URL config = getClass().getResource("invalid-engines.xml");

        bus = new SpringBusFactory().createBus(config);
            
        JettyHTTPServerEngineFactory factory =
            bus.getExtension(JettyHTTPServerEngineFactory.class);
            
        assertNotNull("EngineFactory is not configured.", factory);


        // The Engine for port 1234 was configured for TLS.
        // and the connector which is injected into the engine
        // should be an instance of SslSocketConnector
        // here will throw an error if it is not.

        
        try {
            factory.createJettyHTTPServerEngine(1234, "https");
            fail("A configure error should be thrown here ");            
        } catch (Exception e) {
            // experct the exception
        }
        
    }   

}
