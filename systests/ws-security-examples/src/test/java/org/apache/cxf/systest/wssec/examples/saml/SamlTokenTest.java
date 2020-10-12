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

package org.apache.cxf.systest.wssec.examples.saml;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.wssec.examples.common.SecurityTestUtil;
import org.apache.cxf.systest.wssec.examples.common.TestParam;
import org.apache.cxf.systest.wssec.examples.sts.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.trust.STSClient;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertTrue;

/**
 * A set of tests for SAML Tokens using policies defined in the OASIS spec:
 * "WS-SecurityPolicy Examples Version 1.0".
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class SamlTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String STAX_PORT = allocatePort(StaxServer.class);
    static final String PORT2 = allocatePort(Server.class, 2);
    static final String STAX_PORT2 = allocatePort(StaxServer.class, 2);
    static final String STS_PORT = allocatePort(STSServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    final TestParam test;

    public SamlTokenTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxServer.class, true)
        );
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(STSServer.class, true)
        );
    }

    @Parameters(name = "{0}")
    public static Collection<TestParam> data() {

        return Arrays.asList(new TestParam[] {new TestParam(PORT, false),
                                              new TestParam(PORT, true),
                                              new TestParam(STAX_PORT, false),
                                              new TestParam(STAX_PORT, true),
        });
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    /**
     * 2.3.1.1 (WSS1.0) SAML1.1 Assertion (Bearer)
     */
    @org.junit.Test
    public void testBearer() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItBearerPort");
        DoubleItPortType samlPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(samlPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(samlPort);
        }

        samlPort.doubleIt(25);

        ((java.io.Closeable)samlPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.3.1.2 (WSS1.0) SAML1.1 Assertion (Sender Vouches) over SSL
     */
    @org.junit.Test
    public void testTLSSenderVouches() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTLSSenderVouchesPort");
        DoubleItPortType samlPort =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(samlPort, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(samlPort);
        }

        samlPort.doubleIt(25);

        ((java.io.Closeable)samlPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.3.1.3 (WSS1.0) SAML1.1 Assertion (HK) over SSL
     */
    @org.junit.Test
    public void testTLSHOKSignedEndorsing() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTLSHOKSignedEndorsingPort");
        DoubleItPortType samlPort =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(samlPort, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(samlPort);
        }

        samlPort.doubleIt(25);

        ((java.io.Closeable)samlPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.3.1.4 (WSS1.0) SAML1.1 Sender Vouches with X.509 Certificates, Sign, Optional Encrypt
     */
    @org.junit.Test
    public void testAsymmetricSigned() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSignedPort");
        DoubleItPortType samlPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(samlPort, test.getPort());

        samlPort.doubleIt(25);

        ((java.io.Closeable)samlPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.3.1.5 (WSS1.0) SAML1.1 Holder of Key, Sign, Optional Encrypt
     */
    @org.junit.Test
    public void testAsymmetricInitiator() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricInitiatorPort");
        DoubleItPortType samlPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(samlPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(samlPort);
        }

        samlPort.doubleIt(25);

        ((java.io.Closeable)samlPort).close();
        bus.shutdown(true);
    }


    /**
     * 2.3.2.1 (WSS1.1) SAML 2.0 Bearer
     */
    @org.junit.Test
    public void testAsymmetricSaml2Bearer() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricSaml2BearerPort");
        DoubleItPortType samlPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(samlPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(samlPort);
        }

        samlPort.doubleIt(25);

        ((java.io.Closeable)samlPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.3.2.2 (WSS1.1) SAML2.0 Sender Vouches over SSL
     */
    @org.junit.Test
    public void testTLSSenderVouchesSaml2() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTLSSenderVouchesSaml2Port");
        DoubleItPortType samlPort =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(samlPort, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(samlPort);
        }

        samlPort.doubleIt(25);

        ((java.io.Closeable)samlPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.3.2.3 (WSS1.1) SAML2.0 HoK over SSL
     */
    @org.junit.Test
    public void testTLSHOKSignedEndorsingSaml2() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTLSHOKSignedEndorsingSaml2Port");
        DoubleItPortType samlPort =
                service.getPort(portQName, DoubleItPortType.class);
        String portNumber = PORT2;
        if (STAX_PORT.equals(test.getPort())) {
            portNumber = STAX_PORT2;
        }
        updateAddressPort(samlPort, portNumber);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(samlPort);
        }

        samlPort.doubleIt(25);

        ((java.io.Closeable)samlPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.3.2.4 (WSS1.1) SAML1.1/2.0 Sender Vouches with X.509 Certificate, Sign, Encrypt
     */
    @org.junit.Test
    public void testSymmetricSV() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricSVPort");
        DoubleItPortType samlPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(samlPort, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(samlPort);
        }

        // TODO Endorsing Streaming not supported yet Streaming
        if (!test.isStreaming()) {
            samlPort.doubleIt(25);
        }

        ((java.io.Closeable)samlPort).close();
        bus.shutdown(true);
    }

    /**
     * 2.3.2.5 (WSS1.1) SAML1.1/2.0 Holder of Key, Sign, Encrypt
     */
    @org.junit.Test
    public void testSymmetricIssuedToken() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlTokenTest.class.getResource("DoubleItSaml.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSymmetricIssuedTokenPort");
        DoubleItPortType samlPort =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(samlPort, test.getPort());
        updateSTSPort((BindingProvider)samlPort, STS_PORT);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(samlPort);
        }

        // TODO Endorsing SAML not supported Streaming
        if (!test.isStreaming()) {
            samlPort.doubleIt(25);
        }

        ((java.io.Closeable)samlPort).close();
        bus.shutdown(true);
    }

    private static void updateSTSPort(BindingProvider p, String port) {
        STSClient stsClient = (STSClient)p.getRequestContext()
                 .get(org.apache.cxf.rt.security.SecurityConstants.STS_CLIENT);
        if (stsClient != null) {
            String location = stsClient.getWsdlLocation();
            if (location.contains("8080")) {
                stsClient.setWsdlLocation(location.replace("8080", port));
            } else if (location.contains("8443")) {
                stsClient.setWsdlLocation(location.replace("8443", port));
            }
        }
    }
}
