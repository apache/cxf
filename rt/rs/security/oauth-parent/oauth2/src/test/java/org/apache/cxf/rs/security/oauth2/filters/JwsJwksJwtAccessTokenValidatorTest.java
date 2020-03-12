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

import java.util.Arrays;
import java.util.HashSet;

import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.PublicKeyUse;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.NoneJwsSignatureVerifier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JwsJwksJwtAccessTokenValidatorTest {

    @Test
    public void testGetInitializedSignatureVerifier() {
        final JsonWebKey jwk = new JsonWebKey();
        jwk.setKeyId("anyKid");
        jwk.setPublicKeyUse(PublicKeyUse.ENCRYPT);
        final JsonWebKey jwk1 = new JsonWebKey();
        jwk1.setKeyId("kid1");
        final JsonWebKey jwk2 = new JsonWebKey();
        jwk2.setKeyId("kid2");
        jwk2.setPublicKeyUse(PublicKeyUse.SIGN);
        final JsonWebKey jwk3 = new JsonWebKey();
        jwk3.setKeyId("kid3");
        jwk3.setPublicKeyUse(PublicKeyUse.SIGN);

        final JwsJwksJwtAccessTokenValidator validator = new JwsJwksJwtAccessTokenValidator() {
            int invokeCnt;
            @Override
            JsonWebKeys getJsonWebKeys() {
                ++invokeCnt;
                if (invokeCnt == 1) {
                    return new JsonWebKeys(Arrays.asList(jwk, jwk1, jwk2));
                } else if (invokeCnt == 2) {
                    return new JsonWebKeys(Arrays.asList(jwk, jwk1, jwk3));
                }
                throw new IllegalStateException();
            }
        };
        validator.setJwksURL("https://any.url");

        validator.getInitializedSignatureVerifier(new JwsHeaders(jwk2.getKeyId()));
        assertEquals(new HashSet<>(Arrays.asList(jwk1.getKeyId(), jwk2.getKeyId())),
            validator.jsonWebKeys.keySet());

        // rotate keys
        validator.getInitializedSignatureVerifier(new JwsHeaders(jwk3.getKeyId()));
        assertEquals(new HashSet<>(Arrays.asList(jwk1.getKeyId(), jwk3.getKeyId())),
            validator.jsonWebKeys.keySet());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetJwsVerifier() {
        new JwsJwksJwtAccessTokenValidator().setJwsVerifier(new NoneJwsSignatureVerifier());
    }

    @Test(expected = NullPointerException.class)
    public void testGetInitializedSignatureVerifierUninitialized() {
        new JwsJwksJwtAccessTokenValidator().getInitializedSignatureVerifier(new JwsHeaders("kid"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetInitializedSignatureVerifierNoKid() {
        new JwsJwksJwtAccessTokenValidator().getInitializedSignatureVerifier(new JwsHeaders());
    }

}