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

package org.apache.cxf.systest.ws.tokens;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.common.TestParam;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This is a test for various properties associated with SupportingTokens, i.e.
 * Signed, Encrypted etc.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class SupportingTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String TLS_PORT = allocatePort(TLSServer.class);
    static final String STAX_PORT = allocatePort(StaxServer.class);
    static final String TLS_STAX_PORT = allocatePort(TLSStaxServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    final TestParam test;

    public SupportingTokenTest(TestParam type) {
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
                   launchServer(TLSServer.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxServer.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(TLSStaxServer.class, true)
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
    public void testSignedSupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SupportingTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SupportingTokenTest.class.getResource("DoubleItTokens.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItSignedSupportingPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is not signing the UsernameToken
        portQName = new QName(NAMESPACE, "DoubleItSignedSupportingPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not signing the UsernameToken");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "The received token does not match the signed supporting token requirement";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("UsernameToken not satisfied"));
        }

        // This should fail, as the client is (encrypting) but not signing the UsernameToken
        portQName = new QName(NAMESPACE, "DoubleItSignedSupportingPort3");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not signing the UsernameToken");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "The received token does not match the signed supporting token requirement";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("UsernameToken not satisfied"));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testEncryptedSupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SupportingTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SupportingTokenTest.class.getResource("DoubleItTokens.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptedSupportingPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is not encrypting the UsernameToken
        portQName = new QName(NAMESPACE, "DoubleItEncryptedSupportingPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not encrypting the UsernameToken");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "The received token does not match the encrypted supporting token requirement";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("UsernameToken not satisfied"));
        }

        // This should fail, as the client is (signing) but not encrypting the UsernameToken
        portQName = new QName(NAMESPACE, "DoubleItEncryptedSupportingPort3");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not encrypting the UsernameToken");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "The received token does not match the encrypted supporting token requirement";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("UsernameToken not satisfied"));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testEncryptedSupportingOverTLS() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SupportingTokenTest.class.getResource("tls-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SupportingTokenTest.class.getResource("DoubleItTokens.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptedSupportingPort4");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);

        if (PORT.equals(test.getPort())) {
            updateAddressPort(port, TLS_PORT);
        } else if (STAX_PORT.equals(test.getPort())) {
            updateAddressPort(port, TLS_STAX_PORT);
        }

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is not encrypting the UsernameToken
        portQName = new QName(NAMESPACE, "DoubleItEncryptedSupportingPort5");
        port = service.getPort(portQName, DoubleItPortType.class);

        if (PORT.equals(test.getPort())) {
            updateAddressPort(port, TLS_PORT);
        } else if (STAX_PORT.equals(test.getPort())) {
            updateAddressPort(port, TLS_STAX_PORT);
        }

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not encrypting the UsernameToken");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "The received token does not match the encrypted supporting token requirement";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("UsernameToken not satisfied"));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignedEncryptedSupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SupportingTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SupportingTokenTest.class.getResource("DoubleItTokens.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItSignedEncryptedSupportingPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is not encrypting the UsernameToken
        portQName = new QName(NAMESPACE, "DoubleItSignedEncryptedSupportingPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not encrypting the UsernameToken");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error =
                "The received token does not match the signed encrypted supporting token requirement";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("UsernameToken not satisfied"));
        }

        // This should fail, as the client is (encrypting) but not signing the UsernameToken
        portQName = new QName(NAMESPACE, "DoubleItSignedEncryptedSupportingPort3");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not encrypting the UsernameToken");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error =
                "The received token does not match the signed encrypted supporting token requirement";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("UsernameToken not satisfied"));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

}
