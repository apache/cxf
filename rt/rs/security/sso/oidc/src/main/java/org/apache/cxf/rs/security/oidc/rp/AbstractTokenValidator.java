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
import org.apache.cxf.rs.security.jose.jwe.JweJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;

public abstract class AbstractTokenValidator {
    private JweDecryptionProvider jweDecryptor;
    private JwsSignatureVerifier jwsVerifier;
    private String issuerId;
    private int issuedAtRange;
    private WebClient jwkSetClient;
    private ConcurrentHashMap<String, JsonWebKey> keyMap = new ConcurrentHashMap<String, JsonWebKey>(); 
    
    protected JwtToken getJwtToken(String wrappedJwtToken, String clientId, String idTokenKid, 
                                   boolean jweOnly) {
        if (wrappedJwtToken == null) {
            throw new SecurityException("ID Token is missing");
        }
        // Decrypt the token if needed
        if (jweDecryptor != null) {
            if (jweOnly) {
                return new JweJwtCompactConsumer(wrappedJwtToken).decryptWith(jweDecryptor);    
            }
            wrappedJwtToken = jweDecryptor.decrypt(wrappedJwtToken).getContentText();
        } else if (jweOnly) {
            throw new SecurityException("Token can not be decrypted");
        }

        // validate token signature
        return getTokenValidateSignature(wrappedJwtToken, idTokenKid);
    }
    
    protected void validateJwtClaims(JwtClaims claims, String clientId, boolean validateClaimsAlways) {
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
    
    protected JwtToken getTokenValidateSignature(String wrappedJwtToken, String idTokenKid) {
        // read id_token into JwtToken
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(wrappedJwtToken);
        JwtToken jwt = jwtConsumer.getJwtToken(); 
        if (jwsVerifier != null) {
            return validateToken(jwtConsumer, jwt, jwsVerifier);
        }
        if (jwkSetClient == null) {
            throw new SecurityException("Provider Jwk Set Client is not available");
        }
        String keyId = idTokenKid != null ? idTokenKid : jwtConsumer.getJwtToken().getHeaders().getKeyId();
        if (keyId == null) {
            throw new SecurityException("Provider JWK key id is null");
        }
        JsonWebKey key = keyMap.get(keyId);
        if (key == null) {
            JsonWebKeys keys = jwkSetClient.get(JsonWebKeys.class);
            key = keys.getKey(keyId);
            keyMap.putAll(keys.getKeyIdMap());
        }
        if (key == null) {
            throw new SecurityException("JWK key with the key id: \"" + keyId + "\" is not available");
        }
        return validateToken(jwtConsumer, jwt, JwsUtils.getSignatureVerifier(key));
    }
    protected JwtToken validateToken(JwsJwtCompactConsumer consumer, JwtToken jwt, JwsSignatureVerifier jws) {
        if (!consumer.verifySignatureWith(jws)) {
            throw new SecurityException("Invalid Signature");
        }
        return jwt;
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

    
}
