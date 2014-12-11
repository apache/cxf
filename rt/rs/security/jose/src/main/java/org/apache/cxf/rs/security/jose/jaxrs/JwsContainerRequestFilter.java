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

import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.JoseUtils;
import org.apache.cxf.rs.security.jose.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;

@PreMatching
@Priority(Priorities.JWS_SERVER_READ_PRIORITY)
public class JwsContainerRequestFilter extends AbstractJwsReaderProvider implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        if (HttpMethod.GET.equals(context.getMethod())) {
            return;
        }
        JwsSignatureVerifier theSigVerifier = getInitializedSigVerifier();
        JwsCompactConsumer p = new JwsCompactConsumer(IOUtils.readStringFromStream(context.getEntityStream()));
        if (!p.verifySignatureWith(theSigVerifier)) {
            context.abortWith(JAXRSUtils.toResponse(400));
            return;
        }
        JoseUtils.validateRequestContextProperty(p.getJoseHeaders());
        byte[] bytes = p.getDecodedJwsPayloadBytes();
        context.setEntityStream(new ByteArrayInputStream(bytes));
        context.getHeaders().putSingle("Content-Length", Integer.toString(bytes.length));
        
        String ct = JoseUtils.checkContentType(p.getJoseHeaders().getContentType(), getDefaultMediaType());
        if (ct != null) {
            context.getHeaders().putSingle("Content-Type", ct);
        }
    }
    
}
