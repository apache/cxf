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
import java.util.Set;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.KeyProperties;

public abstract class AbstractWrapKeyEncryptionAlgorithm implements KeyEncryptionAlgorithm {
    private Key keyEncryptionKey;
    private boolean wrap;
    private String algorithm;
    private Set<String> supportedAlgorithms;
    protected AbstractWrapKeyEncryptionAlgorithm(Key key, Set<String> supportedAlgorithms) {
        this(key, null, true, supportedAlgorithms);
    }
    protected AbstractWrapKeyEncryptionAlgorithm(Key key, boolean wrap, Set<String> supportedAlgorithms) {
        this(key, null, wrap, supportedAlgorithms);
    }
    protected AbstractWrapKeyEncryptionAlgorithm(Key key, String jweAlgo, Set<String> supportedAlgorithms) {
        this(key, jweAlgo, true, supportedAlgorithms);
    }
    protected AbstractWrapKeyEncryptionAlgorithm(Key key, String jweAlgo, boolean wrap, 
                                                 Set<String> supportedAlgorithms) {
        this.keyEncryptionKey = key;
        this.algorithm = jweAlgo;
        this.wrap = wrap;
        this.supportedAlgorithms = supportedAlgorithms;
    }
    @Override
    public byte[] getEncryptedContentEncryptionKey(JweHeaders headers, byte[] cek) {
        checkAlgorithms(headers, algorithm);
        KeyProperties secretKeyProperties = new KeyProperties(getKeyEncryptionAlgoJava(headers));
        AlgorithmParameterSpec spec = getAlgorithmParameterSpec(); 
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
        return Algorithm.toJavaName(headers.getKeyEncryptionAlgorithm());
    }
    protected String getContentEncryptionAlgoJava(JweHeaders headers) {
        return Algorithm.toJavaName(headers.getContentEncryptionAlgorithm());
    }
    protected AlgorithmParameterSpec getAlgorithmParameterSpec() {
        return null;
    }
    private static String checkAlgorithm(Set<String> supportedAlgorithms, String algo) {
        if (algo != null && !supportedAlgorithms.contains(algo)) {
            throw new SecurityException();
        }
        return algo;
    }
    private void checkAlgorithms(JweHeaders headers, String defaultAlgo) {
        String providedAlgo = headers.getKeyEncryptionAlgorithm();
        if ((providedAlgo == null && defaultAlgo == null)
            || (providedAlgo != null && defaultAlgo != null && !providedAlgo.equals(defaultAlgo))) {
            throw new SecurityException();
        }
        if (providedAlgo != null) {
            checkAlgorithm(supportedAlgorithms, providedAlgo);
        } else {
            checkAlgorithms(headers, defaultAlgo);
            headers.setKeyEncryptionAlgorithm(defaultAlgo);
        }
    }
    
}
