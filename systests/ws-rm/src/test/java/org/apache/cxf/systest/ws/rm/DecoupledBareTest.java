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

package org.apache.cxf.systest.ws.rm;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;


import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.hello_world_soap_http.DocLitBare;
import org.apache.hello_world_soap_http.DocLitBareGreeterImpl;
import org.apache.hello_world_soap_http.SOAPServiceAddressingDocLitBare;
import org.apache.hello_world_soap_http.types.BareDocumentResponse;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the addition of WS-RM properties to application messages and the
 * exchange of WS-RM protocol messages.
 */
public class DecoupledBareTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    public static final String DECOUPLE_PORT = allocatePort(DecoupledBareTest.class);

    private static final Logger LOG = LogUtils.getLogger(DecoupledBareTest.class);

    public static class Server extends AbstractBusTestServerBase {

        Endpoint ep;
        protected void run()  {
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus("/org/apache/cxf/systest/ws/rm/decoupled_bare.xml");
            BusFactory.setDefaultBus(bus);
            setBus(bus);

            Object implementor = new DocLitBareGreeterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/SoapPort";
            ep = Endpoint.create(implementor);
            Map<String, Object> properties = new HashMap<>();
            properties.put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE);
            ep.setProperties(properties);
            ep.publish(address);
            LOG.info("Published server endpoint.");
        }
        public void tearDown() {
            ep.stop();
            ep = null;
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testDecoupled() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus("/org/apache/cxf/systest/ws/rm/decoupled_bare.xml");
        BusFactory.setDefaultBus(bus);

        SOAPServiceAddressingDocLitBare service = new SOAPServiceAddressingDocLitBare();
        assertNotNull(service);

        DocLitBare greeter = service.getSoapPort();
        updateAddressPort(greeter, PORT);
        ((BindingProvider)greeter).getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE);

        ConnectionHelper.setKeepAliveConnection(greeter, true);

        BareDocumentResponse bareres = greeter.testDocLitBare("MySimpleDocument");
        assertNotNull("no response for operation testDocLitBare", bareres);
        assertEquals("CXF", bareres.getCompany());
        assertTrue(bareres.getId() == 1);
    }
}
