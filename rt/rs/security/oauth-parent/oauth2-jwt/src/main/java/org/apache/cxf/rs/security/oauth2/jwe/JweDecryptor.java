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

import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.KeyProperties;

public class JweDecryptor {
    private JweCompactConsumer jweConsumer;
    private Key decryptionKey;
    private boolean unwrap;
    private CeProvider ceProvider = new CeProvider();
    public JweDecryptor(String jweContent, Key decryptionKey, boolean unwrap) {    
        this.jweConsumer = new JweCompactConsumer(jweContent);
        this.decryptionKey = decryptionKey;
        this.unwrap = unwrap;
    }
    
    protected Key getDecryptionKey() {
        return decryptionKey;
    }
    
    protected byte[] getDecryptedContentEncryptionKey() {
        // This can be overridden if needed
        KeyProperties keyProps = new KeyProperties(getKeyEncryptionAlgorithm());
        if (!unwrap) {
            keyProps.setBlockSize(getKeyCipherBlockSize());
            return CryptoUtils.decryptBytes(getEncryptedContentEncryptionKey(), decryptionKey, keyProps);
        } else {
            return CryptoUtils.unwrapSecretKey(getEncryptedContentEncryptionKey(), 
                                               getContentEncryptionAlgorithm(), 
                                               decryptionKey, 
                                               keyProps).getEncoded();
        }
    }
    protected int getKeyCipherBlockSize() {
        return -1;
    }
    public byte[] getDecryptedContent() {
        
        return jweConsumer.getDecryptedContent(ceProvider);
        
    }
    public String getDecryptedContentText() {
        return jweConsumer.getDecryptedContentText(ceProvider);
    }
    public JweHeaders getJweHeaders() {
        return getJweConsumer().getJweHeaders();
    }
    
    protected AlgorithmParameterSpec getContentDecryptionCipherSpec() {
        // this can be overridden if needed
        return CryptoUtils.getContentEncryptionCipherSpec(getEncryptionAuthenticationTagLenBits(), 
                                                   getContentEncryptionCipherInitVector());
    }
    protected String getKeyEncryptionAlgorithm() {
        return Algorithm.toJavaName(getJweHeaders().getKeyEncryptionAlgorithm());
    }
    protected String getContentEncryptionAlgorithm() {
        return Algorithm.toJavaName(getJweHeaders().getContentEncryptionAlgorithm());
    }
    protected byte[] getEncryptedContentEncryptionKey() {
        return getJweConsumer().getEncryptedContentEncryptionKey();
    }
    protected byte[] getContentEncryptionCipherAAD() {
        return getJweConsumer().getContentEncryptionCipherAAD();
    }
    protected byte[] getEncryptedContentWithAuthTag() {
        return getJweConsumer().getEncryptedContentWithAuthTag();
    }
    protected byte[] getContentEncryptionCipherInitVector() { 
        return getJweConsumer().getContentDecryptionCipherInitVector();
    }
    protected byte[] getEncryptionAuthenticationTag() {
        return getJweConsumer().getEncryptionAuthenticationTag();
    }
    protected int getEncryptionAuthenticationTagLenBits() {
        return getEncryptionAuthenticationTag().length * 8;
    }
    protected JweCompactConsumer getJweConsumer() { 
        return jweConsumer;
    }
    
    private class CeProvider implements ContentEncryptionProvider {

        @Override
        public byte[] getContentEncryptionKey(JweHeaders headers, byte[] encryptedKey) {
            return getDecryptedContentEncryptionKey();
        }

        @Override
        public AlgorithmParameterSpec getContentEncryptionCipherSpec(JweHeaders headers,
                                                                     int authTagLength,
                                                                     byte[] initVector) {
            return getContentDecryptionCipherSpec();
        }
        
    }
}
