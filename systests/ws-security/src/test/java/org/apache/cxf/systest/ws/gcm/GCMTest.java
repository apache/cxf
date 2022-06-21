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

package org.apache.cxf.systest.ws.gcm;

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
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A set of tests for GCM algorithms using custom WS-SecurityPolicy expressions.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class GCMTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String STAX_PORT = allocatePort(StaxServer.class);
    static final String MGF_PORT = allocatePort(MGFServer.class);
    static final String MGF_STAX_PORT = allocatePort(MGFStaxServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static boolean unrestrictedPoliciesInstalled =
            TestUtilities.checkUnrestrictedPoliciesInstalled();

    final TestParam test;

    public GCMTest(TestParam type) {
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
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(MGFServer.class, true)
        );
        assertTrue(
                  "Server failed to launch",
                  // run the server in the same process
                  // set this to false to fork
                  launchServer(MGFStaxServer.class, true)
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
    public void testAESGCM128() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = GCMTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = GCMTest.class.getResource("DoubleItGCM.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItGCM128Port");
        DoubleItPortType gcmPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(gcmPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(gcmPort);
        }

        assertEquals(50, gcmPort.doubleIt(25));

        ((java.io.Closeable)gcmPort).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testAESGCM192() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = GCMTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = GCMTest.class.getResource("DoubleItGCM.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItGCM192Port");
        DoubleItPortType gcmPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(gcmPort, test.getPort());


        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(gcmPort);
        }

        assertEquals(50, gcmPort.doubleIt(25));

        ((java.io.Closeable)gcmPort).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testAESGCM256() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = GCMTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = GCMTest.class.getResource("DoubleItGCM.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItGCM256Port");
        DoubleItPortType gcmPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(gcmPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(gcmPort);
        }

        assertEquals(50, gcmPort.doubleIt(25));

        ((java.io.Closeable)gcmPort).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testAESGCM256MGFSHA256() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = GCMTest.class.getResource("mgf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = GCMTest.class.getResource("DoubleItGCM.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItGCM256MGFSHA256Port");
        DoubleItPortType gcmPort =
                service.getPort(portQName, DoubleItPortType.class);

        String port = MGF_PORT;
        if (STAX_PORT.equals(test.getPort())) {
            port = MGF_STAX_PORT;
        }
        updateAddressPort(gcmPort, port);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(gcmPort);
        }

        assertEquals(50, gcmPort.doubleIt(25));

        ((java.io.Closeable)gcmPort).close();
        bus.shutdown(true);
    }

    // Same as above but with explicitly adding a ds:DigestMethod of SHA-256 as well
    @org.junit.Test
    public void testAESGCM256MGFSHA256Digest() throws Exception {
        if (!unrestrictedPoliciesInstalled) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = GCMTest.class.getResource("mgf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = GCMTest.class.getResource("DoubleItGCM.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItGCM256MGFSHA256DigestPort");
        DoubleItPortType gcmPort =
                service.getPort(portQName, DoubleItPortType.class);

        String port = MGF_PORT;
        if (STAX_PORT.equals(test.getPort())) {
            port = MGF_STAX_PORT;
        }
        updateAddressPort(gcmPort, port);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(gcmPort);
        }

        assertEquals(50, gcmPort.doubleIt(25));

        ((java.io.Closeable)gcmPort).close();
        bus.shutdown(true);
    }

}
