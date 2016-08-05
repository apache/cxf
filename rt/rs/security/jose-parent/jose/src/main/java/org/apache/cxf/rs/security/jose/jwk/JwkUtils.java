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
package org.apache.cxf.rs.security.jose.jwk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.crypto.SecretKey;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseHeaders;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.common.KeyManagementUtils;
import org.apache.cxf.rs.security.jose.common.PrivateKeyPasswordProvider;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.AesCbcHmacJweDecryption;
import org.apache.cxf.rs.security.jose.jwe.AesCbcHmacJweEncryption;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jwe.KeyDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.KeyEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.PbesHmacAesWrapKeyDecryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.PbesHmacAesWrapKeyEncryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.crypto.MessageDigestUtils;

public final class JwkUtils {
    private static final Map<KeyType, List<String>> JWK_REQUIRED_FIELDS_MAP;
    static {
        JWK_REQUIRED_FIELDS_MAP = new HashMap<KeyType, List<String>>();
        JWK_REQUIRED_FIELDS_MAP.put(KeyType.RSA, Arrays.asList(
            JsonWebKey.RSA_PUBLIC_EXP, JsonWebKey.KEY_TYPE, JsonWebKey.RSA_MODULUS));
        JWK_REQUIRED_FIELDS_MAP.put(KeyType.EC, Arrays.asList(
            JsonWebKey.EC_CURVE, JsonWebKey.KEY_TYPE, JsonWebKey.EC_X_COORDINATE, JsonWebKey.EC_Y_COORDINATE));
        JWK_REQUIRED_FIELDS_MAP.put(KeyType.OCTET, Arrays.asList(
            JsonWebKey.OCTET_KEY_VALUE, JsonWebKey.KEY_TYPE));
    }
    private JwkUtils() {
        
    }
    public static String getThumbprint(String keySequence) {
        return getThumbprint(readJwkKey(keySequence));
    }
    public static String getThumbprint(JsonWebKey key) {
        List<String> fields = getRequiredFields(key.getKeyType());
        JsonWebKey thumbprintKey = new JsonWebKey();
        for (String f : fields) {
            thumbprintKey.setProperty(f, key.getProperty(f));
        }
        String json = new JsonMapObjectReaderWriter().toJson(thumbprintKey);
        byte[] digest = MessageDigestUtils.createDigest(json, MessageDigestUtils.ALGO_SHA_256);
        return Base64UrlUtility.encode(digest);
    }
    public static List<String> getRequiredFields(KeyType keyType) {
        return JWK_REQUIRED_FIELDS_MAP.get(keyType);
    }
    public static JsonWebKey readJwkKey(URI uri) throws IOException {
        return readJwkKey(uri.toURL().openStream());
    }
    public static JsonWebKeys readJwkSet(URI uri) throws IOException {
        return readJwkSet(uri.toURL().openStream());
    }
    public static JsonWebKey readJwkKey(InputStream is) throws IOException {
        return readJwkKey(IOUtils.readStringFromStream(is));
    }
    public static JsonWebKeys readJwkSet(InputStream is) throws IOException {
        return readJwkSet(IOUtils.readStringFromStream(is));
    }
    public static JsonWebKey readJwkKey(String jwkJson) {
        return new JwkReaderWriter().jsonToJwk(jwkJson);
    }
    public static JsonWebKeys readJwkSet(String jwksJson) {
        return new JwkReaderWriter().jsonToJwkSet(jwksJson);
    }
    public static String jwkKeyToJson(JsonWebKey jwkKey) {
        return new JwkReaderWriter().jwkToJson(jwkKey);
    }
    public static void jwkKeyToJson(JsonWebKey jwkKey, OutputStream os) throws IOException {
        IOUtils.copy(new ByteArrayInputStream(StringUtils.toBytesUTF8(jwkKeyToJson(jwkKey))), os);
    }
    public static String jwkSetToJson(JsonWebKeys jwkSet) {
        return new JwkReaderWriter().jwkSetToJson(jwkSet);
    }
    public static void jwkSetToJson(JsonWebKeys jwkSet, OutputStream os) throws IOException {
        IOUtils.copy(new ByteArrayInputStream(StringUtils.toBytesUTF8(jwkSetToJson(jwkSet))), os);
    }
    public static String encodeJwkKey(JsonWebKey jwkKey) {
        return Base64UrlUtility.encode(jwkKeyToJson(jwkKey));
    }
    public static String encodeJwkSet(JsonWebKeys jwkSet) {
        return Base64UrlUtility.encode(jwkSetToJson(jwkSet));
    }
    public static JsonWebKey decodeJwkKey(String jwkJson) {
        return readJwkKey(JoseUtils.decodeToString(jwkJson));
    }
    public static JsonWebKeys decodeJwkSet(String jwksJson) {
        return readJwkSet(JoseUtils.decodeToString(jwksJson));
    }
    public static String encryptJwkSet(JsonWebKeys jwkSet, char[] password) {
        return encryptJwkSet(jwkSet, createDefaultEncryption(password));
    }
    public static String encryptJwkSet(JsonWebKeys jwkSet, JweEncryptionProvider jwe) {
        return jwe.encrypt(StringUtils.toBytesUTF8(new JwkReaderWriter().jwkSetToJson(jwkSet)), 
                           toJweHeaders("jwk-set+json"));
    }
    public static String encryptJwkSet(JsonWebKeys jwkSet, PublicKey key, KeyAlgorithm keyAlgo, 
                                       ContentAlgorithm contentAlgo) {
        return JweUtils.encrypt(key, keyAlgo, contentAlgo, StringUtils.toBytesUTF8(jwkSetToJson(jwkSet)),
                                "jwk-set+json");
    }
    public static String signJwkSet(JsonWebKeys jwkSet, PrivateKey key, SignatureAlgorithm algo) {
        return JwsUtils.sign(key, algo, jwkSetToJson(jwkSet), "jwk-set+json");
    }
    public static String encryptJwkSet(JsonWebKeys jwkSet, SecretKey key, KeyAlgorithm keyAlgo, 
                                       ContentAlgorithm contentAlgo) {
        return JweUtils.encrypt(key, keyAlgo, contentAlgo, StringUtils.toBytesUTF8(jwkSetToJson(jwkSet)),
                                "jwk-set+json");
    }
    public static JsonWebKeys decryptJwkSet(String jsonJwkSet, char[] password) {
        return decryptJwkSet(jsonJwkSet, createDefaultDecryption(password));
    }
    public static JsonWebKeys decryptJwkSet(String jsonJwkSet, JweDecryptionProvider jwe) {
        return new JwkReaderWriter().jsonToJwkSet(jwe.decrypt(jsonJwkSet).getContentText());
    }
    public static JsonWebKeys decryptJwkSet(PrivateKey key, KeyAlgorithm keyAlgo, ContentAlgorithm ctAlgo,
                                            String jsonJwkSet) {
        return readJwkSet(toString(JweUtils.decrypt(key, keyAlgo, ctAlgo, jsonJwkSet)));
    }
    public static JsonWebKeys verifyJwkSet(PublicKey key, SignatureAlgorithm keyAlgo, String jsonJwk) {
        return readJwkSet(JwsUtils.verify(key, keyAlgo, jsonJwk));
    }
    public static JsonWebKeys decryptJwkSet(SecretKey key, KeyAlgorithm keyAlgo, ContentAlgorithm ctAlgo, 
                                            String jsonJwkSet) {
        return readJwkSet(toString(JweUtils.decrypt(key, keyAlgo, ctAlgo, jsonJwkSet)));
    }
    public static JsonWebKeys decryptJwkSet(InputStream is, char[] password) 
        throws IOException {
        return decryptJwkSet(is, createDefaultDecryption(password));
    }
    public static JsonWebKeys decryptJwkSet(InputStream is, JweDecryptionProvider jwe)
        throws IOException {
        return new JwkReaderWriter().jsonToJwkSet(
            jwe.decrypt(IOUtils.readStringFromStream(is)).getContentText());
    }
    public static String encryptJwkKey(JsonWebKey jwkKey, char[] password) {
        return encryptJwkKey(jwkKey, createDefaultEncryption(password));
    }
    public static String encryptJwkKey(JsonWebKey jwkKey, JweEncryptionProvider jwe) {
        return jwe.encrypt(StringUtils.toBytesUTF8(new JwkReaderWriter().jwkToJson(jwkKey)), 
                           toJweHeaders("jwk+json"));
    }
    public static String encryptJwkKey(JsonWebKey jwkKey, PublicKey key, KeyAlgorithm keyAlgo, 
                                       ContentAlgorithm contentAlgo) {
        return JweUtils.encrypt(key, keyAlgo, contentAlgo, StringUtils.toBytesUTF8(jwkKeyToJson(jwkKey)),
                                "jwk+json");
    }
    public static String encryptJwkKey(JsonWebKey jwkKey, SecretKey key, KeyAlgorithm keyAlgo, 
                                       ContentAlgorithm contentAlgo) {
        return JweUtils.encrypt(key, keyAlgo, contentAlgo, StringUtils.toBytesUTF8(jwkKeyToJson(jwkKey)),
                                "jwk+json");
    }
    public static String signJwkKey(JsonWebKey jwkKey, PrivateKey key, SignatureAlgorithm algo) {
        return JwsUtils.sign(key, algo, jwkKeyToJson(jwkKey), "jwk+json");
    }
    public static JsonWebKey decryptJwkKey(String jsonJwkKey, char[] password) {
        return decryptJwkKey(jsonJwkKey, createDefaultDecryption(password));
    }
    public static JsonWebKey decryptJwkKey(PrivateKey key, KeyAlgorithm keyAlgo, ContentAlgorithm ctAlgo, 
                                           String jsonJwk) {
        return readJwkKey(toString(JweUtils.decrypt(key, keyAlgo, ctAlgo, jsonJwk)));
    }
    public static JsonWebKey verifyJwkKey(PublicKey key, SignatureAlgorithm keyAlgo, String jsonJwk) {
        return readJwkKey(JwsUtils.verify(key, keyAlgo, jsonJwk));
    }
    public static JsonWebKey decryptJwkKey(SecretKey key, KeyAlgorithm keyAlgo, ContentAlgorithm ctAlgo, 
                                           String jsonJwk) {
        return readJwkKey(toString(JweUtils.decrypt(key, keyAlgo, ctAlgo, jsonJwk)));
    }
    public static JsonWebKey decryptJwkKey(String jsonJwkKey, JweDecryptionProvider jwe) {
        return new JwkReaderWriter().jsonToJwk(jwe.decrypt(jsonJwkKey).getContentText());
    }
    public static JsonWebKey decryptJwkKey(InputStream is, char[] password) 
        throws IOException {
        return decryptJwkKey(is, createDefaultDecryption(password));
    }
    public static JsonWebKey decryptJwkKey(InputStream is, JweDecryptionProvider jwe) 
        throws IOException {
        return new JwkReaderWriter().jsonToJwk(
            jwe.decrypt(IOUtils.readStringFromStream(is)).getContentText());
    }
    public static JsonWebKeys loadPublicJwkSet(Message m, Properties props) {
        return loadJwkSet(m, props, null);
    }
    public static JsonWebKeys loadJwkSet(Message m, Properties props, PrivateKeyPasswordProvider cb) {
        String key = (String)props.get(JoseConstants.RSSEC_KEY_STORE_FILE);
        JsonWebKeys jwkSet = key != null ? (JsonWebKeys)m.getExchange().get(key) : null;
        if (jwkSet == null) {
            jwkSet = loadJwkSet(props, m.getExchange().getBus(), cb);
            if (key != null) {
                m.getExchange().put(key, jwkSet);
            }
        }
        return jwkSet;
    }
    public static JsonWebKeys loadJwkSet(Properties props, Bus bus, PrivateKeyPasswordProvider cb) {
        JweDecryptionProvider decryption = cb != null
            ? new AesCbcHmacJweDecryption(new PbesHmacAesWrapKeyDecryptionAlgorithm(
                cb.getPassword(props))) : null;
        return loadJwkSet(props, bus, decryption);
    }
    public static JsonWebKeys loadJwkSet(Properties props, Bus bus, JweDecryptionProvider jwe) {
        String keyContent = null;
        String keyStoreLoc = props.getProperty(JoseConstants.RSSEC_KEY_STORE_FILE);
        if (keyStoreLoc != null) {
            try {
                InputStream is = JoseUtils.getResourceStream(keyStoreLoc, bus);
                if (is == null) {
                    throw new JwkException("Error in loading keystore location: " + keyStoreLoc);
                }
                keyContent = IOUtils.readStringFromStream(is);
            } catch (Exception ex) {
                throw new JwkException(ex);
            }
        } else {
            keyContent = props.getProperty(JoseConstants.RSSEC_KEY_STORE_JWKSET);
            if (keyContent == null) {
                keyContent = props.getProperty(JoseConstants.RSSEC_KEY_STORE_JWKKEY);
            }
        }
        if (jwe != null) {
            keyContent = jwe.decrypt(keyContent).getContentText();
        }
        JwkReaderWriter reader = new JwkReaderWriter();
        if (props.getProperty(JoseConstants.RSSEC_KEY_STORE_JWKKEY) == null) {
            return reader.jsonToJwkSet(keyContent);
        } else {
            JsonWebKey jwk = reader.jsonToJwk(keyContent);
            return new JsonWebKeys(jwk);
        }
    }
    
