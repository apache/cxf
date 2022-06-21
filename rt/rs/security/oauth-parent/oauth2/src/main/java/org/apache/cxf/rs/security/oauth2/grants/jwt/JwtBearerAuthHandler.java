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

package org.apache.cxf.rs.security.oauth2.grants.jwt;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.provider.FormEncodingProvider;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.jaxrs.JwtTokenSecurityContext;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.provider.ClientRegistrationProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServerJoseJwtConsumer;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.security.SecurityContext;

public class JwtBearerAuthHandler extends OAuthServerJoseJwtConsumer implements ContainerRequestFilter {
    private ClientRegistrationProvider clientProvider;
    private FormEncodingProvider<Form> provider = new FormEncodingProvider<>(true);
    private boolean validateAudience = true;

    public JwtBearerAuthHandler() {
    }

    @Override
    public void filter(ContainerRequestContext context) {
        Message message = JAXRSUtils.getCurrentMessage();
        Form form = readFormData(message);
        MultivaluedMap<String, String> formData = form.asMap();
        String assertionType = formData.getFirst(Constants.CLIENT_AUTH_ASSERTION_TYPE);
        String decodedAssertionType = assertionType != null ? HttpUtils.urlDecode(assertionType) : null;
        if (decodedAssertionType == null || !Constants.CLIENT_AUTH_JWT_BEARER.equals(decodedAssertionType)) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }

        String assertion = formData.getFirst(Constants.CLIENT_AUTH_ASSERTION_PARAM);
        if (assertion == null) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }

        String clientId = formData.getFirst(OAuthConstants.CLIENT_ID);

        Client client = null;
        if (clientId != null && clientProvider != null) {
            client = clientProvider.getClient(clientId);
            if (client == null) {
                throw ExceptionUtils.toNotAuthorizedException(null, null);
            }
            message.put(Client.class, client);
        }
        JwtToken token = super.getJwtToken(assertion, client);

        String subjectName = (String)token.getClaim(JwtConstants.CLAIM_SUBJECT);
        if (clientId != null && !clientId.equals(subjectName)) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        message.put(OAuthConstants.CLIENT_ID, subjectName);

        formData.remove(OAuthConstants.CLIENT_ID);
        formData.remove(Constants.CLIENT_AUTH_ASSERTION_PARAM);
        formData.remove(Constants.CLIENT_AUTH_ASSERTION_TYPE);

        SecurityContext securityContext = configureSecurityContext(token);
        if (securityContext != null) {
            JAXRSUtils.getCurrentMessage().put(SecurityContext.class, securityContext);
        }

        // restore input stream
        try {
            FormUtils.restoreForm(provider, form, message);
        } catch (Exception ex) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
    }

    protected SecurityContext configureSecurityContext(JwtToken token) {
        return new JwtTokenSecurityContext(token, null);
    }

    private Form readFormData(Message message) {
        try {
            return FormUtils.readForm(provider, message);
        } catch (Exception ex) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
    }

    @Override
    protected void validateToken(JwtToken jwt) {
        super.validateToken(jwt);

        // We must have an issuer
        if (jwt.getClaim(JwtConstants.CLAIM_ISSUER) == null) {
            throw new OAuthServiceException(OAuthConstants.INVALID_GRANT);
        }

        // We must have a Subject
        if (jwt.getClaim(JwtConstants.CLAIM_SUBJECT) == null) {
            throw new OAuthServiceException(OAuthConstants.INVALID_GRANT);
        }

        // We must have an Expiry
        if (jwt.getClaim(JwtConstants.CLAIM_EXPIRY) == null) {
            throw new OAuthServiceException(OAuthConstants.INVALID_GRANT);
        }

        JwtUtils.validateTokenClaims(jwt.getClaims(), getTtl(), getClockOffset(), isValidateAudience());
    }

    public void setClientProvider(ClientRegistrationProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    public boolean isValidateAudience() {
        return validateAudience;
    }

    public void setValidateAudience(boolean validateAudience) {
        this.validateAudience = validateAudience;
    }
}
