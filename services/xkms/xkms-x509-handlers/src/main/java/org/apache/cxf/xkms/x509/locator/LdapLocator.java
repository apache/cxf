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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.xkms.exception.XKMSArgumentNotMatchException;
import org.apache.cxf.xkms.exception.XKMSCertificateException;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.handlers.Locator;
import org.apache.cxf.xkms.model.xkms.LocateRequestType;
import org.apache.cxf.xkms.model.xkms.UnverifiedKeyBindingType;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.x509.handlers.LDAPSearch;
import org.apache.cxf.xkms.x509.parser.LocateRequestParser;
import org.apache.cxf.xkms.x509.utils.X509Utils;

public class LdapLocator implements Locator {

    private static final String OU_SERVICES = "ou=services";
    private static final String CN_PREFIX = "cn=";
    private static final String ATTR_UID_NAME = "uid";
    private static final String ATTR_ISSUER_IDENTIFIER = "manager";
    private static final String ATTR_SERIAL_NUMBER = "employeeNumber";
    private static final String ATTR_USER_CERTIFICATE_BINARY = "userCertificate;binary";
    private static final String FILTER_UID = "(" + ATTR_UID_NAME + "=%s)";
    private static final String FILTER_ISSUER_SERIAL = "(&(" + ATTR_ISSUER_IDENTIFIER + "=%s)(" + ATTR_SERIAL_NUMBER
            + "=%s))";
    private static final Logger LOG = LogUtils.getL7dLogger(LdapLocator.class);
    private final LDAPSearch ldapSearch;
    private CertificateFactory certificateFactory;
    private final String rootDN;

    public LdapLocator(LDAPSearch ldapSearch, String rootDN) {
        this.ldapSearch = ldapSearch;
        this.rootDN = rootDN;
        try {
            this.certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
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
        try {
            String issuer = null;
            String serial = null;

            for (UseKeyWithType key : ids) {
                if (Applications.PKIX.getUri().equals(key.getApplication())) {
                    return findByDn(key.getApplication(), key.getIdentifier());
                } else if (Applications.SERVICE_SOAP.getUri().equals(key.getApplication())) {
                    return findByDn(key.getApplication(), key.getIdentifier());
                } else if (Applications.ISSUER.getUri().equals(key.getApplication())) {
                    issuer = key.getIdentifier();
                } else if (Applications.SERIAL.getUri().equals(key.getApplication())) {
                    serial = key.getIdentifier();
                }
            }

            if (issuer != null && serial != null) {
                return findByIssuerSerial(issuer, serial);
            }

            throw new IllegalArgumentException("Application identifier not supported");

        } catch (Exception e) {
            throw new RuntimeException("Search certificates failure: " + e.getMessage(), e);
        }
    }

    private X509Certificate findByDn(String application, String id) throws CertificateException {
        byte[] content = null;
        try {
            String dn = getDN(application, id);
            content = getCertificateForDn(dn);
        } catch (NamingException e) {
            // Not found
        }
        // Try to find certificate by search for distinguishedName attribute
        try {
            if (content == null) {
                content = getCertificateForDnAttr(getSubjectDN(application, id));
            }
        } catch (NamingException e) {
            // Not found
        }
        return (content != null)
                ? (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(content))
                : null;
    }

    private byte[] getCertificateForDn(String dn) throws NamingException {
        Attribute attr = ldapSearch.getAttribute(dn, ATTR_USER_CERTIFICATE_BINARY);
        return (attr != null)
                ? (byte[]) attr.get()
                : null;
    }

    private X509Certificate findByIssuerSerial(String issuer, String serial) throws CertificateException,
            NamingException {

        if ((issuer == null) || (serial == null)) {
            throw new IllegalArgumentException("Issuer and serial applications are expected in request");
        }
        String filter = String.format(FILTER_ISSUER_SERIAL, issuer, serial);
        Attribute attr = ldapSearch.findAttribute(rootDN, filter, ATTR_USER_CERTIFICATE_BINARY);
        if ((attr != null) && (attr.get() != null)) {
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream((byte[]) attr
                    .get()));
        } else {
            return null;
        }
    }

    private String getDN(String applicationUri, String identifier) {
        if (Applications.PKIX.getUri().equals(applicationUri)) {
            return identifier + "," + rootDN;
        } else if (Applications.SERVICE_SOAP.getUri().equals(applicationUri)) {
            String escapedIdentifier = identifier.replaceAll("\\/", Matcher.quoteReplacement("\\/"));
            return CN_PREFIX + escapedIdentifier + "," + OU_SERVICES + "," + rootDN;
        } else {
            throw new XKMSArgumentNotMatchException("Unsupported application uri: " + applicationUri);
        }
    }

    private String getSubjectDN(String application, String id) {
        if (application.equalsIgnoreCase(Applications.SERVICE_SOAP.getUri())) {
            return CN_PREFIX + id;
        } else {
            return id;
        }
    }

    private byte[] getCertificateForDnAttr(String dn) throws NamingException {
        String filter = String.format(FILTER_UID, dn);
        Attribute attr = ldapSearch.findAttribute(rootDN, filter, ATTR_USER_CERTIFICATE_BINARY);
        return (attr != null)
                ? (byte[]) attr.get()
                : null;
    }
}
