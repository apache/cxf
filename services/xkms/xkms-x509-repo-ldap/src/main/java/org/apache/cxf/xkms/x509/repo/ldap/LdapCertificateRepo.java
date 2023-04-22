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
package org.apache.cxf.xkms.x509.repo.ldap;

import java.io.ByteArrayInputStream;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.x509.repo.CertificateRepo;

public class LdapCertificateRepo implements CertificateRepo {
    private static final Logger LOG = LogUtils.getL7dLogger(LdapCertificateRepo.class);
    private static final String ATTR_OBJECT_CLASS = "objectClass";

    private LdapSearch ldapSearch;
    private String rootDN;
    private CertificateFactory certificateFactory;
    private final LdapSchemaConfig ldapConfig;
    private final String filterUIDTemplate;
    private final String filterIssuerSerialTemplate;

    /**
     *
     * @param ldapSearch
     * @param ldapConfig
     * @param rootDN rootDN of the LDAP tree
     */
    public LdapCertificateRepo(LdapSearch ldapSearch, LdapSchemaConfig ldapConfig, String rootDN) {
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
    public List<X509Certificate> getTrustedCaCerts() {
        return getCertificatesFromLdap(rootDN, ldapConfig.getTrustedAuthorityFilter(), ldapConfig.getAttrCrtBinary());
    }

    @Override
    public List<X509Certificate> getCaCerts() {
        return getCertificatesFromLdap(rootDN, ldapConfig.getIntermediateFilter(), ldapConfig.getAttrCrtBinary());
    }

    @Override
    public List<X509CRL> getCRLs() {
        return getCRLsFromLdap(rootDN, ldapConfig.getCrlFilter(), ldapConfig.getAttrCrlBinary());
    }

    protected List<X509Certificate> getCertificatesFromLdap(String tmpRootDN, String tmpFilter, String tmpAttrName) {
        try {
            List<X509Certificate> certificates = new ArrayList<>();
            NamingEnumeration<SearchResult> answer = ldapSearch.searchSubTree(tmpRootDN, tmpFilter);
            while (answer.hasMore()) {
                SearchResult sr = answer.next();
                Attributes attrs = sr.getAttributes();
                Attribute attribute = attrs.get(tmpAttrName);
                if (attribute != null) {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    X509Certificate certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(
                            (byte[]) attribute.get()));
                    certificates.add(certificate);
                }
            }
            return certificates;
        } catch (CertificateException | NamingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected List<X509CRL> getCRLsFromLdap(String tmpRootDN, String tmpFilter, String tmpAttrName) {
        try {
            List<X509CRL> crls = new ArrayList<>();
            NamingEnumeration<SearchResult> answer = ldapSearch.searchSubTree(tmpRootDN, tmpFilter);
            while (answer.hasMore()) {
                SearchResult sr = answer.next();
                Attributes attrs = sr.getAttributes();
                Attribute attribute = attrs.get(tmpAttrName);
                if (attribute != null) {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    X509CRL crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(
                            (byte[]) attribute.get()));
                    crls.add(crl);
                }
            }
            return crls;
        } catch (CertificateException | NamingException | CRLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected void saveCertificate(X509Certificate cert, String dn, Map<String, String> appAttrs) {
        Attributes attribs = new BasicAttributes();
        attribs.put(new BasicAttribute(ATTR_OBJECT_CLASS, ldapConfig.getCertObjectClass()));
        attribs.put(new BasicAttribute(ldapConfig.getAttrUID(), cert.getSubjectX500Principal().getName()));
        attribs.put(new BasicAttribute(ldapConfig.getAttrIssuerID(), cert.getIssuerX500Principal().getName()));
        attribs.put(new BasicAttribute(ldapConfig.getAttrSerialNumber(), cert.getSerialNumber().toString(16)));
        addConstantAttributes(ldapConfig.getConstAttrNamesCSV(), ldapConfig.getConstAttrValuesCSV(), attribs);
        if (appAttrs != null && !appAttrs.isEmpty()) {
            for (Map.Entry<String, String> entry : appAttrs.entrySet()) {
                attribs.put(new BasicAttribute(entry.getKey(), entry.getValue()));
            }
        }
        try {
            attribs.put(new BasicAttribute(ldapConfig.getAttrCrtBinary(), cert.getEncoded()));
            ldapSearch.bind(dn, attribs);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected void addConstantAttributes(String names, String values, Attributes attribs) {
        String[] arrNames = names.split(",");
        String[] arrValues = values.split(",");
        if (arrNames.length != arrValues.length) {
            throw new IllegalArgumentException(
                      String.format("Inconsintent constant attributes: %s; %s",  names, values));
        }
        for (int i = 0; i < arrNames.length; i++) {
            attribs.put(new BasicAttribute(arrNames[i], arrValues[i]));
        }
    }

    @Override
    public X509Certificate findBySubjectDn(String id) {
        X509Certificate cert = null;
        try {
            String dn = id;
            if (rootDN != null && !rootDN.isEmpty()) {
                dn = dn + "," + rootDN;
            }
            cert = getCertificateForDn(dn);
        } catch (NamingException e) {
             // Not found
        }
        if (cert == null) {
            // Try to find certificate by search for uid attribute
            try {
                cert = getCertificateForUIDAttr(id);
            } catch (NamingException e) {
                // Not found
            }
        }
        return cert;
    }

    @Override
    public X509Certificate findByServiceName(String serviceName) {
        X509Certificate cert = null;
        try {
            String dn = getDnForIdentifier(serviceName);
            cert = getCertificateForDn(dn);
        } catch (NamingException e) {
            // Not found
        }
        if (cert == null) {
            // Try to find certificate by search for uid attribute
            try {
                String filter = String.format(ldapConfig.getServiceCertUIDTemplate(), serviceName);
                Attribute attr = ldapSearch.findAttribute(rootDN, filter, ldapConfig.getAttrCrtBinary());
                return getCert(attr);
            } catch (NamingException e) {
                // Not found
            }
        }
        return cert;
    }

    @Override
    public X509Certificate findByEndpoint(String endpoint) {
        X509Certificate cert = null;
        String filter = String.format("(%s=%s)", ldapConfig.getAttrEndpoint(), endpoint);
        try {
            Attribute attr = ldapSearch.findAttribute(rootDN, filter, ldapConfig.getAttrCrtBinary());
            cert = getCert(attr);
        } catch (NamingException e) {
            // Not found
        }
        return cert;
    }


    protected String getDnForIdentifier(String id) {
        String escapedIdentifier = id.replaceAll("\\/", Matcher.quoteReplacement("\\/"));
        return String.format(ldapConfig.getServiceCertRDNTemplate(), escapedIdentifier) + "," + rootDN;
    }

    protected X509Certificate getCertificateForDn(String dn) throws NamingException {
        Attribute attr = ldapSearch.getAttribute(dn, ldapConfig.getAttrCrtBinary());
        return getCert(attr);
    }

    protected X509Certificate getCertificateForUIDAttr(String uid) throws NamingException {
        String filter = String.format(filterUIDTemplate, uid);
        Attribute attr = ldapSearch.findAttribute(rootDN, filter, ldapConfig.getAttrCrtBinary());
        return getCert(attr);
    }

    @Override
    public X509Certificate findByIssuerSerial(String issuer, String serial) {
        if (issuer == null || serial == null) {
            throw new IllegalArgumentException("Issuer and serial applications are expected in request");
        }
        String filter = String.format(filterIssuerSerialTemplate, issuer, serial);
        try {
            Attribute attr = ldapSearch.findAttribute(rootDN, filter, ldapConfig.getAttrCrtBinary());
            return getCert(attr);
        } catch (NamingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected X509Certificate getCert(Attribute attr) {
        if (attr == null) {
            return null;
        }
        byte[] data;
        try {
            data = (byte[]) attr.get();
        } catch (NamingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (data == null) {
            return null;
        }
        try {
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(data));
        } catch (CertificateException e) {
            throw new RuntimeException("Error deserializing certificate: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveCertificate(X509Certificate cert, UseKeyWithType key) {
        Applications application = Applications.fromUri(key.getApplication());
        final String dn;
        Map<String, String> attrs = new HashMap<>();
        if (application == Applications.PKIX) {
            dn = key.getIdentifier() + "," + rootDN;
        } else if (application == Applications.SERVICE_NAME) {
            dn = getDnForIdentifier(key.getIdentifier());
        } else if (application == Applications.SERVICE_ENDPOINT) {
            attrs.put(ldapConfig.getAttrEndpoint(), key.getIdentifier());
            dn = getDnForIdentifier(key.getIdentifier());
        } else {
            throw new IllegalArgumentException("Unsupported Application " + application);
        }
        saveCertificate(cert, dn, attrs);
    }

}
