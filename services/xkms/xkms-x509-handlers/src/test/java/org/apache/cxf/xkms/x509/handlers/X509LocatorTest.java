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
import java.util.UUID;

import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.handlers.XKMSConstants;
import org.apache.cxf.xkms.model.xkms.LocateRequestType;
import org.apache.cxf.xkms.model.xkms.MessageAbstractType;
import org.apache.cxf.xkms.model.xkms.QueryKeyBindingType;
import org.apache.cxf.xkms.model.xkms.UnverifiedKeyBindingType;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.x509.repo.CertificateRepo;
import org.apache.cxf.xkms.x509.repo.ldap.LdapCertificateRepo;
import org.apache.cxf.xkms.x509.repo.ldap.LdapSchemaConfig;
import org.apache.cxf.xkms.x509.repo.ldap.LdapSearch;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test needs a real LDAP server.
 */
public class X509LocatorTest {

    private static final org.apache.cxf.xkms.model.xkms.ObjectFactory XKMS_OF = 
        new org.apache.cxf.xkms.model.xkms.ObjectFactory();

    CertificateRepo certRepo;
    X509Locator locator;

    @Ignore
    @Before
    public void setUpLdapRepo() throws CertificateException {
        LdapSearch ldapSearch = new LdapSearch("ldap://localhost:2389", 
            "cn=Directory Manager,dc=example,dc=com", "test", 2);
        LdapSchemaConfig ldapConfig = new LdapSchemaConfig();
        ldapConfig.setAttrCrtBinary("userCertificate;binary");
        ldapConfig.setAttrIssuerID("manager");
        ldapConfig.setAttrSerialNumber("employeeNumber");
        ldapConfig.setAttrUID("uid");
        ldapConfig.setCertObjectClass("inetOrgPerson");
        ldapConfig.setConstAttrNamesCSV("sn");
        ldapConfig.setConstAttrValuesCSV("X509 certificate");
        ldapConfig.setIntermediateFilter("(objectClass=*)");
        ldapConfig.setServiceCertRDNTemplate("cn=%s,ou=services");
        ldapConfig.setServiceCertUIDTemplate("cn=%s");
        ldapConfig.setTrustedAuthorityFilter("(&(objectClass=inetOrgPerson)(ou:dn:=CAs))");
        certRepo = new LdapCertificateRepo(ldapSearch, ldapConfig, "dc=example,dc=com");
        locator = new X509Locator(certRepo);
    }

    @Ignore
    @Test
    public void locate() {
        LocateRequestType request = prepareLocateXKMSRequest();
        UnverifiedKeyBindingType result = locator.locate(request);
        Assert.assertNotNull(result.getKeyInfo());
    }

    private LocateRequestType prepareLocateXKMSRequest() {
        QueryKeyBindingType queryKeyBindingType = XKMS_OF.createQueryKeyBindingType();

        UseKeyWithType useKeyWithType = XKMS_OF.createUseKeyWithType();
        useKeyWithType.setIdentifier("alice");
        useKeyWithType.setApplication(Applications.PKIX.getUri());

        queryKeyBindingType.getUseKeyWith().add(useKeyWithType);

        LocateRequestType locateRequestType = XKMS_OF.createLocateRequestType();
        locateRequestType.setQueryKeyBinding(queryKeyBindingType);
        setGenericRequestParams(locateRequestType);
        return locateRequestType;
    }

    private void setGenericRequestParams(MessageAbstractType request) {
        request.setService(XKMSConstants.XKMS_ENDPOINT_NAME);
        request.setId(UUID.randomUUID().toString());
    }

}
