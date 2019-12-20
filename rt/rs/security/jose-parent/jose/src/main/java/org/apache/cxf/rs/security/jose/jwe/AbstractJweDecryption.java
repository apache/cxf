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

import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.crypto.KeyProperties;

public abstract class AbstractJweDecryption implements JweDecryptionProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(JwsUtils.class);

    private final KeyDecryptionProvider keyDecryptionAlgo;
    private final ContentDecryptionProvider contentDecryptionAlgo;

    protected AbstractJweDecryption(KeyDecryptionProvider keyDecryptionAlgo,
                                    ContentDecryptionProvider contentDecryptionAlgo) {
        this.keyDecryptionAlgo = keyDecryptionAlgo;
        this.contentDecryptionAlgo = contentDecryptionAlgo;
    }

    public JweDecryptionOutput decrypt(String content) {
        JweCompactConsumer consumer = new JweCompactConsumer(content);
        byte[] cek = keyDecryptionAlgo.getDecryptedContentEncryptionKey(consumer.getJweDecryptionInput());
        return doDecrypt(consumer.getJweDecryptionInput(), cek);
    }

    public byte[] decrypt(JweDecryptionInput jweDecryptionInput) {
        byte[] cek = keyDecryptionAlgo.getDecryptedContentEncryptionKey(jweDecryptionInput);
        return doDecrypt(jweDecryptionInput, cek).getContent();
    }

    protected JweDecryptionOutput doDecrypt(JweDecryptionInput jweDecryptionInput, byte[] cek) {
        KeyProperties keyProperties = new KeyProperties(getContentEncryptionAlgorithm(jweDecryptionInput));
        keyProperties.setAdditionalData(getContentEncryptionCipherAAD(jweDecryptionInput));
        AlgorithmParameterSpec spec = getContentEncryptionCipherSpec(jweDecryptionInput);
        keyProperties.setAlgoSpec(spec);
        boolean compressionSupported =
            JoseConstants.JWE_DEFLATE_ZIP_ALGORITHM.equals(jweDecryptionInput.getJweHeaders().getZipAlgorithm());
        keyProperties.setCompressionSupported(compressionSupported);
        byte[] actualCek = getActualCek(cek,
                               jweDecryptionInput.getJweHeaders().getContentEncryptionAlgorithm().getJwaName());
        SecretKey secretKey = CryptoUtils.createSecretKeySpec(actualCek, keyProperties.getKeyAlgo());
        byte[] bytes =
            CryptoUtils.decryptBytes(getEncryptedContentWithAuthTag(jweDecryptionInput), secretKey, keyProperties);

        // Here we're finished with the SecretKey we created, so we can destroy it
        try {
            secretKey.destroy();
        } catch (DestroyFailedException e) {
            // ignore
        }
        Arrays.fill(cek, (byte) 0);
        if (actualCek != cek) {
            Arrays.fill(actualCek, (byte) 0);
        }

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
        return AlgorithmUtils.toJavaName(jweDecryptionInput.getJweHeaders()
                                         .getContentEncryptionAlgorithm().getJwaName());
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
