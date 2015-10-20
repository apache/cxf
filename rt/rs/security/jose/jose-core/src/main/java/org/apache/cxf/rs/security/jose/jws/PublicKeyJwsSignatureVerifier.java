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
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public class PublicKeyJwsSignatureVerifier implements JwsSignatureVerifier {
    protected static final Logger LOG = LogUtils.getL7dLogger(PublicKeyJwsSignatureVerifier.class);
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
        JwsUtils.checkSignatureKeySize(key);
    }
    @Override
    public boolean verify(JwsHeaders headers, String unsignedText, byte[] signature) {
        try {
            return CryptoUtils.verifySignature(StringUtils.toBytesUTF8(unsignedText), 
                                               signature, 
                                               key, 
                                               AlgorithmUtils.toJavaName(checkAlgorithm(
                                                                              headers.getSignatureAlgorithm())),
                                               signatureSpec);
        } catch (Exception ex) {
            LOG.warning("Invalid signature: " + ex.getMessage());
            throw new JwsException(JwsException.Error.INVALID_SIGNATURE, ex);
        }
    }
    protected String checkAlgorithm(SignatureAlgorithm sigAlgo) {
        String algo = sigAlgo.getJwaName();
        if (algo == null) {
            LOG.warning("Signature algorithm is not set");
            throw new JwsException(JwsException.Error.ALGORITHM_NOT_SET);
        }
        if (!isValidAlgorithmFamily(algo)
            || !algo.equals(supportedAlgo.getJwaName())) {
            LOG.warning("Invalid signature algorithm: " + algo);
            throw new JwsException(JwsException.Error.INVALID_ALGORITHM);
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
