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

package org.apache.cxf.systest.fault;

import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.intfault.BadRecordLitFault;
import org.apache.intfault.Greeter;
import org.apache.intfault.SOAPService;

import org.junit.BeforeClass;
import org.junit.Test;

public class IntFaultClientServerTest extends AbstractBusClientServerTestBase {    
    public static final String PORT = Server.PORT;
    private final QName serviceName = new QName("http://apache.org/intfault",
                                                "SOAPService");
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testBasicConnection() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world_fault.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);
        
        Greeter greeter = service.getSoapPort();
        ClientProxy.getClient(greeter).getInInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(greeter).getOutInterceptors().add(new LoggingOutInterceptor());
        updateAddressPort(greeter, PORT);
        try {
            greeter.testDocLitFault("fault");
        } catch (BadRecordLitFault e) {
            assertEquals(5, e.getFaultInfo());
            assertSoapHeader((BindingProvider)greeter);
        }

    }
    
    private void assertSoapHeader(BindingProvider serviceProxy) {
        List<?> headers = (List<?>) serviceProxy.getResponseContext().get(Header.HEADER_LIST);
        QName testQName = new QName("http://test", "test");
        if (headers != null) {
            for (Object o : headers) {
                if (o instanceof SoapHeader) {
                    SoapHeader soapHeader = (SoapHeader) o;
                    QName qName = soapHeader.getName();
                    if (testQName.getNamespaceURI().equals(qName.getNamespaceURI())
                            && testQName.getLocalPart().equals(qName.getLocalPart())) {
                        Node returnedContent = (Node) soapHeader.getObject();
                        assertEquals("test", returnedContent.getTextContent());
                        return;
                    }
                }
            }
        }
        fail("Header not found");
    }

}
