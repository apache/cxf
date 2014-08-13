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
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.crypto.Cipher;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.rs.security.oauth2.jwe.AesCbcHmacJweDecryption;
import org.apache.cxf.rs.security.oauth2.jwe.AesCbcHmacJweEncryption;
import org.apache.cxf.rs.security.oauth2.jwe.AesWrapKeyDecryptionAlgorithm;
import org.apache.cxf.rs.security.oauth2.jwe.AesWrapKeyEncryptionAlgorithm;
import org.apache.cxf.rs.security.oauth2.jws.HmacJwsSignatureProvider;
import org.apache.cxf.rs.security.oauth2.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.jwt.jaxrs.JweClientResponseFilter;
import org.apache.cxf.rs.security.oauth2.jwt.jaxrs.JweWriterInterceptor;
import org.apache.cxf.rs.security.oauth2.jwt.jaxrs.JwsClientResponseFilter;
import org.apache.cxf.rs.security.oauth2.jwt.jaxrs.JwsWriterInterceptor;
import org.apache.cxf.rs.security.oauth2.utils.crypto.PrivateKeyPasswordProvider;
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
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerJwt.class, true));
        registerBouncyCastleIfNeeded();
    }
    
    private static void registerBouncyCastleIfNeeded() throws Exception {
        try {
            // Java 8 apparently has it
            Cipher.getInstance(Algorithm.AES_GCM_ALGO_JAVA);
        } catch (Throwable t) {
            // Oracle Java 7
            Security.addProvider(new BouncyCastleProvider());    
        }
    }
    @AfterClass
    public static void unregisterBouncyCastleIfNeeded() throws Exception {
        Security.removeProvider(BouncyCastleProvider.class.getName());    
    }
    
    @Test
    public void testJweRsaJwsRsa() throws Exception {
        String address = "https://localhost:" + PORT + "/jwejwsrsa";
        doTestJweJwsRsa(address, null);
    }
    @Test
    public void testJweRsaJwsHMac() throws Exception {
        String address = "https://localhost:" + PORT + "/jwejwshmac";
        HmacJwsSignatureProvider hmacProvider = new HmacJwsSignatureProvider(ENCODED_MAC_KEY);
        hmacProvider.setDefaultJwtAlgorithm(Algorithm.HmacSHA256.getJwtName());
        doTestJweJwsRsa(address, hmacProvider);
    }
    
    private void doTestJweJwsRsa(String address, 
                                 JwsSignatureProvider jwsSigProvider) throws Exception {
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
        bean.setProviders(providers);
        bean.getProperties(true).put("rs.security.encryption.out.properties", SERVER_JWEJWS_PROPERTIES);
        bean.getProperties(true).put("rs.security.signature.out.properties", CLIENT_JWEJWS_PROPERTIES);
        bean.getProperties(true).put("rs.security.encryption.in.properties", CLIENT_JWEJWS_PROPERTIES);
        bean.getProperties(true).put("rs.security.signature.in.properties", SERVER_JWEJWS_PROPERTIES);
        PrivateKeyPasswordProvider provider = new PrivateKeyPasswordProviderImpl();
        bean.getProperties(true).put("rs.security.signature.key.password.provider", provider);
        bean.getProperties(true).put("rs.security.decryption.key.password.provider", provider);
        BookStore bs = bean.create(BookStore.class);
        String text = bs.echoText("book");
        assertEquals("book", text);
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
            new AesWrapKeyEncryptionAlgorithm(cekEncryptionKey, Algorithm.A128KW.getJwtName());
        jweWriter.setEncryptionProvider(new AesCbcHmacJweEncryption(Algorithm.A128KW.getJwtName(), 
                                                                    Algorithm.A128CBC_HS256.getJwtName(),
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

        @Override
        public char[] getPassword(Properties storeProperties) {
            return "password".toCharArray();
        }
        
    }
}
