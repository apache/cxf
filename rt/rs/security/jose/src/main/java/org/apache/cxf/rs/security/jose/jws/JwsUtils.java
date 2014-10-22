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

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.jaxrs.KeyManagementUtils;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;

public final class JwsUtils {
    private static final String JSON_WEB_SIGNATURE_ALGO_PROP = "rs.security.jws.content.signature.algorithm";
    private JwsUtils() {
        
    }
    public static JwsSignatureProvider getSignatureProvider(JsonWebKey jwk) {
        return getSignatureProvider(jwk, null);
    }
    public static JwsSignatureProvider getSignatureProvider(JsonWebKey jwk, String defaultAlgorithm) {
        String rsaSignatureAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        JwsSignatureProvider theSigProvider = null;
        if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
            theSigProvider = new PrivateKeyJwsSignatureProvider(JwkUtils.toRSAPrivateKey(jwk),
                                                                rsaSignatureAlgo);
        } else if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType()) 
            && Algorithm.isHmacSign(rsaSignatureAlgo)) {
            theSigProvider = 
                new HmacJwsSignatureProvider((String)jwk.getProperty(JsonWebKey.OCTET_KEY_VALUE),
                                             rsaSignatureAlgo);
        } else if (JsonWebKey.KEY_TYPE_ELLIPTIC.equals(jwk.getKeyType())) {
            theSigProvider = new EcDsaJwsSignatureProvider(JwkUtils.toECPrivateKey(jwk),
                                                           rsaSignatureAlgo);
        }
        return theSigProvider;
    }
    public static JwsSignatureVerifier getSignatureVerifier(JsonWebKey jwk) {
        return getSignatureVerifier(jwk, null);
    }
    public static JwsSignatureVerifier getSignatureVerifier(JsonWebKey jwk, String defaultAlgorithm) {
        String rsaSignatureAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        JwsSignatureVerifier theVerifier = null;
        if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
            theVerifier = new PublicKeyJwsSignatureVerifier(JwkUtils.toRSAPublicKey(jwk), rsaSignatureAlgo);
        } else if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType()) 
            && Algorithm.isHmacSign(rsaSignatureAlgo)) {
            theVerifier = 
                new HmacJwsSignatureVerifier((String)jwk.getProperty(JsonWebKey.OCTET_KEY_VALUE), rsaSignatureAlgo);
        } else if (JsonWebKey.KEY_TYPE_ELLIPTIC.equals(jwk.getKeyType())) {
            theVerifier = new EcDsaJwsSignatureVerifier(JwkUtils.toECPublicKey(jwk), rsaSignatureAlgo);
        }
        return theVerifier;
    }
    public static MultivaluedMap<String, JwsJsonSignatureEntry> getJwsJsonSignatureMap(
        List<JwsJsonSignatureEntry> signatures) {
        MultivaluedMap<String, JwsJsonSignatureEntry> map = new MetadataMap<String, JwsJsonSignatureEntry>();
        for (JwsJsonSignatureEntry entry : signatures) {
            map.add(entry.getUnionHeader().getAlgorithm(), entry);
        }
        return map;
    }
    public static JwsSignatureProvider loadSignatureProvider(String propLoc, Message m) {
        Properties props = null;
        try {
            props = ResourceUtils.loadProperties(propLoc, m.getExchange().getBus());
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
        JwsSignatureProvider theSigProvider = null; 
        String rsaSignatureAlgo = null;
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, JsonWebKey.KEY_OPER_SIGN);
            rsaSignatureAlgo = getSignatureAlgo(props, jwk.getAlgorithm());
            theSigProvider = JwsUtils.getSignatureProvider(jwk, rsaSignatureAlgo);
        } else {
            rsaSignatureAlgo = getSignatureAlgo(props, null);
            RSAPrivateKey pk = (RSAPrivateKey)KeyManagementUtils.loadPrivateKey(m, props, 
                KeyManagementUtils.RSSEC_SIG_KEY_PSWD_PROVIDER);
            theSigProvider = new PrivateKeyJwsSignatureProvider(pk, rsaSignatureAlgo);
        }
        if (theSigProvider == null) {
            throw new SecurityException();
        }
        return theSigProvider;
    }
    public static JwsSignatureVerifier loadSignatureVerifier(String propLoc, Message m) {
        Properties props = null;
        try {
            props = ResourceUtils.loadProperties(propLoc, m.getExchange().getBus());
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
        JwsSignatureVerifier theVerifier = null;
        String rsaSignatureAlgo = null;
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, JsonWebKey.KEY_OPER_VERIFY);
            rsaSignatureAlgo = getSignatureAlgo(props, jwk.getAlgorithm());
            theVerifier = JwsUtils.getSignatureVerifier(jwk, rsaSignatureAlgo);
            
        } else {
            rsaSignatureAlgo = getSignatureAlgo(props, null);
            theVerifier = new PublicKeyJwsSignatureVerifier(
                              (RSAPublicKey)KeyManagementUtils.loadPublicKey(m, props), rsaSignatureAlgo);
        }
        return theVerifier;
    }
    private static String getSignatureAlgo(Properties props, String algo) {
        return algo == null ? props.getProperty(JSON_WEB_SIGNATURE_ALGO_PROP) : algo;
    }
}
