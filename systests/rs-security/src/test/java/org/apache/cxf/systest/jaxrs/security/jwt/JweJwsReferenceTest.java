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
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Some encryption or signature tests, focus on how keys and certs are referenced and included.
 */
public class JweJwsReferenceTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerReference.PORT;
    private static final Boolean SKIP_AES_GCM_TESTS = isJava6();
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerReference.class, true));
        registerBouncyCastleIfNeeded();
    }
    
    private static void registerBouncyCastleIfNeeded() throws Exception {
        // Still need it for Oracle Java 7 and Java 8
        Security.addProvider(new BouncyCastleProvider());    
    }
    private static boolean isJava6() {
        String version = System.getProperty("java.version");
        return 1.6D == Double.parseDouble(version.substring(0, 3));    
    }
    @AfterClass
    public static void unregisterBouncyCastleIfNeeded() throws Exception {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);    
    }
    
    //
    // Encryption tests
    //
    // TODO
    @org.junit.Test
    @org.junit.Ignore
    public void testEncryptionIncludePublicKey() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        URL busFile = JweJwsReferenceTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweincludekey/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file", "org/apache/cxf/systest/jaxrs/security/certs/jwkPublicSet.txt");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA-OAEP");
        properties.put("rs.security.encryption.include.public.key", "true");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testEncryptionIncludeCert() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        URL busFile = JweJwsReferenceTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweincludecert/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.alias", "bob");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/bob.jks");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA-OAEP");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        // First test that it fails without adding a cert (reference). This is because
        // the service side does not have an alias configured

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
        
        // Now it should work
        properties.put("rs.security.encryption.include.cert", "true");
        WebClient.getConfig(client).getRequestContext().putAll(properties);
        response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testEncryptionIncludeCertNegativeTest() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        URL busFile = JweJwsReferenceTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweincludecert/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.alias", "alice");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/alice.jks");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA-OAEP");
        properties.put("rs.security.encryption.include.cert", "true");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        // Failure expected as we are encrypting to "alice" instead of "bob"
        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testEncryptionIncludeCertSha1() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        URL busFile = JweJwsReferenceTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweincludecert/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.alias", "bob");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/bob.jks");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA-OAEP");
        WebClient.getConfig(client).getRequestContext().putAll(properties);
        
        // First test that it fails without adding a cert (reference). This is because
        // the service side does not have an alias configured

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
        
        // Now it should work
        properties.put("rs.security.encryption.include.cert.sha1", "true");
        WebClient.getConfig(client).getRequestContext().putAll(properties);
        response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testEncryptionIncludeCertSha1NegativeTest() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        URL busFile = JweJwsReferenceTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweincludecert/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.alias", "alice");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/alice.jks");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA-OAEP");
        properties.put("rs.security.encryption.include.cert.sha1", "true");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        // Failure expected as we are encrypting to "alice" instead of "bob"
        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    //
    // Signature tests
    //
    
    @org.junit.Test
    public void testSignatureIncludeCert() throws Exception {

        URL busFile = JweJwsReferenceTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jwsincludecert/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.alias", "alice");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/alice.jks");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        // First test that it fails without adding a cert (reference). This is because
        // the service side does not have an alias configured

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
        
        // Now it should work
        properties.put("rs.security.signature.include.cert", "true");
        WebClient.getConfig(client).getRequestContext().putAll(properties);
        response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testSignatureIncludeCertNegativeTest() throws Exception {

        
        URL busFile = JweJwsReferenceTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jwsincludecert/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.alias", "morpit");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/Morpit.jks");
        properties.put("rs.security.signature.include.cert", "true");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        // Failure expected as we are signing using a cert not trusted by cxfca.jks
        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testSignatureIncludeCertSha1() throws Exception {

        URL busFile = JweJwsReferenceTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jwsincludecertsha1/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.alias", "alice");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/alice.jks");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        // First test that it fails without adding a cert (reference). This is because
        // the service side does not have an alias configured

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
        
        // Now it should work
        properties.put("rs.security.signature.include.cert.sha1", "true");
        WebClient.getConfig(client).getRequestContext().putAll(properties);
        response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);
    }
    
    
    @org.junit.Test
    public void testSignatureIncludeCertSha1NegativeTest() throws Exception {

        URL busFile = JweJwsReferenceTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jwsincludecertsha1/bookstore/books";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.alias", "morpit");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.file", 
                       "org/apache/cxf/systest/jaxrs/security/certs/Morpit.jks");
        properties.put("rs.security.signature.include.cert.sha1", "true");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        // Failure expected as we are signing using a cert not trusted by cxfca.jks
        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }
    
}
