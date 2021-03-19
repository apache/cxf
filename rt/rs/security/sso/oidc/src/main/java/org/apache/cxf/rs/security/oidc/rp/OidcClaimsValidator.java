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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtException;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJoseJwtConsumer;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oidc.common.IdToken;

public class OidcClaimsValidator extends OAuthJoseJwtConsumer {
    private static final String SELF_ISSUED_ISSUER = "https://self-issued.me";
    private String issuerId;
    private WebClient jwkSetClient;
    private boolean supportSelfIssuedProvider;
    private boolean strictTimeValidation;
    private ConcurrentHashMap<String, JsonWebKey> keyMap = new ConcurrentHashMap<>();

    /**
     * Validate core JWT claims
     * @param claims the claims
     * @param clientId OAuth2 client id
     * @param validateClaimsAlways if set to true then enforce that the claims
     *                             to be validated must be set
     */
    public void validateJwtClaims(JwtClaims claims, String clientId, boolean validateClaimsAlways) {
        // validate the issuer
        String issuer = claims.getIssuer();
        if (issuer == null && validateClaimsAlways) {
            throw new OAuthServiceException("Invalid issuer");
        }
        if (supportSelfIssuedProvider && issuerId == null
            && issuer != null && SELF_ISSUED_ISSUER.equals(issuer)) {
            validateSelfIssuedProvider(claims, clientId, validateClaimsAlways);
        } else {
            if (issuer != null && !issuer.equals(issuerId)) {
                throw new OAuthServiceException("Invalid issuer");
            }
            // validate subject
            if (claims.getSubject() == null) {
                throw new OAuthServiceException("Invalid subject");
            }

            // validate authorized party
            String authorizedParty = (String)claims.getClaim(IdToken.AZP_CLAIM);
            if (authorizedParty != null && !authorizedParty.equals(clientId)) {
                throw new OAuthServiceException("Invalid authorized party");
            }
            // validate audience
            List<String> audiences = claims.getAudiences();
            if (StringUtils.isEmpty(audiences) && validateClaimsAlways
                || !StringUtils.isEmpty(audiences) && !audiences.contains(clientId)) {
                throw new OAuthServiceException("Invalid audience");
            }

            // If strict time validation: if no issuedTime claim is set then an expiresAt claim must be set
            // Otherwise: validate only if expiresAt claim is set
            boolean expiredRequired =
                validateClaimsAlways || strictTimeValidation && claims.getIssuedAt() == null;
            try {
                JwtUtils.validateJwtExpiry(claims, getClockOffset(), expiredRequired);
            } catch (JwtException ex) {
                throw new OAuthServiceException("ID Token has expired", ex);
            }

            // If strict time validation: If no expiresAt claim is set then an issuedAt claim must be set
            // Otherwise: validate only if issuedAt claim is set
            boolean issuedAtRequired =
                validateClaimsAlways || strictTimeValidation && claims.getExpiryTime() == null;
            try {
                JwtUtils.validateJwtIssuedAt(claims, getTtl(), getClockOffset(), issuedAtRequired);
            } catch (JwtException ex) {
                throw new OAuthServiceException("Invalid issuedAt claim", ex);
            }

            // Validate nbf - but don't require it to be present
            try {
                JwtUtils.validateJwtNotBefore(claims, getClockOffset(), false);
            } catch (JwtException ex) {
                throw new OAuthServiceException("ID Token can not be used yet", ex);
            }
        }
    }

    private void validateSelfIssuedProvider(JwtClaims claims, String clientId, boolean validateClaimsAlways) {
    }

    public void setIssuerId(String issuerId) {
        this.issuerId = issuerId;
    }

    public void setJwkSetClient(WebClient jwkSetClient) {
        this.jwkSetClient = jwkSetClient;
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
            String keyId = jwt.getJwsHeaders().getKeyId();
            key = keyId != null ? keyMap.get(keyId) : null;
            if (key == null && jwkSetClient != null) {
                JsonWebKeys keys = jwkSetClient.get(JsonWebKeys.class);
                if (keyId != null) {
                    key = keys.getKey(keyId);
                } else if (keys.getKeys().size() == 1) {
                    key = keys.getKeys().get(0);
                }
                //jwkSetClient returns the most up-to-date keys
                keyMap.clear();
                keyMap.putAll(keys.getKeyIdMap());
            }
        }
        final JwsSignatureVerifier theJwsVerifier;
        if (key != null) {
            theJwsVerifier = JwsUtils.getSignatureVerifier(key, jwt.getJwsHeaders().getSignatureAlgorithm());
        } else {
            theJwsVerifier = super.getInitializedSignatureVerifier(jwt.getJwsHeaders());
        }
        if (theJwsVerifier == null) {
            throw new SecurityException("JWS Verifier is not available");
        }

        return theJwsVerifier;
    }

    public void setSupportSelfIssuedProvider(boolean supportSelfIssuedProvider) {
        this.supportSelfIssuedProvider = supportSelfIssuedProvider;
    }

    public void setStrictTimeValidation(boolean strictTimeValidation) {
        this.strictTimeValidation = strictTimeValidation;
    }
}
