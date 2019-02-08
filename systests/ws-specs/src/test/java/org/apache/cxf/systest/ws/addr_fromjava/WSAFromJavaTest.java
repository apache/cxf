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

package org.apache.cxf.systest.ws.addr_fromjava;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.List;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;


import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.systest.ws.AbstractWSATestBase;
import org.apache.cxf.systest.ws.addr_fromjava.client.AddNumberImpl;
import org.apache.cxf.systest.ws.addr_fromjava.client.AddNumberImplService;
import org.apache.cxf.systest.ws.addr_fromjava.client.AddNumbersException_Exception;
import org.apache.cxf.systest.ws.addr_fromjava.server.Server;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WSAFromJavaTest extends AbstractWSATestBase {
    static final String PORT = allocatePort(Server.class);

    @Before
    public void setUp() throws Exception {
        createBus();
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testAddNumbers() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumberImpl port = getPort();

        assertEquals(3, port.addNumbers(1, 2));

        String expectedOut = "http://cxf.apache.org/input";
        assertTrue(output.toString().indexOf(expectedOut) != -1);

        String expectedIn = "http://cxf.apache.org/output";
        assertTrue(input.toString().indexOf(expectedIn) != -1);
    }

    @Test
    public void testAddNumbersFault() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumberImpl port = getPort();

        try {
            port.addNumbers(-1, 2);
        } catch (AddNumbersException_Exception e) {
            assert true;
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }

        assertTrue(output.toString().indexOf("http://cxf.apache.org/input") != -1);
        String expectedFault =
            "http://server.addr_fromjava.ws.systest.cxf.apache.org/AddNumberImpl/"
            + "addNumbers/Fault/AddNumbersException";
        assertTrue(input.toString(),
                   input.toString().indexOf(expectedFault) != -1);
    }

    @Test
    public void testAddNumbers2() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumberImpl port = getPort();

        assertEquals(3, port.addNumbers2(1, 2));

        String base = "http://server.addr_fromjava.ws.systest.cxf.apache.org/AddNumberImpl";
        String expectedOut = base + "/addNumbers2";
        assertTrue(output.toString().indexOf(expectedOut) != -1);

        String expectedIn = base + "/addNumbers2Response";
        assertTrue(input.toString().indexOf(expectedIn) != -1);
    }

    @Test
    public void testAddNumbers3Fault() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumberImpl port = getPort();

        try {
            port.addNumbers3(-1, 2);
        } catch (AddNumbersException_Exception e) {
            assert true;
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }

        assertTrue(output.toString(), output.toString().indexOf("http://cxf.apache.org/input") != -1);
        assertTrue(input.toString(), input.toString().indexOf("http://cxf.apache.org/fault3") != -1);
    }

    @Test
    public void testAddNumbersJaxWsContext() throws Exception {
        ByteArrayOutputStream output = setupOutLogging();

        AddNumberImpl port = getPort();

        BindingProvider bp = (BindingProvider)port;
        java.util.Map<String, Object> requestContext = bp.getRequestContext();
        requestContext.put(BindingProvider.SOAPACTION_URI_PROPERTY, "cxf");

        try {
            assertEquals(3, port.addNumbers(1, 2));
            fail("Should have thrown an ActionNotSupported exception");
        } catch (SOAPFaultException ex) {
            //expected
        }
        assertLogContains(output.toString(), "//wsa:Action", "cxf");
        assertTrue(output.toString(), output.toString().indexOf("SOAPAction=\"cxf\"") != -1);
    }

    private AddNumberImpl getPort() throws Exception {
        URL wsdl = getClass().getResource("/wsdl_systest_wsspec/add_numbers-fromjava.wsdl");
        assertNotNull("WSDL is null", wsdl);

        AddNumberImplService service = new AddNumberImplService(wsdl);
        assertNotNull("Service is null ", service);

        AddNumberImpl port = service.getAddNumberImplPort();
        updateAddressPort(port, PORT);
        return port;
    }

    @Test
    public void testUnmatchedActions() throws Exception {
        AddNumberImpl port = getPort();

        BindingProvider bp = (BindingProvider)port;
        java.util.Map<String, Object> requestContext = bp.getRequestContext();
        requestContext.put(BindingProvider.SOAPACTION_URI_PROPERTY,
                           "http://cxf.apache.org/input4");
        try {
            //CXF-2035
            port.addNumbers3(-1, -1);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unexpected wrapper"));
        }
    }

    @Test
    public void testFaultFromNonAddressService() throws Exception {
        new LoggingFeature().initialize(this.getBus());
        AddNumberImpl port = getPort();
        java.util.Map<String, Object> requestContext = ((BindingProvider)port).getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                           "http://localhost:" + PORT + "/AddNumberImplPort-noaddr");

        long start = System.currentTimeMillis();
        port.addNumbers(1, 2);
        try {
            port.addNumbers3(-1, -1);
        } catch (Exception ex) {
            //ignore, expected
        }
        long end = System.currentTimeMillis();
        assertTrue((end - start) < 50000);
    }

    static class RemoveRelatesToHeaderInterceptor extends AbstractSoapInterceptor {
        RemoveRelatesToHeaderInterceptor() {
            super(Phase.READ);
            addAfter(ReadHeadersInterceptor.class.getName());
        }
        public void handleMessage(SoapMessage message) throws Fault {
            List<Header> headers = message.getHeaders();
            Header h2 = null;
            for (Header h : headers) {
                if ("RelatesTo".equals(h.getName().getLocalPart())) {
                    h2 = h;
                }
            }
            headers.remove(h2);
        }
    }

    @Test
    public void testNoRelatesToHeader() throws Exception {
        new LoggingFeature().initialize(this.getBus());
        AddNumberImpl port = getPort();

        Client c = ClientProxy.getClient(port);
        c.getInInterceptors().add(new RemoveRelatesToHeaderInterceptor());


        long start = System.currentTimeMillis();
        port.addNumbers(1, 2);
        try {
            port.addNumbers3(-1, -1);
        } catch (Exception ex) {
            //ignore, expected
        }
        long end = System.currentTimeMillis();
        assertTrue((end - start) < 50000);
    }

}