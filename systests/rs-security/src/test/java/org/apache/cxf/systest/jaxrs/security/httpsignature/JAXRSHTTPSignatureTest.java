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

package org.apache.cxf.systest.jaxrs.security.httpsignature;

import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.httpsignature.MessageSigner;
import org.apache.cxf.rs.security.httpsignature.filters.CreateSignatureClientFilter;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A test for the HTTP Signature functionality in the cxf-rt-rs-security-http-signature module.
 */
public class JAXRSHTTPSignatureTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerHttpSignature.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerHttpSignature.class, true));
    }

    @Test
    public void testHttpSignature() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(privateKey, "custom_key_id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "https://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureRsaSha512() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner("rsa-sha512", "SHA-256", privateKey, "custom_key_id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "https://localhost:" + PORT + "/httpsigrsasha512/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    //
    // Negative tests
    //

    @Test
    public void testNonMatchingSignatureAlgorithm() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureClientFilter signatureFilter = new CreateSignatureClientFilter();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner("rsa-sha512", "SHA-256", privateKey, "custom_key_id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "https://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testNoHttpSignature() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testWrongHTTPMethod() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        ClientTestFilter signatureFilter = new ClientTestFilter();
        signatureFilter.setHttpMethod("GET");

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(privateKey, "custom_key_id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "https://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testWrongURI() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        ClientTestFilter signatureFilter = new ClientTestFilter();
        signatureFilter.setUri("/httpsig/bookstore/books2");

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(privateKey, "custom_key_id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "https://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testChangedSignatureMethod() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        ClientTestFilter signatureFilter = new ClientTestFilter();
        signatureFilter.setChangeSignatureAlgorithm(true);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(privateKey, "custom_key_id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "https://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(response.getStatus(), 400);
    }

    @Provider
    @Priority(Priorities.AUTHENTICATION)
    private final class ClientTestFilter implements ClientRequestFilter {

        private MessageSigner messageSigner;
        private String httpMethod;
        private String uri;
        private boolean changeSignatureAlgorithm;

        @Override
        public void filter(ClientRequestContext requestCtx) {

            MultivaluedMap<String, Object> requestHeaders = requestCtx.getHeaders();

            Map<String, List<String>> convertedHeaders = convertHeaders(requestHeaders);
            try {
                messageSigner.sign(convertedHeaders,
                                   uri != null ? uri : requestCtx.getUri().getPath(),
                                   httpMethod != null ? httpMethod : requestCtx.getMethod());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (changeSignatureAlgorithm) {
                String signatureValue = convertedHeaders.get("Signature").get(0);
                signatureValue = signatureValue.replace("rsa-sha256", "rsa-sha512");
                requestHeaders.put("Signature", Collections.singletonList(signatureValue));
            } else {
                requestHeaders.put("Signature", Collections.singletonList(convertedHeaders.get("Signature").get(0)));
            }
        }

        // Convert the headers from List<Object> -> List<String>
        private Map<String, List<String>> convertHeaders(MultivaluedMap<String, Object> requestHeaders) {
            Map<String, List<String>> convertedHeaders = new HashMap<>(requestHeaders.size());
            for (Map.Entry<String, List<Object>> entry : requestHeaders.entrySet()) {
                convertedHeaders.put(entry.getKey(),
                                     entry.getValue().stream().map(o -> o.toString()).collect(Collectors.toList()));
            }
            return convertedHeaders;
        }

        public void setMessageSigner(MessageSigner messageSigner) {
            Objects.requireNonNull(messageSigner);
            this.messageSigner = messageSigner;
        }


        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public void setChangeSignatureAlgorithm(boolean changeSignatureAlgorithm) {
            this.changeSignatureAlgorithm = changeSignatureAlgorithm;
        }

    }
}
