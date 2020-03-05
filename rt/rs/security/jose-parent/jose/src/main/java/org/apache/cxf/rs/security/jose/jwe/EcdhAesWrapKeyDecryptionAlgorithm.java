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

import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;

public class EcdhAesWrapKeyDecryptionAlgorithm implements KeyDecryptionProvider {
    private final ECPrivateKey key;
    private final KeyAlgorithm algo;

    public EcdhAesWrapKeyDecryptionAlgorithm(ECPrivateKey key) {
        this(key, KeyAlgorithm.ECDH_ES_A128KW);
    }
    public EcdhAesWrapKeyDecryptionAlgorithm(ECPrivateKey key, KeyAlgorithm algo) {
        this.key = key;
        this.algo = algo != null ? algo : KeyAlgorithm.ECDH_ES_A128KW;
    }
    @Override
    public byte[] getDecryptedContentEncryptionKey(JweDecryptionInput jweDecryptionInput) {
        byte[] derivedKey = getDecryptedContentEncryptionKeyFromHeaders(
                jweDecryptionInput.getJweHeaders(), key);
        KeyDecryptionProvider aesWrap = new AesWrapKeyDecryptionAlgorithm(derivedKey, algo) {
            protected boolean isValidAlgorithmFamily(String wrapAlgo) {
                return AlgorithmUtils.isEcdhEsWrap(wrapAlgo);
            }
        };
        return aesWrap.getDecryptedContentEncryptionKey(jweDecryptionInput);
    }

    @Override
    public KeyAlgorithm getAlgorithm() {
        return algo;
    }

    protected byte[] getDecryptedContentEncryptionKeyFromHeaders(JweHeaders headers, ECPrivateKey privateKey) {
        KeyAlgorithm jwtAlgo = headers.getKeyEncryptionAlgorithm();
        JsonWebKey publicJwk = headers.getJsonWebKey("epk");
        String apuHeader = (String) headers.getHeader("apu");
        byte[] apuBytes = apuHeader == null ? null : JoseUtils.decode(apuHeader);
        String apvHeader = (String) headers.getHeader("apv");
        byte[] apvBytes = apvHeader == null ? null : JoseUtils.decode(apvHeader);
        return JweUtils.getECDHKey(privateKey, JwkUtils.toECPublicKey(publicJwk), apuBytes, apvBytes,
            jwtAlgo.getJwaName(), jwtAlgo.getKeySizeBits());
    }

}
