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

package org.apache.cxf.systest.ws.mtom;

import java.io.File;
import java.net.URL;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItMtomPortType;
import org.example.contract.doubleit.DoubleItPortType;
import org.example.schema.doubleit.DoubleIt4;
import org.junit.BeforeClass;

/**
 * A set of secured MTOM
 */
public class MTOMSecurityTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(Server.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    // The attachment is inlined + the SOAP Body signed
    @org.junit.Test
    public void testSignedMTOMInline() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = MTOMSecurityTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = MTOMSecurityTest.class.getResource("DoubleItMtom.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignedMTOMInlinePort");
        DoubleItMtomPortType port = 
                service.getPort(portQName, DoubleItMtomPortType.class);
        updateAddressPort(port, PORT);
        
        DataSource source = new FileDataSource(new File("src/test/resources/java.jpg"));
        DoubleIt4 doubleIt = new DoubleIt4();
        doubleIt.setNumberToDouble(25);
        port.doubleIt4(25, new DataHandler(source));
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Here we are not-inlining, but the attachments are signed (as is the SOAP Body)
    // Temporarily @Ignoring this test
    @org.junit.Test
    @org.junit.Ignore
    public void testSignedMTOMSwA() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = MTOMSecurityTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = MTOMSecurityTest.class.getResource("DoubleItMtom.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignedMTOMSwAPort");
        DoubleItMtomPortType port = 
                service.getPort(portQName, DoubleItMtomPortType.class);
        updateAddressPort(port, PORT);
        
        DataSource source = new FileDataSource(new File("src/test/resources/java.jpg"));
        DoubleIt4 doubleIt = new DoubleIt4();
        doubleIt.setNumberToDouble(25);
        port.doubleIt4(25, new DataHandler(source));
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Here we moving encrypted bytes to attachments instead, and referencing them via xop:Include
    // This avoids the BASE-64 encoding/decoding step when the raw bytes are included in the SOAP Envelope
    @org.junit.Test
    @org.junit.Ignore
    public void testEncryptedDataInAttachment() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = MTOMSecurityTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);
        
        URL wsdl = MTOMSecurityTest.class.getResource("DoubleItMtom.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        DoubleItPortType port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        int result = port.doubleIt(25);
        assertEquals(result, 50);
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
}
