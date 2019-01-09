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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.rs.security.httpsignature.DigestVerifier;
import org.apache.cxf.rs.security.httpsignature.exception.DigestFailureException;

/**
 * RS CXF Reader Interceptor which extract the body of the message and verifies the digest
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public final class VerifyDigestInterceptor implements ReaderInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(VerifyDigestInterceptor.class);

    private DigestVerifier digestVerifier;
    private boolean enabled;

    public VerifyDigestInterceptor() {
        setEnabled(true);
        setDigestVerifier(new DigestVerifier());
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        if (!enabled) {
            LOG.fine("Verify signature reader interceptor is disabled");
            return context.proceed();
        }
        LOG.fine("Starting interceptor message verification process");

        Map<String, List<String>> responseHeaders = context.getHeaders();

        byte[] messageBody = extractMessageBody(context);

        digestVerifier.inspectDigest(messageBody, responseHeaders);

        context.setInputStream(new ByteArrayInputStream(messageBody));
        LOG.fine("Finished interceptor message verification process");

        return context.proceed();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DigestVerifier getDigestVerifier() {
        return digestVerifier;
    }

    public void setDigestVerifier(DigestVerifier digestVerifier) {
        Objects.requireNonNull(digestVerifier);
        this.digestVerifier = digestVerifier;
    }

    private byte[] extractMessageBody(ReaderInterceptorContext context) {
        try (InputStream is = context.getInputStream()) {
            return IOUtils.readBytesFromStream(is);
        } catch (IOException e) {
            throw new DigestFailureException("failed to validate the digest", e);
        }
    }

}
