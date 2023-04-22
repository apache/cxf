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
package org.apache.cxf.systest.sts.username_actas;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.common.TestParam;
import org.apache.cxf.systest.sts.common.TokenTestUtils;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.systest.sts.deployment.STSServer;
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
 * In this test case, a CXF client requests a Security Token from an STS, passing a username that
 * it has obtained from an unknown client as an "ActAs" element. This username is obtained
 * by parsing the SecurityConstants.USERNAME property. The client then invokes on the service
 * provider using the returned token from the STS.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class UsernameActAsTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STAX_STSPORT = allocatePort(StaxSTSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);
    static final String STAX_STSPORT2 = allocatePort(StaxSTSServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class, 2);

    final TestParam test;

    public UsernameActAsTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            UsernameActAsTest.class.getResource("cxf-service2.xml")
        )));
        assertTrue(launchServer(new STSServer(
            "cxf-x509.xml",
            "stax-cxf-x509.xml"
        )));
    }

    @Parameters(name = "{0}")
    public static TestParam[] data() {
        return new TestParam[] {new TestParam(PORT, false, STSPORT2),
                                new TestParam(PORT, true, STSPORT2),
                                new TestParam(PORT, false, STAX_STSPORT2),
                                new TestParam(PORT, true, STAX_STSPORT2),
        };
    }

    @org.junit.Test
    public void testUsernameActAs() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = UsernameActAsTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML2BearerPort");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        ((BindingProvider)port).getRequestContext().put("thread.local.request.context", "true");

        updateAddressPort(port, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)port, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        // Transport port
        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.USERNAME, "alice"
        );
        doubleIt(port, 25);

        ((java.io.Closeable)port).close();

        DoubleItPortType port2 =
            service.getPort(portQName, DoubleItPortType.class);
        ((BindingProvider)port2).getRequestContext().put("thread.local.request.context", "true");
        updateAddressPort(port2, test.getPort());

        TokenTestUtils.updateSTSPort((BindingProvider)port2, test.getStsPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port2);
        }

        ((BindingProvider)port2).getRequestContext().put(
            SecurityConstants.USERNAME, "eve"
        );
        // This time we expect a failure as the server validator doesn't accept "eve".
        try {
            doubleIt(port2, 30);
            fail("Failure expected on an unknown user");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)port2).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(2L * numToDouble, resp);
    }
}
