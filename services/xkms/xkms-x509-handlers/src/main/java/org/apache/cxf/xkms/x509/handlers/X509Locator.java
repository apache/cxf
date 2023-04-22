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

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.xkms.exception.XKMSCertificateException;
import org.apache.cxf.xkms.exception.XKMSException;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.handlers.Locator;
import org.apache.cxf.xkms.model.xkms.LocateRequestType;
import org.apache.cxf.xkms.model.xkms.QueryKeyBindingType;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;
import org.apache.cxf.xkms.model.xkms.UnverifiedKeyBindingType;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.model.xmldsig.KeyInfoType;
import org.apache.cxf.xkms.model.xmldsig.X509DataType;
import org.apache.cxf.xkms.model.xmldsig.X509IssuerSerialType;
import org.apache.cxf.xkms.x509.repo.CertificateRepo;
import org.apache.cxf.xkms.x509.utils.X509Utils;

public class X509Locator implements Locator {

    private CertificateRepo certRepo;

    public X509Locator(CertificateRepo certRepo) throws CertificateException {
        this.certRepo = certRepo;
    }

    @Override
    public UnverifiedKeyBindingType locate(LocateRequestType request) {
        List<UseKeyWithType> keyIDs = parse(request);
        X509Certificate cert;
        try {
            cert = findCertificate(keyIDs);
            if (cert == null) {
                return null;
            }
            UnverifiedKeyBindingType result = new UnverifiedKeyBindingType();
            result.setKeyInfo(X509Utils.getKeyInfo(cert));
            return result;
        } catch (CertificateEncodingException e) {
            throw new XKMSCertificateException("Cannot encode certificate: " + e.getMessage(), e);
        } catch (CertificateException e1) {
            throw new XKMSCertificateException(e1.getMessage(), e1);
        }
    }

    public X509Certificate findCertificate(List<UseKeyWithType> ids) throws CertificateException {
        X509Certificate cert = null;
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("No UseKeyWithType elements found");
        }
        if (ids.size() == 1) {
            Applications application = Applications.fromUri(ids.get(0).getApplication());
            String id = ids.get(0).getIdentifier();
            if (application == Applications.PKIX) {
                cert = certRepo.findBySubjectDn(id);
            } else if (application == Applications.SERVICE_NAME) {
                cert = certRepo.findByServiceName(id);
            } else if (application == Applications.SERVICE_ENDPOINT) {
                cert = certRepo.findByEndpoint(id);
            }
        }
        String issuer = getIdForApplication(Applications.ISSUER, ids);
        String serial = getIdForApplication(Applications.SERIAL, ids);
        if ((issuer != null) && (serial != null)) {
            cert = certRepo.findByIssuerSerial(issuer, serial);
        }
        return cert;
    }

    private String getIdForApplication(Applications application, List<UseKeyWithType> ids) {
        for (UseKeyWithType id : ids) {
            if (application.getUri().equalsIgnoreCase(id.getApplication())) {
                return id.getIdentifier();
            }
        }
        return null;
    }

    private List<UseKeyWithType> parse(LocateRequestType request) {
        List<UseKeyWithType> keyIDs = new ArrayList<>();
        if (request == null) {
            return keyIDs;
        }

        QueryKeyBindingType query = request.getQueryKeyBinding();
        if (query == null) {
            return keyIDs;
        }

        // http://www.w3.org/TR/xkms2/ [213]
        if (query.getTimeInstant() != null) {
            throw new XKMSException(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_RECEIVER,
                    ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_TIME_INSTANT_NOT_SUPPORTED);
        }

        keyIDs.addAll(parse(query.getKeyInfo()));

        List<UseKeyWithType> useKeyList = query.getUseKeyWith();
        keyIDs.addAll(useKeyList);

        return keyIDs;
    }

    private List<UseKeyWithType> parse(KeyInfoType keyInfo) {
        List<UseKeyWithType> keyIDs = new ArrayList<>();

        if (keyInfo == null) {
            return keyIDs;
        }

        List<Object> content = keyInfo.getContent();
        for (Object obj1 : content) {
            if (obj1 instanceof JAXBElement) {
                JAXBElement<?> keyInfoChild = (JAXBElement<?>) obj1;
                if (X509Utils.X509_KEY_NAME.equals(keyInfoChild.getName())) {
                    UseKeyWithType keyDN = new UseKeyWithType();
                    keyDN.setApplication(Applications.PKIX.getUri());
                    keyDN.setIdentifier((String) keyInfoChild.getValue());
                    keyIDs.add(keyDN);

                } else if (X509Utils.X509_DATA.equals(keyInfoChild.getName())) {
                    X509DataType x509Data = (X509DataType) keyInfoChild.getValue();
                    List<Object> x509DataContent = x509Data.getX509IssuerSerialOrX509SKIOrX509SubjectName();

                    for (Object obj2 : x509DataContent) {
                        if (obj2 instanceof JAXBElement) {
                            JAXBElement<?> x509DataChild = (JAXBElement<?>) obj2;

                            if (X509Utils.X509_ISSUER_SERIAL.equals(x509DataChild.getName())) {
                                X509IssuerSerialType x509IssuerSerial = (X509IssuerSerialType) x509DataChild.getValue();

                                UseKeyWithType issuer = new UseKeyWithType();
                                issuer.setApplication(Applications.ISSUER.getUri());
                                issuer.setIdentifier(x509IssuerSerial.getX509IssuerName());
                                keyIDs.add(issuer);

                                UseKeyWithType serial = new UseKeyWithType();
                                serial.setApplication(Applications.SERIAL.getUri());
                                serial.setIdentifier(x509IssuerSerial.getX509SerialNumber().toString());
                                keyIDs.add(serial);

                            } else if (X509Utils.X509_SUBJECT_NAME.equals(x509DataChild.getName())) {
                                UseKeyWithType keyDN = new UseKeyWithType();
                                keyDN.setApplication(Applications.PKIX.getUri());
                                keyDN.setIdentifier((String) x509DataChild.getValue());
                                keyIDs.add(keyDN);
                            }
                        }
                    }
                }
            }
        }
        return keyIDs;
    }

}
