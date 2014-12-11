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

import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;

public class PrivateKeyJwsSignatureProvider extends AbstractJwsSignatureProvider {
    private PrivateKey key;
    private SecureRandom random; 
    private AlgorithmParameterSpec signatureSpec;
    
    public PrivateKeyJwsSignatureProvider(PrivateKey key, String algo) {
        this(key, null, algo);
    }
    public PrivateKeyJwsSignatureProvider(PrivateKey key, AlgorithmParameterSpec spec, String algo) {
        this(key, null, spec, algo);
    }
    public PrivateKeyJwsSignatureProvider(PrivateKey key, SecureRandom random, 
                                          AlgorithmParameterSpec spec, String algo) {
        super(algo);
        this.key = key;
        this.random = random;
        this.signatureSpec = spec;
    }
    protected JwsSignature doCreateJwsSignature(JoseHeaders headers) {
        final Signature s = CryptoUtils.getSignature(key, 
                                                     Algorithm.toJavaName(headers.getAlgorithm()),
                                                     random,
                                                     signatureSpec);
        return doCreateJwsSignature(s);
    }
    protected JwsSignature doCreateJwsSignature(Signature s) {
        return new PrivateKeyJwsSignature(s);
    }
    @Override
    protected void checkAlgorithm(String algo) {
        super.checkAlgorithm(algo);
        if (!isValidAlgorithmFamily(algo)) {
            throw new SecurityException();
        }
        //TODO: validate "A key of size 2048 bits or larger MUST be used" for PS-SHA algorithms 
    }
    
    protected boolean isValidAlgorithmFamily(String algo) {
        return Algorithm.isRsaSign(algo);
    }

    protected static class PrivateKeyJwsSignature implements JwsSignature {
        private Signature s;
        public PrivateKeyJwsSignature(Signature s) {
            this.s = s;
        }
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
        
    }
}
