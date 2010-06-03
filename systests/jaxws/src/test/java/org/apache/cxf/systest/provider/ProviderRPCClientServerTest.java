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

package org.apache.cxf.systest.provider;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_rpclit.GreeterRPCLit;
import org.apache.hello_world_rpclit.SOAPServiceRPCLit;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProviderRPCClientServerTest extends AbstractBusClientServerTestBase {
    private static final String PORT = Server.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }
    
    @Test
    public void testSWA() throws Exception {
        SOAPFactory soapFac = SOAPFactory.newInstance();
        MessageFactory msgFac = MessageFactory.newInstance();
        SOAPConnectionFactory conFac = SOAPConnectionFactory.newInstance();
        SOAPMessage msg = msgFac.createMessage();
        
        QName sayHi = new QName("http://apache.org/hello_world_rpclit", "sayHiWAttach");
        msg.getSOAPBody().addChildElement(soapFac.createElement(sayHi));
        AttachmentPart ap1 = msg.createAttachmentPart();
        ap1.setContent("Attachment content", "text/plain");
        msg.addAttachmentPart(ap1);
        AttachmentPart ap2 = msg.createAttachmentPart();
        ap2.setContent("Attachment content - Part 2", "text/plain");
        msg.addAttachmentPart(ap2);
        msg.saveChanges();
        
        SOAPConnection con = conFac.createConnection();
        URL endpoint = new URL("http://localhost:" + PORT 
                               + "/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit1");
        SOAPMessage response = con.call(msg, endpoint); 
        QName sayHiResp = new QName("http://apache.org/hello_world_rpclit", "sayHiResponse");
        assertNotNull(response.getSOAPBody().getChildElements(sayHiResp));
        assertEquals(2, response.countAttachments());
    }

    private void doGreeterRPCLit(SOAPServiceRPCLit service,
                                 QName portName,
                                 int count,
                                 boolean doFault) throws Exception {
        String response1 = new String("TestGreetMeResponse");
        String response2 = new String("TestSayHiResponse");
        try {
            GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class);
            updateAddressPort(greeter, PORT);
            for (int idx = 0; idx < count; idx++) {
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull("no response received from service", greeting);
                assertEquals(response1, greeting);

                String reply = greeter.sayHi();
                assertNotNull("no response received from service", reply);
                assertEquals(response2, reply);
                
                if (doFault) {
                    try {
                        greeter.greetMe("throwFault");
                    } catch (SOAPFaultException ex) {
                        assertNotNull(ex.getFault().getDetail());
                        assertTrue(ex.getFault().getDetail().getDetailEntries().hasNext());
                    }
                }
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void testSOAPMessageModeRPC() throws Exception {

        QName serviceName = new QName("http://apache.org/hello_world_rpclit", "SOAPServiceProviderRPCLit");
        QName portName = new QName("http://apache.org/hello_world_rpclit", "SoapPortProviderRPCLit1");

        URL wsdl = getClass().getResource("/wsdl/hello_world_rpc_lit.wsdl");
        assertNotNull(wsdl);

        SOAPServiceRPCLit service = new SOAPServiceRPCLit(wsdl, serviceName);
        assertNotNull(service);

        
        String response1 = new String("TestGreetMeResponseServerLogicalHandlerServerSOAPHandler");
        String response2 = new String("TestSayHiResponse");
        GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class);
        updateAddressPort(greeter, PORT);

        String greeting = greeter.greetMe("Milestone-0");
        assertNotNull("no response received from service", greeting);
        assertEquals(response1, greeting);

        String reply = greeter.sayHi();
        assertNotNull("no response received from service", reply);
        assertEquals(response2, reply);
    }

    @Test
    public void testSOAPMessageModeWithDOMSourceData() throws Exception {
        QName serviceName = new QName("http://apache.org/hello_world_rpclit", "SOAPServiceProviderRPCLit");
        QName portName = new QName("http://apache.org/hello_world_rpclit", "SoapPortProviderRPCLit2");

        URL wsdl = getClass().getResource("/wsdl/hello_world_rpc_lit.wsdl");
        assertNotNull(wsdl);

        SOAPServiceRPCLit service = new SOAPServiceRPCLit(wsdl, serviceName);
        assertNotNull(service);

        doGreeterRPCLit(service, portName, 2, false);
    }

    @Test
    public void testPayloadModeWithDOMSourceData() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world_rpc_lit.wsdl");
        assertNotNull(wsdl);

        QName serviceName = new QName("http://apache.org/hello_world_rpclit", "SOAPServiceProviderRPCLit");
        QName portName = new QName("http://apache.org/hello_world_rpclit", "SoapPortProviderRPCLit3");

        SOAPServiceRPCLit service = new SOAPServiceRPCLit(wsdl, serviceName);
        assertNotNull(service);

        doGreeterRPCLit(service, portName, 1, true);
    }
    
    @Test
    public void testPayloadModeWithSourceData() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world_rpc_lit.wsdl");
        assertNotNull(wsdl);

        QName serviceName = new QName("http://apache.org/hello_world_rpclit", "SOAPServiceProviderRPCLit");
        QName portName = new QName("http://apache.org/hello_world_rpclit", "SoapPortProviderRPCLit8");

        SOAPServiceRPCLit service = new SOAPServiceRPCLit(wsdl, serviceName);
        assertNotNull(service);

        String addresses[] = {
            "http://localhost:" + PORT 
                + "/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit8",
            "http://localhost:" + PORT 
                + "/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit8-dom",
            "http://localhost:" + PORT 
                + "/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit8-sax",
            "http://localhost:" + PORT 
                + "/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit8-cxfstax",
            "http://localhost:" + PORT 
                + "/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit8-stax",
            "http://localhost:" + PORT 
                + "/SOAPServiceProviderRPCLit/SoapPortProviderRPCLit8-stream"               
        };
        String response1 = new String("TestGreetMeResponseServerLogicalHandlerServerSOAPHandler");
        GreeterRPCLit greeter = service.getPort(portName, GreeterRPCLit.class);
        for (String ad : addresses) {
            ((BindingProvider)greeter).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ad);
            String greeting = greeter.greetMe("Milestone-0");
            assertNotNull("no response received from service " + ad, greeting);
            assertEquals("wrong response received from service " + ad, response1, greeting);
        }
    }
    
    @Test
    public void testMessageModeWithSAXSourceData() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world_rpc_lit.wsdl");
        assertNotNull(wsdl);

        QName serviceName = new QName("http://apache.org/hello_world_rpclit", "SOAPServiceProviderRPCLit");
        QName portName = new QName("http://apache.org/hello_world_rpclit", "SoapPortProviderRPCLit4");

        SOAPServiceRPCLit service = new SOAPServiceRPCLit(wsdl, serviceName);
        assertNotNull(service);

        doGreeterRPCLit(service, portName, 1, false);
    }

    @Test
    public void testMessageModeWithStreamSourceData() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world_rpc_lit.wsdl");
        assertNotNull(wsdl);

        QName serviceName = new QName("http://apache.org/hello_world_rpclit", "SOAPServiceProviderRPCLit");
        QName portName = new QName("http://apache.org/hello_world_rpclit", "SoapPortProviderRPCLit5");

        SOAPServiceRPCLit service = new SOAPServiceRPCLit(wsdl, serviceName);
        assertNotNull(service);

        doGreeterRPCLit(service, portName, 1, false);
    }

    @Test
    public void testPayloadModeWithSAXSourceData() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world_rpc_lit.wsdl");
        assertNotNull(wsdl);

        QName serviceName = new QName("http://apache.org/hello_world_rpclit", "SOAPServiceProviderRPCLit");
        QName portName = new QName("http://apache.org/hello_world_rpclit", "SoapPortProviderRPCLit6");

        SOAPServiceRPCLit service = new SOAPServiceRPCLit(wsdl, serviceName);
        assertNotNull(service);

        doGreeterRPCLit(service, portName, 1, false);
    }

}
