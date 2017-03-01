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
package org.apache.cxf.rs.security.oidc.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class IdToken extends AbstractUserInfo {
    public static final String AUTH_TIME_CLAIM = "auth_time";
    public static final String NONCE_CLAIM = OAuthConstants.NONCE;
    public static final String ACR_CLAIM = "acr";
    public static final String AZP_CLAIM = "azp";
    public static final String AMR_CLAIM = "amr";
    public static final String ACCESS_TOKEN_HASH_CLAIM = "at_hash";
    public static final String AUTH_CODE_HASH_CLAIM = "c_hash";
    private static final long serialVersionUID = -2243170791872714855L;


    public IdToken() {
    }

    public IdToken(JwtClaims claims) {
        this(claims.asMap());
    }

    public IdToken(Map<String, Object> claims) {
        super(new LinkedHashMap<String, Object>(claims));
    }
    public void setAuthenticationTime(Long time) {
        setProperty(AUTH_TIME_CLAIM, time);
    }
    public Long getAuthenticationTime() {
        return getLongProperty(AUTH_TIME_CLAIM);
    }
    public void setNonce(String nonce) {
        setProperty(NONCE_CLAIM, nonce);
    }
    public String getNonce() {
        return (String)getProperty(NONCE_CLAIM);
    }
    public void setAuthenticationContextRef(String ref) {
        setProperty(ACR_CLAIM, ref);
    }
    public String getAuthenticationContextRef() {
        return (String)getProperty(ACR_CLAIM);
    }
    public void setAuthenticationMethodRefs(List<String> refs) {
        setProperty(AMR_CLAIM, refs);
    }
    public List<String> getAuthenticationMethodRefs() {
        return CastUtils.cast((List<?>)getProperty(AMR_CLAIM));
    }
    public void setAuthorizedParty(String azp) {
        setProperty(AZP_CLAIM, azp);
    }
    public String getAuthorizedParty() {
        return (String)getProperty(AZP_CLAIM);
    }
    public void setAccessTokenHash(String at) {
        setProperty(ACCESS_TOKEN_HASH_CLAIM, at);
    }
    public String getAccessTokenHash() {
        return (String)getProperty(ACCESS_TOKEN_HASH_CLAIM);
    }
    public void setAuthorizationCodeHash(String at) {
        setProperty(AUTH_CODE_HASH_CLAIM, at);
    }
    public String getAuthorizationCodeHash() {
        return (String)getProperty(AUTH_CODE_HASH_CLAIM);
    }

}
