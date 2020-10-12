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

import java.util.List;
import java.util.Properties;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.KeyType;

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
