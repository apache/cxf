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

import java.security.interfaces.RSAPrivateKey;

import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;

public class RSAKeyDecryptionAlgorithm extends WrappedKeyDecryptionAlgorithm {
    public RSAKeyDecryptionAlgorithm(RSAPrivateKey privateKey) {
        this(privateKey, KeyAlgorithm.RSA_OAEP);
    }
    public RSAKeyDecryptionAlgorithm(RSAPrivateKey privateKey, KeyAlgorithm supportedAlgo) {
        this(privateKey, supportedAlgo, true);
    }
    public RSAKeyDecryptionAlgorithm(RSAPrivateKey privateKey, KeyAlgorithm supportedAlgo, boolean unwrap) {
        super(privateKey, supportedAlgo, unwrap);
        JweUtils.checkEncryptionKeySize(privateKey);
    }
    protected int getKeyCipherBlockSize() {
        return ((RSAPrivateKey)getCekDecryptionKey()).getModulus().toByteArray().length;
    }
    @Override
    protected void validateKeyEncryptionAlgorithm(String keyAlgo) {
        super.validateKeyEncryptionAlgorithm(keyAlgo);
        if (!AlgorithmUtils.isRsaKeyWrap(keyAlgo)) {
            reportInvalidKeyAlgorithm(keyAlgo);
        }
    }
}
