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
import org.apache.cxf.systest.ws.saml.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.saml.ext.bean.KeyInfoBean.CERT_IDENTIFIER;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;
import org.junit.Ignore;

/**
 * A set of tests for SAML Tokens.
 */
public class SamlTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String PORT2 = allocatePort(Server.class, 2);
    
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

    @org.junit.Test
    public void testSaml1OverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
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
            assertTrue(ex.getMessage().contains("Wrong SAML Version"));
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
            String error = "The received token does not match the token inclusion requirement";
            assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml1Supporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1SupportingPort");
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
    
    // Self-signing (see CXF-5248)
    @org.junit.Test
    public void testSaml1SupportingSelfSigned() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1SupportingPort");
        DoubleItPortType saml1Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT2);
        
        ((BindingProvider)saml1Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler(false)
        );
        
        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.SELF_SIGN_SAML_ASSERTION, true
        );
        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.SIGNATURE_USERNAME, "alice"
        );
        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.SIGNATURE_PROPERTIES, 
            "org/apache/cxf/systest/ws/wssec10/client/alice.properties"
        );
        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.CALLBACK_HANDLER, 
            "org.apache.cxf.systest.ws.wssec10.client.KeystorePasswordCallback"
        );
        
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);
        
        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml2OverSymmetric() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
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
            assertTrue(ex.getMessage().contains("Wrong SAML Version"));
        }
        
        ((BindingProvider)saml2Port).getRequestContext().put(
            "ws-security.saml-callback-handler", new SamlCallbackHandler()
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);
        
        try {
            SamlCallbackHandler callbackHandler = 
                new SamlCallbackHandler();
            callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);
            ((BindingProvider)saml2Port).getRequestContext().put(
                "ws-security.saml-callback-handler", callbackHandler
            );
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with a invalid SAML2 Assertion");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // assertTrue(ex.getMessage().contains("SAML token security failure"));
        }
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    /**
     * Some negative tests. Send a sender-vouches assertion as a SupportingToken...this will
     * fail as the provider will demand that there is a signature covering both the assertion
     * and the message body.
     */
    @org.junit.Test
    public void testSaml2OverSymmetricSupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
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
            assertTrue(ex.getMessage().contains("Assertion fails sender-vouches requirements"));
        }
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2OverAsymmetric() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
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
            assertTrue(ex.getMessage().contains("Wrong SAML Version"));
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
            String error = "The received token does not match the token inclusion requirement";
            assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testSaml1SelfSignedOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
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
    public void testSaml1SelfSignedOverTransportSP11() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
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
    public void testAsymmetricSamlInitiator() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
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
    
    @org.junit.Test
    public void testSaml2OverSymmetricSignedElements() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2SymmetricSignedElementsPort");
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
    public void testSaml2EndorsingOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
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
    
    @org.junit.Test
    public void testSaml2EndorsingPKOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
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
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
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
    public void testSaml2OverAsymmetricSignedEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
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

    @Ignore("invalid")
    @org.junit.Test
    public void testSaml2OverAsymmetricEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
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
    public void testSaml2EndorsingEncryptedOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
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
        URL busFile = SamlTokenTest.class.getResource("client/client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItInlinePolicyPort");
        DoubleItPortType saml2Port = 
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT2);
        
        try {
            saml2Port.doubleIt(25);
            fail("Failure expected on no SamlToken");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            String error = "The received token does not match the token inclusion requirement";
            assertTrue(ex.getMessage().contains(error));
        }
        
        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }
    
    
}
