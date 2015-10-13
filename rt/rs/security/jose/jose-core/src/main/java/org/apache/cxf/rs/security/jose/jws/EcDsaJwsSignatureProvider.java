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
    
    private static byte[] jcaOutputToJoseOutput(int jwsSignatureLen, byte jcaDer[]) {
        // DER uses a pattern of type-length-value triplets
        // http://en.wikipedia.org/wiki/Abstract_Syntax_Notation_One#Example_encoded_in_DER
        
        // The algorithm implementation guarantees the correct DER format so no extra validation
        
        // ECDSA signature production: 
        // 48 (SEQUENCE) + Total Length (1 or 2 bytes, the 1st byte is -127 if 2 bytes) 
        // + R & S triples, where both triples are represented as 
        // 2(INTEGER TYPE) + length + the actual sequence of a given length;
        // The sequence might have the extra leading zeroes which need to be skipped
        int requiredPartLen = jwsSignatureLen / 2;
        
        int rsDataBlockStart = jcaDer[1] == -127 ? 4 : 3;
        int rPartLen = jcaDer[rsDataBlockStart];
        int rDataBlockStart = rsDataBlockStart + 1;
        int rPartLenDiff = rPartLen - requiredPartLen; 
        int rValueStart = rDataBlockStart + getDataBlockOffset(jcaDer, rDataBlockStart, rPartLenDiff);
        
        int sPartStart = rDataBlockStart + rPartLen;
        int sPartLen = jcaDer[sPartStart + 1];
        int sPartLenDiff = sPartLen - requiredPartLen; 
        int sDataBlockStart = sPartStart + 2;
        int sValueStart = sDataBlockStart + getDataBlockOffset(jcaDer, sDataBlockStart, sPartLenDiff);
                
        byte[] result = new byte[jwsSignatureLen]; 
        System.arraycopy(jcaDer, rValueStart, result, 
            rPartLenDiff < 0 ? rPartLenDiff * -1 : 0, 
            rPartLenDiff < 0 ? requiredPartLen + rPartLenDiff : requiredPartLen);
        System.arraycopy(jcaDer, sValueStart, result, 
            sPartLenDiff < 0 ? requiredPartLen + sPartLenDiff * -1 : requiredPartLen, 
            sPartLenDiff < 0 ? requiredPartLen + sPartLenDiff : requiredPartLen);
        return result;
    }
    private static int getDataBlockOffset(byte[] jcaDer, int blockStart, int partLenDiff) {
        // ECDSA productions have 64, 96 or 132 output lengths. The R and S parts would be 32, 48 or 66 bytes each.
        // If it is 32 or 48 bytes then we may have occasional extra zeroes in the JCA DER output
        int i = 0;
        if (partLenDiff > 0) {
            while (i < partLenDiff && jcaDer[blockStart + i] == 0) {
                i++;
            }
        }
        return i;
    }
    
    
}
