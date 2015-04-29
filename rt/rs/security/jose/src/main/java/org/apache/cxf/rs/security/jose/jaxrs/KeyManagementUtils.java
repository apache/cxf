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
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.jose.JoseException;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.security.SecurityContext;


/**
 * Encryption helpers
 */
public final class KeyManagementUtils {
    public static final String RSSEC_KEY_STORE_TYPE = "rs.security.keystore.type";
    public static final String RSSEC_KEY_STORE_PSWD = "rs.security.keystore.password";
    public static final String RSSEC_KEY_PSWD = "rs.security.key.password";
    public static final String RSSEC_KEY_STORE_ALIAS = "rs.security.keystore.alias";
    public static final String RSSEC_KEY_STORE_ALIASES = "rs.security.keystore.aliases";
    public static final String RSSEC_KEY_STORE_FILE = "rs.security.keystore.file";
    public static final String RSSEC_PRINCIPAL_NAME = "rs.security.principal.name";
    public static final String RSSEC_KEY_PSWD_PROVIDER = "rs.security.key.password.provider";
    public static final String RSSEC_SIG_KEY_PSWD_PROVIDER = "rs.security.signature.key.password.provider";
    public static final String RSSEC_DECRYPT_KEY_PSWD_PROVIDER = "rs.security.decryption.key.password.provider";
    public static final String RSSEC_DEFAULT_ALGORITHMS = "rs.security.default.algorithms";
    public static final String RSSEC_REPORT_KEY_PROP = "rs.security.report.public.key";
    private static final Logger LOG = LogUtils.getL7dLogger(KeyManagementUtils.class);
    
