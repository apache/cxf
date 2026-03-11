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
package org.apache.cxf.rs.security.jose.jwe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;

import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests that FIPS mode selects RSA-OAEP-256 instead of RSA-OAEP for key algorithms.
 */
public class JweFipsAlgorithmTest {

    private boolean originalFipsEnabled;
    private String originalFipsProvider;
    private KeyPair rsaKeyPair;

    @Before
    public void setUp() throws Exception {
        originalFipsEnabled = JavaUtils.isFIPSEnabled();
        originalFipsProvider = JavaUtils.getFIPSSecurityProvider();
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        rsaKeyPair = kpg.generateKeyPair();
    }

    @After
    public void restoreFipsState() throws Exception {
        setFipsEnabled(originalFipsEnabled);
        setFipsProvider(originalFipsProvider);
    }

    @Test
    public void testDefaultPublicKeyAlgorithmNonFIPS() throws Exception {
        setFipsEnabled(false);
        KeyAlgorithm algo = invokeGetDefaultPublicKeyAlgorithm(rsaKeyPair.getPublic());
        assertEquals(KeyAlgorithm.RSA_OAEP, algo);
    }

    @Test
    public void testDefaultPublicKeyAlgorithmFIPS() throws Exception {
        setFipsEnabled(true);
        KeyAlgorithm algo = invokeGetDefaultPublicKeyAlgorithm(rsaKeyPair.getPublic());
        assertEquals(KeyAlgorithm.RSA_OAEP_256, algo);
    }

    @Test
    public void testDefaultPrivateKeyAlgorithmNonFIPS() throws Exception {
        setFipsEnabled(false);
        KeyAlgorithm algo = invokeGetDefaultPrivateKeyAlgorithm(rsaKeyPair.getPrivate());
        assertEquals(KeyAlgorithm.RSA_OAEP, algo);
    }

    @Test
    public void testDefaultPrivateKeyAlgorithmFIPS() throws Exception {
        setFipsEnabled(true);
        KeyAlgorithm algo = invokeGetDefaultPrivateKeyAlgorithm(rsaKeyPair.getPrivate());
        assertEquals(KeyAlgorithm.RSA_OAEP_256, algo);
    }

    @Test
    public void testRSAKeyDecryptionAlgorithmDefaultNonFIPS() throws Exception {
        setFipsEnabled(false);
        RSAKeyDecryptionAlgorithm decryptor =
            new RSAKeyDecryptionAlgorithm((RSAPrivateKey) rsaKeyPair.getPrivate());
        assertEquals(KeyAlgorithm.RSA_OAEP.getJwaName(), decryptor.getAlgorithm().getJwaName());
    }

    @Test
    public void testRSAKeyDecryptionAlgorithmDefaultFIPS() throws Exception {
        setFipsEnabled(true);
        RSAKeyDecryptionAlgorithm decryptor =
            new RSAKeyDecryptionAlgorithm((RSAPrivateKey) rsaKeyPair.getPrivate());
        assertEquals(KeyAlgorithm.RSA_OAEP_256.getJwaName(), decryptor.getAlgorithm().getJwaName());
    }

    private static KeyAlgorithm invokeGetDefaultPublicKeyAlgorithm(PublicKey key) throws Exception {
        Method method = JweUtils.class.getDeclaredMethod("getDefaultPublicKeyAlgorithm", PublicKey.class);
        method.setAccessible(true);
        return (KeyAlgorithm) method.invoke(null, key);
    }

    private static KeyAlgorithm invokeGetDefaultPrivateKeyAlgorithm(java.security.PrivateKey key)
        throws Exception {
        Method method = JweUtils.class.getDeclaredMethod("getDefaultPrivateKeyAlgorithm",
                                                          java.security.PrivateKey.class);
        method.setAccessible(true);
        return (KeyAlgorithm) method.invoke(null, key);
    }

    @Test(expected = JweException.class)
    public void testRSA15RejectedInFIPSMode() throws Exception {
        setFipsEnabled(true);
        RSAKeyDecryptionAlgorithm decryptor =
            new RSAKeyDecryptionAlgorithm((RSAPrivateKey) rsaKeyPair.getPrivate(),
                                          KeyAlgorithm.RSA1_5);
        JweHeaders headers = new JweHeaders();
        headers.setKeyEncryptionAlgorithm(KeyAlgorithm.RSA1_5);
        headers.setContentEncryptionAlgorithm(
            org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm.A128GCM);
        JweDecryptionInput input = new JweDecryptionInput(
            new byte[0], new byte[0], new byte[0], new byte[0],
            new byte[0], "{}", headers);
        decryptor.getDecryptedContentEncryptionKey(input);
    }

    private static void setFipsEnabled(boolean enabled) throws Exception {
        Field field = JavaUtils.class.getDeclaredField("isFIPSEnabled");
        field.setAccessible(true);
        field.setBoolean(null, enabled);
    }

    private static void setFipsProvider(String provider) throws Exception {
        Field field = JavaUtils.class.getDeclaredField("fipsSecurityProvider");
        field.setAccessible(true);
        field.set(null, provider);
    }
}
