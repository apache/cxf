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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.cxf.xkms.exception.XKMSConfigurationException;
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
    private static final String CAS_PATH = "cas";
    private final File storageDir;
    private final CertificateFactory certFactory;

    public FileCertificateRepo(String path) throws CertificateException {
        storageDir = new File(path);
        this.certFactory = CertificateFactory.getInstance("X.509");
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

    private boolean saveCategorizedCertificate(X509Certificate cert, UseKeyWithType id, boolean isTrustedCA,
                                               boolean isCA) {
        String name = cert.getSubjectX500Principal().getName();
        String category = "";
        if (isTrustedCA) {
            category = TRUSTED_CAS_PATH;
        }
        if (isCA) {
            category = CAS_PATH;
        }
        try {
            File certFile = new File(storageDir + "/" + category,
                                     getRelativePathForSubjectDn(id.getIdentifier(), cert));
            certFile.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(certFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(cert.getEncoded());
            bos.close();
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException("Error saving certificate " + name + ": " + e.getMessage(), e);
        }
        return true;
    }

    public String convertDnForFileSystem(String dn) {
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

    public String getRelativePathForSubjectDn(String subjectDn, X509Certificate cert)
        throws URISyntaxException {
        BigInteger serialNumber = cert.getSerialNumber();
        String issuer = cert.getIssuerX500Principal().getName();
        String path = convertDnForFileSystem(subjectDn) + "-" + serialNumber.toString() + "-"
                      + convertDnForFileSystem(issuer) + ".cer";
        Pattern p = Pattern.compile("[a-zA-Z_0-9-_]");
        if (p.matcher(path).find()) {
            return path;
        } else {
            throw new URISyntaxException(path, "Input did not match [a-zA-Z_0-9-_].");
        }
    }

    private File[] getX509Files() {
        List<File> certificateFiles = new ArrayList<File>();
        try {
            certificateFiles.addAll(Arrays.asList(storageDir.listFiles()));
            certificateFiles.addAll(Arrays.asList(new File(storageDir + "/" + TRUSTED_CAS_PATH).listFiles()));
            certificateFiles.addAll(Arrays.asList(new File(storageDir + "/" + CAS_PATH).listFiles()));
        } catch (NullPointerException e) {
            //
        }
        if (certificateFiles.isEmpty()) {
            throw new XKMSConfigurationException(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_RECEIVER,
                                                 ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE,
                                                 "File base persistence storage is not found: "
                                                     + storageDir.getPath());
        }
        return certificateFiles.toArray(new File[certificateFiles.size()]);
    }

    public X509Certificate readCertificate(File certFile) throws CertificateException, FileNotFoundException {
        FileInputStream fis = new FileInputStream(certFile);
        return (X509Certificate)certFactory.generateCertificate(fis);
    }

    @Override
    public List<X509Certificate> getTrustedCaCerts() {
        List<X509Certificate> results = new ArrayList<X509Certificate>();
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
        List<X509Certificate> results = new ArrayList<X509Certificate>();
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
    public X509Certificate findByServiceName(String serviceName) {
        return findBySubjectDn(CN_PREFIX + serviceName);
    }

    @Override
    public X509Certificate findBySubjectDn(String subjectDn) {
        List<X509Certificate> result = new ArrayList<X509Certificate>();
        File[] list = getX509Files();
        for (File certFile : list) {
            try {
                if (certFile.isDirectory()) {
                    continue;
                }
                X509Certificate cert = readCertificate(certFile);
                if (subjectDn.equalsIgnoreCase(cert.getSubjectDN().getName())
                    || subjectDn.equalsIgnoreCase(cert.getSubjectX500Principal().getName())) {
                    result.add(cert);
                }
            } catch (Exception e) {
                LOG.warn(String.format("Cannot load certificate from file: %s. Error: %s", certFile,
                                       e.getMessage()));
            }

        }
        return result.get(0);
    }

    @Override
    public X509Certificate findByIssuerSerial(String issuer, String serial) {
        List<X509Certificate> result = new ArrayList<X509Certificate>();
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
        return result.get(0);
    }
}
