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
import java.util.Arrays;
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
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Some tests for authentication and authorization using JWT tokens.
 */
public class JWTAuthnAuthzTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerJwtAuthnAuthz.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerJwtAuthnAuthz.class, true));
    }

    @org.junit.Test
    public void testAuthentication() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

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
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals("book", returnedBook.getName());
        assertEquals(123L, returnedBook.getId());
    }

    @org.junit.Test
    public void testAuthenticationFailure() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

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
        assertNotEquals(200, response.getStatus());
    }

    @org.junit.Test
    public void testAuthorization() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthz/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));
        // The endpoint requires a role of "boss"
        claims.setProperty("role", "boss");

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
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals("book", returnedBook.getName());
        assertEquals(123L, returnedBook.getId());
    }

    @org.junit.Test
    public void testAuthorizationWithTwoRolesAsList() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthz/bookstore/books";
        WebClient client =
                WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));
        // The endpoint requires a role of "boss"
        claims.setProperty("role", Arrays.asList("otherrole", "boss"));

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
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals("book", returnedBook.getName());
        assertEquals(123L, returnedBook.getId());
    }

    @org.junit.Test
    public void testAuthorizationWithTwoRolesAsString() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthz/bookstore/books";
        WebClient client =
                WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));
        // The endpoint requires a role of "boss"
        claims.setProperty("role", "otherrole,boss");

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
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals("book", returnedBook.getName());
        assertEquals(123L, returnedBook.getId());
    }

    @org.junit.Test
    public void testAuthorizationNoRole() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthz/bookstore/books";
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
        assertNotEquals(200, response.getStatus());
    }

    @org.junit.Test
    public void testAuthorizationWrongRole() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthz/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setProperty("role", "manager");
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
        assertNotEquals(200, response.getStatus());
    }

    @org.junit.Test
    public void testAuthorizationRolesAllowedAnnotation() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthzannotations/bookstore/booksrolesallowed";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));
        // The endpoint requires a role of "boss"
        claims.setProperty("role", "boss");

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
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals("book", returnedBook.getName());
        assertEquals(123L, returnedBook.getId());
    }

    @org.junit.Test
    public void testAuthorizationRolesAllowedAnnotationGET() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthzannotations/bookstore/booksrolesallowed";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));
        // The endpoint requires a role of "boss"
        claims.setProperty("role", "boss");

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.get();
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals("book", returnedBook.getName());
        assertEquals(123L, returnedBook.getId());
    }

    @org.junit.Test
    public void testAuthorizationRolesAllowedAnnotationHEAD() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthzannotations/bookstore/booksrolesallowed";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));
        // The endpoint requires a role of "boss"
        claims.setProperty("role", "boss");

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.head();
        assertEquals(200, response.getStatus());
    }

    @org.junit.Test
    public void testAuthorizationWrongRolesAllowedAnnotation() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthzannotations/bookstore/booksrolesallowed";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));
        // The endpoint requires a role of "boss"
        claims.setProperty("role", "manager");

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
        assertNotEquals(200, response.getStatus());
    }

    @org.junit.Test
    public void testAuthorizationWrongRolesAllowedAnnotationGET() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthzannotations/bookstore/booksrolesallowed";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));
        // The endpoint requires a role of "boss"
        claims.setProperty("role", "manager");

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.get();
        assertNotEquals(200, response.getStatus());
    }

    @org.junit.Test
    public void testAuthorizationWrongRolesAllowedAnnotationHEAD() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthzannotations/bookstore/booksrolesallowed";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));
        // The endpoint requires a role of "boss"
        claims.setProperty("role", "manager");

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.head();
        assertNotEquals(200, response.getStatus());
    }

    @org.junit.Test
    public void testClaimsAuthorization() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthz/bookstore/booksclaims";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));
        // The endpoint requires a role of "boss"
        claims.setProperty("role", "boss");
        // We also require a "smartcard" claim
        claims.setProperty("http://claims/authentication", "smartcard");

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
        assertEquals(200, response.getStatus());

        Book returnedBook = response.readEntity(Book.class);
        assertEquals("book", returnedBook.getName());
        assertEquals(123L, returnedBook.getId());
    }

    @org.junit.Test
    public void testClaimsAuthorizationWeakClaims() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthz/bookstore/booksclaims";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));
        // The endpoint requires a role of "boss"
        claims.setProperty("role", "boss");
        claims.setProperty("http://claims/authentication", "password");

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
        assertEquals(403, response.getStatus());
    }

    @org.junit.Test
    public void testClaimsAuthorizationNoClaims() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthz/bookstore/booksclaims";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(Instant.now().getEpochSecond());
        claims.setAudiences(toList(address));
        // The endpoint requires a role of "boss"
        claims.setProperty("role", "boss");

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
        assertEquals(403, response.getStatus());
    }

    private List<String> toList(String address) {
        return Collections.singletonList(address);
    }
}
