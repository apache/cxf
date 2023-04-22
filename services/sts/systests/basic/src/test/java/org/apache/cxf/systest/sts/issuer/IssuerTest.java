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
package org.apache.cxf.systest.sts.issuer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Some tests where the STS address is not hard-coded in the client
 */
public class IssuerTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(IssuerSTSServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(DoubleItServer.class);

    private static final String WSDL_FILTERED = "DoubleItFiltered.wsdl";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            IssuerTest.class.getResource("cxf-service.xml")
        )));

        assertTrue(launchServer(new IssuerSTSServer()));

        try (
            BufferedReader r = new BufferedReader(
                new InputStreamReader(IssuerTest.class.getResourceAsStream("DoubleIt.wsdl")));
            BufferedWriter w = Files.newBufferedWriter(Paths.get(URI.create(
                IssuerTest.class.getResource("DoubleIt.wsdl").toString().replace("DoubleIt.wsdl", WSDL_FILTERED))))) {

            String s;
            while ((s = r.readLine()) != null) {
                w.write(s.replace("${testutil.ports.IssuerSTSServer}", STSPORT));
            }
        }
    }

    // In this test, the client uses the address defined in the <sp:Issuer> address in the policy
    // of the service provider to contact the STS. The client is configured with the STS's service
    // Policy. Useful if you want a simple way to avoid hardcoding the STS host/port in the client.
    @org.junit.Test
    public void testSAML1Issuer() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = IssuerTest.class.getResource(WSDL_FILTERED);
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML1Port");
        DoubleItPortType transportSaml1Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml1Port, PORT);

        doubleIt(transportSaml1Port, 25);

        ((java.io.Closeable)transportSaml1Port).close();
    }

    // Test getting the STS details via WS-MEX
    @org.junit.Test
    public void testSAML2MEX() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = IssuerTest.class.getResource(WSDL_FILTERED);
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2Port");
        DoubleItPortType transportSaml2Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml2Port, PORT);

        doubleIt(transportSaml2Port, 25);

        ((java.io.Closeable)transportSaml2Port).close();
    }

    // Test getting the STS details via WS-MEX + SOAP 1.2
    @org.junit.Test
    public void testSAML2MEXSoap12() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

        URL wsdl = IssuerTest.class.getResource(WSDL_FILTERED);
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2Soap12Port");
        DoubleItPortType transportSaml2Port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(transportSaml2Port, PORT);

        doubleIt(transportSaml2Port, 25);

        ((java.io.Closeable)transportSaml2Port).close();
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2L, resp);
    }
}
