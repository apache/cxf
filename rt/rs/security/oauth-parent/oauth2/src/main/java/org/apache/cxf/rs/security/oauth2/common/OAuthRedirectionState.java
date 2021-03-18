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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class OAuthRedirectionState implements Serializable {

    private static final long serialVersionUID = -661649302262699347L;

    private String clientId;
    private String redirectUri;
    private String state;
    private String proposedScope;
    private String audience;
    private String nonce;
    private String clientCodeChallenge;
    private String clientCodeChallengeMethod;
    private String responseType;
    private Map<String, String> extraProperties = new LinkedHashMap<>();

    public OAuthRedirectionState() {
    }

    /**
     * Sets the client id which needs to be retained in a hidden form field
     * @param clientId the client id
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Gets the client id which needs to be retained in a hidden form field
     * @return the client id
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the redirect uri which needs to be retained in a hidden form field
     * @param redirectUri the redirect uri
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * Gets the redirect uri which needs to be retained in a hidden form field
     * @return the redirect uri
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Sets the client state token which needs to be retained in a hidden form field
     * @param state the state
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Gets the client state token which needs to be retained in a hidden form field
     * @return
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the requested scope which needs to be retained in a hidden form field
     * @param proposedScope the scope
     */
    public void setProposedScope(String proposedScope) {
        this.proposedScope = proposedScope;
    }

    /**
     * Gets the requested scope which needs to be retained in a hidden form field
     * @return the scope
     */
    public String getProposedScope() {
        return proposedScope;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getClientCodeChallenge() {
        return clientCodeChallenge;
    }
    public void setClientCodeChallenge(String clientCodeChallenge) {
        this.clientCodeChallenge = clientCodeChallenge;
    }


    public String getNonce() {
        return nonce;
    }


    public void setNonce(String nonce) {
        this.nonce = nonce;
    }


    public String getResponseType() {
        return responseType;
    }


    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public Map<String, String> getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(Map<String, String> extraProperties) {
        this.extraProperties = extraProperties;
    }

    public String getClientCodeChallengeMethod() {
        return clientCodeChallengeMethod;
    }

    public void setClientCodeChallengeMethod(String clientCodeChallengeMethod) {
        this.clientCodeChallengeMethod = clientCodeChallengeMethod;
    }

}
