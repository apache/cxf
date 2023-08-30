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
package org.apache.cxf.systest.jaxws;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPBinding;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CXF7990Test extends AbstractClientServerTestBase {
    static final String PORT = allocatePort(Server.class);

    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            Object implementor = new EchoServiceImpl();
            String address = "http://localhost:" + PORT + "/echo/service";
            Endpoint.publish(address, implementor);

            Object proxyImpl = new EchoProxyServiceImpl();
            String address2 = "http://localhost:" + PORT + "/proxy/service";
            Endpoint.publish(address2, proxyImpl);
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
    public void testSOAPFaultException() throws Exception {
        QName serviceName = new QName("urn:echo", "EchoServiceImplService");
        QName portName = new QName("urn:echo", "MyEchoServicePort");
        URL wsdlURL = new URL("http://localhost:" + PORT + "/echo/service?wsdl");
        Service service = Service.create(wsdlURL, serviceName);
        EchoService echoService = service.getPort(portName, EchoService.class);
        try {
            echoService.echoException("test");
            fail("SOAPException is expected");
        } catch (SOAPFaultException e) {
            assertEquals(e.getMessage(), "TestSOAPFaultException");
        }
    }

    @Test
    public void testProxySOAPFaultException() throws Exception {
        QName serviceName = new QName("urn:echo", "EchoProxyServiceImplService");
        QName portName = new QName("urn:echo", "MyEchoProxyServicePort");
        URL wsdlURL = new URL("http://localhost:" + PORT + "/proxy/service?wsdl");
        Service service = Service.create(wsdlURL, serviceName);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, "http://localhost:" + PORT
                + "/proxy/service");
        EchoService echoService = service.getPort(portName, EchoService.class);
        try {
            echoService.echoProxy("http://localhost:" + PORT + "/echo/service?wsdl");
            fail("SOAPException is expected");
        } catch (SOAPFaultException e) {
            assertEquals(e.getMessage(), e.getMessage(), "SOAPFaultString");
        }
    }

}
