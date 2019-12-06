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
package org.apache.cxf.rs.security.jose.support;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweCompactConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionOutput;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweJsonConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJsonConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

import org.junit.Assert;

public class ApacheCXFConsumer {

    public void consumeJWS(String signedData, String plainText, String jwks) {
        JsonWebKeys keys = JwkUtils.readJwkSet(jwks);
        if (signedData.startsWith("{")) {
            consumeJsonJWS(signedData, plainText, keys);
        } else {
            consumeCompactJWS(signedData, plainText, keys);
        }
    }

    public void consumeJWE(String encryptedData, String plainText, String jwks) {
        JsonWebKeys keys = JwkUtils.readJwkSet(jwks);
        if (encryptedData.startsWith("{")) {
            consumeJsonJWE(encryptedData, plainText, keys);
        } else {
            consumeCompactJWE(encryptedData, plainText, keys);
        }
    }

    protected void consumeCompactJWS(String signedData, String plainText, JsonWebKeys keys) {

        // Validate Signature

        // 1. Read data to get key id (only need to do this if you don't know the key)
        JwsCompactConsumer jwsConsumer = new JwsCompactConsumer(signedData);
        String kid = jwsConsumer.getJwsHeaders().getKeyId();

        Assert.assertNotNull("Data does not contain kid header.", kid);

        // 2. Get key
        JsonWebKey key = keys.getKey(kid);
        Assert.assertNotNull("Data signed with unknown key", key);

        // 3. Verify
        SignatureAlgorithm signAlgo = jwsConsumer.getJwsHeaders().getSignatureAlgorithm();
        Assert.assertNotNull("Signed data does not define algorithm used", signAlgo);
        JwsSignatureVerifier signatureVerifier = JwsUtils.getSignatureVerifier(key, signAlgo);
        Assert.assertTrue("Signature validation failed", jwsConsumer.verifySignatureWith(signatureVerifier));

        // Validate plain text
        Assert.assertEquals(plainText, jwsConsumer.getDecodedJwsPayload());
    }

    protected void consumeJsonJWS(String signedData, String plainText, JsonWebKeys keys) {

        // Validate signature

        // 1. Read data
        JwsJsonConsumer jwsConsumer = new JwsJsonConsumer(signedData);
        jwsConsumer.getSignatureEntries().forEach(signature -> {
            String kid = signature.getKeyId();
            Assert.assertNotNull("Signature does not contain kid.", kid);

            // 2. Get Key
            JsonWebKey key = keys.getKey(kid);
            Assert.assertNotNull("Data signed with unknown key", key);

            // 3. Verify
            SignatureAlgorithm signAlgo = signature.getUnionHeader().getSignatureAlgorithm();
            Assert.assertNotNull("Signed data does not define algorithm used", signAlgo);
            JwsSignatureVerifier signatureVerifier = JwsUtils.getSignatureVerifier(key, signAlgo);
            Assert.assertTrue("Signature validation failed", jwsConsumer.verifySignatureWith(signatureVerifier));

            // Validate plain text
            Assert.assertEquals(plainText, signature.getDecodedJwsPayload());
        });
    }

    protected void consumeCompactJWE(String encryptedData, String plainText, JsonWebKeys keys) {

        // Decrypt

        // 1. Read data to get key id (only need to do this if you don't know the key)
        JweCompactConsumer jweConsumer = new JweCompactConsumer(encryptedData);
        String kid = jweConsumer.getJweHeaders().getKeyId();

        Assert.assertNotNull("Data does not contain kid header.", kid);

        // 2. Get key
        JsonWebKey key = keys.getKey(kid);
        Assert.assertNotNull("Data encrypted with unknown key", key);

        // 3. decrypt
        JweDecryptionProvider decryptor = getJweDecryptionProvider(key,
            jweConsumer.getJweHeaders().getKeyEncryptionAlgorithm(),
            jweConsumer.getJweHeaders().getContentEncryptionAlgorithm());
        String decryptedText = decryptor.decrypt(encryptedData).getContentText();

        // Validate plain text
        Assert.assertEquals(plainText, decryptedText);
    }

    protected void consumeJsonJWE(String encryptedData, String plainText, JsonWebKeys keys) {

        // Decrypt

        // 1. Read data
        JweJsonConsumer jweConsumer = new JweJsonConsumer(encryptedData);
        jweConsumer.getRecipients().forEach(encryptionBlock -> {
            String kid = Crypto.findKeyId(jweConsumer, encryptionBlock);
            Assert.assertNotNull("Data does not contain kid header.", kid);

            // 2. Get Key
            JsonWebKey key = keys.getKey(kid);
            Assert.assertNotNull("Data encrypted with unknown key", key);

            // 3. Decrypt
            KeyAlgorithm keyAlgo = Crypto.findKeyAlgorithm(jweConsumer, encryptionBlock);
            ContentAlgorithm contentAlgo = Crypto.findContentAlgorithm(jweConsumer, encryptionBlock);
            Assert.assertNotNull("Encrypted data does not define algorithm used", contentAlgo);
            JweDecryptionProvider decryptor = getJweDecryptionProvider(key, keyAlgo, contentAlgo);
            JweDecryptionOutput output = jweConsumer.decryptWith(decryptor, encryptionBlock);

            // Validate plain text
            String payload = output.getContentText();
            Assert.assertEquals(plainText, payload);
        });
    }

    private JweDecryptionProvider getJweDecryptionProvider(JsonWebKey key, KeyAlgorithm keyEncryptionAlgorithm,
        ContentAlgorithm contentEncryptionAlgorithm) {
        if (key.getAlgorithm() != null) {
            return JweUtils.createJweDecryptionProvider(key, contentEncryptionAlgorithm);
        }
        switch (key.getKeyType()) {
        case EC:
            return JweUtils.createJweDecryptionProvider(JwkUtils.toECPrivateKey(key), keyEncryptionAlgorithm,
                contentEncryptionAlgorithm);
        case RSA:
            return JweUtils.createJweDecryptionProvider(JwkUtils.toRSAPrivateKey(key), keyEncryptionAlgorithm,
                contentEncryptionAlgorithm);
        case OCTET:
            SecretKey secretKey = CryptoUtils.createSecretKeySpec(
                (String) key.getProperty(JsonWebKey.OCTET_KEY_VALUE), keyEncryptionAlgorithm.getJavaName());
            return JweUtils.createJweDecryptionProvider(secretKey, keyEncryptionAlgorithm,
                contentEncryptionAlgorithm);
        default:
            throw new IllegalArgumentException("JWK KeyType not supported: " + key.getKeyType());
        }
    }

}
