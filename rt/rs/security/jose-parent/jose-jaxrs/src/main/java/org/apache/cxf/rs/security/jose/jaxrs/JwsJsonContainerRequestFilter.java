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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jws.JwsException;
import org.apache.cxf.rs.security.jose.jws.JwsJsonConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJsonSignatureEntry;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;

@PreMatching
@Priority(Priorities.JWS_SERVER_READ_PRIORITY)
public class JwsJsonContainerRequestFilter extends AbstractJwsJsonReaderProvider implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        if (HttpMethod.GET.equals(context.getMethod())) {
            return;
        }
        List<JwsSignatureVerifier> theSigVerifiers = getInitializedSigVerifiers();
        if (theSigVerifiers.isEmpty()) {
            context.abortWith(JAXRSUtils.toResponse(400));
            return;
        }
        JwsJsonConsumer p = new JwsJsonConsumer(IOUtils.readStringFromStream(context.getEntityStream()));
        
        try {
            List<JwsJsonSignatureEntry> remaining = p.verifyAndGetNonValidated(theSigVerifiers,
                                                                               isStrictVerification());
            if (!remaining.isEmpty()) {
                JAXRSUtils.getCurrentMessage().put("jws.json.remaining.entries", remaining);
            }
        } catch (JwsException ex) {
            context.abortWith(JAXRSUtils.toResponse(400));
            return;
        }
        
        byte[] bytes = p.getDecodedJwsPayloadBytes();
        context.setEntityStream(new ByteArrayInputStream(bytes));
        context.getHeaders().putSingle("Content-Length", Integer.toString(bytes.length));
        
        // the list is guaranteed to be non-empty
        JwsJsonSignatureEntry sigEntry = p.getSignatureEntries().get(0);
        String ct = JoseUtils.checkContentType(sigEntry.getUnionHeader().getContentType(), getDefaultMediaType());
        if (ct != null) {
            context.getHeaders().putSingle("Content-Type", ct);
        }
    }
}
