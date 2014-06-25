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
package org.apache.cxf.rs.security.oauth2.jwe;

import java.io.IOException;
import java.io.OutputStream;
import java.security.spec.AlgorithmParameterSpec;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersWriter;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.KeyProperties;

public abstract class AbstractJweEncryptor implements JweEncryptor {
    protected static final int DEFAULT_IV_SIZE = 96;
    protected static final int DEFAULT_AUTH_TAG_LENGTH = 128;
    private JweHeaders headers;
    private JwtHeadersWriter writer = new JwtTokenReaderWriter();
    private byte[] cek;
    private byte[] iv;
    private AtomicInteger providedIvUsageCount;
    private int authTagLen = DEFAULT_AUTH_TAG_LENGTH;
    
    protected AbstractJweEncryptor(SecretKey cek, byte[] iv) {
        this(new JweHeaders(Algorithm.toJwtName(cek.getAlgorithm(),
                                                cek.getEncoded().length * 8)),
                                                cek.getEncoded(), iv);
    }
    protected AbstractJweEncryptor(JweHeaders headers, byte[] cek, byte[] iv) {
        this.headers = headers;
        this.cek = cek;
        this.iv = iv;
        if (iv != null && iv.length > 0) {
            providedIvUsageCount = new AtomicInteger();
        }
    }
    protected AbstractJweEncryptor(JweHeaders headers, byte[] cek, byte[] iv, int authTagLen) {
        this(headers, cek, iv);
        this.authTagLen = authTagLen;
    }
    protected AbstractJweEncryptor(JweHeaders headers) {
        this.headers = headers;
    }
    protected AbstractJweEncryptor(JweHeaders headers, byte[] cek, byte[] iv, int authTagLen, 
                                   JwtHeadersWriter writer) {
        this(headers, cek, iv, authTagLen);
        if (writer != null) {
            this.writer = writer;
        }
    }
    
    protected AlgorithmParameterSpec getContentEncryptionCipherSpec(byte[] theIv) {
        return CryptoUtils.getContentEncryptionCipherSpec(getAuthTagLen(), theIv);
    }
    
    protected byte[] getContentEncryptionCipherInitVector() {
        if (iv == null) {
            return CryptoUtils.generateSecureRandomBytes(DEFAULT_IV_SIZE);
        } else if (iv.length > 0 && providedIvUsageCount.addAndGet(1) > 1) {
            throw new SecurityException();
        } else {
            return iv;
        }
    }
    
    protected byte[] getContentEncryptionKey() {
        return cek;
    }
    
    protected abstract byte[] getEncryptedContentEncryptionKey(byte[] theCek);
    
    protected String getContentEncryptionAlgoJwt() {
        return headers.getContentEncryptionAlgorithm();
    }
    protected String getContentEncryptionAlgoJava() {
        return Algorithm.toJavaName(getContentEncryptionAlgoJwt());
    }
    
    protected int getAuthTagLen() {
        return authTagLen;
    }
    protected JweHeaders getJweHeaders() {
        return headers;
    }
    public String encrypt(byte[] content, String contentType) {
        JweEncryptorInternalState state = getInternalState(contentType);
        
        byte[] cipherText = CryptoUtils.encryptBytes(
            content, 
            state.secretKey,
            state.keyProps);
        
        
        JweCompactProducer producer = new JweCompactProducer(state.theHeaders, 
                                             writer,                
                                             state.jweContentEncryptionKey,
                                             state.theIv,
                                             cipherText,
                                             getAuthTagLen());
        return producer.getJweContent();
    }
    
    @Override
    public JweOutputStream createJweStream(OutputStream os, String contentType) {
        JweEncryptorInternalState state = getInternalState(contentType);
        String jweStart = JweCompactProducer.startJweContent(state.theHeaders, 
                                           writer, 
                                           state.jweContentEncryptionKey, 
                                           state.theIv);
        Cipher c = CryptoUtils.initCipher(state.secretKey, state.keyProps, 
                                          Cipher.ENCRYPT_MODE);
        try {
            os.write(jweStart.getBytes("UTF-8"));
        } catch (IOException ex) {
            throw new SecurityException(ex);
        }
        return new JweOutputStream(os, c, getAuthTagLen());
    }
    
    private JweEncryptorInternalState getInternalState(String contentType) {
        JweHeaders theHeaders = headers;
        if (contentType != null) {
            theHeaders = new JweHeaders(theHeaders.asMap());
            theHeaders.setContentType(contentType);
        }
        
        byte[] theCek = getContentEncryptionKey();
        String contentEncryptionAlgoJavaName = Algorithm.toJavaName(theHeaders.getContentEncryptionAlgorithm());
        KeyProperties keyProps = new KeyProperties(contentEncryptionAlgoJavaName);
        byte[] additionalEncryptionParam = theHeaders.toCipherAdditionalAuthData(writer);
        keyProps.setAdditionalData(additionalEncryptionParam);
        
        byte[] theIv = getContentEncryptionCipherInitVector();
        AlgorithmParameterSpec specParams = getContentEncryptionCipherSpec(theIv);
        keyProps.setAlgoSpec(specParams);
        byte[] jweContentEncryptionKey = getEncryptedContentEncryptionKey(theCek);
        JweEncryptorInternalState state = new JweEncryptorInternalState();
        state.theHeaders = theHeaders;
        state.jweContentEncryptionKey = jweContentEncryptionKey;
        state.keyProps = keyProps;
        state.secretKey = CryptoUtils.createSecretKeySpec(theCek, 
                                        contentEncryptionAlgoJavaName);
        state.theIv = theIv;
        return state;
    }
    
    private static class JweEncryptorInternalState {
        JweHeaders theHeaders;
        byte[] jweContentEncryptionKey;
        byte[] theIv;
        KeyProperties keyProps;
        SecretKey secretKey;
    }
}
