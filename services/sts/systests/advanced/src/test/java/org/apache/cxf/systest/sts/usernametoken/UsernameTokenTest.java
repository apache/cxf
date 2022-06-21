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
package org.apache.cxf.systest.sts.usernametoken;

import java.net.URL;

import javax.xml.namespace.QName;

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
 * In this test case, a CXF client sends a Username Token via (1-way) TLS to a CXF provider.
 * The provider dispatches the Username Token to an STS for validation (via TLS). It also
 * includes a test where the service provider sends the token for validation using the
 * WS-Trust "Issue" binding, and sending the token "OnBehalfOf". Roles are also requested, and
 * access is only granted to the service if the "admin-user" role is in effect.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class UsernameTokenTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STAX_STSPORT = allocatePort(StaxSTSServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);
    private static final String STAX_PORT = allocatePort(StaxDoubleItServer.class);

    final TestParam test;

    public UsernameTokenTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new StaxDoubleItServer(
            UsernameTokenTest.class.getResource("cxf-service.xml"),
            UsernameTokenTest.class.getResource("stax-cxf-service.xml")
        )));
        assertTrue(launchServer(new StaxSTSServer()));
    }

    @Parameters(name = "{0}")
    public static TestParam[] data() {
        return new TestParam[] {new TestParam(PORT, false, ""),
                                new TestParam(PORT, true, ""),
                                new TestParam(STAX_PORT, false, ""),
                                new TestParam(STAX_PORT, true, ""),
        };
    }

    @org.junit.Test
    public void testUsernameToken() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = UsernameTokenTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTPort");
        DoubleItPortType transportUTPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportUTPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportUTPort);
        }

        doubleIt(transportUTPort, 25);

        ((java.io.Closeable)transportUTPort).close();
    }

    @org.junit.Test
    public void testBadUsernameToken() throws Exception {
        createBus(getClass().getResource("cxf-bad-client.xml").toString());

        URL wsdl = UsernameTokenTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTPort");
        DoubleItPortType transportUTPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportUTPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportUTPort);
        }

        try {
            doubleIt(transportUTPort, 30);
            fail("Expected failure on a bad password");
        } catch (jakarta.xml.ws.soap.SOAPFaultException fault) {
            // expected
        }

        ((java.io.Closeable)transportUTPort).close();
    }

    @org.junit.Test
    public void testUsernameTokenAuthorization() throws Exception {
        // Token transformation is not supported for the streaming code
        if (STAX_PORT.equals(test.getPort())) {
            return;
        }

        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = UsernameTokenTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTAuthorizationPort");
        DoubleItPortType transportUTPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportUTPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportUTPort);
        }

        doubleIt(transportUTPort, 25);

        ((java.io.Closeable)transportUTPort).close();
    }

    @org.junit.Test
    public void testUnauthorizedUsernameToken() throws Exception {
        // Token transformation is not supported for the streaming code
        if (STAX_PORT.equals(test.getPort())) {
            return;
        }

        createBus(getClass().getResource("cxf-bad-client.xml").toString());

        URL wsdl = UsernameTokenTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTAuthorizationPort");
        DoubleItPortType transportUTPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportUTPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportUTPort);
        }

        try {
            doubleIt(transportUTPort, 30);
            fail("Expected failure on a bad password");
        } catch (jakarta.xml.ws.soap.SOAPFaultException fault) {
            // expected
        }

        ((java.io.Closeable)transportUTPort).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
