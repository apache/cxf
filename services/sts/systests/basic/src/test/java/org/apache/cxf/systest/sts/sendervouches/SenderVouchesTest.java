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
package org.apache.cxf.systest.sts.sendervouches;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TestParam;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.systest.sts.deployment.StaxDoubleItServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * In this test case, a CXF client sends a Username Token via (1-way) TLS to a CXF intermediary.
 * The intermediary validates the UsernameToken, and then inserts the username into a SAML
 * Assertion which it signs and sends to a provider (via TLS).
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class SenderVouchesTest extends AbstractBusClientServerTestBase {

    static final String PORT2 = allocatePort(DoubleItServer.class, 2);
    static final String STAX_PORT2 = allocatePort(StaxDoubleItServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);
    private static final String STAX_PORT = allocatePort(StaxDoubleItServer.class);

    final TestParam test;

    public SenderVouchesTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            SenderVouchesTest.class.getResource("cxf-service.xml"),
            SenderVouchesTest.class.getResource("cxf-stax-service.xml")
            )));
        assertTrue(launchServer(new DoubleItServer(
            SenderVouchesTest.class.getResource("cxf-intermediary.xml"),
            SenderVouchesTest.class.getResource("cxf-stax-intermediary.xml")
            )));
    }

    @Parameters(name = "{0}")
    public static TestParam[] data() {
        return new TestParam[] {new TestParam(PORT, false),
                                new TestParam(PORT, true),
                                new TestParam(STAX_PORT, false),
                                new TestParam(STAX_PORT, true),
        };
    }
    @org.junit.Test
    public void testSenderVouches() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = SenderVouchesTest.class.getResource("DoubleIt.wsdl");
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

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
