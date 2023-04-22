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
package org.apache.cxf.systest.sts.binarysecuritytoken;

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
 * In this test case, a CXF client sends a BinarySecurityToken via the Asymmetric message
 * binding to a CXF provider. The provider dispatches the BinarySecurityToken to an STS for
 * validation (via TLS).
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class BinarySecurityTokenTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STAX_STSPORT = allocatePort(StaxSTSServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);
    private static final String STAX_PORT = allocatePort(StaxDoubleItServer.class);

    final TestParam test;

    public BinarySecurityTokenTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            BinarySecurityTokenTest.class.getResource("cxf-service.xml"),
            BinarySecurityTokenTest.class.getResource("stax-cxf-service.xml")
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
    public void testBinarySecurityToken() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = BinarySecurityTokenTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricBSTPort");
        DoubleItPortType asymmetricBSTPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(asymmetricBSTPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(asymmetricBSTPort);
        }

        doubleIt(asymmetricBSTPort, 25);

        ((java.io.Closeable)asymmetricBSTPort).close();
    }

    @org.junit.Test
    public void testBadBinarySecurityToken() throws Exception {
        createBus(getClass().getResource("cxf-bad-client.xml").toString());

        URL wsdl = BinarySecurityTokenTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricBSTPort");
        DoubleItPortType asymmetricBSTPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(asymmetricBSTPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(asymmetricBSTPort);
        }

        try {
            doubleIt(asymmetricBSTPort, 30);
            fail("Expected failure on a bad cert");
        } catch (jakarta.xml.ws.soap.SOAPFaultException fault) {
            // expected
        }

        ((java.io.Closeable)asymmetricBSTPort).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
