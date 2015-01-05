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
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwe.EcdhDirectKeyJweEncryption.EcdhHelper;

public class EcdhAesWrapKeyEncryptionAlgorithm implements KeyEncryptionAlgorithm {
    
    private static final Map<String, String> ECDH_AES_MAP;
    static {
        ECDH_AES_MAP = new HashMap<String, String>();
        ECDH_AES_MAP.put(Algorithm.ECDH_ES_A128KW.getJwtName(), Algorithm.A128KW.getJwtName());
        ECDH_AES_MAP.put(Algorithm.ECDH_ES_A192KW.getJwtName(), Algorithm.A192KW.getJwtName());
        ECDH_AES_MAP.put(Algorithm.ECDH_ES_A256KW.getJwtName(), Algorithm.A256KW.getJwtName());
    }
    private String keyAlgo;
    private EcdhHelper helper;
    
    public EcdhAesWrapKeyEncryptionAlgorithm(ECPublicKey peerPublicKey,
                                             String curve,
                                             String apuString,
                                             String apvString,
                                             String keyAlgo) {
        
        this.keyAlgo = keyAlgo;
        helper = new EcdhHelper(peerPublicKey, curve, apuString, apvString, keyAlgo);
    }
    
    @Override
    public byte[] getEncryptedContentEncryptionKey(JweHeaders headers, byte[] cek) {
        final byte[] derivedKey = helper.getDerivedKey(headers);
        Algorithm jwtAlgo = Algorithm.valueOf(ECDH_AES_MAP.get(keyAlgo));
        KeyEncryptionAlgorithm aesWrap = new AesWrapKeyEncryptionAlgorithm(derivedKey, 
                                                                           jwtAlgo.getJwtName()) {
            protected void checkAlgorithms(JweHeaders headers) {
                // complete
            }
            protected String getKeyEncryptionAlgoJava(JweHeaders headers) {
                return Algorithm.AES_WRAP_ALGO_JAVA;
            }
        };
        return aesWrap.getEncryptedContentEncryptionKey(headers, cek);
    }
    
    @Override
    public String getAlgorithm() {
        return keyAlgo;
    }
}
