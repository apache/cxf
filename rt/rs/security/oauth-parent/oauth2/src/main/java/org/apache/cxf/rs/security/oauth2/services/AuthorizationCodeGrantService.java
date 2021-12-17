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

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.FormAuthorizationResponse;
import org.apache.cxf.rs.security.oauth2.common.OAuthRedirectionState;
import org.apache.cxf.rs.security.oauth2.common.OOBAuthorizationResponse;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeDataProvider;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeRegistration;
import org.apache.cxf.rs.security.oauth2.grants.code.ServerAuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.provider.AuthorizationCodeResponseFilter;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.provider.OOBResponseDeliverer;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;


/**
 * This resource handles the End User authorizing
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
    private AuthorizationCodeResponseFilter codeResponseFilter;

    public AuthorizationCodeGrantService() {
        super(OAuthConstants.CODE_RESPONSE_TYPE, OAuthConstants.AUTHORIZATION_CODE_GRANT);
    }

    protected OAuthRedirectionState recreateRedirectionStateFromParams(MultivaluedMap<String, String> params) {
        OAuthRedirectionState state = super.recreateRedirectionStateFromParams(params);
        state.setClientCodeChallenge(params.getFirst(OAuthConstants.AUTHORIZATION_CODE_CHALLENGE));
        state.setClientCodeChallengeMethod(params.getFirst(OAuthConstants.AUTHORIZATION_CODE_CHALLENGE_METHOD));
        return state;
    }

    protected Response createGrant(OAuthRedirectionState state,
                                   Client client,
                                   List<String> requestedScope,
                                   List<String> approvedScope,
                                   UserSubject userSubject,
                                   ServerAccessToken preauthorizedToken) {
        // in this flow the code is still created, the preauthorized token
        // will be retrieved by the authorization code grant handler
        final ServerAuthorizationCodeGrant grant;
        try {
            grant = getGrantRepresentation(state,
                                           client,
                                           requestedScope,
                                           approvedScope,
                                           userSubject,
                                           preauthorizedToken);
        } catch (OAuthServiceException ex) {
            return createErrorResponse(state.getState(), state.getRedirectUri(), OAuthConstants.ACCESS_DENIED);
        }
        String grantCode = processCodeGrant(client, grant.getCode(), grant.getSubject());
        if (state.getRedirectUri() == null) {
            OOBAuthorizationResponse bean = new OOBAuthorizationResponse();
            bean.setClientId(client.getClientId());
            bean.setClientDescription(client.getApplicationDescription());
            bean.setAuthorizationCode(grantCode);
            bean.setUserId(userSubject.getLogin());
            bean.setExpiresIn(grant.getExpiresIn());
            return deliverOOBResponse(bean);
        } else if (isFormResponse(state)) {
            FormAuthorizationResponse bean = new FormAuthorizationResponse();
            bean.setAuthorizationCode(grantCode);
            bean.setExpiresIn(grant.getExpiresIn());
            bean.setState(state.getState());
            bean.setRedirectUri(state.getRedirectUri());
            return createHtmlResponse(bean);
        } else {
            // return the code by appending it as a query parameter to the redirect URI
            UriBuilder ub = getRedirectUriBuilder(state.getState(), state.getRedirectUri());
            ub.queryParam(OAuthConstants.AUTHORIZATION_CODE_VALUE, grantCode);
            return Response.seeOther(ub.build()).build();
        }
    }

    public ServerAuthorizationCodeGrant getGrantRepresentation(OAuthRedirectionState state,
                           Client client,
                           List<String> requestedScope,
                           List<String> approvedScope,
                           UserSubject userSubject,
                           ServerAccessToken preauthorizedToken) {
        AuthorizationCodeRegistration codeReg = createCodeRegistration(state,
                                                                       client,
                                                                       requestedScope,
                                                                       approvedScope,
                                                                       userSubject,
                                                                       preauthorizedToken);

        ServerAuthorizationCodeGrant grant =
            ((AuthorizationCodeDataProvider)getDataProvider()).createCodeGrant(codeReg);
        if (grant.getExpiresIn() > RECOMMENDED_CODE_EXPIRY_TIME_SECS) {
            LOG.warning("Code expiry time exceeds 10 minutes");
        }
        return grant;
    }

    protected AuthorizationCodeRegistration createCodeRegistration(OAuthRedirectionState state,
                                                                   Client client,
                                                                   List<String> requestedScope,
                                                                   List<String> approvedScope,
                                                                   UserSubject userSubject,
                                                                   ServerAccessToken preauthorizedToken) {
        AuthorizationCodeRegistration codeReg = new AuthorizationCodeRegistration();
        codeReg.setPreauthorizedTokenAvailable(preauthorizedToken != null);
        codeReg.setClient(client);
        codeReg.setRedirectUri(state.getRedirectUri());
        codeReg.setRequestedScope(requestedScope);
        codeReg.setResponseType(state.getResponseType());
        codeReg.setApprovedScope(getApprovedScope(requestedScope, approvedScope));
        codeReg.setSubject(userSubject);
        codeReg.setAudience(state.getAudience());
        codeReg.setNonce(state.getNonce());
        codeReg.setClientCodeChallenge(state.getClientCodeChallenge());
        codeReg.setClientCodeChallengeMethod(state.getClientCodeChallengeMethod());
        codeReg.getExtraProperties().putAll(state.getExtraProperties());
        return codeReg;
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
        }
        return createHtmlResponse(response);
    }

    protected Response createErrorResponse(String state,
                                           String redirectUri,
                                           String error) {
        if (redirectUri == null) {
            return Response.status(401).entity(error).build();
        }
        UriBuilder ub = getRedirectUriBuilder(state, redirectUri);
        ub.queryParam(OAuthConstants.ERROR_KEY, error);
        return Response.seeOther(ub.build()).build();
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
        return c.isConfidential() && canSupportEmptyRedirectForPrivateClients;
    }

    public void setCanSupportPublicClients(boolean support) {
        this.canSupportPublicClients = support;
    }

    public void setCodeResponseFilter(AuthorizationCodeResponseFilter filter) {
        this.codeResponseFilter = filter;
    }
    public void setCanSupportEmptyRedirectForPrivateClients(boolean canSupportEmptyRedirectForPrivateClients) {
        this.canSupportEmptyRedirectForPrivateClients = canSupportEmptyRedirectForPrivateClients;
    }


}


