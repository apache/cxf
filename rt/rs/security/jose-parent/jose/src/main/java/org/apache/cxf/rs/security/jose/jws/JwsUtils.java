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

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAKey;
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
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.common.KeyManagementUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.KeyOperation;
import org.apache.cxf.rs.security.jose.jwk.KeyType;
import org.apache.cxf.rs.security.jose.jwk.PublicKeyUse;
import org.apache.cxf.rt.security.crypto.MessageDigestUtils;

public final class JwsUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(JwsUtils.class);
    
    private JwsUtils() {
        
    }
    public static String sign(PrivateKey key, SignatureAlgorithm algo, String content) {
        return sign(key, algo, content, null);
    }
    
    
    public static String sign(PrivateKey key, SignatureAlgorithm algo, String content, String ct) {
        return sign(getPrivateKeySignatureProvider(key, algo), content, ct);
    }
    public static String sign(String encodedKey, SignatureAlgorithm algo, String content) {
        return sign(JoseUtils.decode(encodedKey), algo, content);
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
    public static String verify(String encodedKey, SignatureAlgorithm algo, String content) {
        return verify(JoseUtils.decode(encodedKey), algo, content);
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
    public static JwsSignatureProvider getHmacSignatureProvider(String encodedKey, SignatureAlgorithm algo) {
        return getHmacSignatureProvider(JoseUtils.decode(encodedKey), algo);
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
        if (algo == null) {
            LOG.warning("No signature algorithm was defined");
            throw new JwsException(JwsException.Error.ALGORITHM_NOT_SET);
        }
        
        if (cert != null) {
            if (cert.getPublicKey() instanceof RSAPublicKey) {
                return new PublicKeyJwsSignatureVerifier(cert, algo);
            } else if (cert.getPublicKey() instanceof ECPublicKey) {
                return new EcDsaJwsSignatureVerifier(cert, algo);
            }
        }
        
        return null;
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
    public static JwsSignatureVerifier getHmacSignatureVerifier(String encodedKey, SignatureAlgorithm algo) {
        return getHmacSignatureVerifier(JoseUtils.decode(encodedKey), algo);    
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
        Properties props = loadSignatureOutProperties(required);
        if (props == null) {
            return null;
        }
        JwsSignatureProvider theSigProvider = loadSignatureProvider(props, headers);
        if (headers != null) {
            headers.setSignatureAlgorithm(theSigProvider.getAlgorithm());
        }
        return theSigProvider;
    }
    public static Properties loadSignatureOutProperties(boolean required) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        return KeyManagementUtils.loadStoreProperties(m, required, 
                                                      JoseConstants.RSSEC_SIGNATURE_OUT_PROPS, 
                                                      JoseConstants.RSSEC_SIGNATURE_PROPS);
        
    }
    public static Properties loadSignatureInProperties(boolean required) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        return KeyManagementUtils.loadStoreProperties(m, required, 
                                                      JoseConstants.RSSEC_SIGNATURE_IN_PROPS, 
                                                      JoseConstants.RSSEC_SIGNATURE_PROPS);
        
    }
    public static Properties loadSignatureProperties(String propertiesName, boolean required) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        return KeyManagementUtils.loadStoreProperties(m, required, propertiesName, null);
        
    }
    public static JwsSignatureVerifier loadSignatureVerifier(boolean required) {
        return loadSignatureVerifier(null, required);
    }
    public static JwsSignatureVerifier loadSignatureVerifier(JwsHeaders headers, boolean required) {
        Properties props = loadSignatureInProperties(required);
        return loadSignatureVerifier(props, headers);
    }
    public static List<JwsSignatureProvider> loadSignatureProviders(String propLoc, Message m) {
        Properties props = loadJwsProperties(m, propLoc);
        JwsSignatureProvider theSigProvider = loadSignatureProvider(m, props, null, true);
        if (theSigProvider != null) {
            return Collections.singletonList(theSigProvider);
        }
        List<JwsSignatureProvider> theSigProviders = null; 
        if (JoseConstants.HEADER_JSON_WEB_KEY.equals(props.get(JoseConstants.RSSEC_KEY_STORE_TYPE))) {
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
        if (JoseConstants.HEADER_JSON_WEB_KEY.equals(props.get(JoseConstants.RSSEC_KEY_STORE_TYPE))) {
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
    public static boolean validateCriticalHeaders(JwsHeaders headers) {
        //TODO: validate JWS specific constraints
        return JoseUtils.validateCriticalHeaders(headers);
    }
    public static JwsSignatureProvider loadSignatureProvider(Properties props,
                                                             JwsHeaders headers) {
        return loadSignatureProvider(PhaseInterceptorChain.getCurrentMessage(),
                                     props, headers, false);
    }
    private static JwsSignatureProvider loadSignatureProvider(Message m, 
                                                             Properties props,
                                                             JwsHeaders headers,
                                                             boolean ignoreNullProvider) {
        JwsSignatureProvider theSigProvider = null;
        
        boolean includeCert = headers != null && MessageUtils.getContextualBoolean(
                m, JoseConstants.RSSEC_SIGNATURE_INCLUDE_CERT, false);
        boolean includeCertSha1 = headers != null && MessageUtils.getContextualBoolean(
                m, JoseConstants.RSSEC_SIGNATURE_INCLUDE_CERT_SHA1, false);
        
        if (JoseConstants.HEADER_JSON_WEB_KEY.equals(props.get(JoseConstants.RSSEC_KEY_STORE_TYPE))) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, KeyOperation.SIGN);
            if (jwk != null) {
                SignatureAlgorithm signatureAlgo = getSignatureAlgorithm(m, 
                                                             props, 
                                                             SignatureAlgorithm.getAlgorithm(jwk.getAlgorithm()), 
                                                             getDefaultKeyAlgorithm(jwk));
                theSigProvider = JwsUtils.getSignatureProvider(jwk, signatureAlgo);
                
                boolean includePublicKey = headers != null && MessageUtils.getContextualBoolean(
                    m, JoseConstants.RSSEC_SIGNATURE_INCLUDE_PUBLIC_KEY, false);
                boolean includeKeyId = headers != null && MessageUtils.getContextualBoolean(
                    m, JoseConstants.RSSEC_SIGNATURE_INCLUDE_KEY_ID, false);
                
                if (includeCert) {
                    JwkUtils.includeCertChain(jwk, headers, signatureAlgo.getJwaName());
                }
                if (includeCertSha1) {
                    String digest = KeyManagementUtils.loadDigestAndEncodeX509Certificate(m, props);
                    if (digest != null) {
                        headers.setX509Thumbprint(digest);
                    }
                }
                if (includePublicKey) {
                    JwkUtils.includePublicKey(jwk, headers, signatureAlgo.getJwaName());
                }
                if (includeKeyId && jwk.getKeyId() != null && headers != null) {
                    headers.setKeyId(jwk.getKeyId());
                }
            }
        } else {
            SignatureAlgorithm signatureAlgo = getSignatureAlgorithm(m, props, null, null);
            if (signatureAlgo == SignatureAlgorithm.NONE) {
                theSigProvider = new NoneJwsSignatureProvider();
            } else {
                PrivateKey pk = KeyManagementUtils.loadPrivateKey(m, props, KeyOperation.SIGN);
                theSigProvider = getPrivateKeySignatureProvider(pk, signatureAlgo);
                if (includeCert) {
                    headers.setX509Chain(KeyManagementUtils.loadAndEncodeX509CertificateOrChain(m, props));
                }
                if (includeCertSha1) {
                    String digest = KeyManagementUtils.loadDigestAndEncodeX509Certificate(m, props);
                    if (digest != null) {
                        headers.setX509Thumbprint(digest);
                    }
                }
            }
        }
        if (theSigProvider == null && !ignoreNullProvider) {
            LOG.warning("Provider is not available");
            throw new JwsException(JwsException.Error.NO_PROVIDER);
        }
        return theSigProvider;
    }
    public static JwsSignatureVerifier loadSignatureVerifier(Properties props,
                                                             JwsHeaders inHeaders) {
        return loadSignatureVerifier(PhaseInterceptorChain.getCurrentMessage(),
                                     props, inHeaders, false);
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
                    || !MessageUtils.getContextualBoolean(m, JoseConstants.RSSEC_ACCEPT_PUBLIC_KEY, false)) {
                    throw new JwsException(JwsException.Error.INVALID_KEY);
                }
                return getSignatureVerifier(publicJwk, 
                                            inHeaders.getSignatureAlgorithm());
            } else if (inHeaders.getHeader(JoseConstants.HEADER_X509_CHAIN) != null) {
                List<X509Certificate> chain = KeyManagementUtils.toX509CertificateChain(inHeaders.getX509Chain());
                KeyManagementUtils.validateCertificateChain(props, chain);
                return getPublicKeySignatureVerifier(chain.get(0), 
                                                     inHeaders.getSignatureAlgorithm());
            } else if (inHeaders.getHeader(JoseConstants.HEADER_X509_THUMBPRINT) != null) {
                X509Certificate foundCert = 
                    KeyManagementUtils.getCertificateFromThumbprint(inHeaders.getX509Thumbprint(), 
                                                                    MessageDigestUtils.ALGO_SHA_1,
                                                                    m, props);
                if (foundCert != null) {
                    return getPublicKeySignatureVerifier(foundCert, 
                                                         inHeaders.getSignatureAlgorithm());
                }
            }
        }
        
        if (JoseConstants.HEADER_JSON_WEB_KEY.equals(props.get(JoseConstants.RSSEC_KEY_STORE_TYPE))) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, KeyOperation.VERIFY, inHeaderKid);
            if (jwk != null) {
                SignatureAlgorithm signatureAlgo = getSignatureAlgorithm(m, props, 
                                                             SignatureAlgorithm.getAlgorithm(jwk.getAlgorithm()), 
                                                             getDefaultKeyAlgorithm(jwk));
                theVerifier = getSignatureVerifier(jwk, signatureAlgo);
            }
            
        } else {
            SignatureAlgorithm signatureAlgo = getSignatureAlgorithm(m, props, null, null);
            if (signatureAlgo == SignatureAlgorithm.NONE 
                && SignatureAlgorithm.NONE.getJwaName().equals(inHeaders.getAlgorithm())) {
                theVerifier = new NoneJwsSignatureVerifier();
            } else {
                X509Certificate[] certs = KeyManagementUtils.loadX509CertificateOrChain(m, props);
                if (certs != null && certs.length > 0) {
                    theVerifier = getPublicKeySignatureVerifier(certs[0], signatureAlgo);
                }
            }
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
    
    @SuppressWarnings("deprecation")
    public static SignatureAlgorithm getSignatureAlgorithm(Message m, Properties props, 
                                               SignatureAlgorithm algo, 
                                               SignatureAlgorithm defaultAlgo) {
        if (algo == null) {
            if (defaultAlgo == null) {
                defaultAlgo = SignatureAlgorithm.RS256;
            }
            
            // Check for deprecated identifier first
            String sigAlgo = null;
            if (props != null) {
                sigAlgo = props.getProperty(JoseConstants.DEPR_RSSEC_SIGNATURE_ALGORITHM);
            }
            if (sigAlgo == null && m != null) {
                sigAlgo = (String)m.getContextualProperty(JoseConstants.DEPR_RSSEC_SIGNATURE_ALGORITHM);
            }
            if (sigAlgo != null) {
                return SignatureAlgorithm.getAlgorithm(sigAlgo);
            }
            
            // Otherwise check newer identifier
            if (props != null) {
                return getSignatureAlgorithm(props, defaultAlgo);
            }
        }
        return algo;
    }
    public static SignatureAlgorithm getSignatureAlgorithm(Properties props, 
                                               SignatureAlgorithm defaultAlgo) {
        String algo = KeyManagementUtils.getKeyAlgorithm(PhaseInterceptorChain.getCurrentMessage(),
                                                  props, 
                                                  JoseConstants.RSSEC_SIGNATURE_ALGORITHM, 
                                                  defaultAlgo == null ? null : defaultAlgo.getJwaName());
        return SignatureAlgorithm.getAlgorithm(algo);
    }
    private static SignatureAlgorithm getDefaultKeyAlgorithm(JsonWebKey jwk) {
        KeyType keyType = jwk.getKeyType();
        if (KeyType.OCTET == keyType) {
            return SignatureAlgorithm.HS256;
        } else if (KeyType.EC == keyType) {
            return SignatureAlgorithm.ES256;
        } else {
            return SignatureAlgorithm.RS256;
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
        
        Properties props = loadSignatureInProperties(true);
        KeyManagementUtils.validateCertificateChain(props, certs);
    }
    public static boolean isPayloadUnencoded(JwsHeaders jwsHeaders) {
        return jwsHeaders.getPayloadEncodingStatus() == Boolean.FALSE;
    }
    
    public static void checkSignatureKeySize(Key key) {
        if (key instanceof RSAKey && ((RSAKey)key).getModulus().bitLength() < 2048) {
            LOG.fine("A key of size: " + ((RSAKey)key).getModulus().bitLength()
                     + " was used with an RSA signature algorithm. 2048 is the minimum size that is accepted");
            throw new JwsException(JwsException.Error.INVALID_KEY);
        }
    }
    public static JsonWebKeys loadPublicVerificationKeys(Message m, Properties props) {
        String storeType = props.getProperty(JoseConstants.RSSEC_KEY_STORE_TYPE);
        if ("jwk".equals(storeType)) {
            return JwkUtils.loadPublicJwkSet(m, props);
        } else {
            X509Certificate[] certs = null;
            if (PropertyUtils.isTrue(props.get(JoseConstants.RSSEC_SIGNATURE_INCLUDE_CERT))) {
                certs = KeyManagementUtils.loadX509CertificateOrChain(m, props);
            }
            PublicKey key = certs != null && certs.length > 0 
                ? certs[0].getPublicKey() : KeyManagementUtils.loadPublicKey(m, props); 
            JsonWebKey jwk = JwkUtils.fromPublicKey(key, props, JoseConstants.RSSEC_SIGNATURE_ALGORITHM);
            jwk.setPublicKeyUse(PublicKeyUse.SIGN);
            if (certs != null) {
                jwk.setX509Chain(KeyManagementUtils.encodeX509CertificateChain(certs));
            }
            return new JsonWebKeys(jwk);
        }
    }
}
