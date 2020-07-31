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
package org.apache.cxf.sts.rs.authentication;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static org.apache.cxf.common.util.StringUtils.isEmpty;
import static org.apache.cxf.jaxrs.utils.JAXRSUtils.getCurrentMessage;

@PreMatching
@Priority(AUTHENTICATION)
public class JwtAuthenticationFilter extends AbstractAuthenticationFilter {
    private static final String AUTHENTICATION_SCHEME = "Bearer";
    private static final String TOKEN_TYPE = "jwt";
    private static final String ROLES_CLAIM = "roles";

    protected String getTokenType() {
        return TOKEN_TYPE;
    };

    protected boolean isTokenBasedAuthentication(String authorizationHeader) {
        return !isEmpty(authorizationHeader)
            && authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
    }

    protected void createSecurityContext(final ContainerRequestContext context, final String token) {
        final JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token);
        final JwtToken jwt = jwtConsumer.getJwtToken();
        final SecurityContext securityContext = context.getSecurityContext();
        final SecurityContext newSecurityContext = new SecurityContext() {

            public Principal getUserPrincipal() {
                return ofNullable(jwt)
                        .map(j -> j.getClaims())
                        .map(c -> c.getSubject())
                        .map(SimplePrincipal::new)
                        .orElse(null);
            }

            public boolean isUserInRole(String role) {
                List<String> roles = (List<String>) ofNullable(jwt)
                        .map(j -> j.getClaims())
                        .map(c -> c.getClaim(ROLES_CLAIM))
                        .orElse(null);
                return ofNullable(roles)
                        .orElse(Collections.emptyList())
                        .stream()
                        .anyMatch(r -> r.equals(role));
            }

            @Override
            public boolean isSecure() {
                return securityContext.isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return securityContext.getAuthenticationScheme();
            }
        };
        context.setSecurityContext(newSecurityContext);
        getCurrentMessage().put(SecurityContext.class, newSecurityContext);
    }
}

