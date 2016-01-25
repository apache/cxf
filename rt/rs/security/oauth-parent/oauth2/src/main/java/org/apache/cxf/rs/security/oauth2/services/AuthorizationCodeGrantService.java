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

package org.apache.cxf.rs.security.oauth2.services;

import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.OAuthRedirectionState;
import org.apache.cxf.rs.security.oauth2.common.OOBAuthorizationResponse;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeDataProvider;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeRegistration;
import org.apache.cxf.rs.security.oauth2.grants.code.ServerAuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.provider.AuthorizationCodeRequestFilter;
import org.apache.cxf.rs.security.oauth2.provider.AuthorizationCodeResponseFilter;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.provider.OOBResponseDeliverer;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;


/**
 * This resource handles the End User authorising
 * or denying the Client to access its resources.
 * If End User approves the access this resource will
 * redirect End User back to the Client, supplying 
 * the authorization code.
 */
@Path("/authorize")
public class AuthorizationCodeGrantService extends RedirectionBasedGrantService {
    private static final long RECOMMENDED_CODE_EXPIRY_TIME_SECS = 10L * 60L;
    private boolean canSupportPublicClients;
    private boolean canSupportEmptyRedirectForPrivateClients;
    private OOBResponseDeliverer oobDeliverer;
    private AuthorizationCodeRequestFilter codeRequestFilter;
    private AuthorizationCodeResponseFilter codeResponseFilter;
    
    public AuthorizationCodeGrantService() {
        super(OAuthConstants.CODE_RESPONSE_TYPE, OAuthConstants.AUTHORIZATION_CODE_GRANT);
    }
    @Override
    protected OAuthAuthorizationData createAuthorizationData(Client client, 
                                                             MultivaluedMap<String, String> params,
                                                             String redirectUri,
                                                             UserSubject subject,
                                                             List<String> requestedScopes,
                                                             List<OAuthPermission> perms,
                                                             boolean authorizationCanBeSkipped) {
        OAuthAuthorizationData data = 
            super.createAuthorizationData(client, params, redirectUri, subject, 
                                          requestedScopes, perms, authorizationCanBeSkipped);
        setCodeQualifier(data, params);
        return data;
    }
    protected OAuthRedirectionState recreateRedirectionStateFromSession(
        UserSubject subject, MultivaluedMap<String, String> params, String sessionToken) {
        OAuthRedirectionState state = super.recreateRedirectionStateFromSession(subject, params, sessionToken);
        setCodeQualifier(state, params);
        return state;
    }
    private static void setCodeQualifier(OAuthRedirectionState data, MultivaluedMap<String, String> params) {
        data.setClientCodeChallenge(params.getFirst(OAuthConstants.AUTHORIZATION_CODE_CHALLENGE));
    }
    protected Response startAuthorization(MultivaluedMap<String, String> params, 
                                          UserSubject userSubject,
                                          Client client) {
        if (codeRequestFilter != null) {
            params = codeRequestFilter.process(params, userSubject, client);
        }
        return super.startAuthorization(params, userSubject, client);
    }
    protected Response createGrant(OAuthRedirectionState state,
                                   Client client,
                                   List<String> requestedScope,
                                   List<String> approvedScope,
                                   UserSubject userSubject,
                                   ServerAccessToken preauthorizedToken) {
        // in this flow the code is still created, the preauthorized token
        // will be retrieved by the authorization code grant handler
        AuthorizationCodeRegistration codeReg = new AuthorizationCodeRegistration(); 
        codeReg.setPreauthorizedTokenAvailable(preauthorizedToken != null);
        codeReg.setClient(client);
        codeReg.setRedirectUri(state.getRedirectUri());
        codeReg.setRequestedScope(requestedScope);
        if (approvedScope == null || approvedScope.isEmpty()) {
            // no down-scoping done by a user, all of the requested scopes have been authorized
            codeReg.setApprovedScope(requestedScope);
        } else {
            codeReg.setApprovedScope(approvedScope);
        }
        codeReg.setSubject(userSubject);
        codeReg.setAudience(state.getAudience());
        codeReg.setNonce(state.getNonce());
        codeReg.setClientCodeChallenge(state.getClientCodeChallenge());
        
        ServerAuthorizationCodeGrant grant = null;
        try {
            grant = ((AuthorizationCodeDataProvider)getDataProvider()).createCodeGrant(codeReg);
        } catch (OAuthServiceException ex) {
            return createErrorResponse(state.getState(), state.getRedirectUri(), OAuthConstants.ACCESS_DENIED);
        }
        if (grant.getExpiresIn() > RECOMMENDED_CODE_EXPIRY_TIME_SECS) {
            LOG.warning("Code expiry time exceeds 10 minutes");
        }
        String grantCode = processCodeGrant(client, grant.getCode(), grant.getSubject());
        if (state.getRedirectUri() == null) {
            OOBAuthorizationResponse oobResponse = new OOBAuthorizationResponse();
            oobResponse.setClientId(client.getClientId());
            oobResponse.setClientDescription(client.getApplicationDescription());
            oobResponse.setAuthorizationCode(grant.getCode());
            oobResponse.setUserId(userSubject.getLogin());
            oobResponse.setExpiresIn(grant.getExpiresIn());
            return deliverOOBResponse(oobResponse);
        } else {
            // return the code by appending it as a query parameter to the redirect URI
            UriBuilder ub = getRedirectUriBuilder(state.getState(), state.getRedirectUri());
            ub.queryParam(OAuthConstants.AUTHORIZATION_CODE_VALUE, grantCode);
            return Response.seeOther(ub.build()).build();
        }
    }
    protected String processCodeGrant(Client client, String code, UserSubject endUser) {
        if (codeResponseFilter != null) {
            return codeResponseFilter.process(client, code, endUser);
        }
        return code;
    }
    protected Response deliverOOBResponse(OOBAuthorizationResponse response) {
        if (oobDeliverer != null) {    
            return oobDeliverer.deliver(response);
        } else {
            return Response.ok(response).type(MediaType.TEXT_HTML).build();
        }
    }
    
