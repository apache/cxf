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

package org.apache.cxf.systest.ws.bindings;

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
 * This is a test for various properties associated with a security binding.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class BindingPropertiesTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String STAX_PORT = allocatePort(StaxServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    final TestParam test;

    public BindingPropertiesTest(TestParam type) {
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

    // Child of Body is signed which conflicts with the OnlySignEntireHeadersAndBody property
    @org.junit.Test
    public void testOnlySignEntireHeadersAndBody() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = BindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // This should work, as OnlySignEntireHeadersAndBody is not specified
        QName portQName = new QName(NAMESPACE, "DoubleItNotOnlySignPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as OnlySignEntireHeadersAndBody is specified
        portQName = new QName(NAMESPACE, "DoubleItOnlySignPort");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on OnlySignEntireHeadersAndBody property");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "OnlySignEntireHeadersAndBody";
            assertTrue(ex.getMessage().contains(error));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testEncryptSignature() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = BindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptSignaturePort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is not encrypting the signature is specified
        portQName = new QName(NAMESPACE, "DoubleItEncryptSignaturePort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not encrypting the signature property");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "The signature is not protected";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("Signature must be encrypted"));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testIncludeTimestamp() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = BindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItIncludeTimestampPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is not sending a Timestamp
        portQName = new QName(NAMESPACE, "DoubleItIncludeTimestampPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not sending a Timestamp");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "Received Timestamp does not match the requirements";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("Timestamp must be present"));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testEncryptBeforeSigning() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = BindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItEncryptBeforeSigningPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is not following the correct steps for this property
        portQName = new QName(NAMESPACE, "DoubleItEncryptBeforeSigningPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not encrypting before signing");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "Not encrypted before signed";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("EncryptBeforeSigning"));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignBeforeEncrypting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = BindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItSignBeforeEncryptingPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is not following the correct steps for this property
        portQName = new QName(NAMESPACE, "DoubleItSignBeforeEncryptingPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        // DOM
        try {
            port.doubleIt(25);
            fail("Failure expected on not signing before encrypting");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "Not signed before encrypted";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("SignBeforeEncrypting"));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testTimestampFirst() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = BindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItTimestampFirstPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        // TODO Timestamp First/Last validation not working - see WSS-444
        if (!STAX_PORT.equals(test.getPort())) {
            assertEquals(50, port.doubleIt(25));
        }

        // This should fail, as the client is sending the timestamp last
        portQName = new QName(NAMESPACE, "DoubleItTimestampFirstPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            // TODO Timestamp First/Last validation not working - see WSS-444
            if (!STAX_PORT.equals(test.getPort())) {
                port.doubleIt(25);
                fail("Failure expected on on sending the timestamp last");
            }
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "Layout does not match the requirements";
            assertTrue(ex.getMessage().contains(error));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testTimestampLast() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = BindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItTimestampLastPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        // TODO Timestamp First/Last validation not working - see WSS-444
        if (!STAX_PORT.equals(test.getPort())) {
            assertEquals(50, port.doubleIt(25));
        }

        // This should fail, as the client is sending the timestamp first
        portQName = new QName(NAMESPACE, "DoubleItTimestampLastPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            // TODO Timestamp First/Last validation not working - see WSS-444
            if (!STAX_PORT.equals(test.getPort())) {
                port.doubleIt(25);
                fail("Failure expected on sending the timestamp first");
            }
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "Layout does not match the requirements";
            assertTrue(ex.getMessage().contains(error));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testStrict() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = BindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItStrictPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        // TODO Strict validation not working - see WSS-444
        if (!STAX_PORT.equals(test.getPort())) {
            assertEquals(50, port.doubleIt(25));
        }

        // This should fail, as the client is sending the timestamp last
        portQName = new QName(NAMESPACE, "DoubleItStrictPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            // TODO Strict validation not working - see WSS-444
            if (!STAX_PORT.equals(test.getPort())) {
                port.doubleIt(25);
                fail("Failure expected on sending the timestamp last");
            }
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error = "Layout does not match the requirements";
            assertTrue(ex.getMessage().contains(error));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testTokenProtection() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = BindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItTokenProtectionPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        // TODO DOM server not working
        if (!PORT.equals(test.getPort())) {
            assertEquals(50, port.doubleIt(25));
        }

        // This should fail, as the property is not enabled
        portQName = new QName(NAMESPACE, "DoubleItTokenProtectionPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            // TODO DOM server not working
            if (!PORT.equals(test.getPort())) {
                port.doubleIt(25);
                fail("Failure expected on not protecting the token");
            }
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // String error = "Layout does not match the requirements";
            // assertTrue(ex.getMessage().contains(error));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Not strictly a BindingProperty but a property of WSS11...
    @org.junit.Test
    public void testSignatureConfirmation() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = BindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // This should work, as SignatureConfirmation is enabled
        QName portQName = new QName(NAMESPACE, "DoubleItSignatureConfirmationPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // This should fail, as SignatureConfirmation is not enabled
        portQName = new QName(NAMESPACE, "DoubleItSignatureConfirmationPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        try {
            port.doubleIt(25);
            fail("Failure expected on not enabling SignatureConfirmation");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignatureConfirmationEncBeforeSigning() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BindingPropertiesTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = BindingPropertiesTest.class.getResource("DoubleItBindings.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        QName portQName = new QName(NAMESPACE, "DoubleItSignatureConfirmationEncBeforeSigningPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }


}