    private KeyManagementUtils() {
    }
    public static List<String> loadAndEncodeX509CertificateOrChain(Message m, Properties props) {
        X509Certificate[] chain = loadX509CertificateOrChain(m, props);
        return encodeX509CertificateChain(chain);
    }
    public static X509Certificate[] loadX509CertificateOrChain(Message m, Properties props) {
        KeyStore keyStore = KeyManagementUtils.loadPersistKeyStore(m, props);
        String alias = props.getProperty(RSSEC_KEY_STORE_ALIAS);
        return loadX509CertificateOrChain(keyStore, alias);
    }
    private static X509Certificate[] loadX509CertificateOrChain(KeyStore keyStore, String alias) {
        try {
            Certificate[] certs = keyStore.getCertificateChain(alias);
            if (certs != null) {
                return Arrays.copyOf(certs, certs.length, X509Certificate[].class);
            } else {
                return new X509Certificate[]{(X509Certificate)CryptoUtils.loadCertificate(keyStore, alias)};
            }
        } catch (Exception ex) {
            LOG.warning("X509 Certificates can not be created");
            throw new JoseException(ex);
        }    
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
            LOG.warning("Public key can not be loaded");
            throw new JoseException(ex);
        }
    }
    private static String getMessageProperty(Message m, String keyStoreLocPropPreferred, 
                                             String keyStoreLocPropDefault) {
        String propLoc = 
            (String)MessageUtils.getContextualProperty(m, keyStoreLocPropPreferred, keyStoreLocPropDefault);
        if (propLoc == null) {
            LOG.warning("Properties resource is not identified");
            throw new JoseException();
        }
        return propLoc;
    }
    private static PrivateKey loadPrivateKey(KeyStore keyStore, 
                                            Message m,
                                            Properties props, 
                                            Bus bus, 
                                            PrivateKeyPasswordProvider provider,
                                            String keyOper,
                                            String alias) {
        
        String keyPswd = props.getProperty(RSSEC_KEY_PSWD);
        String theAlias = alias != null ? alias : getKeyId(m, props, RSSEC_KEY_STORE_ALIAS, keyOper);
        char[] keyPswdChars = provider != null ? provider.getPassword(props) 
            : keyPswd != null ? keyPswd.toCharArray() : null;    
        return CryptoUtils.loadPrivateKey(keyStore, keyPswdChars, theAlias);
    }
    
    public static PrivateKey loadPrivateKey(Message m, String keyStoreLocProp, String keyOper) {
        return loadPrivateKey(m, keyStoreLocProp, null, keyOper);
    }
    public static PrivateKey loadPrivateKey(Message m, String keyStoreLocPropPreferred,
                                            String keyStoreLocPropDefault, String keyOper) {
        String keyStoreLoc = getMessageProperty(m, keyStoreLocPropPreferred, keyStoreLocPropDefault);
        Bus bus = m.getExchange().getBus();
        try {
            Properties props = ResourceUtils.loadProperties(keyStoreLoc, bus);
            return loadPrivateKey(m, props, keyOper);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public static String getKeyId(Message m, Properties props, String preferredPropertyName, String keyOper) {
        String kid = null;
        String altPropertyName = null;
        if (keyOper != null) {
            if (keyOper.equals(JsonWebKey.KEY_OPER_ENCRYPT) || keyOper.equals(JsonWebKey.KEY_OPER_DECRYPT)) {
                altPropertyName = preferredPropertyName + ".jwe";
            } else if (keyOper.equals(JsonWebKey.KEY_OPER_SIGN) || keyOper.equals(JsonWebKey.KEY_OPER_VERIFY)) {
                altPropertyName = preferredPropertyName + ".jws";
            }
            String direction = m.getExchange().getOutMessage() == m ? ".out" : ".in";
            kid = (String)MessageUtils.getContextualProperty(m, preferredPropertyName, altPropertyName + direction);
            // Check whether the direction is not set for the altPropertyName
            if (kid == null && altPropertyName != null) {
                kid = (String)m.getContextualProperty(altPropertyName);
            }
        }
        
        if (kid == null) {
            kid = props.getProperty(preferredPropertyName);
        }
        if (kid == null && altPropertyName != null) {
            kid = props.getProperty(altPropertyName);
        }
        return kid;
    }
    public static PrivateKeyPasswordProvider loadPasswordProvider(Message m, Properties props, String keyOper) {
        PrivateKeyPasswordProvider cb = 
            (PrivateKeyPasswordProvider)m.getContextualProperty(RSSEC_KEY_PSWD_PROVIDER);
        if (cb == null && keyOper != null) {
            String propName = keyOper.equals(JsonWebKey.KEY_OPER_SIGN) ? RSSEC_SIG_KEY_PSWD_PROVIDER
                : keyOper.equals(JsonWebKey.KEY_OPER_DECRYPT) 
                ? RSSEC_DECRYPT_KEY_PSWD_PROVIDER : null;
            if (propName != null) {
                cb = (PrivateKeyPasswordProvider)m.getContextualProperty(propName);
            }
        }
        return cb;
    }
    
    public static PrivateKey loadPrivateKey(Message m, Properties props, String keyOper) {
        KeyStore keyStore = loadPersistKeyStore(m, props);
        return loadPrivateKey(keyStore, m, props, keyOper, null);
    }
    private static PrivateKey loadPrivateKey(KeyStore keyStore, Message m, Properties props, String keyOper, 
                                                String alias) {
        Bus bus = m.getExchange().getBus();
        PrivateKeyPasswordProvider cb = loadPasswordProvider(m, props, keyOper);
        if (cb != null && m.getExchange().getInMessage() != null) {
            SecurityContext sc = m.getExchange().getInMessage().get(SecurityContext.class);
            if (sc != null) {
                Principal p = sc.getUserPrincipal();
                if (p != null) {
                    props.setProperty(RSSEC_PRINCIPAL_NAME, p.getName());
                }
            }
        }
        return loadPrivateKey(keyStore, m, props, bus, cb, keyOper, alias);
    }
    public static KeyStore loadPersistKeyStore(Message m, Properties props) {
        if (!props.containsKey(RSSEC_KEY_STORE_FILE)) {
            LOG.warning("No keystore file has been configured");
            throw new JoseException("No keystore file has been configured");
        }
        KeyStore keyStore = (KeyStore)m.getExchange().get(props.get(RSSEC_KEY_STORE_FILE));
        if (keyStore == null) {
            keyStore = loadKeyStore(props, m.getExchange().getBus());
            m.getExchange().put((String)props.get(RSSEC_KEY_STORE_FILE), keyStore);
        }
        return keyStore;
    }
    public static KeyStore loadKeyStore(Properties props, Bus bus) {
        String keyStoreType = props.getProperty(RSSEC_KEY_STORE_TYPE);
        String keyStoreLoc = props.getProperty(RSSEC_KEY_STORE_FILE);
        String keyStorePswd = props.getProperty(RSSEC_KEY_STORE_PSWD);
        
        if (keyStorePswd == null) {
            throw new JoseException("No keystore password was defined");
        }
        try {
            InputStream is = ResourceUtils.getResourceStream(keyStoreLoc, bus);
            return CryptoUtils.loadKeyStore(is, keyStorePswd.toCharArray(), keyStoreType);
        } catch (Exception ex) {
            LOG.warning("Key store can not be loaded");
            throw new JoseException(ex);
        }
    }
    public static List<String> encodeX509CertificateChain(X509Certificate[] chain) {
        return encodeX509CertificateChain(Arrays.asList(chain));
    }
    public static List<String> encodeX509CertificateChain(List<X509Certificate> chain) {
        List<String> encodedChain = new ArrayList<String>(chain.size());
        for (X509Certificate cert : chain) {
            try {
                encodedChain.add(CryptoUtils.encodeCertificate(cert));
            } catch (Exception ex) {
                LOG.warning("X509 Certificate can not be encoded");
                throw new JoseException(ex);
            }    
        }
        return encodedChain;
    }
    public static List<X509Certificate> toX509CertificateChain(List<String> base64EncodedChain) {
        if (base64EncodedChain != null) {
            List<X509Certificate> certs = new ArrayList<X509Certificate>(base64EncodedChain.size());
            for (String encodedCert : base64EncodedChain) {
                try {
                    certs.add((X509Certificate)CryptoUtils.decodeCertificate(encodedCert));
                } catch (Exception ex) {
                    LOG.warning("X509 Certificate can not be decoded");
                    throw new JoseException(ex);
                }
            }
            return certs;
        } else {
            return null;
        }
    }
    public static void validateCertificateChain(Properties storeProperties, List<X509Certificate> inCerts) {
        KeyStore ks = loadPersistKeyStore(JAXRSUtils.getCurrentMessage(), storeProperties);
        validateCertificateChain(ks, inCerts);
    }
    public static void validateCertificateChain(KeyStore ks, List<X509Certificate> inCerts) {
        // Initial chain validation, to be enhanced as needed
        try {
            X509CertSelector certSelect = new X509CertSelector();
            certSelect.setCertificate((X509Certificate) inCerts.get(0));
            PKIXBuilderParameters pbParams = new PKIXBuilderParameters(ks, certSelect);
            pbParams.addCertStore(CertStore.getInstance("Collection", 
                                                        new CollectionCertStoreParameters(inCerts)));
            pbParams.setMaxPathLength(-1);
            pbParams.setRevocationEnabled(false);
            CertPathBuilderResult buildResult = CertPathBuilder.getInstance("PKIX").build(pbParams);               
            CertPath certPath = buildResult.getCertPath();
            CertPathValidator.getInstance("PKIX").validate(certPath, pbParams);
        } catch (Exception ex) {
            LOG.warning("Certificate path validation error");
            throw new JoseException(ex);
        }
    }
    public static X509Certificate[] toX509CertificateChainArray(List<String> base64EncodedChain) {
        List<X509Certificate> chain = toX509CertificateChain(base64EncodedChain);
        return chain == null ? null : chain.toArray(new X509Certificate[]{});
    }
    public static String getKeyAlgorithm(Message m, Properties props, String propName, String defaultAlg) {
        String algo = props.getProperty(propName);
        if (algo == null) {
            algo = (String)m.getContextualProperty(propName);
        }
        if (algo == null && PropertyUtils.isTrue(m.getContextualProperty(RSSEC_DEFAULT_ALGORITHMS))) {
            algo = defaultAlg;
        }
        return algo;
    }

    public static Properties loadStoreProperties(Message m, boolean required, 
                                                 String storeProp1, String storeProp2) {
        if (m == null) {
            if (required) {
                throw new JoseException();
            }
            return null;
        }
        Properties props = null;
        String propLoc = 
            (String)MessageUtils.getContextualProperty(m, storeProp1, storeProp2);
        if (propLoc != null) {
            try {
                props = ResourceUtils.loadProperties(propLoc, m.getExchange().getBus());
            } catch (Exception ex) {
                LOG.warning("Properties resource is not identified");
                throw new JoseException(ex);
            }
        } else {
            String keyFile = (String)m.getContextualProperty(RSSEC_KEY_STORE_FILE);
            if (keyFile != null) {
                props = new Properties();
                props.setProperty(RSSEC_KEY_STORE_FILE, keyFile);
                String type = (String)m.getContextualProperty(RSSEC_KEY_STORE_TYPE);
                if (type == null) {
                    type = "jwk";
                }
                props.setProperty(RSSEC_KEY_STORE_TYPE, type);
                String alias = (String)m.getContextualProperty(RSSEC_KEY_STORE_ALIAS);
                if (alias != null) {
                    props.setProperty(RSSEC_KEY_STORE_ALIAS, alias);
                }
                String keystorePassword = (String)m.getContextualProperty(RSSEC_KEY_STORE_PSWD);
                if (keystorePassword != null) {
                    props.setProperty(RSSEC_KEY_STORE_PSWD, keystorePassword);
                }
                String keyPassword = (String)m.getContextualProperty(RSSEC_KEY_PSWD);
                if (keyPassword != null) {
                    props.setProperty(RSSEC_KEY_PSWD, keyPassword);
                }
            }
        }
        if (props == null && required) { 
            LOG.warning("Properties resource is not identified");
            throw new JoseException();
        }
        return props; 
    }
    public static PrivateKey loadPrivateKey(Message m, Properties props, 
                                               List<X509Certificate> inCerts, String keyOper) {
        KeyStore ks = loadPersistKeyStore(m, props);
        
        try {
            String alias = ks.getCertificateAlias(inCerts.get(0));
            if (alias != null) {
                for (Enumeration<String> e = ks.aliases(); e.hasMoreElements();) {
                    String currentAlias = e.nextElement();
                    X509Certificate[] currentCertArray = loadX509CertificateOrChain(ks, currentAlias);
                    if (currentCertArray != null) {
                        alias = currentAlias;
                        break;
                    }
                }
            }
            return loadPrivateKey(ks, m, props, keyOper, alias);
            
        } catch (Exception ex) {
            LOG.warning("Private key can not be loaded");
            throw new JoseException(ex);
        }
    }
}
