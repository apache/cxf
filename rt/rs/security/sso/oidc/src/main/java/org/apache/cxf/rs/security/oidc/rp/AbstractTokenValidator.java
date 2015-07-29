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
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.AbstractJoseJwtConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;

public abstract class AbstractTokenValidator extends AbstractJoseJwtConsumer {
    private static final String SELF_ISSUED_ISSUER = "https://self-issued.me";
    private String issuerId;
    private int issuedAtRange;
    private int clockOffset;
    private WebClient jwkSetClient;
    private boolean supportSelfIssuedProvider;
    private ConcurrentHashMap<String, JsonWebKey> keyMap = new ConcurrentHashMap<String, JsonWebKey>(); 
        
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
    
    public void setIssuerId(String issuerId) {
        this.issuerId = issuerId;
    }

    public void setJwkSetClient(WebClient jwkSetClient) {
        this.jwkSetClient = jwkSetClient;
    }

    public void setIssuedAtRange(int issuedAtRange) {
        this.issuedAtRange = issuedAtRange;
    }

    @Override
    protected JwsSignatureVerifier getInitializedSignatureVerifier(JwtToken jwt) {
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
            if (key == null && jwkSetClient != null) {
                JsonWebKeys keys = jwkSetClient.get(JsonWebKeys.class);
                if (keyId != null) {
                    key = keys.getKey(keyId);
                } else if (keys.getKeys().size() == 1) {
                    key = keys.getKeys().get(0);
                }
                keyMap.putAll(keys.getKeyIdMap());
            }
        }
        JwsSignatureVerifier theJwsVerifier = null;
        if (key != null) {
            theJwsVerifier = JwsUtils.getSignatureVerifier(key);
        } else {
            theJwsVerifier = super.getInitializedSignatureVerifier(jwt);
        }
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
