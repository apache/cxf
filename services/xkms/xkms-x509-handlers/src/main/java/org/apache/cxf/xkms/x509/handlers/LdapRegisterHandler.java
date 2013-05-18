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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.apache.cxf.xkms.exception.XKMSArgumentNotMatchException;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;

public class LdapRegisterHandler extends AbstractX509RegisterHandler {

    private static final String OU_SERVICES = "ou=services";
    private static final String CN_PREFIX = "cn=";
    private static final String INET_ORG_PERSON = "inetOrgPerson";
    private static final String ATTR_OBJECT_CLASS = "objectClass";
    private static final String ATTR_SN = "sn";
    private static final String ATTR_UID_NAME = "uid";
    private static final String ATTR_ISSUER_IDENTIFIER = "manager";
    private static final String ATTR_SERIAL_NUMBER = "employeeNumber";
    private static final String ATTR_USER_CERTIFICATE_BINARY = "userCertificate;binary";

    private final LDAPSearch ldapSearch;
    private final String rootDN;

    public LdapRegisterHandler(LDAPSearch ldapSearch, String rootDN) throws CertificateException {
        super();
        this.ldapSearch = ldapSearch;
        this.rootDN = rootDN;
    }

    @Override
    public void saveCertificate(X509Certificate cert, UseKeyWithType id) {
        Attributes attribs = new BasicAttributes();
        attribs.put(new BasicAttribute(ATTR_OBJECT_CLASS, INET_ORG_PERSON));
        attribs.put(new BasicAttribute(ATTR_SN, "X509 certificate"));
        attribs.put(new BasicAttribute(ATTR_UID_NAME, cert.getSubjectX500Principal().getName()));
        attribs.put(new BasicAttribute(ATTR_SERIAL_NUMBER, cert.getSerialNumber().toString(16)));
        attribs.put(new BasicAttribute(ATTR_ISSUER_IDENTIFIER, cert.getIssuerX500Principal().getName()));
        try {
            attribs.put(new BasicAttribute(ATTR_USER_CERTIFICATE_BINARY, cert.getEncoded()));
            String dn = getDN(id.getApplication(), id.getIdentifier());
            ldapSearch.bind(dn, attribs);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
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
}
