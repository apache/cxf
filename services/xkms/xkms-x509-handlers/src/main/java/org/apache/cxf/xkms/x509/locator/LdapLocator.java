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

import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.xkms.exception.XKMSCertificateException;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.handlers.Locator;
import org.apache.cxf.xkms.model.xkms.LocateRequestType;
import org.apache.cxf.xkms.model.xkms.UnverifiedKeyBindingType;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.x509.handlers.LdapSchemaConfig;
import org.apache.cxf.xkms.x509.handlers.LdapSearch;
import org.apache.cxf.xkms.x509.parser.LocateRequestParser;
import org.apache.cxf.xkms.x509.utils.X509Utils;

public class LdapLocator implements Locator {

    private static final Logger LOG = LogUtils.getL7dLogger(LdapLocator.class);
    private final LdapSearch ldapSearch;
    private CertificateFactory certificateFactory;
    private final LdapSchemaConfig ldapConfig;
    private final String filterUIDTemplate;
    private final String filterIssuerSerialTemplate;
    private final String rootDN;
    
    
    public LdapLocator(LdapSearch ldapSearch, LdapSchemaConfig ldapConfig, String rootDN) {
        this.ldapSearch = ldapSearch;
        this.ldapConfig = ldapConfig;
        this.rootDN = rootDN;
        try {
            this.certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
        filterUIDTemplate = "(" + ldapConfig.getAttrUID() + "=%s)";
        filterIssuerSerialTemplate = "(&(" + ldapConfig.getAttrIssuerID() + "=%s)(" + ldapConfig.getAttrSerialNumber()
            + "=%s))";
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
            String dn = X509Utils.getDN(application, id, ldapConfig.getServiceCertRDNTemplate(),
                                        rootDN);
            content = getCertificateForDn(dn);
        } catch (NamingException e) {
            // Not found
        }
        // Try to find certificate by search for uid attribute
        try {
            if (content == null) {
                String uidAttr = X509Utils.getSubjectDN(application, id, ldapConfig.getServiceCertUIDTemplate());
                content = getCertificateForUIDAttr(uidAttr);
            }
        } catch (NamingException e) {
            // Not found
        }
        return (content != null)
                ? (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(content))
                : null;
    }

    private byte[] getCertificateForDn(String dn) throws NamingException {
        Attribute attr = ldapSearch.getAttribute(dn, ldapConfig.getAttrCrtBinary());
        return (attr != null)
                ? (byte[]) attr.get()
                : null;
    }

    private X509Certificate findByIssuerSerial(String issuer, String serial) throws CertificateException,
            NamingException {

        if ((issuer == null) || (serial == null)) {
            throw new IllegalArgumentException("Issuer and serial applications are expected in request");
        }
        String filter = String.format(filterIssuerSerialTemplate, issuer, serial);
        Attribute attr = ldapSearch.findAttribute(rootDN, filter, ldapConfig.getAttrCrtBinary());
        if ((attr != null) && (attr.get() != null)) {
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream((byte[]) attr
                    .get()));
        } else {
            return null;
        }
    }

    private byte[] getCertificateForUIDAttr(String dn) throws NamingException {
        String filter = String.format(filterUIDTemplate, dn);
        Attribute attr = ldapSearch.findAttribute(rootDN, filter, ldapConfig.getAttrCrtBinary());
        return (attr != null)
                ? (byte[]) attr.get()
                : null;
    }
}
