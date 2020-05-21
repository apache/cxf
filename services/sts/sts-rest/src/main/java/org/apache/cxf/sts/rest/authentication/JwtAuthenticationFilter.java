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
package org.apache.cxf.sts.rest.authentication;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.operation.TokenValidateOperation;
import org.apache.cxf.sts.rest.impl.TokenUtils;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.StatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.Priorities.AUTHENTICATION;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.Response.status;
import static org.apache.cxf.common.util.StringUtils.isEmpty;
import static org.apache.cxf.jaxrs.utils.JAXRSUtils.getCurrentMessage;
import static org.apache.cxf.sts.rest.impl.TokenUtils.createValidateRequestSecurityTokenType;
import static org.apache.cxf.sts.rest.impl.TokenUtils.getEncodedJwtToken;

@PreMatching
@Priority(AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final QName QNAME_WST_STATUS =
            QNameConstants.WS_TRUST_FACTORY.createStatus(null).getName();
    private static final String AUTHENTICATION_SCHEME = "Bearer";
    private TokenValidateOperation validateOperation;

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        if (context.getSecurityContext().getUserPrincipal() != null) {
            LOG.debug("User principal is already set, pass filter without authentication processing");
            return;
        }

        final String authorizationHeader  = context.getHeaderString(AUTHORIZATION);
        if (isEmpty(authorizationHeader)) {
            return; //abortUnauthorized(context, "This is no Authorization header in the request");
        }

        if (!isTokenBasedAuthentication(authorizationHeader)) {
            abortUnauthorized(context, "Authorization header is present, but is not Bearer schema");
        }

        if (ofNullable(validateOperation).isPresent()) {
            try {
                validateToken(context, authorizationHeader);
            } catch (Exception e) {
                abortUnauthorized(context, e.getMessage());
            }
        } else {
            LOG.debug("ValidationOperation bean is not defined");
            abortUnauthorized(context, null);
        }
    }

    private boolean isTokenBasedAuthentication(String authorizationHeader) {
        return !isEmpty(authorizationHeader)
            && authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
    }

    private void abortUnauthorized(final ContainerRequestContext context, final String message) {
        LOG.error("Authorization error: {}", message);
        context.abortWith(status(UNAUTHORIZED).build());
    }

    private void validateToken(final ContainerRequestContext context, final String authorizationHeader)
        throws Exception {
        final String tokenString = getEncodedJwtToken(authorizationHeader);
        if (isEmpty(tokenString)) {
            throw new NotAuthorizedException("Bearer schema is present but token is empty",
                TokenUtils.BEARER_AUTH_SCHEMA);
        }

        final Message message = getCurrentMessage();
        final RequestSecurityTokenResponseType response = validateOperation.validate(
            createValidateRequestSecurityTokenType(tokenString, "jwt"), null,
            new WrappedMessageContext(message));
        if (validateResponse(response)) {
            createSecurityContext(context, tokenString);
        } else {
            throw new NotAuthorizedException("Bearer token validation is failed", TokenUtils.BEARER_AUTH_SCHEMA);
        }
    }

    private void createSecurityContext(final ContainerRequestContext context, final String token) {
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
                        .map(c -> c.getClaim("roles"))
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

    private boolean validateResponse(RequestSecurityTokenResponseType response) {

        for (Object requestObject : response.getAny()) {
            if (requestObject instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>) requestObject;
                if (QNAME_WST_STATUS.equals(jaxbElement.getName())) {
                    StatusType status = (StatusType)jaxbElement.getValue();
                    if (STSConstants.VALID_CODE.equals(status.getCode())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public TokenValidateOperation getValidateOperation() {
        return validateOperation;
    }

    public void setValidateOperation(TokenValidateOperation validateOperation) {
        this.validateOperation = validateOperation;
    }
}

