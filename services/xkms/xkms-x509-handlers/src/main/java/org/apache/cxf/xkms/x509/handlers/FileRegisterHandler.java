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
package org.apache.cxf.xkms.x509.handlers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.cxf.xkms.model.xkms.UseKeyWithType;

public class FileRegisterHandler extends AbstractX509RegisterHandler {

    private final File storageDir;

    public FileRegisterHandler(File storageDir) throws CertificateException {
        super();
        if (storageDir == null) {
            throw new IllegalStateException("File Persistence: root certificate directory is not initialized");
        }
        this.storageDir = storageDir;
    }

    @Override
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

    public String getRelativePathForSubjectDn(String subjectDn, X509Certificate cert) {
        BigInteger serialNumber = cert.getSerialNumber();
        String issuer = cert.getIssuerX500Principal().getName();
        String path = convertDnForFileSystem(subjectDn)
                + "-" + serialNumber.toString() + "-" + convertDnForFileSystem(issuer) + ".cer";
        // TODO Filter for only valid and safe characters
        return path;
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

}
