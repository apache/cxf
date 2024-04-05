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

package org.apache.cxf.systest.kerberos.wssec.kerberos;

import java.io.File;
import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.kerberos.common.SecurityTestUtil;
import org.apache.cxf.systest.kerberos.wssec.sts.STSServer;
import org.apache.cxf.systest.kerberos.wssec.sts.StaxSTSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * A set of tests for Kerberos Tokens that use an Apache Kerby instance as the KDC.
 */
public class KerberosTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = TestUtil.getPortNumber(Server.class);
    static final String STAX_PORT = TestUtil.getPortNumber(StaxServer.class);
    static final String PORT2 = TestUtil.getPortNumber(Server.class, 2);
    static final String STAX_PORT2 = TestUtil.getPortNumber(StaxServer.class, 2);

    static final String PORT3 = TestUtil.getPortNumber(Server.class, 3);
    static final String STSPORT = TestUtil.getPortNumber(STSServer.class);
    static final String STAX_STSPORT = TestUtil.getPortNumber(StaxSTSServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    private static boolean unrestrictedPoliciesInstalled =
        SecurityTestUtil.checkUnrestrictedPoliciesInstalled();

    private static boolean runTests;

    private static SimpleKdcServer kerbyServer;

    @BeforeClass
    public static void startServers() throws Exception {

        WSSConfig.init();

        //
        // This test fails with the IBM JDK
        //
        if (!"IBM Corporation".equals(System.getProperty("java.vendor"))) {
            runTests = true;
        }

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        // System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.auth.login.config", basedir + "/target/test-classes/kerberos.jaas");
        System.setProperty("java.security.krb5.conf", basedir + "/target/krb5.conf");

        kerbyServer = new SimpleKdcServer();

        kerbyServer.setKdcRealm("service.ws.apache.org");
        kerbyServer.setAllowUdp(false);
        kerbyServer.setWorkDir(new File(basedir + "/target"));

        //kerbyServer.setInnerKdcImpl(new NettyKdcServerImpl(kerbyServer.getKdcSetting()));

        kerbyServer.init();

        // Create principals
        String alice = "alice@service.ws.apache.org";
        String bob = "bob/service.ws.apache.org@service.ws.apache.org";
        String carol = "carol@service.ws.apache.org";
        String dave = "dave/service.ws.apache.org@service.ws.apache.org";

        kerbyServer.createPrincipal(alice, "alice");
        kerbyServer.createPrincipal(bob, "bob");
        kerbyServer.createPrincipal(carol, "carol");
        kerbyServer.createPrincipal(dave, "dave");

        File keytabFile = new File(basedir + "/target/dave.keytab");
        kerbyServer.exportPrincipal(dave, keytabFile);

        kerbyServer.start();

        // Launch servers
        org.junit.Assert.assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            AbstractClientServerTestBase.launchServer(Server.class, true)
        );

        org.junit.Assert.assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            AbstractClientServerTestBase.launchServer(StaxServer.class, true)
        );

        org.junit.Assert.assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            AbstractClientServerTestBase.launchServer(STSServer.class, true)
        );

        org.junit.Assert.assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            AbstractClientServerTestBase.launchServer(StaxSTSServer.class, true)
        );
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        AbstractClientServerTestBase.stopAllServers();
        if (kerbyServer != null) {
            kerbyServer.stop();
        }
    }

    @org.junit.Test
    public void testKerberosOverTransport() throws Exception {
        if (!runTests) {
            return;
        }

        String portName = "DoubleItKerberosTransportPort";
        runKerberosTest(portName, false, PORT2);
        runKerberosTest(portName, false, STAX_PORT2);
        runKerberosTest(portName, true, PORT2);
        runKerberosTest(portName, true, STAX_PORT2);
    }

    @org.junit.Test
    public void testKerberosOverTransportDifferentConfiguration() throws Exception {
        if (!runTests) {
            return;
        }

        String portName = "DoubleItKerberosTransportPort2";
        runKerberosTest(portName, false, PORT2);
        runKerberosTest(portName, false, STAX_PORT2);
        runKerberosTest(portName, true, PORT2);
        runKerberosTest(portName, true, STAX_PORT2);
    }

    @org.junit.Test
    public void testKerberosOverTransportKeytabs() throws Exception {
        if (!runTests) {
            return;
        }

        String portName = "DoubleItKerberosTransportPort3";
        runKerberosTest(portName, false, PORT2);
        runKerberosTest(portName, true, PORT2);
    }

    @org.junit.Test
    public void testKerberosOverSymmetric() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItKerberosSymmetricPort";
        runKerberosTest(portName, false, PORT);
        runKerberosTest(portName, false, STAX_PORT);
        runKerberosTest(portName, true, PORT);
        runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testKerberosOverSymmetricSupporting() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItKerberosSymmetricSupportingPort";
        runKerberosTest(portName, false, PORT);
        runKerberosTest(portName, false, STAX_PORT);
        runKerberosTest(portName, true, PORT);
        runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testKerberosSupporting() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItKerberosSupportingPort";
        runKerberosTest(portName, false, PORT);
        runKerberosTest(portName, false, STAX_PORT);
        runKerberosTest(portName, true, PORT);
        runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testKerberosOverAsymmetric() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItKerberosAsymmetricPort";
        runKerberosTest(portName, false, PORT);
        runKerberosTest(portName, false, STAX_PORT);
        runKerberosTest(portName, true, PORT);
        runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testKerberosOverTransportEndorsing() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItKerberosTransportEndorsingPort";
        runKerberosTest(portName, false, PORT2);
        runKerberosTest(portName, false, STAX_PORT2);
        runKerberosTest(portName, true, PORT2);
        runKerberosTest(portName, true, STAX_PORT2);
    }

    @org.junit.Test
    public void testKerberosOverAsymmetricEndorsing() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItKerberosAsymmetricEndorsingPort";
        runKerberosTest(portName, false, PORT);
        runKerberosTest(portName, false, STAX_PORT);
        // TODO Streaming support
        // runKerberosTest(portName, true, PORT);
        // runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testKerberosOverSymmetricProtection() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItKerberosSymmetricProtectionPort";
        runKerberosTest(portName, false, PORT);
        runKerberosTest(portName, false, STAX_PORT);
        runKerberosTest(portName, true, PORT);
        runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testKerberosOverSymmetricDerivedProtection() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItKerberosSymmetricDerivedProtectionPort";
        runKerberosTest(portName, false, PORT);
        // TODO Streaming support
        // TODO Kerberos derived regression on streaming inbound
        //runKerberosTest(portName, false, STAX_PORT);
        //runKerberosTest(portName, true, PORT);
        //runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testKerberosOverAsymmetricSignedEndorsing() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItKerberosAsymmetricSignedEndorsingPort";
        runKerberosTest(portName, false, PORT);
        runKerberosTest(portName, false, STAX_PORT);
        // TODO Streaming support
        // runKerberosTest(portName, true, PORT);
        // runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testKerberosOverAsymmetricSignedEncrypted() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItKerberosAsymmetricSignedEncryptedPort";
        runKerberosTest(portName, false, PORT);
        runKerberosTest(portName, false, STAX_PORT);
        runKerberosTest(portName, true, PORT);
        runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testKerberosOverSymmetricEndorsingEncrypted() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItKerberosSymmetricEndorsingEncryptedPort";
        runKerberosTest(portName, false, PORT);
        runKerberosTest(portName, false, STAX_PORT);
        // TODO Streaming support
        // runKerberosTest(portName, true, PORT);
        // runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testKerberosOverSymmetricSignedEndorsingEncrypted() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItKerberosSymmetricSignedEndorsingEncryptedPort";
        runKerberosTest(portName, false, PORT);
        runKerberosTest(portName, false, STAX_PORT);
        // TODO Streaming support
        // runKerberosTest(portName, true, PORT);
        // runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testKerberosOverSymmetricSecureConversation() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItKerberosSymmetricSecureConversationPort";
        runKerberosTest(portName, false, PORT);
        // TODO Streaming support
        // runKerberosTest(portName, false, STAX_PORT);
        // runKerberosTest(portName, true, PORT);
        // runKerberosTest(portName, true, STAX_PORT);
    }

    // In this test, a CXF client requests a SAML2 HOK Assertion from the STS, which has a
    // policy of requiring a KerberosToken over the TransportBinding. The CXF client
    // retrieves a service ticket from the KDC and inserts it into the security header of
    // the request. The STS validates the ticket using the KerberosTokenValidator.
    @org.junit.Test
    public void testWSTrustKerberosToken() throws Exception {

        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItTransportSAML2Port";
        runKerberosSTSTest(portName, false, PORT3, STSPORT);
        runKerberosSTSTest(portName, true, PORT3, STSPORT);
        runKerberosSTSTest(portName, false, PORT3, STAX_STSPORT);
        runKerberosSTSTest(portName, true, PORT3, STAX_STSPORT);
    }

    @org.junit.Test
    public void testKerberosViaCustomTokenAction() throws Exception {
        if (!runTests) {
            return;
        }

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItKerberosTransportActionPort");
        DoubleItPortType kerberosPort =
                service.getPort(portQName, DoubleItPortType.class);

        TestUtil.updateAddressPort(kerberosPort, PORT2);

        Assert.assertEquals(50, kerberosPort.doubleIt(25));

        ((java.io.Closeable)kerberosPort).close();
        bus.shutdown(true);
    }

    private void runKerberosTest(String portName, boolean streaming, String portNumber) throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, portName);
        DoubleItPortType kerberosPort =
                service.getPort(portQName, DoubleItPortType.class);

        TestUtil.updateAddressPort(kerberosPort, portNumber);

        if (streaming) {
            SecurityTestUtil.enableStreaming(kerberosPort);
        }

        Assert.assertEquals(50, kerberosPort.doubleIt(25));

        ((java.io.Closeable)kerberosPort).close();
        bus.shutdown(true);
    }

    private void runKerberosSTSTest(String portName, boolean streaming, String portNumber,
                                    String stsPortNumber) throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = KerberosTokenTest.class.getResource("sts-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = KerberosTokenTest.class.getResource("DoubleItKerberos.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, portName);
        DoubleItPortType kerberosPort =
                service.getPort(portQName, DoubleItPortType.class);

        TestUtil.updateAddressPort(kerberosPort, portNumber);

        SecurityTestUtil.updateSTSPort((BindingProvider)kerberosPort, stsPortNumber);

        if (streaming) {
            SecurityTestUtil.enableStreaming(kerberosPort);
        }

        Assert.assertEquals(50, kerberosPort.doubleIt(25));

        ((java.io.Closeable)kerberosPort).close();
        bus.shutdown(true);
    }

}
