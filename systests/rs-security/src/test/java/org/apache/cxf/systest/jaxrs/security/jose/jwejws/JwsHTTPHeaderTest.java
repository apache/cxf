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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rs.security.jose.jaxrs.JwsWriterInterceptor;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Some signature tests for signing HTTP Headers
 */
public class JwsHTTPHeaderTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerHTTPHeaders.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerHTTPHeaders.class, true));
    }

    @org.junit.Test
    public void testSignHTTPHeaders() throws Exception {

        URL busFile = JwsHTTPHeaderTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        JwsWriterInterceptor jwsWriterInterceptor = new JwsWriterInterceptor();
        providers.add(jwsWriterInterceptor);

        String address = "http://localhost:" + PORT + "/jwsheaderdefault/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "RS256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        // Expect failure on not signing the default headers
        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);

        jwsWriterInterceptor.setProtectHttpHeaders(true);
        response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testSpecifyHeadersToSign() throws Exception {

        URL busFile = JwsHTTPHeaderTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        JwsWriterInterceptor jwsWriterInterceptor = new JwsWriterInterceptor();
        jwsWriterInterceptor.setProtectHttpHeaders(true);
        Set<String> headersToSign = new HashSet<>();
        headersToSign.add(HttpHeaders.CONTENT_TYPE);
        jwsWriterInterceptor.setProtectedHttpHeaders(headersToSign);
        providers.add(jwsWriterInterceptor);

        String address = "http://localhost:" + PORT + "/jwsheaderdefault/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "RS256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        // Expect failure on not signing all of the default headers
        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);

        headersToSign.add(HttpHeaders.ACCEPT);
        response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testSignAdditionalCustomHeader() throws Exception {

        URL busFile = JwsHTTPHeaderTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        JwsWriterInterceptor jwsWriterInterceptor = new JwsWriterInterceptor();
        jwsWriterInterceptor.setProtectHttpHeaders(true);
        Set<String> headersToSign = new HashSet<>();
        headersToSign.add(HttpHeaders.CONTENT_TYPE);
        headersToSign.add(HttpHeaders.ACCEPT);
        headersToSign.add("customheader");
        jwsWriterInterceptor.setProtectedHttpHeaders(headersToSign);
        providers.add(jwsWriterInterceptor);

        String address = "http://localhost:" + PORT + "/jwsheaderdefault/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "RS256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);
        WebClient.getConfig(client).getOutInterceptors().add(new CustomHeaderInterceptor(Phase.PRE_STREAM));

        Response response = client.post(new Book("book", 123L));
        response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testSignCustomHeaderRequired() throws Exception {

        URL busFile = JwsHTTPHeaderTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        JwsWriterInterceptor jwsWriterInterceptor = new JwsWriterInterceptor();
        jwsWriterInterceptor.setProtectHttpHeaders(true);
        providers.add(jwsWriterInterceptor);

        String address = "http://localhost:" + PORT + "/jwsheadercustom/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "RS256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);
        WebClient.getConfig(client).getOutInterceptors().add(new CustomHeaderInterceptor(Phase.PRE_STREAM));

        // Expect failure on not signing all of the required headers
        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);

        Set<String> headersToSign = new HashSet<>();
        headersToSign.add(HttpHeaders.CONTENT_TYPE);
        headersToSign.add(HttpHeaders.ACCEPT);
        headersToSign.add("customheader");
        jwsWriterInterceptor.setProtectedHttpHeaders(headersToSign);

        response = client.post(new Book("book", 123L));
        response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testSignEmptyCustomHeader() throws Exception {

        URL busFile = JwsHTTPHeaderTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        JwsWriterInterceptor jwsWriterInterceptor = new JwsWriterInterceptor();
        jwsWriterInterceptor.setProtectHttpHeaders(true);
        Set<String> headersToSign = new HashSet<>();
        headersToSign.add(HttpHeaders.CONTENT_TYPE);
        headersToSign.add(HttpHeaders.ACCEPT);
        headersToSign.add("customheader");
        jwsWriterInterceptor.setProtectedHttpHeaders(headersToSign);
        providers.add(jwsWriterInterceptor);

        String address = "http://localhost:" + PORT + "/jwsheadercustom/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "RS256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);
        CustomHeaderInterceptor customHeaderInterceptor = new CustomHeaderInterceptor(Phase.PRE_STREAM);
        customHeaderInterceptor.setEmpty(true);
        assertTrue(customHeaderInterceptor.isEmpty());
        WebClient.getConfig(client).getOutInterceptors().add(customHeaderInterceptor);

        Response response = client.post(new Book("book", 123L));
        response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }

    private static class CustomHeaderInterceptor extends AbstractPhaseInterceptor<Message> {

        private boolean empty;

        CustomHeaderInterceptor(String phase) {
            super(phase);
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            @SuppressWarnings("unchecked")
            Map<String, List<?>> headers = (Map<String, List<?>>) message.get(Message.PROTOCOL_HEADERS);
            headers.put("customheader", empty ? Arrays.asList("") : Arrays.asList("value1", "value2"));
        }

        public boolean isEmpty() {
            return empty;
        }

        public void setEmpty(boolean empty) {
            this.empty = empty;
        }

    }

}
