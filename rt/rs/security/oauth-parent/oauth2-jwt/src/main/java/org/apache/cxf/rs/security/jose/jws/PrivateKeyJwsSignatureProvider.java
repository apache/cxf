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

import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwt.JwtHeaders;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

public class PrivateKeyJwsSignatureProvider extends AbstractJwsSignatureProvider {
    private static final Set<String> SUPPORTED_ALGORITHMS = new HashSet<String>(
        Arrays.asList(Algorithm.SHA256withRSA.getJwtName(),
                      Algorithm.SHA384withRSA.getJwtName(),
                      Algorithm.SHA512withRSA.getJwtName())); 
    private PrivateKey key;
    private SecureRandom random; 
    private AlgorithmParameterSpec signatureSpec;
    
    public PrivateKeyJwsSignatureProvider(PrivateKey key) {
        this(key, null);
    }
    public PrivateKeyJwsSignatureProvider(PrivateKey key, AlgorithmParameterSpec spec) {
        this(key, null, spec);
    }
    public PrivateKeyJwsSignatureProvider(PrivateKey key, SecureRandom random, 
                                          AlgorithmParameterSpec spec) {
        this(key, random, spec, SUPPORTED_ALGORITHMS);
    }
    protected PrivateKeyJwsSignatureProvider(PrivateKey key, 
                                             SecureRandom random, 
                                             AlgorithmParameterSpec spec,
                                             Set<String> supportedAlgorithms) {
        super(supportedAlgorithms);
        this.key = key;
        this.random = random;
        this.signatureSpec = spec;
    }
    protected JwsSignature doCreateJwsSignature(JwtHeaders headers) {
        final Signature s = CryptoUtils.getSignature(key, 
                                                     Algorithm.toJavaName(headers.getAlgorithm()),
                                                     random,
                                                     signatureSpec);
        return new JwsSignature() {

            @Override
            public void update(byte[] src, int off, int len) {
                try {
                    s.update(src, off, len);
                } catch (SignatureException ex) {
                    throw new SecurityException();
                }
            }

            @Override
            public byte[] sign() {
                try {
                    return s.sign();
                } catch (SignatureException ex) {
                    throw new SecurityException();
                }
            }
            
        };
    }
    

}
