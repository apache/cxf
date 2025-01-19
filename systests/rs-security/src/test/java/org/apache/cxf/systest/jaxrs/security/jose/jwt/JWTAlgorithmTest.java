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

package org.apache.cxf.systest.jaxrs.security.jose.jwt;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jaxrs.JwtAuthenticationClientFilter;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.systest.jaxrs.security.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
/**
 * Some tests for JWT tokens.
 */
public class JWTAlgorithmTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerJwtAlgorithms.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerJwtAlgorithms.class, true));
    }

   
    //
    // Encryption tests
    //
    @org.junit.Test
    public void testEncryptionProperties() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        JwtAuthenticationClientFilter clientFilter = new JwtAuthenticationClientFilter();
        clientFilter.setJwsRequired(false);
        clientFilter.setJweRequired(true);
        providers.add(clientFilter);

        String address = "https://localhost:" + PORT + "/encryptedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.encryption.properties",
            "org/apache/cxf/systest/jaxrs/security/bob.jwk.properties");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    @org.junit.Test
    public void testEncryptionDynamic() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        JwtAuthenticationClientFilter clientFilter = new JwtAuthenticationClientFilter();
        clientFilter.setJwsRequired(false);
        clientFilter.setJweRequired(true);
        providers.add(clientFilter);

        String address = "https://localhost:" + PORT + "/encryptedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file", "org/apache/cxf/systest/jaxrs/security/certs/jwkPublicSet.txt");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA-OAEP");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    @org.junit.Test
    public void testWrongKeyEncryptionAlgorithm() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        JwtAuthenticationClientFilter clientFilter = new JwtAuthenticationClientFilter();
        clientFilter.setJwsRequired(false);
        clientFilter.setJweRequired(true);
        providers.add(clientFilter);

        String address = "https://localhost:" + PORT + "/encryptedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file", "org/apache/cxf/systest/jaxrs/security/certs/jwkPublicSet.txt");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA1_5");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testWrongContentEncryptionAlgorithm() throws Exception {
        if (!SecurityTestUtil.checkUnrestrictedPoliciesInstalled()) {
            return;
        }

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        JwtAuthenticationClientFilter clientFilter = new JwtAuthenticationClientFilter();
        clientFilter.setJwsRequired(false);
        clientFilter.setJweRequired(true);
        providers.add(clientFilter);

        String address = "https://localhost:" + PORT + "/encryptedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file", "org/apache/cxf/systest/jaxrs/security/certs/jwkPublicSet.txt");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.content.algorithm", "A192GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA-OAEP");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testBadEncryptingKey() throws Exception {
        if (!SecurityTestUtil.checkUnrestrictedPoliciesInstalled()) {
            return;
        }

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        JwtAuthenticationClientFilter clientFilter = new JwtAuthenticationClientFilter();
        clientFilter.setJwsRequired(false);
        clientFilter.setJweRequired(true);
        providers.add(clientFilter);

        String address = "https://localhost:" + PORT + "/encryptedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "AliceCert");
        properties.put("rs.security.keystore.file", "org/apache/cxf/systest/jaxrs/security/certs/jwkPublicSet.txt");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA-OAEP");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    //
    // Signature tests
    //

    @org.junit.Test
    public void testSignatureProperties() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.properties",
                       "org/apache/cxf/systest/jaxrs/security/alice.jwk.properties");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    @org.junit.Test
    public void testSignatureDynamic() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    @org.junit.Test
    public void testWrongSignatureAlgorithm() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "PS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testBadSigningKey() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.alias", "alice");
        properties.put("rs.security.keystore.file", "keys/alice.jks");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testSignatureEllipticCurve() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtec/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "ECKey");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "ES256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    // 1024 bits not allowed with RSA according to the spec
    @org.junit.Test
    public void testSmallSignatureKeySize() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.alias", "smallkey");
        properties.put("rs.security.keystore.password", "security");
        properties.put("rs.security.key.password", "security");
        properties.put("rs.security.keystore.file",
            "org/apache/cxf/systest/jaxrs/security/certs/smallkeysize.jks");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testUnsignedTokenSuccess() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/unsignedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.algorithm", "none");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    @org.junit.Test
    public void testUnsignedTokenFailure() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.algorithm", "none");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testSignatureEncryptionProperties() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        JwtAuthenticationClientFilter clientFilter = new JwtAuthenticationClientFilter();
        clientFilter.setJwsRequired(true);
        clientFilter.setJweRequired(true);
        providers.add(clientFilter);

        String address = "https://localhost:" + PORT + "/signedencryptedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.properties",
                       "org/apache/cxf/systest/jaxrs/security/alice.jwk.properties");
        properties.put("rs.security.encryption.properties",
                       "org/apache/cxf/systest/jaxrs/security/bob.jwk.properties");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    // Include the cert in the "x5c" header
    @org.junit.Test
    public void testSignatureCertificateTest() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtincludecert/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.alias", "alice");
        properties.put("rs.security.keystore.file", "keys/alice.jks");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put("rs.security.signature.include.cert", "true");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    // Include the cert in the "x5c" header
    @org.junit.Test
    public void testBadSignatureCertificateTest() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtincludecert/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.alias", "bethal");
        properties.put("rs.security.keystore.file", "keys/Bethal.jks");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put("rs.security.signature.include.cert", "true");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testHMACSignature() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/hmacsignedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "HMAC512Key");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    @org.junit.Test
    public void testBadHMACSignature() throws Exception {

        URL busFile = JWTAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/hmacsignedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "HMACKey");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    private List<String> toList(String address) {
        return Collections.singletonList(address);
    }

}
