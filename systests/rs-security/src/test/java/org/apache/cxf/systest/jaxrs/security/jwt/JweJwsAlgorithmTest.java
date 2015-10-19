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

package org.apache.cxf.systest.jaxrs.security.jwt;

import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jaxrs.JweWriterInterceptor;
import org.apache.cxf.rs.security.jose.jaxrs.JwsWriterInterceptor;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.systest.jaxrs.security.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Some encryption or signature tests, focus on algorithms.
 */
public class JweJwsAlgorithmTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerAlgorithms.PORT;
    private static final Boolean SKIP_AES_GCM_TESTS = isJava6();
    
    private static boolean isJava6() {
        String version = System.getProperty("java.version");
        return 1.6D == Double.parseDouble(version.substring(0, 3));    
    }
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerAlgorithms.class, true));
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
    
    //
    // Encryption tests
    //
    @org.junit.Test
    public void testEncryptionProperties() throws Exception {

        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        
        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweoaepgcm/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.encryption.properties", 
                       "org/apache/cxf/systest/jaxrs/security/bob.jwk.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
        
        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }
    
    @org.junit.Test
    public void testEncryptionDynamic() throws Exception {
        
        if (SKIP_AES_GCM_TESTS) {
            return;
        }

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweoaepgcm/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file", "org/apache/cxf/systest/jaxrs/security/certs/jwkPublicSet.txt");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA-OAEP");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
        
        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    @org.junit.Test
    public void testWrongKeyEncryptionAlgorithm() throws Exception {
        
        if (SKIP_AES_GCM_TESTS) {
            return;
        }

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweoaepgcm/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file", "org/apache/cxf/systest/jaxrs/security/certs/jwkPublicSet.txt");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA1_5");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testWrongContentEncryptionAlgorithm() throws Exception {
        
        if (SKIP_AES_GCM_TESTS || !SecurityTestUtil.checkUnrestrictedPoliciesInstalled()) {
            return;
        }

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweoaepgcm/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file", "org/apache/cxf/systest/jaxrs/security/certs/jwkPublicSet.txt");
        properties.put("rs.security.encryption.content.algorithm", "A192GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA-OAEP");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testBadEncryptingKey() throws Exception {
        
        if (SKIP_AES_GCM_TESTS) {
            return;
        }

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweoaepgcm/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "AliceCert");
        properties.put("rs.security.keystore.file", "org/apache/cxf/systest/jaxrs/security/certs/jwkPublicSet.txt");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA-OAEP");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    //
    // Signature tests
    //
    
    @org.junit.Test
    public void testSignatureProperties() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jws/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.signature.properties", 
                       "org/apache/cxf/systest/jaxrs/security/alice.jwk.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
        
        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }
    
    @org.junit.Test
    public void testSignatureDynamic() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jws/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "RS256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
        
        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }
    
    @org.junit.Test
    public void testWrongSignatureAlgorithm() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jws/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "PS256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testBadSigningKey() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jws/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.alias", "alice");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/alice.jks");
        properties.put("rs.security.signature.algorithm", "RS256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testSignatureEllipticCurve() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jwsec/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "ECKey");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "ES256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
        
        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }
}
