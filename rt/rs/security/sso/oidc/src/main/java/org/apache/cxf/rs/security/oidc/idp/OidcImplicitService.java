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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.OAuthRedirectionState;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.services.ImplicitGrantService;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;


public class OidcImplicitService extends ImplicitGrantService {
    private static final String OPEN_ID_CONNECT_SCOPE = "openid";
    private static final String ID_TOKEN_RESPONSE_TYPE = "id_token";
    private static final String ID_TOKEN_AND_AT_RESPONSE_TYPE = "id_token token";
    private boolean skipAuthorizationWithOidcScope;
    
    public OidcImplicitService() {
        super(new HashSet<String>(Arrays.asList(ID_TOKEN_RESPONSE_TYPE,
                                                ID_TOKEN_AND_AT_RESPONSE_TYPE)));
    }
    
    @Override
    protected boolean canAccessTokenBeReturned(OAuthRedirectionState state, 
                                               List<String> requestedScope, 
                                               List<String> approvedScope) {
        return state.getResponseType() != null 
            && state.getResponseType().contains(ID_TOKEN_AND_AT_RESPONSE_TYPE);
    }
    
    @Override
    protected Response startAuthorization(MultivaluedMap<String, String> params, 
                                          UserSubject userSubject,
                                          Client client) {    
        // Validate the nonce, it must be present for the Implicit flow
        if (params.getFirst(OAuthConstants.NONCE) == null) {
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
            && OPEN_ID_CONNECT_SCOPE.equals(requestedScope.get(0));
    }
    public void setSkipAuthorizationWithOidcScope(boolean skipAuthorizationWithOidcScope) {
        this.skipAuthorizationWithOidcScope = skipAuthorizationWithOidcScope;
    }
    
}
