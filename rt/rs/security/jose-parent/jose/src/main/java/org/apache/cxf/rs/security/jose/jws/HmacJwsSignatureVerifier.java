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

import java.security.MessageDigest;
import java.security.spec.AlgorithmParameterSpec;
import java.util.logging.Logger;

import javax.crypto.Mac;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rt.security.crypto.HmacUtils;

public class HmacJwsSignatureVerifier implements JwsSignatureVerifier {
    protected static final Logger LOG = LogUtils.getL7dLogger(HmacJwsSignatureVerifier.class);
    private final byte[] key;
    private final AlgorithmParameterSpec hmacSpec;
    private final SignatureAlgorithm supportedAlgo;

    public HmacJwsSignatureVerifier(String encodedKey) {
        this(JoseUtils.decode(encodedKey), SignatureAlgorithm.HS256);
    }
    public HmacJwsSignatureVerifier(String encodedKey, SignatureAlgorithm supportedAlgo) {
        this(JoseUtils.decode(encodedKey), supportedAlgo);
    }
    public HmacJwsSignatureVerifier(byte[] key, SignatureAlgorithm supportedAlgo) {
        this(key, null, supportedAlgo);
    }
    public HmacJwsSignatureVerifier(byte[] key, AlgorithmParameterSpec spec, SignatureAlgorithm supportedAlgo) {
        this.key = key;
        this.hmacSpec = spec;
        this.supportedAlgo = supportedAlgo;
    }


    @Override
    public boolean verify(JwsHeaders headers, String unsignedText, byte[] signature) {
        byte[] expected = computeMac(headers, unsignedText);
        return MessageDigest.isEqual(expected, signature);
    }

    private byte[] computeMac(JwsHeaders headers, String text) {
        final String sigAlgo = checkAlgorithm(headers.getSignatureAlgorithm());
        return HmacUtils.computeHmac(key,
                                     AlgorithmUtils.toJavaName(sigAlgo),
                                     hmacSpec,
                                     text);
    }

    protected String checkAlgorithm(SignatureAlgorithm sigAlgo) {

        if (sigAlgo == null) {
            LOG.warning("Signature algorithm is not set");
            throw new JwsException(JwsException.Error.ALGORITHM_NOT_SET);
        }
        String algo = sigAlgo.getJwaName();
        if (!AlgorithmUtils.isHmacSign(algo)
            || !algo.equals(supportedAlgo.getJwaName())) {
            LOG.warning("Invalid signature algorithm: " + algo);
            throw new JwsException(JwsException.Error.INVALID_ALGORITHM);
        }
        return algo;
    }
    @Override
    public SignatureAlgorithm getAlgorithm() {
        return supportedAlgo;
    }
    @Override
    public JwsVerificationSignature createJwsVerificationSignature(JwsHeaders headers) {
        final String sigAlgo = checkAlgorithm(headers.getSignatureAlgorithm());
        Mac mac = HmacUtils.getInitializedMac(key,
                                     AlgorithmUtils.toJavaName(sigAlgo),
                                     hmacSpec);
        return new HmacJwsVerificationSignature(mac);
    }

    private static class HmacJwsVerificationSignature implements JwsVerificationSignature {

        private Mac mac;

        HmacJwsVerificationSignature(Mac mac) {
            this.mac = mac;
        }

        @Override
        public void update(byte[] src, int off, int len) {
            mac.update(src, off, len);
        }

        @Override
        public boolean verify(byte[] signature) {
            byte[] macBytes = mac.doFinal();
            return MessageDigest.isEqual(macBytes, signature);
        }

    }
}
