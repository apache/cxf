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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        assertTrue("Cert file " + certFile + " should exist", certFile.exists());
        try (FileInputStream fis = new FileInputStream(certFile)) {
            X509Certificate outCert = loadTestCert(fis);
            assertEquals(cert, outCert);
        }

        X509Certificate resultCert = fileRegisterHandler.findBySubjectDn(EXAMPLE_SUBJECT_DN);
        assertNotNull(resultCert);
    }

    private X509Certificate loadTestCert(InputStream is) throws IOException, CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate)factory.generateCertificate(is);
    }

    @Test
    public void testFindBySubjectName() throws CertificateException {
        File storageDir = new File("src/test/resources/store1");
        assertTrue(storageDir.exists());
        assertTrue(storageDir.isDirectory());
        FileCertificateRepo persistenceManager = new FileCertificateRepo("src/test/resources/store1");
        X509Certificate resCert = persistenceManager.findBySubjectDn(EXAMPLE_SUBJECT_DN);
        assertNotNull(resCert);
    }

    @Test
    public void testConvertDnForFileSystem() throws CertificateException {
        String convertedName = new FileCertificateRepo("src/test/resources/store1")
            .convertIdForFileSystem(EXAMPLE_SUBJECT_DN);
        assertEquals("CN-www.issuer.com_L-CGN_ST-NRW_C-DE_O-Issuer", convertedName);
    }

    @Test
    public void testGetCertPath() throws CertificateException, URISyntaxException, IOException {
        File storageDir = new File("target/teststore2");
        storageDir.mkdirs();
        FileCertificateRepo fileRegisterHandler = new FileCertificateRepo("target/teststore2");

        InputStream is = this.getClass().getResourceAsStream("/store1/" + EXPECTED_CERT_FILE_NAME);
        if (is == null) {
            throw new RuntimeException("Can not find path " + is + " in classpath");
        }
        X509Certificate cert = loadTestCert(is);

        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.SERVICE_ENDPOINT.getUri());
        key.setIdentifier(EXAMPLE_SUBJECT_DN);

        String path = fileRegisterHandler.getCertPath(cert, key);
        assertEquals(EXPECTED_CERT_FILE_NAME, path);

        // Test that we're not vulnerable to a path traversal attack
        key.setIdentifier("../../../test.txt");

        path = fileRegisterHandler.getCertPath(cert, key);
        assertEquals(".._.._.._test.txt.cer", path);
    }


}
