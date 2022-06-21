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
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.model.xkms.KeyBindingEnum;
import org.apache.cxf.xkms.model.xkms.ReasonEnum;
import org.apache.cxf.xkms.model.xkms.StatusType;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.model.xkms.ValidateRequestType;
import org.apache.cxf.xkms.x509.repo.file.FileCertificateRepo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TrustedAuthorityValidatorTest extends BasicValidationTest {
    private static final String PATH_TO_RESOURCES = "/trustedAuthorityValidator/";
    private final X509Certificate certificateRoot;
    private final X509Certificate certificateAlice;
    private FileCertificateRepo certificateRepo;

    public TrustedAuthorityValidatorTest() throws CertificateException {
        certificateRoot = readCertificate("root.cer");
        certificateAlice = readCertificate("alice.cer");
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
        subjectDN = certificateAlice.getSubjectX500Principal().getName();
        aliceKey.setIdentifier(subjectDN);
        certificateRepo.saveCACertificate(certificateAlice, aliceKey);
    }

    @Test
    public void testIsCertChainValid() throws CertificateException {
        TrustedAuthorityValidator validator = new TrustedAuthorityValidator(certificateRepo);
        Assert.assertTrue("Root should be valid",
                          validator.isCertificateChainValid(Arrays.asList(certificateRoot)));
        Assert.assertTrue("Alice should be valid",
                          validator.isCertificateChainValid(Arrays.asList(certificateAlice)));
    }

    @Test
    public void testRootCertIsValid() throws JAXBException, CertificateException {
        StatusType result = processRequest("validateRequestOKRoot.xml");
        Assert.assertEquals(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID, result.getStatusValue());
        Assert.assertFalse(result.getValidReason().isEmpty());
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_ISSUER_TRUST.value(), result
            .getValidReason().get(0));
    }

    @Test
    public void testAliceSignedByRootIsValid() throws JAXBException, CertificateException {
        StatusType result = processRequest("validateRequestOKAlice.xml");
        Assert.assertEquals(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID, result.getStatusValue());
        Assert.assertFalse(result.getValidReason().isEmpty());
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_ISSUER_TRUST.value(), result
            .getValidReason().get(0));
    }

    @Test
    public void testDaveSignedByAliceSginedByRootIsValid() throws JAXBException, CertificateException {
        StatusType result = processRequest("validateRequestOKDave.xml");
        Assert.assertEquals(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID, result.getStatusValue());
        Assert.assertFalse(result.getValidReason().isEmpty());
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_ISSUER_TRUST.value(), result
            .getValidReason().get(0));
    }

    @Test
    public void testSelfSignedCertOscarIsNotValid() throws JAXBException, CertificateException {
        StatusType result = processRequest("validateRequestInvalidOscar.xml");
        Assert.assertEquals(result.getStatusValue(), KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_INVALID);
        Assert.assertFalse(result.getInvalidReason().isEmpty());
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_ISSUER_TRUST.value(), result
            .getInvalidReason().get(0));
    }

    private StatusType processRequest(String path) throws JAXBException, CertificateException {
        @SuppressWarnings("unchecked")
        JAXBElement<ValidateRequestType> request = (JAXBElement<ValidateRequestType>)unmarshaller.unmarshal(this
            .getClass().getResourceAsStream(PATH_TO_RESOURCES + path));
        TrustedAuthorityValidator validator = new TrustedAuthorityValidator(certificateRepo);
        return validator.validate(request.getValue());
    }

    private static X509Certificate readCertificate(String path) throws CertificateException {
        InputStream inputStream = TrustedAuthorityValidatorTest.class.getResourceAsStream(PATH_TO_RESOURCES
                                                                                          + path);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate)cf.generateCertificate(inputStream);
    }

}
