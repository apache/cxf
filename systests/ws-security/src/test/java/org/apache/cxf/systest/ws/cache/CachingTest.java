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

package org.apache.cxf.systest.ws.cache;

import java.net.URL;
import java.util.Random;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.common.TestParam;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.EHCacheTokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.tokenstore.jcache.JCacheTokenStore;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A set of tests for token caching on the client side
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class CachingTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    @FunctionalInterface
    private interface TokenStoreCacheFactory {
        TokenStore create(String key, Bus b) throws TokenStoreException;
    }

    private final TestParam test;
    private final TokenStoreCacheFactory factory;

    public CachingTest(TestParam type, TokenStoreCacheFactory factory) {
        this.test = type;
        this.factory = factory;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(Server.class, true)
        );
    }

    @Parameters(name = "{0},{1}")
    public static Object[][] data() {
        return new Object[][] {
            {new TestParam(PORT, false), (TokenStoreCacheFactory) (String key, Bus b) ->
                new EHCacheTokenStore(key, b, ClassLoaderUtils.getResource("cxf-ehcache.xml", CachingTest.class))},
            {new TestParam(PORT, false), (TokenStoreCacheFactory) (String key, Bus b) ->
                new JCacheTokenStore(key, b, ClassLoaderUtils.getResource("cxf-jcache.xml", CachingTest.class))},
            {new TestParam(PORT, true), (TokenStoreCacheFactory) (String key, Bus b) ->
                new EHCacheTokenStore(key, b, ClassLoaderUtils.getResource("cxf-ehcache.xml", CachingTest.class))},
            {new TestParam(PORT, true), (TokenStoreCacheFactory) (String key, Bus b) ->
                new JCacheTokenStore(key, b, ClassLoaderUtils.getResource("cxf-jcache.xml", CachingTest.class))}
        };
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    // By default, we have one cache per-proxy
    @org.junit.Test
    public void testSymmetric() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CachingTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CachingTest.class.getResource("DoubleItCache.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItCacheSymmetricPort");

        // First invocation
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        Client client = ClientProxy.getClient(port);
        TokenStore tokenStore =
            (TokenStore)client.getEndpoint().getEndpointInfo().getProperty(
                SecurityConstants.TOKEN_STORE_CACHE_INSTANCE
            );
        assertNotNull(tokenStore);
        // We expect two tokens as the identifier + SHA-1 are cached
        assertEquals(2, tokenStore.getTokenIdentifiers().size());

        // Second invocation
        DoubleItPortType port2 = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port2, test.getPort());

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port2);
        }

        port2.doubleIt(35);

        client = ClientProxy.getClient(port2);
        tokenStore =
            (TokenStore)client.getEndpoint().getEndpointInfo().getProperty(
                SecurityConstants.TOKEN_STORE_CACHE_INSTANCE
            );

        assertNotNull(tokenStore);
        // We expect two tokens as the identifier + SHA-1 are cached
        assertEquals(2, tokenStore.getTokenIdentifiers().size());

        ((java.io.Closeable)port).close();
        //port2 is still holding onto the cache, thus, this should still be 2
        assertEquals(2, tokenStore.getTokenIdentifiers().size());
        ((java.io.Closeable)port2).close();

        bus.shutdown(true);
    }

    // Here we manually create a cache and share it for both proxies
    @org.junit.Test
    public void testSymmetricSharedCache() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CachingTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CachingTest.class.getResource("DoubleItCache.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItCacheSymmetricPort");

        // First invocation
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Create shared cache
        String cacheKey = SecurityConstants.TOKEN_STORE_CACHE_INSTANCE + '-' + Math.abs(new Random().nextInt());
        TokenStore tokenStore = factory.create(cacheKey, bus);
        Client client = ClientProxy.getClient(port);
        client.getEndpoint().getEndpointInfo().setProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, tokenStore);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        // We expect two tokens as the identifier + SHA-1 are cached
        assertEquals(2, tokenStore.getTokenIdentifiers().size());

        // Second invocation
        DoubleItPortType port2 = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port2, test.getPort());

        client = ClientProxy.getClient(port2);
        client.getEndpoint().getEndpointInfo().setProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, tokenStore);

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port2);
        }

        port2.doubleIt(35);

        client = ClientProxy.getClient(port2);
        tokenStore =
                (TokenStore)client.getEndpoint().getEndpointInfo().getProperty(
                        SecurityConstants.TOKEN_STORE_CACHE_INSTANCE
                );

        assertNotNull(tokenStore);
        // We expect four tokens as the identifier + SHA-1 are cached
        assertEquals(4, tokenStore.getTokenIdentifiers().size());

        ((java.io.Closeable)port).close();
        ((java.io.Closeable)port2).close();

        bus.shutdown(true);
    }

    // Here we supply custom caching configuration
    @org.junit.Test
    public void testSymmetricCustom() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = CachingTest.class.getResource("client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = CachingTest.class.getResource("DoubleItCache.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItCachePerProxySymmetricPort");

        // First invocation
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.CACHE_IDENTIFIER, "proxy1"
        );
        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.CACHE_CONFIG_FILE,
                ClassLoaderUtils.getResource("per-proxy-cache.xml", this.getClass())
        );

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port);
        }

        assertEquals(50, port.doubleIt(25));

        Client client = ClientProxy.getClient(port);
        TokenStore tokenStore =
            (TokenStore)client.getEndpoint().getEndpointInfo().getProperty(
                SecurityConstants.TOKEN_STORE_CACHE_INSTANCE
            );
        assertNotNull(tokenStore);
        // We expect two tokens as the identifier + SHA-1 are cached
        assertEquals(2, tokenStore.getTokenIdentifiers().size());

        // Second invocation
        DoubleItPortType port2 = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port2, test.getPort());

        ((BindingProvider)port2).getRequestContext().put(
            SecurityConstants.CACHE_IDENTIFIER, "proxy2"
        );
        ((BindingProvider)port2).getRequestContext().put(
            SecurityConstants.CACHE_CONFIG_FILE,
                ClassLoaderUtils.getResource("per-proxy-cache.xml", this.getClass())
        );

        if (test.isStreaming()) {
            SecurityTestUtil.enableStreaming(port2);
        }

        assertEquals(70, port2.doubleIt(35));

        client = ClientProxy.getClient(port2);
        tokenStore =
            (TokenStore)client.getEndpoint().getEndpointInfo().getProperty(
                SecurityConstants.TOKEN_STORE_CACHE_INSTANCE
            );
        assertNotNull(tokenStore);
        // We expect two tokens as the identifier + SHA-1 are cached
        assertEquals(2, tokenStore.getTokenIdentifiers().size());

        ((java.io.Closeable)port).close();
        ((java.io.Closeable)port2).close();
        bus.shutdown(true);
    }

}
