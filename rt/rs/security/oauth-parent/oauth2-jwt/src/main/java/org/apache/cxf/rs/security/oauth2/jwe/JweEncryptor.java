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

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithms;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersWriter;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.KeyProperties;

public class JweEncryptor {
    private Key cekEncryptionKey;
    private JweHeaders headers;
    private JwtHeadersWriter writer = new JwtTokenReaderWriter();
    private byte[] cek;
    private byte[] iv;
    private int authTagLen;
    private boolean wrap;
    
    public JweEncryptor(JweHeaders headers, byte[] iv, int authTagLen) {
        this.headers = headers;
        this.iv = iv;
        this.authTagLen = authTagLen;
    }
    public JweEncryptor(JweHeaders headers, Key cekEncryptionKey, byte[] cek, byte[] iv, 
                                   int authTagLen, boolean wrap) {
        this(headers, iv, authTagLen);
        this.cekEncryptionKey = cekEncryptionKey;
        this.cek = cek;
        this.wrap = wrap;
    }
    public JweEncryptor(JweHeaders headers, Key cekEncryptionKey, byte[] cek, byte[] iv, int authTagLen, 
                                   boolean wrap, JwtHeadersWriter writer) {
        this(headers, cekEncryptionKey, cek, iv, authTagLen, wrap);
        if (writer != null) {
            this.writer = writer;
        }
    }
    
    protected AlgorithmParameterSpec getContentEncryptionCipherSpec() {
        return CryptoUtils.getContentEncryptionCipherSpec(getAuthTagLen(), getIv());
    }
    
    protected byte[] getIv() {
        return iv;
    }
    
    protected byte[] cek() {
        return cek;
    }
    
    protected byte[] getEncryptedContentEncryptionKey() {
        if (cekEncryptionKey == null) {
            // direct key encryption
            return new byte[]{};
        } else  {
            KeyProperties secretKeyProperties = new KeyProperties(getContentEncryptionKeyEncryptionAlgo());
            if (!wrap) {
                return CryptoUtils.encryptBytes(cek, cekEncryptionKey, secretKeyProperties);
            } else {
                return CryptoUtils.wrapSecretKey(cek, getContentEncryptionAlgo(), cekEncryptionKey, 
                                                 secretKeyProperties.getKeyAlgo());
            }
        }
    }
    
    protected String getContentEncryptionKeyEncryptionAlgo() {
        return Algorithms.toJavaName(headers.getKeyEncryptionAlgorithm());
    }
    protected String getContentEncryptionAlgo() {
        return Algorithms.toJavaName(headers.getContentEncryptionAlgorithm());
    }
    
    protected int getAuthTagLen() {
        return authTagLen;
    }
    
    public String getJweContent(byte[] content) {
        byte[] jweContentEncryptionKey = getEncryptedContentEncryptionKey();
        
        String contentEncryptionAlgoJavaName = Algorithms.toJavaName(headers.getContentEncryptionAlgorithm());
        KeyProperties keyProps = new KeyProperties(contentEncryptionAlgoJavaName);
        byte[] additionalEncryptionParam = headers.toCipherAdditionalAuthData(writer);
        keyProps.setAdditionalData(additionalEncryptionParam);
        AlgorithmParameterSpec specParams = getContentEncryptionCipherSpec();
        keyProps.setAlgoSpec(specParams);
        
        byte[] cipherText = CryptoUtils.encryptBytes(
            content, 
            CryptoUtils.createSecretKeySpec(cek, contentEncryptionAlgoJavaName),
            keyProps);
        
        JweCompactProducer producer = new JweCompactProducer(headers, 
                                             jweContentEncryptionKey,
                                             iv,
                                             cipherText,
                                             getAuthTagLen());
        return producer.getJweContent();
    }
    
    public String getJweContent(String text) {
        try {
            return getJweContent(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new SecurityException(ex);
        }
    }
    
    
}
