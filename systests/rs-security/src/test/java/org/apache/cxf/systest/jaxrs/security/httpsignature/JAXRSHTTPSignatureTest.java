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
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.MessageDigestInputStream;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.httpsignature.MessageSigner;
import org.apache.cxf.rs.security.httpsignature.MessageVerifier;
import org.apache.cxf.rs.security.httpsignature.filters.CreateSignatureInterceptor;
import org.apache.cxf.rs.security.httpsignature.filters.VerifySignatureClientFilter;
import org.apache.cxf.rt.security.rs.PrivateKeyPasswordProvider;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A test for the HTTP Signature functionality in the cxf-rt-rs-security-http-signature module.
 */
public class JAXRSHTTPSignatureTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerHttpSignature.PORT;

    @BeforeClass
    public static void startServers() {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerHttpSignature.class, true));
    }

    @Test
    public void testHttpSignature() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureGET() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.path("/126").get();
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(123L, returnedBook.getId());

        String address2 = "http://localhost:" + PORT + "/httpsigrequired/bookstore/books";
        client =
            WebClient.create(address2, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        response = client.path("/126").get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testHttpSignatureServiceProperties() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsigprops/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureProperties() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.properties",
                       "org/apache/cxf/systest/jaxrs/security/httpsignature/alice.httpsig.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureOutProperties() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.out.properties",
                       "org/apache/cxf/systest/jaxrs/security/httpsignature/alice.httpsig.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignaturePropertiesPasswordProvider() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.alias", "alice");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.keystore.file", "keys/alice.jks");
        PrivateKeyPasswordProvider passwordProvider = storeProperties -> "password".toCharArray();
        properties.put("rs.security.key.password.provider", passwordProvider);
        properties.put("rs.security.http.signature.key.id", "alice-key-id");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureRsaSha512() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner("rsa-sha512", keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsigrsasha512/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureRsaSha512ServiceProperties() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner("rsa-sha512", keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsigrsasha512props/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureSignaturePropertiesRsaSha512() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();

        String address = "http://localhost:" + PORT + "/httpsigrsasha512/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.alias", "alice");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.keystore.file", "keys/alice.jks");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.signature.algorithm", "rsa-sha512");
        properties.put("rs.security.http.signature.key.id", "alice-key-id");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureResponse() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        VerifySignatureClientFilter signatureResponseFilter = new VerifySignatureClientFilter();
        MessageVerifier messageVerifier = new MessageVerifier(new CustomPublicKeyProvider());
        signatureResponseFilter.setMessageVerifier(messageVerifier);

        List<Object> providers = new ArrayList<>();
        providers.add(signatureFilter);
        providers.add(signatureResponseFilter);
        String address = "http://localhost:" + PORT + "/httpsigresponse/bookstore/books";
        WebClient client = WebClient.create(address, providers, busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureResponseServiceProperties() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        VerifySignatureClientFilter signatureResponseFilter = new VerifySignatureClientFilter();
        MessageVerifier messageVerifier = new MessageVerifier(new CustomPublicKeyProvider());
        signatureResponseFilter.setMessageVerifier(messageVerifier);

        List<Object> providers = new ArrayList<>();
        providers.add(signatureFilter);
        providers.add(signatureResponseFilter);
        String address = "http://localhost:" + PORT + "/httpsigresponseprops/bookstore/books";
        WebClient client = WebClient.create(address, providers, busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureResponseProperties() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new CreateSignatureInterceptor());
        providers.add(new VerifySignatureClientFilter());
        String address = "http://localhost:" + PORT + "/httpsigresponse/bookstore/books";
        WebClient client = WebClient.create(address, providers, busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.out.properties",
            "org/apache/cxf/systest/jaxrs/security/httpsignature/alice.httpsig.properties");
        properties.put("rs.security.signature.in.properties",
                       "org/apache/cxf/systest/jaxrs/security/httpsignature/bob.httpsig.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureNoRequestTarget() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        List<String> headerList = Arrays.asList("accept");
        MessageSigner messageSigner =
            new MessageSigner("rsa-sha512", keyId -> privateKey, "alice-key-id", headerList);
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsigrsasha512/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureSignSpecificHeader() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        List<String> headerList = Arrays.asList("accept", "(request-target)");
        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id", headerList);
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureSignSpecificHeaderProperties() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.properties",
                       "org/apache/cxf/systest/jaxrs/security/httpsignature/alice.httpsig.properties");
        List<String> headerList = Arrays.asList("accept", "(request-target)");
        properties.put("rs.security.http.signature.out.headers", headerList);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHeaderTrailingWhitespace() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        List<String> headerList = Arrays.asList("custom", "(request-target)");
        MessageSigner messageSigner = new MessageSigner(keyid -> privateKey, "alice-key-id", headerList);
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        client.header("custom", " someval    ");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testMultipleHeaderConcatenation() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        List<String> headerList = Arrays.asList("custom", "(request-target)");
        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id", headerList);
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        client.header("custom", "someval, someval2");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureDigestResponse() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        VerifySignatureClientFilter signatureResponseFilter = new VerifySignatureClientFilter();
        MessageVerifier messageVerifier = new MessageVerifier(new CustomPublicKeyProvider());
        signatureResponseFilter.setMessageVerifier(messageVerifier);

        String address = "http://localhost:" + PORT + "/httpsigrequired/bookstore/books";
        WebClient client =
            WebClient.create(address, Arrays.asList(signatureFilter, signatureResponseFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());

        String address2 = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        client =
            WebClient.create(address2, Arrays.asList(signatureFilter, signatureResponseFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");
        try {
            client.post(new Book("CXF", 126L));
            fail("Failure expected on no digest");
        } catch (Exception ex) {
            // expected
        }
    }

    @Test
    public void testHttpSignatureDigestSHA512() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.http.signature.digest.algorithm", "SHA-512");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(126L, returnedBook.getId());
    }

    @Test
    public void testHttpSignatureEmptyResponse() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        VerifySignatureClientFilter signatureResponseFilter = new VerifySignatureClientFilter();
        MessageVerifier messageVerifier = new MessageVerifier(new CustomPublicKeyProvider());
        signatureResponseFilter.setMessageVerifier(messageVerifier);

        String address = "http://localhost:" + PORT + "/httpsigresponse/bookstore/booksnoresp";
        WebClient client =
            WebClient.create(address, Arrays.asList(signatureFilter, signatureResponseFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(204, response.getStatus());
    }

    @Test
    public void testHttpSignatureEmptyResponseProps() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        VerifySignatureClientFilter signatureResponseFilter = new VerifySignatureClientFilter();

        String address = "http://localhost:" + PORT + "/httpsigresponse/bookstore/booksnoresp";
        WebClient client =
            WebClient.create(address, Arrays.asList(signatureFilter, signatureResponseFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.in.properties",
            "org/apache/cxf/systest/jaxrs/security/httpsignature/bob.httpsig.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(204, response.getStatus());
    }

    //
    // Negative tests
    //
    @Test
    public void testNonMatchingSignatureAlgorithm() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner("rsa-sha512", keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testNoHttpSignature() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testNoHttpSignatureGET() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.path("/126").get();
        assertEquals(400, response.getStatus());
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

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
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

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
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

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testEmptySignatureValue() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        ClientTestFilter signatureFilter = new ClientTestFilter();
        signatureFilter.setEmptySignatureValue(true);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testChangedSignatureValue() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        ClientTestFilter signatureFilter = new ClientTestFilter();
        signatureFilter.setChangeSignatureValue(true);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testDifferentSigningKey() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        ClientTestFilter signatureFilter = new ClientTestFilter();

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        MessageSigner messageSigner = new MessageSigner(keyId -> keyPair.getPrivate(), "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testHttpSignatureMissingRequiredHeader() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        List<String> headerList = Arrays.asList("accept");
        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id", headerList);
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testUnknownKeyId() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "unknown-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testPropertiesWrongSignatureVerification() {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new CreateSignatureInterceptor());
        providers.add(new VerifySignatureClientFilter());
        String address = "http://localhost:" + PORT + "/httpsigresponse/bookstore/books";
        WebClient client = WebClient.create(address, providers, busFile.toString());
        client.type("application/xml").accept("application/xml");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.out.properties",
            "org/apache/cxf/systest/jaxrs/security/httpsignature/alice.httpsig.properties");
        properties.put("rs.security.signature.in.properties",
                       "org/apache/cxf/systest/jaxrs/security/httpsignature/alice.httpsig.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        try {
            client.post(new Book("CXF", 126L));
            fail("Failure expected on the wrong signature verification keystore");
        } catch (Exception ex) {
            // expected
        }
    }

    @Test
    public void testIncorrectDigest() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateDigestTestInterceptor signatureFilter = new CreateDigestTestInterceptor();
        signatureFilter.setChangeDigestValue(true);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testEmptyDigest() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateDigestTestInterceptor signatureFilter = new CreateDigestTestInterceptor();
        signatureFilter.setEmptyDigestValue(true);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testIncorrectDigestAlgorithm() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateDigestTestInterceptor signatureFilter = new CreateDigestTestInterceptor("SHA-1");

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id");
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsig/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testMissingDigestHeader() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        signatureFilter.setAddDigest(false);
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        List<String> headerList = Arrays.asList("accept", "(request-target)");
        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id", headerList);
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsigprops/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testMissingSignedDigestHeader() throws Exception {

        URL busFile = JAXRSHTTPSignatureTest.class.getResource("client.xml");

        CreateSignatureInterceptor signatureFilter = new CreateSignatureInterceptor();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(ClassLoaderUtils.getResourceAsStream("keys/alice.jks", this.getClass()),
                      "password".toCharArray());
        PrivateKey privateKey = (PrivateKey)keyStore.getKey("alice", "password".toCharArray());
        assertNotNull(privateKey);

        List<String> headerList = Arrays.asList("accept", "(request-target)");
        MessageSigner messageSigner = new MessageSigner(keyId -> privateKey, "alice-key-id", headerList);
        signatureFilter.setMessageSigner(messageSigner);

        String address = "http://localhost:" + PORT + "/httpsigprops/bookstore/books";
        WebClient client =
            WebClient.create(address, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        Response response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());

        String address2 = "http://localhost:" + PORT + "/httpsigrequired/bookstore/books";
        client =
            WebClient.create(address2, Collections.singletonList(signatureFilter), busFile.toString());
        client.type("application/xml").accept("application/xml");

        response = client.post(new Book("CXF", 126L));
        assertEquals(400, response.getStatus());
    }

    @Provider
    @Priority(Priorities.AUTHENTICATION)
    private final class ClientTestFilter implements ClientRequestFilter {

        private MessageSigner messageSigner;
        private String httpMethod;
        private String uri;
        private boolean changeSignatureAlgorithm;
        private boolean emptySignatureValue;
        private boolean changeSignatureValue;

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
            } else if (emptySignatureValue) {
                String signatureValue = convertedHeaders.get("Signature").get(0);
                signatureValue =
                    signatureValue.substring(0, signatureValue.indexOf("signature=") + "signature=\"".length()) + "\"";
                requestHeaders.put("Signature", Collections.singletonList(signatureValue));
            } else if (changeSignatureValue) {
                String signatureValue = convertedHeaders.get("Signature").get(0);
                String signature =
                    signatureValue.substring(signatureValue.indexOf("signature=") + "signature=\"".length(),
                                                            signatureValue.length() - 1);
                byte[] decodedSignature = Base64.getDecoder().decode(signature);
                decodedSignature[0]++;
                signatureValue =
                    signatureValue.substring(0, signatureValue.indexOf("signature=") + "signature=\"".length())
                    + Base64.getEncoder().encodeToString(decodedSignature)
                    + "\"";
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
                                     entry.getValue().stream().map(Object::toString).collect(Collectors.toList()));
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

        public void setChangeSignatureValue(boolean changeSignatureValue) {
            this.changeSignatureValue = changeSignatureValue;
        }

        public void setEmptySignatureValue(boolean emptySignatureValue) {
            this.emptySignatureValue = emptySignatureValue;
        }

    }

    @Provider
    @Priority(Priorities.HEADER_DECORATOR)
    private final class CreateDigestTestInterceptor extends CreateSignatureInterceptor {
        private static final String DIGEST_HEADER_NAME = "Digest";
        private final String digestAlgorithmName;
        private boolean changeDigestValue;
        private boolean emptyDigestValue;

        private CreateDigestTestInterceptor() {
            this(MessageDigestInputStream.ALGO_SHA_256);
        }

        private CreateDigestTestInterceptor(String digestAlgorithmName) {
            this.digestAlgorithmName = digestAlgorithmName;
        }

        private boolean shouldAddDigest(WriterInterceptorContext context) {
            return null != context.getEntity()
                && context.getHeaders().keySet().stream().noneMatch(DIGEST_HEADER_NAME::equalsIgnoreCase);
        }

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException {
            // skip digest if already set or we actually don't have a body
            if (shouldAddDigest(context)) {
                addDigest(context);
            } else {
                sign(context);
                context.proceed();
            }
        }

        private void addDigest(WriterInterceptorContext context) throws IOException {
            // make sure we have all content
            OutputStream originalOutputStream = context.getOutputStream();
            CachedOutputStream cachedOutputStream = new CachedOutputStream();
            context.setOutputStream(cachedOutputStream);

            context.proceed();
            cachedOutputStream.flush();

            // then digest using requested encoding
            String encoding = context.getMediaType().getParameters()
                .getOrDefault(MediaType.CHARSET_PARAMETER, StandardCharsets.UTF_8.toString());
            // not so nice - would be better to have a stream

            String digest = digestAlgorithmName + "=";
            try {
                MessageDigest messageDigest = MessageDigest.getInstance(digestAlgorithmName);
                messageDigest.update(new String(cachedOutputStream.getBytes(), encoding).getBytes());
                if (!emptyDigestValue) {
                    if (changeDigestValue) {
                        byte[] bytes = messageDigest.digest();
                        bytes[0]++;
                        digest += Base64.getEncoder().encodeToString(bytes);
                    } else {
                        digest += Base64.getEncoder().encodeToString(messageDigest.digest());
                    }
                }
            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // add header
            context.getHeaders().add(DIGEST_HEADER_NAME, digest);
            sign(context);

            // write the contents
            context.setOutputStream(originalOutputStream);
            IOUtils.copy(cachedOutputStream.getInputStream(), originalOutputStream);
        }

        public void setChangeDigestValue(boolean changeDigestValue) {
            this.changeDigestValue = changeDigestValue;
        }

        public void setEmptyDigestValue(boolean emptyDigestValue) {
            this.emptyDigestValue = emptyDigestValue;
        }

    }

}
