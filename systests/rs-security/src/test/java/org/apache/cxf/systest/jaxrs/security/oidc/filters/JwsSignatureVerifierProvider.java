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
package org.apache.cxf.systest.jaxrs.security.oidc.filters;

import java.util.Objects;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

public class JwsSignatureVerifierProvider {

    private WebClient jwksClient;

    public void setJwksClient(WebClient jwksClient) {
        this.jwksClient = jwksClient;
    }

    public JwsSignatureVerifier getJwsSignatureVerifier() {
        for (JsonWebKey jwk : jwksClient.get(JsonWebKeys.class).getKeys()) {
            Objects.requireNonNull(jwk.getKeyId());
            return JwsUtils.getSignatureVerifier(jwk);
        }
        return null;
    }
}