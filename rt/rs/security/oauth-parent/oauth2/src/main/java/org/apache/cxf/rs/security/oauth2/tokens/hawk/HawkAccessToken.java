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
package org.apache.cxf.rs.security.oauth2.tokens.hawk;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rt.security.crypto.HmacUtils;

//https://tools.ietf.org/html/draft-hammer-oauth-v2-mac-token-05
//->
//https://github.com/hueniverse/hawk/blob/master/README.md

public class HawkAccessToken extends ServerAccessToken {

    private static final long serialVersionUID = -4331703769692080818L;

    public HawkAccessToken(Client client,
                          long lifetime) {
        this(client, HmacAlgorithm.HmacSHA256, lifetime);
    }

    public HawkAccessToken(Client client,
                          String macAuthAlgo,
                          long lifetime) {
        this(client, HmacAlgorithm.toHmacAlgorithm(macAuthAlgo), lifetime);
    }

    public HawkAccessToken(Client client,
                          HmacAlgorithm macAlgo,
                          long lifetime) {
        this(client,
             macAlgo,
             OAuthUtils.generateRandomTokenKey(),
             lifetime,
             OAuthUtils.getIssuedAt());
    }
    public HawkAccessToken(Client client,
                          HmacAlgorithm algo,
                          String tokenKey,
                          long lifetime,
                          long issuedAt) {
        this(client, algo, tokenKey, null, lifetime, issuedAt);
    }

    public HawkAccessToken(Client client,
                          HmacAlgorithm algo,
                          String tokenKey,
                          String macKey,
                          long lifetime,
                          long issuedAt) {
        super(checkClient(client), OAuthConstants.HAWK_TOKEN_TYPE, tokenKey, lifetime, issuedAt);
        this.setExtraParameters(algo, macKey);
    }
    public HawkAccessToken(ServerAccessToken token) {
        this(token, OAuthUtils.generateRandomTokenKey());
    }
    public HawkAccessToken(ServerAccessToken token, String newKey) {
        super(validateTokenType(token, OAuthConstants.HAWK_TOKEN_TYPE), newKey);
    }

    private void setExtraParameters(HmacAlgorithm algo, String macKey) {
        String theKey = macKey == null ? HmacUtils.generateKey(algo.getJavaName()) : macKey;
        super.getParameters().put(OAuthConstants.HAWK_TOKEN_KEY,
                                  theKey);
        super.getParameters().put(OAuthConstants.HAWK_TOKEN_ALGORITHM,
                                  algo.getOAuthName());
    }

    public String getMacId() {
        return super.getTokenKey();
    }

    public String getMacKey() {
        return super.getParameters().get(OAuthConstants.HAWK_TOKEN_KEY);
    }

    public String getMacAlgorithm() {
        return super.getParameters().get(OAuthConstants.HAWK_TOKEN_ALGORITHM);
    }
    private static Client checkClient(Client c) {
        if (!c.isConfidential()) {
            throw new OAuthServiceException("Public clients can not keep a MAC secret");
        }
        return c;
    }
}
