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

import java.util.List;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.services.AuthorizationCodeGrantService;

public class OidcAuthorizationCodeService extends AuthorizationCodeGrantService {
    private static final String OPEN_ID_CONNECT_SCOPE = "openid";
    private boolean skipAuthorizationWithOidcScope;
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
