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
import java.util.logging.Logger;

import javax.crypto.SecretKey;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.rs.security.jose.common.JoseException;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public class AesGcmWrapKeyDecryptionAlgorithm extends WrappedKeyDecryptionAlgorithm {
    protected static final Logger LOG = LogUtils.getL7dLogger(AesGcmWrapKeyDecryptionAlgorithm.class);
    public AesGcmWrapKeyDecryptionAlgorithm(String encodedKey) {
        this(encodedKey, null);
    }
    public AesGcmWrapKeyDecryptionAlgorithm(String encodedKey, KeyAlgorithm supportedAlgo) {
        this(CryptoUtils.decodeSequence(encodedKey), supportedAlgo);
    }
    public AesGcmWrapKeyDecryptionAlgorithm(byte[] secretKey) {
        this(secretKey, KeyAlgorithm.A128GCMKW);
    }
    public AesGcmWrapKeyDecryptionAlgorithm(byte[] secretKey, KeyAlgorithm supportedAlgo) {
        this(CryptoUtils.createSecretKeySpec(secretKey, AlgorithmUtils.AES), supportedAlgo);
    }
    public AesGcmWrapKeyDecryptionAlgorithm(SecretKey secretKey) {
        this(secretKey, null);
    }
    public AesGcmWrapKeyDecryptionAlgorithm(SecretKey secretKey, KeyAlgorithm supportedAlgo) {
        super(secretKey, supportedAlgo);
    }
    @Override
    protected byte[] getEncryptedContentEncryptionKey(JweDecryptionInput jweDecryptionInput) {
        byte[] encryptedCekKey = super.getEncryptedContentEncryptionKey(jweDecryptionInput);
        byte[] tag = getDecodedBytes(jweDecryptionInput, "tag");
        return JweCompactConsumer.getCipherWithAuthTag(encryptedCekKey, tag);
    }
    protected AlgorithmParameterSpec getAlgorithmParameterSpec(JweDecryptionInput jweDecryptionInput) {
        byte[] iv = getDecodedBytes(jweDecryptionInput, "iv");
        return CryptoUtils.getContentEncryptionCipherSpec(128, iv);
    }
    private byte[] getDecodedBytes(JweDecryptionInput jweDecryptionInput, String headerName) {
        try {
            Object ivHeader = jweDecryptionInput.getJweHeaders().getHeader(headerName);
            return Base64UrlUtility.decode(ivHeader.toString());
        } catch (Exception ex) {
            throw new JoseException(ex);
        }
    }
    protected void validateKeyEncryptionAlgorithm(String keyAlgo) {
        super.validateKeyEncryptionAlgorithm(keyAlgo);
        if (!AlgorithmUtils.isAesGcmKeyWrap(keyAlgo)) {
            LOG.warning("Invalid key encryption algorithm");
            throw new JweException(JweException.Error.INVALID_KEY_ALGORITHM);
        }
    }
}
