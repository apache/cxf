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
package org.apache.cxf.xkms.itests.handlers.validator;

import java.io.InputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.UUID;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.xkms.handlers.XKMSConstants;
import org.apache.cxf.xkms.itests.BasicIntegrationTest;
import org.apache.cxf.xkms.model.xkms.KeyBindingEnum;
import org.apache.cxf.xkms.model.xkms.KeyUsageEnum;
import org.apache.cxf.xkms.model.xkms.MessageAbstractType;
import org.apache.cxf.xkms.model.xkms.QueryKeyBindingType;
import org.apache.cxf.xkms.model.xkms.ReasonEnum;
import org.apache.cxf.xkms.model.xkms.StatusType;
import org.apache.cxf.xkms.model.xkms.ValidateRequestType;
import org.apache.cxf.xkms.model.xmldsig.KeyInfoType;
import org.apache.cxf.xkms.model.xmldsig.X509DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class ValidatorTest extends BasicIntegrationTest {
    private static final String PATH_TO_RESOURCES = "/data/xkms/certificates/";

    private static final org.apache.cxf.xkms.model.xmldsig.ObjectFactory DSIG_OF =
        new org.apache.cxf.xkms.model.xmldsig.ObjectFactory();
    private static final org.apache.cxf.xkms.model.xkms.ObjectFactory XKMS_OF =
        new org.apache.cxf.xkms.model.xkms.ObjectFactory();

    private static final Logger LOG = LoggerFactory.getLogger(ValidatorTest.class);

    @Test
    public void testRootCertIsValid() throws CertificateException {
        X509Certificate rootCertificate = readCertificate("trusted_cas/root.cer");
        ValidateRequestType request = prepareValidateXKMSRequest(rootCertificate);
        StatusType result = doValidate(request);

        Assert.assertEquals(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID, result.getStatusValue());
        Assert.assertFalse(result.getValidReason().isEmpty());
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALIDITY_INTERVAL.value(), result
            .getValidReason().get(0));
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_ISSUER_TRUST.value(), result
            .getValidReason().get(1));
    }

    @Test
    public void testAliceSignedByRootIsValid() throws JAXBException, CertificateException {
        X509Certificate aliceCertificate = readCertificate("cas/alice.cer");
        ValidateRequestType request = prepareValidateXKMSRequest(aliceCertificate);
        StatusType result = doValidate(request);

        Assert.assertEquals(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID, result.getStatusValue());
        Assert.assertFalse(result.getValidReason().isEmpty());
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALIDITY_INTERVAL.value(), result
            .getValidReason().get(0));
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_ISSUER_TRUST.value(), result
            .getValidReason().get(1));
    }

    @Test
    public void testDaveSignedByAliceSginedByRootIsValid() throws JAXBException, CertificateException {
        X509Certificate daveCertificate = readCertificate("dave.cer");
        ValidateRequestType request = prepareValidateXKMSRequest(daveCertificate);
        StatusType result = doValidate(request);

        Assert.assertEquals(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID, result.getStatusValue());
        Assert.assertFalse(result.getValidReason().isEmpty());
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALIDITY_INTERVAL.value(), result
            .getValidReason().get(0));
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_ISSUER_TRUST.value(), result
            .getValidReason().get(1));
    }

    @Test
    public void testSelfSignedCertOscarIsNotValid() throws JAXBException, CertificateException {
        X509Certificate oscarCertificate = readCertificate("oscar.cer");
        ValidateRequestType request = prepareValidateXKMSRequest(oscarCertificate);
        StatusType result = doValidate(request);

        Assert.assertEquals(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_INVALID, result.getStatusValue());
        Assert.assertFalse(result.getInvalidReason().isEmpty());
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_ISSUER_TRUST.value(), result
            .getInvalidReason().get(0));
    }

    @Test
    public void testExpiredCertIsNotValid() throws CertificateException {
        X509Certificate expiredCertificate = readCertificate("expired.cer");
        ValidateRequestType request = prepareValidateXKMSRequest(expiredCertificate);
        StatusType result = doValidate(request);

        Assert.assertEquals(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_INVALID, result.getStatusValue());
        Assert.assertFalse(result.getInvalidReason().isEmpty());
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALIDITY_INTERVAL.value(), result
            .getInvalidReason().get(0));
    }

    @Test
    public void testDaveDirectTrust() throws JAXBException, CertificateException {
        X509Certificate daveCertificate = readCertificate("dave.cer");
        ValidateRequestType request = prepareValidateXKMSRequest(daveCertificate);
        request.getQueryKeyBinding().getKeyUsage().add(KeyUsageEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SIGNATURE);
        StatusType result = doValidate(request);

        Assert.assertEquals(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID, result.getStatusValue());
        Assert.assertFalse(result.getValidReason().isEmpty());
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALIDITY_INTERVAL.value(), result
            .getValidReason().get(0));
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_ISSUER_TRUST.value(), result
            .getValidReason().get(1));
        Assert.assertEquals(XKMSConstants.DIRECT_TRUST_VALIDATION, result
                            .getValidReason().get(2));
    }

    @Test
    public void testWss40DirectTrustNegative() throws JAXBException, CertificateException {
        X509Certificate wss40Certificate = readCertificate("wss40.cer");
        ValidateRequestType request = prepareValidateXKMSRequest(wss40Certificate);
        request.getQueryKeyBinding().getKeyUsage().add(KeyUsageEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SIGNATURE);
        StatusType result = doValidate(request);

        Assert.assertEquals(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_INVALID, result.getStatusValue());
        Assert.assertFalse(result.getInvalidReason().isEmpty());
        Assert.assertEquals(XKMSConstants.DIRECT_TRUST_VALIDATION, result
                            .getInvalidReason().get(0));
    }

    /*
     * Method is taken from {@link org.apache.cxf.xkms.client.XKMSInvokder}.
     */
    private ValidateRequestType prepareValidateXKMSRequest(X509Certificate cert) {
        JAXBElement<byte[]> x509Cert;
        try {
            x509Cert = DSIG_OF.createX509DataTypeX509Certificate(cert.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException(e);
        }
        X509DataType x509DataType = DSIG_OF.createX509DataType();
        x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(x509Cert);
        JAXBElement<X509DataType> x509Data = DSIG_OF.createX509Data(x509DataType);

        KeyInfoType keyInfoType = DSIG_OF.createKeyInfoType();
        keyInfoType.getContent().add(x509Data);

        QueryKeyBindingType queryKeyBindingType = XKMS_OF.createQueryKeyBindingType();
        queryKeyBindingType.setKeyInfo(keyInfoType);

        ValidateRequestType validateRequestType = XKMS_OF.createValidateRequestType();
        setGenericRequestParams(validateRequestType);
        validateRequestType.setQueryKeyBinding(queryKeyBindingType);
        // temporary
        validateRequestType.setId(cert.getSubjectDN().toString());
        return validateRequestType;
    }

    private void setGenericRequestParams(MessageAbstractType request) {
        request.setService(XKMSConstants.XKMS_ENDPOINT_NAME);
        request.setId(UUID.randomUUID().toString());
    }

    private X509Certificate readCertificate(String path) throws CertificateException {
        InputStream inputStream = ValidatorTest.class.getResourceAsStream(PATH_TO_RESOURCES + path);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate)cf.generateCertificate(inputStream);
    }

    private StatusType doValidate(ValidateRequestType request) {
        try {
            return xkmsService.validate(request).getKeyBinding().get(0).getStatus();
        } catch (Exception e) {
            // Avoid serialization problems for some exceptions when transported by pax exam
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

}
