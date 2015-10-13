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

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseHeaders;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.common.KeyManagementUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.KeyOperation;
import org.apache.cxf.rs.security.jose.jwk.KeyType;

public final class JwsUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(JwsUtils.class);
    private static final String JSON_WEB_SIGNATURE_ALGO_PROP = "rs.security.jws.content.signature.algorithm";
    private static final String RSSEC_SIGNATURE_OUT_PROPS = "rs.security.signature.out.properties";
    private static final String RSSEC_SIGNATURE_IN_PROPS = "rs.security.signature.in.properties";
    private static final String RSSEC_SIGNATURE_PROPS = "rs.security.signature.properties";
    private static final String RSSEC_REPORT_KEY_PROP = "rs.security.jws.report.public.key";
    private static final String RSSEC_REPORT_KEY_ID_PROP = "rs.security.jws.report.public.key.id";
    private JwsUtils() {
        
    }
    public static String sign(PrivateKey key, SignatureAlgorithm algo, String content) {
        return sign(key, algo, content, null);
    }
    
    
    public static String sign(PrivateKey key, SignatureAlgorithm algo, String content, String ct) {
        return sign(getPrivateKeySignatureProvider(key, algo), content, ct);
    }
    public static String sign(byte[] key, SignatureAlgorithm algo, String content) {
        return sign(key, algo, content, null);
    }
    public static String sign(byte[] key, SignatureAlgorithm algo, String content, String ct) {
        return sign(getHmacSignatureProvider(key, algo), content, ct);
    }
    public static String verify(PublicKey key, SignatureAlgorithm algo, String content) {
        JwsCompactConsumer jws = verify(getPublicKeySignatureVerifier(key, algo), content);
        return jws.getDecodedJwsPayload();
    }
    public static String verify(byte[] key, SignatureAlgorithm algo, String content) {
        JwsCompactConsumer jws = verify(getHmacSignatureVerifier(key, algo), content);
        return jws.getDecodedJwsPayload();
    }
    public static JwsSignatureProvider getSignatureProvider(JsonWebKey jwk) {
        return getSignatureProvider(jwk, null);
    }
    public static JwsSignatureProvider getSignatureProvider(JsonWebKey jwk, 
                                                            SignatureAlgorithm defaultAlgorithm) {
        SignatureAlgorithm sigAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm 
            : SignatureAlgorithm.getAlgorithm(jwk.getAlgorithm());
        JwsSignatureProvider theSigProvider = null;
        KeyType keyType = jwk.getKeyType();
        if (KeyType.RSA == keyType) {
            theSigProvider = getPrivateKeySignatureProvider(JwkUtils.toRSAPrivateKey(jwk),
                                                            sigAlgo);
        } else if (KeyType.OCTET == keyType) { 
            byte[] key = JoseUtils.decode((String)jwk.getProperty(JsonWebKey.OCTET_KEY_VALUE));
            theSigProvider = getHmacSignatureProvider(key, sigAlgo);
        } else if (KeyType.EC == jwk.getKeyType()) {
            theSigProvider = getPrivateKeySignatureProvider(JwkUtils.toECPrivateKey(jwk),
                                                            sigAlgo);
        }
        return theSigProvider;
    }
    public static JwsSignatureProvider getPrivateKeySignatureProvider(PrivateKey key, SignatureAlgorithm algo) {
        if (algo == null) {
            LOG.warning("No signature algorithm was defined");
            throw new JwsException(JwsException.Error.ALGORITHM_NOT_SET);
        }
        if (key instanceof ECPrivateKey) {
            return new EcDsaJwsSignatureProvider((ECPrivateKey)key, algo);
        } else if (key instanceof RSAPrivateKey) {
            return new PrivateKeyJwsSignatureProvider(key, algo);
        }
        
        return null;
    }
    public static JwsSignatureProvider getHmacSignatureProvider(byte[] key, SignatureAlgorithm algo) {
        if (algo == null) {
            LOG.warning("No signature algorithm was defined");
            throw new JwsException(JwsException.Error.ALGORITHM_NOT_SET);
        }
        if (AlgorithmUtils.isHmacSign(algo.getJwaName())) {
            return new HmacJwsSignatureProvider(key, algo);
        }
        return null;
    }
    public static JwsSignatureVerifier getSignatureVerifier(JsonWebKey jwk) {
        return getSignatureVerifier(jwk, null);
    }
    public static JwsSignatureVerifier getSignatureVerifier(JsonWebKey jwk, SignatureAlgorithm defaultAlgorithm) {
        SignatureAlgorithm sigAlgo = jwk.getAlgorithm() == null ? defaultAlgorithm 
            : SignatureAlgorithm.getAlgorithm(jwk.getAlgorithm());
        JwsSignatureVerifier theVerifier = null;
        KeyType keyType = jwk.getKeyType();
        if (KeyType.RSA == keyType) {
            theVerifier = getPublicKeySignatureVerifier(JwkUtils.toRSAPublicKey(jwk, true), sigAlgo);
        } else if (KeyType.OCTET == keyType) { 
            byte[] key = JoseUtils.decode((String)jwk.getProperty(JsonWebKey.OCTET_KEY_VALUE));
            theVerifier = getHmacSignatureVerifier(key, sigAlgo);
        } else if (KeyType.EC == keyType) {
            theVerifier = getPublicKeySignatureVerifier(JwkUtils.toECPublicKey(jwk), sigAlgo);
        }
        return theVerifier;
    }
    public static JwsSignatureVerifier getPublicKeySignatureVerifier(X509Certificate cert, SignatureAlgorithm algo) {
        return getPublicKeySignatureVerifier(cert.getPublicKey(), algo);
    }
    public static JwsSignatureVerifier getPublicKeySignatureVerifier(PublicKey key, SignatureAlgorithm algo) {
        if (algo == null) {
            LOG.warning("No signature algorithm was defined");
            throw new JwsException(JwsException.Error.ALGORITHM_NOT_SET);
        }
        
        if (key instanceof RSAPublicKey) {
            return new PublicKeyJwsSignatureVerifier(key, algo);
        } else if (key instanceof ECPublicKey) {
            return new EcDsaJwsSignatureVerifier(key, algo);
        }
        
        return null;
    }
    public static JwsSignatureVerifier getHmacSignatureVerifier(byte[] key, SignatureAlgorithm algo) {
        if (algo == null) {
            LOG.warning("No signature algorithm was defined");
            throw new JwsException(JwsException.Error.ALGORITHM_NOT_SET);
        }
        
        if (AlgorithmUtils.isHmacSign(algo.getJwaName())) {
            return new HmacJwsSignatureVerifier(key, algo);
        }
        return null;
    }
    
    public static Map<SignatureAlgorithm, List<JwsJsonSignatureEntry>> getJwsJsonSignatureMap(
        List<JwsJsonSignatureEntry> signatures) {
        Map<SignatureAlgorithm, List<JwsJsonSignatureEntry>> map = new HashMap<>();
        for (JwsJsonSignatureEntry entry : signatures) {
            SignatureAlgorithm sigAlgorithm = entry.getUnionHeader().getSignatureAlgorithm();
            List<JwsJsonSignatureEntry> entries = map.get(sigAlgorithm);
            if (entries == null) {
                entries = new ArrayList<>();
            }
            entries.add(entry);
            map.put(sigAlgorithm, entries);
        }
        return map;
    }
    
    public static JwsSignatureProvider loadSignatureProvider(boolean required) {
        return loadSignatureProvider(null, required);    
    }
    public static JwsSignatureProvider loadSignatureProvider(JwsHeaders headers, boolean required) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        Properties props = KeyManagementUtils.loadStoreProperties(m, required, 
                                                                  RSSEC_SIGNATURE_OUT_PROPS, RSSEC_SIGNATURE_PROPS);
        if (props == null) {
            return null;
        }
        JwsSignatureProvider theSigProvider = loadSignatureProvider(m, props, headers, false);
        if (headers != null) {
            headers.setSignatureAlgorithm(theSigProvider.getAlgorithm());
        }
        return theSigProvider;
    }
    public static JwsSignatureVerifier loadSignatureVerifier(boolean required) {
        return loadSignatureVerifier(null, required);
    }
    public static JwsSignatureVerifier loadSignatureVerifier(JwsHeaders headers, boolean required) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        Properties props = KeyManagementUtils.loadStoreProperties(m, required, 
                                                                  RSSEC_SIGNATURE_IN_PROPS, RSSEC_SIGNATURE_PROPS);
        if (props == null) {
            return null;
        }
        return loadSignatureVerifier(m, props, headers, false);
    }
    public static List<JwsSignatureProvider> loadSignatureProviders(String propLoc, Message m) {
        Properties props = loadJwsProperties(m, propLoc);
        JwsSignatureProvider theSigProvider = loadSignatureProvider(m, props, null, true);
        if (theSigProvider != null) {
            return Collections.singletonList(theSigProvider);
        }
        List<JwsSignatureProvider> theSigProviders = null; 
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            List<JsonWebKey> jwks = JwkUtils.loadJsonWebKeys(m, props, KeyOperation.SIGN);
            if (jwks != null) {
                theSigProviders = new ArrayList<JwsSignatureProvider>(jwks.size());
                for (JsonWebKey jwk : jwks) {
                    theSigProviders.add(JwsUtils.getSignatureProvider(jwk));
                }
            }
        }
        if (theSigProviders == null) {
            LOG.warning("Providers are not available");
            throw new JwsException(JwsException.Error.NO_PROVIDER);
        }
        return theSigProviders;
    }
    
    public static List<JwsSignatureVerifier> loadSignatureVerifiers(String propLoc, Message m) {
        Properties props = loadJwsProperties(m, propLoc);
        JwsSignatureVerifier theVerifier = loadSignatureVerifier(m, props, null, true);
        if (theVerifier != null) {
            return Collections.singletonList(theVerifier);
        }
        List<JwsSignatureVerifier> theVerifiers = null; 
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            List<JsonWebKey> jwks = JwkUtils.loadJsonWebKeys(m, props, KeyOperation.VERIFY);
            if (jwks != null) {
                theVerifiers = new ArrayList<JwsSignatureVerifier>(jwks.size());
                for (JsonWebKey jwk : jwks) {
                    theVerifiers.add(JwsUtils.getSignatureVerifier(jwk));
                }
            }
        }
        if (theVerifiers == null) {
            LOG.warning("Verifiers are not available");
            throw new JwsException(JwsException.Error.NO_VERIFIER);
        }
        return theVerifiers;
    }
    public static boolean validateCriticalHeaders(JoseHeaders headers) {
        //TODO: validate JWS specific constraints
        return JoseUtils.validateCriticalHeaders(headers);
    }
    private static JwsSignatureProvider loadSignatureProvider(Message m, 
                                                              Properties props,
                                                              JoseHeaders headers,
                                                              boolean ignoreNullProvider) {
        JwsSignatureProvider theSigProvider = null;
        
        boolean reportPublicKey = headers != null && MessageUtils.isTrue(
                MessageUtils.getContextualProperty(m, RSSEC_REPORT_KEY_PROP, 
                                                   KeyManagementUtils.RSSEC_REPORT_KEY_PROP));
        boolean reportPublicKeyId = 
            headers != null && MessageUtils.isTrue(
                MessageUtils.getContextualProperty(m, RSSEC_REPORT_KEY_ID_PROP,
                                                   KeyManagementUtils.RSSEC_REPORT_KEY_ID_PROP));
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, KeyOperation.SIGN);
            if (jwk != null) {
                String signatureAlgo = getSignatureAlgo(m, props, jwk.getAlgorithm(), getDefaultKeyAlgo(jwk));
                theSigProvider = JwsUtils.getSignatureProvider(jwk, SignatureAlgorithm.getAlgorithm(signatureAlgo));
                if (reportPublicKey || reportPublicKeyId) {
                    JwkUtils.setPublicKeyInfo(jwk, headers, signatureAlgo, reportPublicKey, reportPublicKeyId);
                }
            }
        } else {
            String signatureAlgo = getSignatureAlgo(m, props, null, null);
            PrivateKey pk = KeyManagementUtils.loadPrivateKey(m, props, KeyOperation.SIGN);
            theSigProvider = getPrivateKeySignatureProvider(pk, 
                                                            SignatureAlgorithm.getAlgorithm(signatureAlgo));
            if (reportPublicKey) {
                headers.setX509Chain(KeyManagementUtils.loadAndEncodeX509CertificateOrChain(m, props));
            }
        }
        if (theSigProvider == null && !ignoreNullProvider) {
            LOG.warning("Provider is not available");
            throw new JwsException(JwsException.Error.NO_PROVIDER);
        }
        return theSigProvider;
    }
    private static JwsSignatureVerifier loadSignatureVerifier(Message m, 
                                                              Properties props,
                                                              JwsHeaders inHeaders, 
                                                              boolean ignoreNullVerifier) {
        JwsSignatureVerifier theVerifier = null;
        String inHeaderKid = null;
        if (inHeaders != null) {
            inHeaderKid = inHeaders.getKeyId();
            //TODO: optionally validate inHeaders.getAlgorithm against a property in props
            if (inHeaders.getHeader(JoseConstants.HEADER_JSON_WEB_KEY) != null) {
                JsonWebKey publicJwk = inHeaders.getJsonWebKey();
                if (inHeaderKid != null && !inHeaderKid.equals(publicJwk.getKeyId())
                    || !MessageUtils.getContextualBoolean(m, KeyManagementUtils.RSSEC_ACCEPT_PUBLIC_KEY_PROP, true)) {
                    throw new JwsException(JwsException.Error.INVALID_KEY);
                }
                return getSignatureVerifier(publicJwk, 
                                            inHeaders.getSignatureAlgorithm());
            } else if (inHeaders.getHeader(JoseConstants.HEADER_X509_CHAIN) != null) {
                List<X509Certificate> chain = KeyManagementUtils.toX509CertificateChain(inHeaders.getX509Chain());
                KeyManagementUtils.validateCertificateChain(props, chain);
                return getPublicKeySignatureVerifier(chain.get(0).getPublicKey(), 
                                                     inHeaders.getSignatureAlgorithm());
            }
        }
        
        if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(KeyManagementUtils.RSSEC_KEY_STORE_TYPE))) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, KeyOperation.VERIFY, inHeaderKid);
            if (jwk != null) {
                String signatureAlgo = getSignatureAlgo(m, props, jwk.getAlgorithm(), getDefaultKeyAlgo(jwk));
                theVerifier = getSignatureVerifier(jwk, SignatureAlgorithm.getAlgorithm(signatureAlgo));
            }
            
        } else {
            String signatureAlgo = getSignatureAlgo(m, props, null, null);
            theVerifier = getPublicKeySignatureVerifier(
                              KeyManagementUtils.loadPublicKey(m, props), 
                              SignatureAlgorithm.getAlgorithm(signatureAlgo));
        }
        if (theVerifier == null && !ignoreNullVerifier) {
            LOG.warning("Verifier is not available");
            throw new JwsException(JwsException.Error.NO_VERIFIER);
        }
        return theVerifier;
    }
    private static Properties loadJwsProperties(Message m, String propLoc) {
        try {
            return JoseUtils.loadProperties(propLoc, m.getExchange().getBus());
        } catch (Exception ex) {
            LOG.warning("JWS init properties are not available");
            throw new JwsException(JwsException.Error.NO_INIT_PROPERTIES);
        }
    }
    private static String getSignatureAlgo(Message m, Properties props, String algo, String defaultAlgo) {
        if (algo == null) {
            if (defaultAlgo == null) {
                defaultAlgo = AlgorithmUtils.RS_SHA_256_ALGO;
            }
            return KeyManagementUtils.getKeyAlgorithm(m, props, JSON_WEB_SIGNATURE_ALGO_PROP, defaultAlgo);
        }
        return algo;
    }
    private static String getDefaultKeyAlgo(JsonWebKey jwk) {
        KeyType keyType = jwk.getKeyType();
        if (KeyType.OCTET == keyType) {
            return AlgorithmUtils.HMAC_SHA_256_ALGO;
        } else if (KeyType.EC == keyType) {
            return AlgorithmUtils.ES_SHA_256_ALGO;
        } else {
            return AlgorithmUtils.RS_SHA_256_ALGO;
        }
    }
    public static JwsCompactConsumer verify(JwsSignatureVerifier v, String content) {
        JwsCompactConsumer jws = new JwsCompactConsumer(content);
        if (!jws.verifySignatureWith(v)) {
            throw new JwsException(JwsException.Error.INVALID_SIGNATURE);
        }
        return jws;
    }
    public static String sign(JwsSignatureProvider jwsSig, String content, String ct) {
        JwsHeaders headers = new JwsHeaders();
        if (ct != null) {
            headers.setContentType(ct);
        }
        JwsCompactProducer jws = new JwsCompactProducer(headers, content);
        jws.signWith(jwsSig);
        return jws.getSignedEncodedJws();
    }
    public static void validateJwsCertificateChain(List<X509Certificate> certs) {
        
        Message m = PhaseInterceptorChain.getCurrentMessage();
        Properties props = KeyManagementUtils.loadStoreProperties(m, true, 
                                                                  RSSEC_SIGNATURE_IN_PROPS, RSSEC_SIGNATURE_PROPS);
        KeyManagementUtils.validateCertificateChain(props, certs);
    }
    public static boolean isPayloadUnencoded(JwsHeaders jwsHeaders) {
        return jwsHeaders.getPayloadEncodingStatus() == Boolean.FALSE;
    }
}
