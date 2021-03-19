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
package org.apache.cxf.rs.security.httpsignature.utils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.rs.security.httpsignature.HTTPSignatureConstants;
import org.apache.cxf.rs.security.httpsignature.exception.SignatureException;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.rs.PrivateKeyPasswordProvider;

public final class KeyManagementUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(KeyManagementUtils.class);
    private static final String CLASSPATH_PREFIX = "classpath:";

    private KeyManagementUtils() {

    }

    public static Properties loadSignatureOutProperties() {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        return loadStoreProperties(m, HTTPSignatureConstants.RSSEC_SIGNATURE_OUT_PROPS,
                                   HTTPSignatureConstants.RSSEC_SIGNATURE_PROPS);
    }

    public static Properties loadSignatureInProperties() {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        return loadStoreProperties(m, HTTPSignatureConstants.RSSEC_SIGNATURE_IN_PROPS,
                                   HTTPSignatureConstants.RSSEC_SIGNATURE_PROPS);

    }

    private static Properties loadStoreProperties(Message m, String storeProp1, String storeProp2) {
        if (m == null) {
            return null;
        }
        Properties props = null;
        String propLoc =
            (String)MessageUtils.getContextualProperty(m, storeProp1, storeProp2);
        if (propLoc != null) {
            try {
                props = loadProperties(propLoc, m.getExchange().getBus());
            } catch (Exception ex) {
                LOG.warning("Properties resource is not identified");
                throw new SignatureException("Properties resource is not identified", ex);
            }
        } else {
            String keyFile = (String)m.getContextualProperty(HTTPSignatureConstants.RSSEC_KEY_STORE_FILE);
            if (keyFile != null) {
                props = new Properties();
                props.setProperty(HTTPSignatureConstants.RSSEC_KEY_STORE_FILE, keyFile);
                String type = (String)m.getContextualProperty(HTTPSignatureConstants.RSSEC_KEY_STORE_TYPE);
                if (type == null) {
                    type = "JKS";
                }
                props.setProperty(HTTPSignatureConstants.RSSEC_KEY_STORE_TYPE, type);
                String alias = (String)m.getContextualProperty(HTTPSignatureConstants.RSSEC_KEY_STORE_ALIAS);
                if (alias != null) {
                    props.setProperty(HTTPSignatureConstants.RSSEC_KEY_STORE_ALIAS, alias);
                }
                String keystorePassword = (String)m.getContextualProperty(HTTPSignatureConstants.RSSEC_KEY_STORE_PSWD);
                if (keystorePassword != null) {
                    props.setProperty(HTTPSignatureConstants.RSSEC_KEY_STORE_PSWD, keystorePassword);
                }
                String keyPassword = (String)m.getContextualProperty(HTTPSignatureConstants.RSSEC_KEY_PSWD);
                if (keyPassword != null) {
                    props.setProperty(HTTPSignatureConstants.RSSEC_KEY_PSWD, keyPassword);
                }
            }
        }
        return props;
    }

    public static PrivateKey loadPrivateKey(Message m, Properties props) {
        KeyStore keyStore = loadPersistKeyStore(m, props);

        String keyPswd = props.getProperty(HTTPSignatureConstants.RSSEC_KEY_PSWD);
        String alias = props.getProperty(HTTPSignatureConstants.RSSEC_KEY_STORE_ALIAS);
        char[] keyPswdChars = keyPswd != null ? keyPswd.toCharArray() : null;
        if (keyPswdChars == null) {
            PrivateKeyPasswordProvider provider = loadPasswordProvider(m, props);
            keyPswdChars = provider != null ? provider.getPassword(props) : null;
        }
        return CryptoUtils.loadPrivateKey(keyStore, keyPswdChars, alias);
    }

    public static PublicKey loadPublicKey(Message m, Properties props) {
        KeyStore keyStore = loadPersistKeyStore(m, props);

        String alias = props.getProperty(HTTPSignatureConstants.RSSEC_KEY_STORE_ALIAS);
        return CryptoUtils.loadCertificate(keyStore, alias).getPublicKey();
    }

    private static PrivateKeyPasswordProvider loadPasswordProvider(Message m, Properties props) {
        PrivateKeyPasswordProvider cb = null;
        if (props.containsKey(HTTPSignatureConstants.RSSEC_KEY_PSWD_PROVIDER)) {
            cb = (PrivateKeyPasswordProvider)props.get(HTTPSignatureConstants.RSSEC_KEY_PSWD_PROVIDER);
        } else if (m != null) {
            cb = (PrivateKeyPasswordProvider)m.getContextualProperty(HTTPSignatureConstants.RSSEC_KEY_PSWD_PROVIDER);
        }
        return cb;
    }

    private static KeyStore loadPersistKeyStore(Message m, Properties props) {
        KeyStore keyStore = null;
        if (props.containsKey(HTTPSignatureConstants.RSSEC_KEY_STORE)) {
            keyStore = (KeyStore)props.get(HTTPSignatureConstants.RSSEC_KEY_STORE);
        }

        if (keyStore == null) {
            if (!props.containsKey(HTTPSignatureConstants.RSSEC_KEY_STORE_FILE)) {
                LOG.warning("No keystore file has been configured");
                throw new SignatureException("No keystore file has been configured");
            }
            if (m != null) {
                Object keyStoreProp = m.getExchange().get(props.get(HTTPSignatureConstants.RSSEC_KEY_STORE_FILE));
                if (keyStoreProp != null && !(keyStoreProp instanceof KeyStore)) {
                    throw new SignatureException("Unexpected key store class: " + keyStoreProp.getClass().getName());
                } else {
                    keyStore = (KeyStore)keyStoreProp;
                }
            }
        }

        if (keyStore == null) {
            Bus bus = m != null ? m.getExchange().getBus() : null;
            keyStore = loadKeyStore(props, bus);
            if (m != null) {
                m.getExchange().put((String)props.get(HTTPSignatureConstants.RSSEC_KEY_STORE_FILE), keyStore);
            }
        }
        return keyStore;
    }

    private static KeyStore loadKeyStore(Properties props, Bus bus) {
        String keyStoreLoc = props.getProperty(HTTPSignatureConstants.RSSEC_KEY_STORE_FILE);
        String keyStoreType = props.getProperty(HTTPSignatureConstants.RSSEC_KEY_STORE_TYPE);
        String keyStorePswd = props.getProperty(HTTPSignatureConstants.RSSEC_KEY_STORE_PSWD);

        return loadKeyStore(keyStoreLoc, keyStoreType, keyStorePswd, bus);
    }

    private static KeyStore loadKeyStore(String keyStoreLoc,
                                        String keyStoreType,
                                        String keyStorePswd,
                                        Bus bus) {
        if (keyStorePswd == null) {
            throw new SignatureException("No keystore password was defined");
        }
        try {
            InputStream is = getResourceStream(keyStoreLoc, bus);
            return CryptoUtils.loadKeyStore(is, keyStorePswd.toCharArray(), keyStoreType);
        } catch (Exception ex) {
            LOG.warning("Key store can not be loaded");
            throw new SignatureException("Key store can not be loaded", ex);
        }
    }

    //
    // <Start> Copied from JAX-RS RT FRONTEND ResourceUtils
    //

    private static InputStream getResourceStream(String loc, Bus bus) throws Exception {
        URL url = getResourceURL(loc, bus);
        return url == null ? null : url.openStream();
    }

    private static URL getResourceURL(String loc, Bus bus) throws Exception {
        URL url;
        if (loc.startsWith(CLASSPATH_PREFIX)) {
            String path = loc.substring(CLASSPATH_PREFIX.length());
            url = getClasspathResourceURL(path, KeyManagementUtils.class, bus);
        } else {
            try {
                url = new URL(loc);
            } catch (Exception ex) {
                // it can be either a classpath or file resource without a scheme
                url = getClasspathResourceURL(loc, KeyManagementUtils.class, bus);
                if (url == null) {
                    File file = new File(loc);
                    if (file.exists()) {
                        url = file.toURI().toURL();
                    }
                }
            }
        }
        if (url == null) {
            LOG.warning("No resource " + loc + " is available");
        }
        return url;
    }

    private static URL getClasspathResourceURL(String path, Class<?> callingClass, Bus bus) {
        URL url = ClassLoaderUtils.getResource(path, callingClass);
        return url == null ? getResource(path, URL.class, bus) : url;
    }

    private static <T> T getResource(String path, Class<T> resourceClass, Bus bus) {
        if (bus != null) {
            ResourceManager rm = bus.getExtension(ResourceManager.class);
            if (rm != null) {
                return rm.resolveResource(path, resourceClass);
            }
        }
        return null;
    }

    private static Properties loadProperties(String propertiesLocation, Bus bus) throws Exception {
        Properties props = new Properties();
        try (InputStream is = getResourceStream(propertiesLocation, bus)) {
            if (is == null) {
                throw new SignatureException("The properties file " + propertiesLocation + " could not be read");
            }
            props.load(is);
        }
        return props;
    }

    //
    // <End> Copied from JAX-RS RT FRONTEND ResourceUtils
    //
}