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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.AbstractFormImplicitResponse;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.OAuthRedirectionState;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJoseJwtProducer;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.services.ImplicitGrantService;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;


public class OidcImplicitService extends ImplicitGrantService {
    private OAuthJoseJwtProducer idTokenHandler;
    private IdTokenProvider idTokenProvider;

    public OidcImplicitService() {
        super(new HashSet<>(Arrays.asList(OidcUtils.ID_TOKEN_RESPONSE_TYPE,
                                                OidcUtils.ID_TOKEN_AT_RESPONSE_TYPE)));
    }
    protected OidcImplicitService(Set<String> supportedResponseTypes,
                                  String supportedGrantType) {
        super(supportedResponseTypes, supportedGrantType);
    }
    @Override
    protected boolean canAccessTokenBeReturned(String responseType) {
        return OidcUtils.ID_TOKEN_AT_RESPONSE_TYPE.equals(responseType);
    }

    @Override
    protected Response startAuthorization(MultivaluedMap<String, String> params,
                                          UserSubject userSubject,
                                          Client client,
                                          String redirectUri) {
        // Validate the nonce, it must be present for the Implicit flow
        if (params.getFirst(OAuthConstants.NONCE) == null) {
            LOG.fine("A nonce is required for the Implicit flow");
            return createErrorResponse(params, redirectUri, OAuthConstants.INVALID_REQUEST);
        }

        // Validate the prompt - if it contains "none" then an error is returned with any other value
        List<String> promptValues = OidcUtils.getPromptValues(params);
        if (promptValues.size() > 1 && promptValues.contains(OidcUtils.PROMPT_NONE_VALUE)) {
            LOG.log(Level.FINE, "The prompt value {} is invalid", params.getFirst(OidcUtils.PROMPT_PARAMETER));
            return createErrorResponse(params, redirectUri, OAuthConstants.INVALID_REQUEST);
        }

        return super.startAuthorization(params, userSubject, client, redirectUri);
    }

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
    protected StringBuilder prepareRedirectResponse(OAuthRedirectionState state,
                                   Client client,
                                   List<String> requestedScope,
                                   List<String> approvedScope,
                                   UserSubject userSubject,
                                   ServerAccessToken preAuthorizedToken) {

        if (canAccessTokenBeReturned(state.getResponseType())) {
            return super.prepareRedirectResponse(state, client, requestedScope, approvedScope,
                                                 userSubject, preAuthorizedToken);
        }
        // id_token response type processing

        StringBuilder sb = getUriWithFragment(state.getRedirectUri());

        String idToken = getProcessedIdToken(state, userSubject,
                                             getApprovedScope(requestedScope, approvedScope));
        if (idToken != null) {
            sb.append(OidcUtils.ID_TOKEN).append('=').append(idToken);
        } else if (state.getResponseType().contains(OidcUtils.ID_TOKEN_RESPONSE_TYPE)) {
            LOG.warning("No IdToken available. Did you configure a IdTokenProvider implementation?");
            throw ExceptionUtils.toInternalServerErrorException(null, null);
        }

        finalizeResponse(sb, state);
        return sb;
    }

    @Override
    protected AbstractFormImplicitResponse prepareFormResponse(OAuthRedirectionState state,
                                                Client client,
                                                List<String> requestedScope,
                                                List<String> approvedScope,
                                                UserSubject userSubject,
                                                ServerAccessToken preAuthorizedToken) {
        if (canAccessTokenBeReturned(state.getResponseType())) {
            return super.prepareFormResponse(state, client, requestedScope, approvedScope,
                                                  userSubject, preAuthorizedToken);
        }
        // id_token response type processing
        String idToken = getProcessedIdToken(state, userSubject,
                                             getApprovedScope(requestedScope, approvedScope));
        if (idToken == null && state.getResponseType().contains(OidcUtils.ID_TOKEN_RESPONSE_TYPE)) {
            LOG.warning("No IdToken available. Did you configure a IdTokenProvider implementation?");
            throw ExceptionUtils.toInternalServerErrorException(null, null);
        }
        FormIdTokenResponse response = new FormIdTokenResponse();
        response.setIdToken(idToken);
        response.setResponseType(state.getResponseType());
        response.setRedirectUri(state.getRedirectUri());
        response.setState(state.getState());
        return response;
    }

    private String getProcessedIdToken(OAuthRedirectionState state,
                                       UserSubject subject,
                                       List<String> scopes) {
        if (subject.getProperties().containsKey(OidcUtils.ID_TOKEN)) {
            return subject.getProperties().get(OidcUtils.ID_TOKEN);
        } else if (idTokenProvider != null) {
            IdToken idToken = idTokenProvider.getIdToken(state.getClientId(), subject, scopes);
            return processIdToken(state, idToken);
        } else if (subject instanceof OidcUserSubject) {
            OidcUserSubject sub = (OidcUserSubject)subject;
            IdToken idToken = new IdToken(sub.getIdToken());
            idToken.setAudience(state.getClientId());
            idToken.setAuthorizedParty(state.getClientId());
            return processIdToken(state, idToken);
        } else {
            return null;
        }
    }

    @Override
    protected OAuthRedirectionState recreateRedirectionStateFromParams(
        MultivaluedMap<String, String> params) {
        OAuthRedirectionState state = super.recreateRedirectionStateFromParams(params);
        OidcUtils.setStateClaimsProperty(state, params);
        return state;
    }


    protected String processIdToken(OAuthRedirectionState state, IdToken idToken) {
        OAuthJoseJwtProducer processor = idTokenHandler == null ? new OAuthJoseJwtProducer() : idTokenHandler;

        String code =
            (String)JAXRSUtils.getCurrentMessage().getExchange().get(OAuthConstants.AUTHORIZATION_CODE_VALUE);
        if (code != null) {
            // this service is invoked as part of the hybrid flow
            Properties props = JwsUtils.loadSignatureOutProperties(false);
            final SignatureAlgorithm sigAlgo;
            if (processor.isSignWithClientSecret()) {
                sigAlgo = OAuthUtils.getClientSecretSignatureAlgorithm(props);
            } else {
                sigAlgo = JwsUtils.getSignatureAlgorithm(props, SignatureAlgorithm.RS256);
            }
            idToken.setAuthorizationCodeHash(OidcUtils.calculateAuthorizationCodeHash(code, sigAlgo));
        }

        idToken.setNonce(state.getNonce());
        return processor.processJwt(new JwtToken(idToken));
    }

    public void setIdTokenJoseHandler(OAuthJoseJwtProducer idTokenJoseHandler) {
        this.idTokenHandler = idTokenJoseHandler;
    }
    public void setIdTokenProvider(IdTokenProvider idTokenProvider) {
        this.idTokenProvider = idTokenProvider;
    }
}
