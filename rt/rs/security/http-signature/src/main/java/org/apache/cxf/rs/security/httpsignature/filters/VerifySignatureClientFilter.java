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

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.rs.security.httpsignature.exception.SignatureException;

/**
 * RS CXF client Filter which verifies the Digest header, and then extracts signature data from the context
 * and sends it to the message verifier
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class VerifySignatureClientFilter extends AbstractSignatureInFilter implements ClientResponseFilter {

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        byte[] messageBody = verifyDigest(responseContext.getHeaders(), responseContext.getEntityStream());
        if (messageBody != null) {
            responseContext.setEntityStream(new ByteArrayInputStream(messageBody));
        }

        verifySignature(responseContext.getHeaders(), "", "", messageBody);
    }

    @Override
    protected void handleException(Exception ex) {
        throw new SignatureException("Error verifying signature", ex);
    }
}
