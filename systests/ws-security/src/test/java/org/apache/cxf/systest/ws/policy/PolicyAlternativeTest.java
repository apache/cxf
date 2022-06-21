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

package org.apache.cxf.systest.ws.policy;

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
 * This is a test for policy alternatives. The endpoint requires either a UsernameToken (insecured) OR
 * a message signature using the Asymmetric binding.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class PolicyAlternativeTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String PORT2 = allocatePort(Server.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    final TestParam test;

    public PolicyAlternativeTest(TestParam type) {
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
    }

    @Parameters(name = "{0}")
    public static Collection<TestParam> data() {

        return Arrays.asList(new TestParam[] {new TestParam(PORT, false),
                                              new TestParam(PORT, true),
        });
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    /**
     * The client uses the Asymmetric policy - this should succeed.
     */
    @org.junit.Test
    public void testAsymmetric() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PolicyAlternativeTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = PolicyAlternativeTest.class.getResource("DoubleItPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        int result = port.doubleIt(25);
        assertEquals(50, result);

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    /**
     * The client uses no security - this should fail.
     */
    @org.junit.Test
    public void testNoSecurity() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PolicyAlternativeTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = PolicyAlternativeTest.class.getResource("DoubleItPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItNoSecurityPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on no Security");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    /**
     * The client uses the UsernameToken policy - this should succeed.
     */
    @org.junit.Test
    public void testUsernameToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PolicyAlternativeTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = PolicyAlternativeTest.class.getResource("DoubleItPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItUsernameTokenPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        int result = port.doubleIt(25);
        assertEquals(50, result);

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    /**
     * The client does not require a client cert so invocation should fail
     *
     * @throws Exception
     */
    @org.junit.Test
    public void testRequireClientCertToken() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PolicyAlternativeTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = PolicyAlternativeTest.class.getResource("DoubleItPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItClientCertPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT2);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected because no client certificate");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            if (!test.isStreaming()) {
                assertTrue(ex.getMessage().contains("HttpsToken"));
            }
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    /**
     * The client uses a Transport binding policy with a Endorsing Supporting X509 Token. The client does
     * not sign part of the WSA header though and so the invocation should fail.
     */
    @org.junit.Test
    public void testTransportSupportingSigned() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PolicyAlternativeTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = PolicyAlternativeTest.class.getResource("DoubleItPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSupportingSignedPort");
        DoubleItPortType transportPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT2);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportPort);
        }

        try {
            transportPort.doubleIt(25);
            fail("Failure expected on not signing a wsa header");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)transportPort).close();
        bus.shutdown(true);
    }

    /**
     * The client uses a Transport binding policy with a Endorsing Supporting X509 Token as well as a
     * Signed Endorsing UsernameToken. Here the client is trying to trick the Service Provider as
     * the UsernameToken signs the wsa:To Header, not the X.509 Token.
     */
    @org.junit.Test
    public void testTransportUTSupportingSigned() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PolicyAlternativeTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = PolicyAlternativeTest.class.getResource("DoubleItPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTSupportingSignedPort");
        DoubleItPortType transportPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT2);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportPort);
        }

        try {
            transportPort.doubleIt(25);
            fail("Failure expected on not signing a wsa header");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)transportPort).close();
        bus.shutdown(true);
    }

    /**
     * The client uses the Asymmetric policy defined at the bus level - this should succeed.
     */
    @org.junit.Test
    public void testAsymmetricBusLevel() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = PolicyAlternativeTest.class.getResource("client-bus.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = PolicyAlternativeTest.class.getResource("DoubleItPolicy.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
}
