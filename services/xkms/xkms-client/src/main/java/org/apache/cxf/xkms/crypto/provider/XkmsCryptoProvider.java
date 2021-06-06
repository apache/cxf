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

package org.apache.cxf.xkms.crypto.provider;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.xkms.cache.EHCacheXKMSClientCache;
import org.apache.cxf.xkms.cache.XKMSCacheToken;
import org.apache.cxf.xkms.cache.XKMSClientCache;
import org.apache.cxf.xkms.cache.XKMSClientCacheException;
import org.apache.cxf.xkms.client.XKMSInvoker;
import org.apache.cxf.xkms.crypto.CryptoProviderException;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoBase;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.crypto.CryptoType.TYPE;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

public class XkmsCryptoProvider extends CryptoBase {

    private static final Logger LOG = LogUtils.getL7dLogger(XkmsCryptoProvider.class);

    private final XKMSInvoker xkmsInvoker;
    private Crypto fallbackCrypto;
    private XKMSClientCache xkmsClientCache;
    private boolean allowX509FromJKS = true;

    public XkmsCryptoProvider(XKMSPortType xkmsConsumer) throws XKMSClientCacheException {
        this(xkmsConsumer, null);
    }

    public XkmsCryptoProvider(XKMSPortType xkmsConsumer, Crypto fallbackCrypto) throws XKMSClientCacheException {
        this(xkmsConsumer, fallbackCrypto, new EHCacheXKMSClientCache(), true);
    }

    public XkmsCryptoProvider(XKMSPortType xkmsConsumer, Crypto fallbackCrypto, boolean allowX509FromJKS)
            throws XKMSClientCacheException {
        this(xkmsConsumer, fallbackCrypto, new EHCacheXKMSClientCache(), allowX509FromJKS);
    }

    public XkmsCryptoProvider(XKMSPortType xkmsConsumer, Crypto fallbackCrypto,
                              XKMSClientCache xkmsClientCache, boolean allowX509FromJKS) {
        if (xkmsConsumer == null) {
            throw new IllegalArgumentException("xkmsConsumer may not be null");
        }
        this.xkmsInvoker = new XKMSInvoker(xkmsConsumer);
        this.fallbackCrypto = fallbackCrypto;
        this.xkmsClientCache = xkmsClientCache;
        this.allowX509FromJKS = allowX509FromJKS;
    }

