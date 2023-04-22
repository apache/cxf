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

package org.apache.cxf.systest.ws.swa;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.common.TestParam;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.example.contract.doubleit.DoubleItSwaPortType;
import org.example.schema.doubleit.DoubleIt3;
import org.example.schema.doubleit.DoubleItResponse;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A set of tests for the SwA specification (SOAP with Attachments) via WS-SecurityPolicy.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class SWAPolicyTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(PolicyServer.class);
    public static final String STAX_PORT = allocatePort(StaxPolicyServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    final TestParam test;

    public SWAPolicyTest(TestParam type) {
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
    public void testSWASignatureContentPolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SWAPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SWAPolicyTest.class.getResource("DoubleItSwa.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSWASignatureContentPolicyPort");
        DoubleItSwaPortType port =
                service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            enableStreaming(port);
        }

        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        DoubleItResponse response = port.doubleIt3(doubleIt, "12345".getBytes());
        assertEquals(50, response.getDoubledNumber());

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSWASignatureCompletePolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SWAPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SWAPolicyTest.class.getResource("DoubleItSwa.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSWASignatureCompletePolicyPort");
        DoubleItSwaPortType port =
                service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            enableStreaming(port);
        }

        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        DoubleItResponse response = port.doubleIt3(doubleIt, "12345".getBytes());
        assertEquals(50, response.getDoubledNumber());

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSWAEncryptionPolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SWAPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SWAPolicyTest.class.getResource("DoubleItSwa.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSWAEncryptionPolicyPort");
        DoubleItSwaPortType port =
                service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            enableStreaming(port);
        }

        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        DoubleItResponse response = port.doubleIt3(doubleIt, "12345".getBytes());
        assertEquals(50, response.getDoubledNumber());

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSWAEncryptionContentPolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SWAPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SWAPolicyTest.class.getResource("DoubleItSwa.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSWAEncryptionContentPolicyPort");
        DoubleItSwaPortType port =
                service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            enableStreaming(port);
        }

        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        DoubleItResponse response = port.doubleIt3(doubleIt, "12345".getBytes());
        assertEquals(50, response.getDoubledNumber());

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSWACombinedPolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SWAPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SWAPolicyTest.class.getResource("DoubleItSwa.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSWACombinedPolicyPort");
        DoubleItSwaPortType port =
                service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            enableStreaming(port);
        }

        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        DoubleItResponse response = port.doubleIt3(doubleIt, "12345".getBytes());
        assertEquals(50, response.getDoubledNumber());

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSWACombinedDerivedPolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SWAPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SWAPolicyTest.class.getResource("DoubleItSwa.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSWACombinedDerivedPolicyPort");
        DoubleItSwaPortType port =
                service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            enableStreaming(port);
        }

        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        DoubleItResponse response = port.doubleIt3(doubleIt, "12345".getBytes());
        assertEquals(50, response.getDoubledNumber());

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSWACombinedAsymmetricPolicy() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SWAPolicyTest.class.getResource("policy-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SWAPolicyTest.class.getResource("DoubleItSwa.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSWACombinedAsymmetricPolicyPort");
        DoubleItSwaPortType port =
                service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            enableStreaming(port);
        }

        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        DoubleItResponse response = port.doubleIt3(doubleIt, "12345".getBytes());
        assertEquals(50, response.getDoubledNumber());

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    static void enableStreaming(DoubleItSwaPortType port) {
        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.ENABLE_STREAMING_SECURITY, "true"
        );
        ((BindingProvider)port).getResponseContext().put(
            SecurityConstants.ENABLE_STREAMING_SECURITY, "true"
        );
    }

}
