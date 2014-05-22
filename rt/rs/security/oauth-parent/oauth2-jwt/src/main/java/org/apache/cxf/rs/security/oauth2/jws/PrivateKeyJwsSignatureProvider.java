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
package org.apache.cxf.rs.security.oauth2.jws;

import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeaders;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

public class PrivateKeyJwsSignatureProvider implements JwsSignatureVerifier {
    private PrivateKey key;
    private SecureRandom random; 
    private AlgorithmParameterSpec signatureSpec;
    
    public PrivateKeyJwsSignatureProvider(PrivateKey key) {
        this(key, null);
    }
    public PrivateKeyJwsSignatureProvider(PrivateKey key, AlgorithmParameterSpec spec) {
        this(key, null, spec);
    }
    public PrivateKeyJwsSignatureProvider(PrivateKey key, SecureRandom random, AlgorithmParameterSpec spec) {
        this.key = key;
        this.random = random;
        this.signatureSpec = spec;
    }
    
    
    @Override
    public byte[] sign(JwtHeaders headers, String unsignedText) {
        try {
            return CryptoUtils.signData(unsignedText.getBytes("UTF-8"), 
                                        key, 
                                        Algorithm.toJavaName(headers.getAlgorithm()),
                                        random,
                                        signatureSpec);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    

}