    public static JsonWebKey loadJsonWebKey(Message m, Properties props, KeyOperation keyOper) {
        return loadJsonWebKey(m, props, keyOper, null);
    }
    public static JsonWebKey loadJsonWebKey(Message m, Properties props, KeyOperation keyOper, String inHeaderKid) {
        PrivateKeyPasswordProvider cb = KeyManagementUtils.loadPasswordProvider(m, props, keyOper);
        JsonWebKeys jwkSet = loadJwkSet(m, props, cb);
        String kid = null;
        if (inHeaderKid != null 
            && MessageUtils.getContextualBoolean(m, JoseConstants.RSSEC_ACCEPT_PUBLIC_KEY, false)) {
            kid = inHeaderKid;
        } else {
            kid = KeyManagementUtils.getKeyId(m, props, JoseConstants.RSSEC_KEY_STORE_ALIAS, keyOper);
        }
        if (kid != null) {
            return jwkSet.getKey(kid);
        } else if (keyOper != null) {
            List<JsonWebKey> keys = jwkSet.getKeyOperationMap().get(keyOper);
            if (keys != null && keys.size() == 1) {
                return keys.get(0);
            }
        }
        return null;
    }
    public static List<JsonWebKey> loadJsonWebKeys(Message m, 
                                                   Properties props, 
                                                   KeyOperation keyOper) {
        PrivateKeyPasswordProvider cb = KeyManagementUtils.loadPasswordProvider(m, props, keyOper);
        JsonWebKeys jwkSet = loadJwkSet(m, props, cb);
        String kid = KeyManagementUtils.getKeyId(m, props, JoseConstants.RSSEC_KEY_STORE_ALIAS, keyOper);
        if (kid != null) {
            return Collections.singletonList(jwkSet.getKey(kid));
        }
        String kids = KeyManagementUtils.getKeyId(m, props, JoseConstants.RSSEC_KEY_STORE_ALIASES, keyOper);
        if (kids != null) {
            String[] values = kids.split(",");
            List<JsonWebKey> keys = new ArrayList<JsonWebKey>(values.length);
            for (String value : values) {
                keys.add(jwkSet.getKey(value));
            }
            return keys;
        }
        if (keyOper != null) {
            List<JsonWebKey> keys = jwkSet.getKeyOperationMap().get(keyOper);
            if (keys != null && keys.size() == 1) {
                return Collections.singletonList(keys.get(0));
            }
        }
        return null;
    }
    public static RSAPublicKey toRSAPublicKey(JsonWebKey jwk) {
        return toRSAPublicKey(jwk, false);
    }
    public static RSAPublicKey toRSAPublicKey(JsonWebKey jwk, boolean checkX509) {
        String encodedModulus = (String)jwk.getProperty(JsonWebKey.RSA_MODULUS);
        String encodedPublicExponent = (String)jwk.getProperty(JsonWebKey.RSA_PUBLIC_EXP);
        if (encodedModulus != null) {
            return CryptoUtils.getRSAPublicKey(encodedModulus, encodedPublicExponent);
        } else if (checkX509) {
            List<X509Certificate> chain = toX509CertificateChain(jwk);
            return (RSAPublicKey)chain.get(0).getPublicKey();
        }
        return null;
    }
    public static List<X509Certificate> toX509CertificateChain(JsonWebKey jwk) {
        List<String> base64EncodedChain = jwk.getX509Chain();
        return KeyManagementUtils.toX509CertificateChain(base64EncodedChain);
    }
    public static JsonWebKey fromECPublicKey(ECPublicKey pk, String curve) {
        JsonWebKey jwk = prepareECJwk(curve);
        jwk.setProperty(JsonWebKey.EC_X_COORDINATE, 
                        Base64UrlUtility.encode(pk.getW().getAffineX().toByteArray()));
        jwk.setProperty(JsonWebKey.EC_Y_COORDINATE, 
                        Base64UrlUtility.encode(pk.getW().getAffineY().toByteArray()));
        return jwk;
    }
    public static JsonWebKey fromECPrivateKey(ECPrivateKey pk, String curve) {
        JsonWebKey jwk = prepareECJwk(curve);
        jwk.setProperty(JsonWebKey.EC_PRIVATE_KEY, 
                        Base64UrlUtility.encode(pk.getS().toByteArray()));
        return jwk;
    }
    public static JsonWebKey fromRSAPublicKey(RSAPublicKey pk, String algo) {
        JsonWebKey jwk = prepareRSAJwk(pk.getModulus(), algo);
        String encodedPublicExponent = Base64UrlUtility.encode(pk.getPublicExponent().toByteArray());
        jwk.setProperty(JsonWebKey.RSA_PUBLIC_EXP, encodedPublicExponent);
        return jwk;
    }
    public static JsonWebKey fromPublicKey(PublicKey key, Properties props, String algoProp) {
        JsonWebKey jwk = null;
        if (key instanceof RSAPublicKey) {
            String algo = props.getProperty(algoProp);
            jwk = JwkUtils.fromRSAPublicKey((RSAPublicKey)key, algo);
        } else {
            jwk = JwkUtils.fromECPublicKey((ECPublicKey)key,
                                           props.getProperty(JoseConstants.RSSEC_EC_CURVE));
        }
        String kid = props.getProperty(JoseConstants.RSSEC_KEY_STORE_ALIAS);
        if (kid != null) {
            jwk.setKeyId(kid);
        }
        return jwk;
    }
    public static JsonWebKey fromX509CertificateChain(List<X509Certificate> chain, String algo) {
        JsonWebKey jwk = new JsonWebKey();
        jwk.setAlgorithm(algo);
        List<String> encodedChain = KeyManagementUtils.encodeX509CertificateChain(chain);
        jwk.setX509Chain(encodedChain);
        return jwk;
    }
    
