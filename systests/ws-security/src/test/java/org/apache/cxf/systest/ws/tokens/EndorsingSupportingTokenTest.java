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
public class EndorsingSupportingTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(EndorsingServer.class);
    static final String STAX_PORT = allocatePort(StaxEndorsingServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    final TestParam test;

    public EndorsingSupportingTokenTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(EndorsingServer.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxEndorsingServer.class, true)
        );
    }

    @Parameters(name = "{0}")
    public static Collection<TestParam> data() {

        return Arrays.asList(new TestParam[] {new TestParam(PORT, false),
                                              new TestParam(STAX_PORT, false),
        });
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @org.junit.Test
    public void testEndorsingSupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = EndorsingSupportingTokenTest.class.getResource("endorsing-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = EndorsingSupportingTokenTest.class.getResource("DoubleItTokens.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItEndorsingSupportingPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is signing (but not endorsing) the X.509 Token
        portQName = new QName(NAMESPACE, "DoubleItEndorsingSupportingPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        try {
            port.doubleIt(25);
            fail("Failure expected on not endorsing the X.509 token");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error =
                "The received token does not match the endorsing supporting token requirement";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("EncryptedKey must sign the main signature"));
        }

        // This should fail, as the client is not endorsing the X.509 Token
        portQName = new QName(NAMESPACE, "DoubleItEndorsingSupportingPort3");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        try {
            port.doubleIt(25);
            fail("Failure expected on not endorsing the X.509 token");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error =
                "The received token does not match the endorsing supporting token requirement";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("X509Token not satisfied"));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSignedEndorsingSupporting() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = EndorsingSupportingTokenTest.class.getResource("endorsing-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = EndorsingSupportingTokenTest.class.getResource("DoubleItTokens.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);

        // Successful invocation
        QName portQName = new QName(NAMESPACE, "DoubleItSignedEndorsingSupportingPort");
        DoubleItPortType port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        assertEquals(50, port.doubleIt(25));

        // This should fail, as the client is signing (but not endorsing) the X.509 Token
        portQName = new QName(NAMESPACE, "DoubleItSignedEndorsingSupportingPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        try {
            port.doubleIt(25);
            fail("Failure expected on not endorsing the X.509 token");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error =
                "The received token does not match the signed endorsing supporting token requirement";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("EncryptedKey must sign the main signature"));
        }

        // This should fail, as the client is endorsing but not signing the X.509 Token
        portQName = new QName(NAMESPACE, "DoubleItSignedEndorsingSupportingPort3");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        try {
            port.doubleIt(25);
            fail("Failure expected on not signing the X.509 token");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            String error =
                "The received token does not match the signed endorsing supporting token requirement";
            assertTrue(ex.getMessage().contains(error)
                       || ex.getMessage().contains("X509Token not satisfied"));
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }


}
