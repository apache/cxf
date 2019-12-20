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

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;


public abstract class AbstractJwsSignatureProvider implements JwsSignatureProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractJwsSignatureProvider.class);
    private final SignatureAlgorithm algorithm;

    protected AbstractJwsSignatureProvider(SignatureAlgorithm algo) {
        this.algorithm = algo;
    }

    protected JwsHeaders prepareHeaders(JwsHeaders headers) {
        if (headers == null) {
            headers = new JwsHeaders();
        }
        SignatureAlgorithm sigAlgo = headers.getSignatureAlgorithm();
        if (sigAlgo != null) {
            checkAlgorithm(sigAlgo.getJwaName());
        } else {
            checkAlgorithm(algorithm.getJwaName());
            headers.setSignatureAlgorithm(algorithm);
        }
        return headers;
    }
    @Override
    public SignatureAlgorithm getAlgorithm() {
        return algorithm;
    }
    @Override
    public byte[] sign(JwsHeaders headers, byte[] content) {
        JwsSignature sig = createJwsSignature(headers);
        sig.update(content, 0, content.length);
        return sig.sign();
    }
    @Override
    public JwsSignature createJwsSignature(JwsHeaders headers) {
        return doCreateJwsSignature(prepareHeaders(headers));
    }

    protected abstract JwsSignature doCreateJwsSignature(JwsHeaders headers);

    protected void checkAlgorithm(String algo) {
        if (algo == null) {
            LOG.warning("Signature algorithm is not set");
            throw new JwsException(JwsException.Error.ALGORITHM_NOT_SET);
        }
        if (!isValidAlgorithmFamily(algo)) {
            LOG.warning("Invalid signature algorithm: " + algo);
            throw new JwsException(JwsException.Error.INVALID_ALGORITHM);
        }
    }
    protected abstract boolean isValidAlgorithmFamily(String algo);
}
