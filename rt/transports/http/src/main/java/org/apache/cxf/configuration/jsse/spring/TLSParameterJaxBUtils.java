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
package org.apache.cxf.configuration.jsse.spring;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.security.CertStoreType;
import org.apache.cxf.configuration.security.KeyManagersType;
import org.apache.cxf.configuration.security.KeyStoreType;
import org.apache.cxf.configuration.security.SecureRandomParameters;
import org.apache.cxf.configuration.security.TrustManagersType;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.transport.https.SSLUtils;

/**
 * This class provides some functionality to convert the JAXB
 * generated types in the security.xsd to the items needed
 * to programatically configure the HTTPConduit and HTTPDestination
 * with TLSClientParameters and TLSServerParameters respectively.
 */
public final class TLSParameterJaxBUtils {

    private static final Logger LOG =
        LogUtils.getL7dLogger(TLSParameterJaxBUtils.class);

    private TLSParameterJaxBUtils() {
        // empty
    }
    /**
     * This method converts the JAXB generated type into a SecureRandom.
     */
    public static SecureRandom getSecureRandom(
            SecureRandomParameters secureRandomParams
    ) throws GeneralSecurityException {

        SecureRandom secureRandom = null;
        if (secureRandomParams != null) {
            String secureRandomAlg =
                secureRandomParams.getAlgorithm();
            String randomProvider =
                secureRandomParams.getProvider();
            if (randomProvider != null) {
                secureRandom = secureRandomAlg != null
                               ? SecureRandom.getInstance(
                                       secureRandomAlg,
                                       randomProvider)
                               : null;
            } else {
                secureRandom = secureRandomAlg != null
                               ? SecureRandom.getInstance(
                                       secureRandomAlg)
                               : null;
            }
        }
        return secureRandom;
    }
    /**
     * This method converts a JAXB generated KeyStoreType into a KeyStore.
     */
    public static KeyStore getKeyStore(KeyStoreType kst)
        throws GeneralSecurityException,
               IOException {

        if (kst == null) {
            return null;
        }
        String type = SSLUtils.getKeystoreType(kst.isSetType()
                                 ? kst.getType() : null, LOG, KeyStore.getDefaultType());

        char[] password = kst.isSetPassword()
                    ? deobfuscate(kst.getPassword())
                    : null;
        if (password == null) {
            String tmp = SSLUtils.getKeystorePassword(null, LOG);
            if (tmp != null) {
                password = tmp.toCharArray();
            }
        }
        String provider = SSLUtils.getKeystoreProvider(kst.isSetProvider() 
                                                       ? kst.getProvider() : null,
                                                       LOG);
        KeyStore keyStore = provider == null
                    ? KeyStore.getInstance(type)
                    : KeyStore.getInstance(type, provider);

        if (kst.isSetFile()) {
            keyStore.load(new FileInputStream(kst.getFile()), password);
        } else if (kst.isSetResource()) {
            final java.io.InputStream is = getResourceAsStream(kst.getResource());
            if (is == null) {
                final String msg =
                    "Could not load keystore resource " + kst.getResource();
                LOG.severe(msg);
                throw new java.io.IOException(msg);
            }
            keyStore.load(is, password);
        } else if (kst.isSetUrl()) {
            keyStore.load(new URL(kst.getUrl()).openStream(), password);
        } else {
            String loc = SSLUtils.getKeystore(null, LOG);
            InputStream ins = null;
            if (loc != null) {
                ins = new FileInputStream(loc);
            }
            keyStore.load(ins, password);
        }
        return keyStore;
    }

    /**
     * This method converts a JAXB generated CertStoreType into a KeyStore.
     */
    public static KeyStore getKeyStore(final CertStoreType pst)
        throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {

        if (pst == null) {
            return null;
        }

        if (pst.isSetFile()) {
            return createTrustStore(new FileInputStream(pst.getFile()));
        }
        if (pst.isSetResource()) {
            final java.io.InputStream is = getResourceAsStream(pst.getResource());
            if (is == null) {
                final String msg =
                    "Could not load truststore resource " + pst.getResource();
                LOG.severe(msg);
                throw new java.io.IOException(msg);
            }
            return createTrustStore(is);
        }
        if (pst.isSetUrl()) {
            return createTrustStore(new URL(pst.getUrl()).openStream());
        }
        // TODO error?
        return null;
    }

