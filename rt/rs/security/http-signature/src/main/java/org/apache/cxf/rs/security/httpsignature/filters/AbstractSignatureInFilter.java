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
package org.apache.cxf.rs.security.httpsignature.filters;

import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.httpsignature.DigestVerifier;
import org.apache.cxf.rs.security.httpsignature.HTTPSignatureConstants;
import org.apache.cxf.rs.security.httpsignature.MessageVerifier;
import org.apache.cxf.rs.security.httpsignature.exception.DifferentAlgorithmsException;
import org.apache.cxf.rs.security.httpsignature.exception.DifferentDigestsException;
import org.apache.cxf.rs.security.httpsignature.exception.DigestFailureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidDataToVerifySignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidSignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MissingDigestException;
import org.apache.cxf.rs.security.httpsignature.exception.MissingSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MultipleSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.SignatureException;
import org.apache.cxf.rs.security.httpsignature.utils.DefaultSignatureConstants;
import org.apache.cxf.rs.security.httpsignature.utils.KeyManagementUtils;

/**
 * RS CXF abstract Filter which verifies the Digest header, and then extracts signature data from the context
 * and sends it to the message verifier
 */
abstract class AbstractSignatureInFilter {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractSignatureInFilter.class);

    private MessageVerifier messageVerifier;
    private boolean enabled;

    AbstractSignatureInFilter() {
        this.enabled = true;
    }

    protected byte[] verifyDigest(MultivaluedMap<String, String> headers, InputStream entityStream) {
        byte[] messageBody = null;
        if (!enabled) {
            return messageBody;
        }

        // Verify digest if we have the appropriate header and a non-empty request body. Note that it is up to
        // the MessageVerifier configuration, or the HTTPSignatureConstants.RSSEC_HTTP_SIGNATURE_IN_HEADERS
        // configuration to require that the digest is signed (and hence present)
        if (entityStream != null && headers.containsKey("Digest")) {
            LOG.fine("Digesting message body");
            try {
                messageBody = IOUtils.readBytesFromStream(entityStream);
            } catch (IOException e) {
                throw new DigestFailureException("failed to validate the digest", e);
            }
            DigestVerifier digestVerifier = new DigestVerifier();

            try {
                digestVerifier.inspectDigest(messageBody, headers);
            } catch (DigestFailureException | DifferentDigestsException | MissingDigestException ex) {
                Message message = PhaseInterceptorChain.getCurrentMessage();
                if (MessageUtils.isRequestor(message)) {
                    throw ex;
                }
                throw new BadRequestException(ex);
            }
        }

        LOG.fine("Finished digest message verification process");
        return messageBody;
    }

    protected void verifySignature(MultivaluedMap<String, String> headers, String uriPath,
                                   String httpMethod, byte[] messageBody) {
        if (!enabled) {
            LOG.fine("Verify signature filter is disabled");
            return;
        }

        if (messageVerifier == null) {
            messageVerifier = createMessageVerifier();
        }

        LOG.fine("Starting filter message verification process");
        try {
            messageVerifier.verifyMessage(headers, httpMethod, uriPath,
                    PhaseInterceptorChain.getCurrentMessage(),
                    messageBody);
        } catch (DifferentAlgorithmsException | InvalidSignatureHeaderException
            | InvalidDataToVerifySignatureException | InvalidSignatureException
            | MultipleSignatureHeaderException | MissingSignatureHeaderException ex) {
            LOG.warning(ex.getMessage());
            handleException(ex);
        }
        LOG.fine("Finished filter message verification process");
    }

    public void setMessageVerifier(MessageVerifier messageVerifier) {
        Objects.requireNonNull(messageVerifier);
        this.messageVerifier = messageVerifier;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    protected MessageVerifier createMessageVerifier() {
        Properties props = KeyManagementUtils.loadSignatureInProperties();
        if (props == null) {
            throw new SignatureException("Signature properties are not configured correctly");
        }

        Message m = PhaseInterceptorChain.getCurrentMessage();
        PublicKey publicKey = KeyManagementUtils.loadPublicKey(m, props);

        String signatureAlgorithm = (String)m.getContextualProperty(HTTPSignatureConstants.RSSEC_SIGNATURE_ALGORITHM);
        if (signatureAlgorithm == null) {
            signatureAlgorithm = DefaultSignatureConstants.SIGNING_ALGORITHM;
        }

        final String finalSignatureAlgorithm = signatureAlgorithm;
        final Provider provider = Security.getProvider(DefaultSignatureConstants.SECURITY_PROVIDER);
        return new MessageVerifier(keyId -> publicKey, keyId -> provider, keyId -> finalSignatureAlgorithm);
    }

    protected abstract void handleException(Exception ex);
}
