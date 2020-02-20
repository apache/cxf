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

import java.security.interfaces.ECPublicKey;

import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

public class EcdhAesWrapKeyEncryptionAlgorithm implements KeyEncryptionProvider {

    private final KeyAlgorithm keyAlgo;
    private final EcdhHelper helper;

    public EcdhAesWrapKeyEncryptionAlgorithm(ECPublicKey peerPublicKey,
                                             KeyAlgorithm keyAlgo) {
        this(peerPublicKey, JsonWebKey.EC_CURVE_P256, keyAlgo);
    }
    public EcdhAesWrapKeyEncryptionAlgorithm(ECPublicKey peerPublicKey,
                                             String curve,
                                             KeyAlgorithm keyAlgo) {

        this(peerPublicKey, curve, null, null, keyAlgo, ContentAlgorithm.A128GCM);
    }
    public EcdhAesWrapKeyEncryptionAlgorithm(ECPublicKey peerPublicKey,
                                             String curve,
                                             KeyAlgorithm keyAlgo,
                                             ContentAlgorithm ctAlgo) {

        this(peerPublicKey, curve, null, null, keyAlgo, ctAlgo);
    }
    public EcdhAesWrapKeyEncryptionAlgorithm(ECPublicKey peerPublicKey,
                                             String curve,
                                             String apuString,
                                             String apvString,
                                             KeyAlgorithm keyAlgo,
                                             ContentAlgorithm ctAlgo) {

        this.keyAlgo = keyAlgo;
        helper = new EcdhHelper(peerPublicKey, curve, apuString, apvString,
                                ctAlgo.getJwaName());
    }

    @Override
    public byte[] getEncryptedContentEncryptionKey(JweHeaders headers, byte[] cek) {
        final byte[] derivedKey = helper.getDerivedKey(headers);
        KeyEncryptionProvider aesWrap = new AesWrapKeyEncryptionAlgorithm(derivedKey,
                                                                           keyAlgo) {
            protected void checkAlgorithms(JweHeaders headers) {
                // complete
            }
            protected String getKeyEncryptionAlgoJava(JweHeaders headers) {
                return AlgorithmUtils.AES_WRAP_ALGO_JAVA;
            }
        };
        return aesWrap.getEncryptedContentEncryptionKey(headers, cek);
    }

    @Override
    public KeyAlgorithm getAlgorithm() {
        return keyAlgo;
    }
}
