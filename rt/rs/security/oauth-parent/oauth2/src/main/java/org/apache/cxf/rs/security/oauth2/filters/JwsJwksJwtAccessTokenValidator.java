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
package org.apache.cxf.rs.security.oauth2.filters;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jaxrs.JsonWebKeysProvider;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.PublicKeyUse;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

/**
 * Validate Access Token signature using JWK Set from
 * {@link org.apache.cxf.rs.security.oauth2.services.AuthorizationMetadata#getJwksURL()} according to
 * {@link JwsHeaders#getKeyId()} 
 */
public class JwsJwksJwtAccessTokenValidator extends JwtAccessTokenValidator {

    final Map<String, JwkHolder> jsonWebKeys = new ConcurrentHashMap<>();

    private String jwksURL;

    @Override
    protected JwsSignatureVerifier getInitializedSignatureVerifier(JwsHeaders jwsHeaders) {
        Objects.requireNonNull(jwsHeaders.getKeyId());
        final JwkHolder jwkHolder = jsonWebKeys.computeIfAbsent(jwsHeaders.getKeyId(), keyId -> updateJwk(keyId));
        return jwkHolder != null ? jwkHolder.getJwsSignatureVerifier() : null;
    }

    public void setJwksURL(String jwksURL) {
        this.jwksURL = jwksURL;
    }

    @Override
    public void setJwsVerifier(JwsSignatureVerifier theJwsVerifier) {
        throw new IllegalArgumentException("Actual JwsSignatureVerifier will be populated from the JWK Set URL");
    }

    private JwkHolder updateJwk(String keyId) {
        Objects.requireNonNull(jwksURL, "JWK Set URL must be specified");
        JwkHolder jwkHolder = null;
        final Set<String> kids = new HashSet<>();
        for (JsonWebKey jwk : getJsonWebKeys().getKeys()) {
            if (PublicKeyUse.ENCRYPT != jwk.getPublicKeyUse()) {
                final String kid = jwk.getKeyId();
                kids.add(kid);
                final JwkHolder h = new JwkHolder(jwk);
                if (keyId.equals(kid)) {
                    jwkHolder = h;
                } else {
                    jsonWebKeys.putIfAbsent(kid, h);
                }
            }
        }
        jsonWebKeys.keySet().removeIf(not(kids::contains));
        return jwkHolder;
    }

    JsonWebKeys getJsonWebKeys() {
        return WebClient.create(jwksURL, Collections.singletonList(new JsonWebKeysProvider()))
            .accept(MediaType.APPLICATION_JSON).get(JsonWebKeys.class);
    }

    // from Java 11
    @SuppressWarnings("unchecked")
    static <T> Predicate<T> not(Predicate<? super T> target) {
        return (Predicate<T>)target.negate();
    }

    private static class JwkHolder {
        private final JsonWebKey jsonWebKey;
        private JwsSignatureVerifier jwsSignatureVerifier;
        JwkHolder(JsonWebKey jsonWebKey) {
            this.jsonWebKey = jsonWebKey;
        }
        public JwsSignatureVerifier getJwsSignatureVerifier() {
            if (null == jwsSignatureVerifier) {
                jwsSignatureVerifier = JwsUtils.getSignatureVerifier(jsonWebKey);
            }
            return jwsSignatureVerifier;
        }
    }

}