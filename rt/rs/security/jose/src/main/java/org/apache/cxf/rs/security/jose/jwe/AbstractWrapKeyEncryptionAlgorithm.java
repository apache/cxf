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
import java.util.Set;

import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.common.util.crypto.KeyProperties;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;

public abstract class AbstractWrapKeyEncryptionAlgorithm implements KeyEncryptionProvider {
    private Key keyEncryptionKey;
    private boolean wrap;
    private KeyAlgorithm algorithm;
    private Set<String> supportedAlgorithms;
    protected AbstractWrapKeyEncryptionAlgorithm(Key key, Set<String> supportedAlgorithms) {
        this(key, null, true, supportedAlgorithms);
    }
    protected AbstractWrapKeyEncryptionAlgorithm(Key key, boolean wrap, Set<String> supportedAlgorithms) {
        this(key, null, wrap, supportedAlgorithms);
    }
    protected AbstractWrapKeyEncryptionAlgorithm(Key key, KeyAlgorithm jweAlgo, Set<String> supportedAlgorithms) {
        this(key, jweAlgo, true, supportedAlgorithms);
    }
    protected AbstractWrapKeyEncryptionAlgorithm(Key key, KeyAlgorithm jweAlgo, boolean wrap, 
                                                 Set<String> supportedAlgorithms) {
        this.keyEncryptionKey = key;
        this.algorithm = jweAlgo;
        this.wrap = wrap;
        this.supportedAlgorithms = supportedAlgorithms;
    }
    @Override
    public KeyAlgorithm getAlgorithm() {
        return algorithm;
    }
    @Override
    public byte[] getEncryptedContentEncryptionKey(JweHeaders headers, byte[] cek) {
        checkAlgorithms(headers);
        KeyProperties secretKeyProperties = new KeyProperties(getKeyEncryptionAlgoJava(headers));
        AlgorithmParameterSpec spec = getAlgorithmParameterSpec(headers); 
        if (spec != null) {
            secretKeyProperties.setAlgoSpec(spec);
        }
        if (!wrap) {
            return CryptoUtils.encryptBytes(cek, keyEncryptionKey, secretKeyProperties);
        } else {
            return CryptoUtils.wrapSecretKey(cek, 
                                             getContentEncryptionAlgoJava(headers),
                                             keyEncryptionKey, 
                                             secretKeyProperties);
        }
    }
    protected String getKeyEncryptionAlgoJava(JweHeaders headers) {
        return AlgorithmUtils.toJavaName(headers.getKeyEncryptionAlgorithm());
    }
    protected String getContentEncryptionAlgoJava(JweHeaders headers) {
        return AlgorithmUtils.toJavaName(headers.getContentEncryptionAlgorithm());
    }
    protected AlgorithmParameterSpec getAlgorithmParameterSpec(JweHeaders headers) {
        return null;
    }
    protected String checkAlgorithm(String algo) {
        if (algo != null && !supportedAlgorithms.contains(algo)) {
            throw new SecurityException();
        }
        return algo;
    }
    protected void checkAlgorithms(JweHeaders headers) {
        String providedAlgo = headers.getKeyEncryptionAlgorithm();
        if ((providedAlgo == null && algorithm == null)
            || (providedAlgo != null && algorithm != null && !providedAlgo.equals(algorithm.getJwaName()))) {
            throw new SecurityException();
        }
        if (providedAlgo != null) {
            checkAlgorithm(providedAlgo);
        } else if (algorithm != null) {
            headers.setKeyEncryptionAlgorithm(algorithm.getJwaName());
            checkAlgorithm(algorithm.getJwaName());
        }
    }
    
}
