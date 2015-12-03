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
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jaxrs.JwtAuthenticationClientFilter;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Some tests for authentication and authorization using JWT tokens.
 */
public class JWTAuthnAuthzTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerJwtAuthnAuthz.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerJwtAuthnAuthz.class, true));
        registerBouncyCastleIfNeeded();
    }
    
    private static void registerBouncyCastleIfNeeded() throws Exception {
        // Still need it for Oracle Java 7 and Java 8
        Security.addProvider(new BouncyCastleProvider());    
    }
    
    @AfterClass
    public static void unregisterBouncyCastleIfNeeded() throws Exception {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);    
    }
    
    @org.junit.Test
    public void testAuthentication() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwt/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setAudiences(toList(address));
        
        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<String, Object>();
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
    public void testAuthenticationFailure() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwt/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setAudiences(toList(address));
        
        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.alias", "alice");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/alice.jks");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testAuthorization() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthz/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setAudiences(toList(address));
        // The endpoint requires a role of "boss"
        claims.setProperty("role", "boss");
        
        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<String, Object>();
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
    public void testAuthorizationNoRole() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthz/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setAudiences(toList(address));
        
        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testAuthorizationWrongRole() throws Exception {

        URL busFile = JWTAuthnAuthzTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());

        String address = "https://localhost:" + PORT + "/signedjwtauthz/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setProperty("role", "manager");
        claims.setAudiences(toList(address));
        
        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    private List<String> toList(String address) {
        return Collections.singletonList(address);
    }
}
