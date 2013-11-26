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

package org.apache.cxf.systest.ws.parts;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.example.contract.doubleit.DoubleItPortType;
import org.example.contract.doubleit.DoubleItSwaPortType;
import org.example.schema.doubleit.DoubleIt3;
import org.junit.BeforeClass;

/**
 * This is a test for various Required/Signed/Encrypted Parts/Elements. It tests both DOM + StAX 
 * clients against the StAX server
 */
public class StaxPartsTest extends AbstractBusClientServerTestBase {
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
    public void testRequiredParts() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxPartsTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxPartsTest.class.getResource("DoubleItParts.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItRequiredPartsPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as the service requires a (bad) header
        portQName = new QName(NAMESPACE, "DoubleItRequiredPartsPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on a required header which isn't present");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "must be present";
            assertTrue(ex.getMessage().contains(error));
        }

        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on a required header which isn't present");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "must be present";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testRequiredElements() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PartsTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = PartsTest.class.getResource("DoubleItParts.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItRequiredElementsPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as the service requires a (bad) header
        portQName = new QName(NAMESPACE, "DoubleItRequiredElementsPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on a required header which isn't present");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "ToTo must be present";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on a required header which isn't present");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "ToTo must be present";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSignedParts() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxPartsTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxPartsTest.class.getResource("DoubleItParts.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItSignedPartsPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as the service requires that the Body must be signed
        portQName = new QName(NAMESPACE, "DoubleItSignedPartsPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on a body which isn't signed");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "Body must be signed";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on a body which isn't signed");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "SignedParts";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        // This should fail, as the service requires that the To header must be signed
        portQName = new QName(NAMESPACE, "DoubleItSignedPartsPort3");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on a header which isn't signed");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "To must be signed";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on a body which isn't signed");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "SignedParts";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSignedElements() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PartsTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = PartsTest.class.getResource("DoubleItParts.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItSignedElementsPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as the service requires that the To header must be signed
        portQName = new QName(NAMESPACE, "DoubleItSignedElementsPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on a header which isn't signed");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "To must be signed";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on a header which isn't signed");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "To must be signed";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testEncryptedParts() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxPartsTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxPartsTest.class.getResource("DoubleItParts.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptedPartsPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as the service requires that the Body must be encrypted
        portQName = new QName(NAMESPACE, "DoubleItEncryptedPartsPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on a body which isn't encrypted");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "Body must be encrypted";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on a body which isn't encrypted");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "Body must be encrypted";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        // This should fail, as the service requires that the To header must be encrypted
        portQName = new QName(NAMESPACE, "DoubleItEncryptedPartsPort3");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on a header which isn't encrypted");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "To must be encrypted";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on a header which isn't encrypted");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "To must be encrypted";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testEncryptedElements() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PartsTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = PartsTest.class.getResource("DoubleItParts.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptedElementsPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as the service requires that the header must be encrypted
        portQName = new QName(NAMESPACE, "DoubleItEncryptedElementsPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on a header which isn't encrypted");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "To must be encrypted";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on a header which isn't encrypted");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "To must be encrypted";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testContentEncryptedElements() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PartsTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = PartsTest.class.getResource("DoubleItParts.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItContentEncryptedElementsPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // This should fail, as the service requires that the header must be encrypted
        portQName = new QName(NAMESPACE, "DoubleItContentEncryptedElementsPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        try {
            port.doubleIt(25);
            fail("Failure expected on a header which isn't encrypted");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "To must be encrypted";
            assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignedAttachments() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PartsTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = PartsTest.class.getResource("DoubleItParts.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItSignedAttachmentsPort");
        DoubleItSwaPortType port = service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        port.doubleIt3(doubleIt, "12345".getBytes());
        
        // Streaming
        enableStreaming(port);
        doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        port.doubleIt3(doubleIt, "12345".getBytes());
        
        // This should fail, as the service requires that the Attachments must be signed
        portQName = new QName(NAMESPACE, "DoubleItSignedAttachmentsPort2");
        port = service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            doubleIt = new DoubleIt3();
            doubleIt.setNumberToDouble(25);
            port.doubleIt3(doubleIt, "12345".getBytes());
            fail("Failure expected on an attachment which isn't signed");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "SignedParts";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            enableStreaming(port);
            doubleIt = new DoubleIt3();
            doubleIt.setNumberToDouble(25);
            port.doubleIt3(doubleIt, "12345".getBytes());
            fail("Failure expected on an attachment which isn't signed");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "SignedParts";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testEncryptedAttachments() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PartsTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = PartsTest.class.getResource("DoubleItParts.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptedAttachmentsPort");
        DoubleItSwaPortType port = service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        port.doubleIt3(doubleIt, "12345".getBytes());
        
        // Streaming
        enableStreaming(port);
        doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        port.doubleIt3(doubleIt, "12345".getBytes());
        
        // This should fail, as the service requires that the Attachments must be encrypted
        portQName = new QName(NAMESPACE, "DoubleItEncryptedAttachmentsPort2");
        port = service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            doubleIt = new DoubleIt3();
            doubleIt.setNumberToDouble(25);
            port.doubleIt3(doubleIt, "12345".getBytes());
            fail("Failure expected on an attachment which isn't encrypted");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "EncryptedParts";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            enableStreaming(port);
            doubleIt = new DoubleIt3();
            doubleIt.setNumberToDouble(25);
            port.doubleIt3(doubleIt, "12345".getBytes());
            fail("Failure expected on an attachment which isn't encrypted");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "SignedParts";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    static void enableStreaming(DoubleItSwaPortType port) {
        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.ENABLE_STREAMING_SECURITY, "true"
        );
        ((BindingProvider)port).getResponseContext().put(
            SecurityConstants.ENABLE_STREAMING_SECURITY, "true"
        );
    }
  
    
}
