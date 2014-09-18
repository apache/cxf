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
package org.apache.cxf.rs.security.jose.jws;

import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;

public final class JwsUtils {
    private JwsUtils() {
        
    }
    public static JwsSignatureProvider getSignatureProvider(JsonWebKey jwk) {
        return getSignatureProvider(jwk, null);
    }
    public static JwsSignatureProvider getSignatureProvider(JsonWebKey jwk, String defaultAlgorithm) {
        String rsaSignatureAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        JwsSignatureProvider theSigProvider = null;
        if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
            theSigProvider = new PrivateKeyJwsSignatureProvider(JwkUtils.toRSAPrivateKey(jwk));
        } else if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType()) 
            && Algorithm.isHmacSign(rsaSignatureAlgo)) {
            theSigProvider = 
                new HmacJwsSignatureProvider((String)jwk.getProperty(JsonWebKey.OCTET_KEY_VALUE));
        } else if (JsonWebKey.KEY_TYPE_ELLIPTIC.equals(jwk.getKeyType())) {
            theSigProvider = new EcDsaJwsSignatureProvider(JwkUtils.toECPrivateKey(jwk));
        }
        return theSigProvider;
    }
    public static JwsSignatureVerifier getSignatureVerifier(JsonWebKey jwk, String defaultAlgorithm) {
        String rsaSignatureAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        JwsSignatureVerifier theVerifier = null;
        if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
            theVerifier = new PublicKeyJwsSignatureVerifier(JwkUtils.toRSAPublicKey(jwk));
        } else if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType()) 
            && Algorithm.isHmacSign(rsaSignatureAlgo)) {
            theVerifier = 
                new HmacJwsSignatureProvider((String)jwk.getProperty(JsonWebKey.OCTET_KEY_VALUE));
        } else if (JsonWebKey.KEY_TYPE_ELLIPTIC.equals(jwk.getKeyType())) {
            theVerifier = new PublicKeyJwsSignatureVerifier(JwkUtils.toECPublicKey(jwk));
        }
        return theVerifier;
    }
    
}
