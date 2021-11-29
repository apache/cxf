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

import java.util.*;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
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
    private final List<String> requiredHeaders;
    private boolean addDefaultRequiredHeaders = true;

    public MessageVerifier(KeyProvider keyProvider) {
        this(keyProvider, (List<String>)null);
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
        this(keyProvider, securityProvider, algorithmProvider, requiredHeaders, new TomitribeSignatureValidator());
    }

    public MessageVerifier(KeyProvider keyProvider,
                           SecurityProvider securityProvider,
                           AlgorithmProvider algorithmProvider,
                           List<String> requiredHeaders,
                           SignatureValidator signatureValidator) {
        setkeyProvider(keyProvider);
        setSecurityProvider(securityProvider);
        setAlgorithmProvider(algorithmProvider);
        this.requiredHeaders = requiredHeaders == null ? Collections.emptyList() : requiredHeaders;
        this.signatureValidator = signatureValidator;
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

    public void verifyMessage(Map<String, List<String>> messageHeaders, String method,
                              String uri, Message m, byte[] messageBody) {
        SignatureHeaderUtils.inspectMessageHeaders(messageHeaders);
        inspectMissingSignatureHeader(messageHeaders);

        inspectMultipleSignatureHeaders(messageHeaders);

        // Calculate the headers that we require to be signed. Use the headers configured on this class if available,
        // falling back to the contextual property.
        List<String> signedHeaders = new ArrayList<>(requiredHeaders);
        if (requiredHeaders.isEmpty()
            && m.getContextualProperty(HTTPSignatureConstants.RSSEC_HTTP_SIGNATURE_IN_HEADERS) != null) {
            signedHeaders =
                CastUtils.cast(
                    (List<?>)m.getContextualProperty(HTTPSignatureConstants.RSSEC_HTTP_SIGNATURE_IN_HEADERS));
        }

        // Add the default required headers
        boolean requestor = MessageUtils.isRequestor(m);
        if (addDefaultRequiredHeaders) {
            if (!(requestor || signedHeaders.contains(HTTPSignatureConstants.REQUEST_TARGET))) {
                signedHeaders.add(HTTPSignatureConstants.REQUEST_TARGET);
            }

            if (!signedHeaders.contains("digest") && ((messageBody != null && messageBody.length > 0)
                    || ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                    || "PATCH".equalsIgnoreCase(method)))) {
                signedHeaders.add("digest");
            }
        }

        // Now dynamically check to see whether the "digest" header is required or not. It's not required
        // for the service case on a GET/HEAD request as there is no request, and also not required on
        // the client side unless it is an OK response that is not 204
        Integer status = (Integer)m.get(Message.RESPONSE_CODE);
        if ((requestor && (status < 200 || status >= 300 || status == 204))
            || (!requestor && ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)))) {
            signedHeaders.remove("digest");
        }

        LOG.fine("Starting signature verification");
        signatureValidator.validate(messageHeaders,
                                    algorithmProvider,
                                    keyProvider,
                                    securityProvider,
                                    method,
                                    uri,
                                    signedHeaders);
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

    public boolean isAddDefaultRequiredHeaders() {
        return addDefaultRequiredHeaders;
    }

    /**
     * Set whether we require some default headers to be signed, such as "digest" and "(request-target"),
     * depending on whether there is a request body or not, and whether we are the client or not
     */
    public void setAddDefaultRequiredHeaders(boolean addDefaultRequiredHeaders) {
        this.addDefaultRequiredHeaders = addDefaultRequiredHeaders;
    }

}
