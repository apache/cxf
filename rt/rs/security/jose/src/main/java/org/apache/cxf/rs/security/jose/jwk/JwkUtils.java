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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.crypto.SecretKey;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.JoseUtils;
import org.apache.cxf.rs.security.jose.jaxrs.KeyManagementUtils;
import org.apache.cxf.rs.security.jose.jaxrs.PrivateKeyPasswordProvider;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwe.AesCbcHmacJweDecryption;
import org.apache.cxf.rs.security.jose.jwe.AesCbcHmacJweEncryption;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jwe.KeyDecryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.KeyEncryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.PbesHmacAesWrapKeyDecryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.PbesHmacAesWrapKeyEncryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

public final class JwkUtils {
    public static final String JWK_KEY_STORE_TYPE = "jwk";
    public static final String RSSEC_KEY_STORE_JWKSET = "rs.security.keystore.jwkset";
    public static final String RSSEC_KEY_STORE_JWKKEY = "rs.security.keystore.jwkkey";
    private JwkUtils() {
        
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
        return new DefaultJwkReaderWriter().jsonToJwk(jwkJson);
    }
    public static JsonWebKeys readJwkSet(String jwksJson) {
        return new DefaultJwkReaderWriter().jsonToJwkSet(jwksJson);
    }
    public static String jwkKeyToJson(JsonWebKey jwkKey) {
        return new DefaultJwkReaderWriter().jwkToJson(jwkKey);
    }
    public static String jwkSetToJson(JsonWebKeys jwkSet) {
        return new DefaultJwkReaderWriter().jwkSetToJson(jwkSet);
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
        return encryptJwkSet(jwkSet, password, new DefaultJwkReaderWriter());
    }
    public static String encryptJwkSet(JsonWebKeys jwkSet, char[] password, JwkReaderWriter writer) {
        return encryptJwkSet(jwkSet, createDefaultEncryption(password), writer);
    }
    public static String encryptJwkSet(JsonWebKeys jwkSet, JweEncryptionProvider jwe, JwkReaderWriter writer) {
        return jwe.encrypt(StringUtils.toBytesUTF8(writer.jwkSetToJson(jwkSet)), 
                           toJweHeaders("jwk-set+json"));
    }
    public static String encryptJwkSet(JsonWebKeys jwkSet, RSAPublicKey key, String keyAlgo, String contentAlgo) {
        return JweUtils.encrypt(key, keyAlgo, contentAlgo, StringUtils.toBytesUTF8(jwkSetToJson(jwkSet)),
                                "jwk-set+json");
    }
    public static String signJwkSet(JsonWebKeys jwkSet, RSAPrivateKey key, String algo) {
        return JwsUtils.sign(key, algo, jwkSetToJson(jwkSet), "jwk-set+json");
    }
    public static String encryptJwkSet(JsonWebKeys jwkSet, SecretKey key, String keyAlgo, String contentAlgo) {
        return JweUtils.encrypt(key, keyAlgo, contentAlgo, StringUtils.toBytesUTF8(jwkSetToJson(jwkSet)),
                                "jwk-set+json");
    }
    public static JsonWebKeys decryptJwkSet(String jsonJwkSet, char[] password) {
        return decryptJwkSet(jsonJwkSet, password, new DefaultJwkReaderWriter());
    }
    public static JsonWebKeys decryptJwkSet(String jsonJwkSet, char[] password, JwkReaderWriter reader) {
        return decryptJwkSet(jsonJwkSet, createDefaultDecryption(password), reader);
    }
    public static JsonWebKeys decryptJwkSet(String jsonJwkSet, JweDecryptionProvider jwe, JwkReaderWriter reader) {
        return reader.jsonToJwkSet(jwe.decrypt(jsonJwkSet).getContentText());
    }
    public static JsonWebKeys decryptJwkSet(RSAPrivateKey key, String keyAlgo, String ctAlgo, String jsonJwkSet) {
        return readJwkSet(toString(JweUtils.decrypt(key, keyAlgo, ctAlgo, jsonJwkSet)));
    }
    public static JsonWebKeys verifyJwkSet(RSAPublicKey key, String keyAlgo, String jsonJwk) {
        return readJwkSet(JwsUtils.verify(key, keyAlgo, jsonJwk));
    }
    public static JsonWebKeys decryptJwkSet(SecretKey key, String keyAlgo, String ctAlgo, String jsonJwkSet) {
        return readJwkSet(toString(JweUtils.decrypt(key, keyAlgo, ctAlgo, jsonJwkSet)));
    }
    public static JsonWebKeys decryptJwkSet(InputStream is, char[] password) throws IOException {
        return decryptJwkSet(is, password, new DefaultJwkReaderWriter());
    }
    public static JsonWebKeys decryptJwkSet(InputStream is, char[] password, JwkReaderWriter reader) 
        throws IOException {
        return decryptJwkSet(is, createDefaultDecryption(password), reader);
    }
    public static JsonWebKeys decryptJwkSet(InputStream is, JweDecryptionProvider jwe, JwkReaderWriter reader)
        throws IOException {
        return reader.jsonToJwkSet(jwe.decrypt(IOUtils.readStringFromStream(is)).getContentText());
    }
    public static String encryptJwkKey(JsonWebKey jwk, char[] password) {
        return encryptJwkKey(jwk, password, new DefaultJwkReaderWriter());
    }
    public static String encryptJwkKey(JsonWebKey jwkKey, char[] password, JwkReaderWriter writer) {
        return encryptJwkKey(jwkKey, createDefaultEncryption(password), writer);
    }
    public static String encryptJwkKey(JsonWebKey jwkKey, JweEncryptionProvider jwe, JwkReaderWriter writer) {
        return jwe.encrypt(StringUtils.toBytesUTF8(writer.jwkToJson(jwkKey)), 
                           toJweHeaders("jwk+json"));
    }
    public static String encryptJwkKey(JsonWebKey jwkKey, RSAPublicKey key, String keyAlgo, String contentAlgo) {
        return JweUtils.encrypt(key, keyAlgo, contentAlgo, StringUtils.toBytesUTF8(jwkKeyToJson(jwkKey)),
                                "jwk+json");
    }
    public static String encryptJwkKey(JsonWebKey jwkKey, SecretKey key, String keyAlgo, String contentAlgo) {
        return JweUtils.encrypt(key, keyAlgo, contentAlgo, StringUtils.toBytesUTF8(jwkKeyToJson(jwkKey)),
                                "jwk+json");
    }
    public static String signJwkKey(JsonWebKey jwkKey, RSAPrivateKey key, String algo) {
        return JwsUtils.sign(key, algo, jwkKeyToJson(jwkKey), "jwk+json");
    }
    public static JsonWebKey decryptJwkKey(String jsonJwkKey, char[] password) {
        return decryptJwkKey(jsonJwkKey, password, new DefaultJwkReaderWriter());
    }
    public static JsonWebKey decryptJwkKey(String jsonJwkKey, char[] password, JwkReaderWriter reader) {
        return decryptJwkKey(jsonJwkKey, createDefaultDecryption(password), reader);
    }
    public static JsonWebKey decryptJwkKey(RSAPrivateKey key, String keyAlgo, String ctAlgo, String jsonJwk) {
        return readJwkKey(toString(JweUtils.decrypt(key, keyAlgo, ctAlgo, jsonJwk)));
    }
    public static JsonWebKey verifyJwkKey(RSAPublicKey key, String keyAlgo, String jsonJwk) {
        return readJwkKey(JwsUtils.verify(key, keyAlgo, jsonJwk));
    }
    public static JsonWebKey decryptJwkKey(SecretKey key, String keyAlgo, String ctAlgo, String jsonJwk) {
        return readJwkKey(toString(JweUtils.decrypt(key, keyAlgo, ctAlgo, jsonJwk)));
    }
    public static JsonWebKey decryptJwkKey(String jsonJwkKey, JweDecryptionProvider jwe, JwkReaderWriter reader) {
        return reader.jsonToJwk(jwe.decrypt(jsonJwkKey).getContentText());
    }
    public static JsonWebKey decryptJwkKey(InputStream is, char[] password) throws IOException {
        return decryptJwkKey(is, password, new DefaultJwkReaderWriter());
    }
    public static JsonWebKey decryptJwkKey(InputStream is, char[] password, JwkReaderWriter reader) 
        throws IOException {
        return decryptJwkKey(is, createDefaultDecryption(password), reader);
    }
    public static JsonWebKey decryptJwkKey(InputStream is, JweDecryptionProvider jwe, JwkReaderWriter reader) 
        throws IOException {
        return reader.jsonToJwk(jwe.decrypt(IOUtils.readStringFromStream(is)).getContentText());
    }
    public static JsonWebKeys loadJwkSet(Message m, Properties props, PrivateKeyPasswordProvider cb) {
        return loadJwkSet(m, props, cb, new DefaultJwkReaderWriter());
    }
    public static JsonWebKeys loadJwkSet(Message m, Properties props, PrivateKeyPasswordProvider cb, 
                                         JwkReaderWriter reader) {
        String key = (String)props.get(KeyManagementUtils.RSSEC_KEY_STORE_FILE);
        JsonWebKeys jwkSet = key != null ? (JsonWebKeys)m.getExchange().get(key) : null;
        if (jwkSet == null) {
            jwkSet = loadJwkSet(props, m.getExchange().getBus(), cb, reader);
            if (key != null) {
                m.getExchange().put(key, jwkSet);
            }
        }
        return jwkSet;
    }
    public static JsonWebKeys loadJwkSet(Properties props, Bus bus, PrivateKeyPasswordProvider cb) {
        return loadJwkSet(props, bus, cb, new DefaultJwkReaderWriter());
    }
    public static JsonWebKeys loadJwkSet(Properties props, Bus bus, PrivateKeyPasswordProvider cb, 
                                         JwkReaderWriter reader) {
        JweDecryptionProvider decryption = cb != null
            ? new AesCbcHmacJweDecryption(new PbesHmacAesWrapKeyDecryptionAlgorithm(cb.getPassword(props))) : null;
        return loadJwkSet(props, bus, decryption, reader);
    }
    public static JsonWebKeys loadJwkSet(Properties props, Bus bus, JweDecryptionProvider jwe, JwkReaderWriter reader) {
        String keyContent = null;
        String keyStoreLoc = props.getProperty(KeyManagementUtils.RSSEC_KEY_STORE_FILE);
        if (keyStoreLoc != null) {
            try {
                InputStream is = ResourceUtils.getResourceStream(keyStoreLoc, bus);
                keyContent = IOUtils.readStringFromStream(is);
            } catch (Exception ex) {
                throw new SecurityException(ex);
            }
        } else {
            keyContent = props.getProperty(RSSEC_KEY_STORE_JWKSET);
            if (keyContent == null) {
                keyContent = props.getProperty(RSSEC_KEY_STORE_JWKKEY);
            }
        }
        if (jwe != null) {
            keyContent = jwe.decrypt(keyContent).getContentText();
        }
        if (props.getProperty(RSSEC_KEY_STORE_JWKKEY) == null) {
            return reader.jsonToJwkSet(keyContent);
        } else {
            JsonWebKey key = reader.jsonToJwk(keyContent);
            JsonWebKeys keys = new JsonWebKeys();
            keys.setKeys(Collections.singletonList(key));
            return keys;
        }
    }
    public static JsonWebKey loadJsonWebKey(Message m, Properties props, String keyOper) {
        return loadJsonWebKey(m, props, keyOper, new DefaultJwkReaderWriter());
    }

