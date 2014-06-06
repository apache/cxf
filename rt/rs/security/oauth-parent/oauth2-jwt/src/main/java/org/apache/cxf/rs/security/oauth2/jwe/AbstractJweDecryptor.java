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
    private JweCompactConsumer jweConsumer;
    private CeProvider ceProvider = new CeProvider();
    private JweCryptoProperties props;
    protected AbstractJweDecryptor(JweCryptoProperties props) {
        this.props = props;
    }
    
    protected abstract byte[] getContentEncryptionKey();
    
    public JweDecryptionOutput decrypt(String content) {
        byte[] bytes = getJweConsumer(content).getDecryptedContent(ceProvider);
        return new JweDecryptionOutput(getHeaders(), bytes);
    }
    private JweCompactConsumer getJweConsumer(String jweContent) {
        if (jweConsumer == null) {
            this.jweConsumer = new JweCompactConsumer(jweContent, props);
        }
        return jweConsumer;
    }
    
    protected JweHeaders getHeaders() {
        return getJweConsumer().getJweHeaders();
    }
    
    protected AlgorithmParameterSpec getContentDecryptionCipherSpec() {
        return CryptoUtils.getContentEncryptionCipherSpec(getEncryptionAuthenticationTagLenBits(), 
                                                   getContentEncryptionCipherInitVector());
    }
    protected byte[] getEncryptedContentEncryptionKey() {
        return getJweConsumer().getEncryptedContentEncryptionKey();
    }
    protected String getContentEncryptionAlgorithm() {
        return Algorithm.toJavaName(getHeaders().getContentEncryptionAlgorithm());
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
            return AbstractJweDecryptor.this.getContentEncryptionKey();
        }

        @Override
        public AlgorithmParameterSpec getContentEncryptionCipherSpec(JweHeaders headers,
                                                                     int authTagLength,
                                                                     byte[] initVector) {
            return getContentDecryptionCipherSpec();
        }
        
    }
}
