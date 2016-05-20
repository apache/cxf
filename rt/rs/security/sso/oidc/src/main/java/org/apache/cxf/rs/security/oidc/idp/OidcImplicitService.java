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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
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
    private boolean skipAuthorizationWithOidcScope;
    private OAuthJoseJwtProducer idTokenHandler;
    private IdTokenProvider idTokenProvider;
    
    public OidcImplicitService() {
        super(new HashSet<String>(Arrays.asList(OidcUtils.ID_TOKEN_RESPONSE_TYPE,
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
                                          Client client) {    
        // Validate the nonce, it must be present for the Implicit flow
        if (params.getFirst(OAuthConstants.NONCE) == null) {
            LOG.fine("A nonce is required for the Implicit flow");
            throw new OAuthServiceException(new OAuthError(OAuthConstants.INVALID_REQUEST));
        }
        return super.startAuthorization(params, userSubject, client);
    }
    
    @Override
    protected boolean canAuthorizationBeSkipped(Client client,
                                                UserSubject userSubject,
                                                List<String> requestedScope,
                                                List<OAuthPermission> permissions) {
        // No need to challenge the authenticated user with the authorization form 
        // if all the client application redirecting a user needs is to get this user authenticated
        // with OIDC IDP
        return requestedScope.size() == 1 && permissions.size() == 1 && skipAuthorizationWithOidcScope
            && OidcUtils.OPENID_SCOPE.equals(requestedScope.get(0));
    }
    public void setSkipAuthorizationWithOidcScope(boolean skipAuthorizationWithOidcScope) {
        this.skipAuthorizationWithOidcScope = skipAuthorizationWithOidcScope;
    }
    
    @Override
    protected StringBuilder prepareGrant(OAuthRedirectionState state,
                                   Client client,
                                   List<String> requestedScope,
                                   List<String> approvedScope,
                                   UserSubject userSubject,
                                   ServerAccessToken preAuthorizedToken) {
        
        if (canAccessTokenBeReturned(state.getResponseType())) {
            return super.prepareGrant(state, client, requestedScope, approvedScope, userSubject, preAuthorizedToken);
        }
        // id_token response type processing
        
        StringBuilder sb = getUriWithFragment(state.getRedirectUri());
        
        String idToken = getProcessedIdToken(state, userSubject, 
                                             getApprovedScope(requestedScope, approvedScope));
        if (idToken != null) {
            sb.append(OidcUtils.ID_TOKEN).append("=").append(idToken);
        }
        finalizeResponse(sb, state);
        return sb;
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
    
    @Override
    protected AccessTokenRegistration createTokenRegistration(OAuthRedirectionState state, 
                                                              Client client, 
                                                              List<String> requestedScope, 
                                                              List<String> approvedScope, 
                                                              UserSubject userSubject) {
        AccessTokenRegistration reg = 
            super.createTokenRegistration(state, client, requestedScope, approvedScope, userSubject);
        reg.getExtraProperties().putAll(state.getExtraProperties());
        return reg;
    }
    
    protected String processIdToken(OAuthRedirectionState state, IdToken idToken) {
        OAuthJoseJwtProducer processor = idTokenHandler == null ? new OAuthJoseJwtProducer() : idTokenHandler; 
        
        String code = 
            (String)JAXRSUtils.getCurrentMessage().getExchange().get(OAuthConstants.AUTHORIZATION_CODE_VALUE);
        if (code != null) {
            // this service is invoked as part of the hybrid flow
            Properties props = JwsUtils.loadSignatureOutProperties(false);
            SignatureAlgorithm sigAlgo = null;
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
