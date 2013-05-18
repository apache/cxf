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

package org.apache.cxf.xkms.x509.locator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.xkms.exception.XKMSArgumentNotMatchException;
import org.apache.cxf.xkms.exception.XKMSCertificateException;
import org.apache.cxf.xkms.exception.XKMSConfigurationException;
import org.apache.cxf.xkms.exception.XKMSTooManyResponsesException;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.handlers.Locator;
import org.apache.cxf.xkms.model.xkms.LocateRequestType;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;
import org.apache.cxf.xkms.model.xkms.UnverifiedKeyBindingType;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.x509.parser.LocateRequestParser;
import org.apache.cxf.xkms.x509.utils.X509Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileLocator implements Locator {

    private static final String CN_PREFIX = "cn=";
    private static final Logger LOG = LoggerFactory.getLogger(FileLocator.class);
    private final File storageDir;
    private final CertificateFactory certFactory;

    public FileLocator(File storageDir) throws CertificateException {
        if (storageDir == null) {
            throw new IllegalStateException("File Persistence: root certificate directory is not initialized");
        }
        this.storageDir = storageDir;
        this.certFactory = CertificateFactory.getInstance("X.509");
    }

    @Override
    public UnverifiedKeyBindingType locate(LocateRequestType request) {

        List<UseKeyWithType> keyIDs = LocateRequestParser.parse(request);

        X509Certificate cert = findCertificate(keyIDs);

        if (cert == null) {
            return null;
        }

        try {
            UnverifiedKeyBindingType result = new UnverifiedKeyBindingType();
            result.setKeyInfo(X509Utils.getKeyInfo(cert));
            return result;
        } catch (CertificateEncodingException e) {
            throw new XKMSCertificateException("Cannot encode certificate: " + e.getMessage(), e);
        }
    }

    public X509Certificate findCertificate(List<UseKeyWithType> ids) {
        List<X509Certificate> certs = Collections.emptyList();
        if ((ids.size() == 1) && (getIdForApplication(Applications.PKIX, ids) != null)
                || (getIdForApplication(Applications.SERVICE_SOAP, ids) != null)) {
            String subjectDN = getSubjectDN(ids.get(0).getApplication(), ids.get(0).getIdentifier());
            certs = findCertificateBySubjectDn(subjectDN);
        }
        String issuer = getIdForApplication(Applications.ISSUER, ids);
        String serial = getIdForApplication(Applications.SERIAL, ids);
        if ((issuer != null) && (serial != null)) {
            certs = findCertificateByIssuerSerial(issuer, serial);
        }
        if (certs.size() > 1) {
            throw new XKMSTooManyResponsesException("More than one matching key was found for: " + ids);
        }
        if (certs.size() == 0) {
            return null;
        }
        return certs.get(0);
    }

    private String getSubjectDN(String applicationUri, String identifier) {
        if (Applications.PKIX.getUri().equals(applicationUri)) {
            return identifier;
        } else if (Applications.SERVICE_SOAP.getUri().equals(applicationUri)) {
            return CN_PREFIX + identifier;
        } else {
            throw new XKMSArgumentNotMatchException("Unsupported application uri: " + applicationUri);
        }
    }

    private List<X509Certificate> findCertificateBySubjectDn(String subjectDN) {
        List<X509Certificate> result = new ArrayList<X509Certificate>();
        File[] list = getX509Files();
        for (File certFile : list) {
            try {
                if (certFile.isDirectory()) {
                    continue;
                }
                X509Certificate cert = readCertificate(certFile);
                if (subjectDN.equalsIgnoreCase(cert.getSubjectDN().getName())
                        || subjectDN.equalsIgnoreCase(cert.getSubjectX500Principal().getName())) {
                    result.add(cert);
                }
            } catch (Exception e) {
                LOG.warn(String.format("Cannot load certificate from file: %s. Error: %s", certFile, e.getMessage()));
            }

        }
        return result;
    }

    private List<X509Certificate> findCertificateByIssuerSerial(String issuer, String serial) {
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
                LOG.warn(String.format("Cannot load certificate from file: %s. Error: %s", certFile, e.getMessage()));
            }

        }
        return result;
    }

    private File[] getX509Files() {
        File[] list = storageDir.listFiles();
        if (list == null) {
            throw new XKMSConfigurationException(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_RECEIVER,
                    ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE,
                    "File base persistence storage is not found: " + storageDir.getPath());
        }
        return list;
    }

    private X509Certificate readCertificate(File certFile) throws CertificateException, FileNotFoundException {
        FileInputStream fis = new FileInputStream(certFile);
        return (X509Certificate) certFactory.generateCertificate(fis);
    }

    public void saveCertificate(X509Certificate cert, UseKeyWithType id) {
        String name = cert.getSubjectX500Principal().getName();
        try {
            File certFile = new File(storageDir, getRelativePathForSubjectDn(id.getIdentifier(), cert));
            certFile.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(certFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(cert.getEncoded());
            bos.close();
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException("Error saving certificate " + name + ": " + e.getMessage(), e);
        }
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

    public String getRelativePathForSubjectDn(String subjectDn, X509Certificate cert) {
        BigInteger serialNumber = cert.getSerialNumber();
        String issuer = cert.getIssuerX500Principal().getName();
        String path = convertDnForFileSystem(subjectDn) + "-" + serialNumber.toString() + "-"
                + convertDnForFileSystem(issuer) + ".cer";
        // TODO Filter for only valid and safe characters
        return path;
    }

    private String getIdForApplication(Applications application, List<UseKeyWithType> ids) {
        for (UseKeyWithType id : ids) {
            if (application.getUri().equalsIgnoreCase(id.getApplication())) {
                return id.getIdentifier();
            }
        }
        return null;
    }

}
