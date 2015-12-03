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

package org.apache.cxf.rs.security.jose.common;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.jose.jwk.KeyOperation;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.crypto.MessageDigestUtils;

/**
 * Encryption helpers
 */
public final class KeyManagementUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(KeyManagementUtils.class);
    
    private KeyManagementUtils() {
    }
    
    public static List<String> loadAndEncodeX509CertificateOrChain(Message m, Properties props) {
        X509Certificate[] chain = loadX509CertificateOrChain(m, props);
        return encodeX509CertificateChain(chain);
    }
    
    public static String loadDigestAndEncodeX509Certificate(Message m, Properties props) {
        X509Certificate[] certs = loadX509CertificateOrChain(m, props);
        if (certs != null && certs.length > 0) {
            try {
                byte[] digest = 
                    MessageDigestUtils.createDigest(certs[0].getEncoded(), 
                                                MessageDigestUtils.ALGO_SHA_1);
                return Base64UrlUtility.encode(digest);
            } catch (NoSuchAlgorithmException ex) {
                LOG.log(Level.FINE, "Error creating digest", ex);
                throw new JoseException(ex);
            } catch (CertificateEncodingException ex) {
                LOG.log(Level.FINE, "Error creating digest", ex);
                throw new JoseException(ex);
            }
        }
        
        return null;
    }
    
    public static X509Certificate[] loadX509CertificateOrChain(Message m, Properties props) {
        KeyStore keyStore = KeyManagementUtils.loadPersistKeyStore(m, props);
        String alias = props.getProperty(JoseConstants.RSSEC_KEY_STORE_ALIAS);
        return loadX509CertificateOrChain(keyStore, alias);
    }
    private static X509Certificate[] loadX509CertificateOrChain(KeyStore keyStore, String alias) {
        if (alias == null) {
            throw new JoseException("No alias supplied");
        }
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
        return CryptoUtils.loadPublicKey(keyStore, props.getProperty(JoseConstants.RSSEC_KEY_STORE_ALIAS));
    }
    public static PublicKey loadPublicKey(Message m, String keyStoreLocProp) {
        return loadPublicKey(m, keyStoreLocProp, null);
    }
    public static PublicKey loadPublicKey(Message m, String keyStoreLocPropPreferred, String keyStoreLocPropDefault) {
        String keyStoreLoc = getMessageProperty(m, keyStoreLocPropPreferred, keyStoreLocPropDefault);
        Bus bus = m.getExchange().getBus();
        try {
            Properties props = JoseUtils.loadProperties(keyStoreLoc, bus);
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
                                            KeyOperation keyOper,
                                            String alias) {
        
        String keyPswd = props.getProperty(JoseConstants.RSSEC_KEY_PSWD);
        String theAlias = alias != null ? alias : getKeyId(m, props, JoseConstants.RSSEC_KEY_STORE_ALIAS, keyOper);
        if (theAlias != null) {
            props.put(JoseConstants.RSSEC_KEY_STORE_ALIAS, theAlias);
        }
        char[] keyPswdChars = keyPswd != null ? keyPswd.toCharArray() : null;
        if (keyPswdChars == null) {
            PrivateKeyPasswordProvider provider = loadPasswordProvider(m, props, keyOper);
            keyPswdChars = provider != null ? provider.getPassword(props) : null;
        }
        return CryptoUtils.loadPrivateKey(keyStore, keyPswdChars, theAlias);
    }
    
    public static PrivateKey loadPrivateKey(Message m, String keyStoreLocProp, KeyOperation keyOper) {
        return loadPrivateKey(m, keyStoreLocProp, null, keyOper);
    }
    public static PrivateKey loadPrivateKey(Message m, String keyStoreLocPropPreferred,
                                            String keyStoreLocPropDefault, KeyOperation keyOper) {
        String keyStoreLoc = getMessageProperty(m, keyStoreLocPropPreferred, keyStoreLocPropDefault);
        Bus bus = m.getExchange().getBus();
        try {
            Properties props = JoseUtils.loadProperties(keyStoreLoc, bus);
            return loadPrivateKey(m, props, keyOper);
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    
    public static String getKeyId(Message m, Properties props, 
                                  String preferredPropertyName, 
                                  KeyOperation keyOper) {
        String kid = null;
        String altPropertyName = null;
        if (keyOper != null && m != null) {
            if (keyOper == KeyOperation.ENCRYPT || keyOper == KeyOperation.DECRYPT) {
                altPropertyName = preferredPropertyName + ".jwe";
            } else if (keyOper == KeyOperation.SIGN || keyOper == KeyOperation.VERIFY) {
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
    public static PrivateKeyPasswordProvider loadPasswordProvider(Message m, Properties props, KeyOperation keyOper) {
        PrivateKeyPasswordProvider cb = null;
        if (keyOper != null) {
            String propName = keyOper == KeyOperation.SIGN ? JoseConstants.RSSEC_SIGNATURE_KEY_PSWD_PROVIDER
                : keyOper == KeyOperation.DECRYPT 
                ? JoseConstants.RSSEC_DECRYPTION_KEY_PSWD_PROVIDER : null;
            if (propName != null) {
                if (props.containsKey(propName)) {
                    cb = (PrivateKeyPasswordProvider)props.get(propName);
                } else if (m != null) {
                    cb = (PrivateKeyPasswordProvider)m.getContextualProperty(propName);
                }
            }
        }
        if (cb == null) {
            if (props.containsKey(JoseConstants.RSSEC_KEY_PSWD_PROVIDER)) {
                cb = (PrivateKeyPasswordProvider)props.get(JoseConstants.RSSEC_KEY_PSWD_PROVIDER);
            } else if (m != null) {
                cb = (PrivateKeyPasswordProvider)m.getContextualProperty(JoseConstants.RSSEC_KEY_PSWD_PROVIDER);
            }
        }
        return cb;
    }
    
    public static PrivateKey loadPrivateKey(Message m, Properties props, KeyOperation keyOper) {
        KeyStore keyStore = loadPersistKeyStore(m, props);
        return loadPrivateKey(keyStore, m, props, keyOper, null);
    }
    public static KeyStore loadPersistKeyStore(Message m, Properties props) {
        KeyStore keyStore = null;
        if (props.containsKey(JoseConstants.RSSEC_KEY_STORE)) {
            keyStore = (KeyStore)props.get(JoseConstants.RSSEC_KEY_STORE);
        }
        
        if (keyStore == null) {
            if (!props.containsKey(JoseConstants.RSSEC_KEY_STORE_FILE)) {
                LOG.warning("No keystore file has been configured");
                throw new JoseException("No keystore file has been configured");
            }
            if (m != null) {
                keyStore = (KeyStore)m.getExchange().get(props.get(JoseConstants.RSSEC_KEY_STORE_FILE));
            }
        }
        
        if (keyStore == null) {
            Bus bus = m != null ? m.getExchange().getBus() : null;
            keyStore = loadKeyStore(props, bus);
            if (m != null) {
                m.getExchange().put((String)props.get(JoseConstants.RSSEC_KEY_STORE_FILE), keyStore);
            }
        }
        return keyStore;
    }
    public static KeyStore loadKeyStore(Properties props, Bus bus) {
        String keyStoreType = props.getProperty(JoseConstants.RSSEC_KEY_STORE_TYPE);
        String keyStoreLoc = props.getProperty(JoseConstants.RSSEC_KEY_STORE_FILE);
        String keyStorePswd = props.getProperty(JoseConstants.RSSEC_KEY_STORE_PSWD);
        
        if (keyStorePswd == null) {
            throw new JoseException("No keystore password was defined");
        }
        try {
            InputStream is = JoseUtils.getResourceStream(keyStoreLoc, bus);
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
    //TODO: enhance the certificate validation code
    public static void validateCertificateChain(Properties storeProperties, List<X509Certificate> inCerts) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        KeyStore ks = loadPersistKeyStore(message, storeProperties);
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
        String algo = props != null ? props.getProperty(propName) : null;
        if (algo == null && m != null) {
            algo = (String)m.getContextualProperty(propName);
        }
        if (algo == null) {
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
                props = JoseUtils.loadProperties(propLoc, m.getExchange().getBus());
            } catch (Exception ex) {
                LOG.warning("Properties resource is not identified");
                throw new JoseException(ex);
            }
        } else {
            String keyFile = (String)m.getContextualProperty(JoseConstants.RSSEC_KEY_STORE_FILE);
            if (keyFile != null) {
                props = new Properties();
                props.setProperty(JoseConstants.RSSEC_KEY_STORE_FILE, keyFile);
                String type = (String)m.getContextualProperty(JoseConstants.RSSEC_KEY_STORE_TYPE);
                if (type == null) {
                    type = "jwk";
                }
                props.setProperty(JoseConstants.RSSEC_KEY_STORE_TYPE, type);
                String alias = (String)m.getContextualProperty(JoseConstants.RSSEC_KEY_STORE_ALIAS);
                if (alias != null) {
                    props.setProperty(JoseConstants.RSSEC_KEY_STORE_ALIAS, alias);
                }
                String keystorePassword = (String)m.getContextualProperty(JoseConstants.RSSEC_KEY_STORE_PSWD);
                if (keystorePassword != null) {
                    props.setProperty(JoseConstants.RSSEC_KEY_STORE_PSWD, keystorePassword);
                }
                String keyPassword = (String)m.getContextualProperty(JoseConstants.RSSEC_KEY_PSWD);
                if (keyPassword != null) {
                    props.setProperty(JoseConstants.RSSEC_KEY_PSWD, keyPassword);
                }
            }
        }
        if (props == null) {
            if (required) {
                LOG.warning("Properties resource is not identified");
                throw new JoseException();
            }
            props = new Properties();
        }
        return props; 
    }
    public static PrivateKey loadPrivateKey(Message m, Properties props, 
                                            X509Certificate inCert, 
                                            KeyOperation keyOper) {
        KeyStore ks = loadPersistKeyStore(m, props);
        
        try {
            String alias = ks.getCertificateAlias(inCert);
            return loadPrivateKey(ks, m, props, keyOper, alias);
            
        } catch (Exception ex) {
            LOG.warning("Private key can not be loaded");
            throw new JoseException(ex);
        }
    }
    
    public static X509Certificate getCertificateFromThumbprint(String thumbprint,
                                                               String digestAlgorithm,
                                                               Message m, 
                                                               Properties props) {
        KeyStore ks = loadPersistKeyStore(m, props);
        if (ks == null || thumbprint == null) {
            return null;
        }
        
        try {
            byte[] decodedThumbprint = Base64UrlUtility.decode(thumbprint);
            
            for (Enumeration<String> e = ks.aliases(); e.hasMoreElements();) {
                String alias = e.nextElement();
                Certificate[] certs = ks.getCertificateChain(alias);
                if (certs == null || certs.length == 0) {
                    // no cert chain, so lets check if getCertificate gives us a result.
                    Certificate cert = ks.getCertificate(alias);
                    if (cert != null) {
                        certs = new Certificate[]{cert};
                    }
                }
                
                if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate) {
                    X509Certificate x509cert = (X509Certificate) certs[0];
                    byte[] data = 
                        MessageDigestUtils.createDigest(x509cert.getEncoded(), digestAlgorithm);

                    if (Arrays.equals(data, decodedThumbprint)) {
                        return x509cert;
                    }
                }
            }
        } catch (KeyStoreException e) {
            LOG.log(Level.WARNING, "X509Certificate can not be loaded: ", e);
            throw new JoseException(e);
        } catch (CertificateEncodingException e) {
            LOG.log(Level.WARNING, "X509Certificate can not be loaded: ", e);
            throw new JoseException(e);
        } catch (NoSuchAlgorithmException e) {
            LOG.log(Level.WARNING, "X509Certificate can not be loaded: ", e);
            throw new JoseException(e);
        } catch (Base64Exception e) {
            LOG.log(Level.WARNING, "X509Certificate can not be loaded: ", e);
            throw new JoseException(e);
        }
        
        return null;
    }
}
