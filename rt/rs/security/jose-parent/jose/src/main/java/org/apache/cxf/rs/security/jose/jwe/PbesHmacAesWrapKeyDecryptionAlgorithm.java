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
import org.apache.cxf.rs.security.jose.common.JoseException;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;

public class PbesHmacAesWrapKeyDecryptionAlgorithm implements KeyDecryptionProvider {
    private final byte[] password;
    private final KeyAlgorithm algo;
    private final int maxPbesCount;

    public PbesHmacAesWrapKeyDecryptionAlgorithm(String password) {
        this(password, KeyAlgorithm.PBES2_HS256_A128KW, false);
    }
    public PbesHmacAesWrapKeyDecryptionAlgorithm(String password, int maxPbesCount) {
        this(PbesHmacAesWrapKeyEncryptionAlgorithm.stringToBytes(password), KeyAlgorithm.PBES2_HS256_A128KW, 
            false, maxPbesCount);
    }
    public PbesHmacAesWrapKeyDecryptionAlgorithm(String password, KeyAlgorithm algo, boolean hashLargePasswords) {
        this(PbesHmacAesWrapKeyEncryptionAlgorithm.stringToBytes(password), algo, hashLargePasswords);
    }
    public PbesHmacAesWrapKeyDecryptionAlgorithm(char[] password) {
        this(password, KeyAlgorithm.PBES2_HS256_A128KW, false);
    }
    public PbesHmacAesWrapKeyDecryptionAlgorithm(char[] password, KeyAlgorithm algo, boolean hashLargePasswords) {
        this(PbesHmacAesWrapKeyEncryptionAlgorithm.charsToBytes(password), algo, hashLargePasswords);
    }
    public PbesHmacAesWrapKeyDecryptionAlgorithm(byte[] password) {
        this(password, KeyAlgorithm.PBES2_HS256_A128KW, false);
    }
    public PbesHmacAesWrapKeyDecryptionAlgorithm(byte[] password, KeyAlgorithm algo, boolean hashLargePasswords) {
        this(password, algo, hashLargePasswords, 1_000_000);
    }

    public PbesHmacAesWrapKeyDecryptionAlgorithm(byte[] password, KeyAlgorithm algo, boolean hashLargePasswords,
        int maxPbesCount) {
        this.password =
            PbesHmacAesWrapKeyEncryptionAlgorithm.validatePassword(password, algo.getJwaName(), hashLargePasswords);
        this.algo = algo;
        this.maxPbesCount = maxPbesCount;
    }

    @Override
    public byte[] getDecryptedContentEncryptionKey(JweDecryptionInput jweDecryptionInput) {
        JweHeaders jweHeaders = jweDecryptionInput.getJweHeaders();
        byte[] saltInput = getDecodedBytes(jweHeaders.getHeader("p2s"));
        int pbesCount = jweHeaders.getIntegerHeader("p2c");
        if (pbesCount > maxPbesCount) {
            throw new JoseException("Too many PBES2 iterations");
        }
        String keyAlgoJwt = jweHeaders.getKeyEncryptionAlgorithm().getJwaName();
        int keySize = PbesHmacAesWrapKeyEncryptionAlgorithm.getKeySize(keyAlgoJwt);
        byte[] derivedKey = PbesHmacAesWrapKeyEncryptionAlgorithm
            .createDerivedKey(keyAlgoJwt, keySize, password, saltInput, pbesCount);
        KeyDecryptionProvider aesWrap = new AesWrapKeyDecryptionAlgorithm(derivedKey, algo) {
            protected boolean isValidAlgorithmFamily(String wrapAlgo) {
                return AlgorithmUtils.isPbesHsWrap(wrapAlgo);
            }
        };
        return aesWrap.getDecryptedContentEncryptionKey(jweDecryptionInput);
    }
    private byte[] getDecodedBytes(Object p2sHeader) {
        try {
            return Base64UrlUtility.decode(p2sHeader.toString());
        } catch (Exception ex) {
            throw new JoseException(ex);
        }
    }
    @Override
    public KeyAlgorithm getAlgorithm() {
        return algo;
    }

}
