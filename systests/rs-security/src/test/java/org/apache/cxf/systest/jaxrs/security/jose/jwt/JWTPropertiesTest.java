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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

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
 * Some tests for various properties of JWT tokens.
 */
public class JWTPropertiesTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerJwtProperties.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServerJwtProperties.class, true));
    }

    @org.junit.Test
    public void testExpiredToken() throws Exception {

        URL busFile = JWTPropertiesTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/unsignedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        claims.setIssuedAt(now.toEpochSecond());
        claims.setAudiences(toList(address));

        // Set the expiry date to be yesterday
        claims.setExpiryTime(now.minusDays(1L).toEpochSecond());

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.algorithm", "none");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testFutureToken() throws Exception {

        URL busFile = JWTPropertiesTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/unsignedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setAudiences(toList(address));

        // Set the issued date to be in the future
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        claims.setIssuedAt(now.plusDays(1L).toEpochSecond());

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.algorithm", "none");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testNearFutureTokenFailure() throws Exception {

        URL busFile = JWTPropertiesTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/unsignedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setAudiences(toList(address));

        // Set the issued date to be in the near future
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        claims.setIssuedAt(now.plusSeconds(30L).toEpochSecond());

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.algorithm", "none");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testNearFutureTokenSuccess() throws Exception {

        URL busFile = JWTPropertiesTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/unsignedjwtnearfuture/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setAudiences(toList(address));

        // Set the issued date to be in the near future
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        claims.setIssuedAt(now.plusSeconds(30L).toEpochSecond());

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.algorithm", "none");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testNotBeforeFailure() throws Exception {

        URL busFile = JWTPropertiesTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/unsignedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setAudiences(toList(address));

        // Set the issued date to be in the near future
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        claims.setIssuedAt(now.toEpochSecond());
        claims.setNotBefore(now.plusSeconds(30L).toEpochSecond());

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.algorithm", "none");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testNotBeforeSuccess() throws Exception {

        URL busFile = JWTPropertiesTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/unsignedjwtnearfuture/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setAudiences(toList(address));

        // Set the issued date to be in the near future
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        claims.setIssuedAt(now.toEpochSecond());
        claims.setNotBefore(now.plusSeconds(30L).toEpochSecond());

        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.algorithm", "none");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testSetClaimsDirectly() throws Exception {

        URL busFile = JWTPropertiesTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/unsignedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        claims.setIssuedAt(now.toEpochSecond());
        claims.setAudiences(toList(address));

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.algorithm", "none");
        properties.put(JwtConstants.JWT_CLAIMS, claims);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testBadAudience() throws Exception {

        URL busFile = JWTPropertiesTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/unsignedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        claims.setIssuedAt(now.toEpochSecond());
        String badAddress = "https://localhost:" + PORT + "/badunsignedjwt/bookstore/books";
        claims.setAudiences(toList(badAddress));

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.algorithm", "none");
        properties.put(JwtConstants.JWT_CLAIMS, claims);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testNoAudience() throws Exception {

        URL busFile = JWTPropertiesTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/unsignedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        claims.setIssuedAt(now.toEpochSecond());

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.algorithm", "none");
        properties.put(JwtConstants.JWT_CLAIMS, claims);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testMultipleAudiences() throws Exception {

        URL busFile = JWTPropertiesTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/unsignedjwt/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        claims.setIssuedAt(now.toEpochSecond());

        String badAddress = "https://localhost:" + PORT + "/badunsignedjwt/bookstore/books";
        List<String> audiences = new ArrayList<>();
        audiences.add(address);
        audiences.add(badAddress);
        claims.setAudiences(audiences);

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.signature.algorithm", "none");
        properties.put(JwtConstants.JWT_CLAIMS, claims);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }

    private List<String> toList(String address) {
        return Collections.singletonList(address);
    }

}
