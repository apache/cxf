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
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public class PrivateKeyJwsSignatureProvider extends AbstractJwsSignatureProvider {
    private final PrivateKey key;
    private final SecureRandom random;
    private final AlgorithmParameterSpec signatureSpec;

    public PrivateKeyJwsSignatureProvider(PrivateKey key, SignatureAlgorithm algo) {
        this(key, null, algo);
    }
    public PrivateKeyJwsSignatureProvider(PrivateKey key, AlgorithmParameterSpec spec, SignatureAlgorithm algo) {
        this(key, null, spec, algo);
    }
    public PrivateKeyJwsSignatureProvider(PrivateKey key, SecureRandom random,
                                          AlgorithmParameterSpec spec, SignatureAlgorithm algo) {
        super(algo);
        this.key = key;
        this.random = random;
        String javaAlgoName = algo.getJavaName();
        if (javaAlgoName.equals(AlgorithmUtils.PS_SHA_JAVA)
            && spec == null) {
            //must have spec in this case
            String size = algo.getJwaName().substring(2);
            switch (size) {
            case "256" : 
                spec = new PSSParameterSpec(MGF1ParameterSpec.SHA256.getDigestAlgorithm(), 
                                            "MGF1", MGF1ParameterSpec.SHA256, Integer.valueOf(size) / 8, 1);
                break;
            case "384" : 
                spec = new PSSParameterSpec(MGF1ParameterSpec.SHA384.getDigestAlgorithm(),  
                                            "MGF1", MGF1ParameterSpec.SHA384, Integer.valueOf(size) / 8, 1);
                break;
            case "512" : 
                spec = new PSSParameterSpec(MGF1ParameterSpec.SHA512.getDigestAlgorithm(), 
                                            "MGF1", MGF1ParameterSpec.SHA512, Integer.valueOf(size) / 8, 1);
                break;
            default : 
                spec = PSSParameterSpec.DEFAULT;
            }
            

        }
        this.signatureSpec = spec;
    }
    protected JwsSignature doCreateJwsSignature(JwsHeaders headers) {
        final String sigAlgo = headers.getSignatureAlgorithm().getJwaName();
        final Signature s = CryptoUtils.getSignature(key,
                                                     AlgorithmUtils.toJavaName(sigAlgo),
                                                     random,
                                                     signatureSpec);
        return doCreateJwsSignature(s);
    }
    protected JwsSignature doCreateJwsSignature(Signature s) {
        return new PrivateKeyJwsSignature(s);
    }

    @Override
    protected boolean isValidAlgorithmFamily(String algo) {
        return AlgorithmUtils.isRsaSign(algo);
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
                throw new JwsException(JwsException.Error.SIGNATURE_FAILURE, ex);
            }
        }

        @Override
        public byte[] sign() {
            try {
                return s.sign();
            } catch (SignatureException ex) {
                throw new JwsException(JwsException.Error.SIGNATURE_FAILURE, ex);
            }
        }

    }

}
