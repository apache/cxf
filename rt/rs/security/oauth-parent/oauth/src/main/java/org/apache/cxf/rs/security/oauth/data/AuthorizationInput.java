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
package org.apache.cxf.rs.security.oauth.data;

import java.util.List;

/**
 * Represents the user approval of the client RequestToken.
 * It also contains the set of approved scopes which may be 
 * more restricted than the original list requested by the client
 */
public class AuthorizationInput {
    
    private RequestToken token;
    private List<OAuthPermission> approvedScopes;
    public void setToken(RequestToken token) {
        this.token = token;
    }
    public RequestToken getToken() {
        return token;
    }
    public void setApprovedScopes(List<OAuthPermission> approvedScopes) {
        this.approvedScopes = approvedScopes;
    }
    public List<OAuthPermission> getApprovedScopes() {
        return approvedScopes;
    }

}
