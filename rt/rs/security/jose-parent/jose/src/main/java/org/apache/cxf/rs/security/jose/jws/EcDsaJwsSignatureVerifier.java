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

import org.apache.cxf.rs.security.jose.common.JoseException;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;

public class EcDsaJwsSignatureVerifier extends PublicKeyJwsSignatureVerifier {
    static final Map<String, Integer> SIGNATURE_LENGTH_MAP;
    static {
        SIGNATURE_LENGTH_MAP = new HashMap<>();
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
    
    private static byte[] signatureToDer(byte[] joseSig) {
        // Apache2 Licensed Jose4j code which adapts the Apache Santuario XMLSecurity
        // code and aligns it with JWS/JWA requirements
        int rawLen = joseSig.length / 2;

        int i;

        for (i = rawLen; i > 0 && joseSig[rawLen - i] == 0; i--) {
            // complete
        }

        int j = i;

        if (joseSig[rawLen - i] < 0) {
            j += 1;
        }

        int k;

        for (k = rawLen; k > 0 && joseSig[2 * rawLen - k] == 0; k--) {
            // complete
        }

        int l = k;

        if (joseSig[2 * rawLen - k] < 0) {
            l += 1;
        }

        int len = 2 + j + 2 + l;
        if (len > 255) {
            throw new JoseException("Invalid format of ECDSA signature");
        }
        int offset;
        byte[] derEncodedSignatureBytes;
        if (len < 128) {
            derEncodedSignatureBytes = new byte[2 + 2 + j + 2 + l];
            offset = 1;
        } else {
            derEncodedSignatureBytes = new byte[3 + 2 + j + 2 + l];
            derEncodedSignatureBytes[1] = (byte) 0x81;
            offset = 2;
        }

        derEncodedSignatureBytes[0] = 48;
        derEncodedSignatureBytes[offset++] = (byte) len;
        derEncodedSignatureBytes[offset++] = 2;
        derEncodedSignatureBytes[offset++] = (byte) j;

        System.arraycopy(joseSig, rawLen - i, derEncodedSignatureBytes, (offset + j) - i, i);

        offset += j;

        derEncodedSignatureBytes[offset++] = 2;
        derEncodedSignatureBytes[offset++] = (byte) l;

        System.arraycopy(joseSig, 2 * rawLen - k, derEncodedSignatureBytes, (offset + l) - k, k);

        return derEncodedSignatureBytes;
    }
}
