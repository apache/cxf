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
package org.apache.cxf.systest.https.pqc;

import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.BCSSLParameters;
import org.bouncycastle.jsse.BCSSLSocket;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies BouncyCastle JSSE ({@code bctls-jdk18on} 1.84+) TLS support on JDK 17-26
 * using standard CXF XML configuration.
 *
 * <p>The server is configured via {@code bcjsse-server.xml} using
 * {@code jsseProvider="BCJSSE"} on {@code httpj:tlsServerParameters}.
 * HTTP/2 is supported via {@link BCJsseServerALPNProcessor}, which bridges
 * BC JSSE's {@code ProvSSLEngine} into Jetty's ALPN negotiation framework.
 * The client is configured via {@code bcjsse-client.xml}.
 *
 * <p>On JDK 27+, {@code PQCTLSTest} provides coverage via SunJSSE (JEP 527).
 */
public class BCJssePQCTest extends AbstractBusClientServerTestBase {

    static final String PORT = allocatePort(BCJsseJettyServer.class);

    // ------------------------------------------------------------------ setup

    @BeforeClass
    public static void setup() throws Exception {
        // JDK 27+ is covered by PQCTLSTest via SunJSSE (JEP 527).
        Assume.assumeTrue(
            "BCJssePQCTest targets JDK 17-26; use PQCTLSTest on JDK 27+",
            Runtime.version().feature() < 27);
        // BC JSSE builds its context-level named-group map when SSLContext is first
        // created; X25519MLKEM768 must be present there before the server starts.
        System.setProperty("jdk.tls.namedGroups", "X25519MLKEM768");
        // Append so Sun's JKS (which supports private-key entries) stays first.
        // BC's JKS is read-only and cannot load private-key entries.
        Security.addProvider(new BouncyCastleProvider());
        Security.addProvider(new BouncyCastleJsseProvider());
        assertTrue("Server failed to launch", launchServer(BCJsseJettyServer.class, true));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
        Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        System.clearProperty("jdk.tls.namedGroups");
    }

    // ------------------------------------------------------------------ server

    /**
     * CXF/Jetty server configured via {@code bcjsse-server.xml}: uses
     * {@code jsseProvider="BCJSSE"} on {@code httpj:tlsServerParameters} so that
     * CXF builds the SSLContext from BC JSSE rather than SunJSSE.  HTTP/2 is
     * enabled via {@link BCJsseServerALPNProcessor}.
     */
    public static class BCJsseJettyServer extends AbstractBusTestServerBase {
        @Override
        protected void run() {
            URL busFile = BCJsseJettyServer.class.getResource("bcjsse-server.xml");
            Bus busLocal = new SpringBusFactory().createBus(busFile);
            BusFactory.setDefaultBus(busLocal);
            setBus(busLocal);
        }
    }

    // ------------------------------------------------------------------ tests

    @Test
    public void testJettyMlKemHandshake() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = BCJssePQCTest.class.getResource("bcjsse-client.xml");
        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        QName serviceName =
            new QName("http://apache.org/hello_world/services", "SOAPService");
        URL wsdl = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter greeter = service.getHttpsPort();
        assertNotNull("Port is null", greeter);

        updateAddressPort(greeter, PORT);

        assertEquals("Hello Kitty", greeter.greetMe("Kitty"));

        assertX25519MlKem768Negotiated();

        ((java.io.Closeable) greeter).close();
        bus.shutdown(true);
    }

    // Indirect proof: client restricted to X25519MLKEM768 only; server must pick from offered groups.
    private void assertX25519MlKem768Negotiated() throws Exception {
        KeyStore ts = KeyStore.getInstance("JKS");
        try (InputStream is = BCJssePQCTest.class.getClassLoader()
                .getResourceAsStream("keys/Truststore.jks")) {
            ts.load(is, "password".toCharArray());
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        SSLContext ctx = SSLContext.getInstance("TLS", "BCJSSE");
        ctx.init(null, tmf.getTrustManagers(), null);

        try (SSLSocket sock = (SSLSocket) ctx.getSocketFactory()
                .createSocket("localhost", Integer.parseInt(PORT))) {
            sock.setSoTimeout(5000);
            BCSSLSocket bcSock = (BCSSLSocket) sock;
            BCSSLParameters p = bcSock.getParameters();
            p.setNamedGroups(new String[] {"X25519MLKEM768"});
            bcSock.setParameters(p);
            sock.startHandshake();
            assertEquals("TLSv1.3", sock.getSession().getProtocol());
        }
    }
}