    protected Response createErrorResponse(String state,
                                           String redirectUri,
                                           String error) {
        if (redirectUri == null) {
            return Response.status(401).entity(error).build();
        } else {
            UriBuilder ub = getRedirectUriBuilder(state, redirectUri);
            ub.queryParam(OAuthConstants.ERROR_KEY, error);
            return Response.seeOther(ub.build()).build();
        }
    }
    
    protected UriBuilder getRedirectUriBuilder(String state, String redirectUri) {
        UriBuilder ub = UriBuilder.fromUri(redirectUri);
        if (state != null) { 
            ub.queryParam(OAuthConstants.STATE, state);
        }
        return ub;
    }

    @Override
    protected boolean canSupportPublicClient(Client c) {
        return canSupportPublicClients && !c.isConfidential() && c.getClientSecret() == null;
    }

    @Override
    protected boolean canRedirectUriBeEmpty(Client c) {
        // If a redirect URI is empty then the code will be returned out of band, 
        // typically will be returned directly to a human user
        return (c.isConfidential() && canSupportEmptyRedirectForPrivateClients || canSupportPublicClient(c)) 
                && c.getRedirectUris().isEmpty();
    }
    
    public void setCanSupportPublicClients(boolean support) {
        this.canSupportPublicClients = support;
    }

    public void setCodeResponseFilter(AuthorizationCodeResponseFilter filter) {
        this.codeResponseFilter = filter;
    }

    public void setCodeRequestFilter(AuthorizationCodeRequestFilter codeRequestFilter) {
        this.codeRequestFilter = codeRequestFilter;
    }
    public void setCanSupportEmptyRedirectForPrivateClients(boolean canSupportEmptyRedirectForPrivateClients) {
        this.canSupportEmptyRedirectForPrivateClients = canSupportEmptyRedirectForPrivateClients;
    }
    
    
}


