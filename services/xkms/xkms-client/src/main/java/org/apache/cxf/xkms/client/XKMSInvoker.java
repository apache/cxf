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
package org.apache.cxf.xkms.client;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.xkms.exception.ExceptionMapper;
import org.apache.cxf.xkms.exception.XKMSException;
import org.apache.cxf.xkms.exception.XKMSLocateException;
import org.apache.cxf.xkms.exception.XKMSValidateException;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.handlers.XKMSConstants;
import org.apache.cxf.xkms.model.xkms.KeyBindingEnum;
import org.apache.cxf.xkms.model.xkms.KeyUsageEnum;
import org.apache.cxf.xkms.model.xkms.LocateRequestType;
import org.apache.cxf.xkms.model.xkms.LocateResultType;
import org.apache.cxf.xkms.model.xkms.MessageAbstractType;
import org.apache.cxf.xkms.model.xkms.QueryKeyBindingType;
import org.apache.cxf.xkms.model.xkms.StatusType;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.model.xkms.ValidateRequestType;
import org.apache.cxf.xkms.model.xkms.ValidateResultType;
import org.apache.cxf.xkms.model.xmldsig.KeyInfoType;
import org.apache.cxf.xkms.model.xmldsig.X509DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

public class XKMSInvoker {
    private static final Logger LOG = LoggerFactory.getLogger(XKMSInvoker.class);

    private static final org.apache.cxf.xkms.model.xmldsig.ObjectFactory DSIG_OF =
            new org.apache.cxf.xkms.model.xmldsig.ObjectFactory();
    private static final org.apache.cxf.xkms.model.xkms.ObjectFactory XKMS_OF =
            new org.apache.cxf.xkms.model.xkms.ObjectFactory();

    private static final String XKMS_LOCATE_INVALID_CERTIFICATE =
            "Cannot instantiate X509 certificate from XKMS response";
    private static final String XKMS_VALIDATE_ERROR = "Certificate [%s] is not valid";

    private final XKMSPortType xkmsConsumer;

    public XKMSInvoker(XKMSPortType xkmsConsumer) {
        this.xkmsConsumer = xkmsConsumer;
    }

    public X509Certificate getServiceCertificate(QName serviceName) {
        return getCertificateForId(Applications.SERVICE_NAME, serviceName.toString());
    }

    public X509Certificate getCertificateForId(Applications application, String id) {
        List<X509AppId> ids = Collections.singletonList(new X509AppId(application, id));
        return getCertificate(ids);
    }

    public X509Certificate getCertificateForIssuerSerial(String issuerDN, BigInteger serial) {
        List<X509AppId> ids = new ArrayList<>();
        ids.add(new X509AppId(Applications.ISSUER, issuerDN));
        ids.add(new X509AppId(Applications.SERIAL, serial.toString(16)));
        return getCertificate(ids);
    }

    public X509Certificate getCertificateForEndpoint(String endpoint) {
        List<X509AppId> ids = new ArrayList<>();
        ids.add(new X509AppId(Applications.SERVICE_ENDPOINT, endpoint));
        return getCertificate(ids);
    }

    public X509Certificate getCertificate(List<X509AppId> ids) {
        try {
            LocateRequestType locateRequestType = prepareLocateXKMSRequest(ids);
            LocateResultType locateResultType = xkmsConsumer.locate(locateRequestType);
            return parseLocateXKMSResponse(locateResultType, ids);
        } catch (RuntimeException e) {
            String msg = String
                .format("XKMS locate call fails for certificate: %s. Error: %s",
                        ids,
                        e.getMessage());
            LOG.warn(msg, e);
            throw new XKMSLocateException(msg, e);
        }
    }

    public boolean validateCertificate(X509Certificate cert) {
        return checkCertificateValidity(cert, false);
    }

    public boolean validateDirectTrustCertificate(X509Certificate cert) {
        return checkCertificateValidity(cert, true);
    }

    protected boolean checkCertificateValidity(X509Certificate cert, boolean directTrust) {
        try {
            ValidateRequestType validateRequestType = prepareValidateXKMSRequest(cert);
            if (directTrust) {
                validateRequestType.getQueryKeyBinding().getKeyUsage()
                    .add(KeyUsageEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SIGNATURE);
            }
            ValidateResultType validateResultType = xkmsConsumer.validate(validateRequestType);
            String id = cert.getSubjectDN().getName();
            CertificateValidationResult result = parseValidateXKMSResponse(validateResultType, id);
            if (!result.isValid()) {
                LOG.warn(String.format("Certificate %s is not valid: %s",
                                         cert.getSubjectDN(), result.getDescription()));
            }
            return result.isValid();
        } catch (RuntimeException e) {
            String msg = String.format("XKMS validate call fails for certificate: %s. Error: %s",
                                       cert.getSubjectDN(),
                                       e.getMessage());
            LOG.warn(msg, e);
            throw new XKMSValidateException(msg, e);
        }
    }

