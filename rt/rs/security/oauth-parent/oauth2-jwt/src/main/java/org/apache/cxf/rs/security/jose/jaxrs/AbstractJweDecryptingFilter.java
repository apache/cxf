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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.util.Properties;

import javax.crypto.SecretKey;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwe.AesCbcHmacJweDecryption;
import org.apache.cxf.rs.security.jose.jwe.AesGcmWrapKeyDecryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.AesWrapKeyDecryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweCryptoProperties;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionOutput;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.RSAOaepKeyDecryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.WrappedKeyDecryptionAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.WrappedKeyJweDecryption;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

public class AbstractJweDecryptingFilter {
    private static final String RSSEC_ENCRYPTION_IN_PROPS = "rs.security.encryption.in.properties";
    private static final String RSSEC_ENCRYPTION_PROPS = "rs.security.encryption.properties";
    private static final String JSON_WEB_ENCRYPTION_CEK_ALGO_PROP = "rs.security.jwe.content.encryption.algorithm";    
    private JweDecryptionProvider decryption;
    private JweCryptoProperties cryptoProperties;
    private String defaultMediaType;
    protected JweDecryptionOutput decrypt(InputStream is) throws IOException {
        JweDecryptionProvider theDecryptor = getInitializedDecryptionProvider();
        JweDecryptionOutput out = theDecryptor.decrypt(new String(IOUtils.readBytesFromStream(is), "UTF-8"));
        validateHeaders(out.getHeaders());
        return out;
    }

    protected void validateHeaders(JweHeaders headers) {
        // complete
    }
    public void setDecryptionProvider(JweDecryptionProvider decryptor) {
        this.decryption = decryptor;
    }
    protected JweDecryptionProvider getInitializedDecryptionProvider() {
        if (decryption != null) {
            return decryption;    
        } 
        Message m = JAXRSUtils.getCurrentMessage();
        String propLoc = 
            (String)MessageUtils.getContextualProperty(m, RSSEC_ENCRYPTION_IN_PROPS, RSSEC_ENCRYPTION_PROPS);
        if (propLoc == null) {
            throw new SecurityException();
        }
        Bus bus = m.getExchange().getBus();
        try {
            WrappedKeyDecryptionAlgorithm keyDecryptionProvider = null;
            Properties props = ResourceUtils.loadProperties(propLoc, bus);
            if (JwkUtils.JWK_KEY_STORE_TYPE.equals(props.get(CryptoUtils.RSSEC_KEY_STORE_TYPE))) {
                //TODO: Private JWK sets can be JWE encrypted
                JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, props, JsonWebKey.KEY_OPER_ENCRYPT);
                if (JsonWebKey.KEY_TYPE_RSA.equals(jwk.getKeyType())) {
                    keyDecryptionProvider = new RSAOaepKeyDecryptionAlgorithm(jwk.toRSAPrivateKey());
                } else if (JsonWebKey.KEY_TYPE_OCTET.equals(jwk.getKeyType())) {
                    SecretKey key = jwk.toSecretKey();
                    if (Algorithm.isAesKeyWrap(jwk.getAlgorithm())) {
                        keyDecryptionProvider = new AesWrapKeyDecryptionAlgorithm(key);
                    } else if (Algorithm.isAesGcmKeyWrap(jwk.getAlgorithm())) {
                        keyDecryptionProvider = new AesGcmWrapKeyDecryptionAlgorithm(key);
                    } 
                } else {
                    // TODO: support elliptic curve keys
                }
            } else {
                keyDecryptionProvider = new RSAOaepKeyDecryptionAlgorithm(
                    (RSAPrivateKey)CryptoUtils.loadPrivateKey(m, props, CryptoUtils.RSSEC_DECRYPT_KEY_PSWD_PROVIDER));
            }
            if (keyDecryptionProvider == null) {
                throw new SecurityException();
            }
            String contentEncryptionAlgo = props.getProperty(JSON_WEB_ENCRYPTION_CEK_ALGO_PROP);
            boolean isAesHmac = Algorithm.isAesCbcHmac(contentEncryptionAlgo);
            if (isAesHmac) { 
                return new AesCbcHmacJweDecryption(keyDecryptionProvider);
            } else {
                return new WrappedKeyJweDecryption(keyDecryptionProvider, cryptoProperties, null);
            }
            
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
        
    }

    public void setCryptoProperties(JweCryptoProperties cryptoProperties) {
        this.cryptoProperties = cryptoProperties;
    }

    public String getDefaultMediaType() {
        return defaultMediaType;
    }

    public void setDefaultMediaType(String defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }

}
