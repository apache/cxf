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

package org.apache.cxf.systest.ws.addr_feature;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.AddressingFeature;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.systest.ws.AbstractWSATestBase;
import org.apache.cxf.ws.addressing.WSAddressingFeature;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WSAClientServerTest extends AbstractWSATestBase {
    static final String PORT = Server.PORT;
    static final String PORT2 = Server.PORT2;

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
    public void testNoWsaFeature() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(AddNumbersPortType.class);
        factory.setAddress("http://localhost:" + PORT + "/jaxws/add");
        AddNumbersPortType port = (AddNumbersPortType) factory.create();

        assertEquals(3, port.addNumbers(1, 2));

        assertLogNotContains(output.toString(), "//wsa:Address");
        assertLogNotContains(input.toString(), "//wsa:RelatesTo");
    }

    @Test
    public void testCxfWsaFeature() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(AddNumbersPortType.class);
        factory.setAddress("http://localhost:" + PORT + "/jaxws/add");
        factory.getFeatures().add(new WSAddressingFeature());
        AddNumbersPortType port = (AddNumbersPortType) factory.create();
        ((BindingProvider)port).getRequestContext().put("ws-addressing.write.optional.replyto", Boolean.TRUE);
        assertEquals(3, port.addNumbers(1, 2));

        assertLogContains(output.toString(), "//wsa:Address", "http://www.w3.org/2005/08/addressing/anonymous");
        assertLogContains(input.toString(), "//wsa:RelatesTo",
                          getLogValue(output.toString(), "//wsa:MessageID"));
    }

    @Test
    public void testJaxwsWsaFeature() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        AddNumbersPortType port = getPort();
        ((BindingProvider)port).getRequestContext().put("ws-addressing.write.optional.replyto", Boolean.TRUE);

        assertEquals(3, port.addNumbers(1, 2));

        assertLogContains(output.toString(), "//wsa:Address", "http://www.w3.org/2005/08/addressing/anonymous");
        assertLogContains(input.toString(), "//wsa:RelatesTo",
                          getLogValue(output.toString(), "//wsa:MessageID"));
    }

    //CXF-3456
    @Test
    public void testDuplicateHeaders() throws Exception {
        URL wsdl = getClass().getResource("/wsdl_systest_wsspec/add_numbers.wsdl");
        assertNotNull("WSDL is null", wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        QName portName = new QName("http://apache.org/cxf/systest/ws/addr_feature/", "AddNumbersPort");
        Dispatch<SOAPMessage> disp = service.createDispatch(portName, SOAPMessage.class,
                                                            Service.Mode.MESSAGE,
                                                            new AddressingFeature(false, false));
        disp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                 "http://localhost:" + PORT + "/jaxws/add");

        InputStream msgIns = getClass().getResourceAsStream("./duplicate-wsa-header-msg.xml");
        String msg = new String(IOUtils.readBytesFromStream(msgIns));
        msg = msg.replaceAll("$PORT", PORT);

        ByteArrayInputStream bout = new ByteArrayInputStream(msg.getBytes());

        SOAPMessage soapReqMsg = MessageFactory.newInstance().createMessage(null, bout);
        assertNotNull(soapReqMsg);

        try {
            disp.invoke(soapReqMsg);
            fail("SOAPFaultFxception is expected");
        } catch (SOAPFaultException ex) {
            assertTrue("WSA header exception is expected",
                       ex.getMessage().indexOf("A header representing a Message Addressing") > -1);
        }
    }


    @Test
    public void testNonAnonSoap12Fault() throws Exception {
        try {
            AddNumbersPortType port = getNonAnonPort();
            ((BindingProvider)port).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                     "http://localhost:" + PORT2 + "/jaxws/soap12/add");
            port.addNumbers(1, 2);
            fail("expected SOAPFaultException");
        } catch (SOAPFaultException e) {
            assertTrue("expected non-anonymous required message",
                       e.getMessage().contains("Found anonymous address but non-anonymous required"));
            assertTrue("expected sender faultCode", e.getFault().getFaultCode().contains("Sender"));
            assertTrue("expected OnlyNonAnonymousAddressSupported fault subcode",
                       e.getFault()
                           .getFaultSubcodes()
                           .next()
                           .toString()
                           .contains("{http://www.w3.org/2005/08/addressing}OnlyNonAnonymousAddressSupported"));

        }

    }


    private AddNumbersPortType getNonAnonPort() {
        URL wsdl = getClass().getResource("/wsdl_systest_soap12/add_numbers_soap12.wsdl");
        assertNotNull("WSDL is null", wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull("Service is null ", service);
        return service.getAddNumbersNonAnonPort(new AddressingFeature());
    }



    private AddNumbersPortType getPort() throws Exception {
        URL wsdl = getClass().getResource("/wsdl_systest_wsspec/add_numbers.wsdl");
        assertNotNull("WSDL is null", wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull("Service is null ", service);
        AddNumbersPortType port = service.getAddNumbersPort(new AddressingFeature());
        updateAddressPort(port, PORT);
        return port;
    }
}
