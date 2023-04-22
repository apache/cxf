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

import java.io.*;

import jakarta.annotation.Priority;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;
/**
 * RS CXF container Filter which verifies the Digest header, and then extracts signature data from the context
 * and sends it to the message verifier
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class VerifySignatureFilter extends AbstractSignatureInFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestCtx) {
        byte[] messageBody = verifyDigest(requestCtx.getHeaders(), requestCtx.getEntityStream());
        if (messageBody != null) {
            requestCtx.setEntityStream(new ByteArrayInputStream(messageBody));
        }
        verifySignature(requestCtx.getHeaders(),
                SignatureHeaderUtils.createRequestTarget(requestCtx.getUriInfo().getRequestUri()),
                        requestCtx.getMethod(), messageBody);
    }

    @Override
    protected void handleException(Exception ex) {
        throw new BadRequestException(ex);
    }
}