    protected LocateRequestType prepareLocateXKMSRequest(List<X509AppId> ids) {
        QueryKeyBindingType queryKeyBindingType = XKMS_OF
            .createQueryKeyBindingType();

        for (X509AppId id : ids) {
            UseKeyWithType useKeyWithType = XKMS_OF.createUseKeyWithType();
            useKeyWithType.setIdentifier(id.getId());
            useKeyWithType.setApplication(id.getApplication().getUri());

            queryKeyBindingType.getUseKeyWith().add(useKeyWithType);
        }

        LocateRequestType locateRequestType = XKMS_OF.createLocateRequestType();
        locateRequestType.setQueryKeyBinding(queryKeyBindingType);
        setGenericRequestParams(locateRequestType);
        return locateRequestType;
    }

    @SuppressWarnings("unchecked")
    protected X509Certificate parseLocateXKMSResponse(LocateResultType locateResultType, List<X509AppId> ids) {

        XKMSException exception = ExceptionMapper.fromResponse(locateResultType);
        if (exception != null) {
            throw exception;
        }

        if (!locateResultType.getUnverifiedKeyBinding().iterator().hasNext()) {
            LOG.warn("X509Certificate is not found in XKMS for id: " + ids);
            return null;
        }
        KeyInfoType keyInfo = locateResultType.getUnverifiedKeyBinding()
            .iterator().next().getKeyInfo();
        if (!keyInfo.getContent().iterator().hasNext()) {
            LOG.warn("X509Certificate is not found in XKMS for id: " + ids);
            return null;
        }
        JAXBElement<X509DataType> x509Data = (JAXBElement<X509DataType>)keyInfo
            .getContent().iterator().next();
        JAXBElement<byte[]> certificate = (JAXBElement<byte[]>)x509Data
            .getValue().getX509IssuerSerialOrX509SKIOrX509SubjectName()
            .iterator().next();

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate)cf
                .generateCertificate(new ByteArrayInputStream(certificate
                    .getValue()));
        } catch (CertificateException e) {
            throw new XKMSLocateException(XKMS_LOCATE_INVALID_CERTIFICATE, e);
        }
    }

    protected ValidateRequestType prepareValidateXKMSRequest(
                                                          X509Certificate cert) {
        JAXBElement<byte[]> x509Cert;
        try {
            x509Cert = DSIG_OF.createX509DataTypeX509Certificate(cert
                .getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException(e);
        }
        X509DataType x509DataType = DSIG_OF.createX509DataType();
        x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(
                                                                         x509Cert);
        JAXBElement<X509DataType> x509Data = DSIG_OF
            .createX509Data(x509DataType);

        KeyInfoType keyInfoType = DSIG_OF.createKeyInfoType();
        keyInfoType.getContent().add(x509Data);

        QueryKeyBindingType queryKeyBindingType = XKMS_OF
            .createQueryKeyBindingType();
        queryKeyBindingType.setKeyInfo(keyInfoType);

        ValidateRequestType validateRequestType = XKMS_OF
            .createValidateRequestType();
        setGenericRequestParams(validateRequestType);
        validateRequestType.setQueryKeyBinding(queryKeyBindingType);
        // temporary
        validateRequestType.setId(cert.getSubjectDN().toString());
        return validateRequestType;
    }

    protected CertificateValidationResult parseValidateXKMSResponse(ValidateResultType validateResultType,
                                                                 String id) {
        XKMSException exception = ExceptionMapper.fromResponse(validateResultType);
        if (exception != null) {
            throw exception;
        }

        StatusType status = validateResultType.getKeyBinding().iterator()
            .next().getStatus();
        if (KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID != status.getStatusValue()) {
            return new CertificateValidationResult(false, XKMS_VALIDATE_ERROR);
        }
        return new CertificateValidationResult(true, null);
    }

    public static class CertificateValidationResult {

        private final boolean valid;
        private final String description;

        public CertificateValidationResult(boolean valid, String description) {
            this.valid = valid;
            this.description = description;
        }

        public boolean isValid() {
            return valid;
        }

        public String getDescription() {
            return description;
        }
    }

    private void setGenericRequestParams(MessageAbstractType request) {
        request.setService(XKMSConstants.XKMS_ENDPOINT_NAME);
        request.setId(UUID.randomUUID().toString());
    }

}
