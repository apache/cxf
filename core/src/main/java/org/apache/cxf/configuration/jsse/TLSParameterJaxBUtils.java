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
package org.apache.cxf.configuration.jsse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;

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

    public static KeyStore getKeyStore(KeyStoreType kst) throws GeneralSecurityException, IOException {
        return getKeyStore(kst, false);
    }

    /**
     * This method converts a JAXB generated KeyStoreType into a KeyStore.
     */
    public static KeyStore getKeyStore(KeyStoreType kst, boolean trustStore)
        throws GeneralSecurityException,
               IOException {

        if (kst == null) {
            return null;
        }
        final String type;
        if (trustStore) {
            type = SSLUtils.getTrustStoreType(kst.isSetType()
                                     ? kst.getType() : null, LOG, KeyStore.getDefaultType());
        } else {
            type = SSLUtils.getKeystoreType(kst.isSetType()
                                 ? kst.getType() : null, LOG, KeyStore.getDefaultType());
        }

        char[] password = kst.isSetPassword()
                    ? deobfuscate(kst.getPassword())
                    : null;
        if (password == null) {
            final String tmp;
            if (trustStore) {
                tmp = SSLUtils.getTruststorePassword(null, LOG);
            } else {
                tmp = SSLUtils.getKeystorePassword(null, LOG);
            }
            if (tmp != null) {
                password = tmp.toCharArray();
            }
        }
        final String provider;
        if (trustStore) {
            provider = SSLUtils.getTruststoreProvider(kst.isSetProvider() ? kst.getProvider() : null, LOG);
        } else {
            provider = SSLUtils.getKeystoreProvider(kst.isSetProvider() ? kst.getProvider() : null, LOG);
        }
        KeyStore keyStore = provider == null
                    ? KeyStore.getInstance(type)
                    : KeyStore.getInstance(type, provider);

        if (kst.isSetFile()) {
            try (InputStream kstInputStream = Files.newInputStream(Paths.get(kst.getFile()))) {
                keyStore.load(kstInputStream, password);
            }
        } else if (kst.isSetResource()) {
            final InputStream is = getResourceAsStream(kst.getResource());
            if (is == null) {
                final String msg =
                    "Could not load keystore resource " + kst.getResource();
                LOG.severe(msg);
                throw new IOException(msg);
            }
            keyStore.load(is, password);
        } else if (kst.isSetUrl()) {
            keyStore.load(new URL(kst.getUrl()).openStream(), password);
        } else {
            final String loc;
            if (trustStore) {
                loc = SSLUtils.getTruststore(null, LOG);
            } else {
                loc = SSLUtils.getKeystore(null, LOG);
            }
            if (loc != null) {
                try (InputStream ins = Files.newInputStream(Paths.get(loc))) {
                    keyStore.load(ins, password);
                } catch (NoSuchFileException ex) {
                    // Fall back to load the location as a stream
                    try (InputStream ins = getResourceAsStream(loc)) {
                        keyStore.load(ins, password);
                    }
                }
            }
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
        String type;
        if (pst.isSetType()) {
            type = pst.getType();
        } else {
            type = KeyStore.getDefaultType();
        }
        if (pst.isSetFile()) {
            InputStream is = Files.newInputStream(Paths.get(pst.getFile()));
            return createTrustStore(is, type);
        }
        if (pst.isSetResource()) {
            final InputStream is = getResourceAsStream(pst.getResource());
            if (is == null) {
                final String msg =
                    "Could not load truststore resource " + pst.getResource();
                LOG.severe(msg);
                throw new IOException(msg);
            }
            return createTrustStore(is, type);
        }
        if (pst.isSetUrl()) {
            return createTrustStore(new URL(pst.getUrl()).openStream(), type);
        }
        throw new IllegalArgumentException("Could not create KeyStore based on information in CertStoreType");
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
    private static KeyStore createTrustStore(final InputStream is, String type)
        throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {

        final Collection<? extends Certificate> certs = loadCertificates(is);
        final KeyStore keyStore =
            KeyStore.getInstance(type);
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
    private static Collection<? extends Certificate> loadCertificates(final InputStream is)
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

        KeyStore keyStore = getKeyStore(kmc.getKeyStore(), false);

        String alg = kmc.isSetFactoryAlgorithm()
                     ? kmc.getFactoryAlgorithm()
                     : KeyManagerFactory.getDefaultAlgorithm();

        char[] keyPass = getKeyPassword(kmc);

        KeyManagerFactory fac =
                     kmc.isSetProvider()
                     ? KeyManagerFactory.getInstance(alg, kmc.getProvider())
                     : KeyManagerFactory.getInstance(alg);

        fac.init(keyStore, keyPass);

        return fac.getKeyManagers();
    }

    /**
     * This method converts the JAXB KeyManagersType into a list of
     * JSSE KeyManagers.
     */
    public static KeyManager[] getKeyManagers(KeyManagersType kmc, String alias)
        throws GeneralSecurityException,
               IOException {

        KeyStore keyStore = getKeyStore(kmc.getKeyStore(), false);

        String alg = kmc.isSetFactoryAlgorithm()
                     ? kmc.getFactoryAlgorithm()
                     : KeyManagerFactory.getDefaultAlgorithm();

        char[] keyPass = getKeyPassword(kmc);

        KeyManagerFactory fac =
                     kmc.isSetProvider()
                     ? KeyManagerFactory.getInstance(alg, kmc.getProvider())
                     : KeyManagerFactory.getInstance(alg);

        try {
            fac.init(keyStore, keyPass);

            return fac.getKeyManagers();
        } catch (java.security.UnrecoverableKeyException uke) {
            //jsse has the restriction that different key in keystore
            //cannot has different password, use MultiKeyPasswordKeyManager
            //as fallback when this happen
            MultiKeyPasswordKeyManager manager
                = new MultiKeyPasswordKeyManager(keyStore, alias,
                                             new String(keyPass));
            return new KeyManager[]{manager};
        }
    }

    private static char[] getKeyPassword(KeyManagersType kmc) {
        char[] keyPass = kmc.isSetKeyPassword()
            ? deobfuscate(kmc.getKeyPassword())
            : null;

        if (keyPass != null) {
            return keyPass;
        }

        String callbackHandlerClass = kmc.getKeyPasswordCallbackHandler();
        if (callbackHandlerClass == null) {
            return null;
        }
        try {
            final CallbackHandler ch = (CallbackHandler) ClassLoaderUtils
                .loadClass(callbackHandlerClass, TLSParameterJaxBUtils.class).newInstance();
            String prompt = kmc.getKeyStore().getFile();
            if (prompt == null) {
                prompt = kmc.getKeyStore().getResource();
            }
            PasswordCallback pwCb = new PasswordCallback(prompt, false);
            PasswordCallback[] callbacks = new PasswordCallback[] {pwCb};
            ch.handle(callbacks);
            keyPass = callbacks[0].getPassword();
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                    "Cannot load key password from callback handler: " + e.getMessage(), e);
        }
        return keyPass;
    }

    /**
     * This method converts the JAXB TrustManagersType into a list of
     * JSSE TrustManagers.
     */
    @Deprecated
    public static TrustManager[] getTrustManagers(TrustManagersType tmc)
        throws GeneralSecurityException,
               IOException {
        return getTrustManagers(tmc, false);
    }

    public static TrustManager[] getTrustManagers(TrustManagersType tmc, boolean enableRevocation)
        throws GeneralSecurityException,
               IOException {

        final KeyStore keyStore =
            tmc.isSetKeyStore()
                ? getKeyStore(tmc.getKeyStore(), true)
                : (tmc.isSetCertStore()
                    ? getKeyStore(tmc.getCertStore())
                    : null);

        String alg = tmc.isSetFactoryAlgorithm()
                     ? tmc.getFactoryAlgorithm()
                     : TrustManagerFactory.getDefaultAlgorithm();

        TrustManagerFactory fac =
                     tmc.isSetProvider()
                     ? TrustManagerFactory.getInstance(alg, tmc.getProvider())
                     : TrustManagerFactory.getInstance(alg);

        if (enableRevocation) {
            PKIXBuilderParameters param = new PKIXBuilderParameters(keyStore, new X509CertSelector());
            param.setRevocationEnabled(true);

            fac.init(new CertPathTrustManagerParameters(param));
        } else {
            fac.init(keyStore);
        }

        return fac.getTrustManagers();
    }
}
