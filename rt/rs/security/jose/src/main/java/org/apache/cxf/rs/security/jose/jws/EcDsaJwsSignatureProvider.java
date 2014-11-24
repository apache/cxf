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

import org.apache.cxf.rs.security.jose.jwa.Algorithm;

public class EcDsaJwsSignatureProvider extends PrivateKeyJwsSignatureProvider {
    public EcDsaJwsSignatureProvider(ECPrivateKey key, String algo) {
        this(key, null, algo);
    }
    public EcDsaJwsSignatureProvider(ECPrivateKey key, AlgorithmParameterSpec spec, String algo) {
        this(key, null, spec, algo);
    }
    public EcDsaJwsSignatureProvider(ECPrivateKey key, SecureRandom random, AlgorithmParameterSpec spec, 
                                     String algo) {
        super(key, random, spec, algo);
    }
    @Override
    protected boolean isValidAlgorithmFamily(String algo) {
        return Algorithm.isEcDsaSign(algo);
    }
    @Override
    protected JwsSignature doCreateJwsSignature(Signature s) {
        return new EcDsaPrivateKeyJwsSignature(s);
    }
    
    protected static class EcDsaPrivateKeyJwsSignature extends PrivateKeyJwsSignature {
        public EcDsaPrivateKeyJwsSignature(Signature s) {
            super(s);
        }
        @Override
        public byte[] sign() {
            byte[] der = super.sign();
            return jcaOutputToJoseOutput(der);
        }
    }
    
    private static byte[] jcaOutputToJoseOutput(byte jcaDer[]) {
        // DER uses a pattern of type-length-value triplets
        // http://en.wikipedia.org/wiki/Abstract_Syntax_Notation_One#Example_encoded_in_DER
        
        // The algorithm implementation guarantees the correct DER format so no extra validation
        
        // ECDSA signature production: 
        // 48 (SEQUENCE) + total length + R & S triples, where every triple is 2 
        // (INTEGER TYPE + length + the actual integer)
        
        int rPartLen = jcaDer[3];
        int rOffset = rPartLen % 8;
        int rValueStart = 4 + rOffset;
        int sPartStart = 4 + rPartLen;
        int sPartLen = jcaDer[sPartStart + 1];
        int sOffset = sPartLen % 8;
        int sValueStart = sPartStart + 2 + sOffset;
        
        int partLen = rPartLen - rOffset;
        byte[] result = new byte[partLen * 2]; 
        System.arraycopy(jcaDer, rValueStart, result, 0, partLen);
        System.arraycopy(jcaDer, sValueStart, result, partLen, partLen);
        return result;
    }
}
