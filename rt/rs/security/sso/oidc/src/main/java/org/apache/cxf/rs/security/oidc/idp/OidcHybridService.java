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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.Path;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.common.AbstractFormImplicitResponse;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthRedirectionState;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.grants.code.ServerAuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;

@Path("authorize-hybrid")
public class OidcHybridService extends OidcImplicitService {
    private OidcAuthorizationCodeService codeService;

    public OidcHybridService() {
        this(false);
    }
    public OidcHybridService(boolean hybridOnly) {
        super(getResponseTypes(hybridOnly), OAuthConstants.IMPLICIT_GRANT);
    }

    private static Set<String> getResponseTypes(boolean hybridOnly) {
        List<String> types = new ArrayList<>();
        types.addAll(
            Arrays.asList(OidcUtils.CODE_AT_RESPONSE_TYPE,
                          OidcUtils.CODE_ID_TOKEN_RESPONSE_TYPE,
                          OidcUtils.CODE_ID_TOKEN_AT_RESPONSE_TYPE));
        if (!hybridOnly) {
            types.add(OidcUtils.ID_TOKEN_RESPONSE_TYPE);
            types.add(OidcUtils.ID_TOKEN_AT_RESPONSE_TYPE);
        }
        return new HashSet<>(types);
    }

    @Override
    protected boolean canAccessTokenBeReturned(String responseType) {
        return OidcUtils.ID_TOKEN_AT_RESPONSE_TYPE.equals(responseType)
            || OidcUtils.CODE_ID_TOKEN_AT_RESPONSE_TYPE.equals(responseType)
            || OidcUtils.CODE_AT_RESPONSE_TYPE.equals(responseType);
    }

    @Override
    protected StringBuilder prepareRedirectResponse(OAuthRedirectionState state,
                                   Client client,
                                   List<String> requestedScope,
                                   List<String> approvedScope,
                                   UserSubject userSubject,
                                   ServerAccessToken preAuthorizedToken) {
        ServerAuthorizationCodeGrant codeGrant = prepareHybrideCode(
            state, client, requestedScope, approvedScope, userSubject, preAuthorizedToken);

        StringBuilder sb = super.prepareRedirectResponse(state, client, requestedScope,
                                                          approvedScope, userSubject, preAuthorizedToken);

        if (codeGrant != null) {
            sb.append('&');
            sb.append(OAuthConstants.AUTHORIZATION_CODE_VALUE).append('=').append(codeGrant.getCode());
        }
        return sb;
    }

    @Override
    protected AbstractFormImplicitResponse prepareFormResponse(OAuthRedirectionState state,
                                                Client client,
                                                List<String> requestedScope,
                                                List<String> approvedScope,
                                                UserSubject userSubject,
                                                ServerAccessToken preAuthorizedToken) {
        ServerAuthorizationCodeGrant codeGrant = prepareHybrideCode(
            state, client, requestedScope, approvedScope, userSubject, preAuthorizedToken);

        AbstractFormImplicitResponse implResp = super.prepareFormResponse(state, client, requestedScope,
                                                          approvedScope, userSubject, preAuthorizedToken);

        FormHybridResponse response = new FormHybridResponse();
        response.setResponseType(state.getResponseType());
        response.setRedirectUri(state.getRedirectUri());
        response.setState(state.getState());
        response.setImplicitResponse(implResp);
        if (codeGrant != null) {
            response.setCode(codeGrant.getCode());
        }
        return response;
    }


    protected ServerAuthorizationCodeGrant prepareHybrideCode(OAuthRedirectionState state,
                                                Client client,
                                                List<String> requestedScope,
                                                List<String> approvedScope,
                                                UserSubject userSubject,
                                                ServerAccessToken preAuthorizedToken) {
        ServerAuthorizationCodeGrant codeGrant = null;
        if (state.getResponseType() != null && state.getResponseType().startsWith(OAuthConstants.CODE_RESPONSE_TYPE)) {
            codeGrant = codeService.getGrantRepresentation(
                state, client, requestedScope, approvedScope, userSubject, preAuthorizedToken);
            JAXRSUtils.getCurrentMessage().getExchange().put(OAuthConstants.AUTHORIZATION_CODE_VALUE,
                                                             codeGrant.getCode());
        }
        return codeGrant;
    }

    public void setCodeService(OidcAuthorizationCodeService codeService) {
        this.codeService = codeService;
    }


}
