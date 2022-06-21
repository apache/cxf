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

import javax.xml.namespace.QName;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NBProviderClientServerTest extends AbstractBusClientServerTestBase {
    public static final String ADDRESS
        = "http://localhost:" + TestUtil.getPortNumber(Server.class)
            + "/SoapContext/SoapProviderPort";

    private static QName sayHi = new QName("http://apache.org/hello_world_soap_http/types", "sayHi");

    public static class Server extends AbstractBusTestServerBase {
        Endpoint ep;

        protected void run() {
            Object implementor = new NBSoapMessageDocProvider();
            ep = Endpoint.publish(ADDRESS, implementor);
        }

        @Override
        public void tearDown() {
            ep.stop();
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testSOAPMessageModeDocLit() throws Exception {
        QName serviceName =
            new QName("http://apache.org/hello_world_soap_http", "SOAPProviderService");
        QName portName =
            new QName("http://apache.org/hello_world_soap_http", "SoapProviderPort");

        Service service = Service.create(serviceName);
        assertNotNull(service);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, ADDRESS);

        try {
            Dispatch<SOAPMessage> dispatch = service.createDispatch(portName, SOAPMessage.class, Service.Mode.MESSAGE);

            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage request = encodeRequest(factory, "sayHi");
            SOAPMessage response;
            try {
                response = dispatch.invoke(request);
                fail("Should have thrown an exception");
            } catch (Exception ex) {
                //expected
                assertEquals("no body expected", ex.getMessage());
            }

            request = encodeRequest(factory, null);
            response = dispatch.invoke(request);
            String resp = decodeResponse(response);
            assertEquals("Bonjour", resp);

        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }

    }

    private SOAPMessage encodeRequest(MessageFactory factory, String value) throws SOAPException {
        SOAPMessage request = factory.createMessage();
        SOAPEnvelope envelope = request.getSOAPPart().getEnvelope();
        request.setProperty("soapaction", "");
        if (value != null) {
            request.getSOAPBody().addBodyElement(envelope.createName(value, "ns1", sayHi.getNamespaceURI()));
        }

        return request;
    }

    private String decodeResponse(SOAPMessage response) throws SOAPException {
        NodeList nodelist = response.getSOAPBody().getElementsByTagNameNS(sayHi.getNamespaceURI(), "responseType");
        if (nodelist.getLength() == 1) {
            Node node = nodelist.item(0).getFirstChild();
            if (node != null) {
                return node.getNodeValue();
            }
        }
        return null;
    }
}
