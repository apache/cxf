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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweCompactConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweCompactProducer;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweJsonConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweJsonProducer;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.KeyType;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJsonProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

import org.junit.Assert;

public class ApacheCXFProducer {

    public void produceJWS(String keyType, String signatureAlgorithm, Serialization serialization, String plainText,
        String jwksJson) {
        JsonWebKeys keys = JwkUtils.readJwkSet(jwksJson);
        JsonWebKey key = getRequestedKeyType(keyType, keys).orElseThrow(IllegalArgumentException::new);

        // Sign
        JwsHeaders jwsHeaders = new JwsHeaders();
        jwsHeaders.setKeyId(key.getKeyId());
        jwsHeaders.setAlgorithm(signatureAlgorithm);
        switch (serialization) {
        case COMPACT:
            produceCompactJWS(plainText, key, jwsHeaders);
            break;
        case FLATTENED:
            produceJsonJWS(plainText, key, jwsHeaders, true);
            break;
        case JSON:
            produceJsonJWS(plainText, key, jwsHeaders, false);
            break;
        default:
            throw new IllegalArgumentException("Serialization not supported: " + serialization);
        }

    }

    private void produceCompactJWS(String plainText, JsonWebKey key, JwsHeaders jwsHeaders) {
        JwsCompactProducer jwsProducer = new JwsCompactProducer(jwsHeaders, plainText);
        jwsProducer.signWith(key);
    }

    private void produceJsonJWS(String plainText, JsonWebKey key, JwsHeaders jwsHeaders, boolean flattened) {
        JwsJsonProducer jwsProducer = new JwsJsonProducer(plainText, flattened);
        JwsSignatureProvider jwsSignatureProvider = JwsUtils.getSignatureProvider(key,
            jwsHeaders.getSignatureAlgorithm());
        jwsProducer.signWith(jwsSignatureProvider, null, jwsHeaders);
    }
    
    public void produceJWE(String keyType, String keyEncryptionAlgorithm, String contentEncryptionAlgorithm,
        Serialization serialization, String plainText, String jwksJson) {
        JsonWebKeys keys = JwkUtils.readJwkSet(jwksJson);
        JsonWebKey key = getRequestedKeyType(keyType, keys).orElseThrow(IllegalArgumentException::new);

        // Encrypt
        switch (serialization) {
        case COMPACT:
            JweHeaders headers = new JweHeaders();
            headers.setKeyId(key.getKeyId());
            headers.setKeyEncryptionAlgorithm(KeyAlgorithm.getAlgorithm(keyEncryptionAlgorithm));
            headers.setContentEncryptionAlgorithm(ContentAlgorithm.getAlgorithm(contentEncryptionAlgorithm));
            produceCompactJWE(plainText, key, headers);
            break;
        case FLATTENED:
            produceJsonJWE(keyEncryptionAlgorithm, contentEncryptionAlgorithm, plainText, key, true);
            break;
        case JSON:
            produceJsonJWE(keyEncryptionAlgorithm, contentEncryptionAlgorithm, plainText, key, false);
            break;
        default: 
            throw new IllegalArgumentException("Serialization not supported: " + serialization);
        }

    }

    private void produceJsonJWE(String keyEncryptionAlgorithm, String contentEncryptionAlgorithm, String plainText,
        JsonWebKey key, boolean flattened) {
        JweHeaders protectedHeaders = new JweHeaders();
        protectedHeaders.setKeyEncryptionAlgorithm(KeyAlgorithm.getAlgorithm(keyEncryptionAlgorithm));
        protectedHeaders
            .setContentEncryptionAlgorithm(ContentAlgorithm.getAlgorithm(contentEncryptionAlgorithm));
        JweHeaders recipientHeaders = new JweHeaders(key.getKeyId());
        produceJsonJWE(plainText, key, protectedHeaders, null, recipientHeaders, flattened);
    }

    private void produceCompactJWE(String plainText, JsonWebKey key, JweHeaders headers) {
        JweCompactProducer jweProducer = new JweCompactProducer(headers, plainText);
        JweEncryptionProvider jweEncryptionProvider = JweUtils.createJweEncryptionProvider(key, headers);
        String encryptedData = jweProducer.encryptWith(jweEncryptionProvider);
        JweCompactConsumer validator = new JweCompactConsumer(encryptedData);
        Assert.assertEquals(headers.getKeyEncryptionAlgorithm(), validator.getJweHeaders().getKeyEncryptionAlgorithm());
        Assert.assertEquals(headers.getContentEncryptionAlgorithm(),
            validator.getJweHeaders().getContentEncryptionAlgorithm());
        Assert.assertEquals(headers.getKeyId(), validator.getJweHeaders().getKeyId());
    }

    private void produceJsonJWE(String plainText, JsonWebKey key, JweHeaders protectedHeaders,
        JweHeaders unprotectedJweHeaders, JweHeaders recipientHeaders, boolean flattened) {
        JweJsonProducer jweProducer = new JweJsonProducer(protectedHeaders, unprotectedJweHeaders,
            plainText.getBytes(StandardCharsets.UTF_8), null, flattened);
        Map<String, Object> union = new HashMap<>();
        if (protectedHeaders != null) {
            union.putAll(protectedHeaders.asMap());
        }
        if (unprotectedJweHeaders != null) {
            union.putAll(unprotectedJweHeaders.asMap());
        }
        JweHeaders unionHeaders = new JweHeaders(union);
        JweEncryptionProvider jweEncryptionProvider = JweUtils.createJweEncryptionProvider(key, unionHeaders);
        String encryptedData = jweProducer.encryptWith(jweEncryptionProvider, recipientHeaders);
        JweJsonConsumer validator = new JweJsonConsumer(encryptedData);
        Assert.assertEquals(protectedHeaders.getKeyEncryptionAlgorithm(),
            validator.getProtectedHeader().getKeyEncryptionAlgorithm());
        Assert.assertEquals(protectedHeaders.getContentEncryptionAlgorithm(),
            validator.getProtectedHeader().getContentEncryptionAlgorithm());
        Assert.assertEquals(1, validator.getRecipients().size());
        Assert.assertEquals(recipientHeaders.getKeyId(),
            validator.getRecipients().get(0).getUnprotectedHeader().getKeyId());
    }

    protected Optional<JsonWebKey> getRequestedKeyType(String keyType, JsonWebKeys keys) {
        KeyType kty = KeyType.valueOf(keyType);
        switch (kty) {
        case EC:
            return keys.getEllipticKeys().stream().findFirst();
        case RSA:
            return keys.getRsaKeys().stream().findFirst();
        case OCTET:
            return keys.getSecretKeys().stream().findFirst();
        default:
            throw new IllegalArgumentException("KeyType not supported: " + kty);
        }
    }

}
