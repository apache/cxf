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
package org.apache.cxf.rs.security.oidc.idp;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.OAuthRedirectionState;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.services.AuthorizationCodeGrantService;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

public class OidcAuthorizationCodeService extends AuthorizationCodeGrantService {

    @Override
    protected boolean canAuthorizationBeSkipped(MultivaluedMap<String, String> params,
                                                Client client,
                                                UserSubject userSubject,
                                                List<String> requestedScope,
                                                List<OAuthPermission> permissions) {
        List<String> promptValues = OidcUtils.getPromptValues(params);
        if (promptValues.contains(OidcUtils.PROMPT_CONSENT_VALUE)) {
            // Displaying the consent screen is preferred by the client
            return false;
        }
        // Check the pre-configured consent
        boolean preConfiguredConsentForScopes =
            super.canAuthorizationBeSkipped(params, client, userSubject, requestedScope, permissions);

        if (!preConfiguredConsentForScopes && promptValues.contains(OidcUtils.PROMPT_NONE_VALUE)) {
            // An error is returned if client does not have pre-configured consent for the requested scopes/claims
            LOG.log(Level.FINE, "Prompt 'none' request can not be met");
            throw new OAuthServiceException(new OAuthError(OidcUtils.CONSENT_REQUIRED_ERROR));
        }
        return preConfiguredConsentForScopes;
    }

    public void setSkipAuthorizationWithOidcScope(boolean skipAuthorizationWithOidcScope) {
        super.setScopesRequiringNoConsent(Collections.singletonList(OidcUtils.OPENID_SCOPE));
    }

    @Override
    protected Response startAuthorization(MultivaluedMap<String, String> params,
                                          UserSubject userSubject,
                                          Client client,
                                          String redirectUri) {
        // Validate the prompt - if it contains "none" then an error is returned with any other value
        List<String> promptValues = OidcUtils.getPromptValues(params);
        if (promptValues != null && promptValues.size() > 1 && promptValues.contains(OidcUtils.PROMPT_NONE_VALUE)) {
            LOG.log(Level.FINE, "The prompt value {} is invalid", params.getFirst(OidcUtils.PROMPT_PARAMETER));
            return createErrorResponse(params, redirectUri, OAuthConstants.INVALID_REQUEST);
        }

        return super.startAuthorization(params, userSubject, client, redirectUri);
    }

    @Override
    protected OAuthRedirectionState recreateRedirectionStateFromParams(
        MultivaluedMap<String, String> params) {
        OAuthRedirectionState state = super.recreateRedirectionStateFromParams(params);
        OidcUtils.setStateClaimsProperty(state, params);
        return state;
    }
}
