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
package org.apache.cxf.systest.sts.claims;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TestParam;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.systest.sts.deployment.StaxDoubleItServer;
import org.apache.cxf.systest.sts.deployment.StaxSTSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test sending claims that are defined in the policy of the WSDL to the STS for evaluation.
 * The SAML token is tested on the service side for the correct claims (role) information via a
 * custom validator.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class ClaimsTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STAX_STSPORT = allocatePort(StaxSTSServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);
    private static final String STAX_PORT = allocatePort(StaxDoubleItServer.class);

    final TestParam test;

    public ClaimsTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            ClaimsTest.class.getResource("cxf-service.xml"),
            ClaimsTest.class.getResource("stax-cxf-service.xml")
        )));
        assertTrue(launchServer(new StaxSTSServer()));
    }

    @Parameters(name = "{0}")
    public static TestParam[] data() {
        return new TestParam[] {new TestParam(PORT, false, STSPORT),
                                new TestParam(PORT, true, STSPORT),
                                new TestParam(STAX_PORT, false, STSPORT),
                                new TestParam(STAX_PORT, true, STSPORT),

                                new TestParam(PORT, false, STAX_STSPORT),
                                new TestParam(PORT, true, STAX_STSPORT),
                                new TestParam(STAX_PORT, false, STAX_STSPORT),
                                new TestParam(STAX_PORT, true, STAX_STSPORT),
        };
    }

    @org.junit.Test
    public void testSaml1Claims() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = ClaimsTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1ClaimsPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportClaimsPort, test.getPort());

        SecurityTestUtil.updateSTSPort((BindingProvider)transportClaimsPort, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportClaimsPort);
        }

        doubleIt(transportClaimsPort, 25);

        ((java.io.Closeable)transportClaimsPort).close();
    }

    @org.junit.Test
    public void testSaml1CustomClaims() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = ClaimsTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1CustomClaimsPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(transportClaimsPort, test.getPort());

        SecurityTestUtil.updateSTSPort((BindingProvider)transportClaimsPort, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportClaimsPort);
        }

        doubleIt(transportClaimsPort, 25);

        ((java.io.Closeable)transportClaimsPort).close();
    }

    @org.junit.Test
    public void testSaml2CustomClaims() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = ClaimsTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2CustomClaimsPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(transportClaimsPort, test.getPort());

        SecurityTestUtil.updateSTSPort((BindingProvider)transportClaimsPort, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportClaimsPort);
        }

        doubleIt(transportClaimsPort, 25);

        ((java.io.Closeable)transportClaimsPort).close();
    }

    @org.junit.Test
    public void testSaml1WrongClaims() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = ClaimsTest.class.getResource("DoubleItWrongClaims.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1ClaimsPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(transportClaimsPort, test.getPort());

        SecurityTestUtil.updateSTSPort((BindingProvider)transportClaimsPort, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportClaimsPort);
        }

        try {
            doubleIt(transportClaimsPort, 25);
            fail("Expected Exception");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)transportClaimsPort).close();
    }

    @org.junit.Test
    public void testSaml1ClaimsWrongRole() throws Exception {
        createBus(getClass().getResource("cxf-bad-client.xml").toString());

        URL wsdl = ClaimsTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1FailingClaimsPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(transportClaimsPort, test.getPort());

        SecurityTestUtil.updateSTSPort((BindingProvider)transportClaimsPort, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportClaimsPort);
        }

        try {
            doubleIt(transportClaimsPort, 25);
            fail("Expected Exception");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)transportClaimsPort).close();
    }

    @org.junit.Test
    public void testSaml2Claims() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = ClaimsTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2ClaimsPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(transportClaimsPort, test.getPort());

        SecurityTestUtil.updateSTSPort((BindingProvider)transportClaimsPort, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportClaimsPort);
        }

        doubleIt(transportClaimsPort, 25);

        ((java.io.Closeable)transportClaimsPort).close();
    }

    @org.junit.Test
    public void testSaml2WrongClaims() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = ClaimsTest.class.getResource("DoubleItWrongClaims.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2ClaimsPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(transportClaimsPort, test.getPort());

        SecurityTestUtil.updateSTSPort((BindingProvider)transportClaimsPort, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportClaimsPort);
        }

        try {
            doubleIt(transportClaimsPort, 25);
            fail("Expected Exception");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)transportClaimsPort).close();
    }

    // In this test, the WSDL the client is using has no Claims Element (however the service
    // is using a WSDL that requires Claims). A CallbackHandler is used to send the Claims
    // Element to the STS.
    @org.junit.Test
    public void testSaml2ClaimsCallbackHandler() throws Exception {
        createBus(getClass().getResource("cxf-client-cbhandler.xml").toString());

        URL wsdl = ClaimsTest.class.getResource("DoubleItNoClaims.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2ClaimsPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(transportClaimsPort, test.getPort());

        SecurityTestUtil.updateSTSPort((BindingProvider)transportClaimsPort, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportClaimsPort);
        }

        doubleIt(transportClaimsPort, 25);

        ((java.io.Closeable)transportClaimsPort).close();
    }

    // In this test, the WSDL the client is using has no Claims Element (however the service
    // is using a WSDL that requires Claims). A CallbackHandler is used to send the Claims
    // Element to the STS.
    @org.junit.Test
    public void testSaml2ClaimsCallbackHandler2() throws Exception {
        createBus(getClass().getResource("cxf-client-cbhandler2.xml").toString());

        URL wsdl = ClaimsTest.class.getResource("DoubleItNoClaims.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2ClaimsPort");
        DoubleItPortType transportClaimsPort =
                service.getPort(portQName, DoubleItPortType.class);

        updateAddressPort(transportClaimsPort, test.getPort());

        SecurityTestUtil.updateSTSPort((BindingProvider)transportClaimsPort, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportClaimsPort);
        }

        doubleIt(transportClaimsPort, 25);

        ((java.io.Closeable)transportClaimsPort).close();
    }

    @org.junit.Test
    public void testSaml2ChildClaims() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = ClaimsTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2ChildClaimsPort");
        DoubleItPortType transportClaimsPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportClaimsPort, PORT);

        doubleIt(transportClaimsPort, 25);

        ((java.io.Closeable)transportClaimsPort).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
