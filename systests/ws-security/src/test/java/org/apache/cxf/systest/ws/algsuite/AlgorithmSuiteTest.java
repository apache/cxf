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

package org.apache.cxf.systest.ws.algsuite;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This is a test for AlgorithmSuites. Essentially it checks that a service endpoint will
 * reject a client request that uses a different AlgorithmSuite. It tests both DOM + StAX
 * clients against the DOM server.
 */
public class AlgorithmSuiteTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);

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
        stopAllServers();
    }

    @org.junit.Test
    public void testSecurityPolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AlgorithmSuiteTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = AlgorithmSuiteTest.class.getResource("DoubleItAlgSuite.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetric128Port");

        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // This should succeed as the client + server policies match
        // DOM
        assertEquals(50, port.doubleIt(25));

        // Streaming
        SecurityTestUtil.enableStreaming(port);
        assertEquals(50, port.doubleIt(25));

        portQName = new QName(NAMESPACE, "DoubleItSymmetric128Port2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // This should fail as the client uses Basic128Rsa15 + the server uses Basic128
        try {
            // DOM
            port.doubleIt(25);
            fail("Failure expected on Rsa15 AlgorithmSuite");
        } catch (Exception ex) {
            // expected
        }

        try {
            // Streaming
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on Rsa15 AlgorithmSuite");
        } catch (Exception ex) {
            // expected
        }


        // This should fail as the client uses Basic256 + the server uses Basic128
        if (TestUtilities.checkUnrestrictedPoliciesInstalled()) {
            portQName = new QName(NAMESPACE, "DoubleItSymmetric128Port3");
            port = service.getPort(portQName, DoubleItPortType.class);
            updateAddressPort(port, PORT);

            // This should fail as the client uses Basic128Rsa15 + the server uses Basic128
            try {
                // DOM
                port.doubleIt(25);
                fail("Failure expected on Basic256 AlgorithmSuite");
            } catch (Exception ex) {
                // expected
            }

            try {
                // Streaming
                SecurityTestUtil.enableStreaming(port);
                port.doubleIt(25);
                fail("Failure expected on Basic256 AlgorithmSuite");
            } catch (Exception ex) {
                // expected
            }
        }

        bus.shutdown(true);
    }

    @org.junit.Test
    public void testCombinedPolicy() throws Exception {

        if (!TestUtilities.checkUnrestrictedPoliciesInstalled()) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AlgorithmSuiteTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = AlgorithmSuiteTest.class.getResource("DoubleItAlgSuite.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // The client + server use Basic256 (but there is a sp:TripleDesRsa15 policy in the
        // WSDL as well)
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricCombinedPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // DOM
        assertEquals(50, port.doubleIt(25));

        // Streaming
        SecurityTestUtil.enableStreaming(port);
        assertEquals(50, port.doubleIt(25));

        bus.shutdown(true);
    }

    @org.junit.Test
    public void testManualConfigurationEncryption() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AlgorithmSuiteTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = AlgorithmSuiteTest.class.getResource("DoubleItAlgSuite.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptionOAEPPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // This should succeed as the client + server settings match
        assertEquals(50, port.doubleIt(25));

        portQName = new QName(NAMESPACE, "DoubleItEncryptionOAEPPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // This should fail as the client uses RSA 1.5 + the server uses RSA OAEP
        try {
            port.doubleIt(25);
            fail("Failure expected on Rsa15");
        } catch (Exception ex) {
            // expected
        }

        // This should fail as the client uses AES-256 and the server uses AES-128
        if (TestUtilities.checkUnrestrictedPoliciesInstalled()) {
            portQName = new QName(NAMESPACE, "DoubleItEncryptionOAEPPort3");
            port = service.getPort(portQName, DoubleItPortType.class);
            updateAddressPort(port, PORT);

            // This should fail as the client uses AES-256 and the server uses AES-128
            try {
                port.doubleIt(25);
                fail("Failure expected on AES-256");
            } catch (Exception ex) {
                // expected
            }
        }

        bus.shutdown(true);
    }

    @org.junit.Test
    public void testManualConfigurationSignature() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AlgorithmSuiteTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = AlgorithmSuiteTest.class.getResource("DoubleItAlgSuite.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSignaturePort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // This should succeed as the client + server settings match
        assertEquals(50, port.doubleIt(25));

        // This should fail as the client uses uses RSA-SHA256 + the server uses RSA-SHA1
        if (TestUtilities.checkUnrestrictedPoliciesInstalled()) {
            portQName = new QName(NAMESPACE, "DoubleItSignaturePort2");
            port = service.getPort(portQName, DoubleItPortType.class);
            updateAddressPort(port, PORT);

            // This should fail as the client uses uses RSA-SHA256 + the server uses RSA-SHA1
            try {
                port.doubleIt(25);
                fail("Failure expected on SHA-256");
            } catch (Exception ex) {
                // expected
            }
        }

        bus.shutdown(true);
    }

    @org.junit.Test
    public void testInclusiveC14NPolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AlgorithmSuiteTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = AlgorithmSuiteTest.class.getResource("DoubleItAlgSuite.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetric128InclusivePort");

        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // This should succeed as the client + server policies match
        // DOM
        assertEquals(50, port.doubleIt(25));

        // Streaming
        SecurityTestUtil.enableStreaming(port);
        assertEquals(50, port.doubleIt(25));

        portQName = new QName(NAMESPACE, "DoubleItSymmetric128InclusivePort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // This should fail as the client uses Exclusive C14N for the signature c14n method
        // + the server uses Inclusive C14n
        try {
            // DOM
            port.doubleIt(25);
            fail("Failure expected on Exclusive C14n");
        } catch (Exception ex) {
            // expected
        }

        try {
            // Streaming
            SecurityTestUtil.enableStreaming(port);
            port.doubleIt(25);
            fail("Failure expected on Exclusive C14n");
        } catch (Exception ex) {
            // expected
        }

        bus.shutdown(true);
    }

    @org.junit.Test
    public void testMultipleAlgorithmSuitesPolicy() throws Exception {

        if (!TestUtilities.checkUnrestrictedPoliciesInstalled()) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = AlgorithmSuiteTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = AlgorithmSuiteTest.class.getResource("DoubleItAlgSuite.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        QName portQName = new QName(NAMESPACE, "DoubleItMultipleAlgSuitesPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        // DOM
        assertEquals(50, port.doubleIt(25));

        // Streaming
        SecurityTestUtil.enableStreaming(port);
        assertEquals(50, port.doubleIt(25));

        bus.shutdown(true);
    }
}