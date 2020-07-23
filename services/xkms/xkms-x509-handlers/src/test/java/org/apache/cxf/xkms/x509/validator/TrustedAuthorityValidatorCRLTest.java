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

package org.apache.cxf.xkms.x509.validator;

import java.io.File;
import java.io.InputStream;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.x509.repo.file.FileCertificateRepo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TrustedAuthorityValidatorCRLTest extends BasicValidationTest {
    private static final String PATH_TO_RESOURCES = "/trustedAuthorityValidator/";
    private final X509Certificate certificateRoot;
    private final X509Certificate certificateWss40Rev;
    private final X509Certificate certificateWss40;
    private final X509CRL crl;
    private FileCertificateRepo certificateRepo;

    public TrustedAuthorityValidatorCRLTest() throws CertificateException, CRLException {
        certificateRoot = readCertificate("wss40CA.cer");
        certificateWss40Rev = readCertificate("wss40rev.cer");
        certificateWss40 = readCertificate("wss40.cer");
        crl = readCRL("wss40CACRL.cer");
    }

    @Before
    public void setUpCertificateRepo() throws CertificateException {
        File storageDir = new File("target/teststore_trusted_authority_validator");
        FileUtils.removeDir(storageDir);
        storageDir.mkdirs();
        certificateRepo = new FileCertificateRepo("target/teststore_trusted_authority_validator");

        UseKeyWithType rootKey = new UseKeyWithType();
        rootKey.setApplication(Applications.PKIX.getUri());
        String subjectDN = certificateRoot.getSubjectX500Principal().getName();
        rootKey.setIdentifier(subjectDN);
        certificateRepo.saveTrustedCACertificate(certificateRoot, rootKey);

        UseKeyWithType aliceKey = new UseKeyWithType();
        aliceKey.setApplication(Applications.PKIX.getUri());
        subjectDN = certificateWss40Rev.getSubjectX500Principal().getName();
        aliceKey.setIdentifier(subjectDN);
        certificateRepo.saveCACertificate(certificateWss40Rev, aliceKey);

        UseKeyWithType bobKey = new UseKeyWithType();
        bobKey.setApplication(Applications.PKIX.getUri());
        subjectDN = certificateWss40.getSubjectX500Principal().getName();
        bobKey.setIdentifier(subjectDN);
        certificateRepo.saveCACertificate(certificateWss40, bobKey);

        UseKeyWithType crlKey = new UseKeyWithType();
        crlKey.setApplication(Applications.PKIX.getUri());
        crlKey.setIdentifier(crl.getIssuerX500Principal().getName());
        certificateRepo.saveCRL(crl, crlKey);
    }

    @Test
    public void testIsCertChainValid() throws CertificateException {
        TrustedAuthorityValidator validator = new TrustedAuthorityValidator(certificateRepo);
        validator.setEnableRevocation(true);
        Assert.assertTrue("Root should be valid",
                          validator.isCertificateChainValid(Arrays.asList(certificateRoot)));
        Assert.assertTrue("wss40rev should not be valid",
                          !validator.isCertificateChainValid(Arrays.asList(certificateWss40Rev)));
        Assert.assertTrue("wss40 should be valid",
                          validator.isCertificateChainValid(Arrays.asList(certificateWss40)));
    }

    private static X509Certificate readCertificate(String path) throws CertificateException {
        InputStream inputStream = TrustedAuthorityValidatorCRLTest.class.getResourceAsStream(PATH_TO_RESOURCES
                                                                                          + path);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate)cf.generateCertificate(inputStream);
    }

    private static X509CRL readCRL(String path) throws CertificateException, CRLException {
        InputStream inputStream = TrustedAuthorityValidatorCRLTest.class.getResourceAsStream(PATH_TO_RESOURCES
                                                                                          + path);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509CRL)cf.generateCRL(inputStream);
    }

}
