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
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;

public class EcDsaJwsSignatureVerifier extends PublicKeyJwsSignatureVerifier {
    static final Map<String, Integer> SIGNATURE_LENGTH_MAP;
    static {
        SIGNATURE_LENGTH_MAP = new HashMap<String, Integer>();
        SIGNATURE_LENGTH_MAP.put(SignatureAlgorithm.ES256.getJwaName(), 64);
        SIGNATURE_LENGTH_MAP.put(SignatureAlgorithm.ES384.getJwaName(), 96);
        SIGNATURE_LENGTH_MAP.put(SignatureAlgorithm.ES512.getJwaName(), 132);
    }
    public EcDsaJwsSignatureVerifier(PublicKey key, SignatureAlgorithm supportedAlgo) {
        this(key, null, supportedAlgo);
    }
    public EcDsaJwsSignatureVerifier(PublicKey key, AlgorithmParameterSpec spec, SignatureAlgorithm supportedAlgo) {
        super(key, spec, supportedAlgo);
    }
    public EcDsaJwsSignatureVerifier(X509Certificate cert, SignatureAlgorithm supportedAlgo) {
        this(cert, null, supportedAlgo);
    }
    public EcDsaJwsSignatureVerifier(X509Certificate cert, 
                                     AlgorithmParameterSpec spec, 
                                     SignatureAlgorithm supportedAlgo) {
        super(cert, spec, supportedAlgo);
    }
    @Override
    public boolean verify(JwsHeaders headers, String unsignedText, byte[] signature) {
        final String algoName = super.getAlgorithm().getJwaName();
        if (SIGNATURE_LENGTH_MAP.get(algoName) != signature.length) {
            LOG.warning("Algorithm " + algoName + " signature length is " + SIGNATURE_LENGTH_MAP.get(algoName) 
                        + ", actual length is " + signature.length);
            throw new JwsException(JwsException.Error.INVALID_SIGNATURE);
        }
        byte[] der = signatureToDer(signature);
        return super.verify(headers, unsignedText, der);
    }
    @Override
    protected boolean isValidAlgorithmFamily(String algo) {
        return AlgorithmUtils.isEcDsaSign(algo);
    }
    private static byte[] signatureToDer(byte joseSig[]) {
        int partLen = joseSig.length / 2;
        int rOffset = joseSig[0] < 0 ? 1 : 0;
        int sOffset = joseSig[partLen] < 0 ? 1 : 0;
        int rPartLen = partLen + rOffset;
        int sPartLen = partLen + sOffset;
        int totalLenBytesCount = joseSig.length > 127 ? 2 : 1;
        int rPartStart = 1 + totalLenBytesCount + 2;
        byte[] der = new byte[rPartStart + 2 + rPartLen + sPartLen];
        der[0] = 48;
        if (totalLenBytesCount == 2) {
            der[1] = -127;
        }
        der[totalLenBytesCount] = (byte)(der.length - (1 + totalLenBytesCount));
        der[totalLenBytesCount + 1] = 2;
        der[totalLenBytesCount + 2] = (byte)rPartLen;
        int sPartStart = rPartStart + rPartLen;
        der[sPartStart] = 2;
        der[sPartStart + 1] = (byte)sPartLen;
        System.arraycopy(joseSig, 0, der, rPartStart + rOffset, partLen);
        System.arraycopy(joseSig, partLen, der, sPartStart + 2 + sOffset, partLen);
        return der;
    }
}
