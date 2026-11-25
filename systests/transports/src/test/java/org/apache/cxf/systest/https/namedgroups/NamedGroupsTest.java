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

package org.apache.cxf.systest.https.namedgroups;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.https.SSLUtils;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The TLS named groups configured for a server endpoint (Spring XML) and for a client conduit are used in
 * the handshake. The test JVM offers both secp256r1 and secp384r1 (jdk.tls.namedGroups), so a handshake
 * only fails when the per-endpoint setting is applied.
 */
public class NamedGroupsTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(NamedGroupsServer.class);

    public static class NamedGroupsServer extends AbstractBusTestServerBase {
        @Override
        protected void run() {
            URL busFile = NamedGroupsServer.class.getResource("named-groups-server.xml");
            Bus busLocal = new SpringBusFactory().createBus(busFile);
            BusFactory.setDefaultBus(busLocal);
            setBus(busLocal);
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        Assume.assumeTrue("SSLParameters.setNamedGroups() requires JDK 20+", hasNamedGroupsApi());
        assertTrue("Server failed to launch", launchServer(NamedGroupsServer.class, true));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @Test
    public void testServerAcceptsOnlyConfiguredGroups() throws Exception {
        assertTrue(handshake(Integer.parseInt(PORT), "secp384r1"));
        assertFalse(handshake(Integer.parseInt(PORT), "secp256r1"));
    }

    @Test
    public void testServerEndpointCall() throws Exception {
        assertEquals("Hello Kitty", greetMe(PORT, null));
        assertEquals("Hello Kitty", greetMe(PORT, Arrays.asList("secp384r1")));
    }

    @Test
    public void testClientOffersOnlyConfiguredGroups() throws Exception {
        Assume.assumeTrue("the HttpClient of this JDK ignores named groups (JDK 26+ required)",
            isForceURLConnection() || httpClientKeepsNamedGroups());
        try (SingleGroupServer server = new SingleGroupServer("secp256r1")) {
            callIgnoringResponse(server.getPort(), Arrays.asList("secp384r1"));
            assertFalse("a client restricted to secp384r1 must not complete a handshake with a secp256r1-only server",
                server.handshakeResult());
        }
        try (SingleGroupServer server = new SingleGroupServer("secp256r1")) {
            callIgnoringResponse(server.getPort(), Arrays.asList("secp256r1", "secp384r1"));
            assertTrue(server.handshakeResult());
        }
    }

    private String greetMe(String port, List<String> groups) throws Exception {
        Bus bus = BusFactory.newInstance().createBus();
        BusFactory.setThreadDefaultBus(bus);
        try {
            Greeter greeter = greeter(port, groups);
            String result = greeter.greetMe("Kitty");
            ((java.io.Closeable) greeter).close();
            return result;
        } finally {
            bus.shutdown(true);
        }
    }

    private void callIgnoringResponse(int port, List<String> groups) throws Exception {
        Bus bus = BusFactory.newInstance().createBus();
        BusFactory.setThreadDefaultBus(bus);
        try {
            Greeter greeter = greeter(String.valueOf(port), groups);
            try {
                greeter.greetMe("Kitty");
                fail("the test server does not speak HTTP");
            } catch (Exception expected) {
                // the server only does the handshake
            }
            ((java.io.Closeable) greeter).close();
        } finally {
            bus.shutdown(true);
        }
    }

    private Greeter greeter(String port, List<String> groups) throws Exception {
        SOAPService service = new SOAPService(SOAPService.WSDL_LOCATION,
            new QName("http://apache.org/hello_world/services", "SOAPService"));
        Greeter greeter = service.getHttpsPort();
        updateAddressPort(greeter, port);
        HTTPConduit conduit = (HTTPConduit) ClientProxy.getClient(greeter).getConduit();
        TLSClientParameters tls = new TLSClientParameters();
        tls.setDisableCNCheck(true);
        tls.setTrustManagers(trustManagerFactory().getTrustManagers());
        if (groups != null) {
            tls.setNamedGroups(groups);
        }
        conduit.setTlsClientParameters(tls);
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(10000);
        policy.setReceiveTimeout(10000);
        conduit.setClient(policy);
        return greeter;
    }

    /** A raw TLS handshake offering only the given group. */
    private static boolean handshake(int port, String group) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, trustManagerFactory().getTrustManagers(), null);
        try (SSLSocket socket = (SSLSocket) ctx.getSocketFactory().createSocket("localhost", port)) {
            socket.setSoTimeout(10000);
            SSLUtils.applyNamedGroups(socket, List.of(group));
            socket.startHandshake();
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private static TrustManagerFactory trustManagerFactory() throws Exception {
        KeyStore ts = KeyStore.getInstance("JKS");
        try (InputStream is = NamedGroupsTest.class.getClassLoader().getResourceAsStream("keys/Truststore.jks")) {
            ts.load(is, "password".toCharArray());
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
        return tmf;
    }

    private static boolean hasNamedGroupsApi() {
        try {
            SSLParameters.class.getMethod("setNamedGroups", String[].class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean isForceURLConnection() {
        return Boolean.getBoolean("org.apache.cxf.transport.http.forceURLConnection");
    }

    private static boolean httpClientKeepsNamedGroups() throws Exception {
        SSLParameters params = new SSLParameters();
        SSLUtils.applyNamedGroups(params, List.of("secp384r1"));
        HttpClient client = HttpClient.newBuilder().sslContext(SSLContext.getDefault()).sslParameters(params).build();
        return SSLUtils.getNamedGroups(client.sslParameters()) != null;
    }

    /** A TLS server that accepts a single named group and reports whether the first handshake completed. */
    private static final class SingleGroupServer implements AutoCloseable {
        private final SSLServerSocket serverSocket;
        private final CompletableFuture<Boolean> result = new CompletableFuture<>();

        SingleGroupServer(String group) throws Exception {
            KeyStore ks = KeyStore.getInstance("JKS");
            try (InputStream is = NamedGroupsTest.class.getClassLoader().getResourceAsStream("keys/Bethal.jks")) {
                ks.load(is, "password".toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, "password".toCharArray());
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), null, null);
            serverSocket = (SSLServerSocket) ctx.getServerSocketFactory()
                .createServerSocket(0, 1, InetAddress.getLoopbackAddress());
            Thread t = new Thread(() -> {
                try (SSLSocket s = (SSLSocket) serverSocket.accept()) {
                    s.setSoTimeout(10000);
                    SSLUtils.applyNamedGroups(s, List.of(group));
                    s.startHandshake();
                    result.complete(true);
                } catch (Exception e) {
                    result.complete(false);
                }
            }, "single-group-tls-server");
            t.setDaemon(true);
            t.start();
        }

        int getPort() {
            return serverSocket.getLocalPort();
        }

        boolean handshakeResult() throws Exception {
            return result.get(30, TimeUnit.SECONDS);
        }

        @Override
        public void close() throws Exception {
            serverSocket.close();
        }
    }
}
