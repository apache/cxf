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

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;


public class AesGcmContentEncryptionAlgorithm extends AbstractContentEncryptionAlgorithm {
    private static final int DEFAULT_IV_SIZE = 96;
    public AesGcmContentEncryptionAlgorithm(ContentAlgorithm algo) {
        this(algo, false);
    }
    public AesGcmContentEncryptionAlgorithm(ContentAlgorithm algo, boolean generateCekOnce) {
        super(checkAlgorithm(algo), generateCekOnce);
    }
    public AesGcmContentEncryptionAlgorithm(String encodedCek, String encodedIv, ContentAlgorithm algo) {
        this(CryptoUtils.decodeSequence(encodedCek), CryptoUtils.decodeSequence(encodedIv), algo);
    }
    public AesGcmContentEncryptionAlgorithm(String encodedCek, ContentAlgorithm algo) {
        this(CryptoUtils.decodeSequence(encodedCek), null, algo);
    }
    public AesGcmContentEncryptionAlgorithm(SecretKey key, ContentAlgorithm algo) {
        this(key, (byte[])null, algo);
    }
    public AesGcmContentEncryptionAlgorithm(SecretKey key, byte[] iv, ContentAlgorithm algo) {
        this(key.getEncoded(), iv, algo);
    }
    public AesGcmContentEncryptionAlgorithm(byte[] cek, ContentAlgorithm algo) {
        this(cek, (byte[])null, algo);
    }
    public AesGcmContentEncryptionAlgorithm(byte[] cek, byte[] iv, ContentAlgorithm algo) {
        super(cek, iv, checkAlgorithm(algo));
    }
    protected int getIvSize() {
        return DEFAULT_IV_SIZE;
    }
    private static ContentAlgorithm checkAlgorithm(ContentAlgorithm algo) {
        if (AlgorithmUtils.isAesGcm(algo.getJwaName())) {
            return algo;
        }
        LOG.warning("Invalid content encryption algorithm");
        throw new JweException(JweException.Error.INVALID_CONTENT_ALGORITHM);
    }
    
}