    public static RSAPrivateKey toRSAPrivateKey(JsonWebKey jwk) {
        String encodedModulus = (String)jwk.getProperty(JsonWebKey.RSA_MODULUS);
        String encodedPrivateExponent = (String)jwk.getProperty(JsonWebKey.RSA_PRIVATE_EXP);
        String encodedPrimeP = (String)jwk.getProperty(JsonWebKey.RSA_FIRST_PRIME_FACTOR);
        if (encodedPrimeP == null) {
            return CryptoUtils.getRSAPrivateKey(encodedModulus, encodedPrivateExponent);
        } else {
            String encodedPublicExponent = (String)jwk.getProperty(JsonWebKey.RSA_PUBLIC_EXP);
            String encodedPrimeQ = (String)jwk.getProperty(JsonWebKey.RSA_SECOND_PRIME_FACTOR);
            String encodedPrimeExpP = (String)jwk.getProperty(JsonWebKey.RSA_FIRST_PRIME_CRT);
            String encodedPrimeExpQ = (String)jwk.getProperty(JsonWebKey.RSA_SECOND_PRIME_CRT);
            String encodedCrtCoefficient = (String)jwk.getProperty(JsonWebKey.RSA_FIRST_CRT_COEFFICIENT);
            return CryptoUtils.getRSAPrivateKey(encodedModulus, 
                                                encodedPublicExponent,
                                                encodedPrivateExponent,
                                                encodedPrimeP,
                                                encodedPrimeQ,
                                                encodedPrimeExpP,
                                                encodedPrimeExpQ,
                                                encodedCrtCoefficient);
        }
    }
    public static JsonWebKey fromRSAPrivateKey(RSAPrivateKey pk, String algo) {
        JsonWebKey jwk = prepareRSAJwk(pk.getModulus(), algo);
        String encodedPrivateExponent = Base64UrlUtility.encode(pk.getPrivateExponent().toByteArray());
        jwk.setProperty(JsonWebKey.RSA_PRIVATE_EXP, encodedPrivateExponent);
        if (pk instanceof RSAPrivateCrtKey) {
            RSAPrivateCrtKey pkCrt = (RSAPrivateCrtKey)pk;
            jwk.setProperty(JsonWebKey.RSA_FIRST_PRIME_FACTOR, 
                            Base64UrlUtility.encode(pkCrt.getPrimeP().toByteArray()));
            jwk.setProperty(JsonWebKey.RSA_SECOND_PRIME_FACTOR, 
                            Base64UrlUtility.encode(pkCrt.getPrimeQ().toByteArray()));
            jwk.setProperty(JsonWebKey.RSA_FIRST_PRIME_CRT, 
                            Base64UrlUtility.encode(pkCrt.getPrimeExponentP().toByteArray()));
            jwk.setProperty(JsonWebKey.RSA_SECOND_PRIME_CRT, 
                            Base64UrlUtility.encode(pkCrt.getPrimeExponentQ().toByteArray()));
            jwk.setProperty(JsonWebKey.RSA_FIRST_CRT_COEFFICIENT, 
                            Base64UrlUtility.encode(pkCrt.getCrtCoefficient().toByteArray()));
        }
        // "oth" can be populated too if needed
        return jwk;
    }
    public static ECPublicKey toECPublicKey(JsonWebKey jwk) {
        String eCurve = (String)jwk.getProperty(JsonWebKey.EC_CURVE);
        String encodedXCoord = (String)jwk.getProperty(JsonWebKey.EC_X_COORDINATE);
        String encodedYCoord = (String)jwk.getProperty(JsonWebKey.EC_Y_COORDINATE);
        return CryptoUtils.getECPublicKey(eCurve, encodedXCoord, encodedYCoord);
    }
    public static ECPrivateKey toECPrivateKey(JsonWebKey jwk) {
        String eCurve = (String)jwk.getProperty(JsonWebKey.EC_CURVE);
        String encodedPrivateKey = (String)jwk.getProperty(JsonWebKey.EC_PRIVATE_KEY);
        return CryptoUtils.getECPrivateKey(eCurve, encodedPrivateKey);
    }
    
