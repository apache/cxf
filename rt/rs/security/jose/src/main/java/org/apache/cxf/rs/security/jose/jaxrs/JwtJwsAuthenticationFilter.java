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

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.JoseException;
import org.apache.cxf.rs.security.jose.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.security.SecurityContext;

@PreMatching
@Priority(Priorities.JWS_SERVER_READ_PRIORITY)
public class JwtJwsAuthenticationFilter extends AbstractJwsReaderProvider implements ContainerRequestFilter {
    private static final String JWS_CONTEXT_PROPERTY = "org.apache.cxf.jws.context";
    private static final String JWT_SCHEME_PROPERTY = "JWT";
    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        String authHeader = context.getHeaderString(HttpHeaders.AUTHORIZATION);
        String[] schemeData = authHeader.split(" ");
        if (schemeData.length != 2 || !JWT_SCHEME_PROPERTY.equals(schemeData[0])) {
            throw new JoseException("JWT scheme is expected");
        }
        
        JwsJwtCompactConsumer p = new JwsJwtCompactConsumer(schemeData[1]);
        JwsSignatureVerifier theSigVerifier = getInitializedSigVerifier(p.getJoseHeaders());
        if (!p.verifySignatureWith(theSigVerifier)) {
            context.abortWith(JAXRSUtils.toResponse(400));
            return;
        }
        Message m = JAXRSUtils.getCurrentMessage();
        setRequestContextProperty(m, p);
        JwtToken token = p.getJwtToken();
        m.put(SecurityContext.class, new JwtTokenSecurityContext(token));
        
    }
    protected void setRequestContextProperty(Message m, JwsCompactConsumer c) {
        Object headerContext = c.getJoseHeaders().getHeader(JWS_CONTEXT_PROPERTY);
        if (headerContext != null) {
            m.put(JWS_CONTEXT_PROPERTY, headerContext);
        }
    }
}
