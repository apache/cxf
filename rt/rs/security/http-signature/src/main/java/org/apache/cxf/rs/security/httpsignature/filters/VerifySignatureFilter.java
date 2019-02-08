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

import java.util.Objects;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.httpsignature.MessageVerifier;
import org.apache.cxf.rs.security.httpsignature.exception.DifferentAlgorithmsException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidDataToVerifySignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidSignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MissingSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MultipleSignatureHeaderException;

/**
 * RS CXF Filter which extracts signature data from the context and sends it to the message verifier
 *
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public final class VerifySignatureFilter implements ContainerRequestFilter {
    private static final Logger LOG = LogUtils.getL7dLogger(VerifySignatureFilter.class);

    private MessageVerifier messageVerifier;
    private boolean enabled;

    public VerifySignatureFilter() {
        setEnabled(true);
    }

    @Override
    public void filter(ContainerRequestContext requestCtx) {
        if (!enabled) {
            LOG.fine("Verify signature filter is disabled");
            return;
        }

        if (messageVerifier == null) {
            LOG.warning("Message verifier cannot be null");
            return;
        }

        LOG.fine("Starting filter message verification process");
        MultivaluedMap<String, String> responseHeaders = requestCtx.getHeaders();
        try {
            messageVerifier.verifyMessage(responseHeaders,
                                          requestCtx.getMethod(), requestCtx.getUriInfo().getAbsolutePath().getPath());
        } catch (DifferentAlgorithmsException | InvalidSignatureHeaderException
            | InvalidDataToVerifySignatureException | InvalidSignatureException
            | MultipleSignatureHeaderException | MissingSignatureHeaderException ex) {
            LOG.warning(ex.getMessage());
            throw new BadRequestException(ex);
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

}
