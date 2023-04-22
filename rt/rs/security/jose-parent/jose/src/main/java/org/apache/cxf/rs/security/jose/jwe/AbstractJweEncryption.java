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

import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.crypto.KeyProperties;

public abstract class AbstractJweEncryption implements JweEncryptionProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractJweEncryption.class);
    protected static final int DEFAULT_AUTH_TAG_LENGTH = 128;
    private final ContentEncryptionProvider contentEncryptionAlgo;
    private final KeyEncryptionProvider keyEncryptionAlgo;
    private final JsonMapObjectReaderWriter writer = new JsonMapObjectReaderWriter();

    protected AbstractJweEncryption(ContentEncryptionProvider contentEncryptionAlgo,
                                    KeyEncryptionProvider keyEncryptionAlgo) {
        this.keyEncryptionAlgo = keyEncryptionAlgo;
        this.contentEncryptionAlgo = contentEncryptionAlgo;
    }
    protected ContentEncryptionProvider getContentEncryptionAlgorithm() {
        return contentEncryptionAlgo;
    }
    protected AlgorithmParameterSpec getAlgorithmParameterSpec(byte[] theIv) {
        return getContentEncryptionAlgorithm().getAlgorithmParameterSpec(theIv);
    }

    protected byte[] getContentEncryptionKey(JweHeaders headers) {
        return getContentEncryptionAlgorithm().getContentEncryptionKey(headers);
    }

    protected byte[] getEncryptedContentEncryptionKey(JweHeaders headers, byte[] theCek) {
        return getKeyEncryptionAlgo().getEncryptedContentEncryptionKey(headers, theCek);
    }

    protected String getContentEncryptionAlgoJwt() {
        return getContentEncryptionAlgorithm().getAlgorithm().getJwaName();
    }
    protected String getContentEncryptionAlgoJava() {
        return getContentEncryptionAlgorithm().getAlgorithm().getJavaName();
    }
    protected byte[] getAAD(String protectedHeaders, byte[] aad) {
        return getContentEncryptionAlgorithm().getAdditionalAuthenticationData(protectedHeaders, aad);
    }
    @Override
    public String encrypt(byte[] content, JweHeaders jweHeaders) {
        JweEncryptionInternal state = getInternalState(jweHeaders, new JweEncryptionInput());

        byte[] encryptedContent = encryptInternal(state, content);
        byte[] cipher = getActualCipher(encryptedContent);
        byte[] authTag = getAuthenticationTag(state, encryptedContent);
        JweCompactBuilder producer = new JweCompactBuilder(state.protectedHeadersJson,
                                                             state.jweContentEncryptionKey,
                                                             state.theIv,
                                                             cipher,
                                                             authTag);
        return producer.getJweContent();
    }
    @Override
    public JweEncryptionOutput getEncryptionOutput(JweEncryptionInput jweInput) {
        JweEncryptionInternal state = getInternalState(jweInput.getJweHeaders(), jweInput);
        Cipher c = null;
        AuthenticationTagProducer authTagProducer = null;
        byte[] cipher = null;
        byte[] authTag = null;
        if (jweInput.isContentEncryptionRequired()) {
            if (jweInput.getContent() == null) {
                // Streaming
                c = CryptoUtils.initCipher(createCekSecretKey(state), state.keyProps,
                                                  Cipher.ENCRYPT_MODE);
                authTagProducer = getAuthenticationTagProducer(state);
            } else {
                byte[] encryptedContent = encryptInternal(state, jweInput.getContent());
                cipher = getActualCipher(encryptedContent);
                authTag = getAuthenticationTag(state, encryptedContent);
            }
        }
        // else only CEK is encrypted
        return new JweEncryptionOutput(c,
                                      state.theHeaders,
                                      state.jweContentEncryptionKey,
                                      state.theIv,
                                      authTagProducer,
                                      state.keyProps,
                                      cipher,
                                      authTag);
    }
    protected byte[] encryptInternal(JweEncryptionInternal state, byte[] content) {
        try {
            SecretKey createCekSecretKey = createCekSecretKey(state);
            byte[] encryptedBytes = CryptoUtils.encryptBytes(content, createCekSecretKey, state.keyProps);

            // Here we're finished with the SecretKey we created, so we can destroy it
            try {
                createCekSecretKey.destroy();
            } catch (DestroyFailedException e) {
                // ignore
            }
            return encryptedBytes;
        } catch (SecurityException ex) {
            LOG.fine(ex.getMessage());
            if (ex.getCause() instanceof NoSuchAlgorithmException) {
                LOG.warning("Unsupported algorithm: " + state.keyProps.getKeyAlgo());
                throw new JweException(JweException.Error.INVALID_CONTENT_ALGORITHM);
            }
            throw new JweException(JweException.Error.CONTENT_ENCRYPTION_FAILURE, ex);
        }
    }
    protected byte[] getActualCipher(byte[] cipher) {
        return Arrays.copyOf(cipher, cipher.length - DEFAULT_AUTH_TAG_LENGTH / 8);
    }
    protected byte[] getAuthenticationTag(JweEncryptionInternal state, byte[] cipher) {
        return Arrays.copyOfRange(cipher, cipher.length - DEFAULT_AUTH_TAG_LENGTH / 8, cipher.length);
    }
    @Override
    public KeyAlgorithm getKeyAlgorithm() {
        KeyAlgorithm keyAlgo = getKeyEncryptionAlgo().getAlgorithm();
        return keyAlgo != null ? keyAlgo : null;
    }
    @Override
    public ContentAlgorithm getContentAlgorithm() {
        return getContentEncryptionAlgorithm().getAlgorithm();
    }
    protected JsonMapObjectReaderWriter getJwtHeadersWriter() {
        return writer;
    }

    protected AuthenticationTagProducer getAuthenticationTagProducer(JweEncryptionInternal state) {
        return null;
    }
    protected SecretKey createCekSecretKey(JweEncryptionInternal state) {
        return CryptoUtils.createSecretKeySpec(getActualCek(state.secretKey, this.getContentEncryptionAlgoJwt()),
                                               state.keyProps.getKeyAlgo());
    }

    protected byte[] getActualCek(byte[] theCek, String algoJwt) {
        return theCek;
    }

    private JweEncryptionInternal getInternalState(JweHeaders jweInHeaders, JweEncryptionInput jweInput) {
        JweHeaders theHeaders = new JweHeaders();
        if (getKeyAlgorithm() != null) {
            theHeaders.setKeyEncryptionAlgorithm(getKeyAlgorithm());
        }
        theHeaders.setContentEncryptionAlgorithm(getContentEncryptionAlgorithm().getAlgorithm());

        final JweHeaders protectedHeaders;
        if (jweInHeaders != null) {
            if (jweInHeaders.getKeyEncryptionAlgorithm() != null
                && (getKeyAlgorithm() == null
                    || !getKeyAlgorithm().equals(jweInHeaders.getKeyEncryptionAlgorithm()))) {
                LOG.warning("Invalid key encryption algorithm");
                throw new JweException(JweException.Error.INVALID_KEY_ALGORITHM);
            }
            if (jweInHeaders.getContentEncryptionAlgorithm() != null
                && !getContentEncryptionAlgoJwt().equals(jweInHeaders.getContentEncryptionAlgorithm().getJwaName())) {
                LOG.warning("Invalid content encryption algorithm");
                throw new JweException(JweException.Error.INVALID_CONTENT_ALGORITHM);
            }
            theHeaders.asMap().putAll(jweInHeaders.asMap());
            protectedHeaders = jweInHeaders.getProtectedHeaders() != null
                ? jweInHeaders.getProtectedHeaders() : theHeaders;
        } else {
            protectedHeaders = theHeaders;
        }

        byte[] theCek = jweInput.getCek() != null
            ? jweInput.getCek() : getContentEncryptionKey(theHeaders);

        JweEncryptionInternal state = new JweEncryptionInternal();
        state.jweContentEncryptionKey = getEncryptedContentEncryptionKey(theHeaders, theCek);
        state.theHeaders = theHeaders;
        
        if (jweInput.isContentEncryptionRequired()) {
            String contentEncryptionAlgoJavaName = getContentEncryptionAlgoJava();
            KeyProperties keyProps = new KeyProperties(contentEncryptionAlgoJavaName);
            keyProps.setCompressionSupported(compressionRequired(theHeaders));

            byte[] theIv = jweInput.getIv() != null
                ? jweInput.getIv() : getContentEncryptionAlgorithm().getInitVector();
            AlgorithmParameterSpec specParams = getAlgorithmParameterSpec(theIv);
            keyProps.setAlgoSpec(specParams);

            String protectedHeadersJson = writer.toJson(protectedHeaders);

            byte[] additionalEncryptionParam = getAAD(protectedHeadersJson, jweInput.getAad());
            keyProps.setAdditionalData(additionalEncryptionParam);

            state.keyProps = keyProps;
            state.theIv = theIv;
            state.protectedHeadersJson = protectedHeadersJson;
            state.aad = jweInput.getAad();
            state.secretKey = theCek;
        }

        return state;
    }
    private boolean compressionRequired(JweHeaders theHeaders) {
        return JoseConstants.JWE_DEFLATE_ZIP_ALGORITHM.equals(theHeaders.getZipAlgorithm());
    }
    protected KeyEncryptionProvider getKeyEncryptionAlgo() {
        return keyEncryptionAlgo;
    }
    protected static class JweEncryptionInternal {
        JweHeaders theHeaders;
        byte[] jweContentEncryptionKey;
        byte[] theIv;
        KeyProperties keyProps;
        byte[] secretKey;
        String protectedHeadersJson;
        byte[] aad;
    }
}
