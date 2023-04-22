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
package org.apache.cxf.systest.sts.transformation;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * In this test case, a CXF client sends a Username Token via (1-way) TLS to a CXF provider.
 * The provider dispatches the Username Token to an STS for validation (via TLS), and also
 * send a TokenType corresponding to a SAML2 Assertion. The STS will create the requested
 * SAML Assertion after validation and return it to the provider.
 *
 * In the second test, the service will also send some claims to the STS for inclusion in the
 * SAML Token, and validate the result.
 */
public class TransformationTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            TransformationTest.class.getResource("cxf-service.xml")
        )));
        assertTrue(launchServer(new STSServer()));
    }

    @org.junit.Test
    public void testTokenTransformation() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TransformationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTPort");
        DoubleItPortType transportUTPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportUTPort, PORT);

        doubleIt(transportUTPort, 25);

        ((java.io.Closeable)transportUTPort).close();
    }

    @org.junit.Test
    public void testTokenTransformationClaims() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = TransformationTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportUTClaimsPort");
        DoubleItPortType transportUTPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportUTPort, PORT);

        doubleIt(transportUTPort, 25);

        ((java.io.Closeable)transportUTPort).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
