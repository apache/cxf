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

import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.common.util.crypto.KeyProperties;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;

public class WrappedKeyDecryptionAlgorithm implements KeyDecryptionAlgorithm {
    private Key cekDecryptionKey;
    private boolean unwrap;
    private String supportedAlgo;
    public WrappedKeyDecryptionAlgorithm(Key cekDecryptionKey, String supportedAlgo) {    
        this(cekDecryptionKey, supportedAlgo, true);
    }
    public WrappedKeyDecryptionAlgorithm(Key cekDecryptionKey, String supportedAlgo, boolean unwrap) {    
        this.cekDecryptionKey = cekDecryptionKey;
        this.supportedAlgo = supportedAlgo;
        this.unwrap = unwrap;
    }
    public byte[] getDecryptedContentEncryptionKey(JweCompactConsumer consumer) {
        KeyProperties keyProps = new KeyProperties(getKeyEncryptionAlgorithm(consumer));
        AlgorithmParameterSpec spec = getAlgorithmParameterSpec(consumer); 
        if (spec != null) {
            keyProps.setAlgoSpec(spec);
        }
        if (!unwrap) {
            keyProps.setBlockSize(getKeyCipherBlockSize());
            return CryptoUtils.decryptBytes(getEncryptedContentEncryptionKey(consumer), 
                                            getCekDecryptionKey(), keyProps);
        } else {
            return CryptoUtils.unwrapSecretKey(getEncryptedContentEncryptionKey(consumer), 
                                               getContentEncryptionAlgorithm(consumer), 
                                               getCekDecryptionKey(), 
                                               keyProps).getEncoded();
        }
    }
    
    protected Key getCekDecryptionKey() {
        return cekDecryptionKey;
    }
    protected int getKeyCipherBlockSize() {
        return -1;
    }
    protected String getKeyEncryptionAlgorithm(JweCompactConsumer consumer) {
        String keyAlgo = consumer.getJweHeaders().getKeyEncryptionAlgorithm();
        validateKeyEncryptionAlgorithm(keyAlgo);
        return Algorithm.toJavaName(keyAlgo);
    }
    protected void validateKeyEncryptionAlgorithm(String keyAlgo) {
        if (keyAlgo == null || supportedAlgo != null && !supportedAlgo.equals(keyAlgo)) {
            throw new SecurityException();
        }
    }
    protected String getContentEncryptionAlgorithm(JweCompactConsumer consumer) {
        return Algorithm.toJavaName(consumer.getJweHeaders().getContentEncryptionAlgorithm());
    }
    protected AlgorithmParameterSpec getAlgorithmParameterSpec(JweCompactConsumer consumer) {
        return null;
    }
    protected byte[] getEncryptedContentEncryptionKey(JweCompactConsumer consumer) {
        return consumer.getEncryptedContentEncryptionKey();
    }
    @Override
    public String getAlgorithm() {
        return supportedAlgo;
    }
}
