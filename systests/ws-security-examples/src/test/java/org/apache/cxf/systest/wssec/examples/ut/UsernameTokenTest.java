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

package org.apache.cxf.systest.wssec.examples.ut;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.wssec.examples.common.SecurityTestUtil;
import org.apache.cxf.systest.wssec.examples.common.TestParam;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertTrue;

/**
 * A set of tests for Username Tokens using policies defined in the OASIS spec:
 * "WS-SecurityPolicy Examples Version 1.0".
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class UsernameTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String PORT2 = allocatePort(Server.class, 2);
    static final String STAX_PORT = allocatePort(StaxServer.class);
    static final String STAX_PORT2 = allocatePort(StaxServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    final TestParam test;

    public UsernameTokenTest(TestParam type) {
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

    /**
     * 2.1.1.1 UsernameToken with plain text password
     */
    @org.junit.Test
    public void testPlaintext() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPlaintextPort");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }

        utPort.doubleIt(25);

        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.1.1.2 UsernameToken without password
     */
    @org.junit.Test
    public void testPlaintextNoPassword() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItPlaintextNoPasswordPort");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }

        utPort.doubleIt(25);

        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.1.1.3 UsernameToken with timestamp, nonce and password hash
     */
    @org.junit.Test
    public void testDigest() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItDigestPort");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }

        utPort.doubleIt(25);

        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.1.2.1 UsernameToken as supporting token
     */
    @org.junit.Test
    public void testTLSSupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTLSSupportingPort");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(utPort, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }

        utPort.doubleIt(25);

        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.1.3 (WSS 1.0) UsernameToken with Mutual X.509v3 Authentication, Sign, Encrypt
     */
    @org.junit.Test
    public void testAsymmetricSESupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSESupportingPort");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }

        utPort.doubleIt(25);

        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.1.3.1 (WSS 1.0) Encrypted UsernameToken with X.509v3
     */
    @org.junit.Test
    public void testAsymmetricEncrSupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricEncrSupportingPort");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }

        utPort.doubleIt(25);

        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.1.4 (WSS 1.1), User Name with Certificates, Sign, Encrypt
     */
    @org.junit.Test
    public void testSymmetricSESupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = UsernameTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = UsernameTokenTest.class.getResource("DoubleItUt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSESupportingPort");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(utPort);
        }

        utPort.doubleIt(25);

        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
}
