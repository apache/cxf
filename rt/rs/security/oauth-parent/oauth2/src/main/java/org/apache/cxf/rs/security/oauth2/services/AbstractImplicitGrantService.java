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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.common.AbstractFormImplicitResponse;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.FormTokenResponse;
import org.apache.cxf.rs.security.oauth2.common.OAuthRedirectionState;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenResponseFilter;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;


public abstract class AbstractImplicitGrantService extends RedirectionBasedGrantService {
    // For a client to validate that this client is a targeted recipient.
    private boolean reportClientId;
    private List<AccessTokenResponseFilter> responseHandlers = new LinkedList<>();

    protected AbstractImplicitGrantService(String supportedResponseType,
                                           String supportedGrantType) {
        super(supportedResponseType, supportedGrantType);
    }
    protected AbstractImplicitGrantService(Set<String> supportedResponseTypes,
                                           String supportedGrantType) {
        super(supportedResponseTypes, supportedGrantType);
    }

    protected Response createGrant(OAuthRedirectionState state,
                                   Client client,
                                   List<String> requestedScope,
                                   List<String> approvedScope,
                                   UserSubject userSubject,
                                   ServerAccessToken preAuthorizedToken) {
        if (isFormResponse(state)) {
            return createHtmlResponse(prepareFormResponse(state, client, requestedScope,
                                            approvedScope, userSubject, preAuthorizedToken));
        }
        StringBuilder sb =
            prepareRedirectResponse(state, client, requestedScope, approvedScope, userSubject, preAuthorizedToken);
        return Response.seeOther(URI.create(sb.toString())).build();
    }

    protected StringBuilder prepareRedirectResponse(OAuthRedirectionState state,
                                          Client client,
                                          List<String> requestedScope,
                                          List<String> approvedScope,
                                          UserSubject userSubject,
                                          ServerAccessToken preAuthorizedToken) {

        ClientAccessToken clientToken =
            getClientAccessToken(state, client, requestedScope, approvedScope, userSubject, preAuthorizedToken);
        // return the token by appending it as a fragment parameter to the redirect URI

        StringBuilder sb = getUriWithFragment(state.getRedirectUri());

        sb.append(OAuthConstants.ACCESS_TOKEN).append('=').append(clientToken.getTokenKey());
        sb.append('&');
        sb.append(OAuthConstants.ACCESS_TOKEN_TYPE).append('=').append(clientToken.getTokenType());

        if (isWriteOptionalParameters()) {
            sb.append('&').append(OAuthConstants.ACCESS_TOKEN_EXPIRES_IN)
                .append('=').append(clientToken.getExpiresIn());
            if (!StringUtils.isEmpty(clientToken.getApprovedScope())) {
                sb.append('&').append(OAuthConstants.SCOPE).append('=')
                    .append(HttpUtils.queryEncode(clientToken.getApprovedScope()));
            }
            for (Map.Entry<String, String> entry : clientToken.getParameters().entrySet()) {
                sb.append('&').append(entry.getKey()).append('=').append(HttpUtils.queryEncode(entry.getValue()));
            }
        }
        if (clientToken.getRefreshToken() != null) {
            processRefreshToken(sb, clientToken.getRefreshToken());
        }

        finalizeResponse(sb, state);
        return sb;
    }

    protected AbstractFormImplicitResponse prepareFormResponse(OAuthRedirectionState state,
                                           Client client,
                                           List<String> requestedScope,
                                           List<String> approvedScope,
                                           UserSubject userSubject,
                                           ServerAccessToken preAuthorizedToken) {

        ClientAccessToken clientToken =
            getClientAccessToken(state, client, requestedScope, approvedScope, userSubject, preAuthorizedToken);

        FormTokenResponse bean = new FormTokenResponse();
        bean.setResponseType(OAuthConstants.TOKEN_RESPONSE_TYPE);
        bean.setRedirectUri(state.getRedirectUri());
        bean.setState(state.getState());
        bean.setAccessToken(clientToken.getTokenKey());
        bean.setAccessTokenType(clientToken.getTokenType());
        bean.setAccessTokenExpiresIn(clientToken.getExpiresIn());
        bean.getParameters().putAll(clientToken.getParameters());
        return bean;
    }

    protected ClientAccessToken getClientAccessToken(OAuthRedirectionState state,
                                                     Client client,
                                                     List<String> requestedScope,
                                                     List<String> approvedScope,
                                                     UserSubject userSubject,
                                                     ServerAccessToken preAuthorizedToken) {

        final ServerAccessToken token;
        if (preAuthorizedToken == null) {
            AccessTokenRegistration reg = createTokenRegistration(state,
                                                                  client,
                                                                  requestedScope,
                                                                  approvedScope,
                                                                  userSubject);
            token = getDataProvider().createAccessToken(reg);
        } else {
            token = preAuthorizedToken;
            if (state.getNonce() != null) {
                JAXRSUtils.getCurrentMessage().getExchange().put(OAuthConstants.NONCE, state.getNonce());
            }
        }

        ClientAccessToken clientToken = OAuthUtils.toClientAccessToken(token, isWriteOptionalParameters());
        processClientAccessToken(clientToken, token);
        return clientToken;
    }

    protected AccessTokenRegistration createTokenRegistration(OAuthRedirectionState state,
                                                              Client client,
                                                              List<String> requestedScope,
                                                              List<String> approvedScope,
                                                              UserSubject userSubject) {
        AccessTokenRegistration reg = new AccessTokenRegistration();
        reg.setClient(client);
        reg.setGrantType(super.getSupportedGrantType());
        reg.setResponseType(state.getResponseType());
        reg.setSubject(userSubject);
        reg.setRequestedScope(requestedScope);
        reg.setApprovedScope(getApprovedScope(requestedScope, approvedScope));
        reg.setAudiences(Collections.singletonList(state.getAudience()));
        reg.setNonce(state.getNonce());
        reg.getExtraProperties().putAll(state.getExtraProperties());
        return reg;
    }
    protected void finalizeResponse(StringBuilder sb, OAuthRedirectionState state) {
        if (state.getState() != null) {
            sb.append('&');
            String stateParam = state.getState();
            sb.append(OAuthConstants.STATE).append('=').append(HttpUtils.urlEncode(stateParam));
        }
        if (reportClientId) {
            sb.append('&').append(OAuthConstants.CLIENT_ID).append('=').append(state.getClientId());
        }
    }

    protected void processRefreshToken(StringBuilder sb, String refreshToken) {
        LOG.warning("Implicit grant tokens MUST not have refresh tokens, refresh token will not be reported");
    }

    protected void processClientAccessToken(ClientAccessToken clientToken, ServerAccessToken serverToken) {
        for (AccessTokenResponseFilter filter : responseHandlers) {
            filter.process(clientToken, serverToken);
        }
    }
    protected Response createErrorResponse(String state,
                                           String redirectUri,
                                           String error) {
        StringBuilder sb = getUriWithFragment(redirectUri);
        sb.append(OAuthConstants.ERROR_KEY).append('=').append(error);
        if (state != null) {
            sb.append('&');
            sb.append(OAuthConstants.STATE).append('=').append(state);
        }

        return Response.seeOther(URI.create(sb.toString())).build();
    }

    protected StringBuilder getUriWithFragment(String redirectUri) {
        StringBuilder sb = new StringBuilder();
        sb.append(redirectUri);
        sb.append('#');
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


