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
package org.apache.cxf.systest.sts.bearer;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TestParam;
import org.apache.cxf.systest.sts.common.TokenTestUtils;
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

/**
 * Test the Bearer TokenType over TLS.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class BearerTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STAX_STSPORT = allocatePort(StaxSTSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);
    static final String STAX_STSPORT2 = allocatePort(StaxSTSServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);
    private static final String STAX_PORT = allocatePort(StaxDoubleItServer.class);

    final TestParam test;

    public BearerTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            BearerTest.class.getResource("cxf-service.xml"),
            BearerTest.class.getResource("cxf-stax-service.xml")))
        );
        assertTrue(launchServer(new STSServer(
            "cxf-transport.xml",
            "stax-cxf-transport.xml")));
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
    public void testSAML2Bearer() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = BearerTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2BearerPort");
        DoubleItPortType transportSaml2Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml2Port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml2Port, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml2Port);
        }

        doubleIt(transportSaml2Port, 45);

        ((java.io.Closeable)transportSaml2Port).close();
    }

    @org.junit.Test
    public void testSAML2BearerNoBinding() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = BearerTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2BearerPort2");
        DoubleItPortType transportSaml2Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml2Port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)transportSaml2Port, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(transportSaml2Port);
        }

        doubleIt(transportSaml2Port, 45);

        ((java.io.Closeable)transportSaml2Port).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
