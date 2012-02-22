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
package org.apache.cxf.rs.security.oauth.grants.code;

import java.util.List;

import org.apache.cxf.rs.security.oauth.common.Client;
import org.apache.cxf.rs.security.oauth.common.UserSubject;

/**
 * Captures the information associated with the code grant registration request.
 * @see ServerAuthorizationCodeGrant  
 */
public class AuthorizationCodeRegistration {
    private Client client; 
    private List<String> requestedScope;
    private List<String> approvedScope;
    private long lifetime;
    private long issuedAt;
    private String redirectUri;
    private UserSubject subject;
    
    public void setClient(Client client) {
        this.client = client;
    }
    public Client getClient() {
        return client;
    }
    
    public void setLifetime(long lifetime) {
        this.lifetime = lifetime;
    }
    public long getLifetime() {
        return lifetime;
    }
    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }
    public long getIssuedAt() {
        return issuedAt;
    }
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
    public String getRedirectUri() {
        return redirectUri;
    }
    public void setRequestedScope(List<String> requestedScope) {
        this.requestedScope = requestedScope;
    }
    public List<String> getRequestedScope() {
        return requestedScope;
    }
    public void setApprovedScope(List<String> approvedScope) {
        this.approvedScope = approvedScope;
    }
    public List<String> getApprovedScope() {
        return approvedScope;
    }
    public void setSubject(UserSubject subject) {
        this.subject = subject;
    }
    public UserSubject getSubject() {
        return subject;
    }
}
