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

import java.security.interfaces.ECPrivateKey;

import org.apache.cxf.rs.security.jose.jwa.Algorithm;

public class EcdhAesWrapKeyDecryptionAlgorithm implements KeyDecryptionAlgorithm {
    private ECPrivateKey key;
    private String algo;
    public EcdhAesWrapKeyDecryptionAlgorithm(ECPrivateKey key) {    
        this(key, Algorithm.ECDH_ES_A128KW.getJwtName());
    }
    public EcdhAesWrapKeyDecryptionAlgorithm(ECPrivateKey key, String algo) {    
        this.key = key;
        this.algo = algo;
    }
    @Override
    public byte[] getDecryptedContentEncryptionKey(JweDecryptionInput jweDecryptionInput) {
        byte[] derivedKey = 
            EcdhDirectKeyJweDecryption.getDecryptedContentEncryptionKeyFromHeaders(
                jweDecryptionInput.getJweHeaders(), key);
        KeyDecryptionAlgorithm aesWrap = new AesWrapKeyDecryptionAlgorithm(derivedKey) {
            protected boolean isValidAlgorithmFamily(String wrapAlgo) {
                return Algorithm.isEcdhEsWrap(wrapAlgo);
            }    
        };
        return aesWrap.getDecryptedContentEncryptionKey(jweDecryptionInput);
    }    
    
    @Override
    public String getAlgorithm() {
        return algo;
    }
    
}