    private static InputStream getResourceAsStream(String resource) {
        InputStream is = ClassLoaderUtils.getResourceAsStream(resource, TLSParameterJaxBUtils.class);
        if (is == null) {
            Bus bus = BusFactory.getThreadDefaultBus(true);
            ResourceManager rm = bus.getExtension(ResourceManager.class);
            if (rm != null) {
                is = rm.getResourceAsStream(resource);
            }
        }
        return is;
    }
    
    /**
     * Create a KeyStore containing the trusted CA certificates contained
     * in the supplied input stream.
     */
    private static KeyStore createTrustStore(final java.io.InputStream is)
        throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {

        final Collection<? extends Certificate> certs = loadCertificates(is);
        final KeyStore keyStore =
            KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        for (Certificate cert : certs) {
            final X509Certificate xcert = (X509Certificate) cert;
            keyStore.setCertificateEntry(
                xcert.getSubjectX500Principal().getName(),
                cert
            );
        }
        return keyStore;
    }

    /**
     * load the certificates as X.509 certificates
     */
    private static Collection<? extends Certificate>
    loadCertificates(final java.io.InputStream is)
        throws IOException, CertificateException {

        final CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return factory.generateCertificates(is);
    }

    private static char[] deobfuscate(String s) {
        // From the Jetty org.eclipse.jetty.http.security.Password class
        if (!s.startsWith("OBF:")) {
            return s.toCharArray();
        }
        s = s.substring(4);

        char[] b = new char[s.length() / 2];
        int l = 0;
        for (int i = 0; i < s.length(); i += 4) {
            String x = s.substring(i, i + 4);
            int i0 = Integer.parseInt(x, 36);
            int i1 = i0 / 256;
            int i2 = i0 % 256;
            b[l++] = (char) ((i1 + i2 - 254) / 2);
        }

        return new String(b, 0, l).toCharArray();
    }    
    
    /**
     * This method converts the JAXB KeyManagersType into a list of
     * JSSE KeyManagers.
     */
    public static KeyManager[] getKeyManagers(KeyManagersType kmc)
        throws GeneralSecurityException,
               IOException {

        KeyStore keyStore = getKeyStore(kmc.getKeyStore());

        if (keyStore == null) {
            return null;
        }

        String alg = kmc.isSetFactoryAlgorithm()
                     ? kmc.getFactoryAlgorithm()
                     : KeyManagerFactory.getDefaultAlgorithm();

        char[] keyPass = kmc.isSetKeyPassword()
                     ? deobfuscate(kmc.getKeyPassword())
                     : null;

        KeyManagerFactory fac =
                     kmc.isSetProvider()
                     ? KeyManagerFactory.getInstance(alg, kmc.getProvider())
                     : KeyManagerFactory.getInstance(alg);

        fac.init(keyStore, keyPass);

        return fac.getKeyManagers();
    }

    /**
     * This method converts the JAXB KeyManagersType into a list of
     * JSSE TrustManagers.
     */
    public static TrustManager[] getTrustManagers(TrustManagersType tmc)
        throws GeneralSecurityException,
               IOException {

        final KeyStore keyStore =
            tmc.isSetKeyStore()
                ? getKeyStore(tmc.getKeyStore())
                : (tmc.isSetCertStore()
                    ? getKeyStore(tmc.getCertStore())
                    : (KeyStore) null);
        if (keyStore == null) {
            return null;
        }

        String alg = tmc.isSetFactoryAlgorithm()
                     ? tmc.getFactoryAlgorithm()
                     : KeyManagerFactory.getDefaultAlgorithm();

        TrustManagerFactory fac =
                     tmc.isSetProvider()
                     ? TrustManagerFactory.getInstance(alg, tmc.getProvider())
                     : TrustManagerFactory.getInstance(alg);

        fac.init(keyStore);

        return fac.getTrustManagers();
    }
}
