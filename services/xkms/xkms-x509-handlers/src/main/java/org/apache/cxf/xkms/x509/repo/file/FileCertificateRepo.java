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
package org.apache.cxf.xkms.x509.repo.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.xkms.exception.XKMSConfigurationException;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.x509.repo.CertificateRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileCertificateRepo implements CertificateRepo {
    private static final Logger LOG = LoggerFactory.getLogger(FileCertificateRepo.class);
    private static final String CN_PREFIX = "cn=";
    private static final String TRUSTED_CAS_PATH = "trusted_cas";
    private static final String CRLS_PATH = "crls";
    private static final String CAS_PATH = "cas";
    private static final String SPLIT_REGEX = "\\s*,\\s*";
    private final File storageDir;
    private final CertificateFactory certFactory;

    public FileCertificateRepo(String path) {
        storageDir = new File(path);
        try {
            this.certFactory = CertificateFactory.getInstance("X.509");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void saveCertificate(X509Certificate cert, UseKeyWithType id) {
        saveCategorizedCertificate(cert, id, false, false);
    }

    public void saveTrustedCACertificate(X509Certificate cert, UseKeyWithType id) {
        saveCategorizedCertificate(cert, id, true, false);
    }

    public void saveCACertificate(X509Certificate cert, UseKeyWithType id) {
        saveCategorizedCertificate(cert, id, false, true);
    }

    public void saveCRL(X509CRL crl, UseKeyWithType id) {
        String name = crl.getIssuerX500Principal().getName();
        try {
            String path = convertIdForFileSystem(name) + ".cer";

            File certFile = new File(storageDir + "/" + CRLS_PATH, path);
            certFile.getParentFile().mkdirs();
            try (OutputStream os = Files.newOutputStream(certFile.toPath());
                BufferedOutputStream bos = new BufferedOutputStream(os)) {
                bos.write(crl.getEncoded());
                bos.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error saving CRL " + name + ": " + e.getMessage(), e);
        }
    }

    private boolean saveCategorizedCertificate(X509Certificate cert, UseKeyWithType id, boolean isTrustedCA,
                                               boolean isCA) {
        String category = "";
        if (isTrustedCA) {
            category = TRUSTED_CAS_PATH;
        }
        if (isCA) {
            category = CAS_PATH;
        }
        try {
            File certFile = new File(storageDir + "/" + category,
                                     getCertPath(cert, id));
            certFile.getParentFile().mkdirs();
            try (OutputStream os = Files.newOutputStream(certFile.toPath());
                BufferedOutputStream bos = new BufferedOutputStream(os)) {
                bos.write(cert.getEncoded());
                bos.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error saving certificate " + cert.getSubjectDN() + ": " + e.getMessage(), e);
        }
        return true;
    }

    public String convertIdForFileSystem(String dn) {
        String result = dn.replace("=", "-");
        result = result.replace(", ", "_");
        result = result.replace(",", "_");
        result = result.replace("/", "_");
        result = result.replace("\\", "_");
        result = result.replace("{", "_");
        result = result.replace("}", "_");
        result = result.replace(":", "_");
        return result;
    }

    public String getCertPath(X509Certificate cert, UseKeyWithType id)
        throws URISyntaxException {
        Applications application = null;
        if (id != null) {
            application = Applications.fromUri(id.getApplication());
        }
        final String path;
        if (application == Applications.SERVICE_ENDPOINT) {
            path = id.getIdentifier();
        } else {
            path = cert.getSubjectDN().getName();
        }
        return convertIdForFileSystem(path) + ".cer";
    }

    private File[] getX509Files() {
        List<File> certificateFiles = new ArrayList<>();
        try {
            Collections.addAll(certificateFiles, storageDir.listFiles());
            Collections.addAll(certificateFiles, new File(storageDir, TRUSTED_CAS_PATH).listFiles());
            Collections.addAll(certificateFiles, new File(storageDir, CAS_PATH).listFiles());
            Collections.addAll(certificateFiles, new File(storageDir, CRLS_PATH).listFiles());
        } catch (NullPointerException e) {
            //
        }
        if (certificateFiles.isEmpty()) {
            throw new XKMSConfigurationException(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_RECEIVER,
                                                 ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE,
                                                 "File base persistence storage is not found: "
                                                     + storageDir.getPath());
        }
        return certificateFiles.toArray(new File[0]);
    }

    public X509Certificate readCertificate(File certFile) throws CertificateException, FileNotFoundException,
        IOException {
        try (InputStream is = Files.newInputStream(certFile.toPath())) {
            return (X509Certificate)certFactory.generateCertificate(is);
        }
    }

    public X509CRL readCRL(File crlFile) throws FileNotFoundException, CRLException, IOException {
        try (InputStream is = Files.newInputStream(crlFile.toPath())) {
            return (X509CRL)certFactory.generateCRL(is);
        }
    }

    @Override
    public List<X509Certificate> getTrustedCaCerts() {
        List<X509Certificate> results = new ArrayList<>();
        File[] list = getX509Files();
        for (File certFile : list) {
            try {
                if (certFile.isDirectory()) {
                    continue;
                }
                if (certFile.getParent().endsWith(TRUSTED_CAS_PATH)) {
                    X509Certificate cert = readCertificate(certFile);
                    results.add(cert);
                }
            } catch (Exception e) {
                LOG.warn(String.format("Cannot load certificate from file: %s. Error: %s", certFile,
                                       e.getMessage()));
            }

        }
        return results;
    }

    @Override
    public List<X509Certificate> getCaCerts() {
        List<X509Certificate> results = new ArrayList<>();
        File[] list = getX509Files();
        for (File certFile : list) {
            try {
                if (certFile.isDirectory()) {
                    continue;
                }
                if (certFile.getParent().endsWith(CAS_PATH)) {
                    X509Certificate cert = readCertificate(certFile);
                    results.add(cert);
                }
            } catch (Exception e) {
                LOG.warn(String.format("Cannot load certificate from file: %s. Error: %s", certFile,
                                       e.getMessage()));
            }

        }
        return results;
    }

    @Override
    public List<X509CRL> getCRLs() {
        List<X509CRL> results = new ArrayList<>();
        File[] list = getX509Files();
        for (File crlFile : list) {
            try {
                if (crlFile.isDirectory()) {
                    continue;
                }
                if (crlFile.getParent().endsWith(CRLS_PATH)) {
                    X509CRL crl = readCRL(crlFile);
                    results.add(crl);
                }
            } catch (Exception e) {
                LOG.warn(String.format("Cannot load CRL from file: %s. Error: %s", crlFile,
                                       e.getMessage()));
            }

        }

        return results;
    }

    @Override
    public X509Certificate findByServiceName(String serviceName) {
        return findBySubjectDn(CN_PREFIX + serviceName);
    }

    @Override
    public X509Certificate findByEndpoint(String endpoint) {
        try {
            String path = convertIdForFileSystem(endpoint) + ".cer";
            File certFile = new File(storageDir.getAbsolutePath() + "/" + path);
            if (!certFile.exists()) {
                LOG.warn(String.format("Certificate not found for endpoint %s, path %s", endpoint,
                                       certFile.getAbsolutePath()));
                return null;
            }
            InputStream input = Files.newInputStream(certFile.toPath());
            return (X509Certificate)certFactory.generateCertificate(input);
        } catch (Exception e) {
            LOG.warn(String.format("Cannot load certificate by endpoint: %s. Error: %s", endpoint,
                                   e.getMessage()), e);
            return null;
        }
    }

    @Override
    public X509Certificate findBySubjectDn(String subjectDn) {
        List<X509Certificate> result = new ArrayList<>();
        File[] list = getX509Files();
        String[] sDnArray = subjectDn.split(SPLIT_REGEX);
        Arrays.sort(sDnArray);
        for (File certFile : list) {
            try {
                if (certFile.isDirectory()) {
                    continue;
                }
                X509Certificate cert = readCertificate(certFile);
                LOG.debug("Searching for " + subjectDn + ". Checking cert "
                    + cert.getSubjectDN().getName() + ", " + cert.getSubjectX500Principal().getName());
                String[] csDnArray = cert.getSubjectDN().getName().split(SPLIT_REGEX);
                Arrays.sort(csDnArray);
                String[] csX500Array = cert.getSubjectX500Principal().getName().split(SPLIT_REGEX);
                Arrays.sort(csX500Array);
                if (arraysEqualsIgnoreCaseIgnoreWhiteSpace(sDnArray, csDnArray)
                    || arraysEqualsIgnoreCaseIgnoreWhiteSpace(sDnArray, csX500Array)) {
                    result.add(cert);
                }
            } catch (Exception e) {
                LOG.warn(String.format("Cannot load certificate from file: %s. Error: %s", certFile,
                                       e.getMessage()));
            }

        }
        if (!result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }


    private boolean arraysEqualsIgnoreCaseIgnoreWhiteSpace(String[] s1, String[] s2) {
        if (s1 == null || s2 == null || s1.length != s2.length) {
            return false;
        }
        for (int i = 0; i < s1.length; i++) {
            if (!s1[i].trim().equalsIgnoreCase(s2[i].trim())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public X509Certificate findByIssuerSerial(String issuer, String serial) {
        List<X509Certificate> result = new ArrayList<>();
        File[] list = getX509Files();
        for (File certFile : list) {
            try {
                if (certFile.isDirectory()) {
                    continue;
                }
                X509Certificate cert = readCertificate(certFile);
                BigInteger cs = cert.getSerialNumber();
                BigInteger ss = new BigInteger(serial, 16);
                if (issuer.equalsIgnoreCase(cert.getIssuerX500Principal().getName()) && cs.equals(ss)) {
                    result.add(cert);
                }
            } catch (Exception e) {
                LOG.warn(String.format("Cannot load certificate from file: %s. Error: %s", certFile,
                                       e.getMessage()));
            }

        }
        if (!result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }

}
