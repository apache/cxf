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

import java.io.InputStream;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Properties;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.security.SecurityContext;


/**
 * Encryption helpers
 */
public final class KeyManagementUtils {
    public static final String RSSEC_KEY_STORE_TYPE = "rs.security.keystore.type";
    public static final String RSSEC_KEY_STORE_PSWD = "rs.security.keystore.password";
    public static final String RSSEC_KEY_PSWD = "rs.security.key.password";
    public static final String RSSEC_KEY_STORE_ALIAS = "rs.security.keystore.alias";
    public static final String RSSEC_KEY_STORE_FILE = "rs.security.keystore.file";
    public static final String RSSEC_PRINCIPAL_NAME = "rs.security.principal.name";
    public static final String RSSEC_KEY_PSWD_PROVIDER = "rs.security.key.password.provider";
    public static final String RSSEC_SIG_KEY_PSWD_PROVIDER = "rs.security.signature.key.password.provider";
    public static final String RSSEC_DECRYPT_KEY_PSWD_PROVIDER = "rs.security.decryption.key.password.provider";
    
    private KeyManagementUtils() {
    }
    
    public static PublicKey loadPublicKey(Message m, Properties props) {
        KeyStore keyStore = KeyManagementUtils.loadPersistKeyStore(m, props);
        return CryptoUtils.loadPublicKey(keyStore, props.getProperty(RSSEC_KEY_STORE_ALIAS));
    }
    public static PublicKey loadPublicKey(Message m, String keyStoreLocProp) {
        return loadPublicKey(m, keyStoreLocProp, null);
    }
    public static PublicKey loadPublicKey(Message m, String keyStoreLocPropPreferred, String keyStoreLocPropDefault) {
        String keyStoreLoc = getMessageProperty(m, keyStoreLocPropPreferred, keyStoreLocPropDefault);
        Bus bus = m.getExchange().getBus();
        try {
            Properties props = ResourceUtils.loadProperties(keyStoreLoc, bus);
            return KeyManagementUtils.loadPublicKey(m, props);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    private static String getMessageProperty(Message m, String keyStoreLocPropPreferred, 
                                             String keyStoreLocPropDefault) {
        String propLoc = 
            (String)MessageUtils.getContextualProperty(m, keyStoreLocPropPreferred, keyStoreLocPropDefault);
        if (propLoc == null) {
            throw new SecurityException();
        }
        return propLoc;
    }
    public static PrivateKey loadPrivateKey(Properties props, Bus bus, PrivateKeyPasswordProvider provider) {
        KeyStore keyStore = loadKeyStore(props, bus);
        return loadPrivateKey(keyStore, props, bus, provider);
    }
    public static PrivateKey loadPrivateKey(KeyStore keyStore, 
                                            Properties props, 
                                            Bus bus, 
                                            PrivateKeyPasswordProvider provider) {
        
        String keyPswd = props.getProperty(RSSEC_KEY_PSWD);
        String alias = props.getProperty(RSSEC_KEY_STORE_ALIAS);
        char[] keyPswdChars = provider != null ? provider.getPassword(props) 
            : keyPswd != null ? keyPswd.toCharArray() : null;    
        return CryptoUtils.loadPrivateKey(keyStore, keyPswdChars, alias);
    }
    
    public static PrivateKey loadPrivateKey(Message m, String keyStoreLocProp, String passwordProviderProp) {
        return loadPrivateKey(m, keyStoreLocProp, null, passwordProviderProp);
    }
    public static PrivateKey loadPrivateKey(Message m, String keyStoreLocPropPreferred,
                                            String keyStoreLocPropDefault, String passwordProviderProp) {
        String keyStoreLoc = getMessageProperty(m, keyStoreLocPropPreferred, keyStoreLocPropDefault);
        Bus bus = m.getExchange().getBus();
        try {
            Properties props = ResourceUtils.loadProperties(keyStoreLoc, bus);
            return KeyManagementUtils.loadPrivateKey(m, props, passwordProviderProp);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    public static PrivateKey loadPrivateKey(Message m, Properties props, String passwordProviderProp) {
        Bus bus = m.getExchange().getBus();
        KeyStore keyStore = KeyManagementUtils.loadPersistKeyStore(m, props);
        PrivateKeyPasswordProvider cb = 
            (PrivateKeyPasswordProvider)m.getContextualProperty(passwordProviderProp);
        if (cb != null && m.getExchange().getInMessage() != null) {
            SecurityContext sc = m.getExchange().getInMessage().get(SecurityContext.class);
            if (sc != null) {
                Principal p = sc.getUserPrincipal();
                if (p != null) {
                    props.setProperty(RSSEC_PRINCIPAL_NAME, p.getName());
                }
            }
        }
        return KeyManagementUtils.loadPrivateKey(keyStore, props, bus, cb);
    }
    public static KeyStore loadPersistKeyStore(Message m, Properties props) {
        KeyStore keyStore = (KeyStore)m.getExchange().get(props.get(KeyManagementUtils.RSSEC_KEY_STORE_FILE));
        if (keyStore == null) {
            keyStore = KeyManagementUtils.loadKeyStore(props, m.getExchange().getBus());
            m.getExchange().put((String)props.get(KeyManagementUtils.RSSEC_KEY_STORE_FILE), keyStore);
        }
        return keyStore;
    }
    public static KeyStore loadKeyStore(Properties props, Bus bus) {
        String keyStoreType = props.getProperty(RSSEC_KEY_STORE_TYPE);
        String keyStoreLoc = props.getProperty(RSSEC_KEY_STORE_FILE);
        String keyStorePswd = props.getProperty(RSSEC_KEY_STORE_PSWD);
        try {
            InputStream is = ResourceUtils.getResourceStream(keyStoreLoc, bus);
            return CryptoUtils.loadKeyStore(is, keyStorePswd.toCharArray(), keyStoreType);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
}
