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
package org.apache.cxf.systest.sts.x509;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.sts.common.TokenTestUtils;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The CXF client gets a token from the STS by authenticating via an X.509 Cert over the asymmetric binding,
 * and then sends it to the CXF endpoint using the asymmetric binding. The SAML Token contains a role, that is
 * required by the endpoint to access the service correctly. Therefore, client authentication is done by the
 * asymmetric signature, authorization is done by extracting roles from the SAML Token.
 */
public class X509AsymmetricBindingTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static final String PORT = allocatePort(AsymmetricServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(AsymmetricServer.class, true)
        );
        STSServer stsServer = new STSServer();
        stsServer.setContext("cxf-x509.xml");
        assertTrue(launchServer(stsServer));
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @org.junit.Test
    public void testX509SAML2() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = X509AsymmetricBindingTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = X509AsymmetricBindingTest.class.getResource("DoubleItAsymmetric.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSAML2Port");
        DoubleItPortType port =
            service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, PORT);

        TokenTestUtils.updateSTSPort((BindingProvider)port, STSPORT2);

        doubleIt(port, 30);

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2, resp);
    }
}
