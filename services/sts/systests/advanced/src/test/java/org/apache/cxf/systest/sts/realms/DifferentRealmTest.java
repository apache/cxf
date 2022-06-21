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
package org.apache.cxf.systest.sts.realms;

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
 * In this test, a CXF client obtains a SAML token from an STS in one realm and sends it to a CXF
 * endpoint. The CXF endpoint dispatches it for validation to a different STS.
 */
public class DifferentRealmTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            DifferentRealmTest.class.getResource("cxf-service.xml")
        )));
        assertTrue(launchServer(new STSServer(
            DifferentRealmTest.class.getResource("cxf-sts-saml1.xml"))));
        assertTrue(launchServer(new STSServer(
            DifferentRealmTest.class.getResource("cxf-sts-saml2.xml"))));
    }

    /**
     * In this test, a token is issued by the first STS in realm "A". The second STS is configured
     * to trust the signing cert of realm "A" (via a cert constraint) and so authentication succeeds.
     */
    @org.junit.Test
    public void testKnownRealm() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = DifferentRealmTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItRealmAPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        doubleIt(transportPort, 25);

        ((java.io.Closeable)transportPort).close();
    }

    /**
     * In this test, a token is issued by the first STS in the default realm. The second STS is
     * configured to trust the signing cert of the default realm (via a cert constraint) and so
     * authentication succeeds.
     */
    @org.junit.Test
    public void testDefaultRealm() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = DifferentRealmTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItDefaultRealmPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        doubleIt(transportPort, 25);

        ((java.io.Closeable)transportPort).close();
    }

    /**
     * In this test, a token is issued by the first STS in realm "C". The second STS is not
     * configured to trust realm "C" (via a cert constraint) and so authentication does not succeed.
     */
    @org.junit.Test
    public void testUnknownRealm() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = DifferentRealmTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItRealmCPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        try {
            doubleIt(transportPort, 25);
        } catch (Exception ex) {
            // expected
        }

        ((java.io.Closeable)transportPort).close();
    }

    /**
     * In this test, a token is issued by the first STS in realm "A". The second STS is configured
     * to trust realm "A" (via a cert constraint) and so authentication succeeds. The service
     * endpoint also sends a tokenType (SAML2) to the second STS, and so the IdentityMapper is
     * invoked to transform the authenticated principal into a principal in the current realm.
     */
    @org.junit.Test
    public void testRealmTransform() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = DifferentRealmTest.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItRealmTransformPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        doubleIt(transportPort, 25);

        ((java.io.Closeable)transportPort).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
