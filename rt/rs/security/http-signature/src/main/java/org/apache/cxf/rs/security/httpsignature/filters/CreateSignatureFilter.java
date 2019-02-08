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
import java.util.Objects;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.httpsignature.MessageSigner;

/**
 * RS CXF container Filter which signs outgoing messages. It does not create a digest header
 *
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public final class CreateSignatureFilter implements ContainerRequestFilter {
    private static final Logger LOG = LogUtils.getL7dLogger(VerifySignatureFilter.class);

    private MessageSigner messageSigner;
    private boolean enabled;

    public CreateSignatureFilter() {
        setEnabled(true);
    }

    @Override
    public void filter(ContainerRequestContext requestCtx) {
        if (!enabled) {
            LOG.fine("Create signature filter is disabled");
            return;
        }

        if (messageSigner == null) {
            LOG.warning("Message signer cannot be null");
            return;
        }

        MultivaluedMap<String, String> responseHeaders = requestCtx.getHeaders();
        if (responseHeaders.containsKey("Signature")) {
            LOG.fine("Message already contains a signature");
            return;
        }

        LOG.fine("Starting filter message signing process");
        try {
            messageSigner.sign(responseHeaders,
                requestCtx.getUriInfo().getAbsolutePath().getPath(), requestCtx.getMethod());
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.fine("Finished filter message verification process");
    }

    public MessageSigner getMessageSigner() {
        return messageSigner;
    }

    public void setMessageSigner(MessageSigner messageSigner) {
        Objects.requireNonNull(messageSigner);
        this.messageSigner = messageSigner;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
