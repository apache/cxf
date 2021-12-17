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

package org.apache.cxf.systest.ws.addr_disable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.AddressingFeature;
import org.apache.cxf.systest.ws.AbstractWSATestBase;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersPortType;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersService;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WSADisableTest extends AbstractWSATestBase {
    static final String PORT = allocatePort(Server.class);

    private final QName serviceName = new QName("http://apache.org/cxf/systest/ws/addr_feature/",
                                                "AddNumbersService");

    @Before
    public void setUp() throws Exception {
        createBus();
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testDisableServerSide() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getService().getAddNumbersPort();

        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                        "http://localhost:"
                                                        + PORT + "/jaxws/add");

        assertEquals(3, port.addNumbers(1, 2));

        String expectedOut = "http://apache.org/cxf/systest/ws/addr_feature/AddNumbersPortType/addNumbersRequest";
        String expectedIn = "http://www.w3.org/2005/08/addressing";

        assertLogContains(output.toString(), "//wsa:Action", expectedOut);
        assertTrue(input.toString().indexOf(expectedIn) == -1);
    }

    @Test
    public void testDisableAll() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getService().getAddNumbersPort(new AddressingFeature(false));

        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                        "http://localhost:"
                                                        + PORT + "/jaxws/add");

        assertEquals(3, port.addNumbers(1, 2));

        String expectedOut = "http://www.w3.org/2005/08/addressing";
        String expectedIn = "http://www.w3.org/2005/08/addressing";

        assertTrue(output.toString().indexOf(expectedOut) == -1);
        assertTrue(input.toString().indexOf(expectedIn) == -1);
    }


    @Test
    public void testDiaptchWithWsaDisable() throws Exception {

        QName port = new QName("http://apache.org/cxf/systest/ws/addr_feature/", "AddNumbersPort");
        Dispatch<SOAPMessage> disptch = getService().createDispatch(port, SOAPMessage.class,
                                                                    jakarta.xml.ws.Service.Mode.MESSAGE,
                                                                    new AddressingFeature(false));
        ((BindingProvider)disptch).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                           "http://localhost:"
                                                           + PORT + "/jaxws/add");

        InputStream is = getClass().getResourceAsStream("resources/AddNumbersDispatchReq.xml");
        SOAPMessage soapReqMsg = MessageFactory.newInstance().createMessage(null, is);
        assertNotNull(soapReqMsg);
        try {
            disptch.invoke(soapReqMsg);
            fail("The MAPcodec ate the SOAPFaultException");
        } catch (jakarta.xml.ws.soap.SOAPFaultException e) {
            //expected
        }
    }

    //CXF-3060
    @Test
    public void testDisableServerEnableClientRequired() throws Exception {
        AddNumbersPortType port = getService().getAddNumbersPort(new AddressingFeature(true, true));

        ((BindingProvider)port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                        "http://localhost:" + PORT + "/jaxws/add");
        try {
            port.addNumbers(1, 2);
            fail("Expected missing WSA header exception");
        } catch (WebServiceException e) {
            String expected = "A required header representing a Message Addressing"
                              + " Property is not present";
            assertTrue("Caught unexpected exception : " + e.getMessage(),
                       e.getMessage().indexOf(expected) > -1);
        }
    }


    private AddNumbersService getService() {
        URL wsdl = getClass().getResource("/wsdl_systest_wsspec/add_numbers.wsdl");
        assertNotNull("WSDL is null", wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull("Service is null ", service);
        return service;
    }
}
