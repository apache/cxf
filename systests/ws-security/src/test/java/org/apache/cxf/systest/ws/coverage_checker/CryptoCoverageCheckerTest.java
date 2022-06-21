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

package org.apache.cxf.systest.ws.coverage_checker;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.common.TestParam;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxOutInterceptor;
import org.apache.wss4j.common.WSS4JConstants;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A set of tests for the CryptoCoverageChecker functionality.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class CryptoCoverageCheckerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    public static final String STAX_PORT = allocatePort(StaxServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static boolean unrestrictedPoliciesInstalled;

    static {
        unrestrictedPoliciesInstalled = TestUtilities.checkUnrestrictedPoliciesInstalled();
    };

    final TestParam test;

    public CryptoCoverageCheckerTest(TestParam type) {
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
    public void testSignedBodyTimestamp() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItBodyTimestampPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        Map<String, Object> outProps = new HashMap<>();
        outProps.put("action", "Timestamp Signature");
        outProps.put("signaturePropFile", "alice.properties");
        outProps.put("user", "alice");
        outProps.put("passwordCallbackClass",
                     "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        outProps.put("signatureParts",
                     "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;"
                     + "{}{http://docs.oasis-open.org/wss/2004/01/oasis-"
                     + "200401-wss-wssecurity-utility-1.0.xsd}Timestamp;");

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        if (test.isStreaming()) {
            WSS4JStaxOutInterceptor staxOutInterceptor = new WSS4JStaxOutInterceptor(outProps);
            bus.getOutInterceptors().add(staxOutInterceptor);
        } else {
            WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProps);
            bus.getOutInterceptors().add(outInterceptor);
        }

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignedBodyOnly() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItBodyTimestampPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        Map<String, Object> outProps = new HashMap<>();
        outProps.put("action", "Timestamp Signature");
        outProps.put("signaturePropFile", "alice.properties");
        outProps.put("user", "alice");
        outProps.put("passwordCallbackClass",
                     "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        outProps.put("signatureParts",
                     "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;");

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        if (test.isStreaming()) {
            WSS4JStaxOutInterceptor staxOutInterceptor = new WSS4JStaxOutInterceptor(outProps);
            bus.getOutInterceptors().add(staxOutInterceptor);
        } else {
            WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProps);
            bus.getOutInterceptors().add(outInterceptor);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not signing the Timestamp");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignedTimestampOnly() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItBodyTimestampPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        Map<String, Object> outProps = new HashMap<>();
        outProps.put("action", "Timestamp Signature");
        outProps.put("signaturePropFile", "alice.properties");
        outProps.put("user", "alice");
        outProps.put("passwordCallbackClass",
                     "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        outProps.put("signatureParts",
                     "{}{http://docs.oasis-open.org/wss/2004/01/oasis-"
                     + "200401-wss-wssecurity-utility-1.0.xsd}Timestamp;");

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        if (test.isStreaming()) {
            WSS4JStaxOutInterceptor staxOutInterceptor = new WSS4JStaxOutInterceptor(outProps);
            bus.getOutInterceptors().add(staxOutInterceptor);
        } else {
            WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProps);
            bus.getOutInterceptors().add(outInterceptor);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not signing the Timestamp");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignedBodyTimestampSoap12() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItBodyTimestampSoap12Port");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        Map<String, Object> outProps = new HashMap<>();
        outProps.put("action", "Timestamp Signature");
        outProps.put("signaturePropFile", "alice.properties");
        outProps.put("user", "alice");
        outProps.put("passwordCallbackClass",
                     "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        outProps.put("signatureParts",
                     "{}{http://www.w3.org/2003/05/soap-envelope}Body;"
                     + "{}{http://docs.oasis-open.org/wss/2004/01/oasis-"
                     + "200401-wss-wssecurity-utility-1.0.xsd}Timestamp;");

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        if (test.isStreaming()) {
            WSS4JStaxOutInterceptor staxOutInterceptor = new WSS4JStaxOutInterceptor(outProps);
            bus.getOutInterceptors().add(staxOutInterceptor);
        } else {
            WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProps);
            bus.getOutInterceptors().add(outInterceptor);
        }

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignedBodyOnlySoap12() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItBodyTimestampSoap12Port");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        Map<String, Object> outProps = new HashMap<>();
        outProps.put("action", "Timestamp Signature");
        outProps.put("signaturePropFile", "alice.properties");
        outProps.put("user", "alice");
        outProps.put("passwordCallbackClass",
                     "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        outProps.put("signatureParts",
                     "{}{http://www.w3.org/2003/05/soap-envelope}Body;");

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        if (test.isStreaming()) {
            WSS4JStaxOutInterceptor staxOutInterceptor = new WSS4JStaxOutInterceptor(outProps);
            bus.getOutInterceptors().add(staxOutInterceptor);
        } else {
            WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProps);
            bus.getOutInterceptors().add(outInterceptor);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not signing the Timestamp");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignedTimestampOnlySoap12() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItBodyTimestampSoap12Port");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        Map<String, Object> outProps = new HashMap<>();
        outProps.put("action", "Timestamp Signature");
        outProps.put("signaturePropFile", "alice.properties");
        outProps.put("user", "alice");
        outProps.put("passwordCallbackClass",
                     "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        outProps.put("signatureParts",
                     "{}{http://docs.oasis-open.org/wss/2004/01/oasis-"
                     + "200401-wss-wssecurity-utility-1.0.xsd}Timestamp;");

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        if (test.isStreaming()) {
            WSS4JStaxOutInterceptor staxOutInterceptor = new WSS4JStaxOutInterceptor(outProps);
            bus.getOutInterceptors().add(staxOutInterceptor);
        } else {
            WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProps);
            bus.getOutInterceptors().add(outInterceptor);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not signing the Timestamp");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignedEncryptedBody() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignedEncryptedBodyPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        Map<String, Object> outProps = new HashMap<>();
        outProps.put("action", "Timestamp Signature Encrypt");
        outProps.put("signaturePropFile", "alice.properties");
        outProps.put("encryptionPropFile", "bob.properties");
        outProps.put("user", "alice");
        outProps.put("encryptionUser", "bob");
        outProps.put("passwordCallbackClass",
                     "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        outProps.put("signatureParts",
                     "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;");
        outProps.put("encryptionParts",
                     "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;");

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        if (test.isStreaming()) {
            WSS4JStaxOutInterceptor staxOutInterceptor = new WSS4JStaxOutInterceptor(outProps);
            bus.getOutInterceptors().add(staxOutInterceptor);
        } else {
            WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProps);
            bus.getOutInterceptors().add(outInterceptor);
        }

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignedNotEncryptedBody() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignedEncryptedBodyPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        Map<String, Object> outProps = new HashMap<>();
        outProps.put("action", "Timestamp Signature Encrypt");
        outProps.put("signaturePropFile", "alice.properties");
        outProps.put("encryptionPropFile", "bob.properties");
        outProps.put("user", "alice");
        outProps.put("encryptionUser", "bob");
        outProps.put("passwordCallbackClass",
                     "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        outProps.put("signatureParts",
                     "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;");
        outProps.put("encryptionParts",
                     "{}{http://docs.oasis-open.org/wss/2004/01/oasis-"
                     + "200401-wss-wssecurity-utility-1.0.xsd}Timestamp;");

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        if (test.isStreaming()) {
            WSS4JStaxOutInterceptor staxOutInterceptor = new WSS4JStaxOutInterceptor(outProps);
            bus.getOutInterceptors().add(staxOutInterceptor);
        } else {
            WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProps);
            bus.getOutInterceptors().add(outInterceptor);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not encrypting the SOAP Body");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testWSAddressing() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItWSAPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        Map<String, Object> outProps = new HashMap<>();
        outProps.put("action", "Timestamp Signature");
        outProps.put("signaturePropFile", "alice.properties");
        outProps.put("user", "alice");
        outProps.put("passwordCallbackClass",
                     "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        outProps.put("signatureParts",
                     "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;"
                     + "{}{http://docs.oasis-open.org/wss/2004/01/oasis-"
                     + "200401-wss-wssecurity-utility-1.0.xsd}Timestamp;");

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        WSS4JStaxOutInterceptor staxOutInterceptor = null;
        WSS4JOutInterceptor outInterceptor = null;
        if (test.isStreaming()) {
            staxOutInterceptor = new WSS4JStaxOutInterceptor(outProps);
            bus.getOutInterceptors().add(staxOutInterceptor);
        } else {
            outInterceptor = new WSS4JOutInterceptor(outProps);
            bus.getOutInterceptors().add(outInterceptor);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not signing the WS-Addressing headers");
        } catch (Exception ex) {
            // expected
        }

        // Now sign the WS-Addressing headers
        outProps.put("signatureParts",
                "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;"
                + "{}{http://docs.oasis-open.org/wss/2004/01/oasis-"
                + "200401-wss-wssecurity-utility-1.0.xsd}Timestamp;"
                + "{}{http://www.w3.org/2005/08/addressing}ReplyTo;");

        if (test.isStreaming()) {
            bus.getOutInterceptors().remove(staxOutInterceptor);
            SecurityTestUtil.enableStreaming(port);
        } else {
            bus.getOutInterceptors().remove(outInterceptor);
        }

        if (test.isStreaming()) {
            staxOutInterceptor = new WSS4JStaxOutInterceptor(outProps);
            bus.getOutInterceptors().add(staxOutInterceptor);
        } else {
            outInterceptor = new WSS4JOutInterceptor(outProps);
            bus.getOutInterceptors().add(outInterceptor);
        }

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testWSAddressingOptionalSignatureParts() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItWSAPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        Map<String, Object> outProps = new HashMap<>();
        outProps.put("action", "Timestamp Signature");
        outProps.put("signaturePropFile", "alice.properties");
        outProps.put("user", "alice");
        outProps.put("passwordCallbackClass",
                     "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        outProps.put("optionalSignatureParts",
                     "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;"
                     + "{}{http://docs.oasis-open.org/wss/2004/01/oasis-"
                     + "200401-wss-wssecurity-utility-1.0.xsd}Timestamp;"
                     + "{}{http://www.w3.org/2005/08/addressing}ReplyTo;");

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        WSS4JStaxOutInterceptor staxOutInterceptor = null;
        WSS4JOutInterceptor outInterceptor = null;
        if (test.isStreaming()) {
            staxOutInterceptor = new WSS4JStaxOutInterceptor(outProps);
            bus.getOutInterceptors().add(staxOutInterceptor);
        } else {
            outInterceptor = new WSS4JOutInterceptor(outProps);
            bus.getOutInterceptors().add(outInterceptor);
        }

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Here the service is sending an secured message back to the client. For a server Fault
    // message it returns the original fault, as the CryptoCoverageChecker is configured not
    // to check a fault (see CXF-4954)
    @org.junit.Test
    public void testClientChecker() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItClientCheckerPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // This test only applies to the DOM implementation
        if (PORT.equals(test.getPort()) && !test.isStreaming()) {
            int result = port.doubleIt(25);
            assertEquals(50, result);

            // Now try with a message that will create a Fault in the SEI
            try {
                port.doubleIt(0);
                fail("Failure expected on trying to double 0");
            } catch (Exception ex) {
                assertTrue(ex.getMessage().contains("0 can't be doubled"));
            }
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Here the service is sending an secured message back to the client. For a server Fault
    // message it should return a secured Fault message as well
    @org.junit.Test
    public void testClientChecker2() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItClientCheckerPort2");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // This test only applies to the DOM implementation
        if (PORT.equals(test.getPort()) && !test.isStreaming()) {
            assertEquals(50, port.doubleIt(25));

            // Now try with a message that will create a Fault in the SEI
            try {
                port.doubleIt(0);
                fail("Failure expected on trying to double 0");
            } catch (Exception ex) {
                assertTrue(ex.getMessage().contains("0 can't be doubled"));
            }
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testEncryptedUsernameToken() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptedUsernameTokenPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        Map<String, Object> outProps = new HashMap<>();
        outProps.put("action", "UsernameToken Encrypt");
        outProps.put("encryptionPropFile", "bob.properties");
        outProps.put("user", "alice");
        outProps.put("encryptionUser", "bob");
        outProps.put("passwordCallbackClass",
                     "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        outProps.put("encryptionParts",
                     "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;"
                     + "{Element}{" + WSS4JConstants.WSSE_NS + "}UsernameToken;");

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        if (test.isStreaming()) {
            WSS4JStaxOutInterceptor staxOutInterceptor = new WSS4JStaxOutInterceptor(outProps);
            bus.getOutInterceptors().add(staxOutInterceptor);
        } else {
            WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProps);
            bus.getOutInterceptors().add(outInterceptor);
        }

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testNotEncryptedUsernameToken() throws Exception {

        if (!unrestrictedPoliciesInstalled) {
            return;
        }
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CryptoCoverageCheckerTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CryptoCoverageCheckerTest.class.getResource("DoubleItCoverageChecker.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptedUsernameTokenPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        Map<String, Object> outProps = new HashMap<>();
        outProps.put("action", "UsernameToken Encrypt");
        outProps.put("encryptionPropFile", "bob.properties");
        outProps.put("user", "alice");
        outProps.put("encryptionUser", "bob");
        outProps.put("passwordCallbackClass",
                     "org.apache.cxf.systest.ws.common.KeystorePasswordCallback");
        outProps.put("encryptionParts",
                     "{}{http://schemas.xmlsoap.org/soap/envelope/}Body;");

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        if (test.isStreaming()) {
            WSS4JStaxOutInterceptor staxOutInterceptor = new WSS4JStaxOutInterceptor(outProps);
            bus.getOutInterceptors().add(staxOutInterceptor);
        } else {
            WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProps);
            bus.getOutInterceptors().add(outInterceptor);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not encrypting the UsernameToken");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
}
