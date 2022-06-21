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
package org.apache.cxf.systest.sts.intermediary_transformation;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.systest.sts.common.TokenTestUtils;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * In this test case, a CXF client sends a Username Token via (1-way) TLS to a STS instance, and
 * receives a (HOK) SAML 1.1 Assertion. This is then sent via (1-way) TLS to an Intermediary
 * service provider. The intermediary service provider validates the token, and then the
 * Intermediary client uses delegation to dispatch the received token (via OnBehalfOf) to another
 * STS instance. This returns another (HOK) SAML 2 Assertion which is sent to the service provider
 * via (2-way) TLS.
 */
public class IntermediaryTransformationTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);

    static final String PORT2 = allocatePort(DoubleItServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            IntermediaryTransformationTest.class.getResource("cxf-service.xml"),
            IntermediaryTransformationTest.class.getResource("cxf-intermediary.xml")))
        );

        assertTrue(launchServer(new STSServer("cxf-transport.xml")));
    }

    @org.junit.Test
    public void testIntermediaryTransformation() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = IntermediaryTransformationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1EndorsingPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        TokenTestUtils.updateSTSPort((BindingProvider)transportPort, STSPORT);

        ((BindingProvider)transportPort).getRequestContext().put(SecurityConstants.USERNAME, "alice");
        doubleIt(transportPort, 25);

        ((java.io.Closeable)transportPort).close();
    }

    @org.junit.Test
    public void testIntermediaryTransformationBadClient() throws Exception {
        createBus(getClass().getResource("cxf-bad-client.xml").toString());

        URL wsdl = IntermediaryTransformationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1EndorsingPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        TokenTestUtils.updateSTSPort((BindingProvider)transportPort, STSPORT);

        try {
            doubleIt(transportPort, 30);
            fail("Expected failure on a bad user");
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)transportPort).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
