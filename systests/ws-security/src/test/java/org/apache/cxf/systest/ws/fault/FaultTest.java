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

package org.apache.cxf.systest.ws.fault;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.wss4j.common.WSS4JConstants;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A set of tests for (signing and encrypting) SOAP Faults.
 */
public class FaultTest extends AbstractBusClientServerTestBase {
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
    public void testSoap11() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = FaultTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = FaultTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSoap11Port");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);

        // Make a successful invocation
        ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "alice");
        assertEquals(50, utPort.doubleIt(25));

        // Now make an invocation using another username
        ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "bob");
        ((BindingProvider)utPort).getRequestContext().put("security.password", "password");
        try {
            utPort.doubleIt(25);
            fail("Expected failure on bob");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("This is a fault"));
        }

        // Switch to the StAX stack
        SecurityTestUtil.enableStreaming(utPort);

        // Make a successful invocation
        ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "alice");
        assertEquals(50, utPort.doubleIt(25));

        // Now make an invocation using another username
        ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "bob");
        ((BindingProvider)utPort).getRequestContext().put("security.password", "password");
        try {
            utPort.doubleIt(25);
            fail("Expected failure on bob");
        } catch (Exception ex) {
            // TODO assertTrue(ex.getMessage().contains("This is a fault"));
        }

        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSoap12() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = FaultTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = FaultTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSoap12Port");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);

        // Make a successful invocation
        ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "alice");
        assertEquals(50, utPort.doubleIt(25));

        // Now make an invocation using another username
        ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "bob");
        ((BindingProvider)utPort).getRequestContext().put("security.password", "password");
        try {
            utPort.doubleIt(25);
            fail("Expected failure on bob");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("This is a fault"));
        }
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSoap12Mtom() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = FaultTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = FaultTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSoap12MtomPort");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);

        // Make a successful invocation
        ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "alice");
        assertEquals(50, utPort.doubleIt(25));

        // Now make an invocation using another username
        ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "bob");
        ((BindingProvider)utPort).getRequestContext().put("security.password", "password");
        try {
            utPort.doubleIt(25);
            fail("Expected failure on bob");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("This is a fault"));
        }
        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSoap12Dispatch() throws Exception {
        createBus();
        BusFactory.setDefaultBus(getBus());
        URL wsdl = FaultTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSoap12DispatchPort");

        Dispatch<DOMSource> dispatch =
            service.createDispatch(portQName, DOMSource.class, Service.Mode.PAYLOAD);

        // Creating a DOMSource Object for the request
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document requestDoc = db.newDocument();
        Element root = requestDoc.createElementNS("http://www.example.org/schema/DoubleIt", "ns2:DoubleIt");
        root.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns:ns2", "http://www.example.org/schema/DoubleIt");
        Element number = requestDoc.createElementNS(null, "numberToDouble");
        number.setTextContent("25");
        root.appendChild(number);
        requestDoc.appendChild(root);
        DOMSource request = new DOMSource(requestDoc);

        // Add WS-Security configuration
        Client client = ((DispatchImpl<DOMSource>) dispatch).getClient();
        client.getRequestContext().put(
            SecurityConstants.CALLBACK_HANDLER,
            "org.apache.cxf.systest.ws.common.KeystorePasswordCallback"
        );
        client.getRequestContext().put(
            SecurityConstants.ENCRYPT_PROPERTIES,
            "bob.properties"
        );
        client.getRequestContext().put(SecurityConstants.ENCRYPT_USERNAME, "bob");

        updateAddressPort(dispatch, PORT);

        // Make a successful request
        client.getRequestContext().put(SecurityConstants.USERNAME, "alice");
        DOMSource response = dispatch.invoke(request);
        assertNotNull(response);

        // Now make an invocation using another username
        client.getRequestContext().put(SecurityConstants.USERNAME, "bob");
        client.getRequestContext().put("security.password", "password");
        try {
            dispatch.invoke(request);
            fail("Expected failure on bob");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("This is a fault"));
        }

        client.destroy();
    }

    @org.junit.Test
    public void testSoap11PolicyWithParts() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = FaultTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = FaultTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSoap11PolicyWithPartsPort");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);

        // Make a successful invocation
        ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "alice");
        assertEquals(50, utPort.doubleIt(25));

        // Now make an invocation using another username
        ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "bob");
        ((BindingProvider)utPort).getRequestContext().put("security.password", "password");
        try {
            utPort.doubleIt(25);
            fail("Expected failure on bob");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("This is a fault"));
        }

        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }

    // See DoubleItPortTypeImplJavaFirst
    @org.junit.Test
    public void testJavaFirst() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = FaultTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = FaultTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItJavaFirstPort");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);

        // Make a successful invocation
        ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "alice");
        assertEquals(50, utPort.doubleIt(25));

        // Now make an invocation using another username
        ((BindingProvider)utPort).getRequestContext().put(SecurityConstants.USERNAME, "bob");
        ((BindingProvider)utPort).getRequestContext().put("security.password", "password");
        try {
            utPort.doubleIt(25);
            fail("Expected failure on bob");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("This is a fault"));
        }

        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testUnsecuredSoap11Action() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = FaultTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = FaultTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSoap11UnsecuredPort");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);

        try {
            utPort.doubleIt(25);
            fail("Expected failure on bob");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("This is a fault"));
        }

        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testUnsecuredSoap11ActionStAX() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = FaultTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = FaultTest.class.getResource("DoubleItFault.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSoap11UnsecuredPort2");
        DoubleItPortType utPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(utPort, PORT);

        try {
            utPort.doubleIt(25);
            fail("Expected failure on bob");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("This is a fault"));
        }

        ((java.io.Closeable)utPort).close();
        bus.shutdown(true);
    }
}
