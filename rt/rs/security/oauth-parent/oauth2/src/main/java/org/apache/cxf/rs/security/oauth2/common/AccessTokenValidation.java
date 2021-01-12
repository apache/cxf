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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;

// Represents the information about the validated ServerAccessToken.
// It is returned by AccessTokenValidatorService and is checked by CXF OAuthRequestFilter
// protecting the service resources.

// If the protected resources are not CXF based then use TokenIntrospectionService which
// returns RFC 7622 compliant TokenIntrospection response.


// The problem with reading specific ServerAccessToken instances is that
// the (JAXB) reader needs to be specifically aware of the concrete token
// classes like BearerAccessToken, etc, even though classes like BearerAccessToken
// will not add anything useful to the filter protecting the application.

//TODO: consider simply extending ServerAccessToken,
// though this will require relaxing a bit the ServerAccessToken model
// (introduce default constructors, etc)
@XmlRootElement
public class AccessTokenValidation {
    private boolean initialValidationSuccessful = true;
    private String clientId;
    private String clientIpAddress;
    private UserSubject clientSubject;
    private boolean isClientConfidential;
    private String tokenKey;
    private String tokenType;
    private String tokenGrantType;
    private long tokenIssuedAt;
    private long tokenLifetime;
    private long tokenNotBefore;
    private String tokenIssuer;
    private UserSubject tokenSubject;
    private List<OAuthPermission> tokenScopes = new LinkedList<>();
    private List<String> audiences = new LinkedList<>();
    private String clientCodeVerifier;
    private Map<String, String> extraProps = new HashMap<>();

    public AccessTokenValidation() {

    }

    public AccessTokenValidation(ServerAccessToken token) {
        this.clientId = token.getClient().getClientId();
        this.clientSubject = token.getClient().getSubject();
        this.isClientConfidential = token.getClient().isConfidential();
        this.clientIpAddress = token.getClient().getClientIpAddress();
        this.tokenKey = token.getTokenKey();
        this.tokenType = token.getTokenType();
        this.tokenGrantType = token.getGrantType();
        this.tokenIssuedAt = token.getIssuedAt();
        this.tokenLifetime = token.getExpiresIn();
        this.tokenNotBefore = token.getNotBefore();
        this.tokenIssuer = token.getIssuer();
        this.tokenSubject = token.getSubject();
        this.tokenScopes = token.getScopes();
        this.audiences = token.getAudiences();
        this.clientCodeVerifier = token.getClientCodeVerifier();
        this.extraProps.putAll(token.getExtraProperties());
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

    public String getClientIpAddress() {
        return clientIpAddress;
    }

    public void setClientIpAddress(String clientIpAddress) {
        this.clientIpAddress = clientIpAddress;
    }

    public Map<String, String> getExtraProps() {
        return extraProps;
    }

    public void setExtraProps(Map<String, String> extraProps) {
        this.extraProps = extraProps;
    }

    public boolean isClientConfidential() {
        return isClientConfidential;
    }

    public void setClientConfidential(boolean isConfidential) {
        this.isClientConfidential = isConfidential;
    }
    public String getClientCodeVerifier() {
        return clientCodeVerifier;
    }

    public void setClientCodeVerifier(String clientCodeVerifier) {
        this.clientCodeVerifier = clientCodeVerifier;
    }

    public boolean isInitialValidationSuccessful() {
        return initialValidationSuccessful;
    }

    public void setInitialValidationSuccessful(boolean localValidationSuccessful) {
        this.initialValidationSuccessful = localValidationSuccessful;
    }

    public List<String> getAudiences() {
        return audiences;
    }

    public void setAudiences(List<String> audiences) {
        this.audiences = audiences;
    }

    public String getTokenIssuer() {
        return tokenIssuer;
    }

    public void setTokenIssuer(String tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }

    public long getTokenNotBefore() {
        return tokenNotBefore;
    }

    public void setTokenNotBefore(long tokenNotBefore) {
        this.tokenNotBefore = tokenNotBefore;
    }

}