    public static SecretKey toSecretKey(JsonWebKey jwk) {
        return CryptoUtils.createSecretKeySpec((String)jwk.getProperty(JsonWebKey.OCTET_KEY_VALUE), 
                                               AlgorithmUtils.toJavaName(jwk.getAlgorithm()));
    }
    public static JsonWebKey fromSecretKey(SecretKey secretKey, String algo) {
        if (!AlgorithmUtils.isOctet(algo)) {
            throw new JwkException("Invalid algorithm");
        }
        JsonWebKey jwk = new JsonWebKey();
        jwk.setKeyType(KeyType.OCTET);
        jwk.setAlgorithm(algo);
        String encodedSecretKey = Base64UrlUtility.encode(secretKey.getEncoded());
        jwk.setProperty(JsonWebKey.OCTET_KEY_VALUE, encodedSecretKey);
        return jwk;
    }
    
    
    private static JweEncryptionProvider createDefaultEncryption(char[] password) {
        KeyEncryptionProvider keyEncryption = 
            new PbesHmacAesWrapKeyEncryptionAlgorithm(password, KeyAlgorithm.PBES2_HS256_A128KW);
        return new AesCbcHmacJweEncryption(ContentAlgorithm.A128CBC_HS256, keyEncryption);
    }
    private static JweDecryptionProvider createDefaultDecryption(char[] password) {
        KeyDecryptionProvider keyDecryption = new PbesHmacAesWrapKeyDecryptionAlgorithm(password);
        return new AesCbcHmacJweDecryption(keyDecryption);
    }
    private static JsonWebKey prepareRSAJwk(BigInteger modulus, String algo) {
        JsonWebKey jwk = new JsonWebKey();
        jwk.setKeyType(KeyType.RSA);
        if (algo != null) {
            if (!AlgorithmUtils.isRsa(algo)) {
                throw new JwkException("Invalid algorithm");
            }
            jwk.setAlgorithm(algo);
        }
        byte[] modulusBytes = modulus.toByteArray();
        int extraBytesLength = modulusBytes.length - modulus.bitLength() / 8;
        if (extraBytesLength > 0) {
            modulusBytes = Arrays.copyOfRange(modulusBytes, extraBytesLength, modulusBytes.length);
        }
        String encodedModulus = Base64UrlUtility.encode(modulusBytes);
        jwk.setProperty(JsonWebKey.RSA_MODULUS, encodedModulus);
        return jwk;
    }
    private static JsonWebKey prepareECJwk(String curve) {
        JsonWebKey jwk = new JsonWebKey();
        jwk.setKeyType(KeyType.EC);
        jwk.setProperty(JsonWebKey.EC_CURVE, curve);
        return jwk;
    }
    private static String toString(byte[] bytes) {
        try {
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private static JweHeaders toJweHeaders(String ct) {
        return new JweHeaders(Collections.<String, Object>singletonMap(JoseConstants.HEADER_CONTENT_TYPE, ct));
    }
    
    public static void includeCertChain(JsonWebKey jwk, JoseHeaders headers, String algo) {
        if (KeyType.RSA.equals(jwk.getKeyType())) {
            List<String> chain = CastUtils.cast((List<?>)jwk.getProperty(JsonWebKey.X509_CHAIN));
            if (chain != null) {
                headers.setX509Chain(chain);
            }
        }
    }
    
    public static void includePublicKey(JsonWebKey jwk, JoseHeaders headers, String algo) {
        if (KeyType.RSA.equals(jwk.getKeyType())) {
            JsonWebKey jwkPublic = JwkUtils.fromRSAPublicKey(JwkUtils.toRSAPublicKey(jwk), algo);
            if (jwk.getKeyId() != null) {
                jwkPublic.setKeyId(jwk.getKeyId());
            }
            headers.setJsonWebKey(jwkPublic);
        }
    }
}