    public static JsonWebKey loadJsonWebKey(Message m, Properties props, String keyOper, JwkReaderWriter reader) {
        PrivateKeyPasswordProvider cb = KeyManagementUtils.loadPasswordProvider(m, props, keyOper);
        JsonWebKeys jwkSet = loadJwkSet(m, props, cb, reader);
        String kid = 
            KeyManagementUtils.getKeyId(m, props, KeyManagementUtils.RSSEC_KEY_STORE_ALIAS, keyOper);
        if (kid != null) {
            return jwkSet.getKey(kid);
        } else if (keyOper != null) {
            List<JsonWebKey> keys = jwkSet.getKeyUseMap().get(keyOper);
            if (keys != null && keys.size() == 1) {
                return keys.get(0);
            }
        }
        return null;
    }
    public static List<JsonWebKey> loadJsonWebKeys(Message m, Properties props, String keyOper) {
        return loadJsonWebKeys(m, props, keyOper, new DefaultJwkReaderWriter());
    }

    public static List<JsonWebKey> loadJsonWebKeys(Message m, Properties props, String keyOper, 
                                                   JwkReaderWriter reader) {
        PrivateKeyPasswordProvider cb = KeyManagementUtils.loadPasswordProvider(m, props, keyOper);
        JsonWebKeys jwkSet = loadJwkSet(m, props, cb, reader);
        String kid = KeyManagementUtils.getKeyId(m, props, KeyManagementUtils.RSSEC_KEY_STORE_ALIAS, keyOper);
        if (kid != null) {
            return Collections.singletonList(jwkSet.getKey(kid));
        }
        String kids = KeyManagementUtils.getKeyId(m, props, KeyManagementUtils.RSSEC_KEY_STORE_ALIASES, keyOper);
        if (kids != null) {
            String[] values = kids.split(",");
            List<JsonWebKey> keys = new ArrayList<JsonWebKey>(values.length);
            for (String value : values) {
                keys.add(jwkSet.getKey(value));
            }
            return keys;
        }
        if (keyOper != null) {
            List<JsonWebKey> keys = jwkSet.getKeyUseMap().get(keyOper);
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
        JsonWebKey jwk = new JsonWebKey();
        jwk.setKeyType(JsonWebKey.KEY_TYPE_ELLIPTIC);
        jwk.setProperty(JsonWebKey.EC_CURVE, curve);
        jwk.setProperty(JsonWebKey.EC_X_COORDINATE, 
                        Base64UrlUtility.encode(pk.getW().getAffineX().toByteArray()));
        jwk.setProperty(JsonWebKey.EC_Y_COORDINATE, 
                        Base64UrlUtility.encode(pk.getW().getAffineY().toByteArray()));
        return jwk;
    }
    public static JsonWebKey fromECPrivateKey(ECPrivateKey pk, String curve) {
        JsonWebKey jwk = new JsonWebKey();
        jwk.setKeyType(JsonWebKey.KEY_TYPE_ELLIPTIC);
        jwk.setProperty(JsonWebKey.EC_CURVE, curve);
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
                                               Algorithm.toJavaName(jwk.getAlgorithm()));
    }
    public static JsonWebKey fromSecretKey(SecretKey secretKey, String algo) {
        if (!Algorithm.isOctet(algo)) {
            throw new SecurityException("Invalid algorithm");
        }
        JsonWebKey jwk = new JsonWebKey();
        jwk.setKeyType(JsonWebKey.KEY_TYPE_OCTET);
        jwk.setAlgorithm(algo);
        String encodedSecretKey = Base64UrlUtility.encode(secretKey.getEncoded());
        jwk.setProperty(JsonWebKey.OCTET_KEY_VALUE, encodedSecretKey);
        return jwk;
    }
    
    
    private static JweEncryptionProvider createDefaultEncryption(char[] password) {
        KeyEncryptionAlgorithm keyEncryption = 
            new PbesHmacAesWrapKeyEncryptionAlgorithm(password, Algorithm.PBES2_HS256_A128KW.getJwtName());
        return new AesCbcHmacJweEncryption(Algorithm.A128CBC_HS256.getJwtName(), keyEncryption);
    }
    private static JweDecryptionProvider createDefaultDecryption(char[] password) {
        KeyDecryptionAlgorithm keyDecryption = new PbesHmacAesWrapKeyDecryptionAlgorithm(password);
        return new AesCbcHmacJweDecryption(keyDecryption);
    }
    private static JsonWebKey prepareRSAJwk(BigInteger modulus, String algo) {
        if (!Algorithm.isRsa(algo)) {
            throw new SecurityException("Invalid algorithm");
        }
        JsonWebKey jwk = new JsonWebKey();
        jwk.setKeyType(JsonWebKey.KEY_TYPE_RSA);
        jwk.setAlgorithm(algo);
        String encodedModulus = Base64UrlUtility.encode(modulus.toByteArray());
        jwk.setProperty(JsonWebKey.RSA_MODULUS, encodedModulus);
        return jwk;
    }
    private static String toString(byte[] bytes) {
        try {
            return new String(bytes, "UTF-8");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    private static JweHeaders toJweHeaders(String ct) {
        return new JweHeaders(Collections.<String, Object>singletonMap(JoseConstants.HEADER_CONTENT_TYPE, ct));
    }
    public static void setPublicKeyInfo(JsonWebKey jwk, JoseHeaders headers, String algo) {
        if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
            List<String> chain = CastUtils.cast((List<?>)jwk.getProperty("x5c"));
            if (chain != null) {
                headers.setX509Chain(chain);
            } else {
                headers.setJsonWebKey(
                    JwkUtils.fromRSAPublicKey(JwkUtils.toRSAPublicKey(jwk), algo));
            }
        }
    }
}
