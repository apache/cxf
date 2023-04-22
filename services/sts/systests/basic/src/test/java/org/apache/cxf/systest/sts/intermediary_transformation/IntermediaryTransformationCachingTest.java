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
import jakarta.xml.ws.soap.SOAPFaultException;
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
 * STS instance. The retrieved token is sent to the service provider via (2-way) TLS. The STSClient is disabled
 * after two invocations, meaning that the Intermediary client must rely on its cache to get tokens.
 */
public class IntermediaryTransformationCachingTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);

    static final String PORT2 = allocatePort(DoubleItServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            IntermediaryTransformationCachingTest.class.getResource("cxf-service.xml"),
            IntermediaryTransformationCachingTest.class.getResource("cxf-intermediary-caching.xml")))
        );

        assertTrue(launchServer(new STSServer("cxf-transport.xml")));
    }

    @org.junit.Test
    public void testIntermediaryTransformationCaching() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = IntermediaryTransformationCachingTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1EndorsingPort");
        DoubleItPortType alicePort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(alicePort, PORT);

        TokenTestUtils.updateSTSPort((BindingProvider)alicePort, STSPORT);

        ((BindingProvider)alicePort).getRequestContext().put(SecurityConstants.USERNAME, "alice");

        // Make initial successful invocation (for "alice")
        doubleIt(alicePort, 25);

        // Make another successful invocation for "bob"
        DoubleItPortType bobPort = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(bobPort, PORT);
        TokenTestUtils.updateSTSPort((BindingProvider)bobPort, STSPORT);

        ((BindingProvider)bobPort).getRequestContext().put(SecurityConstants.USERNAME, "bob");
        doubleIt(bobPort, 30);

        // Make another invocation for "bob" - this should work as the intermediary caches the token
        // even though its STSClient is disabled after the second invocation
        doubleIt(bobPort, 35);

        // Make another invocation for "alice" - this should work as the intermediary caches the token
        // even though its STSClient is disabled after the first invocation
        doubleIt(alicePort, 40);

        // Now make an invocation for "myservicekey"
        DoubleItPortType servicePort = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(servicePort, PORT);
        TokenTestUtils.updateSTSPort((BindingProvider)servicePort, STSPORT);

        ((BindingProvider)servicePort).getRequestContext().put(SecurityConstants.USERNAME, "myservicekey");

        // Make invocation for "myservicekey"...this should fail as the intermediary's STS client is disabled
        try {
            doubleIt(servicePort, 45);
            fail("Expected failure on a cache retrieval failure");
        } catch (SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)alicePort).close();
        ((java.io.Closeable)bobPort).close();
        ((java.io.Closeable)servicePort).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
