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
package org.apache.cxf.rs.security.oauth2.jwk;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.jwe.AesCbcHmacJweDecryption;
import org.apache.cxf.rs.security.oauth2.jwe.AesCbcHmacJweEncryption;
import org.apache.cxf.rs.security.oauth2.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.oauth2.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.oauth2.jwe.KeyDecryptionAlgorithm;
import org.apache.cxf.rs.security.oauth2.jwe.KeyEncryptionAlgorithm;
import org.apache.cxf.rs.security.oauth2.jwe.PbesHmacAesWrapKeyDecryptionAlgorithm;
import org.apache.cxf.rs.security.oauth2.jwe.PbesHmacAesWrapKeyEncryptionAlgorithm;
import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.PrivateKeyPasswordProvider;

public final class JwkUtils {
    public static final String JWK_KEY_STORE_TYPE = "jwk";
    public static final String RSSEC_KEY_STORE_JWKSET = "rs.security.keystore.jwkset";
    public static final String RSSEC_KEY_STORE_JWKKEY = "rs.security.keystore.jwkkey";
    private JwkUtils() {
        
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
    private static JweEncryptionProvider createDefaultEncryption(char[] password) {
        KeyEncryptionAlgorithm keyEncryption = 
            new PbesHmacAesWrapKeyEncryptionAlgorithm(password, Algorithm.PBES2_HS256_A128KW.getJwtName());
        return new AesCbcHmacJweEncryption(Algorithm.PBES2_HS256_A128KW.getJwtName(),
                                           Algorithm.A128CBC_HS256.getJwtName(),
                                           keyEncryption);
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
        JsonWebKeys jwkSet = (JsonWebKeys)m.getExchange().get(props.get(CryptoUtils.RSSEC_KEY_STORE_FILE));
        if (jwkSet == null) {
            jwkSet = loadJwkSet(props, m.getExchange().getBus(), cb, reader);
            m.getExchange().put((String)props.get(CryptoUtils.RSSEC_KEY_STORE_FILE), jwkSet);
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
        String keyStoreLoc = props.getProperty(CryptoUtils.RSSEC_KEY_STORE_FILE);
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
            (PrivateKeyPasswordProvider)m.getContextualProperty(CryptoUtils.RSSEC_KEY_PSWD_PROVIDER);
        if (cb == null && keyOper != null) {
            String propName = keyOper.equals(JsonWebKey.KEY_OPER_SIGN) ? CryptoUtils.RSSEC_SIG_KEY_PSWD_PROVIDER
                : keyOper.equals(JsonWebKey.KEY_OPER_ENCRYPT) ? CryptoUtils.RSSEC_DECRYPT_KEY_PSWD_PROVIDER : null;
            if (propName != null) {
                cb = (PrivateKeyPasswordProvider)m.getContextualProperty(propName);
            }
        }
        JsonWebKeys jwkSet = loadJwkSet(m, props, cb, reader);
        String kid = props.getProperty(CryptoUtils.RSSEC_KEY_STORE_ALIAS);
        if (kid == null && keyOper != null) {
            String keyIdProp = null;
            if (keyOper.equals(JsonWebKey.KEY_OPER_ENCRYPT)) {
                keyIdProp = CryptoUtils.RSSEC_KEY_STORE_ALIAS + ".jwe";
            } else if (keyOper.equals(JsonWebKey.KEY_OPER_SIGN)
                       || keyOper.equals(JsonWebKey.KEY_OPER_VERIFY)) {
                keyIdProp = CryptoUtils.RSSEC_KEY_STORE_ALIAS + ".jws";
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
    private static byte[] stringToBytes(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new SecurityException(ex);
        }
    }
}
