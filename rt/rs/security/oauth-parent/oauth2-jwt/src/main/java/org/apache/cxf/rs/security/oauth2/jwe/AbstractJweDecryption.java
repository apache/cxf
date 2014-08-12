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
import org.apache.cxf.rs.security.oauth2.jwt.JwtConstants;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersReader;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.KeyProperties;

public abstract class AbstractJweDecryption implements JweDecryptionProvider {
    private JweCryptoProperties props;
    private KeyDecryptionAlgorithm keyDecryptionAlgo;
    private ContentDecryptionAlgorithm contentDecryptionAlgo;
    private JwtHeadersReader reader = new JwtTokenReaderWriter();
    protected AbstractJweDecryption(JweCryptoProperties props, 
                                    JwtHeadersReader theReader,
                                    KeyDecryptionAlgorithm keyDecryptionAlgo,
                                    ContentDecryptionAlgorithm contentDecryptionAlgo) {
        this.props = props;
        if (theReader != null) {
            reader = theReader;
        }
        this.keyDecryptionAlgo = keyDecryptionAlgo;
        this.contentDecryptionAlgo = contentDecryptionAlgo;
    }
    
    protected byte[] getContentEncryptionKey(JweCompactConsumer consumer) {
        return this.keyDecryptionAlgo.getDecryptedContentEncryptionKey(consumer);
    }
    
    public JweDecryptionOutput decrypt(String content) {
        JweCompactConsumer consumer = new JweCompactConsumer(content, reader);
        return doDecrypt(consumer);
    }
    public byte[] decrypt(JweCompactConsumer consumer) {
        return doDecrypt(consumer).getContent();
    }
    
    protected JweDecryptionOutput doDecrypt(JweCompactConsumer consumer) {
        consumer.enforceJweCryptoProperties(props);
        byte[] cek = getContentEncryptionKey(consumer);
        return doDecrypt(consumer, cek);
    }
    protected JweDecryptionOutput doDecrypt(JweCompactConsumer consumer, byte[] cek) {
        KeyProperties keyProperties = new KeyProperties(getContentEncryptionAlgorithm(consumer));
        keyProperties.setAdditionalData(getContentEncryptionCipherAAD(consumer));
        AlgorithmParameterSpec spec = getContentEncryptionCipherSpec(consumer);
        keyProperties.setAlgoSpec(spec);
        boolean compressionSupported = 
            JwtConstants.DEFLATE_ZIP_ALGORITHM.equals(consumer.getJweHeaders().getZipAlgorithm());
        keyProperties.setCompressionSupported(compressionSupported);
        byte[] actualCek = getActualCek(cek, consumer.getJweHeaders().getContentEncryptionAlgorithm());
        Key secretKey = CryptoUtils.createSecretKeySpec(actualCek, keyProperties.getKeyAlgo());
        byte[] bytes = 
            CryptoUtils.decryptBytes(getEncryptedContentWithAuthTag(consumer), secretKey, keyProperties);
        return new JweDecryptionOutput(consumer.getJweHeaders(), bytes);
    }
    protected byte[] getEncryptedContentEncryptionKey(JweCompactConsumer consumer) {
        return consumer.getEncryptedContentEncryptionKey();
    }
    protected AlgorithmParameterSpec getContentEncryptionCipherSpec(JweCompactConsumer consumer) {
        return contentDecryptionAlgo.getAlgorithmParameterSpec(getContentEncryptionCipherInitVector(consumer));
    }
    protected String getContentEncryptionAlgorithm(JweCompactConsumer consumer) {
        return Algorithm.toJavaName(consumer.getJweHeaders().getContentEncryptionAlgorithm());
    }
    protected byte[] getContentEncryptionCipherAAD(JweCompactConsumer consumer) {
        return contentDecryptionAlgo.getAdditionalAuthenticationData(consumer.getDecodedJsonHeaders());
    }
    protected byte[] getEncryptedContentWithAuthTag(JweCompactConsumer consumer) {
        return contentDecryptionAlgo.getEncryptedSequence(consumer.getEncryptedContent(), 
                                                          getEncryptionAuthenticationTag(consumer));
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
    protected byte[] getActualCek(byte[] theCek, String algoJwt) {
        return theCek;
    }
    
}
