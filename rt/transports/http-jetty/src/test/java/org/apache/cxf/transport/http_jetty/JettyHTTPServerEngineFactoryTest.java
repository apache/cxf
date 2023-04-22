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
import java.util.Collection;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;



public class JettyHTTPServerEngineFactoryTest {
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
    public void testMakeSureTransportFactoryHasEngineFactory() throws Exception {
        bus = BusFactory.getDefaultBus(true);

        assertNotNull("Cannot get bus", bus);

        // Make sure we got the Transport Factory.
        DestinationFactoryManager destFM =
            bus.getExtension(DestinationFactoryManager.class);
        assertNotNull("Cannot get DestinationFactoryManager", destFM);
        DestinationFactory destF =
            destFM.getDestinationFactory(
                    "http://cxf.apache.org/transports/http");
        assertNotNull("No DestinationFactory", destF);
        assertTrue(HTTPTransportFactory.class.isInstance(destF));

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
    public void testMakeSureTransportFactoryHasEngineFactoryConfigured() throws Exception {

        // This file configures the factory to configure
        // port 1234 with default TLS.

        URL config = getClass().getResource("server-engine-factory.xml");

        bus = new SpringBusFactory().createBus(config, true);

        JettyHTTPServerEngineFactory factory =
            bus.getExtension(JettyHTTPServerEngineFactory.class);

        assertNotNull("EngineFactory is not configured.", factory);

        // The Engine for port 1234 should be configured for TLS.
        // This will throw an error if it is not.
        JettyHTTPServerEngine engine = factory.createJettyHTTPServerEngine(1234, "https");

        assertNotNull("Engine is not available.", engine);
        assertEquals(1234, engine.getPort());
        assertEquals("Not https", "https", engine.getProtocol());

        try {
            factory.createJettyHTTPServerEngine(1234, "http");
            fail("The engine's protocol should be https");
        } catch (Exception e) {
            // expect the exception
        }
    }

    @Test
    public void testMakeSureJetty9ConnectorConfigured() throws Exception {


        URL config = getClass().getResource("server-engine-factory-jetty9-connector.xml");

        bus = new SpringBusFactory().createBus(config, true);

        JettyHTTPServerEngineFactory factory =
            bus.getExtension(JettyHTTPServerEngineFactory.class);

        assertNotNull("EngineFactory is not configured.", factory);

        JettyHTTPServerEngine engine = factory.createJettyHTTPServerEngine(1234, "http");

        assertNotNull("Engine is not available.", engine);
        assertEquals(1234, engine.getPort());
        assertEquals("Not http", "http", engine.getProtocol());
        Connector connector = engine.getConnector();
        Collection<ConnectionFactory> connectionFactories = connector.getConnectionFactories();
        assertEquals("Has one HttpConnectionFactory", 1, connectionFactories.size());
        ConnectionFactory connectionFactory = connectionFactories.iterator().next();
        assertTrue(connectionFactory instanceof HttpConnectionFactory);
        HttpConfiguration httpConfiguration = ((HttpConnectionFactory)connectionFactory).getHttpConfiguration();
        assertEquals("Has one ForwardedRequestCustomizer", 1, httpConfiguration.getCustomizers().size());
        assertTrue(httpConfiguration.getCustomizers().iterator().next()
                   instanceof org.eclipse.jetty.server.ForwardedRequestCustomizer);
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
    }

}
