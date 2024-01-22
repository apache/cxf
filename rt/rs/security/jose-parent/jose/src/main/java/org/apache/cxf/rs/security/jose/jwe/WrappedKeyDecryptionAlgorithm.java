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
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.crypto.KeyProperties;

public class WrappedKeyDecryptionAlgorithm implements KeyDecryptionProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(WrappedKeyDecryptionAlgorithm.class);
    private final Key cekDecryptionKey;
    private final boolean unwrap;
    private final KeyAlgorithm supportedAlgo;

    public WrappedKeyDecryptionAlgorithm(Key cekDecryptionKey, KeyAlgorithm supportedAlgo) {
        this(cekDecryptionKey, supportedAlgo, true);
    }
    public WrappedKeyDecryptionAlgorithm(Key cekDecryptionKey, KeyAlgorithm supportedAlgo, boolean unwrap) {
        this.cekDecryptionKey = cekDecryptionKey;
        this.supportedAlgo = supportedAlgo;
        this.unwrap = unwrap;
    }
    public byte[] getDecryptedContentEncryptionKey(JweDecryptionInput jweDecryptionInput) {
        KeyProperties keyProps = new KeyProperties(getKeyEncryptionAlgorithm(jweDecryptionInput));
        AlgorithmParameterSpec spec = getAlgorithmParameterSpec(jweDecryptionInput);
        if (spec != null) {
            keyProps.setAlgoSpec(spec);
        }
        if (!unwrap) {
            keyProps.setBlockSize(getKeyCipherBlockSize());
            return CryptoUtils.decryptBytes(getEncryptedContentEncryptionKey(jweDecryptionInput),
                                            getCekDecryptionKey(), keyProps);
        }
        return CryptoUtils.unwrapSecretKey(getEncryptedContentEncryptionKey(jweDecryptionInput),
                                           getKeyEncryptionAlgorithm(jweDecryptionInput),
                                           getCekDecryptionKey(),
                                           keyProps).getEncoded();
    }

    protected Key getCekDecryptionKey() {
        return cekDecryptionKey;
    }
    protected int getKeyCipherBlockSize() {
        return -1;
    }
    protected String getKeyEncryptionAlgorithm(JweDecryptionInput jweDecryptionInput) {
        String keyAlgo = jweDecryptionInput.getJweHeaders().getKeyEncryptionAlgorithm().getJwaName();
        validateKeyEncryptionAlgorithm(keyAlgo);
        return AlgorithmUtils.toJavaName(keyAlgo);
    }
    protected void validateKeyEncryptionAlgorithm(String keyAlgo) {
        if (keyAlgo == null
            || !supportedAlgo.getJwaName().equals(keyAlgo)) {
            reportInvalidKeyAlgorithm(keyAlgo);
        }
    }
    protected void reportInvalidKeyAlgorithm(String keyAlgo) {
        LOG.warning("Invalid key encryption algorithm: " + keyAlgo);
        throw new JweException(JweException.Error.INVALID_KEY_ALGORITHM);
    }
    protected String getContentEncryptionAlgorithm(JweDecryptionInput jweDecryptionInput) {
        return AlgorithmUtils.toJavaName(
            jweDecryptionInput.getJweHeaders().getContentEncryptionAlgorithm().getJwaName());
    }
    protected AlgorithmParameterSpec getAlgorithmParameterSpec(JweDecryptionInput jweDecryptionInput) {
        return null;
    }
    protected byte[] getEncryptedContentEncryptionKey(JweDecryptionInput jweDecryptionInput) {
        return jweDecryptionInput.getEncryptedCEK();
    }
    @Override
    public KeyAlgorithm getAlgorithm() {
        return supportedAlgo;
    }
}
