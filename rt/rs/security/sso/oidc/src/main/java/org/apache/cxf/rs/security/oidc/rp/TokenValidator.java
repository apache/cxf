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
package org.apache.cxf.rs.security.oidc.rp;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oidc.common.UserIdToken;
import org.apache.cxf.rs.security.oidc.common.UserProfile;

public class TokenValidator {
    private JweDecryptionProvider jweDecryptor;
    private JwsSignatureVerifier jwsVerifier;
    private String issuerId;
    private int issuedAtRange;
    private boolean requireAtHash = true;
    private WebClient jwkSetClient;
    private ConcurrentHashMap<String, JsonWebKey> keyMap = new ConcurrentHashMap<String, JsonWebKey>(); 
    
    public UserIdToken getIdTokenFromJwt(ClientAccessToken at, String clientId) {
        JwtToken jwt = getIdJwtToken(at, clientId);
        return getIdTokenFromJwt(jwt, clientId);
    }
    public UserIdToken getIdTokenFromJwt(JwtToken jwt, String clientId) {
        //TODO: do the extra validation if needed
        return new UserIdToken(jwt.getClaims().asMap());
    }
    public JwtToken getIdJwtToken(ClientAccessToken at, String clientId) {
        String idJwtToken = at.getParameters().get("id_token");
        JwtToken jwt = getJwtToken(idJwtToken, clientId);
        validateJwtClaims(jwt.getClaims(), clientId, true);
        OidcUtils.validateAccessTokenHash(at, jwt, requireAtHash);
        return jwt;
    }
    public UserProfile getProfile(WebClient profileClient, UserIdToken idToken) {
        return getProfile(profileClient, idToken, false);
    }
    public UserProfile getProfile(WebClient profileClient, UserIdToken idToken, boolean asJwt) {
        if (asJwt) {
            String jwt = profileClient.get(String.class);
            return getProfileFromJwt(jwt, idToken);
        } else {
            UserProfile profile = profileClient.get(UserProfile.class);
            validateUserProfile(profile, idToken);
            return profile;
        }
        
    }
    public UserProfile getProfileFromJwt(String profileJwtToken, UserIdToken idToken) {
        JwtToken jwt = getProfileJwtToken(profileJwtToken, idToken);
        return getProfileFromJwt(jwt, idToken);
    }
    public UserProfile getProfileFromJwt(JwtToken jwt, UserIdToken idToken) {
        UserProfile profile = new UserProfile(jwt.getClaims().asMap());
        validateUserProfile(profile, idToken);
        return profile;
    }
    public JwtToken getProfileJwtToken(String profileJwtToken, UserIdToken idToken) {
        return getJwtToken(profileJwtToken, idToken.getAudience());
    }
    public void validateUserProfile(UserProfile profile, UserIdToken idToken) {
        validateJwtClaims(profile, idToken.getAudience(), false);
        // validate subject
        if (!idToken.getSubject().equals(profile.getSubject())) {
            throw new SecurityException("Invalid subject");
        }
    }
    public JwtToken getJwtToken(String wrappedJwtToken, String clientId) {
        if (wrappedJwtToken == null) {
            throw new SecurityException("ID Token is missing");
        }
        // Decrypt the token if needed
        if (jweDecryptor != null) {
            wrappedJwtToken = jweDecryptor.decrypt(wrappedJwtToken).getContentText();
        }

        // read id_token into JwtToken
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(wrappedJwtToken);
        JwtToken jwt = jwtConsumer.getJwtToken();
        // validate token signature
        JwsSignatureVerifier theJwsVerifier = loadJwkSignatureVerifier(jwt);
        if (!jwtConsumer.verifySignatureWith(theJwsVerifier)) {
            throw new SecurityException("ID Token signature verification failed");
        }
        return jwt;
    }
    
    private void validateJwtClaims(JwtClaims claims, String clientId, boolean validateClaimsAlways) {
        // validate subject
        if (claims.getSubject() == null) {
            throw new SecurityException("Invalid subject");
        }
        // validate audience
        String aud = claims.getAudience();
        if (aud == null && validateClaimsAlways || aud != null && !clientId.equals(aud)) {
            throw new SecurityException("Invalid audience");
        }

        // validate the provider
        String issuer = claims.getIssuer();
        if (issuerId == null && validateClaimsAlways || issuerId != null && !issuerId.equals(issuer)) {
            throw new SecurityException("Invalid provider");
        }
        Long currentTimeInSecs = System.currentTimeMillis() / 1000;
        Long expiryTimeInSecs = claims.getExpiryTime();
        if (expiryTimeInSecs == null && validateClaimsAlways 
            || expiryTimeInSecs != null && currentTimeInSecs > expiryTimeInSecs) {
            throw new SecurityException("The token expired");
        }
        Long issuedAtInSecs = claims.getIssuedAt();
        if (issuedAtInSecs == null && validateClaimsAlways 
            || issuedAtInSecs != null && (issuedAtInSecs > currentTimeInSecs || issuedAtRange > 0
            && issuedAtInSecs < currentTimeInSecs - issuedAtRange)) {
            throw new SecurityException("Invalid issuedAt");
        }
        
    }
    
    private JwsSignatureVerifier loadJwkSignatureVerifier(JwtToken jwt) {
        if (jwsVerifier != null) {
            return jwsVerifier;
        }
        if (jwkSetClient == null) {
            throw new SecurityException("Provider Jwk Set Client is not available");
        }
        String keyId = jwt.getHeaders().getKeyId();
        if (keyId == null) {
            throw new SecurityException("Provider JWK key id is null");
        }
        JsonWebKey key = keyMap.get(keyId);
        if (key == null) {
            JsonWebKeys keys = jwkSetClient.get(JsonWebKeys.class);
            key = keys.getKey(keyId);
            keyMap.putIfAbsent(keyId, key);
        }
        if (key == null) {
            throw new SecurityException("JWK key with the key id: \"" + keyId + "\" is not available");
        }
        return JwsUtils.getSignatureVerifier(key);
    }

    public void setJweDecryptor(JweDecryptionProvider jweDecryptor) {
        this.jweDecryptor = jweDecryptor;
    }

    public void setJweVerifier(JwsSignatureVerifier theJwsVerifier) {
        this.jwsVerifier = theJwsVerifier;
    }

    public void setIssuerId(String issuerId) {
        this.issuerId = issuerId;
    }

    public void setJwkSetClient(WebClient jwkSetClient) {
        this.jwkSetClient = jwkSetClient;
    }

    public void setIssuedAtRange(int issuedAtRange) {
        this.issuedAtRange = issuedAtRange;
    }

    public void setRequireAtHash(boolean requireAtHash) {
        this.requireAtHash = requireAtHash;
    }
}
