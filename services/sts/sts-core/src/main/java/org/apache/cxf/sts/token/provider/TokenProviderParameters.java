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

package org.apache.cxf.sts.token.provider;

import java.security.Principal;
import java.util.Map;

import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.claims.ClaimsManager;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.ws.security.tokenstore.TokenStore;

/**
 * This class encapsulates the parameters that will be passed to a TokenProvider instance to
 * create a token. It consists of both parameters that have been extracted from the request,
 * as well as configuration specific to the STS itself.
 */
public class TokenProviderParameters {

    private STSPropertiesMBean stsProperties;
    private EncryptionProperties encryptionProperties;
    private Principal principal;
    private Map<String, Object> messageContext;
    private ClaimCollection requestedPrimaryClaims;
    private ClaimCollection requestedSecondaryClaims;
    private KeyRequirements keyRequirements;
    private TokenRequirements tokenRequirements;
    private String appliesToAddress;
    private ClaimsManager claimsManager;
    private Map<String, Object> additionalProperties;
    private TokenStore tokenStore;
    private String realm;
    private boolean encryptToken;

    public TokenStore getTokenStore() {
        return tokenStore;
    }

    public void setTokenStore(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public ClaimsManager getClaimsManager() {
        return claimsManager;
    }

    public void setClaimsManager(ClaimsManager claimsManager) {
        this.claimsManager = claimsManager;
    }

    public String getAppliesToAddress() {
        return appliesToAddress;
    }

    public void setAppliesToAddress(String appliesToAddress) {
        this.appliesToAddress = appliesToAddress;
    }

    public TokenRequirements getTokenRequirements() {
        return tokenRequirements;
    }

    public void setTokenRequirements(TokenRequirements tokenRequirements) {
        this.tokenRequirements = tokenRequirements;
    }

    public KeyRequirements getKeyRequirements() {
        return keyRequirements;
    }

    public void setKeyRequirements(KeyRequirements keyRequirements) {
        this.keyRequirements = keyRequirements;
    }

    public STSPropertiesMBean getStsProperties() {
        return stsProperties;
    }

    public void setStsProperties(STSPropertiesMBean stsProperties) {
        this.stsProperties = stsProperties;
    }

    public EncryptionProperties getEncryptionProperties() {
        return encryptionProperties;
    }

    public void setEncryptionProperties(EncryptionProperties encryptionProperties) {
        this.encryptionProperties = encryptionProperties;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    public ClaimCollection getRequestedPrimaryClaims() {
        return requestedPrimaryClaims;
    }

    public void setRequestedPrimaryClaims(ClaimCollection requestedPrimaryClaims) {
        this.requestedPrimaryClaims = requestedPrimaryClaims;
    }

    public ClaimCollection getRequestedSecondaryClaims() {
        return requestedSecondaryClaims;
    }

    public void setRequestedSecondaryClaims(ClaimCollection requestedSecondaryClaims) {
        this.requestedSecondaryClaims = requestedSecondaryClaims;
    }

    public boolean isEncryptToken() {
        return encryptToken;
    }

    public void setEncryptToken(boolean encryptToken) {
        this.encryptToken = encryptToken;
    }

    public Map<String, Object> getMessageContext() {
        return messageContext;
    }

    public void setMessageContext(Map<String, Object> messageContext) {
        this.messageContext = messageContext;
    }

}
