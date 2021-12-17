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
package org.apache.cxf.systest.sts.cross_domain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some tests that illustrate how CXF clients can get tokens from different STS instances for
 * service invocations.
 */
public class CrossDomainTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);

    private static final String WSDL_FILTERED = "DoubleItFiltered.wsdl";

    @BeforeClass
    public static void startServers() throws Exception {
        try (
            BufferedReader r = new BufferedReader(
                new InputStreamReader(CrossDomainTest.class.getResourceAsStream("DoubleIt.wsdl")));
            BufferedWriter w = Files.newBufferedWriter(Paths.get(URI.create(CrossDomainTest.class
                .getResource("DoubleIt.wsdl").toString().replace("DoubleIt.wsdl", WSDL_FILTERED))))) {

            String s;
            while ((s = r.readLine()) != null) {
                w.write(s.replace("${testutil.ports.STSServer.2}", STSPORT2));
            }
        }

        assertTrue(launchServer(new DoubleItServer(
            CrossDomainTest.class.getResource("cxf-service.xml"))));

        assertTrue(launchServer(new STSServer(
            CrossDomainTest.class.getResource("cxf-sts-saml1.xml"))));
        assertTrue(launchServer(new STSServer(
            CrossDomainTest.class.getResource("cxf-sts-saml2.xml"))));
    }

    // In this test, the CXF client has two STSClients configured. The "default" STSClient config points to
    // STS "b". This STS has an IssuedToken policy that requires a token from STS "a".
    @org.junit.Test
    public void testCrossDomain() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = CrossDomainTest.class.getResource(WSDL_FILTERED);
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItCrossDomainPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        doubleIt(transportPort, 25);

        ((java.io.Closeable)transportPort).close();
    }

    // The Service references STS "b". The WSDL of STS "b" has an IssuedToken that references STS "a".
    // So the client gets the WSDL of "b" via WS-MEX, which in turn has an IssuedToken policy.
    // The client has a configured STSClient for this + uses it to get a token from "a", and in
    // turn to use the returned token to get a token from "b", to access the service.
    @org.junit.Test
    public void testCrossDomainMEX() throws Exception {
        createBus(getClass().getResource("cxf-client-mex.xml").toString());

        URL wsdl = CrossDomainTest.class.getResource(WSDL_FILTERED);
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItCrossDomainMEXPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        doubleIt(transportPort, 25);

        ((java.io.Closeable)transportPort).close();
    }

    // Here the service references STS "b". The WSDL of STS "b" has an IssuedToken. However our STS
    // client config references STS "b". This could lead to an infinite loop - this test is to make
    // sure that this doesn't happen.
    @org.junit.Test
    public void testIssuedTokenPointingToSameSTS() throws Exception {
        createBus(getClass().getResource("cxf-client-b.xml").toString());

        URL wsdl = CrossDomainTest.class.getResource(WSDL_FILTERED);
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItCrossDomainMEXPort");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportPort, PORT);

        // Transport port
        try {
            doubleIt(transportPort, 25);
            fail("Failure expected on talking to an STS with an IssuedToken policy that points to the same STS");
        } catch (SOAPFaultException ex) {
            String expectedError =
                "Calling an STS with an IssuedToken policy that points to the same STS is not allowed";
            assertTrue(ex.getMessage().contains(expectedError));
        }

        ((java.io.Closeable)transportPort).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
