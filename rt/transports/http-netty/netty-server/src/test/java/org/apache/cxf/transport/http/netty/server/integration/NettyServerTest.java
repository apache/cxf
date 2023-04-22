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
package org.apache.cxf.transport.http.netty.server.integration;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NettyServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(NettyServerTest.class);

    static Endpoint ep;

    static Greeter g;

    @BeforeClass
    public static void start() throws Exception {
        Bus b = createStaticBus();
        BusFactory.setThreadDefaultBus(b);
        ep = Endpoint.publish("netty://http://localhost:" + PORT + "/SoapContext/SoapPort",
                new org.apache.hello_world_soap_http.GreeterImpl());

        URL wsdl = NettyServerTest.class.getResource("/wsdl/hello_world.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl);
        assertNotNull("Service is null", service);

        g = service.getSoapPort();
        assertNotNull("Port is null", g);
    }

    @AfterClass
    public static void stop() throws Exception {
        if (g != null) {
            ((java.io.Closeable)g).close();
        }
        if (ep != null) {
            ep.stop();
        }
        ep = null;
    }

    @Test
    public void testInvocation() throws Exception {

        updateAddressPort(g, PORT);
        String response = g.greetMe("test");
        assertEquals("Get a wrong response", "Hello test", response);
    }

    @Test
    public void testGetWsdl() throws Exception {
        URL url = new URL("http://localhost:" + PORT + "/SoapContext/SoapPort?wsdl");

        InputStream in = url.openStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copyAndCloseInput(in, bos);
        String result = bos.toString();
        assertTrue("Expect the SOAPService", result.indexOf("<service name=\"SOAPService\">") > 0);
    }

}
