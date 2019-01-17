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
package org.apache.cxf.rs.security.httpsignature;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.httpsignature.exception.DifferentAlgorithmsException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidDataToVerifySignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidSignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.provider.AlgorithmProvider;
import org.apache.cxf.rs.security.httpsignature.provider.PublicKeyProvider;
import org.apache.cxf.rs.security.httpsignature.provider.SecurityProvider;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;
import org.tomitribe.auth.signatures.Signature;
import org.tomitribe.auth.signatures.Verifier;

public class TomitribeSignatureValidator implements SignatureValidator {
    private static final Logger LOG = LogUtils.getL7dLogger(TomitribeSignatureValidator.class);

    @Override
    public void validate(Map<String, List<String>> messageHeaders,
                         AlgorithmProvider algorithmProvider,
                         PublicKeyProvider publicKeyProvider,
                         SecurityProvider securityProvider,
                         String method,
                         String uri) {
        Signature signature = extractSignatureFromHeader(messageHeaders.get("Signature").get(0));

        String providedAlgorithm = algorithmProvider.getAlgorithmName(signature.getKeyId());

        String signatureAlgorithm = signature.getAlgorithm().toString();
        if (!providedAlgorithm.equals(signatureAlgorithm)) {
            throw new DifferentAlgorithmsException("signature algorithm from header and provided are different");
        }

        Key key = publicKeyProvider.getKey(signature.getKeyId());

        java.security.Provider provider = securityProvider.getProvider(signature.getKeyId());

        runVerifier(messageHeaders, key, signature, provider, method, uri);
    }

    private static Signature extractSignatureFromHeader(String signatureString) {
        try {
            return Signature.fromString(signatureString);
        } catch (Exception e) {
            throw new InvalidSignatureHeaderException("failed to parse signature from header", e);
        }
    }

    private void runVerifier(Map<String, List<String>> messageHeaders,
                             Key key,
                             Signature signature,
                             java.security.Provider provider,
                             String method,
                             String uri) {
        Verifier verifier = new Verifier(key, signature, provider);
        LOG.fine("Starting signature validation");
        boolean success;
        try {
            success = verifier.verify(method, uri, SignatureHeaderUtils.mapHeaders(messageHeaders));
        } catch (Exception e) {
            throw new InvalidDataToVerifySignatureException(e.getMessage(), e);
        }
        if (!success) {
            throw new InvalidSignatureException("signature is not valid");
        }
        LOG.fine("Finished signature validation");
    }

}
