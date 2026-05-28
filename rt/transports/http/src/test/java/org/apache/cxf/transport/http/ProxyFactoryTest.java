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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProxyFactoryTest {

    private ProxySelector originalSelector;
    private ProxyFactory factory;

    @Before
    public void setUp() {
        originalSelector = ProxySelector.getDefault();
        factory = new ProxyFactory();
    }

    @After
    public void tearDown() {
        ProxySelector.setDefault(originalSelector);
    }

    @Test
    public void explicitPolicyProxyIsUsed() throws Exception {
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setProxyServer("proxy.example.com");
        policy.setProxyServerPort(8080);
        policy.setProxyServerType(ProxyServerType.HTTP);

        Proxy proxy = factory.createProxy(policy, new URI("http://target.example.com/service"));

        assertEquals(Proxy.Type.HTTP, proxy.type());
        assertEquals(new InetSocketAddress("proxy.example.com", 8080), proxy.address());
    }

    @Test
    public void nonProxyHostsExcludesMatchingHost() throws Exception {
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setProxyServer("proxy.example.com");
        policy.setProxyServerPort(8080);
        policy.setProxyServerType(ProxyServerType.HTTP);
        policy.setNonProxyHosts("internal.example.com");

        Proxy proxy = factory.createProxy(policy, new URI("http://internal.example.com/service"));

        assertEquals(Proxy.NO_PROXY, proxy);
    }

    @Test
    public void customProxySelectorIsConsultedWhenNoPolicyProxy() throws Exception {
        Proxy expected = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("custom-proxy.example.com", 3128));
        ProxySelector.setDefault(new FixedProxySelector(expected));

        Proxy proxy = factory.createProxy(new HTTPClientPolicy(), new URI("http://target.example.com/service"));

        assertEquals(expected, proxy);
    }

    @Test
    public void customProxySelectorIsConsultedWhenPolicyIsNull() throws Exception {
        Proxy expected = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080));
        ProxySelector.setDefault(new FixedProxySelector(expected));

        Proxy proxy = factory.createProxy(null, new URI("http://target.example.com/service"));

        assertEquals(expected, proxy);
    }

    @Test
    public void customProxySelectorCanBypassProxyForCertainHosts() throws Exception {
        Proxy routedProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080));
        ProxySelector.setDefault(new BypassingProxySelector("internal.example.com", routedProxy));

        assertNull(factory.createProxy(null, new URI("http://internal.example.com/service")));
        assertEquals(routedProxy, factory.createProxy(null, new URI("http://external.example.com/service")));
    }

    @Test
    public void noProxyReturnedWhenSelectorReturnsNoProxy() throws Exception {
        ProxySelector.setDefault(new FixedProxySelector(Proxy.NO_PROXY));

        assertNull(factory.createProxy(null, new URI("http://target.example.com/service")));
    }

    private static final class FixedProxySelector extends ProxySelector {
        private final Proxy proxy;

        FixedProxySelector(Proxy proxy) {
            this.proxy = proxy;
        }

        @Override
        public List<Proxy> select(URI uri) {
            return Collections.singletonList(proxy);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        }
    }

    private static final class BypassingProxySelector extends ProxySelector {
        private final String bypassHost;
        private final Proxy proxy;

        BypassingProxySelector(String bypassHost, Proxy proxy) {
            this.bypassHost = bypassHost;
            this.proxy = proxy;
        }

        @Override
        public List<Proxy> select(URI uri) {
            if (bypassHost.equals(uri.getHost())) {
                return Collections.singletonList(Proxy.NO_PROXY);
            }
            return Collections.singletonList(proxy);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        }
    }
}
