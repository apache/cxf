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

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;

public class PbesHmacAesWrapKeyDecryptionAlgorithm implements KeyDecryptionAlgorithm {
    private byte[] password;
    private String algo;
    public PbesHmacAesWrapKeyDecryptionAlgorithm(String password) {    
        this(password, Algorithm.PBES2_HS256_A128KW.getJwtName());
    }
    public PbesHmacAesWrapKeyDecryptionAlgorithm(String password, String algo) {    
        this(PbesHmacAesWrapKeyEncryptionAlgorithm.stringToBytes(password), algo);
    }
    public PbesHmacAesWrapKeyDecryptionAlgorithm(char[] password) {    
        this(password, Algorithm.PBES2_HS256_A128KW.getJwtName());
    }
    public PbesHmacAesWrapKeyDecryptionAlgorithm(char[] password, String algo) {    
        this(PbesHmacAesWrapKeyEncryptionAlgorithm.charsToBytes(password), algo);
    }
    public PbesHmacAesWrapKeyDecryptionAlgorithm(byte[] password) {    
        this(password, Algorithm.PBES2_HS256_A128KW.getJwtName());
    }
    public PbesHmacAesWrapKeyDecryptionAlgorithm(byte[] password, String algo) {    
        this.password = password;
        this.algo = algo;
    }
    @Override
    public byte[] getDecryptedContentEncryptionKey(JweCompactConsumer consumer) {
        byte[] saltInput = getDecodedBytes(consumer, "p2s");
        int pbesCount = consumer.getJweHeaders().getIntegerHeader("p2c");
        String keyAlgoJwt = consumer.getJweHeaders().getAlgorithm();
        int keySize = PbesHmacAesWrapKeyEncryptionAlgorithm.getKeySize(keyAlgoJwt);
        byte[] derivedKey = PbesHmacAesWrapKeyEncryptionAlgorithm
            .createDerivedKey(keyAlgoJwt, keySize, password, saltInput, pbesCount);
        KeyDecryptionAlgorithm aesWrap = new AesWrapKeyDecryptionAlgorithm(derivedKey) {
            protected boolean isValidAlgorithmFamily(String wrapAlgo) {
                return Algorithm.isPbesHsWrap(wrapAlgo);
            }    
        };
        return aesWrap.getDecryptedContentEncryptionKey(consumer);
    }    
    private byte[] getDecodedBytes(JweCompactConsumer consumer, String headerName) {
        try {
            Object headerValue = consumer.getJweHeaders().getHeader(headerName);
            return Base64UrlUtility.decode(headerValue.toString());
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    @Override
    public String getAlgorithm() {
        return algo;
    }
    
}
