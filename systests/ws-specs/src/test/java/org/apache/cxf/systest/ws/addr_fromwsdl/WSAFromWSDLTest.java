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

package org.apache.cxf.systest.ws.addr_fromwsdl;

import java.io.ByteArrayOutputStream;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.systest.ws.AbstractWSATestBase;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersFault_Exception;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersPortType;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersService;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersService.AddNumbersPortTypeProxy;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WSAFromWSDLTest extends AbstractWSATestBase {
    static final String PORT = allocatePort(Server.class);
    static final String INVALID_PORT = allocatePort(Server.class, 1);

    private static final String BASE_URI = "http://apache.org/cxf/systest/ws/addr_feature/"
        + "AddNumbersPortType/";

    private final QName serviceName = new QName("http://apache.org/cxf/systest/ws/addr_feature/",
                                                "AddNumbersService");

    @Before
    public void setUp() throws Exception {
        createBus();
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @Test
    public void testAddNumbers() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getPort();

        assertEquals(3, port.addNumbers(1, 2));

        String expectedOut = BASE_URI + "addNumbersRequest";
        String expectedIn = BASE_URI + "addNumbersResponse";

        assertTrue(output.toString().indexOf(expectedOut) != -1);
        assertTrue(input.toString().indexOf(expectedIn) != -1);
    }

    @Test
    public void testAddNumbers2() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getPort();

        assertEquals(3, port.addNumbers2(1, 2));

        String expectedOut = BASE_URI + "add2In";
        String expectedIn = BASE_URI + "add2Out";

        assertTrue(output.toString().indexOf(expectedOut) != -1);
        assertTrue(input.toString().indexOf(expectedIn) != -1);
    }


    @Test
    public void testAddNumbers3() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getPort();

        assertEquals(3, port.addNumbers3(1, 2));

        String expectedOut = "3in";
        String expectedIn = "3out";

        assertTrue(output.toString().indexOf(expectedOut) != -1);
        assertTrue(input.toString().indexOf(expectedIn) != -1);
    }


    @Test
    public void testAddNumbersFault() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getPort();

        try {
            port.addNumbers(-1, 2);
        } catch (AddNumbersFault_Exception ex) {
            assert true;
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }

        String expectedOut = BASE_URI + "addNumbersRequest";
        String expectedIn = BASE_URI + "addNumbers/Fault/addNumbersFault";

        assertTrue(output.toString().indexOf(expectedOut) != -1);
        assertTrue(input.toString().indexOf(expectedIn) != -1);
    }

    @Test
    public void testAddNumbersFault3() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        try (AddNumbersPortTypeProxy port = getPort()) {

            try {
                port.addNumbers3(-1, 2);
            } catch (AddNumbersFault_Exception ex) {
                assert true;
            } catch (Exception e) {
                e.printStackTrace();
                assert false;
            }

            String expectedOut = "3in";
            String expectedIn = "3fault";

            assertTrue(output.toString(), output.toString().indexOf(expectedOut) != -1);
            assertTrue(input.toString(), input.toString().indexOf(expectedIn) != -1);
        }
    }

    @Test
    public void testAnonToNonAnon() throws Exception {
        try (AddNumbersPortTypeProxy port = getPort()) {
            port.getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                     "http://localhost:" + PORT + "/jaxws/addNonAnon");
            try {
                port.addNumbers3(-1, 2);
            } catch (SOAPFaultException e) {
                assertTrue(e.getFault().getFaultCode().contains("OnlyNonAnonymousAddressSupported"));
            }
        }
    }
    @Test
    public void testNonAnonToAnon() throws Exception {
        try (AddNumbersPortTypeProxy port = getPort()) {
            port.getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                     "http://localhost:" + PORT + "/jaxws/addAnon");

            AddressingProperties maps = new AddressingProperties();
            EndpointReferenceType ref = new EndpointReferenceType();
            AttributedURIType add = new AttributedURIType();
            add.setValue("http://localhost:" + INVALID_PORT + "/not/a/real/url");
            ref.setAddress(add);
            maps.setReplyTo(ref);
            maps.setFaultTo(ref);

            port.getRequestContext()
                .put("javax.xml.ws.addressing.context", maps);

            try {
                port.addNumbers3(-1, 2);
            } catch (SOAPFaultException e) {
                assertTrue(e.getFault().getFaultCode().contains("OnlyAnonymousAddressSupported"));
            }
        }
    }
    private AddNumbersPortTypeProxy getPort()throws Exception {
        URL wsdl = getClass().getResource("/wsdl_systest_wsspec/add_numbers.wsdl");
        assertNotNull("WSDL is null", wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull("Service is null ", service);
        AddNumbersPortTypeProxy port = service.getAddNumbersPort(new AddressingFeature());
        updateAddressPort(port, PORT);
        return port;
    }
}
