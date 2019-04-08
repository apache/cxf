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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.httpsignature.exception.MissingSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MultipleSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.provider.AlgorithmProvider;
import org.apache.cxf.rs.security.httpsignature.provider.KeyProvider;
import org.apache.cxf.rs.security.httpsignature.provider.SecurityProvider;
import org.apache.cxf.rs.security.httpsignature.utils.DefaultSignatureConstants;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;

public class MessageVerifier {
    private static final Logger LOG = LogUtils.getL7dLogger(MessageVerifier.class);

    private AlgorithmProvider algorithmProvider;
    private KeyProvider keyProvider;
    private SecurityProvider securityProvider;
    private final SignatureValidator signatureValidator;

    public MessageVerifier(KeyProvider keyProvider) {
        this(keyProvider, Collections.emptyList());
    }

    public MessageVerifier(KeyProvider keyProvider, List<String> requiredHeaders) {
        this(keyProvider,
            null,
            keyId -> DefaultSignatureConstants.SIGNING_ALGORITHM,
            requiredHeaders);
    }

    public MessageVerifier(KeyProvider keyProvider,
                           AlgorithmProvider algorithmProvider) {
        this(keyProvider, null, algorithmProvider, Collections.emptyList());
    }

    public MessageVerifier(KeyProvider keyProvider,
                           SecurityProvider securityProvider,
                           AlgorithmProvider algorithmProvider) {
        this(keyProvider, securityProvider, algorithmProvider, Collections.emptyList());
    }

    public MessageVerifier(KeyProvider keyProvider,
                           SecurityProvider securityProvider,
                           AlgorithmProvider algorithmProvider,
                           List<String> requiredHeaders) {
        setkeyProvider(keyProvider);
        setSecurityProvider(securityProvider);
        setAlgorithmProvider(algorithmProvider);
        this.signatureValidator = new TomitribeSignatureValidator(requiredHeaders);
    }

    public final void setkeyProvider(KeyProvider provider) {
        this.keyProvider = Objects.requireNonNull(provider, "public key provider cannot be null");
    }

    public final void setSecurityProvider(SecurityProvider securityProvider) {

        this.securityProvider = securityProvider;
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
                                    keyProvider,
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
