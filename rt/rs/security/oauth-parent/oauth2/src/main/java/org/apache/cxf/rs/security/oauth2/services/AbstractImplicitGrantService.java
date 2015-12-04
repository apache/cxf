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

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;


import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenResponseFilter;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;


public abstract class AbstractImplicitGrantService extends RedirectionBasedGrantService {
    // For a client to validate that this client is a targeted recipient.
    private boolean reportClientId;
    private List<AccessTokenResponseFilter> responseHandlers = new LinkedList<AccessTokenResponseFilter>();
    
    protected AbstractImplicitGrantService(String supportedResponseType,
                                           String supportedGrantType) {
        super(supportedResponseType, supportedGrantType);
    }
    
    protected Response createGrant(MultivaluedMap<String, String> params,
                                   Client client,
                                   String redirectUri,
                                   List<String> requestedScope,
                                   List<String> approvedScope,
                                   UserSubject userSubject,
                                   ServerAccessToken preAuthorizedToken) {
        ServerAccessToken token = null;
        if (preAuthorizedToken == null) {
<<<<<<< HEAD
            AccessTokenRegistration reg = new AccessTokenRegistration();
            reg.setClient(client);
            reg.setGrantType(OAuthConstants.IMPLICIT_GRANT);
            reg.setSubject(userSubject);
            reg.setRequestedScope(requestedScope);        
            if (approvedScope != null && approvedScope.isEmpty()) {
                // no down-scoping done by a user, all of the requested scopes have been authorized
                reg.setApprovedScope(requestedScope);
            } else {
                reg.setApprovedScope(approvedScope);
=======
            tokenCanBeReturned = canAccessTokenBeReturned(requestedScope, approvedScope);
            if (tokenCanBeReturned) {
                AccessTokenRegistration reg = new AccessTokenRegistration();
                reg.setClient(client);
                reg.setGrantType(super.getSupportedGrantType());
                reg.setSubject(userSubject);
                reg.setRequestedScope(requestedScope);        
                if (approvedScope == null || approvedScope.isEmpty()) {
                    // no down-scoping done by a user, all of the requested scopes have been authorized
                    reg.setApprovedScope(requestedScope);
                } else {
                    reg.setApprovedScope(approvedScope);
                }
                reg.setAudience(state.getAudience());
                reg.setNonce(state.getNonce());
                token = getDataProvider().createAccessToken(reg);
>>>>>>> b7d3336... Add equals/hashCode methods for OAuthPermission so that the containsAll call in AbstractOAuthDataProvider.doRefreshAccessToken works
            }
            reg.setAudience(params.getFirst(OAuthConstants.CLIENT_AUDIENCE));
            token = getDataProvider().createAccessToken(reg);
        } else {
            token = preAuthorizedToken;
        }
        
        ClientAccessToken clientToken = OAuthUtils.toClientAccessToken(token, isWriteOptionalParameters());
        processClientAccessToken(clientToken, token);
   
        // return the token by appending it as a fragment parameter to the redirect URI
        
        StringBuilder sb = getUriWithFragment(redirectUri);
        
        sb.append(OAuthConstants.ACCESS_TOKEN).append("=").append(clientToken.getTokenKey());
        String state = params.getFirst(OAuthConstants.STATE);
        if (state != null) {
            sb.append("&");
            sb.append(OAuthConstants.STATE).append("=").append(state);   
        }
        sb.append("&")
            .append(OAuthConstants.ACCESS_TOKEN_TYPE).append("=").append(clientToken.getTokenType());
        
        if (isWriteOptionalParameters()) {
            sb.append("&").append(OAuthConstants.ACCESS_TOKEN_EXPIRES_IN)
                .append("=").append(clientToken.getExpiresIn());
            if (!StringUtils.isEmpty(clientToken.getApprovedScope())) {
                sb.append("&").append(OAuthConstants.SCOPE).append("=")
                    .append(HttpUtils.queryEncode(clientToken.getApprovedScope()));
            }
            for (Map.Entry<String, String> entry : clientToken.getParameters().entrySet()) {
                sb.append("&").append(entry.getKey()).append("=").append(HttpUtils.queryEncode(entry.getValue()));
            }
        }
        if (token.getRefreshToken() != null) {
            processRefreshToken(sb, token.getRefreshToken());
        }
        if (reportClientId) {
            sb.append("&").append(OAuthConstants.CLIENT_ID).append("=").append(client.getClientId());
        }
        
        return Response.seeOther(URI.create(sb.toString())).build();
    }
    
    protected void processRefreshToken(StringBuilder sb, String refreshToken) {
        LOG.warning("Implicit grant tokens MUST not have refresh tokens, refresh token will not be reported");
    }

    protected void processClientAccessToken(ClientAccessToken clientToken, ServerAccessToken serverToken) {
        for (AccessTokenResponseFilter filter : responseHandlers) {
            filter.process(clientToken, serverToken); 
        }
    }
    protected Response createErrorResponse(MultivaluedMap<String, String> params,
                                          String redirectUri,
                                          String error) {
        StringBuilder sb = getUriWithFragment(redirectUri);
        sb.append(OAuthConstants.ERROR_KEY).append("=").append(error);
        String state = params.getFirst(OAuthConstants.STATE);
        if (state != null) {
            sb.append("&");
            sb.append(OAuthConstants.STATE).append("=").append(state);   
        }
        return Response.seeOther(URI.create(sb.toString())).build();
    }
    
    private StringBuilder getUriWithFragment(String redirectUri) {
        StringBuilder sb = new StringBuilder();
        sb.append(redirectUri);
        sb.append("#");
        return sb;
    }

    public void setReportClientId(boolean reportClientId) {
        this.reportClientId = reportClientId;
    }
    
    public void setResponseFilters(List<AccessTokenResponseFilter> handlers) {
        this.responseHandlers = handlers;
    }
    
    public void setResponseFilter(AccessTokenResponseFilter responseHandler) {
        responseHandlers.add(responseHandler);
    }

    @Override
    protected boolean canRedirectUriBeEmpty(Client c) {
        return false;
    }
    @Override
    protected boolean canSupportPublicClient(Client c) {
        return true;
    }
}


