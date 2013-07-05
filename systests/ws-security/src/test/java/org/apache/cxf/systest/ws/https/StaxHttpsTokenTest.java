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

package org.apache.cxf.systest.ws.https;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.https.server.StaxServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * A set of tests for the HttpsToken policy. It tests both DOM + StAX clients against the 
 * StAX server.
 */
public class StaxHttpsTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(StaxServer.class);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(StaxServer.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testRequireClientCert() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxHttpsTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxHttpsTokenTest.class.getResource("DoubleItHttps.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItRequireClientCertPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as the client does not use a client cert
        portQName = new QName(NAMESPACE, "DoubleItRequireClientCertPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        try {
            // DOM
            port.doubleIt(25);
            fail("Failure expected on not using a client cert");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
        
        try {
            // Streaming
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on not using a client cert");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testBasicAuth() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxHttpsTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxHttpsTokenTest.class.getResource("DoubleItHttps.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItBasicAuthPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);  
        
        // This should fail, as the client does not send a UsernamePassword
        portQName = new QName(NAMESPACE, "DoubleItBasicAuthPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        try {
            // DOM
            port.doubleIt(25);
            fail("Failure expected on not sending a UsernamePassword");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
        
        try {
            // Streaming
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on not sending a UsernamePassword");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
        
    }
    
}
