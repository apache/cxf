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
import java.security.Principal;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.PublicKeyJwsSignatureVerifier;
import org.apache.cxf.security.SecurityContext;

@PreMatching
@Priority(Priorities.JWS_SERVER_READ_PRIORITY)
public class JwsContainerRequestFilter extends AbstractJwsReaderProvider implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        if (isMethodWithNoContent(context.getMethod())
            || isCheckEmptyStream() && !context.hasEntity()) {
            return;
        }
        final String content = IOUtils.readStringFromStream(context.getEntityStream());
        if (StringUtils.isEmpty(content)) {
            return;
        }
        JwsCompactConsumer p = new JwsCompactConsumer(content);
        JwsSignatureVerifier theSigVerifier = getInitializedSigVerifier(p.getJwsHeaders());
        if (!p.verifySignatureWith(theSigVerifier)) {
            context.abortWith(JAXRSUtils.toResponse(400));
            return;
        }
        JoseUtils.validateRequestContextProperty(p.getJwsHeaders());
        
        byte[] bytes = p.getDecodedJwsPayloadBytes();
        context.setEntityStream(new ByteArrayInputStream(bytes));
        context.getHeaders().putSingle("Content-Length", Integer.toString(bytes.length));

        String ct = JoseUtils.checkContentType(p.getJwsHeaders().getContentType(), getDefaultMediaType());
        if (ct != null) {
            context.getHeaders().putSingle("Content-Type", ct);
        }

        if (super.isValidateHttpHeaders()) {
            super.validateHttpHeadersIfNeeded(context.getHeaders(), p.getJwsHeaders());
        }
        
        Principal currentPrincipal = context.getSecurityContext().getUserPrincipal();
        if (currentPrincipal == null || currentPrincipal.getName() == null) {
            SecurityContext securityContext = configureSecurityContext(theSigVerifier);
            if (securityContext != null) {
                JAXRSUtils.getCurrentMessage().put(SecurityContext.class, securityContext);
            }
        }
    }

    protected SecurityContext configureSecurityContext(JwsSignatureVerifier sigVerifier) {
        if (sigVerifier instanceof PublicKeyJwsSignatureVerifier
            && ((PublicKeyJwsSignatureVerifier)sigVerifier).getX509Certificate() != null) {
            final Principal principal =
                ((PublicKeyJwsSignatureVerifier)sigVerifier).getX509Certificate().getSubjectX500Principal();
            return new SecurityContext() {

                public Principal getUserPrincipal() {
                    return principal;
                }

                public boolean isUserInRole(String arg0) {
                    return false;
                }
            };
        }
        return null;
    }
    
    protected boolean isMethodWithNoContent(String method) {
        return HttpMethod.DELETE.equals(method) || HttpUtils.isMethodWithNoRequestContent(method);
    }
}
