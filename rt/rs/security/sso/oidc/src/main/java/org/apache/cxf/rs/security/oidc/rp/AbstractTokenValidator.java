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
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;

public abstract class AbstractTokenValidator {
    private static final String SELF_ISSUED_ISSUER = "https://self-issued.me";
    private JweDecryptionProvider jweDecryptor;
    private JwsSignatureVerifier jwsVerifier;
    private String issuerId;
    private int issuedAtRange;
    private int clockOffset;
    private WebClient jwkSetClient;
    private boolean supportSelfIssuedProvider;
    private ConcurrentHashMap<String, JsonWebKey> keyMap = new ConcurrentHashMap<String, JsonWebKey>(); 
    
    protected JwtToken getJwtToken(String wrappedJwtToken, boolean jweOnly) {
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
        JwsSignatureVerifier theSigVerifier = getInitializedSigVerifier(jwt);
        return validateToken(jwtConsumer, jwt, theSigVerifier);
        
    }
    
    protected void validateJwtClaims(JwtClaims claims, String clientId, boolean validateClaimsAlways) {
        // validate the issuer
        String issuer = claims.getIssuer();
        if (issuer == null && validateClaimsAlways) {
            throw new SecurityException("Invalid provider");
        }
        if (supportSelfIssuedProvider && issuerId == null 
            && issuer != null && SELF_ISSUED_ISSUER.equals(issuer)) {
            //TODO: self-issued provider token validation
        } else {
            if (issuer != null && !issuer.equals(issuerId)) {
                throw new SecurityException("Invalid provider");
            }
            // validate subject
            if (claims.getSubject() == null) {
                throw new SecurityException("Invalid subject");
            }
            // validate audience
            String aud = claims.getAudience();
            if (aud == null && validateClaimsAlways || aud != null && !clientId.equals(aud)) {
                throw new SecurityException("Invalid audience");
            }
    
            JwtUtils.validateJwtTimeClaims(claims, clockOffset, issuedAtRange, validateClaimsAlways);
        }
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
    protected JwsSignatureVerifier getInitializedSigVerifier(JwtToken jwt) {
        if (jwsVerifier != null) {
            return jwsVerifier;    
        } 
        JwsSignatureVerifier theJwsVerifier = JwsUtils.loadSignatureVerifier(false);
        if (theJwsVerifier != null) {
            return theJwsVerifier;
        }
        
        JsonWebKey key = null;
        if (supportSelfIssuedProvider && SELF_ISSUED_ISSUER.equals(jwt.getClaim("issuer"))) {
            String publicKeyJson = (String)jwt.getClaim("sub_jwk");
            if (publicKeyJson != null) {
                JsonWebKey publicKey = JwkUtils.readJwkKey(publicKeyJson);
                String thumbprint = JwkUtils.getThumbprint(publicKey);
                if (thumbprint.equals(jwt.getClaim("sub"))) {
                    key = publicKey;
                }
            }
            if (key == null) {
                throw new SecurityException("Self-issued JWK key is invalid or not available");
            }
        } else {
            String keyId = jwt.getHeaders().getKeyId();
            key = keyId != null ? keyMap.get(keyId) : null;
            if (key == null) {
                if (jwkSetClient == null) {
                    throw new SecurityException("Provider Jwk Set Client is not available");
                }
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
        }
        
        theJwsVerifier = JwsUtils.getSignatureVerifier(key);
        
        if (theJwsVerifier == null) {
            throw new SecurityException("JWS Verifier is not available");
        }
        
        return theJwsVerifier;
    }

    public void setClockOffset(int clockOffset) {
        this.clockOffset = clockOffset;
    }

    public void setSupportSelfIssuedProvider(boolean supportSelfIssuedProvider) {
        this.supportSelfIssuedProvider = supportSelfIssuedProvider;
    }
}