    @Override
    public X509Certificate[] getX509Certificates(CryptoType cryptoType) throws WSSecurityException {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info(String
                .format("XKMS Runtime: getting public certificate for alias: %s; issuer: %s; subjectDN: %s",
                        cryptoType.getAlias(), cryptoType.getIssuer(), cryptoType.getSubjectDN()));
        }
        X509Certificate[] certs = getX509(cryptoType);
        if (certs == null) {
            LOG.warning(String
                .format(
                        "Cannot find certificate for alias: %s, issuer: %s; subjectDN: %s",
                        cryptoType.getAlias(), cryptoType.getIssuer(), cryptoType.getSubjectDN()));
        }
        return certs;
    }

    @Override
    public String getX509Identifier(X509Certificate cert) throws WSSecurityException {
        assertDefaultCryptoProvider();
        return fallbackCrypto.getX509Identifier(cert);
    }

    @Override
    public PrivateKey getPrivateKey(X509Certificate certificate, CallbackHandler callbackHandler)
        throws WSSecurityException {
        assertDefaultCryptoProvider();
        return fallbackCrypto.getPrivateKey(certificate, callbackHandler);
    }

    @Override
    public PrivateKey getPrivateKey(PublicKey publicKey, CallbackHandler callbackHandler)
        throws WSSecurityException {
        assertDefaultCryptoProvider();
        return fallbackCrypto.getPrivateKey(publicKey, callbackHandler);
    }

    @Override
    public PrivateKey getPrivateKey(String identifier, String password) throws WSSecurityException {
        assertDefaultCryptoProvider();
        return fallbackCrypto.getPrivateKey(identifier, password);
    }

    @Override
    public void verifyTrust(
        X509Certificate[] certs,
        boolean enableRevocation,
        Collection<Pattern> subjectCertConstraints,
        Collection<Pattern> issuerCertConstraints
    ) throws WSSecurityException {
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
                return;
            }
        }
        if (certs == null || certs[0] == null || !xkmsInvoker.validateCertificate(certs[0])) {
            throw new CryptoProviderException("The given certificate is not valid");
        }

        // Validate Cached token
        if (cachedToken != null) {
            cachedToken.setXkmsValidated(true);
        }

        // Otherwise, Store in the cache as a validated certificate
        storeCertificateInCache(certs[0], null, true);
    }

    @Override
    public void verifyTrust(PublicKey publicKey) throws WSSecurityException {
        throw new CryptoProviderException("PublicKeys cannot be verified");
    }

    private void assertDefaultCryptoProvider() {
        if (fallbackCrypto == null) {
            throw new UnsupportedOperationException("Not supported by this crypto provider");
        }
    }

    private X509Certificate[] getX509(CryptoType cryptoType) {
        // Try to get X509 certificate from local keystore if it is configured
        if (allowX509FromJKS && fallbackCrypto != null) {
            X509Certificate[] localCerts = getCertificateLocaly(cryptoType);
            if (localCerts != null && localCerts.length > 0) {
                return localCerts;
            }
        }
        CryptoType.TYPE type = cryptoType.getType();
        if (type == TYPE.SUBJECT_DN) {
            return getX509FromXKMSByID(Applications.PKIX, cryptoType.getSubjectDN());
        } else if (type == TYPE.ENDPOINT) {
            return getX509FromXKMSByEndpoint(cryptoType.getEndpoint());
        } else if (type == TYPE.ALIAS) {
            final Applications appId;
            if (!isServiceName(cryptoType)) {
                appId = Applications.PKIX;
            } else {
                appId = Applications.SERVICE_NAME;
            }
            return getX509FromXKMSByID(appId, cryptoType.getAlias());

        } else if (type == TYPE.ISSUER_SERIAL) {
            return getX509FromXKMSByIssuerSerial(cryptoType.getIssuer(), cryptoType.getSerial());
        }
        throw new IllegalArgumentException("Unsupported type " + type);
    }

    private X509Certificate[] getX509FromXKMSByID(Applications application, String id) {
        LOG.fine(String.format("Getting public certificate from XKMS for application:%s; id: %s",
                               application, id));
        if (id == null) {
            throw new IllegalArgumentException("Id is not specified for certificate request");
        }

        // Try local cache first
        X509Certificate[] certs = checkX509Cache(id.toLowerCase());
        if (certs != null) {
            return certs;
        }

        // Now ask the XKMS Service
        X509Certificate cert = xkmsInvoker.getCertificateForId(application, id);

        return buildX509GetResult(id.toLowerCase(), cert);
    }

    private X509Certificate[] getX509FromXKMSByIssuerSerial(String issuer, BigInteger serial) {
        LOG.fine(String.format("Getting public certificate from XKMS for issuer:%s; serial: %x",
                               issuer, serial));

        String key = getKeyForIssuerSerial(issuer, serial);
        // Try local cache first
        X509Certificate[] certs = checkX509Cache(key);
        if (certs != null) {
            return certs;
        }

        // Now ask the XKMS Service
        X509Certificate cert = xkmsInvoker.getCertificateForIssuerSerial(issuer, serial);

        return buildX509GetResult(key, cert);
    }

    private X509Certificate[] getX509FromXKMSByEndpoint(String endpoint) {
        LOG.fine(String.format("Getting public certificate from XKMS for endpoint:%s",
                               endpoint));

        // Try local cache first
        X509Certificate[] certs = checkX509Cache(endpoint);
        if (certs != null) {
            return certs;
        }

        // Now ask the XKMS Service
        X509Certificate cert = xkmsInvoker.getCertificateForEndpoint(endpoint);

        return buildX509GetResult(endpoint, cert);
    }

    private X509Certificate[] checkX509Cache(String key) {
        if (xkmsClientCache == null) {
            return null;
        }

        XKMSCacheToken cachedToken = xkmsClientCache.get(key);
        if (cachedToken != null && cachedToken.getX509Certificate() != null) {
            return new X509Certificate[] {
                cachedToken.getX509Certificate()
            };
        }
        return null;
    }

    private X509Certificate[] buildX509GetResult(String key, X509Certificate cert) {
        if (cert != null) {
            // Certificate was found: store in the cache
            storeCertificateInCache(cert, key, false);

            return new X509Certificate[] {
                cert
            };
        }
        // Certificate was not found: return empty list
        return new X509Certificate[0];
    }

    /**
     * Try to get certificate locally. First try using the supplied CryptoType. If this
     * does not work, and if the supplied CryptoType is a ALIAS, then try again with SUBJECT_DN
     * in case the supplied Alias is actually a Certificate's Subject DN
     *
     * @param cryptoType
     * @return if found certificate otherwise null returned
     */
    private X509Certificate[] getCertificateLocaly(CryptoType cryptoType) {
        // This only applies if we've configured a local Crypto instance...
        if (fallbackCrypto == null) {
            return null;
        }

        // First try using the supplied CryptoType instance
        X509Certificate[] localCerts = null;
        try {
            localCerts = fallbackCrypto.getX509Certificates(cryptoType);
        } catch (Exception e) {
            LOG.info("Certificate is not found in local keystore using desired CryptoType: "
                     + cryptoType.getType().name());
        }

        if (localCerts == null && cryptoType.getType() == CryptoType.TYPE.ALIAS) {
            // If none found then try using either the Subject DN. This is because an
            // Encryption username in CXF is configured as an Alias in WSS4J, but may in fact
            // be a Subject DN
            CryptoType newCryptoType = new CryptoType(CryptoType.TYPE.SUBJECT_DN);
            newCryptoType.setSubjectDN(cryptoType.getAlias());

            try {
                localCerts = fallbackCrypto.getX509Certificates(newCryptoType);
            } catch (Exception e) {
                LOG.info("Certificate is not found in local keystore and will be requested from "
                    + "XKMS (first trying the cache): " + cryptoType.getAlias());
            }
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
