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
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;

public abstract class AbstractTokenValidator {
    private JweDecryptionProvider jweDecryptor;
    private JwsSignatureVerifier jwsVerifier;
    private String issuerId;
    private int issuedAtRange;
    private WebClient jwkSetClient;
    private ConcurrentHashMap<String, JsonWebKey> keyMap = new ConcurrentHashMap<String, JsonWebKey>(); 
    
    protected JwtToken getJwtToken(String wrappedJwtToken, 
                                   String clientId,
                                   String idTokenKid, 
                                   boolean jweOnly) {
        if (wrappedJwtToken == null) {
            throw new SecurityException("ID Token is missing");
        }
        JweDecryptionProvider theJweDecryptor = getInitializedDecryptionProvider(jweOnly);
        if (theJweDecryptor != null) {
            if (jweOnly) {
                return new JweJwtCompactConsumer(wrappedJwtToken).decryptWith(jweDecryptor);    
            }
            wrappedJwtToken = jweDecryptor.decrypt(wrappedJwtToken).getContentText();
        }

        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(wrappedJwtToken);
        JwtToken jwt = jwtConsumer.getJwtToken(); 
        JwsSignatureVerifier theSigVerifier = getInitializedSigVerifier(jwt, idTokenKid);
        return validateToken(jwtConsumer, jwt, theSigVerifier);
        
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
        JwtUtils.validateJwtTimeClaims(claims, issuedAtRange, validateClaimsAlways);
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

    protected JweDecryptionProvider getInitializedDecryptionProvider(boolean jweOnly) {
        if (jweDecryptor != null) {
            return jweDecryptor;    
        } 
        return JweUtils.loadDecryptionProvider(jweOnly);
    }
    protected JwsSignatureVerifier getInitializedSigVerifier(JwtToken jwt, String idTokenKid) {
        if (jwsVerifier != null) {
            return jwsVerifier;    
        } 
        JwsSignatureVerifier theJwsVerifier = JwsUtils.loadSignatureVerifier(false);
        if (theJwsVerifier != null) {
            return theJwsVerifier;
        }
        if (jwkSetClient == null) {
            throw new SecurityException("Provider Jwk Set Client is not available");
        }
        String keyId = idTokenKid != null ? idTokenKid : jwt.getHeaders().getKeyId();
        JsonWebKey key = keyId != null ? keyMap.get(keyId) : null;
        if (key == null) {
            JsonWebKeys keys = jwkSetClient.get(JsonWebKeys.class);
            if (keyId != null) {
                key = keys.getKey(keyId);
            } else if (keys.getKeys().size() == 1) {
                key = keys.getKeys().get(0);
            }
            keyMap.putAll(keys.getKeyIdMap());
        }
        if (key == null) {
            throw new SecurityException("JWK key with the key id: \"" + keyId + "\" is not available");
        }
        theJwsVerifier = JwsUtils.getSignatureVerifier(key);
        if (jwkSetClient == null) {
            throw new SecurityException();
        }
        return theJwsVerifier;
    }
}
