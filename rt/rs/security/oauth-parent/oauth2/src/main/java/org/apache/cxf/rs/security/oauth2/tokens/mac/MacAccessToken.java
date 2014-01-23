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
package org.apache.cxf.rs.security.oauth2.tokens.mac;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

//See http://tools.ietf.org/html/draft-ietf-oauth-v2-http-mac-01
public class MacAccessToken extends ServerAccessToken {
    
    private static final long serialVersionUID = -4331703769692080818L;

    public MacAccessToken(Client client, 
                          long lifetime) {
        this(client, HmacAlgorithm.HmacSHA256, lifetime);
    }
    
    public MacAccessToken(Client client, 
                          String macAuthAlgo,
                          long lifetime) {
        this(client, HmacAlgorithm.toHmacAlgorithm(macAuthAlgo), lifetime);
    }
    
    public MacAccessToken(Client client, 
                          HmacAlgorithm macAlgo,
                          long lifetime) {
        this(client, 
             macAlgo,
             OAuthUtils.generateRandomTokenKey(), 
             lifetime, 
             OAuthUtils.getIssuedAt());
    }
    public MacAccessToken(Client client,
                          HmacAlgorithm algo,
                          String tokenKey,
                          long lifetime, 
                          long issuedAt) {
        super(client, OAuthConstants.MAC_TOKEN_TYPE, tokenKey, lifetime, issuedAt);
        this.setExtraParameters(algo, null);
    }
    
    public MacAccessToken(Client client,
                          HmacAlgorithm algo,
                          String tokenKey,
                          String macKey,
                          long lifetime, 
                          long issuedAt) {
        super(client, OAuthConstants.MAC_TOKEN_TYPE, tokenKey, lifetime, issuedAt);
        this.setExtraParameters(algo, macKey);
    }
    
    public MacAccessToken(ServerAccessToken token, String newKey) {
        super(validateTokenType(token, OAuthConstants.MAC_TOKEN_TYPE), newKey);
    }
    
    private void setExtraParameters(HmacAlgorithm algo, String macKey) {
        String theKey = macKey == null ? HmacUtils.generateSecret(algo) : macKey; 
        super.getParameters().put(OAuthConstants.MAC_TOKEN_KEY,
                                  theKey);
        super.getParameters().put(OAuthConstants.MAC_TOKEN_ALGORITHM,
                                  algo.getOAuthName());
    }
    
    public String getMacId() {
        return super.getTokenKey();
    }
    
    public String getMacKey() {
        return super.getParameters().get(OAuthConstants.MAC_TOKEN_KEY);
    }
    
    public String getMacAlgorithm() {
        return super.getParameters().get(OAuthConstants.MAC_TOKEN_ALGORITHM);
    }
}
