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

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.cxf.message.Message;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.operation.TokenValidateOperation;
import org.apache.cxf.sts.rs.impl.TokenUtils;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.StatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.Response.status;
import static org.apache.cxf.common.util.StringUtils.isEmpty;
import static org.apache.cxf.jaxrs.utils.JAXRSUtils.getCurrentMessage;
import static org.apache.cxf.sts.rs.impl.TokenUtils.createValidateRequestSecurityTokenType;
import static org.apache.cxf.sts.rs.impl.TokenUtils.getEncodedJwtToken;

public abstract class AbstractAuthenticationFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractAuthenticationFilter.class);
    private static final QName QNAME_WST_STATUS =
            QNameConstants.WS_TRUST_FACTORY.createStatus(null).getName();
    private TokenValidateOperation validateOperation;

    @Override
    public void filter(ContainerRequestContext context) {
        if (context.getSecurityContext().getUserPrincipal() != null) {
            LOG.debug("User principal is already set, pass filter without authentication processing");
            return;
        }

        final String authorizationHeader  = context.getHeaderString(AUTHORIZATION);
        if (isEmpty(authorizationHeader)) {
            return;
        }

        if (!isTokenBasedAuthentication(authorizationHeader)) {
            abortUnauthorized(context, "Authorization header is present, but schema is not correct");
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

    private void abortUnauthorized(final ContainerRequestContext context, final String message) {
        LOG.error("Authorization error: {}", message);
        context.abortWith(status(UNAUTHORIZED).build());
    }

    protected void validateToken(final ContainerRequestContext context, final String authorizationHeader) {
        final String tokenString = getEncodedJwtToken(authorizationHeader);
        if (isEmpty(tokenString)) {
            throw new NotAuthorizedException("Bearer schema is present but token is empty",
                    TokenUtils.BEARER_AUTH_SCHEMA);
        }

        final Message message = getCurrentMessage();
        final RequestSecurityTokenResponseType response = validateOperation.validate(
                createValidateRequestSecurityTokenType(tokenString, getTokenType()), null,
                message);
        if (validateResponse(response)) {
            createSecurityContext(context, tokenString);
        } else {
            throw new NotAuthorizedException("Bearer token validation is failed", TokenUtils.BEARER_AUTH_SCHEMA);
        }
    }

    protected abstract void createSecurityContext(ContainerRequestContext context, String token);

    protected abstract String getTokenType();

    protected abstract boolean isTokenBasedAuthentication(String authorizationHeader);

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
