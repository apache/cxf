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

package org.apache.cxf.xkms.crypto;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.xkms.cache.EHCacheXKMSClientCache;
import org.apache.cxf.xkms.cache.XKMSCacheToken;
import org.apache.cxf.xkms.cache.XKMSClientCache;
import org.apache.cxf.xkms.client.XKMSInvoker;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoBase;
import org.apache.ws.security.components.crypto.CryptoType;
import org.apache.ws.security.components.crypto.CryptoType.TYPE;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

public class XkmsCryptoProvider extends CryptoBase {

    private static final Logger LOG = LogUtils.getL7dLogger(XkmsCryptoProvider.class);

    private final XKMSInvoker xkmsInvoker;
    private Crypto defaultCrypto;
    private XKMSClientCache xkmsClientCache;

    public XkmsCryptoProvider(XKMSPortType xkmsConsumer) {
        this(xkmsConsumer, null);
    }

    public XkmsCryptoProvider(XKMSPortType xkmsConsumer, Crypto defaultCrypto) {
        this(xkmsConsumer, defaultCrypto, new EHCacheXKMSClientCache());
    }
    
    public XkmsCryptoProvider(XKMSPortType xkmsConsumer, Crypto defaultCrypto, XKMSClientCache xkmsClientCache) {
        if (xkmsConsumer == null) {
            throw new IllegalArgumentException("xkmsConsumer may not be null");
        }
        this.xkmsInvoker = new XKMSInvoker(xkmsConsumer);
        this.defaultCrypto = defaultCrypto;
        this.xkmsClientCache = xkmsClientCache;
    }
    
    public XkmsCryptoProvider(XKMSInvoker xkmsInvoker) {
        this(xkmsInvoker, null);
    }
    
    public XkmsCryptoProvider(XKMSInvoker xkmsInvoker, Crypto defaultCrypto) {
        this(xkmsInvoker, defaultCrypto, new EHCacheXKMSClientCache());
    }
    
    public XkmsCryptoProvider(XKMSInvoker xkmsInvoker, Crypto defaultCrypto, XKMSClientCache xkmsClientCache) {
        if (xkmsInvoker == null) {
            throw new IllegalArgumentException("xkmsInvoker may not be null");
        }
        this.xkmsInvoker = xkmsInvoker;
        this.defaultCrypto = defaultCrypto;
        this.xkmsClientCache = xkmsClientCache;
    }
    
