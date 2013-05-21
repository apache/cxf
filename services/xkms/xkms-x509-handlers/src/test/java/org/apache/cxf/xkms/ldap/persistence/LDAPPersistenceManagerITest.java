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
package org.apache.cxf.xkms.ldap.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.x509.handlers.LdapRegisterHandler;
import org.apache.cxf.xkms.x509.handlers.LdapSchemaConfig;
import org.apache.cxf.xkms.x509.handlers.LdapSearch;
import org.apache.cxf.xkms.x509.locator.LdapLocator;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests need a real ldap server
 */
public class LDAPPersistenceManagerITest {
    private static final String EXPECTED_SUBJECT_DN = "CN=www.issuer.com, L=CGN, ST=NRW, C=DE, O=Issuer";
    private static final LdapSchemaConfig LDAP_CERT_CONFIG = new LdapSchemaConfig();

    @Test
    @Ignore
    public void testFindUserCert() throws URISyntaxException, NamingException, CertificateException {
        LdapLocator persistenceManager = createLdapLocator();
        testFindBySubjectDnInternal(persistenceManager);
    }

    @Test
    @Ignore
    public void testFindUserCertForNonExistantDn() throws URISyntaxException, NamingException, CertificateException {
        LdapLocator persistenceManager = createLdapLocator();
        List<UseKeyWithType> ids = new ArrayList<UseKeyWithType>();
        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.PKIX.getUri());
        key.setIdentifier("CN=wrong");
        ids.add(key);
        X509Certificate cert = persistenceManager.findCertificate(ids);
        Assert.assertNull("Certifiacte should be null", cert);
    }

    @Test
    @Ignore
    public void testFindServiceCert() throws URISyntaxException, NamingException, CertificateException {
        LdapLocator persistenceManager = createLdapLocator();
        String serviceUri = "cn=http:\\/\\/myservice.apache.org\\/MyServiceName,ou=services";
        List<UseKeyWithType> ids = new ArrayList<UseKeyWithType>();
        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.SERVICE_SOAP.getUri());
        key.setIdentifier(serviceUri);
        ids.add(key);
        X509Certificate cert = persistenceManager.findCertificate(ids);
        Assert.assertEquals(EXPECTED_SUBJECT_DN, cert.getSubjectDN().toString());
    }

    @Test
    @Ignore
    public void testSave() throws Exception {
        LdapSearch ldapSearch = new LdapSearch("ldap://localhost:2389", "cn=Directory Manager", "test", 2);
        LdapLocator locator = createLdapLocator();
        LdapRegisterHandler persistenceManager = new LdapRegisterHandler(ldapSearch,
                                                                         LDAP_CERT_CONFIG,
                                                                         "dc=example,dc=com");
        File certFile = new File("src/test/java/cert1.cer");
        Assert.assertTrue(certFile.exists());
        FileInputStream fis = new FileInputStream(certFile);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(fis);

        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.PKIX.getUri());
        key.setIdentifier(EXPECTED_SUBJECT_DN);
        persistenceManager.saveCertificate(cert, key);
        testFindBySubjectDnInternal(locator);
    }

    private LdapLocator createLdapLocator() throws CertificateException {
        LdapSearch ldapSearch = new LdapSearch("ldap://localhost:2389", "cn=Directory Manager", "test", 2);
        return new LdapLocator(ldapSearch, LDAP_CERT_CONFIG, "dc=example,dc=com");
    }

    private void testFindBySubjectDnInternal(LdapLocator persistenceManager) {
        List<UseKeyWithType> ids = new ArrayList<UseKeyWithType>();
        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.PKIX.getUri());
        key.setIdentifier(EXPECTED_SUBJECT_DN);
        ids.add(key);
        X509Certificate cert2 = persistenceManager.findCertificate(ids);
        Assert.assertEquals(EXPECTED_SUBJECT_DN, cert2.getSubjectDN().toString());
    }
}
