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

package org.apache.cxf.systest.ws.action;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.action.server.StaxServer;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * A set of tests for StAX WS-Security actions (i.e. the non WS-SecurityPolicy approach).
 */
public class StaxActionTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(StaxServer.class);

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
    public void testUsernameTokenText() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxActionTest.class.getResource("client/stax-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = StaxActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUsernameTokenTextPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        int result = port.doubleIt(25);
        assertEquals(50, result);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testUsernameTokenDigest() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxActionTest.class.getResource("client/stax-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = StaxActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUsernameTokenDigestPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        int result = port.doubleIt(25);
        assertEquals(50, result);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testEncrypt() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxActionTest.class.getResource("client/stax-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = StaxActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        int result = port.doubleIt(25);
        assertEquals(50, result);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSignature() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxActionTest.class.getResource("client/stax-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = StaxActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignaturePort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        int result = port.doubleIt(25);
        assertEquals(50, result);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSignedTimestamp() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxActionTest.class.getResource("client/stax-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = StaxActionTest.class.getResource("DoubleItAction.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignedTimestampPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        int result = port.doubleIt(25);
        assertEquals(50, result);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    
}
