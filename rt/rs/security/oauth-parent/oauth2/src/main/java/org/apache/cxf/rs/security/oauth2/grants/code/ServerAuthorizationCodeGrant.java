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
package org.apache.cxf.rs.security.oauth2.grants.code;

import java.util.Collections;
import java.util.List;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;


/**
 * Authorization Code Token representation
 */
public class ServerAuthorizationCodeGrant extends AuthorizationCodeGrant {
    private long issuedAt;
    private long lifetime;
    private Client client;
    private List<OAuthPermission> approvedScopes = Collections.emptyList();
    private UserSubject subject;
    
    public ServerAuthorizationCodeGrant(Client client, 
                                  String code,
                                  long lifetime, 
                                  long issuedAt) {
        super(code);
        this.client = client;
        this.lifetime = lifetime;
        this.issuedAt = issuedAt;
    }

    
    public long getIssuedAt() {
        return issuedAt;
    }

    public long getLifetime() {
        return lifetime;
    }

    public Client getClient() {
        return client;
    }


    public void setApprovedScopes(List<OAuthPermission> scopes) {
        this.approvedScopes = scopes;
    }


    public List<OAuthPermission> getApprovedScopes() {
        return approvedScopes;
    }


    public void setSubject(UserSubject subject) {
        this.subject = subject;
    }


    public UserSubject getSubject() {
        return subject;
    }
}
