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

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;

public class PublicKeyJwsSignatureVerifier implements JwsSignatureVerifier {
    private PublicKey key;
    private AlgorithmParameterSpec signatureSpec;
    private SignatureAlgorithm supportedAlgo;
    
    public PublicKeyJwsSignatureVerifier(PublicKey key, SignatureAlgorithm supportedAlgorithm) {
        this(key, null, supportedAlgorithm);
    }
    public PublicKeyJwsSignatureVerifier(PublicKey key, AlgorithmParameterSpec spec, SignatureAlgorithm supportedAlgo) {
        this.key = key;
        this.signatureSpec = spec;
        this.supportedAlgo = supportedAlgo;
    }
    @Override
    public boolean verify(JoseHeaders headers, String unsignedText, byte[] signature) {
        try {
            return CryptoUtils.verifySignature(StringUtils.toBytesUTF8(unsignedText), 
                                               signature, 
                                               key, 
                                               AlgorithmUtils.toJavaName(checkAlgorithm(headers.getAlgorithm())),
                                               signatureSpec);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    protected String checkAlgorithm(String algo) {
        if (algo == null 
            || !isValidAlgorithmFamily(algo)
            || !algo.equals(supportedAlgo.getJwaName())) {
            throw new SecurityException();
        }
        return algo;
    }
    protected boolean isValidAlgorithmFamily(String algo) {
        return AlgorithmUtils.isRsaSign(algo);
    }
    @Override
    public SignatureAlgorithm getAlgorithm() {
        return supportedAlgo;
    }

}
