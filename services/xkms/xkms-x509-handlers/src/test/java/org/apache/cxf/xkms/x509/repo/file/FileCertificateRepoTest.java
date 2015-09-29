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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.xml.bind.DatatypeConverter;

import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.junit.Assert;
import org.junit.Test;

public class FileCertificateRepoTest {
    private static final String EXAMPLE_SUBJECT_DN = "CN=www.issuer.com, L=CGN, ST=NRW, C=DE, O=Issuer";
    private static final String EXPECTED_CERT_FILE_NAME = "CN-www.issuer.com_L-CGN_ST-NRW_C-DE_O-Issuer.cer";

    @Test
    public void testSaveAndFind() throws CertificateException, IOException, URISyntaxException {
        File storageDir = new File("target/teststore1");
        storageDir.mkdirs();
        FileCertificateRepo fileRegisterHandler = new FileCertificateRepo("target/teststore1");
        InputStream is = this.getClass().getResourceAsStream("/store1/" + EXPECTED_CERT_FILE_NAME);
        if (is == null) {
            throw new RuntimeException("Can not find path " + is + " in classpath");
        }
        X509Certificate cert = loadTestCert(is);
        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.PKIX.getUri());
        key.setIdentifier(EXAMPLE_SUBJECT_DN);
        fileRegisterHandler.saveCertificate(cert, key);

        File certFile = new File(storageDir, fileRegisterHandler.getCertPath(cert, key));
        Assert.assertTrue("Cert file " + certFile + " should exist", certFile.exists());
        FileInputStream fis = new FileInputStream(certFile);
        X509Certificate outCert = loadTestCert(fis);
        Assert.assertEquals(cert, outCert);

        X509Certificate resultCert = fileRegisterHandler.findBySubjectDn(EXAMPLE_SUBJECT_DN);
        Assert.assertNotNull(resultCert);
    }

    private X509Certificate loadTestCert(InputStream is) throws IOException, CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate)factory.generateCertificate(is);
    }

    private String read(InputStream is) throws java.io.IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }

    @SuppressWarnings("unused")
    private void convertBase64ToCer(String sourcePath, String destPath) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(sourcePath);
        String certString = read(is);
        is.close();
        byte[] certData = DatatypeConverter.parseBase64Binary(certString);
        File file = new File(destPath);
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        bos.write(certData);
        bos.close();
        fos.close();
    }

    @Test
    public void testFindBySubjectName() throws CertificateException {
        File storageDir = new File("src/test/resources/store1");
        Assert.assertTrue(storageDir.exists());
        Assert.assertTrue(storageDir.isDirectory());
        FileCertificateRepo persistenceManager = new FileCertificateRepo("src/test/resources/store1");
        X509Certificate resCert = persistenceManager.findBySubjectDn(EXAMPLE_SUBJECT_DN);
        Assert.assertNotNull(resCert);
    }

    @Test
    public void testConvertDnForFileSystem() throws CertificateException {
        String convertedName = new FileCertificateRepo("src/test/resources/store1")
            .convertIdForFileSystem(EXAMPLE_SUBJECT_DN);
        Assert.assertEquals("CN-www.issuer.com_L-CGN_ST-NRW_C-DE_O-Issuer", convertedName);
    }

}
