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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.JoseUtils;
import org.apache.cxf.rs.security.jose.jaxrs.KeyManagementUtils;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;

public final class JwsUtils {
    private static final String JSON_WEB_SIGNATURE_ALGO_PROP = "rs.security.jws.content.signature.algorithm";
    private static final String RSSEC_SIGNATURE_OUT_PROPS = "rs.security.signature.out.properties";
    private static final String RSSEC_SIGNATURE_IN_PROPS = "rs.security.signature.in.properties";
    private static final String RSSEC_SIGNATURE_PROPS = "rs.security.signature.properties";
    private JwsUtils() {
        
    }
    public static String sign(RSAPrivateKey key, String algo, String content) {
        return sign(key, algo, content, null);
    }
    
    
    public static String sign(RSAPrivateKey key, String algo, String content, String ct) {
        return sign(getRSAKeySignatureProvider(key, algo), content, ct);
    }
    public static String sign(byte[] key, String algo, String content) {
        return sign(key, algo, content, null);
    }
    public static String sign(byte[] key, String algo, String content, String ct) {
        return sign(getHmacSignatureProvider(key, algo), content, ct);
    }
    public static String verify(RSAPublicKey key, String algo, String content) {
        JwsCompactConsumer jws = verify(getRSAKeySignatureVerifier(key, algo), content);
        return jws.getDecodedJwsPayload();
    }
    public static String verify(byte[] key, String algo, String content) {
        JwsCompactConsumer jws = verify(getHmacSignatureVerifier(key, algo), content);
        return jws.getDecodedJwsPayload();
    }
    public static JwsSignatureProvider getSignatureProvider(JsonWebKey jwk) {
        return getSignatureProvider(jwk, null);
    }
    public static JwsSignatureProvider getSignatureProvider(JsonWebKey jwk, String defaultAlgorithm) {
        String rsaSignatureAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        JwsSignatureProvider theSigProvider = null;
        if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
            theSigProvider = getRSAKeySignatureProvider(JwkUtils.toRSAPrivateKey(jwk),
                                                        rsaSignatureAlgo);
            
            
        } else if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType())) { 
            byte[] key = JoseUtils.decode((String)jwk.getProperty(JsonWebKey.OCTET_KEY_VALUE));
            theSigProvider = getHmacSignatureProvider(key, rsaSignatureAlgo);
        } else if (JsonWebKey.KEY_TYPE_ELLIPTIC.equals(jwk.getKeyType())) {
            theSigProvider = new EcDsaJwsSignatureProvider(JwkUtils.toECPrivateKey(jwk),
                                                           rsaSignatureAlgo);
        }
        return theSigProvider;
    }
    public static JwsSignatureProvider getRSAKeySignatureProvider(RSAPrivateKey key, String algo) {
        return new PrivateKeyJwsSignatureProvider(key, algo);
    }
    public static JwsSignatureProvider getHmacSignatureProvider(byte[] key, String algo) {
        if (Algorithm.isHmacSign(algo)) {
            return new HmacJwsSignatureProvider(key, algo);
        }
        return null;
    }
    public static JwsSignatureVerifier getSignatureVerifier(JsonWebKey jwk) {
        return getSignatureVerifier(jwk, null);
    }
    public static JwsSignatureVerifier getSignatureVerifier(JsonWebKey jwk, String defaultAlgorithm) {
        String rsaSignatureAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm : jwk.getAlgorithm();
        JwsSignatureVerifier theVerifier = null;
        if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
            theVerifier = getRSAKeySignatureVerifier(JwkUtils.toRSAPublicKey(jwk, true), rsaSignatureAlgo);
        } else if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType())) { 
            byte[] key = JoseUtils.decode((String)jwk.getProperty(JsonWebKey.OCTET_KEY_VALUE));
            theVerifier = getHmacSignatureVerifier(key, rsaSignatureAlgo);
        } else if (JsonWebKey.KEY_TYPE_ELLIPTIC.equals(jwk.getKeyType())) {
            theVerifier = new EcDsaJwsSignatureVerifier(JwkUtils.toECPublicKey(jwk), rsaSignatureAlgo);
        }
        return theVerifier;
    }
    public static JwsSignatureVerifier getRSAKeySignatureVerifier(RSAPublicKey key, String algo) {
        return new PublicKeyJwsSignatureVerifier(key, algo);
    }
    public static JwsSignatureVerifier getHmacSignatureVerifier(byte[] key, String algo) {
        if (Algorithm.isHmacSign(algo)) {
            return new HmacJwsSignatureVerifier(key, algo);
        }
        return null;
    }
    public static MultivaluedMap<String, JwsJsonSignatureEntry> getJwsJsonSignatureMap(
        List<JwsJsonSignatureEntry> signatures) {
        MultivaluedMap<String, JwsJsonSignatureEntry> map = new MetadataMap<String, JwsJsonSignatureEntry>();
        for (JwsJsonSignatureEntry entry : signatures) {
            map.add(entry.getUnionHeader().getAlgorithm(), entry);
        }
        return map;
    }
    public static JwsSignatureProvider loadSignatureProvider(boolean required) {
        Message m = JAXRSUtils.getCurrentMessage();
        if (m != null) {
            String propLoc = 
                (String)MessageUtils.getContextualProperty(m, RSSEC_SIGNATURE_OUT_PROPS, RSSEC_SIGNATURE_PROPS);
            if (propLoc != null) {
                return loadSignatureProvider(propLoc, m);
            }
        }
        if (required) {
            throw new SecurityException();
        }
        return null;
    }
    public static JwsSignatureProvider loadSignatureProvider(String propLoc, Message m) {
        return loadSignatureProvider(propLoc, m, false);
    }
    public static JwsSignatureVerifier loadSignatureVerifier(boolean required) {
        Message m = JAXRSUtils.getCurrentMessage();
        if (m != null) {
            String propLoc = 
                (String)MessageUtils.getContextualProperty(m, RSSEC_SIGNATURE_IN_PROPS, RSSEC_SIGNATURE_PROPS);
            if (propLoc != null) {
                return loadSignatureVerifier(propLoc, m);
            }
        }
        if (required) {
            throw new SecurityException();
        }
        return null;
    }
    public static List<JwsSignatureProvider> loadSignatureProviders(String propLoc, Message m) {
        Properties props = loadProperties(m, propLoc);
        JwsSignatureProvider theSigProvider = loadSignatureProvider(propLoc, m, true);
        if (theSigProvider != null) {
            return Collections.singletonList(theSigProvider);
        }
        List<JwsSignatureProvider> theSigProviders = null; 
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            List<JsonWebKey> jwks = JwkUtils.loadJsonWebKeys(m, props, JsonWebKey.KEY_OPER_SIGN);
            if (jwks != null) {
                theSigProviders = new ArrayList<JwsSignatureProvider>(jwks.size());
                for (JsonWebKey jwk : jwks) {
                    theSigProviders.add(JwsUtils.getSignatureProvider(jwk));
                }
            }
        }
        if (theSigProviders == null) {
            throw new SecurityException();
        }
        return theSigProviders;
    }
    public static JwsSignatureVerifier loadSignatureVerifier(String propLoc, Message m) {
        return loadSignatureVerifier(propLoc, m, false);
    }
    
    public static List<JwsSignatureVerifier> loadSignatureVerifiers(String propLoc, Message m) {
        Properties props = loadProperties(m, propLoc);
        JwsSignatureVerifier theVerifier = loadSignatureVerifier(propLoc, m, true);
        if (theVerifier != null) {
            return Collections.singletonList(theVerifier);
        }
        List<JwsSignatureVerifier> theVerifiers = null; 
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            List<JsonWebKey> jwks = JwkUtils.loadJsonWebKeys(m, props, JsonWebKey.KEY_OPER_VERIFY);
            if (jwks != null) {
                theVerifiers = new ArrayList<JwsSignatureVerifier>(jwks.size());
                for (JsonWebKey jwk : jwks) {
                    theVerifiers.add(JwsUtils.getSignatureVerifier(jwk));
                }
            }
        }
        if (theVerifiers == null) {
            throw new SecurityException();
        }
        return theVerifiers;
    }
    public static boolean validateCriticalHeaders(JoseHeaders headers) {
        //TODO: validate JWS specific constraints
        return JoseUtils.validateCriticalHeaders(headers);
    }
    private static JwsSignatureProvider loadSignatureProvider(String propLoc, Message m, boolean ignoreNullProvider) {
        Properties props = loadProperties(m, propLoc);
        JwsSignatureProvider theSigProvider = null; 
        String rsaSignatureAlgo = null;
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, JsonWebKey.KEY_OPER_SIGN);
            if (jwk != null) {
                rsaSignatureAlgo = getSignatureAlgo(m, props, jwk.getAlgorithm());
                theSigProvider = JwsUtils.getSignatureProvider(jwk, rsaSignatureAlgo);
            }
        } else {
            rsaSignatureAlgo = getSignatureAlgo(m, props, null);
            RSAPrivateKey pk = (RSAPrivateKey)KeyManagementUtils.loadPrivateKey(m, props, 
                JsonWebKey.KEY_OPER_SIGN);
            theSigProvider = getRSAKeySignatureProvider(pk, rsaSignatureAlgo);
        }
        if (theSigProvider == null && !ignoreNullProvider) {
            throw new SecurityException();
        }
        return theSigProvider;
    }
    private static JwsSignatureVerifier loadSignatureVerifier(String propLoc, Message m, boolean ignoreNullVerifier) {
        Properties props = loadProperties(m, propLoc);
        JwsSignatureVerifier theVerifier = null;
        String rsaSignatureAlgo = null;
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, JsonWebKey.KEY_OPER_VERIFY);
            if (jwk != null) {
                rsaSignatureAlgo = getSignatureAlgo(m, props, jwk.getAlgorithm());
                theVerifier = JwsUtils.getSignatureVerifier(jwk, rsaSignatureAlgo);
            }
            
        } else {
            rsaSignatureAlgo = getSignatureAlgo(m, props, null);
            theVerifier = getRSAKeySignatureVerifier(
                              (RSAPublicKey)KeyManagementUtils.loadPublicKey(m, props), rsaSignatureAlgo);
        }
        if (theVerifier == null && !ignoreNullVerifier) {
            throw new SecurityException();
        }
        return theVerifier;
    }
    private static Properties loadProperties(Message m, String propLoc) {
        try {
            return ResourceUtils.loadProperties(propLoc, m.getExchange().getBus());
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    private static String getSignatureAlgo(Message m, Properties props, String algo) {
        if (algo == null) {
            return KeyManagementUtils.getKeyAlgorithm(m, props, 
                                               JSON_WEB_SIGNATURE_ALGO_PROP, JoseConstants.RS_SHA_256_ALGO);
        }
        return algo;
    }
    private static JwsCompactConsumer verify(JwsSignatureVerifier v, String content) {
        JwsCompactConsumer jws = new JwsCompactConsumer(content);
        if (!jws.verifySignatureWith(v)) {
            throw new SecurityException();
        }
        return jws;
    }
    private static String sign(JwsSignatureProvider jwsSig, String content, String ct) {
        JoseHeaders headers = new JoseHeaders();
        if (ct != null) {
            headers.setContentType(ct);
        }
        JwsCompactProducer jws = new JwsCompactProducer(headers, content);
        jws.signWith(jwsSig);
        return jws.getSignedEncodedJws();
    }
}
