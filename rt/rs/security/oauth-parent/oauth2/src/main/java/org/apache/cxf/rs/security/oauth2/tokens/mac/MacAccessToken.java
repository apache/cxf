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

public class MacAccessToken extends ServerAccessToken {
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
             System.currentTimeMillis() / 1000);
    }
    public MacAccessToken(Client client,
                          HmacAlgorithm algo,
                          String tokenKey,
                          long lifetime, 
                          long issuedAt) {
        super(client, OAuthConstants.MAC_TOKEN_TYPE, tokenKey, lifetime, issuedAt);
        this.setExtraParameters(algo);
    }
    
    private void setExtraParameters(HmacAlgorithm algo) {
        super.getParameters().put(OAuthConstants.MAC_TOKEN_SECRET,
                                  HmacUtils.generateSecret(algo));
        super.getParameters().put(OAuthConstants.MAC_TOKEN_ALGORITHM,
                                  algo.getOAuthName());
    }
    
    public String getMacKey() {
        return super.getTokenKey();
    }
    
    public String getMacSecret() {
        return super.getParameters().get(OAuthConstants.MAC_TOKEN_SECRET);
    }
    
    public String getMacAlgorithm() {
        return super.getParameters().get(OAuthConstants.MAC_TOKEN_ALGORITHM);
    }
}
