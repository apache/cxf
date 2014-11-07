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

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;

public class IdTokenValidator {
    private JweDecryptionProvider jweDecryptor;
    private JwsSignatureVerifier jwsVerifier;
    private String issuerId;
    private int issuedAtRange;

    private WebClient jwkSetClient;

    public JwtToken validateIdToken(ClientAccessToken at, String clientId) {
        String idToken = at.getParameters().get("id_token");
        if (idToken == null) {
            throw new SecurityException("ID Token is missing");
        }
        // Decrypt the token if needed
        if (jweDecryptor != null) {
            idToken = new String(jweDecryptor.decrypt(idToken).getContentText());
        }

        // read id_token into JwtToken
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(idToken);
        JwtToken jwt = jwtConsumer.getJwtToken();
        System.out.println("JWT claims" + jwtConsumer.getDecodedJsonToken().getClaimsJson());
        // validate token signature
        JwsSignatureVerifier theJwsVerifier = loadJwkSignatureVerifier(jwt);
        if (!jwtConsumer.verifySignatureWith(theJwsVerifier)) {
            throw new SecurityException("ID Token signature verification failed");
        }

        // validate audience
        if (!clientId.equals(jwt.getClaims().getAudience())) {
            throw new SecurityException("Invalid audience");
        }

        // validate the provider
        if (!issuerId.equals(jwt.getClaims().getIssuer())) {
            throw new SecurityException("Invalid provider");
        }
        long currentTimeInSecs = System.currentTimeMillis() / 1000;
        long expiryTimeInSecs = jwt.getClaims().getExpiryTime();
        if (currentTimeInSecs > expiryTimeInSecs) {
            throw new SecurityException("The token expired");
        }
        long issuedAtInSecs = jwt.getClaims().getIssuedAt();
        if (issuedAtInSecs > currentTimeInSecs || issuedAtRange > 0
            && issuedAtInSecs < currentTimeInSecs - issuedAtRange) {
            throw new SecurityException("Invalid issuedAt");
        }
        // validate at_hash
        OidcUtils.validateAccessTokenHash(at, jwt);
        return jwt;
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
        JsonWebKeys keys = jwkSetClient.get(JsonWebKeys.class);
        JsonWebKey key = keys.getKey(keyId);
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
}
