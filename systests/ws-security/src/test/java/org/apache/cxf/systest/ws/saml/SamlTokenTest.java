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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.saml.client.SamlCallbackHandler;
import org.apache.cxf.systest.ws.saml.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.ws.security.saml.ext.bean.KeyInfoBean.CERT_IDENTIFIER;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;

import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

/**
 * A set of tests for SAML Tokens.
 */
public class SamlTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String PORT2 = allocatePort(Server.class, 2);
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private boolean unrestrictedPoliciesInstalled = checkUnrestrictedPoliciesInstalled();
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
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
    }
    
    @org.junit.Test
    public void testSaml2OverSymmetric() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }
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
            assertTrue(ex.getMessage().contains("SAML token security failure"));
        }
    }
    
    /**
     * Some negative tests. Send a sender-vouches assertion as a SupportingToken...this will
     * fail as the provider will demand that there is a signature covering both the assertion
     * and the message body.
     */
    @org.junit.Test
    public void testSaml2OverSymmetricSupporting() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }
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
    }

    @org.junit.Test
    public void testSaml2OverAsymmetric() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }
        
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
        
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testAsymmetricSamlInitiator() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }
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
    }
    
    @org.junit.Test
    public void testSaml2OverSymmetricSignedElements() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }
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
        
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2OverAsymmetricSignedEncrypted() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }
        
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
    }
    
    @org.junit.Test
    public void testSaml2OverAsymmetricEncrypted() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }
        
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
    }
    
    
    private boolean checkUnrestrictedPoliciesInstalled() {
        try {
            byte[] data = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};

            SecretKey key192 = new SecretKeySpec(
                new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                            0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
                            0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17},
                            "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key192);
            c.doFinal(data);
            return true;
        } catch (Exception e) {
            //
        }
        return false;
    }
}
