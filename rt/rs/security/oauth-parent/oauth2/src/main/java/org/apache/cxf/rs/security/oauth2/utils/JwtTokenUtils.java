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
package org.apache.cxf.rs.security.oauth2.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwt.JoseJwtConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.ClientRegistrationProvider;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;

public final class JwtTokenUtils {
    private JwtTokenUtils() {

    }

    public static String getClaimName(String tokenProperty,
                                      String defaultName,
                                      Map<String, String> claimsMap) {
        String claimName = null;
        if (claimsMap != null) {
            claimName = claimsMap.get(tokenProperty);
        }
        return claimName == null ? defaultName : claimName;
    }

    public static ServerAccessToken createAccessTokenFromJwt(JoseJwtConsumer consumer,
                                                             String jose,
                                                             ClientRegistrationProvider clientProvider,
                                                             Map<String, String> claimsMap) {
        JwtClaims claims = consumer.getJwtToken(jose).getClaims();

        // 'client_id' or 'cid', default client_id
        String clientIdClaimName =
            JwtTokenUtils.getClaimName(OAuthConstants.CLIENT_ID, OAuthConstants.CLIENT_ID, claimsMap);
        String clientId = claims.getStringProperty(clientIdClaimName);
        Client c = clientProvider.getClient(clientId);

        long issuedAt = claims.getIssuedAt();
        long lifetime = claims.getExpiryTime() - issuedAt;
        BearerAccessToken at = new BearerAccessToken(c, jose, lifetime, issuedAt);

        List<String> audiences = claims.getAudiences();
        if (audiences != null && !audiences.isEmpty()) {
            at.setAudiences(claims.getAudiences());
        }

        String issuer = claims.getIssuer();
        if (issuer != null) {
            at.setIssuer(issuer);
        }
        Object scope = claims.getClaim(OAuthConstants.SCOPE);
        if (scope != null) {
            String[] scopes = scope instanceof String
                ? scope.toString().split(" ") : CastUtils.cast((List<?>)scope).toArray(new String[]{});
            List<OAuthPermission> perms = new LinkedList<>();
            for (String s : scopes) {
                if (!StringUtils.isEmpty(s)) {
                    perms.add(new OAuthPermission(s.trim()));
                }
            }
            at.setScopes(perms);
        }
        final String usernameProp = "username";
        String usernameClaimName =
            JwtTokenUtils.getClaimName(usernameProp, usernameProp, claimsMap);
        String username = claims.getStringProperty(usernameClaimName);
        String subject = claims.getSubject();
        if (username != null) {
            UserSubject userSubject = new UserSubject(username);
            if (subject != null) {
                userSubject.setId(subject);
            }
            at.setSubject(userSubject);
        } else if (subject != null) {
            at.setSubject(new UserSubject(subject));
        }

        String grantType = claims.getStringProperty(OAuthConstants.GRANT_TYPE);
        if (grantType != null) {
            at.setGrantType(grantType);
        }
        String grantCode = claims.getStringProperty(OAuthConstants.AUTHORIZATION_CODE_GRANT);
        if (grantCode != null) {
            at.setGrantCode(grantCode);
        }
        String codeVerifier = claims.getStringProperty(OAuthConstants.AUTHORIZATION_CODE_VERIFIER);
        if (codeVerifier != null) {
            at.setClientCodeVerifier(codeVerifier);
        }
        String nonce = claims.getStringProperty(OAuthConstants.NONCE);
        if (nonce != null) {
            at.setNonce(nonce);
        }
        
        Map<String, String> extraProperties = CastUtils.cast((Map<?, ?>)claims.getClaim("extra_properties"));
        if (extraProperties != null) {
            at.getExtraProperties().putAll(extraProperties);
            Map<String, Object> cnfClaim = CastUtils.cast((Map<?, ?>)claims.getClaim(JwtConstants.CLAIM_CONFIRMATION));
            if (cnfClaim != null) {
                Object certCnf = cnfClaim.get(JoseConstants.HEADER_X509_THUMBPRINT_SHA256);
                if (certCnf != null) {
                    at.getExtraProperties().put(JoseConstants.HEADER_X509_THUMBPRINT_SHA256, certCnf.toString());
                }
            }
        }
        
        return at;
    }
}
