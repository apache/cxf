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

package org.apache.cxf.transport.http;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * Conduits with equal settings share one HttpClient. Changing the settings of a conduit after its
 * first call must neither change the client of the other conduits, nor be ignored for the conduit itself.
 */
public class HttpClientHTTPConduitSharedClientTest {
    private static final URI TARGET = URI.create("http://backend.example/test");

    private Bus bus;
    private final List<HttpClientHTTPConduit> conduits = new ArrayList<>();

    @Before
    public void setUp() {
        bus = new ExtensionManagerBus();
    }

    @After
    public void tearDown() {
        conduits.forEach(HttpClientHTTPConduit::close);
        bus.shutdown(true);
    }

    @Test
    public void testConduitsWithEqualSettingsShareClient() throws Exception {
        HttpClient client1 = prepare(newConduit("http"));
        HttpClient client2 = prepare(newConduit("http"));
        assertSame(client1, client2);
    }

    @Test
    public void testProxyOfAnotherConduitIsNotUsed() throws Exception {
        HttpClientHTTPConduit conduit1 = newConduit("http");
        HttpClientHTTPConduit conduit2 = newConduit("http");
        assertSame(prepare(conduit1), prepare(conduit2));

        // only conduit 1 is configured with a proxy
        setProxy(conduit1.getClient(), "proxy1.example", 8080);
        assertEquals("proxy1.example", proxyHost(prepare(conduit1)));
        assertEquals(Proxy.Type.DIRECT, proxy(prepare(conduit2)).type());
    }

    @Test
    public void testOwnProxyIsUsed() throws Exception {
        HttpClientHTTPConduit conduit1 = newConduit("http");
        HttpClientHTTPConduit conduit2 = newConduit("http");
        assertSame(prepare(conduit1), prepare(conduit2));

        // only conduit 2 is configured with a proxy
        setProxy(conduit2.getClient(), "proxy2.example", 3128);
        assertEquals("proxy2.example", proxyHost(prepare(conduit2)));
        assertEquals(Proxy.Type.DIRECT, proxy(prepare(conduit1)).type());
    }

    @Test
    public void testChangedConnectionTimeout() throws Exception {
        HttpClientHTTPConduit conduit1 = newConduit("http");
        HttpClient client1 = prepare(conduit1);
        assertEquals(Optional.of(Duration.ofSeconds(30)), client1.connectTimeout());

        // conduit 1 changes its connection timeout after its first call
        conduit1.getClient().setConnectionTimeout(1000);
        assertEquals(Optional.of(Duration.ofSeconds(1)), prepare(conduit1).connectTimeout());

        // a conduit configured like conduit 1 at first still gets a client with the old timeout
        assertSame(client1, prepare(newConduit("http")));

        // and a conduit configured with the new timeout from the start gets the new one
        HttpClientHTTPConduit conduit3 = newConduit("http");
        conduit3.getClient().setConnectionTimeout(1000);
        assertEquals(Optional.of(Duration.ofSeconds(1)), prepare(conduit3).connectTimeout());
    }

    @Test
    public void testChangedTrustManagersAreNotShared() throws Exception {
        TrustManager trustAll = new TestTrustManager();
        TrustManager[] strict = {new TestTrustManager()};

        HttpClientHTTPConduit conduit1 = newConduit("https");
        TLSClientParameters tls1 = new TLSClientParameters();
        tls1.setTrustManagers(new TrustManager[] {trustAll});
        conduit1.setTlsClientParameters(tls1);
        HttpClient trustAllClient = prepare(conduit1);

        // conduit 1 is switched to the strict trust managers after its first call
        tls1.setTrustManagers(strict);
        HttpClient strictClient = prepare(conduit1);
        assertNotSame(trustAllClient, strictClient);

        // a conduit configured with the strict trust managers from the start
        // must not get the client built with the trust-all trust manager
        HttpClientHTTPConduit conduit2 = newConduit("https");
        TLSClientParameters tls2 = new TLSClientParameters();
        tls2.setTrustManagers(strict);
        conduit2.setTlsClientParameters(tls2);
        HttpClient client2 = prepare(conduit2);
        assertNotSame(trustAllClient, client2);
        assertSame(strictClient, client2);
    }