    @Override
    public X509Certificate[] getX509Certificates(CryptoType cryptoType) throws WSSecurityException {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info(String
                .format("XKMS Runtime: getting public certificate for alias: %s; issuer: %s; subjectDN: %s",
                        cryptoType.getAlias(), cryptoType.getIssuer(), cryptoType.getSubjectDN()));
        }
        X509Certificate[] certs = getX509CertificatesInternal(cryptoType);
        if (certs == null) {
            LOG.severe(String
                .format(
                        "Cannot find certificate for alias: %s, issuer: %s; subjectDN: %s",
                        cryptoType.getAlias(), cryptoType.getIssuer(), cryptoType.getSubjectDN()));
        }
        return certs;
    }

    @Override
    public String getX509Identifier(X509Certificate cert) throws WSSecurityException {
        assertDefaultCryptoProvider();
        return defaultCrypto.getX509Identifier(cert);
    }

    @Override
    public PrivateKey getPrivateKey(X509Certificate certificate, CallbackHandler callbackHandler)
        throws WSSecurityException {
        assertDefaultCryptoProvider();
        return defaultCrypto.getPrivateKey(certificate, callbackHandler);
    }

    @Override
    public PrivateKey getPrivateKey(String identifier, String password) throws WSSecurityException {
        assertDefaultCryptoProvider();
        return defaultCrypto.getPrivateKey(identifier, password);
    }

    @Override
    public boolean verifyTrust(X509Certificate[] certs) {
        return verifyTrust(certs, false);
    }
    
    @Override
    public boolean verifyTrust(X509Certificate[] certs, boolean enableRevocation) {
        if (certs != null) {
            LOG.fine(String.format("Verifying certificate id: %s", certs[0].getSubjectDN()));
        }
        
        XKMSCacheToken cachedToken = null;
        // Try local cache first
        if (certs != null && certs.length > 0 && xkmsClientCache != null) {
            String key = certs[0].getSubjectX500Principal().getName();
            // Try by Subject DN and IssuerSerial
            cachedToken = xkmsClientCache.get(key);
            if (cachedToken == null) {
                key = getKeyForIssuerSerial(certs[0].getIssuerX500Principal().getName(),
                                            certs[0].getSerialNumber());
                cachedToken = xkmsClientCache.get(key);
            }
            if (cachedToken != null && cachedToken.isXkmsValidated()) {
                LOG.fine("Certificate has already been validated by the XKMS service");
                return true;
            }
        }
        if (certs == null || certs[0] == null || !xkmsInvoker.validateCertificate(certs[0])) {
            return false;
        }
        
        // Validate Cached token
        if (cachedToken != null) {
            cachedToken.setXkmsValidated(true);
        }
        
        // Otherwise, Store in the cache as a validated certificate
        storeCertificateInCache(certs[0], null, true);

        return true;
    }

    @Override
    public boolean verifyTrust(PublicKey publicKey) throws WSSecurityException {
        throw new CryptoProviderException("PublicKeys cannot be verified");
    }

    private void assertDefaultCryptoProvider() {
        if (defaultCrypto == null) {
            throw new UnsupportedOperationException("Not supported by this crypto provider");
        }
    }

    private X509Certificate[] getX509CertificatesInternal(CryptoType cryptoType) {
        CryptoType.TYPE type = cryptoType.getType();
        if (type == TYPE.SUBJECT_DN) {
            return getX509CertificatesFromXKMS(Applications.PKIX, cryptoType.getSubjectDN());
        } else if (type == TYPE.ALIAS) {
            return getX509CertificatesFromXKMS(cryptoType);
        } else if (type == TYPE.ISSUER_SERIAL) {
            String key = getKeyForIssuerSerial(cryptoType.getIssuer(), cryptoType.getSerial());
            // Try local cache first
            if (xkmsClientCache != null) {
                XKMSCacheToken cachedToken = xkmsClientCache.get(key);
                if (cachedToken != null && cachedToken.getX509Certificate() != null) {
                    return new X509Certificate[] {cachedToken.getX509Certificate()};
                }
            }
            // Now ask the XKMS Service
            X509Certificate certificate = xkmsInvoker.getCertificateForIssuerSerial(cryptoType
                .getIssuer(), cryptoType.getSerial());
            
            // Store in the cache
            storeCertificateInCache(certificate, key, false);

            return new X509Certificate[] {
                certificate
            };
        }
        throw new IllegalArgumentException("Unsupported type " + type);
    }

    private X509Certificate[] getX509CertificatesFromXKMS(CryptoType cryptoType) {
        Applications appId = null;
        boolean isServiceName = isServiceName(cryptoType);
        if (!isServiceName) {
            X509Certificate[] localCerts = getCertificateLocally(cryptoType);
            if (localCerts != null) {
                return localCerts;
            }
            appId = Applications.PKIX;
        } else {
            appId = Applications.SERVICE_SOAP;
        }
        return getX509CertificatesFromXKMS(appId, cryptoType.getAlias());
    }

    private X509Certificate[] getX509CertificatesFromXKMS(Applications application, String id) {
        LOG.fine(String.format("Getting public certificate from XKMS for application:%s; id: %s",
                               application, id));
        if (id == null) {
            throw new CryptoProviderException("Id is not specified for certificate request");
        }
        
        // Try local cache first
        if (xkmsClientCache != null) {
            XKMSCacheToken cachedToken = xkmsClientCache.get(id.toLowerCase());
            if (cachedToken != null && cachedToken.getX509Certificate() != null) {
                return new X509Certificate[] {cachedToken.getX509Certificate()};
            }
        }
        
        // Now ask the XKMS Service
        X509Certificate cert = xkmsInvoker.getCertificateForId(application, id);
        
        // Store in the cache
        storeCertificateInCache(cert, id.toLowerCase(), false);

        return new X509Certificate[] {
            cert
        };
    }

    /**
     * Try to get certificate locally
     * 
     * @param cryptoType
     * @return if found certificate otherwise null returned
     */
    private X509Certificate[] getCertificateLocally(CryptoType cryptoType) {
        X509Certificate[] localCerts = null;
        try {
            localCerts = defaultCrypto.getX509Certificates(cryptoType);
        } catch (Exception e) {
            LOG.info("Certificate is not found in local keystore and will be requested from "
                + "XKMS (first trying the cache): " + cryptoType.getAlias());
        }
        return localCerts;
    }

    /**
     * Service Aliases contain namespace
     * 
     * @param cryptoType
     * @return
     */
    private boolean isServiceName(CryptoType cryptoType) {
        return cryptoType.getAlias().contains("{");
    }
    
    private String getKeyForIssuerSerial(String issuer, BigInteger serial) {
        return issuer + "-" + serial.toString(16);
    }
    
    private void storeCertificateInCache(X509Certificate certificate, String key, boolean validated) {
        // Store in the cache
        if (certificate != null && xkmsClientCache != null) {
            XKMSCacheToken cacheToken = new XKMSCacheToken(certificate);
            cacheToken.setXkmsValidated(validated);
            // Store using a custom key (if any)
            if (key != null) {
                xkmsClientCache.put(key, cacheToken);
            }
            // Store it using IssuerSerial as well
            String issuerSerialKey = 
                getKeyForIssuerSerial(certificate.getIssuerX500Principal().getName(), 
                                      certificate.getSerialNumber());
            if (!issuerSerialKey.equals(key)) {
                xkmsClientCache.put(issuerSerialKey, cacheToken);
            }
            // Store it using the Subject DN as well
            String subjectDNKey = certificate.getSubjectX500Principal().getName();
            if (!subjectDNKey.equals(key)) {
                xkmsClientCache.put(subjectDNKey, cacheToken);
            }
        }
    }

}
