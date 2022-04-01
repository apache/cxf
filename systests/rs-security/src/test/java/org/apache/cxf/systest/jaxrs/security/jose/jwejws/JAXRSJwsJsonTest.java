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

package org.apache.cxf.systest.jaxrs.security.jose.jwejws;

import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.BadRequestException;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jaxrs.JweClientResponseFilter;
import org.apache.cxf.rs.security.jose.jaxrs.JweWriterInterceptor;
import org.apache.cxf.rs.security.jose.jaxrs.JwsJsonClientResponseFilter;
import org.apache.cxf.rs.security.jose.jaxrs.JwsJsonWriterInterceptor;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.systest.jaxrs.security.SecurityTestUtil;
import org.apache.cxf.systest.jaxrs.security.jose.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JAXRSJwsJsonTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerJwsJson.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerJwsJson.class, true));
        registerBouncyCastle();
    }

    private static void registerBouncyCastle() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
    }
    @AfterClass
    public static void unregisterBouncyCastleIfNeeded() throws Exception {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }

    @Test
    public void testJwsJsonPlainTextHmac() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjsonhmac";
        BookStore bs = createBookStore(address,
                                       "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties",
                                       null);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJwsJsonPlainTextHmacUnencoded() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjsonhmac";
        BookStore bs = createBookStore(address,
                                       Collections.singletonMap(JoseConstants.RSSEC_SIGNATURE_PROPS,
                                           "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties"),
                                       null,
                                       false);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJwsJsonBookBeanHmac() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjsonhmac";
        BookStore bs = createBookStore(address,
                                       "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties",
                                       Collections.singletonList(new JacksonJsonProvider()));
        Book book = bs.echoBook(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }
    @Test
    public void testJweCompactJwsJsonBookBeanHmac() throws Exception {
        if (!SecurityTestUtil.checkUnrestrictedPoliciesInstalled()) {
            return;
        }
        String address = "https://localhost:" + PORT + "/jwejwsjsonhmac";
        List<?> extraProviders = Arrays.asList(new JacksonJsonProvider(),
                                               new JweWriterInterceptor(),
                                               new JweClientResponseFilter());
        String jwkStoreProperty = "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties";
        Map<String, Object> props = new HashMap<>();
        props.put(JoseConstants.RSSEC_SIGNATURE_PROPS, jwkStoreProperty);
        props.put(JoseConstants.RSSEC_ENCRYPTION_PROPS, jwkStoreProperty);
        BookStore bs = createBookStore(address,
                                       props,
                                       extraProviders);
        Book book = bs.echoBook(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }

    @Test
    public void testJwsJsonBookDoubleHmacManyProps() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjsonhmac2";
        List<String> properties = new ArrayList<>();
        properties.add("org/apache/cxf/systest/jaxrs/security/secret.jwk.properties");
        properties.add("org/apache/cxf/systest/jaxrs/security/secret.jwk.hmac.properties");
        Map<String, Object> map = new HashMap<>();
        map.put(JoseConstants.RSSEC_SIGNATURE_OUT_PROPS, properties);
        map.put(JoseConstants.RSSEC_SIGNATURE_IN_PROPS,
                "org/apache/cxf/systest/jaxrs/security/secret.jwk.hmac.properties");
        BookStore bs = createBookStore(address, map, null);
        Book book = bs.echoBook(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }

    // Test signing an XML payload
    @Test
    public void testJwsJsonPlainTextHmacXML() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjsonhmac";
        BookStore bs = createBookStore(address,
                                       "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties",
                                       null);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }

    // Test signing with a bad signature key
    @Test
    public void testJwsJsonPlaintextHMACBadKey() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjsonhmac";
        BookStore bs = createBookStore(address,
                                       "org/apache/cxf/systest/jaxrs/security/secret.jwk.bad.properties",
                                       null);
        try {
            bs.echoText("book");
            fail("Failure expected on a bad signature key");
        } catch (BadRequestException ex) {
            // expected
        }
    }

    private BookStore createBookStore(String address, Object properties,
                                      List<?> extraProviders) throws Exception {
        return createBookStore(address,
                               Collections.singletonMap(JoseConstants.RSSEC_SIGNATURE_PROPS, properties),
                               extraProviders,
                               true);
    }
    private BookStore createBookStore(String address,
                                      Map<String, Object> mapProperties,
                                      List<?> extraProviders) throws Exception {
        return createBookStore(address,
                               mapProperties,
                               extraProviders,
                               true);
    }
    private BookStore createBookStore(String address,
                                      Map<String, Object> mapProperties,
                                      List<?> extraProviders,
                                      boolean encodePayload) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJwsJsonTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<>();
        JwsJsonWriterInterceptor writer = new JwsJsonWriterInterceptor();
        writer.setUseJwsJsonOutputStream(true);
        writer.setEncodePayload(encodePayload);
        providers.add(writer);
        providers.add(new JwsJsonClientResponseFilter());
        if (extraProviders != null) {
            providers.addAll(extraProviders);
        }
        bean.setProviders(providers);
        bean.getProperties(true).putAll(mapProperties);
        return bean.create(BookStore.class);
    }

}
