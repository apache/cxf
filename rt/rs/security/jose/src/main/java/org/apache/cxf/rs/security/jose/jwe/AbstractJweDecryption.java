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

import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.common.util.crypto.KeyProperties;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

public abstract class AbstractJweDecryption implements JweDecryptionProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(JwsUtils.class);
    
    private KeyDecryptionAlgorithm keyDecryptionAlgo;
    private ContentDecryptionAlgorithm contentDecryptionAlgo;
    protected AbstractJweDecryption(KeyDecryptionAlgorithm keyDecryptionAlgo,
                                    ContentDecryptionAlgorithm contentDecryptionAlgo) {
        this.keyDecryptionAlgo = keyDecryptionAlgo;
        this.contentDecryptionAlgo = contentDecryptionAlgo;
    }
    
    protected byte[] getContentEncryptionKey(JweDecryptionInput jweDecryptionInput) {
        return keyDecryptionAlgo.getDecryptedContentEncryptionKey(jweDecryptionInput);
    }
    
    public JweDecryptionOutput decrypt(String content) {
        JweCompactConsumer consumer = new JweCompactConsumer(content);
        byte[] cek = getContentEncryptionKey(consumer.getJweDecryptionInput());
        return doDecrypt(consumer.getJweDecryptionInput(), cek);
    }
    public byte[] decrypt(JweDecryptionInput jweDecryptionInput) {
        byte[] cek = getContentEncryptionKey(jweDecryptionInput);
        return doDecrypt(jweDecryptionInput, cek).getContent();
    }
    protected JweDecryptionOutput doDecrypt(JweDecryptionInput jweDecryptionInput, byte[] cek) {
        KeyProperties keyProperties = new KeyProperties(getContentEncryptionAlgorithm(jweDecryptionInput));
        keyProperties.setAdditionalData(getContentEncryptionCipherAAD(jweDecryptionInput));
        AlgorithmParameterSpec spec = getContentEncryptionCipherSpec(jweDecryptionInput);
        keyProperties.setAlgoSpec(spec);
        boolean compressionSupported = 
            JoseConstants.DEFLATE_ZIP_ALGORITHM.equals(jweDecryptionInput.getJweHeaders().getZipAlgorithm());
        keyProperties.setCompressionSupported(compressionSupported);
        byte[] actualCek = getActualCek(cek, jweDecryptionInput.getJweHeaders().getContentEncryptionAlgorithm());
        Key secretKey = CryptoUtils.createSecretKeySpec(actualCek, keyProperties.getKeyAlgo());
        byte[] bytes = 
            CryptoUtils.decryptBytes(getEncryptedContentWithAuthTag(jweDecryptionInput), secretKey, keyProperties);
        return new JweDecryptionOutput(jweDecryptionInput.getJweHeaders(), bytes);
    }
    protected byte[] getEncryptedContentEncryptionKey(JweCompactConsumer consumer) {
        return consumer.getEncryptedContentEncryptionKey();
    }
    protected AlgorithmParameterSpec getContentEncryptionCipherSpec(JweDecryptionInput jweDecryptionInput) {
        return contentDecryptionAlgo.getAlgorithmParameterSpec(
            getContentEncryptionCipherInitVector(jweDecryptionInput));
    }
    protected String getContentEncryptionAlgorithm(JweDecryptionInput jweDecryptionInput) {
        return AlgorithmUtils.toJavaName(jweDecryptionInput.getJweHeaders().getContentEncryptionAlgorithm());
    }
    protected byte[] getContentEncryptionCipherAAD(JweDecryptionInput jweDecryptionInput) {
        return contentDecryptionAlgo.getAdditionalAuthenticationData(
            jweDecryptionInput.getDecodedJsonHeaders(), jweDecryptionInput.getAad());
    }
    protected byte[] getEncryptedContentWithAuthTag(JweDecryptionInput jweDecryptionInput) {
        return contentDecryptionAlgo.getEncryptedSequence(jweDecryptionInput.getJweHeaders(),
                                                          jweDecryptionInput.getEncryptedContent(), 
                                                          getEncryptionAuthenticationTag(jweDecryptionInput));
    }
    protected byte[] getContentEncryptionCipherInitVector(JweDecryptionInput jweDecryptionInput) { 
        return jweDecryptionInput.getInitVector();
    }
    protected byte[] getEncryptionAuthenticationTag(JweDecryptionInput jweDecryptionInput) {
        return jweDecryptionInput.getAuthTag();
    }
    protected int getEncryptionAuthenticationTagLenBits(JweDecryptionInput jweDecryptionInput) {
        return getEncryptionAuthenticationTag(jweDecryptionInput).length * 8;
    }
    protected byte[] getActualCek(byte[] theCek, String algoJwt) {
        return theCek;
    }
    @Override
    public KeyAlgorithm getKeyAlgorithm() {
        return keyDecryptionAlgo.getAlgorithm();
    }
    @Override
    public ContentAlgorithm getContentAlgorithm() {
        return contentDecryptionAlgo.getAlgorithm();
    }
}
