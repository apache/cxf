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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.rs.security.jose.jwt.JoseJwtConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.ClientRegistrationProvider;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;

public final class JwtAccessTokenUtils {
    private JwtAccessTokenUtils() {
        
    }
    
    public static ServerAccessToken createAccessTokenFromJwt(JoseJwtConsumer consumer, 
                                                             String jose,
                                                             ClientRegistrationProvider clientProvider) {
        JwtClaims claims = consumer.getJwtToken(jose).getClaims();
       
        Client c = clientProvider.getClient(claims.getStringProperty(JwtConstants.CLAIM_AUDIENCE));
        long issuedAt = claims.getLongProperty(JwtConstants.CLAIM_ISSUED_AT);
        long lifetime = claims.getLongProperty(JwtConstants.CLAIM_EXPIRY) - issuedAt;
        BearerAccessToken at = new BearerAccessToken(c, jose, lifetime, issuedAt);
       
        Object resourceAud = claims.getClaim(OAuthConstants.RESOURCE_INDICATOR);
        if (resourceAud != null) {
            List<String> auds = null;
            if (resourceAud instanceof List) {
                auds = CastUtils.cast((List<?>)resourceAud);
            } else {
                auds = Collections.singletonList((String)resourceAud);
            } 
            at.setAudiences(auds);
        }
        String issuer = claims.getStringProperty(JwtConstants.CLAIM_ISSUER);
        if (issuer != null) {
            at.setIssuer(issuer);
        }
        Object scope = claims.getClaim(OAuthConstants.SCOPE);
        if (scope != null) {
            String[] scopes = scope instanceof String 
                ? scope.toString().split(" ") : CastUtils.cast((List<?>)scope).toArray(new String[]{});
            List<OAuthPermission> perms = new LinkedList<OAuthPermission>();
            for (String s : scopes) {    
                if (!StringUtils.isEmpty(s)) {
                    perms.add(new OAuthPermission(s.trim()));
                }
            }
            at.setScopes(perms);
        }
        String username = claims.getStringProperty("preferred_username");
        String subject = claims.getStringProperty(JwtConstants.CLAIM_SUBJECT);
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
        
        Map<String, String> extraProperties = CastUtils.cast((Map<?, ?>)claims.getClaim("extra_propertirs"));
        if (extraProperties != null) {
            at.getExtraProperties().putAll(extraProperties);
        }
       
       
        return at;
    }
}
