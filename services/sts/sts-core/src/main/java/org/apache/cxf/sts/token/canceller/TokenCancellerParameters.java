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

package org.apache.cxf.sts.token.canceller;

import java.security.Principal;
import java.util.Map;

import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.ws.security.tokenstore.TokenStore;

/**
 * This class encapsulates the parameters that will be passed to a TokenCanceller instance to
 * cancel a token. It consists of both parameters that have been extracted from the request,
 * as well as configuration specific to the Operation itself (STSPropertiesMBean etc.)
 */
public class TokenCancellerParameters {

    private STSPropertiesMBean stsProperties;
    private Principal principal;
    private Map<String, Object> messageContext;
    private KeyRequirements keyRequirements;
    private TokenRequirements tokenRequirements;
    private TokenStore tokenStore;
    private ReceivedToken token;

    public ReceivedToken getToken() {
        return token;
    }

    public void setToken(ReceivedToken token) {
        this.token = token;
    }

    public TokenStore getTokenStore() {
        return tokenStore;
    }

    public void setTokenStore(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
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

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public Map<String, Object> getMessageContext() {
        return messageContext;
    }

    public void setMessageContext(Map<String, Object> messageContext) {
        this.messageContext = messageContext;
    }

}
