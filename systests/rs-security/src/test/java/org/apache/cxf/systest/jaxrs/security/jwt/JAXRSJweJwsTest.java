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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.common.PrivateKeyPasswordProvider;
import org.apache.cxf.rs.security.jose.jaxrs.JweClientResponseFilter;
import org.apache.cxf.rs.security.jose.jaxrs.JweWriterInterceptor;
import org.apache.cxf.rs.security.jose.jaxrs.JwsClientResponseFilter;
import org.apache.cxf.rs.security.jose.jaxrs.JwsWriterInterceptor;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.AesCbcHmacJweDecryption;
import org.apache.cxf.rs.security.jose.jwe.AesCbcHmacJweEncryption;
import org.apache.cxf.rs.security.jose.jwe.AesWrapKeyDecryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.AesWrapKeyEncryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.systest.jaxrs.security.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSJweJwsTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerJwt.PORT;
    private static final String CLIENT_JWEJWS_PROPERTIES =
        "org/apache/cxf/systest/jaxrs/security/bob.rs.properties";
    private static final String SERVER_JWEJWS_PROPERTIES =
        "org/apache/cxf/systest/jaxrs/security/alice.rs.properties";
    private static final String ENCODED_MAC_KEY = "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75"
        + "aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow";
    private static final Boolean SKIP_AES_GCM_TESTS = isJava6();
    
    private static boolean isJava6() {
        String version = System.getProperty("java.version");
        return 1.6D == Double.parseDouble(version.substring(0, 3));    
    }
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerJwt.class, true));
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
    @Test
    public void testJweJwkPlainTextRSA() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        String address = "https://localhost:" + PORT + "/jwejwkrsa";
        BookStore bs = createJweBookStore(address, null);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJweJwkBookBeanRSA() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        String address = "https://localhost:" + PORT + "/jwejwkrsa";
        BookStore bs = createJweBookStore(address,
                                       Collections.singletonList(new JacksonJsonProvider()));
        Book book = bs.echoBook(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }
    private BookStore createJweBookStore(String address, 
                                      List<?> mbProviders) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJweJwsTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<Object>();
        JweWriterInterceptor jweWriter = new JweWriterInterceptor();
        jweWriter.setUseJweOutputStream(true);
        providers.add(jweWriter);
        providers.add(new JweClientResponseFilter());
        if (mbProviders != null) {
            providers.addAll(mbProviders);
        }
        bean.setProviders(providers);
        bean.getProperties(true).put("rs.security.encryption.out.properties", 
                                     "org/apache/cxf/systest/jaxrs/security/bob.jwk.properties");
        bean.getProperties(true).put("rs.security.encryption.in.properties",
                                     "org/apache/cxf/systest/jaxrs/security/alice.jwk.properties");
        return bean.create(BookStore.class);
    }
    
    @Test
    public void testJweJwkAesWrap() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        String address = "https://localhost:" + PORT + "/jwejwkaeswrap";
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJweJwsTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<Object>();
        JweWriterInterceptor jweWriter = new JweWriterInterceptor();
        jweWriter.setUseJweOutputStream(true);
        providers.add(jweWriter);
        providers.add(new JweClientResponseFilter());
        bean.setProviders(providers);
        bean.getProperties(true).put("rs.security.encryption.properties",
                                     "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties");
        BookStore bs = bean.create(BookStore.class);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJweJwkAesCbcHMacInlineSet() throws Exception {
        doTestJweJwkAesCbcHMac("org/apache/cxf/systest/jaxrs/security/secret.aescbchmac.inlineset.properties");
    }
    @Test
    public void testJweJwkAesCbcHMacInlineSingleKey() throws Exception {
        doTestJweJwkAesCbcHMac("org/apache/cxf/systest/jaxrs/security/secret.aescbchmac.inlinejwk.properties");
    }
    private void doTestJweJwkAesCbcHMac(String propFile) throws Exception {
        String address = "https://localhost:" + PORT + "/jwejwkaescbchmac";
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJweJwsTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<Object>();
        JweWriterInterceptor jweWriter = new JweWriterInterceptor();
        jweWriter.setUseJweOutputStream(true);
        providers.add(jweWriter);
        providers.add(new JweClientResponseFilter());
        bean.setProviders(providers);
        bean.getProperties(true).put("rs.security.encryption.properties", propFile);
        PrivateKeyPasswordProvider provider = 
            new PrivateKeyPasswordProviderImpl("Thus from my lips, by yours, my sin is purged.");
        bean.getProperties(true).put("rs.security.key.password.provider", provider);
        BookStore bs = bean.create(BookStore.class);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJweRsaJwsRsa() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        String address = "https://localhost:" + PORT + "/jwejwsrsa";
        BookStore bs = createJweJwsBookStore(address, null, null);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJweRsaJwsRsaCert() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        String address = "https://localhost:" + PORT + "/jwejwsrsacert";
        
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJweJwsTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<Object>();
        JweWriterInterceptor jweWriter = new JweWriterInterceptor();
        jweWriter.setUseJweOutputStream(true);
        providers.add(jweWriter);
        providers.add(new JweClientResponseFilter());
        JwsWriterInterceptor jwsWriter = new JwsWriterInterceptor();
        jwsWriter.setUseJwsOutputStream(true);
        providers.add(jwsWriter);
        providers.add(new JwsClientResponseFilter());
        
        bean.setProviders(providers);
        bean.getProperties(true).put("rs.security.keystore.file", 
                                     "org/apache/cxf/systest/jaxrs/security/certs/jwkPublicSet.txt");
        bean.getProperties(true).put("rs.security.signature.out.properties", CLIENT_JWEJWS_PROPERTIES);
        bean.getProperties(true).put("rs.security.encryption.in.properties", CLIENT_JWEJWS_PROPERTIES);
        PrivateKeyPasswordProvider provider = new PrivateKeyPasswordProviderImpl();
        bean.getProperties(true).put("rs.security.signature.key.password.provider", provider);
        bean.getProperties(true).put("rs.security.decryption.key.password.provider", provider);
        BookStore bs = bean.create(BookStore.class);
        
        WebClient.getConfig(bs).getRequestContext().put("rs.security.keystore.alias.jwe.out", "AliceCert");
        WebClient.getConfig(bs).getRequestContext().put("rs.security.keystore.alias.jws.in", "AliceCert");
        WebClient.getConfig(bs).getRequestContext().put("rs.security.default.algorithms", "true");
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJweRsaJwsRsaCertInHeaders() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        String address = "https://localhost:" + PORT + "/jwejwsrsaCertInHeaders";
        BookStore bs = createJweJwsBookStore(address, null, null);
        WebClient.getConfig(bs).getRequestContext().put("rs.security.report.public.key", "true");
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJweRsaJwsPlainTextHMac() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        String address = "https://localhost:" + PORT + "/jwejwshmac";
        HmacJwsSignatureProvider hmacProvider = 
            new HmacJwsSignatureProvider(ENCODED_MAC_KEY, SignatureAlgorithm.HS256);
        BookStore bs = createJweJwsBookStore(address, hmacProvider, null);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJweRsaJwsBookHMac() throws Exception {
        if (SKIP_AES_GCM_TESTS) {
            return;
        }
        String address = "https://localhost:" + PORT + "/jwejwshmac";
        HmacJwsSignatureProvider hmacProvider = 
            new HmacJwsSignatureProvider(ENCODED_MAC_KEY, SignatureAlgorithm.HS256);
        BookStore bs = createJweJwsBookStore(address, hmacProvider,
                                             Collections.singletonList(new JacksonJsonProvider()));
        Book book = bs.echoBook(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }
    
    @Test
    public void testJwsJwkPlainTextHMac() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmac";
        BookStore bs = createJwsBookStore(address, null);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJwsJwkBookHMac() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkhmac";
        BookStore bs = createJwsBookStore(address,
                                       Collections.singletonList(new JacksonJsonProvider()));
        Book book = bs.echoBook(new Book("book", 123L));
        assertEquals("book", book.getName());
        assertEquals(123L, book.getId());
    }
    private BookStore createJwsBookStore(String address, 
                                         List<?> mbProviders) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJweJwsTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<Object>();
        JwsWriterInterceptor jwsWriter = new JwsWriterInterceptor();
        jwsWriter.setUseJwsOutputStream(true);
        providers.add(jwsWriter);
        providers.add(new JwsClientResponseFilter());
        if (mbProviders != null) {
            providers.addAll(mbProviders);
        }
        bean.setProviders(providers);
        bean.getProperties(true).put("rs.security.signature.properties", 
            "org/apache/cxf/systest/jaxrs/security/secret.jwk.properties");
        return bean.create(BookStore.class);
    }
    @Test
    public void testJwsJwkEC() throws Exception {
        String address = "https://localhost:" + PORT + "/jwsjwkec";
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJweJwsTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<Object>();
        JwsWriterInterceptor jwsWriter = new JwsWriterInterceptor();
        jwsWriter.setUseJwsOutputStream(true);
        providers.add(jwsWriter);
        providers.add(new JwsClientResponseFilter());
        bean.setProviders(providers);
        bean.getProperties(true).put("rs.security.signature.out.properties", 
            "org/apache/cxf/systest/jaxrs/security/jws.ec.private.properties");
        bean.getProperties(true).put("rs.security.signature.in.properties", 
            "org/apache/cxf/systest/jaxrs/security/jws.ec.public.properties");
        BookStore bs = bean.create(BookStore.class);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    @Test
    public void testJwsJwkRSA() throws Exception {
        doTestJwsJwkRSA("https://localhost:" + PORT + "/jwsjwkrsa", false, false);
    }
    @Test
    public void testJwsJwkInHeadersRSA() throws Exception {
        doTestJwsJwkRSA("https://localhost:" + PORT + "/jwsjwkrsa", true, true);
    }
    @Test
    public void testJwsJwkKidOnlyInHeadersRSA() throws Exception {
        doTestJwsJwkRSA("https://localhost:" + PORT + "/jwsjwkrsa", false, true);
    }
    private void doTestJwsJwkRSA(String address, 
                                 boolean reportPublicKey,
                                 boolean reportPublicKeyId) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJweJwsTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<Object>();
        JwsWriterInterceptor jwsWriter = new JwsWriterInterceptor();
        jwsWriter.setUseJwsOutputStream(true);
        providers.add(jwsWriter);
        providers.add(new JwsClientResponseFilter());
        bean.setProviders(providers);
        bean.getProperties(true).put("rs.security.signature.out.properties", 
            "org/apache/cxf/systest/jaxrs/security/alice.jwk.properties");
        bean.getProperties(true).put("rs.security.signature.in.properties",
            "org/apache/cxf/systest/jaxrs/security/bob.jwk.properties");
        if (reportPublicKey) {
            bean.getProperties(true).put("rs.security.report.public.key", true);
        }
        if (reportPublicKeyId) {
            bean.getProperties(true).put("rs.security.report.public.key.id", true);
        }
        BookStore bs = bean.create(BookStore.class);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    private BookStore createJweJwsBookStore(String address, 
                                 JwsSignatureProvider jwsSigProvider,
                                 List<?> mbProviders) throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJweJwsTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<Object>();
        JweWriterInterceptor jweWriter = new JweWriterInterceptor();
        jweWriter.setUseJweOutputStream(true);
        providers.add(jweWriter);
        providers.add(new JweClientResponseFilter());
        JwsWriterInterceptor jwsWriter = new JwsWriterInterceptor();
        if (jwsSigProvider != null) {
            jwsWriter.setSignatureProvider(jwsSigProvider);
        }
        jwsWriter.setUseJwsOutputStream(true);
        providers.add(jwsWriter);
        providers.add(new JwsClientResponseFilter());
        if (mbProviders != null) {
            providers.addAll(mbProviders);
        }
        bean.setProviders(providers);
        bean.getProperties(true).put("rs.security.encryption.out.properties", SERVER_JWEJWS_PROPERTIES);
        bean.getProperties(true).put("rs.security.signature.out.properties", CLIENT_JWEJWS_PROPERTIES);
        bean.getProperties(true).put("rs.security.encryption.in.properties", CLIENT_JWEJWS_PROPERTIES);
        bean.getProperties(true).put("rs.security.signature.in.properties", SERVER_JWEJWS_PROPERTIES);
        PrivateKeyPasswordProvider provider = new PrivateKeyPasswordProviderImpl();
        bean.getProperties(true).put("rs.security.signature.key.password.provider", provider);
        bean.getProperties(true).put("rs.security.decryption.key.password.provider", provider);
        return bean.create(BookStore.class);
    }
    
    @Test
    public void testJweAesCbcHmac() throws Exception {
        String address = "https://localhost:" + PORT + "/jweaescbchmac";
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JAXRSJweJwsTest.class.getResource("client.xml");
        Bus springBus = bf.createBus(busFile.toString());
        bean.setBus(springBus);
        bean.setServiceClass(BookStore.class);
        bean.setAddress(address);
        List<Object> providers = new LinkedList<Object>();
        // writer
        JweWriterInterceptor jweWriter = new JweWriterInterceptor();
        jweWriter.setUseJweOutputStream(true);
        
        final String cekEncryptionKey = "GawgguFyGrWKav7AX4VKUg";
        AesWrapKeyEncryptionAlgorithm keyEncryption = 
            new AesWrapKeyEncryptionAlgorithm(cekEncryptionKey, KeyAlgorithm.A128KW);
        jweWriter.setEncryptionProvider(new AesCbcHmacJweEncryption(ContentAlgorithm.A128CBC_HS256,
                                                                    keyEncryption));
        
        // reader 
        JweClientResponseFilter jweReader = new JweClientResponseFilter();
        jweReader.setDecryptionProvider(new AesCbcHmacJweDecryption(
                                    new AesWrapKeyDecryptionAlgorithm(cekEncryptionKey)));
        
        providers.add(jweWriter);
        providers.add(jweReader);
        bean.setProviders(providers);
        
        BookStore bs = bean.create(BookStore.class);
        String text = bs.echoText("book");
        assertEquals("book", text);
    }
    
    private static class PrivateKeyPasswordProviderImpl implements PrivateKeyPasswordProvider {
        private String password = "password";
        public PrivateKeyPasswordProviderImpl() {
            
        }
        public PrivateKeyPasswordProviderImpl(String password) {
            this.password = password;
        }
        @Override
        public char[] getPassword(Properties storeProperties) {
            return password.toCharArray();
        }
        
    }
}
