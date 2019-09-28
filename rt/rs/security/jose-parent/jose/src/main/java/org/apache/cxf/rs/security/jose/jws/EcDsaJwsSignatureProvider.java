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

import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.AlgorithmParameterSpec;

import org.apache.cxf.rs.security.jose.common.JoseException;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;

public class EcDsaJwsSignatureProvider extends PrivateKeyJwsSignatureProvider {
    public EcDsaJwsSignatureProvider(ECPrivateKey key, SignatureAlgorithm algo) {
        this(key, null, algo);
    }
    public EcDsaJwsSignatureProvider(ECPrivateKey key, AlgorithmParameterSpec spec, SignatureAlgorithm algo) {
        this(key, null, spec, algo);
    }
    public EcDsaJwsSignatureProvider(ECPrivateKey key, SecureRandom random, AlgorithmParameterSpec spec,
                                     SignatureAlgorithm algo) {
        super(key, random, spec, algo);
    }
    @Override
    protected boolean isValidAlgorithmFamily(String algo) {
        return AlgorithmUtils.isEcDsaSign(algo);
    }
    @Override
    protected JwsSignature doCreateJwsSignature(Signature s) {
        return new EcDsaPrivateKeyJwsSignature(s,
            EcDsaJwsSignatureVerifier.SIGNATURE_LENGTH_MAP.get(super.getAlgorithm().getJwaName()));
    }

    protected static class EcDsaPrivateKeyJwsSignature extends PrivateKeyJwsSignature {
        private int outLen;
        public EcDsaPrivateKeyJwsSignature(Signature s, int outLen) {
            super(s);
            this.outLen = outLen;
        }
        @Override
        public byte[] sign() {
            byte[] jcaDer = super.sign();
            return jcaOutputToJoseOutput(outLen, jcaDer);
        }
    }

    private static byte[] jcaOutputToJoseOutput(int jwsSignatureLen, byte[] jcaDer) {
        // Apache2 Licensed Jose4j code which adapts the Apache Santuario XMLSecurity
        // code and aligns it with JWS/JWA requirements
        if (jcaDer.length < 8 || jcaDer[0] != 48) {
            throw new JoseException("Invalid format of ECDSA signature");
        }

        int offset;
        if (jcaDer[1] > 0) {
            offset = 2;
        } else if (jcaDer[1] == (byte) 0x81) {
            offset = 3;
        } else {
            throw new JoseException("Invalid format of ECDSA signature");
        }

        byte rLength = jcaDer[offset + 1];

        int i;
        for (i = rLength; i > 0 && jcaDer[(offset + 2 + rLength) - i] == 0; i--) {
            // complete
        }

        byte sLength = jcaDer[offset + 2 + rLength + 1];

        int j;
        for (j = sLength; j > 0 && jcaDer[(offset + 2 + rLength + 2 + sLength) - j] == 0; j--) {
            // complete
        }

        int rawLen = Math.max(i, j);
        rawLen = Math.max(rawLen, jwsSignatureLen / 2);

        if ((jcaDer[offset - 1] & 0xff) != jcaDer.length - offset
            || (jcaDer[offset - 1] & 0xff) != 2 + rLength + 2 + sLength
            || jcaDer[offset] != 2
            || jcaDer[offset + 2 + rLength] != 2) {
            throw new JoseException("Invalid format of ECDSA signature");
        }
        
        byte[] concatenatedSignatureBytes = new byte[2 * rawLen];

        System.arraycopy(jcaDer, (offset + 2 + rLength) - i, concatenatedSignatureBytes, rawLen - i, i);
        System.arraycopy(jcaDer, (offset + 2 + rLength + 2 + sLength) - j, 
                         concatenatedSignatureBytes, 2 * rawLen - j, j);

        return concatenatedSignatureBytes;
    }
    

}
