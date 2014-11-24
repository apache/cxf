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
package org.apache.cxf.rs.security.jose.jws;

import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;

public class EcDsaJwsSignatureVerifier extends PublicKeyJwsSignatureVerifier {
    private static final Map<String, Integer> SIGNATURE_LENGTH_MAP;
    static {
        SIGNATURE_LENGTH_MAP = new HashMap<String, Integer>();
        SIGNATURE_LENGTH_MAP.put(Algorithm.SHA256withECDSA.getJwtName(), 64);
        SIGNATURE_LENGTH_MAP.put(Algorithm.SHA384withECDSA.getJwtName(), 96);
        SIGNATURE_LENGTH_MAP.put(Algorithm.SHA512withECDSA.getJwtName(), 132);
    }
    public EcDsaJwsSignatureVerifier(PublicKey key, String supportedAlgo) {
        this(key, null, supportedAlgo);
    }
    public EcDsaJwsSignatureVerifier(PublicKey key, AlgorithmParameterSpec spec, String supportedAlgo) {
        super(key, spec, supportedAlgo);
    }
    @Override
    public boolean verify(JoseHeaders headers, String unsignedText, byte[] signature) {
        if (SIGNATURE_LENGTH_MAP.get(super.getAlgorithm()) != signature.length) {
            throw new SecurityException();
        }
        byte[] der = signatureToDer(signature);
        return super.verify(headers, unsignedText, der);
    }
    @Override
    protected boolean isValidAlgorithmFamily(String algo) {
        return Algorithm.isEcDsaSign(algo);
    }
    private static byte[] signatureToDer(byte joseSig[]) {
        int partLen = joseSig.length / 2;
        // 0 needs to be appended if the first byte is negative
        int rOffset = joseSig[0] < 0 ? 1 : 0;
        int sOffset = joseSig[partLen] < 0 ? 1 : 0;
        
        byte[] der = new byte[6 + joseSig.length + rOffset + sOffset];
        der[0] = 48;
        der[1] = (byte)(der.length - 2);
        der[2] = 2;
        der[3] = (byte)(partLen + rOffset);
        int sPartStart = 4 + der[3];
        der[sPartStart] = 2;
        der[sPartStart + 1] = (byte)(partLen + sOffset);
        System.arraycopy(joseSig, 0, der, 4 + rOffset, partLen);
        System.arraycopy(joseSig, partLen, der, sPartStart + 2 + sOffset, partLen);
        return der;
    }
    
}
