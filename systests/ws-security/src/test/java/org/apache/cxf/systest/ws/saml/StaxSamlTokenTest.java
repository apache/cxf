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

package org.apache.cxf.systest.ws.saml;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.saml.client.SamlCallbackHandler;
import org.apache.cxf.systest.ws.saml.client.SamlElementCallbackHandler;
import org.apache.cxf.systest.ws.saml.client.SamlRoleCallbackHandler;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.wss4j.common.saml.bean.KeyInfoBean.CERT_IDENTIFIER;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * A set of tests for SAML Tokens using the streaming interceptors. It tests both DOM + StAX 
 * clients against the StAX server
 */
public class StaxSamlTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(StaxServer.class);
    static final String PORT2 = allocatePort(StaxServer.class, 2);
    
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
    public void testSaml1OverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1TransportPort");
        DoubleItPortType saml1Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT2);
        
        try {
            saml1Port.doubleIt(25);
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No SAML CallbackHandler available"));
        }
        
        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler()
        );
        try {
            saml1Port.doubleIt(25);
            fail("Expected failure on an invocation with a SAML2 Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("Policy enforces SamlVersion11Profile11 but we got 2.0"));
        }

        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler(false)
        );
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);
        
        // Don't send any Token...failure expected
        portQName = new QName(NAMESPACE, "DoubleItSaml1TransportPort2");
        saml1Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT2);
        
        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler(false)
        );
        
        try {
            saml1Port.doubleIt(25);
            fail("Failure expected on no token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "SamlToken not satisfied";
            assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml1OverTransportStreaming() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1TransportPort");
        DoubleItPortType saml1Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT2);
        SecurityTestUtil.enableStreaming(saml1Port);
        
        try {
            saml1Port.doubleIt(25);
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // assertTrue(ex.getMessage().contains("No SAML CallbackHandler available"));
        }
        
        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler()
        );
        try {
            saml1Port.doubleIt(25);
            fail("Expected failure on an invocation with a SAML2 Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // assertTrue(ex.getMessage().contains("Policy enforces SamlVersion11Profile11 but we got 2.0"));
        }

        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler(false)
        );
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);
        
        // Don't send any Token...failure expected
        portQName = new QName(NAMESPACE, "DoubleItSaml1TransportPort2");
        saml1Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT2);
        SecurityTestUtil.enableStreaming(saml1Port);
        
        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler(false)
        );
        
        try {
            saml1Port.doubleIt(25);
            fail("Failure expected on no token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "SamlToken not satisfied";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml1Supporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1SupportingPort");
        DoubleItPortType saml1Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT2);
        
        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(false);
        samlCallbackHandler.setConfirmationMethod(SAML1Constants.CONF_BEARER);
        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", samlCallbackHandler
        );
        
        // DOM
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);
        
        // Streaming
        SecurityTestUtil.enableStreaming(saml1Port);
        saml1Port.doubleIt(25);
        
        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml1ElementOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1TransportPort");
        DoubleItPortType saml1Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT2);
        
        try {
            saml1Port.doubleIt(25);
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No SAML CallbackHandler available"));
        }
        
        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlElementCallbackHandler(false)
        );
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml1ElementOverTransportStreaming() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1TransportPort");
        DoubleItPortType saml1Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT2);
        SecurityTestUtil.enableStreaming(saml1Port);
        
        try {
            saml1Port.doubleIt(25);
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // assertTrue(ex.getMessage().contains("No SAML CallbackHandler available"));
        }
        
        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlElementCallbackHandler(false)
        );
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2OverSymmetric() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2SymmetricPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);
       
        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No SAML CallbackHandler available"));
        }
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler(false)
        );
        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with a SAML1 Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "Policy enforces SamlVersion20Profile11 but we got 1.1";
            assertTrue(ex.getMessage().contains(error));
        }
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler()
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml2OverSymmetricStreaming() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2SymmetricPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);
        SecurityTestUtil.enableStreaming(saml2Port);
       
        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No SAML CallbackHandler available"));
        }
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler(false)
        );
        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with a SAML1 Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // assertTrue(ex.getMessage().contains("Wrong SAML Version"));
        }
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler()
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    // Some negative tests. Send a sender-vouches assertion as a SupportingToken...this will
    // fail as the provider will demand that there is a signature covering both the assertion
    // and the message body.
    @org.junit.Test
    public void testSaml2OverSymmetricSupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2SymmetricSupportingPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);

        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler()
        );
        
        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with an unsigned SAML SV Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("SamlToken not satisfied"));
        }
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2OverAsymmetric() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2AsymmetricPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);

        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No SAML CallbackHandler available"));
        }
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler(false)
        );
        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with a SAML1 Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "Policy enforces SamlVersion20Profile11 but we got 1.1";
            assertTrue(ex.getMessage().contains(error));
        }
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler()
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        // Don't send any Token...failure expected
        portQName = new QName(NAMESPACE, "DoubleItSaml2AsymmetricPort2");
        saml2Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler()
        );
        
        try {
            saml2Port.doubleIt(25);
            fail("Failure expected on no token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "SamlToken not satisfied";
            assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml2OverAsymmetricStreaming() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2AsymmetricPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);
        SecurityTestUtil.enableStreaming(saml2Port);

        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // assertTrue(ex.getMessage().contains("No SAML CallbackHandler available"));
        }
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler(false)
        );
        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with a SAML1 Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // assertTrue(ex.getMessage().contains("Wrong SAML Version"));
        }
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler()
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        // Don't send any Token...failure expected
        portQName = new QName(NAMESPACE, "DoubleItSaml2AsymmetricPort2");
        saml2Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);
        SecurityTestUtil.enableStreaming(saml2Port);
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler()
        );
        
        try {
            saml2Port.doubleIt(25);
            fail("Failure expected on no token");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "SamlToken not satisfied";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml1SelfSignedOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1SelfSignedTransportPort");
        DoubleItPortType saml1Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT2);
        
        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler(false)
        );
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml1SelfSignedOverTransportStreaming() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1SelfSignedTransportPort");
        DoubleItPortType saml1Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT2);
        SecurityTestUtil.enableStreaming(saml1Port);
        
        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler(false, true)
        );
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml1SelfSignedOverTransportSP11() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1SelfSignedTransportSP11Port");
        DoubleItPortType saml1Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT2);
        
        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler(false)
        );
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml1SelfSignedOverTransportSP11Streaming() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1SelfSignedTransportSP11Port");
        DoubleItPortType saml1Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT2);
        SecurityTestUtil.enableStreaming(saml1Port);
        
        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler(false, true)
        );
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSamlInitiator() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSamlInitiatorPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);
        
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", callbackHandler
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    // See WSS-446
    @org.junit.Test
    @org.junit.Ignore
    public void testSaml2EndorsingOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2EndorsingTransportPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT2);
        
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", callbackHandler
        );

        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    // See WSS-446
    @org.junit.Test
    @org.junit.Ignore
    public void testSaml2EndorsingPKOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2EndorsingTransportPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT2);
        
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        callbackHandler.setKeyInfoIdentifier(CERT_IDENTIFIER.KEY_VALUE);
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", callbackHandler
        );

        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml2EndorsingOverTransportSP11() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2EndorsingTransportSP11Port");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT2);
        
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", callbackHandler
        );

        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml2EndorsingOverTransportSP11Streaming() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2EndorsingTransportSP11Port");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT2);
        SecurityTestUtil.enableStreaming(saml2Port);
        
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", callbackHandler
        );

        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2OverAsymmetricSignedEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2AsymmetricSignedEncryptedPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler()
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml2OverAsymmetricSignedEncryptedStreaming() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2AsymmetricSignedEncryptedPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);
        SecurityTestUtil.enableStreaming(saml2Port);
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler()
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml2OverAsymmetricEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2AsymmetricEncryptedPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);
        
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", callbackHandler
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml2OverAsymmetricEncryptedStreaming() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2AsymmetricEncryptedPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);
        SecurityTestUtil.enableStreaming(saml2Port);
        
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", callbackHandler
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml2EndorsingEncryptedOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2EndorsingEncryptedTransportPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT2);
        
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", callbackHandler
        );

        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testNoSamlToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItInlinePolicyPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT2);
        
        try {
            saml2Port.doubleIt(25);
            fail("Failure expected on no SamlToken");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "SamlToken not satisfied";
            assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testNoSamlTokenStreaming() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItInlinePolicyPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT2);
        SecurityTestUtil.enableStreaming(saml2Port);
        
        try {
            saml2Port.doubleIt(25);
            fail("Failure expected on no SamlToken");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // String error = "SamlToken not satisfied";
            // assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    // In this test-case, the WSP is configured with a XACML PEP interceptor, which in this
    // case just mocks the call to the PDP + enforces the decision
    @org.junit.Test
    public void testSaml2PEP() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2PEPPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);
       
        try {
            saml2Port.doubleIt(25);
            fail("Failure expected as Assertion doesn't contain Role information");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
        
        SamlRoleCallbackHandler roleCallbackHandler = 
            new SamlRoleCallbackHandler();
        roleCallbackHandler.setRoleName("manager");
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", roleCallbackHandler
        );
        
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        // Expected failure on incorrect role
        roleCallbackHandler.setRoleName("boss");
        try {
            saml2Port.doubleIt(25);
            fail("Failure expected as Assertion doesn't contain correct role");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    // In this test-case, the WSP is configured with a XACML PEP interceptor, which in this
    // case just mocks the call to the PDP + enforces the decision
    @org.junit.Test
    public void testSaml2PEPStreaming() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxSamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = StaxSamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2PEPPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);
        SecurityTestUtil.enableStreaming(saml2Port);
       
        try {
            saml2Port.doubleIt(25);
            fail("Failure expected as Assertion doesn't contain Role information");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
        
        SamlRoleCallbackHandler roleCallbackHandler = 
            new SamlRoleCallbackHandler();
        roleCallbackHandler.setRoleName("manager");
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", roleCallbackHandler
        );
        
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        // Expected failure on incorrect role
        roleCallbackHandler.setRoleName("boss");
        try {
            saml2Port.doubleIt(25);
            fail("Failure expected as Assertion doesn't contain correct role");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
}
