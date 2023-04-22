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

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItSwaPortType;
import org.example.schema.doubleit.DoubleIt3;
import org.example.schema.doubleit.DoubleItResponse;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A set of tests for the SwA specification (SOAP with Attachments)
 */
public class SWAActionTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

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
    public void testSWASignatureContentAction() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SWAActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SWAActionTest.class.getResource("DoubleItSwa.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSWASignatureContentActionPort");
        DoubleItSwaPortType port =
                service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, PORT);

        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        DoubleItResponse response = port.doubleIt3(doubleIt, "12345".getBytes());
        assertEquals(50, response.getDoubledNumber());

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSWASignatureCompleteAction() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SWAActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SWAActionTest.class.getResource("DoubleItSwa.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSWASignatureCompleteActionPort");
        DoubleItSwaPortType port =
                service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, PORT);

        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        DoubleItResponse response = port.doubleIt3(doubleIt, "12345".getBytes());
        assertEquals(50, response.getDoubledNumber());

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSWAEncryptionContentAction() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SWAActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SWAActionTest.class.getResource("DoubleItSwa.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSWAEncryptionContentActionPort");
        DoubleItSwaPortType port =
                service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, PORT);

        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        DoubleItResponse response = port.doubleIt3(doubleIt, "12345".getBytes());
        assertEquals(50, response.getDoubledNumber());

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSWAEncryptionCompleteAction() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SWAActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SWAActionTest.class.getResource("DoubleItSwa.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSWAEncryptionCompleteActionPort");
        DoubleItSwaPortType port =
                service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, PORT);

        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        DoubleItResponse response = port.doubleIt3(doubleIt, "12345".getBytes());
        assertEquals(50, response.getDoubledNumber());

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSWASignatureEncryptionContentAction() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SWAActionTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SWAActionTest.class.getResource("DoubleItSwa.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSWASignatureEncryptionContentActionPort");
        DoubleItSwaPortType port =
                service.getPort(portQName, DoubleItSwaPortType.class);
        updateAddressPort(port, PORT);

        DoubleIt3 doubleIt = new DoubleIt3();
        doubleIt.setNumberToDouble(25);
        DoubleItResponse response = port.doubleIt3(doubleIt, "12345".getBytes());
        assertEquals(50, response.getDoubledNumber());

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }
}
