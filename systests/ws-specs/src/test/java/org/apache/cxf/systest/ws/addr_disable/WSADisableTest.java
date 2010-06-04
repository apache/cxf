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
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.soap.AddressingFeature;

import org.apache.cxf.systest.ws.AbstractWSATestBase;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersPortType;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersService;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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

        String base = "http://apache.org/cxf/systest/ws/addr_feature/AddNumbersPortType/";
        String expectedOut = base + "addNumbersRequest</Action>";
        String expectedIn = "http://www.w3.org/2005/08/addressing";
        
        assertTrue(output.toString().indexOf(expectedOut) != -1);
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
                                                                    javax.xml.ws.Service.Mode.MESSAGE,
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
        } catch (javax.xml.ws.soap.SOAPFaultException e) {
            //expected
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