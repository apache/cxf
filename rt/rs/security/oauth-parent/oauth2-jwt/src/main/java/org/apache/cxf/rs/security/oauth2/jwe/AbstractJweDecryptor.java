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

import java.security.spec.AlgorithmParameterSpec;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

public abstract class AbstractJweDecryptor implements JweDecryptor {
    private JweCryptoProperties props;
    protected AbstractJweDecryptor(JweCryptoProperties props) {
        this.props = props;
    }
    
    protected abstract byte[] getContentEncryptionKey(JweCompactConsumer consumer);
    
    public JweDecryptionOutput decrypt(String content) {
        JweCompactConsumer consumer = new JweCompactConsumer(content, props);
        return doDecrypt(consumer);
    }
    
    protected JweDecryptionOutput doDecrypt(JweCompactConsumer consumer) {
        CeProvider ceProvider = new CeProvider(consumer);
        byte[] bytes = consumer.getDecryptedContent(ceProvider);
        return new JweDecryptionOutput(consumer.getJweHeaders(), bytes);
    }
    protected byte[] getEncryptedContentEncryptionKey(JweCompactConsumer consumer) {
        return consumer.getEncryptedContentEncryptionKey();
    }
    protected AlgorithmParameterSpec getContentDecryptionCipherSpec(JweCompactConsumer consumer) {
        return CryptoUtils.getContentEncryptionCipherSpec(getEncryptionAuthenticationTagLenBits(consumer), 
                                                   getContentEncryptionCipherInitVector(consumer));
    }
    protected String getContentEncryptionAlgorithm(JweCompactConsumer consumer) {
        return Algorithm.toJavaName(consumer.getJweHeaders().getContentEncryptionAlgorithm());
    }
    protected byte[] getContentEncryptionCipherAAD(JweCompactConsumer consumer) {
        return consumer.getContentEncryptionCipherAAD();
    }
    protected byte[] getEncryptedContentWithAuthTag(JweCompactConsumer consumer) {
        return consumer.getEncryptedContentWithAuthTag();
    }
    protected byte[] getContentEncryptionCipherInitVector(JweCompactConsumer consumer) { 
        return consumer.getContentDecryptionCipherInitVector();
    }
    protected byte[] getEncryptionAuthenticationTag(JweCompactConsumer consumer) {
        return consumer.getEncryptionAuthenticationTag();
    }
    protected int getEncryptionAuthenticationTagLenBits(JweCompactConsumer consumer) {
        return getEncryptionAuthenticationTag(consumer).length * 8;
    }
    
    protected class CeProvider implements ContentEncryptionProvider {

        private JweCompactConsumer consumer;
        public CeProvider(JweCompactConsumer consumer) {
            this.consumer = consumer;
        }
        @Override
        public byte[] getContentEncryptionKey(JweHeaders headers, byte[] encryptedKey) {
            return AbstractJweDecryptor.this.getContentEncryptionKey(consumer);
        }

        @Override
        public AlgorithmParameterSpec getContentEncryptionCipherSpec(JweHeaders headers,
                                                                     int authTagLength,
                                                                     byte[] initVector) {
            return getContentDecryptionCipherSpec(consumer);
        }
        
    }
}