    @Test
    public void testHostnameVerifierIsNotShared() throws Exception {
        TrustManager[] trustManagers = {new TestTrustManager()};
        HttpClientHTTPConduit conduit1 = newConduit("https");
        TLSClientParameters tls1 = new TLSClientParameters();
        tls1.setTrustManagers(trustManagers);
        tls1.setHostnameVerifier((host, session) -> true);
        conduit1.setTlsClientParameters(tls1);

        // the hostname verifier is part of the client's trust managers
        HttpClientHTTPConduit conduit2 = newConduit("https");
        TLSClientParameters tls2 = new TLSClientParameters();
        tls2.setTrustManagers(trustManagers);
        conduit2.setTlsClientParameters(tls2);
        assertNotSame(prepare(conduit1), prepare(conduit2));
    }

    @Test
    public void testNamedGroupsAreNotShared() throws Exception {
        HttpClientHTTPConduit conduit1 = newConduit("https");
        TLSClientParameters tls1 = new TLSClientParameters();
        tls1.setNamedGroups(new ArrayList<>(List.of("x25519")));
        conduit1.setTlsClientParameters(tls1);

        HttpClientHTTPConduit conduit2 = newConduit("https");
        conduit2.setTlsClientParameters(new TLSClientParameters());
        assertNotSame(prepare(conduit1), prepare(conduit2));
    }

    @Test
    public void testChangedCipherSuitesFilter() throws Exception {
        HttpClientHTTPConduit conduit = newConduit("https");
        TLSClientParameters tls = new TLSClientParameters();
        FiltersType filters = new FiltersType();
        filters.getInclude().add(".*_AES_128_.*");
        tls.setCipherSuitesFilter(filters);
        conduit.setTlsClientParameters(tls);
        HttpClient client = prepare(conduit);

        // the filter of the conduit is changed in place after its first call
        filters.getInclude().add(".*_AES_256_.*");
        assertNotSame(client, prepare(conduit));
    }

    @Test
    public void testPerRequestSettingsKeepClient() throws Exception {
        HttpClientHTTPConduit conduit = newConduit("http");
        HttpClient client = prepare(conduit);

        // settings which are applied to each request, not to the client, alternate between calls
        for (int i = 0; i < 4; i++) {
            HTTPClientPolicy policy = new HTTPClientPolicy();
            policy.setReceiveTimeout(i % 2 == 0 ? 1000 : 2000);
            assertSame(client, prepare(conduit, policy, true));
            conduit.getClient().setReceiveTimeout(i % 2 == 0 ? 3000 : 4000);
            assertSame(client, prepare(conduit));
        }
    }

    @Test
    public void testAlternatingSettingsReuseClients() throws Exception {
        HttpClientHTTPConduit conduit = newConduit("http");
        HttpClient client1 = prepare(conduit, null, false);
        conduit.getClient().setConnectionTimeout(1000);
        HttpClient client2 = prepare(conduit, null, false);
        assertNotSame(client1, client2);

        // switching back and forth between the two settings does not build a client each time
        for (int i = 0; i < 4; i++) {
            conduit.getClient().setConnectionTimeout(30000);
            assertSame(client1, prepare(conduit, null, false));
            conduit.getClient().setConnectionTimeout(1000);
            assertSame(client2, prepare(conduit, null, false));
        }
    }

    private HttpClientHTTPConduit newConduit(String scheme) throws Exception {
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(scheme + "://backend.example/test");
        HttpClientHTTPConduit conduit = new HttpClientHTTPConduit(bus, ei, null);
        conduit.finalizeConfig();
        conduits.add(conduit);
        return conduit;
    }

    private static HttpClient prepare(HttpClientHTTPConduit conduit) throws Exception {
        return prepare(conduit, null, true);
    }

    private static HttpClient prepare(HttpClientHTTPConduit conduit, HTTPClientPolicy policy,
                                      boolean shareClient) throws Exception {
        Message message = new MessageImpl();
        message.put(Message.HTTP_REQUEST_METHOD, "GET");
        if (policy != null) {
            message.put(HTTPClientPolicy.class, policy);
        }
        if (!shareClient) {
            message.put("share.httpclient.http.conduit", Boolean.FALSE);
        }
        conduit.prepare(message);
        HttpClient client = message.get(HttpClient.class);
        assertNotNull(client);
        return client;
    }

    private static void setProxy(HTTPClientPolicy policy, String host, int port) {
        policy.setProxyServer(host);
        policy.setProxyServerPort(port);
    }

    private static Proxy proxy(HttpClient client) {
        List<Proxy> proxies = client.proxy().get().select(TARGET);
        assertEquals(1, proxies.size());
        return proxies.get(0);
    }

    private static String proxyHost(HttpClient client) {
        Proxy proxy = proxy(client);
        assertEquals(Proxy.Type.HTTP, proxy.type());
        return ((InetSocketAddress)proxy.address()).getHostString();
    }

    private static final class TestTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
