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

package org.apache.cxf.systest.kerberos.wssec.spnego;

import java.io.File;
import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.kerberos.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * A set of tests for Spnego Tokens that use an Apache Kerby instance as the KDC.
 */
public class SpnegoTokenTest extends AbstractBusClientServerTestBase {
    static final String PORT = TestUtil.getPortNumber(Server.class);
    static final String STAX_PORT = TestUtil.getPortNumber(StaxServer.class);
    static final String PORT2 = TestUtil.getPortNumber(Server.class, 2);
    static final String STAX_PORT2 = TestUtil.getPortNumber(StaxServer.class, 2);

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

        kerbyServer.createPrincipal(alice, "alice");
        kerbyServer.createPrincipal(bob, "bob");

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
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        AbstractClientServerTestBase.stopAllServers();
        if (kerbyServer != null) {
            kerbyServer.stop();
        }
    }

    @org.junit.Test
    public void testSpnegoOverSymmetric() throws Exception {
        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItSpnegoSymmetricPort";
        runKerberosTest(portName, false, PORT);
        runKerberosTest(portName, false, STAX_PORT);
        runKerberosTest(portName, true, PORT);
        runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testSpnegoOverSymmetricDerived() throws Exception {
        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItSpnegoSymmetricDerivedPort";
        runKerberosTest(portName, false, PORT);
        runKerberosTest(portName, false, STAX_PORT);
        runKerberosTest(portName, true, PORT);
        runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testSpnegoOverSymmetricEncryptBeforeSigning() throws Exception {
        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItSpnegoSymmetricEncryptBeforeSigningPort";
        runKerberosTest(portName, false, PORT);
        runKerberosTest(portName, false, STAX_PORT);
        runKerberosTest(portName, true, PORT);
        runKerberosTest(portName, true, STAX_PORT);
    }

    @org.junit.Test
    public void testSpnegoOverTransport() throws Exception {
        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItSpnegoTransportPort";
        runKerberosTest(portName, false, PORT2);
        runKerberosTest(portName, false, STAX_PORT2);
        // // TODO Supporting streaming Snego outbound
        // runKerberosTest(portName, true, PORT2);
        // runKerberosTest(portName, true, STAX_PORT2);
    }

    @org.junit.Test
    public void testSpnegoOverTransportEndorsing() throws Exception {
        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItSpnegoTransportEndorsingPort";
        runKerberosTest(portName, false, PORT2);
        runKerberosTest(portName, false, STAX_PORT2);
        // TODO Supporting streaming Spnego outbound
        // runKerberosTest(portName, true, PORT2);
        // runKerberosTest(portName, true, STAX_PORT2);
    }

    @org.junit.Test
    public void testSpnegoOverTransportEndorsingSP11() throws Exception {
        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItSpnegoTransportEndorsingSP11Port";
        runKerberosTest(portName, false, PORT2);
        runKerberosTest(portName, false, STAX_PORT2);
        // TODO Supporting streaming Spnego outbound
        // runKerberosTest(portName, true, PORT2);
        // runKerberosTest(portName, true, STAX_PORT2);
    }

    @org.junit.Test
    @org.junit.Ignore
    public void testSpnegoOverSymmetricSecureConversation() throws Exception {
        if (!runTests || !unrestrictedPoliciesInstalled) {
            return;
        }

        String portName = "DoubleItSpnegoSymmetricSecureConversationPort";
        runKerberosTest(portName, false, PORT);
        //runKerberosTest(portName, false, STAX_PORT);
        //runKerberosTest(portName, true, PORT);
        //runKerberosTest(portName, true, STAX_PORT);
    }

    private void runKerberosTest(String portName, boolean streaming, String portNumber) throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SpnegoTokenTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SpnegoTokenTest.class.getResource("DoubleItSpnego.wsdl");
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
}
