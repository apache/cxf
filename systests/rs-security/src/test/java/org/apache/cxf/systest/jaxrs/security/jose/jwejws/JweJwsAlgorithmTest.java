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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jaxrs.JweWriterInterceptor;
import org.apache.cxf.rs.security.jose.jaxrs.JwsWriterInterceptor;
import org.apache.cxf.rs.security.jose.jws.NoneJwsSignatureProvider;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.systest.jaxrs.security.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Some encryption or signature tests, focus on algorithms.
 */
public class JweJwsAlgorithmTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerAlgorithms.PORT;

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

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweoaepgcm/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
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

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweoaepgcm/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
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

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweoaepgcm/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
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
    public void testWrongKeyEncryptionAlgorithmKeyIncluded() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweoaepgcm/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file", "org/apache/cxf/systest/jaxrs/security/certs/jwkPublicSet.txt");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA1_5");
        properties.put("rs.security.encryption.include.public.key", "true");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testWrongContentEncryptionAlgorithm() throws Exception {
        if (!SecurityTestUtil.checkUnrestrictedPoliciesInstalled()) {
            return;
        }

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweoaepgcm/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
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

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jweoaepgcm/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "AliceCert");
        properties.put("rs.security.keystore.file", "org/apache/cxf/systest/jaxrs/security/certs/jwkPublicSet.txt");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA-OAEP");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    // 1024 bits not allowed with RSA according to the spec
    @org.junit.Test
    public void testSmallEncryptionKeySize() throws Exception {
        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jwesmallkey/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.alias", "smallkey");
        properties.put("rs.security.keystore.password", "security");
        properties.put("rs.security.keystore.file",
            "org/apache/cxf/systest/jaxrs/security/certs/smallkeysize.jks");
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "RSA-OAEP");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testManualEncryption() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());

        String address = "http://localhost:" + PORT + "/jweoaepgcm/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.encryption.properties",
                       "org/apache/cxf/systest/jaxrs/security/bob.jwk.properties");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        String header = "eyJhbGciOiJSU0EtT0FFUCIsImVuYyI6IkExMjhHQ00iLCJjdHkiOiJqc29uIn0";
        String encryptedKey = "f_Njrwn8fLxvIfftV27lSqEgvyIvkfx5tcI6xJdzXqxSL-Xssaq9TFwbhiJIU6k23i1uLFDd3r7rL"
            + "V9THMcAo80C-m_SIbA6X4daeIm7ANmREZ9sw9QkD0URis6MAuZkoYIRB6z9g7TDmPTdrpTUWJbwYaBAe-_VYaoVBwRv_A"
            + "ikPdKJEUWSMxouJEq4TZUVveNjI_tflZpudz1mYXKv9Lw_5byYpwgIB9crI9BR0kfCK9x3BXVFMZHJAg0yIuAKDkcs9Ts"
            + "TIV0jLXRnb50Uc62OuJ6VFGQw-AL3tNHLRKYXjwDnE492wAZmsaxefql9wbv7b8BLmRUNeKER-26tdA";
        String iv = "rqUxWbEenVnC3QFx";
        String cipherText = "8iE2vM79BkXVJ0afH6fbig5uFpQ71nxc-i2SbokQtZO7";
        String authnTag = "bZk8RwVMZgawyFNSOkMLaw";


        // Successful test
        Response response = client.post(header + "." + encryptedKey + "." + iv + "." + cipherText + "." + authnTag);
        assertEquals(response.getStatus(), 200);

        // Tamper with the values
        response = client.post(header + "xyz." + encryptedKey + "." + iv + "." + cipherText + "." + authnTag);
        assertNotEquals(response.getStatus(), 200);

        response =  client.post(header + "." + encryptedKey + "xyz." + iv + "." + cipherText + "." + authnTag);
        assertNotEquals(response.getStatus(), 200);

        response = client.post(header + "." + encryptedKey + "." + iv + "xyz." + cipherText + "." + authnTag);
        assertNotEquals(response.getStatus(), 200);

        response = client.post(header + "." + encryptedKey + "." + iv + "." + cipherText + "xyz." + authnTag);
        assertNotEquals(response.getStatus(), 200);

        response = client.post(header + "." + encryptedKey + "." + iv + "." + cipherText + "." + authnTag + "xyz");
        assertNotEquals(response.getStatus(), 200);

        response = client.post(header + "." + encryptedKey + "." + iv + "." + cipherText + ".");
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testEncryptionPBES() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jwepbes/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "PBES2-HS256+A128KW");
        String password = "123456789123456789";
        properties.put("rs.security.key.password.provider", new PrivateKeyPasswordProviderImpl(password));
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    @org.junit.Test
    public void testEncryptionPBESDifferentCount() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JweWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jwepbes/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        String password = "123456789123456789";
        properties.put("rs.security.encryption.content.algorithm", "A128GCM");
        properties.put("rs.security.encryption.key.algorithm", "PBES2-HS256+A128KW");
        properties.put("rs.security.key.password.provider", new PrivateKeyPasswordProviderImpl(password));
        properties.put("rs.security.encryption.pbes2.count", "1000");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }


    //
    // Signature tests
    //

    @org.junit.Test
    public void testSignatureProperties() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jws/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
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

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jws/bookstore/books";
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

        Response response = client.post(new Book("book", 123L));
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);
    }

    @org.junit.Test
    public void testWrongSignatureAlgorithm() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jws/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
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
    public void testWrongSignatureAlgorithmKeyIncluded() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jws/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "PS256");
        properties.put("rs.security.signature.include.public.key", true);
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testBadSigningKey() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jws/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "password");
        properties.put("rs.security.key.password", "password");
        properties.put("rs.security.keystore.alias", "alice");
        properties.put("rs.security.keystore.file", "keys/alice.jks");
        properties.put("rs.security.signature.algorithm", "RS256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testSignatureEllipticCurve() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jwsec/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
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

    @org.junit.Test
    public void testManualSignature() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());

        String address = "http://localhost:" + PORT + "/jws/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        String header = "eyJhbGciOiJSUzI1NiIsImN0eSI6Impzb24ifQ";
        String payload = "eyJCb29rIjp7ImlkIjoxMjMsIm5hbWUiOiJib29rIn19";
        String sig = "mZJVPy83atFNxQMeJqkVbR8t1srr9LgKBGT0hgiymjNepRgqedvFG5B8E8UPAzfzNLsos91gGdneUEKrWauU4GoDPTzngX"
            + "798aDP6lsn5bUoTMKLfaWp9uzHDIzLMjGkabn92nrIpdK4JKDYNjdSUJIT2L97jggg0aoLhJQHVw2LdF1fpYdM-HCyccNW"
            + "HQbAR7bDZdITZFnDi8b22QfHCqeLV7m4mBvNDtNX337wtoUKyjPYBMoWc12hHDCwQyu_gfW6zFioF5TGx-Ifg8hrFlnyUr"
            + "vnSdP-FUtXiGeWBIvE_L6gD7DfM4u9hkK757vTjjMR_pF2CW3pfSH-Ha8v0A";

        // Successful test
        Response response = client.post(header + "." + payload + "." + sig);
        assertEquals(response.getStatus(), 200);

        Book returnedBook = response.readEntity(Book.class);
        assertEquals(returnedBook.getName(), "book");
        assertEquals(returnedBook.getId(), 123L);

        // No signature
        response = client.post(header + "." + payload + ".");
        assertNotEquals(response.getStatus(), 200);

        // Modified signature
        String sig2 = sig.replace('y', 'z');
        response = client.post(header + "." + payload + "." + sig2);
        assertNotEquals(response.getStatus(), 200);

        // Modified payload
        String payload2 = payload.replace('y', 'z');
        response = client.post(header + "." + payload2 + "." + sig);
        assertNotEquals(response.getStatus(), 200);
    }

    // 1024 bits not allowed with RSA according to the spec
    @org.junit.Test
    public void testSmallSignatureKeySize() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwsWriterInterceptor());

        String address = "http://localhost:" + PORT + "/jwssmallkey/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.alias", "smallkey");
        properties.put("rs.security.keystore.password", "security");
        properties.put("rs.security.key.password", "security");
        properties.put("rs.security.keystore.file",
            "org/apache/cxf/systest/jaxrs/security/certs/smallkeysize.jks");
        properties.put("rs.security.signature.algorithm", "RS256");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testUnsignedTokenFailure() throws Exception {

        URL busFile = JweJwsAlgorithmTest.class.getResource("client.xml");

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonJsonProvider());
        JwsWriterInterceptor writerInterceptor = new JwsWriterInterceptor();
        writerInterceptor.setSignatureProvider(new NoneJwsSignatureProvider());
        providers.add(writerInterceptor);

        String address = "http://localhost:" + PORT + "/jws/bookstore/books";
        WebClient client =
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");

        Map<String, Object> properties = new HashMap<>();
        properties.put("rs.security.keystore.type", "jwk");
        properties.put("rs.security.keystore.alias", "2011-04-29");
        properties.put("rs.security.keystore.file",
                       "org/apache/cxf/systest/jaxrs/security/certs/jwkPrivateSet.txt");
        properties.put("rs.security.signature.algorithm", "none");
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.post(new Book("book", 123L));
        assertNotEquals(response.getStatus(), 200);
    }

}
