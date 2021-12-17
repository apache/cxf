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

package org.apache.cxf.xkms.x509.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.xkms.model.xkms.UnverifiedKeyBindingType;
import org.apache.cxf.xkms.model.xmldsig.KeyInfoType;
import org.apache.cxf.xkms.model.xmldsig.ObjectFactory;
import org.apache.cxf.xkms.model.xmldsig.X509DataType;

public final class X509Utils {
    public static final QName X509_DATA = new QName("http://www.w3.org/2000/09/xmldsig#", "X509Data");

    public static final QName X509_KEY_NAME = new QName("http://www.w3.org/2000/09/xmldsig#", "KeyName");

    public static final QName X509_ISSUER_SERIAL = new QName("http://www.w3.org/2000/09/xmldsig#", "X509IssuerSerial");

    public static final QName X509_SUBJECT_NAME = new QName("http://www.w3.org/2000/09/xmldsig#", "X509SubjectName");

    public static final QName X509_CERTIFICATE = new QName("http://www.w3.org/2000/09/xmldsig#", "X509Certificate");

    private static final Logger LOG = LogUtils.getL7dLogger(X509Utils.class);

    private static final CertificateFactory X509_FACTORY;


    static {
        try {
            X509_FACTORY = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new IllegalStateException("Cannot initialize X509 CertificateFactory: " + e.getMessage(), e);
        }
    }

    private X509Utils() {

    }

    public static void parseX509Data(X509DataType x509Data, List<X509Certificate> certs) throws CertificateException {
        List<Object> dataList = x509Data.getX509IssuerSerialOrX509SKIOrX509SubjectName();
        for (Object x509Object : dataList) {
            if (x509Object instanceof JAXBElement<?>) {
                JAXBElement<?> x509Item = (JAXBElement<?>) x509Object;
                X509Certificate certificate = parseX509Item(x509Item);
                if (null != certificate) {
                    certs.add(certificate);
                }
            }
        }
    }

    private static X509Certificate parseX509Item(JAXBElement<?> x509Item) throws CertificateException {
        QName x509ItemName = x509Item.getName();
        if (X509_CERTIFICATE.equals(x509ItemName)) {
            X509Certificate certificate;
            certificate = extractCertificate(x509Item);
            LOG.fine("Extracted " + certificate.getSubjectX500Principal().getName());
            return certificate;
        }
        return null;
    }

    private static X509Certificate extractCertificate(JAXBElement<?> x509Item) throws CertificateException {
        @SuppressWarnings("unchecked")
        JAXBElement<byte[]> byteElement = (JAXBElement<byte[]>) x509Item;
        byte[] bytes = byteElement.getValue();
        InputStream stream = new ByteArrayInputStream(bytes);
        Certificate certificate = X509_FACTORY.generateCertificate(stream);
        if (certificate instanceof X509Certificate) {
            return (X509Certificate) certificate;
        }
        throw new CertificateException("Unsupported certificate type encountered: "
                + ((certificate != null && certificate.getClass() != null)
                    ? certificate.getClass().getName() : "Null"));
    }

    public static UnverifiedKeyBindingType getUnverifiedKeyBinding(X509Certificate cert)
        throws CertificateEncodingException {
        UnverifiedKeyBindingType unverifiedKeyBinding = new UnverifiedKeyBindingType();
        unverifiedKeyBinding.setKeyInfo(getKeyInfo(cert));
        return unverifiedKeyBinding;
    }

    public static KeyInfoType getKeyInfo(X509Certificate cert) throws CertificateEncodingException {
        KeyInfoType keyInfo = new KeyInfoType();
        JAXBElement<byte[]> certificate = new ObjectFactory().createX509DataTypeX509Certificate(cert.getEncoded());
        X509DataType x509DataType = new X509DataType();
        List<Object> x509DataContent = x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName();
        x509DataContent.add(certificate);
        JAXBElement<X509DataType> x509Data = new ObjectFactory().createX509Data(x509DataType);
        List<Object> keyInfoContent = keyInfo.getContent();
        keyInfoContent.add(x509Data);
        return keyInfo;
    }

    public static void assertElementNotNull(Object element, Class<?> elementClass) {
        if (element == null) {
            throw new IllegalArgumentException(elementClass.getName() + " must be set");
        }
    }

}
