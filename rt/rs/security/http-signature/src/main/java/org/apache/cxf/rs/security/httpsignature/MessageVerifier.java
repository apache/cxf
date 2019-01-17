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

import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.httpsignature.exception.MissingSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MultipleSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.provider.AlgorithmProvider;
import org.apache.cxf.rs.security.httpsignature.provider.PublicKeyProvider;
import org.apache.cxf.rs.security.httpsignature.provider.SecurityProvider;
import org.apache.cxf.rs.security.httpsignature.utils.DefaultSignatureConstants;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;

public class MessageVerifier {
    private static final Logger LOG = LogUtils.getL7dLogger(MessageVerifier.class);

    private AlgorithmProvider algorithmProvider;
    private PublicKeyProvider publicKeyProvider;
    private SecurityProvider securityProvider;
    private final SignatureValidator signatureValidator;

    public MessageVerifier(PublicKeyProvider publicKeyProvider) {
        this(publicKeyProvider,
            keyId -> Security.getProvider(DefaultSignatureConstants.SECURITY_PROVIDER),
            keyId -> DefaultSignatureConstants.SIGNING_ALGORITHM);
    }

    public MessageVerifier(PublicKeyProvider publicKeyProvider,
                           SecurityProvider securityProvider,
                           AlgorithmProvider algorithmProvider) {
        setPublicKeyProvider(publicKeyProvider);
        setSecurityProvider(securityProvider);
        setAlgorithmProvider(algorithmProvider);
        this.signatureValidator = new TomitribeSignatureValidator();
    }

    public final void setPublicKeyProvider(PublicKeyProvider publicKeyProvider) {
        this.publicKeyProvider = Objects.requireNonNull(publicKeyProvider, "public key provider cannot be null");
    }

    public final void setSecurityProvider(SecurityProvider securityProvider) {

        this.securityProvider = Objects.requireNonNull(securityProvider, "security provider cannot be null");
    }

    public final void setAlgorithmProvider(AlgorithmProvider algorithmProvider) {
        this.algorithmProvider = Objects.requireNonNull(algorithmProvider, "algorithm provider cannot be null");
    }

    public void verifyMessage(Map<String, List<String>> messageHeaders, String method, String uri) {
        SignatureHeaderUtils.inspectMessageHeaders(messageHeaders);
        inspectMissingSignatureHeader(messageHeaders);

        inspectMultipleSignatureHeaders(messageHeaders);
        LOG.fine("Starting signature verification");
        signatureValidator.validate(messageHeaders,
                                    algorithmProvider,
                                    publicKeyProvider,
                                    securityProvider,
                                    method,
                                    uri);
        LOG.fine("Finished signature verification");
    }

    private void inspectMultipleSignatureHeaders(Map<String, List<String>> responseHeaders) {
        if (responseHeaders.get("Signature").size() > 1) {
            throw new MultipleSignatureHeaderException("there are multiple signature headers in request");
        }
    }

    private void inspectMissingSignatureHeader(Map<String, List<String>> responseHeaders) {
        if (!responseHeaders.containsKey("Signature")) {
            throw new MissingSignatureHeaderException("there is no signature header in request");
        }
    }

}
