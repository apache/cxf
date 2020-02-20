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
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.crypto.KeyProperties;

public abstract class AbstractWrapKeyEncryptionAlgorithm implements KeyEncryptionProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractWrapKeyEncryptionAlgorithm.class);
    private final Key keyEncryptionKey;
    private final boolean wrap;
    private final KeyAlgorithm algorithm;
    private final Set<String> supportedAlgorithms;

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
        }
        return CryptoUtils.wrapSecretKey(cek,
                                         getContentEncryptionAlgoJava(headers),
                                         keyEncryptionKey,
                                         secretKeyProperties);
    }
    protected String getKeyEncryptionAlgoJava(JweHeaders headers) {
        return AlgorithmUtils.toJavaName(headers.getKeyEncryptionAlgorithm().getJwaName());
    }
    protected String getContentEncryptionAlgoJava(JweHeaders headers) {
        return AlgorithmUtils.toJavaName(headers.getContentEncryptionAlgorithm().getJwaName());
    }
    protected AlgorithmParameterSpec getAlgorithmParameterSpec(JweHeaders headers) {
        return null;
    }
    protected String checkAlgorithm(String algo) {
        if (algo != null && !supportedAlgorithms.contains(algo)) {
            LOG.warning("Invalid key encryption algorithm: " + algo);
            throw new JweException(JweException.Error.INVALID_KEY_ALGORITHM);
        }
        return algo;
    }
    protected void checkAlgorithms(JweHeaders headers) {
        KeyAlgorithm providedAlgo = headers.getKeyEncryptionAlgorithm();
        if (providedAlgo != null && !providedAlgo.equals(algorithm)) {
            LOG.warning("Invalid key encryption algorithm: " + providedAlgo);
            throw new JweException(JweException.Error.INVALID_KEY_ALGORITHM);
        }
        if (providedAlgo != null) {
            checkAlgorithm(providedAlgo.getJwaName());
        } else {
            checkAlgorithm(algorithm.getJwaName());
            headers.setKeyEncryptionAlgorithm(algorithm);
        }
    }

}
