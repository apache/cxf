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
import java.net.URI;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.crypto.SecretKey;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.jaxrs.KeyManagementUtils;
import org.apache.cxf.rs.security.jose.jaxrs.PrivateKeyPasswordProvider;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwe.AesCbcHmacJweDecryption;
import org.apache.cxf.rs.security.jose.jwe.AesCbcHmacJweEncryption;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.KeyDecryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.KeyEncryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.PbesHmacAesWrapKeyDecryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.PbesHmacAesWrapKeyEncryptionAlgorithm;

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
    public static String encryptJwkSet(JsonWebKeys jwkSet, char[] password) {
        return encryptJwkSet(jwkSet, password, new DefaultJwkReaderWriter());
    }
    public static String encryptJwkSet(JsonWebKeys jwkSet, char[] password, JwkReaderWriter writer) {
        return encryptJwkSet(jwkSet, createDefaultEncryption(password), writer);
    }
    public static String encryptJwkSet(JsonWebKeys jwkSet, JweEncryptionProvider jwe, JwkReaderWriter writer) {
        return jwe.encrypt(stringToBytes(writer.jwkSetToJson(jwkSet)), "jwk-set+json");
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
        return jwe.encrypt(stringToBytes(writer.jwkToJson(jwkKey)), "jwk+json");
    }
    public static JsonWebKey decryptJwkKey(String jsonJwkKey, char[] password) {
        return decryptJwkKey(jsonJwkKey, password, new DefaultJwkReaderWriter());
    }
    public static JsonWebKey decryptJwkKey(String jsonJwkKey, char[] password, JwkReaderWriter reader) {
        return decryptJwkKey(jsonJwkKey, createDefaultDecryption(password), reader);
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
    private static JweEncryptionProvider createDefaultEncryption(char[] password) {
        KeyEncryptionAlgorithm keyEncryption = 
            new PbesHmacAesWrapKeyEncryptionAlgorithm(password, Algorithm.PBES2_HS256_A128KW.getJwtName());
        return new AesCbcHmacJweEncryption(Algorithm.A128CBC_HS256.getJwtName(), keyEncryption);
    }
    private static JweDecryptionProvider createDefaultDecryption(char[] password) {
        KeyDecryptionAlgorithm keyDecryption = new PbesHmacAesWrapKeyDecryptionAlgorithm(password);
        return new AesCbcHmacJweDecryption(keyDecryption);
    }
    public static JsonWebKeys loadJwkSet(Message m, Properties props, PrivateKeyPasswordProvider cb) {
        return loadJwkSet(m, props, cb, new DefaultJwkReaderWriter());
    }
    public static JsonWebKeys loadJwkSet(Message m, Properties props, PrivateKeyPasswordProvider cb, 
                                         JwkReaderWriter reader) {
        JsonWebKeys jwkSet = (JsonWebKeys)m.getExchange().get(props.get(KeyManagementUtils.RSSEC_KEY_STORE_FILE));
        if (jwkSet == null) {
            jwkSet = loadJwkSet(props, m.getExchange().getBus(), cb, reader);
            m.getExchange().put((String)props.get(KeyManagementUtils.RSSEC_KEY_STORE_FILE), jwkSet);
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
        PrivateKeyPasswordProvider cb = 
            (PrivateKeyPasswordProvider)m.getContextualProperty(KeyManagementUtils.RSSEC_KEY_PSWD_PROVIDER);
        if (cb == null && keyOper != null) {
            String propName = keyOper.equals(JsonWebKey.KEY_OPER_SIGN) ? KeyManagementUtils.RSSEC_SIG_KEY_PSWD_PROVIDER
                : keyOper.equals(JsonWebKey.KEY_OPER_ENCRYPT) 
                ? KeyManagementUtils.RSSEC_DECRYPT_KEY_PSWD_PROVIDER : null;
            if (propName != null) {
                cb = (PrivateKeyPasswordProvider)m.getContextualProperty(propName);
            }
        }
        JsonWebKeys jwkSet = loadJwkSet(m, props, cb, reader);
        String kid = props.getProperty(KeyManagementUtils.RSSEC_KEY_STORE_ALIAS);
        if (kid == null && keyOper != null) {
            String keyIdProp = null;
            if (keyOper.equals(JsonWebKey.KEY_OPER_ENCRYPT)) {
                keyIdProp = KeyManagementUtils.RSSEC_KEY_STORE_ALIAS + ".jwe";
            } else if (keyOper.equals(JsonWebKey.KEY_OPER_SIGN)
                       || keyOper.equals(JsonWebKey.KEY_OPER_VERIFY)) {
                keyIdProp = KeyManagementUtils.RSSEC_KEY_STORE_ALIAS + ".jws";
            }
            if (keyIdProp != null) {
                kid = props.getProperty(keyIdProp);
            }
        }
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
    public static RSAPublicKey toRSAPublicKey(JsonWebKey jwk) {
        String encodedModulus = (String)jwk.getProperty(JsonWebKey.RSA_MODULUS);
        String encodedPublicExponent = (String)jwk.getProperty(JsonWebKey.RSA_PUBLIC_EXP);
        return CryptoUtils.getRSAPublicKey(encodedModulus, encodedPublicExponent);
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
    private static byte[] stringToBytes(String str) {
        return StringUtils.toBytesUTF8(str);
    }
}
