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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.common.TestParam;
import org.apache.cxf.systest.ws.saml.client.SamlCallbackHandler;
import org.apache.cxf.systest.ws.saml.client.SamlElementCallbackHandler;
import org.apache.cxf.systest.ws.saml.client.SamlRoleCallbackHandler;
import org.apache.cxf.systest.ws.ut.SecurityHeaderCacheInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.bean.AudienceRestrictionBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.KeyInfoBean.CERT_IDENTIFIER;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A set of tests for SAML Tokens.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class SamlTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String STAX_PORT = allocatePort(StaxServer.class);
    static final String PORT2 = allocatePort(Server.class, 2);
    static final String STAX_PORT2 = allocatePort(StaxServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    final TestParam test;

    public SamlTokenTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxServer.class, true)
        );
    }

    @Parameters(name = "{0}")
    public static Collection<TestParam> data() {

        return Arrays.asList(new TestParam[] {new TestParam(PORT, false),
                                              new TestParam(PORT, true),
                                              new TestParam(STAX_PORT, false),
                                              new TestParam(STAX_PORT, true),
        });
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @org.junit.Test
    public void testSaml1OverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1TransportPort");
        DoubleItPortType saml1Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml1Port, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml1Port);
        }

        try {
            saml1Port.doubleIt(25);
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No SAML CallbackHandler available"));
        }

        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler()
        );
        try {
            saml1Port.doubleIt(25);
            fail("Expected failure on an invocation with a SAML2 Assertion");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("Wrong SAML Version")
                       || ex.getMessage().contains("enforces SamlVersion11Profile11 but we got 2.0"));
        }

        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler(false)
        );
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);

        // Don't send any Token...failure expected
        portQName = new QName(NAMESPACE, "DoubleItSaml1TransportPort2");
        saml1Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml1Port, PORT2);

        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler(false)
        );

        try {
            saml1Port.doubleIt(25);
            fail("Failure expected on no token");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "The received token does not match the token inclusion requirement";
            assertTrue(ex.getMessage().contains(error));
        }

        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml1Supporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1SupportingPort");
        DoubleItPortType saml1Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml1Port, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml1Port);
        }

        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(false, true);
        samlCallbackHandler.setConfirmationMethod(SAML1Constants.CONF_BEARER);
        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, samlCallbackHandler
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
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1SupportingPort");
        DoubleItPortType saml1Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml1Port, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml1Port);
        }

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(false, true);
        callbackHandler.setConfirmationMethod(SAML1Constants.CONF_BEARER);
        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.SIGNATURE_USERNAME, "alice"
        );
        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.SIGNATURE_PROPERTIES, "alice.properties"
        );
        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.CALLBACK_HANDLER,
            "org.apache.cxf.systest.ws.common.KeystorePasswordCallback"
        );

        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml1ElementOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1TransportPort");
        DoubleItPortType saml1Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml1Port, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml1Port);
        }

        try {
            saml1Port.doubleIt(25);
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No SAML CallbackHandler available"));
        }

        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlElementCallbackHandler(false)
        );
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2OverSymmetric() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2SymmetricPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No SAML CallbackHandler available"));
        }

        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler(false)
        );
        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with a SAML1 Assertion");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("Wrong SAML Version")
                       || ex.getMessage().contains("enforces SamlVersion20Profile11 but we got 1.1"));
        }

        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler();
        samlCallbackHandler.setSignAssertion(true);
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, samlCallbackHandler
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    // Re-enable once we pick up WSS4J 2.2.3 (https://issues.apache.org/jira/browse/WSS-640)
    @org.junit.Test
    @org.junit.Ignore
    public void testSaml2OverSymmetricSoap12() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2SymmetricSoap12Port");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No SAML CallbackHandler available"));
        }

        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler(false)
        );
        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with a SAML1 Assertion");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("Wrong SAML Version")
                       || ex.getMessage().contains("enforces SamlVersion20Profile11 but we got 1.1"));
        }

        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler();
        samlCallbackHandler.setSignAssertion(true);
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, samlCallbackHandler
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
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2SymmetricSupportingPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler()
        );

        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with an unsigned SAML SV Assertion");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("SamlToken not satisfied")
                       || ex.getMessage().equals(WSSecurityException.UNIFIED_SECURITY_ERR));
        }

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2OverAsymmetric() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2AsymmetricPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with no SAML Assertion");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("No SAML CallbackHandler available"));
        }

        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler(false)
        );
        try {
            saml2Port.doubleIt(25);
            fail("Expected failure on an invocation with a SAML1 Assertion");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("Wrong SAML Version")
                       || ex.getMessage().contains("enforces SamlVersion20Profile11 but we got 1.1"));
        }

        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler()
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        // Don't send any Token...failure expected
        portQName = new QName(NAMESPACE, "DoubleItSaml2AsymmetricPort2");
        saml2Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, PORT);

        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler()
        );

        try {
            saml2Port.doubleIt(25);
            fail("Failure expected on no token");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "The received token does not match the token inclusion requirement";
            assertTrue(ex.getMessage().contains(error));
        }

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml1SelfSignedOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1SelfSignedTransportPort");
        DoubleItPortType saml1Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml1Port, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml1Port);
        }

        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler(false, true)
        );
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml1SelfSignedOverTransportSP11() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml1SelfSignedTransportSP11Port");
        DoubleItPortType saml1Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml1Port, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml1Port);
        }

        ((BindingProvider)saml1Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler(false, true)
        );
        int result = saml1Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml1Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testAsymmetricSamlInitiator() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSamlInitiatorPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testAsymmetricSamlInitiatorProtectTokens() throws Exception {

        // We don't support ProtectTokens + streaming clients
        if (test.isStreaming()) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSamlInitiatorProtectTokensPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2OverSymmetricSignedElements() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2SymmetricSignedElementsPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        // This test only works for DOM
        if (!test.isStreaming() && PORT.equals(test.getPort())) {
            SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler();
            ((BindingProvider)saml2Port).getRequestContext().put(
                SecurityConstants.SAML_CALLBACK_HANDLER, samlCallbackHandler
            );
            int result = saml2Port.doubleIt(25);
            assertTrue(result == 50);
        }

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2EndorsingOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2EndorsingTransportPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml2Port, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2EndorsingPKOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2EndorsingTransportPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml2Port, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        callbackHandler.setKeyInfoIdentifier(CERT_IDENTIFIER.KEY_VALUE);
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2EndorsingOverTransportSP11() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2EndorsingTransportSP11Port");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml2Port, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2OverAsymmetricSignedEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2AsymmetricSignedEncryptedPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler()
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2OverAsymmetricSignedEncryptedEncryptBeforeSigning() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName =
            new QName(NAMESPACE, "DoubleItSaml2AsymmetricSignedEncryptedEncryptBeforeSigningPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        // TODO Only working for DOM client + server atm
        if (!test.isStreaming() && PORT.equals(test.getPort())) {
            ((BindingProvider)saml2Port).getRequestContext().put(
                SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler()
            );
            int result = saml2Port.doubleIt(25);
            assertTrue(result == 50);
        }

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2OverAsymmetricEncrypted() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2AsymmetricEncryptedPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );
        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2EndorsingEncryptedOverTransport() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2EndorsingEncryptedTransportPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml2Port, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testNoSamlToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItInlinePolicyPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml2Port, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        try {
            saml2Port.doubleIt(25);
            fail("Failure expected on no SamlToken");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "The received token does not match the token inclusion requirement";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("SamlToken not satisfied"));
        }

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    // In this test-case, the WSP is configured with a XACML PEP interceptor, which in this
    // case just mocks the call to the PDP + enforces the decision
    @org.junit.Test
    public void testSaml2PEP() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2PEPPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, test.getPort());

        try {
            saml2Port.doubleIt(25);
            fail("Failure expected as Assertion doesn't contain Role information");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        SamlRoleCallbackHandler roleCallbackHandler =
            new SamlRoleCallbackHandler();
        roleCallbackHandler.setSignAssertion(true);
        roleCallbackHandler.setRoleName("manager");
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, roleCallbackHandler
        );

        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        // Expected failure on incorrect role
        roleCallbackHandler.setRoleName("boss");
        try {
            saml2Port.doubleIt(25);
            fail("Failure expected as Assertion doesn't contain correct role");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSaml2Replay() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml2Port, portNumber);

        // Create a SAML Token with no "OneTimeUse" Condition
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, new SamlCallbackHandler()
        );

        Client cxfClient = ClientProxy.getClient(saml2Port);
        SecurityHeaderCacheInterceptor cacheInterceptor =
            new SecurityHeaderCacheInterceptor();
        cxfClient.getOutInterceptors().add(cacheInterceptor);

        // Make two invocations...should succeed
        saml2Port.doubleIt(25);
        saml2Port.doubleIt(25);

        // Now create a SAML Token with a "OneTimeUse" Condition
        ConditionsBean conditions = new ConditionsBean();
        conditions.setTokenPeriodMinutes(5);
        conditions.setOneTimeUse(true);

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setConditions(conditions);

        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        cxfClient.getOutInterceptors().remove(cacheInterceptor);
        cacheInterceptor = new SecurityHeaderCacheInterceptor();
        cxfClient.getOutInterceptors().add(cacheInterceptor);

        // Make two invocations...should fail on the second one
        saml2Port.doubleIt(25);

        try {
            saml2Port.doubleIt(25);
            fail("Failure expected on a replayed SAML Assertion");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains(WSSecurityException.UNIFIED_SECURITY_ERR));
        }

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testAudienceRestriction() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort2");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml2Port, portNumber);

        // Create a SAML Token with an AudienceRestrictionCondition
        ConditionsBean conditions = new ConditionsBean();
        List<AudienceRestrictionBean> audienceRestrictions = new ArrayList<>();
        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList(
            "https://localhost:" + portNumber + "/DoubleItSaml2Transport2"));
        audienceRestrictions.add(audienceRestriction);
        conditions.setAudienceRestrictions(audienceRestrictions);

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setConditions(conditions);
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        saml2Port.doubleIt(25);

        try {
            // Now use an "unknown" audience restriction
            audienceRestriction = new AudienceRestrictionBean();
            audienceRestriction.setAudienceURIs(Collections.singletonList(
                "https://localhost:" + portNumber + "/DoubleItSaml2Transport2unknown"));
            audienceRestrictions.clear();
            audienceRestrictions.add(audienceRestriction);
            conditions.setAudienceRestrictions(audienceRestrictions);
            callbackHandler.setConditions(conditions);

            saml2Port.doubleIt(25);
            fail("Failure expected on unknown AudienceRestriction");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testAudienceRestrictionServiceName() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort2");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml2Port, portNumber);

        // Create a SAML Token with an AudienceRestrictionCondition
        ConditionsBean conditions = new ConditionsBean();
        List<AudienceRestrictionBean> audienceRestrictions = new ArrayList<>();
        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList(
            service.getServiceName().toString()));
        audienceRestrictions.add(audienceRestriction);
        conditions.setAudienceRestrictions(audienceRestrictions);

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setConditions(conditions);
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        saml2Port.doubleIt(25);
    }

    @org.junit.Test
    public void testDisableAudienceRestrictionValidation() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort2");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml2Port, portNumber);

        // Create a SAML Token with an AudienceRestrictionCondition
        ConditionsBean conditions = new ConditionsBean();
        List<AudienceRestrictionBean> audienceRestrictions = new ArrayList<>();
        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList(
            service.getServiceName().toString() + ".xyz"));
        audienceRestrictions.add(audienceRestriction);
        conditions.setAudienceRestrictions(audienceRestrictions);

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler();
        callbackHandler.setConditions(conditions);
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        // It should fail with validation enabled
        try {
            saml2Port.doubleIt(25);
            fail("Failure expected on unknown AudienceRestriction");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        // It should pass with validation disabled
        portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort3");
        saml2Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, portNumber);

        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );
        saml2Port.doubleIt(25);

        // It should pass because we explicitly allow the given audience restriction
        portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort4");
        saml2Port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(saml2Port, portNumber);

        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );
        saml2Port.doubleIt(25);
    }

    @org.junit.Test
    public void testSaml2DifferentAlgorithms() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2EndorsingTransportPort");
        DoubleItPortType saml2Port =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(saml2Port, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(saml2Port);
        }

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, true);
        callbackHandler.setSignatureAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        callbackHandler.setDigestAlgorithm(WSS4JConstants.SHA256);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        ((BindingProvider)saml2Port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        int result = saml2Port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)saml2Port).close();
        bus.shutdown(true);
    }

}
