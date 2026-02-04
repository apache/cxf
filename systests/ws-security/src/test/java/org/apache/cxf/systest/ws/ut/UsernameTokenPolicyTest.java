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

package org.apache.cxf.systest.ws.ut;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor.SoapOutEndingInterceptor;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.common.TestParam;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A set of (negative) tests for Username Tokens policies over the Transport Binding.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class UsernameTokenPolicyTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(PolicyServer.class);
    static final String STAX_PORT = allocatePort(StaxPolicyServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    final TestParam test;

    public UsernameTokenPolicyTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(PolicyServer.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxPolicyServer.class, true)
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
    public void testSupportingToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenPolicyTest.class.getResource("DoubleItUtPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSupportingTokenPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is not sending a UsernameToken Supporting Token
        portQName = new QName(NAMESPACE, "DoubleItSupportingTokenPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not sending a UsernameToken Supporting Token");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "These policy alternatives can not be satisfied";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("UsernameToken not satisfied"));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testPlaintextPassword() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenPolicyTest.class.getResource("DoubleItUtPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPlaintextPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is sending a hashed password
        portQName = new QName(NAMESPACE, "DoubleItPlaintextPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on a hashed password");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "These policy alternatives can not be satisfied";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("password must not be hashed"));
        }

        // This should fail, as the client is not sending any password
        portQName = new QName(NAMESPACE, "DoubleItPlaintextPort3");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not sending a password");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    @org.junit.Test
    public void testOnlyHasUsernameTokenWithoutMustUnderstand() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenPolicyTest.class.getResource("DoubleItUtPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPlaintextPort4");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());
        Client client = ClientProxy.getClient(port);
        client.getRequestContext().put(SecurityConstants.MUST_UNDERSTAND, false);
        client.getOutInterceptors().add(new CheckMustUnderstandHeader());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        assertEquals(50, port.doubleIt(25));
        client.getRequestContext().put(SecurityConstants.MUST_UNDERSTAND, true);
        assertEquals(50, port.doubleIt(25));
        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testHashPassword() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenPolicyTest.class.getResource("DoubleItUtPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItHashPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is sending a plaintext password
        portQName = new QName(NAMESPACE, "DoubleItHashPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on a plaintext password");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "These policy alternatives can not be satisfied";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("UsernameToken does not contain a hashed password"));
        }

        // This should fail, as the client is not sending any password
        portQName = new QName(NAMESPACE, "DoubleItHashPort3");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not sending a password");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testCreated() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenPolicyTest.class.getResource("DoubleItUtPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItCreatedPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is not sending a Created element
        portQName = new QName(NAMESPACE, "DoubleItCreatedPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not sending a Created element");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "These policy alternatives can not be satisfied";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("UsernameToken does not contain a created"));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testNonce() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenPolicyTest.class.getResource("DoubleItUtPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItNoncePort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is not sending a Nonce element
        portQName = new QName(NAMESPACE, "DoubleItNoncePort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not sending a Nonce element");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "These policy alternatives can not be satisfied";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("UsernameToken does not contain a nonce"));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
    class CheckMustUnderstandHeader extends AbstractSoapInterceptor {
        
        CheckMustUnderstandHeader() {
            super(Phase.WRITE_ENDING);
            addBefore(SoapOutEndingInterceptor.class.getName());
        }
        
        @Override
        public void handleMessage(SoapMessage message) throws Fault {
            SoapHeader securityHeader = (SoapHeader)message.getHeader(
                new QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd", 
                          "Security"));
            boolean mustUnderstand =
                MessageUtils.getContextualBoolean(
                    message, SecurityConstants.MUST_UNDERSTAND, true
                );
            assertEquals(securityHeader.isMustUnderstand(), mustUnderstand);
        }
    
    }
    
    // https://issues.apache.org/jira/browse/CXF-9167
    // Here we're sending a UsernameToken with a password unknown by the default CallbackHandler on the server side,
    // but we are overriding the UsernameToken processor to use a CallbackHandler that knows the password.
    @org.junit.Test
    public void testSupportingTokenCustomProcessor() throws Exception {

        if (test.getPort().equals(STAX_PORT)) {
            // We don't support custom processors with streaming for now
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenPolicyTest.class.getResource("DoubleItUtPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSupportingTokenPort3");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
    
}
