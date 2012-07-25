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
package org.apache.cxf.rs.security.oauth2.common;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

// Represents the information about the validated ServerAccessToken.
// The problem with reading specific ServerAccessToken instances is that
// the (JAXB) reader needs to be specifically aware of the concrete token
// classes like BearerAccessToken, etc, even though classes like BearerAccessToken
// will not add anything useful to the filter protecting the application.

//TODO: consider simply extending ServerAccessToken, 
// though this will require relaxing a bit the ServerAccessToken model 
// (introduce default constructors, etc) 
@XmlRootElement
public class AccessTokenValidation {
    private String clientId;
    private UserSubject clientSubject;
    
    private String tokenKey;
    private String tokenType;
    private String tokenGrantType;
    private long tokenIssuedAt;
    private long tokenLifetime;
    private UserSubject tokenSubject;
    private List<OAuthPermission> tokenScopes = new LinkedList<OAuthPermission>();
    
    public AccessTokenValidation() {
        
    }
    
    public AccessTokenValidation(ServerAccessToken token) {
        this.clientId = token.getClient().getClientId();
        this.clientSubject = token.getClient().getSubject();
        
        this.tokenKey = token.getTokenKey();
        this.tokenType = token.getTokenType();
        this.tokenGrantType = token.getGrantType();
        this.tokenIssuedAt = token.getIssuedAt();
        this.tokenLifetime = token.getExpiresIn();
        
        this.tokenSubject = token.getSubject();
        this.tokenScopes = token.getScopes();        
    }
    
    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    public UserSubject getClientSubject() {
        return clientSubject;
    }
    public void setClientSubject(UserSubject clientSubject) {
        this.clientSubject = clientSubject;
    }
    public String getTokenKey() {
        return tokenKey;
    }
    public void setTokenKey(String tokenId) {
        this.tokenKey = tokenId;
    }
    public UserSubject getTokenSubject() {
        return tokenSubject;
    }
    public void setTokenSubject(UserSubject tokenSubject) {
        this.tokenSubject = tokenSubject;
    }
    public List<OAuthPermission> getTokenScopes() {
        return tokenScopes;
    }
    public void setTokenScopes(List<OAuthPermission> tokenPermissions) {
        this.tokenScopes = tokenPermissions;
    }
    public String getTokenGrantType() {
        return tokenGrantType;
    }
    public void setTokenGrantType(String tokenGrantType) {
        this.tokenGrantType = tokenGrantType;
    }
    public long getTokenIssuedAt() {
        return tokenIssuedAt;
    }
    public void setTokenIssuedAt(long tokenIssuedAt) {
        this.tokenIssuedAt = tokenIssuedAt;
    }
    public long getTokenLifetime() {
        return tokenLifetime;
    }
    public void setTokenLifetime(long tokenLifetime) {
        this.tokenLifetime = tokenLifetime;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
}
