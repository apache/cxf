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
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public class AesWrapKeyDecryptionAlgorithm extends WrappedKeyDecryptionAlgorithm {
    public AesWrapKeyDecryptionAlgorithm(String encodedKey) {
        this(encodedKey, KeyAlgorithm.A128KW);
    }
    public AesWrapKeyDecryptionAlgorithm(String encodedKey, KeyAlgorithm supportedAlgo) {
        this(CryptoUtils.decodeSequence(encodedKey), supportedAlgo);
    }
    public AesWrapKeyDecryptionAlgorithm(byte[] secretKey) {
        this(secretKey, KeyAlgorithm.A128KW);
    }
    public AesWrapKeyDecryptionAlgorithm(byte[] secretKey, KeyAlgorithm supportedAlgo) {
        this(CryptoUtils.createSecretKeySpec(secretKey, AlgorithmUtils.AES_WRAP_ALGO_JAVA),
             supportedAlgo);
    }
    public AesWrapKeyDecryptionAlgorithm(SecretKey secretKey) {
        this(secretKey, null);
    }
    public AesWrapKeyDecryptionAlgorithm(SecretKey secretKey, KeyAlgorithm supportedAlgo) {
        super(secretKey, supportedAlgo);
    }
    @Override
    protected void validateKeyEncryptionAlgorithm(String keyAlgo) {
        super.validateKeyEncryptionAlgorithm(keyAlgo);
        if (!isValidAlgorithmFamily(keyAlgo)) {
            reportInvalidKeyAlgorithm(keyAlgo);
        }
    }

    protected boolean isValidAlgorithmFamily(String keyAlgo) {
        return AlgorithmUtils.isAesKeyWrap(keyAlgo);
    }

}
