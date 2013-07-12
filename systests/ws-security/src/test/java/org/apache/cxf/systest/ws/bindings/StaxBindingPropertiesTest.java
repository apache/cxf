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

package org.apache.cxf.systest.ws.bindings;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

/**
 * This is a test for various properties associated with a security binding. It tests both DOM + 
 * StAX clients against the StAX server
 */
public class StaxBindingPropertiesTest extends AbstractBusClientServerTestBase {
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
    
    // Child of Body is signed which conflicts with the OnlySignEntireHeadersAndBody property
    // TODO Support for streaming XPath
    @org.junit.Test
    @org.junit.Ignore
    public void testOnlySignEntireHeadersAndBody() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxBindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxBindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // This should work, as OnlySignEntireHeadersAndBody is not specified
        QName portQName = new QName(NAMESPACE, "DoubleItNotOnlySignPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // TODO - XPath support Streaming
        // SecurityTestUtil.enableStreaming(port);
        // port.doubleIt(25);
        
        // This should fail, as OnlySignEntireHeadersAndBody is specified
        portQName = new QName(NAMESPACE, "DoubleItOnlySignPort");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on OnlySignEntireHeadersAndBody property");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "OnlySignEntireHeadersAndBody does not match the requirements";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on OnlySignEntireHeadersAndBody property");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "OnlySignEntireHeadersAndBody does not match the requirements";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testEncryptSignature() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxBindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxBindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptSignaturePort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as the client is not encrypting the signature is specified
        portQName = new QName(NAMESPACE, "DoubleItEncryptSignaturePort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on not encrypting the signature property");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "Signature must be encrypted";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on not encrypting the signature property");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "The signature is not protected";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testIncludeTimestamp() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxBindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxBindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItIncludeTimestampPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as the client is not sending a Timestamp
        portQName = new QName(NAMESPACE, "DoubleItIncludeTimestampPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on not sending a Timestamp");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "Received Timestamp does not match the requirements";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on not sending a Timestamp");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "Received Timestamp does not match the requirements";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testEncryptBeforeSigning() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxBindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxBindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptBeforeSigningPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        /*
         * TODO - See WSS-462
        // This should fail, as the client is not following the correct steps for this property
        portQName = new QName(NAMESPACE, "DoubleItEncryptBeforeSigningPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on not encrypting before signing");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            System.out.println("ERR: " + ex.getMessage());
            String error = "Not encrypted before signed";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on not encrypting before signing");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "Not encrypted before signed";
            // assertTrue(ex.getMessage().contains(error));
        }
        */
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSignBeforeEncrypting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxBindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxBindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItSignBeforeEncryptingPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        /*
         * TODO - See WSS-462
        // This should fail, as the client is not following the correct steps for this property
        portQName = new QName(NAMESPACE, "DoubleItSignBeforeEncryptingPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on not signing before encrypting");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "Not signed before encrypted";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on not signing before encrypting");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "Not signed before encrypted";
            // assertTrue(ex.getMessage().contains(error));
        }
        */
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // TODO Timestamp First/Last validation not working - see WSS-444
    @org.junit.Test
    @org.junit.Ignore
    public void testTimestampFirst() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxBindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxBindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItTimestampFirstPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // TODO It's not sending the Timestamp "first" correctly - DOM
        // port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as the client is sending the timestamp last
        portQName = new QName(NAMESPACE, "DoubleItTimestampFirstPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        /*
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on on sending the timestamp last");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "Layout does not match the requirements";
            assertTrue(ex.getMessage().contains(error));
        }
        */
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on on sending the timestamp last");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "Layout does not match the requirements";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // TODO Timestamp First/Last validation not working - see WSS-444
    @org.junit.Test
    @org.junit.Ignore
    public void testTimestampLast() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxBindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxBindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItTimestampLastPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as the client is sending the timestamp first
        portQName = new QName(NAMESPACE, "DoubleItTimestampLastPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on sending the timestamp first");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "Layout does not match the requirements";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on sending the timestamp first");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "Layout does not match the requirements";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // TODO Strict validation not working - see WSS-444
    @org.junit.Test
    @org.junit.Ignore
    public void testStrict() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxBindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxBindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItStrictPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as the client is sending the timestamp last
        portQName = new QName(NAMESPACE, "DoubleItStrictPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on sending the timestamp last");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "Layout does not match the requirements";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on sending the timestamp last");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "Layout does not match the requirements";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // TODO
    @org.junit.Test
    @org.junit.Ignore
    public void testTokenProtection() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxBindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxBindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItTokenProtectionPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        port.doubleIt(25);
        
        // This should fail, as the property is not enabled
        portQName = new QName(NAMESPACE, "DoubleItTokenProtectionPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        try {
            port.doubleIt(25);
            fail("Failure expected on not protecting the token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "Layout does not match the requirements";
            // assertTrue(ex.getMessage().contains(error));
            System.out.println("EX: " + ex.getMessage());
        }
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    // Not strictly a BindingProperty but a property of WSS11...
    @org.junit.Test
    public void testSignatureConfirmation() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxBindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxBindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
       
        // This should work, as SignatureConfirmation is enabled
        QName portQName = new QName(NAMESPACE, "DoubleItSignatureConfirmationPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        port.doubleIt(25);
        
        // Streaming
        SecurityTestUtil.enableStreaming(port);
        port.doubleIt(25);
        
        // This should fail, as SignatureConfirmation is not enabled
        portQName = new QName(NAMESPACE, "DoubleItSignatureConfirmationPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);
        
        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on not enabling SignatureConfirmation");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "Check Signature confirmation";
            assertTrue(ex.getMessage().contains(error));
        }
        
        // Streaming
        /*
         * TODO - See WSS-460
        try {
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on not enabling SignatureConfirmation");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "Check Signature confirmation";
            // assertTrue(ex.getMessage().contains(error));
        }
        */
        
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
}
