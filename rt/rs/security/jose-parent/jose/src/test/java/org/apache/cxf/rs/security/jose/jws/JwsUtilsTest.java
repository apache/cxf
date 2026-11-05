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
package org.apache.cxf.rs.security.jose.jws;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseException;
import org.apache.cxf.rs.security.jose.common.KeyManagementUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.KeyOperation;
import org.apache.cxf.rs.security.jose.jwk.KeyType;
import org.apache.cxf.rs.security.jose.jwk.PublicKeyUse;
import org.apache.cxf.rt.security.crypto.MessageDigestUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JwsUtilsTest {

    @Test
    public void testSignatureAlgorithm() {
        assertTrue(AlgorithmUtils.isRsaSign(SignatureAlgorithm.RS256));
        assertFalse(AlgorithmUtils.isRsaSign(SignatureAlgorithm.NONE));

        try {
            AlgorithmUtils.RSA_SHA_SIGN_SET.add(SignatureAlgorithm.NONE.getJwaName());
            fail("Failure expected on trying to modify the algorithm lists");
        } catch (UnsupportedOperationException ex) {
            // expected
        }
    }

    @Test
    public void testLoadSignatureProviderFromJKS() throws Exception {
        Properties p = new Properties();
        p.put(JoseConstants.RSSEC_KEY_STORE_FILE,
            "org/apache/cxf/rs/security/jose/jws/alice.jks");
        p.put(JoseConstants.RSSEC_KEY_STORE_PSWD, "password");
        p.put(JoseConstants.RSSEC_KEY_PSWD, "password");
        p.put(JoseConstants.RSSEC_KEY_STORE_ALIAS, "alice");
        JwsHeaders headers = new JwsHeaders();
        JwsSignatureProvider jws = JwsUtils.loadSignatureProvider(createMessage(),
                                                                  p,
                                                                  headers);
        assertNotNull(jws);
        assertEquals("alice", headers.getKeyId());
    }
    @Test
    public void testLoadSignatureVerifierFromJKS() throws Exception {
        Properties p = new Properties();
        p.put(JoseConstants.RSSEC_KEY_STORE_FILE,
            "org/apache/cxf/rs/security/jose/jws/alice.jks");
        p.put(JoseConstants.RSSEC_KEY_STORE_PSWD, "password");
        p.put(JoseConstants.RSSEC_KEY_STORE_ALIAS, "alice");
        JwsSignatureVerifier jws = JwsUtils.loadSignatureVerifier(createMessage(),
                                                                  p,
                                                                  new JwsHeaders());
        assertNotNull(jws);
    }
    @Test
    public void testLoadSignatureVerifierFromJwkStoreWithX509Thumbprint() throws Exception {
        Properties p = new Properties();
        p.put(JoseConstants.RSSEC_KEY_STORE_TYPE, "jwk");
        p.put(JoseConstants.RSSEC_KEY_STORE_FILE, "jwk/pubKeys.jwks");
        p.put(JoseConstants.RSSEC_KEY_STORE_ALIAS, "rsas001");
        p.put(JoseConstants.RSSEC_SIGNATURE_ALGORITHM, "RS256");

        JwsHeaders headers = new JwsHeaders(SignatureAlgorithm.RS256);
        headers.setX509Thumbprint("dGh1bWJwcmludA");
        assertNotNull(JwsUtils.loadSignatureVerifier(createMessage(), p, headers));

        headers = new JwsHeaders(SignatureAlgorithm.RS256);
        headers.setX509ThumbprintSHA256("dGh1bWJwcmludA");
        assertNotNull(JwsUtils.loadSignatureVerifier(createMessage(), p, headers));
    }
    @Test
    public void testLoadSignatureVerifierFromProperties() throws Exception {
        JwsSignatureVerifier jws = JwsUtils.loadSignatureVerifier("classpath:/jws/signature.properties", null);
        assertEquals(SignatureAlgorithm.NONE, jws.getAlgorithm());
    }
    @Test
    public void testLoadVerificationKey() throws Exception {
        Properties p = new Properties();
        p.put(JoseConstants.RSSEC_KEY_STORE_FILE,
            "org/apache/cxf/rs/security/jose/jws/alice.jks");
        p.put(JoseConstants.RSSEC_KEY_STORE_PSWD, "password");
        p.put(JoseConstants.RSSEC_KEY_STORE_ALIAS, "alice");
        JsonWebKeys keySet = JwsUtils.loadPublicVerificationKeys(createMessage(), p, true);
        assertEquals(1, keySet.asMap().size());
        List<JsonWebKey> keys = keySet.getRsaKeys();
        assertEquals(1, keys.size());
        JsonWebKey key = keys.get(0);
        assertEquals(KeyType.RSA, key.getKeyType());
        assertEquals("alice", key.getKeyId());
        assertNotNull(key.getKeyProperty(JsonWebKey.RSA_PUBLIC_EXP));
        assertNotNull(key.getKeyProperty(JsonWebKey.RSA_MODULUS));
        assertNull(key.getKeyProperty(JsonWebKey.RSA_PRIVATE_EXP));
        assertNull(key.getX509Chain());
    }
    @Test
    public void testLoadVerificationKeyWithCert() throws Exception {
        Properties p = new Properties();
        p.put(JoseConstants.RSSEC_KEY_STORE_FILE,
            "org/apache/cxf/rs/security/jose/jws/alice.jks");
        p.put(JoseConstants.RSSEC_KEY_STORE_PSWD, "password");
        p.put(JoseConstants.RSSEC_KEY_STORE_ALIAS, "alice");
        p.put(JoseConstants.RSSEC_SIGNATURE_INCLUDE_CERT, true);
        JsonWebKeys keySet = JwsUtils.loadPublicVerificationKeys(createMessage(), p, true);
        assertEquals(1, keySet.asMap().size());
        List<JsonWebKey> keys = keySet.getRsaKeys();
        assertEquals(1, keys.size());
        JsonWebKey key = keys.get(0);
        assertEquals(KeyType.RSA, key.getKeyType());
        assertEquals("alice", key.getKeyId());
        assertNotNull(key.getKeyProperty(JsonWebKey.RSA_PUBLIC_EXP));
        assertNotNull(key.getKeyProperty(JsonWebKey.RSA_MODULUS));
        assertNull(key.getKeyProperty(JsonWebKey.RSA_PRIVATE_EXP));
        List<String> chain = key.getX509Chain();
        assertNotNull(chain);
        assertEquals(2, chain.size());
    }

    @Test
    public void testLoadSignatureVerifierFromJwkStoreUsesHeaderKeyId() throws Exception {
        KeyPair oldKey = createRsaKeyPair();
        KeyPair newKey = createRsaKeyPair();
        KeyPair encKey = createRsaKeyPair();
        KeyPair opsKey = createRsaKeyPair();
        KeyPair unmarkedKey = createRsaKeyPair();
        JsonWebKey opsJwk = createPublicJwk(opsKey, "key-ops", null);
        opsJwk.setKeyOperation(Arrays.asList(KeyOperation.VERIFY));
        JsonWebKeys jwks = new JsonWebKeys(Arrays.asList(
            createPublicJwk(oldKey, "key-old", PublicKeyUse.SIGN),
            createPublicJwk(newKey, "key-new", PublicKeyUse.SIGN),
            createPublicJwk(encKey, "key-enc", PublicKeyUse.ENCRYPT),
            opsJwk,
            createPublicJwk(unmarkedKey, "key-unmarked", null)));

        Properties p = new Properties();
        p.put(JoseConstants.RSSEC_KEY_STORE_TYPE, "jwk");
        p.put(JoseConstants.RSSEC_KEY_STORE_JWKSET, JwkUtils.jwkSetToJson(jwks));
        p.put(JoseConstants.RSSEC_SIGNATURE_ALGORITHM, "RS256");

        // Without an alias, the key id in the headers selects the key, so a rotated key is picked up
        assertTrue(verifyWithLoadedVerifier(p, sign(oldKey, "key-old"), false));
        assertTrue(verifyWithLoadedVerifier(p, sign(newKey, "key-new"), false));
        assertTrue(verifyWithLoadedVerifier(p, sign(opsKey, "key-ops"), false));
        // Only a key explicitly marked for signatures is selected by the key id, otherwise
        // the single key marked for verification ("key-ops") is used
        assertFalse(verifyWithLoadedVerifier(p, sign(encKey, "key-enc"), false));
        assertFalse(verifyWithLoadedVerifier(p, sign(unmarkedKey, "key-unmarked"), false));
    }

    @Test
    public void testLoadSignatureVerifierFromJwkStoreAliasPinsKey() throws Exception {
        KeyPair oldKey = createRsaKeyPair();
        KeyPair newKey = createRsaKeyPair();
        KeyPair unmarkedKey = createRsaKeyPair();
        JsonWebKeys jwks = new JsonWebKeys(Arrays.asList(
            createPublicJwk(oldKey, "key-old", PublicKeyUse.SIGN),
            createPublicJwk(newKey, "key-new", PublicKeyUse.SIGN),
            createPublicJwk(unmarkedKey, "key-unmarked", null)));

        Properties p = new Properties();
        p.put(JoseConstants.RSSEC_KEY_STORE_TYPE, "jwk");
        p.put(JoseConstants.RSSEC_KEY_STORE_JWKSET, JwkUtils.jwkSetToJson(jwks));
        p.put(JoseConstants.RSSEC_KEY_STORE_ALIAS, "key-old");
        p.put(JoseConstants.RSSEC_SIGNATURE_ALGORITHM, "RS256");

        // A configured alias pins the key, whatever the key id in the headers
        assertTrue(verifyWithLoadedVerifier(p, sign(oldKey, null), false));
        assertTrue(verifyWithLoadedVerifier(p, sign(oldKey, "key-unknown"), false));
        assertFalse(verifyWithLoadedVerifier(p, sign(newKey, "key-new"), false));
        assertFalse(verifyWithLoadedVerifier(p, sign(unmarkedKey, "key-unmarked"), false));
        // unless accepting public keys has been explicitly enabled, which selects any key by key id as before
        assertTrue(verifyWithLoadedVerifier(p, sign(newKey, "key-new"), true));
        assertTrue(verifyWithLoadedVerifier(p, sign(unmarkedKey, "key-unmarked"), true));
    }

    @Test
    public void testLoadSignatureVerifierWithX509HeadersUsesAlias() throws Exception {
        // The trust store holds the certificates of alice, bob and a CA which issued carol's certificate
        Properties p = trustStoreProperties("alice");

        assertTrue(verifyWithLoadedVerifier(p, signWithX509Headers("alice", false, false)));
        assertTrue(verifyWithLoadedVerifier(p, signWithX509Headers("alice", true, false)));
        assertTrue(verifyWithLoadedVerifier(p, signWithX509Headers("alice", false, true)));
        // The certificate of the configured alias is used
        assertFalse(verifyWithLoadedVerifier(p, signWithX509Headers("bob", false, false)));
        assertFalse(verifyWithLoadedVerifier(p, signWithX509Headers("bob", true, false)));
        assertFalse(verifyWithLoadedVerifier(p, signWithX509Headers("bob", true, true)));
        assertFalse(verifyWithLoadedVerifier(p, signWithX509Headers("bob", false, true)));
        assertFalse(verifyWithLoadedVerifier(p, signWithX509Headers("carol", false, true)));
        // unless the X.509 headers have been explicitly accepted, which select the certificate as before
        p.put(JoseConstants.RSSEC_ACCEPT_X509_KEY_HEADERS, "true");
        assertTrue(verifyWithLoadedVerifier(p, signWithX509Headers("bob", true, false)));
        assertTrue(verifyWithLoadedVerifier(p, signWithX509Headers("bob", false, true)));
        assertTrue(verifyWithLoadedVerifier(p, signWithX509Headers("carol", false, true)));
        assertTrue(verifyWithLoadedVerifier(p, signWithX509Headers("alice", false, false)));
    }

    @Test
    public void testLoadSignatureVerifierWithX509ChainIssuedByAlias() throws Exception {
        // An x5c certificate issued by the certificate of the configured alias is accepted
        Properties p = trustStoreProperties("ca");

        assertTrue(verifyWithLoadedVerifier(p, signWithX509Headers("carol", false, true)));
        assertFalse(verifyWithLoadedVerifier(p, signWithX509Headers("bob", false, true)));
    }

    @Test
    public void testLoadSignatureVerifierWithX509HeadersWithoutAlias() throws Exception {
        // Without an alias, the x5c and x5t headers select the certificate as before
        Properties p = trustStoreProperties(null);

        assertTrue(verifyWithLoadedVerifier(p, signWithX509Headers("alice", true, false)));
        assertTrue(verifyWithLoadedVerifier(p, signWithX509Headers("bob", true, false)));
        assertTrue(verifyWithLoadedVerifier(p, signWithX509Headers("bob", false, true)));
        assertTrue(verifyWithLoadedVerifier(p, signWithX509Headers("carol", false, true)));
    }

    private static Properties trustStoreProperties(String alias) {
        Properties p = new Properties();
        p.put(JoseConstants.RSSEC_KEY_STORE_TYPE, "PKCS12");
        p.put(JoseConstants.RSSEC_KEY_STORE_FILE, "org/apache/cxf/rs/security/jose/jws/trust.p12");
        p.put(JoseConstants.RSSEC_KEY_STORE_PSWD, "password");
        p.put(JoseConstants.RSSEC_SIGNATURE_ALGORITHM, "RS256");
        if (alias != null) {
            p.put(JoseConstants.RSSEC_KEY_STORE_ALIAS, alias);
        }
        return p;
    }

    private static String signWithX509Headers(String signer, boolean thumbprint, boolean chain) throws Exception {
        KeyStore signers = KeyStore.getInstance("PKCS12");
        try (InputStream is = JwsUtilsTest.class.getResourceAsStream("signers.p12")) {
            signers.load(is, "password".toCharArray());
        }
        X509Certificate[] certs = Arrays.copyOf(signers.getCertificateChain(signer),
            signers.getCertificateChain(signer).length, X509Certificate[].class);

        JwsHeaders headers = new JwsHeaders(SignatureAlgorithm.RS256);
        if (thumbprint) {
            headers.setX509Thumbprint(Base64UrlUtility.encode(
                MessageDigestUtils.createDigest(certs[0].getEncoded(), MessageDigestUtils.ALGO_SHA_1)));
            headers.setX509ThumbprintSHA256(Base64UrlUtility.encode(
                MessageDigestUtils.createDigest(certs[0].getEncoded(), MessageDigestUtils.ALGO_SHA_256)));
        }
        if (chain) {
            headers.setX509Chain(KeyManagementUtils.encodeX509CertificateChain(certs));
        }
        JwsCompactProducer producer = new JwsCompactProducer(headers, "payload");
        return producer.signWith(JwsUtils.getPrivateKeySignatureProvider(
            (PrivateKey)signers.getKey(signer, "password".toCharArray()), SignatureAlgorithm.RS256));
    }

    private static boolean verifyWithLoadedVerifier(Properties props, String jws) {
        JwsCompactConsumer consumer = new JwsCompactConsumer(jws);
        try {
            JwsSignatureVerifier verifier =
                JwsUtils.loadSignatureVerifier(createMessage(), props, consumer.getJwsHeaders());
            return consumer.verifySignatureWith(verifier);
        } catch (JoseException ex) {
            return false;
        }
    }

    private static KeyPair createRsaKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    private static JsonWebKey createPublicJwk(KeyPair keyPair, String kid, PublicKeyUse use) {
        JsonWebKey jwk = JwkUtils.fromRSAPublicKey((RSAPublicKey)keyPair.getPublic(), "RS256", kid);
        if (use != null) {
            jwk.setPublicKeyUse(use);
        }
        return jwk;
    }

    private static String sign(KeyPair keyPair, String kid) {
        JwsHeaders headers = new JwsHeaders(SignatureAlgorithm.RS256);
        if (kid != null) {
            headers.setKeyId(kid);
        }
        JwsCompactProducer producer = new JwsCompactProducer(headers, "payload");
        return producer.signWith(
            JwsUtils.getPrivateKeySignatureProvider(keyPair.getPrivate(), SignatureAlgorithm.RS256));
    }

    private static boolean verifyWithLoadedVerifier(Properties props, String jws, boolean acceptPublicKey) {
        JwsCompactConsumer consumer = new JwsCompactConsumer(jws);
        Message m = createMessage();
        m.put(JoseConstants.RSSEC_ACCEPT_PUBLIC_KEY, acceptPublicKey);
        JwsSignatureVerifier verifier = JwsUtils.loadSignatureVerifier(m, props, consumer.getJwsHeaders());
        return consumer.verifySignatureWith(verifier);
    }

    private static Message createMessage() {
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        e.put(Bus.class, BusFactory.getThreadDefaultBus());
        m.setExchange(e);
        m.put(JoseConstants.RSSEC_SIGNATURE_INCLUDE_KEY_ID, "true");
        e.setInMessage(m);
        return m;
    }
}
