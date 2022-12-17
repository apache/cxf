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

package org.apache.cxf.systest.ldap.xkms;

import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.naming.NamingException;

import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.x509.repo.CertificateRepo;
import org.apache.cxf.xkms.x509.repo.ldap.LdapCertificateRepo;
import org.apache.cxf.xkms.x509.repo.ldap.LdapSchemaConfig;
import org.apache.cxf.xkms.x509.repo.ldap.LdapSearch;
import org.zapodot.junit.ldap.EmbeddedLdapRule;
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Add a test for the XKMS LDAP CertificateRepo
 */
public class LDAPCertificateRepoTest {
    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder
        .newInstance()
        .bindingToAddress("localhost")
        .usingBindCredentials("ldap_su")
        .usingBindDSN("UID=admin,DC=example,DC=com")
        .usingDomainDsn("dc=example,dc=com")
        .importingLdifs("ldap.ldif")
        .build();

    private static final String EXPECTED_SUBJECT_DN = "cn=dave,ou=users";
    private static final String ROOT_DN = "dc=example,dc=com";
    private static final String EXPECTED_SUBJECT_DN2 = "cn=newuser,ou=users";
    private static final String EXPECTED_SERVICE_URI = "http://myservice.apache.org/MyServiceName";

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        AbstractClientServerTestBase.stopAllServers();
    }

    @Test
    public void testFindUserCert() throws URISyntaxException, NamingException, CertificateException {
        CertificateRepo persistenceManager = createLdapCertificateRepo();
        X509Certificate cert = persistenceManager.findBySubjectDn(EXPECTED_SUBJECT_DN);
        assertNotNull(cert);
    }

    @Test
    public void testFindUserCertForNonExistentDn() throws URISyntaxException, NamingException, CertificateException {
        CertificateRepo persistenceManager = createLdapCertificateRepo();
        X509Certificate cert = persistenceManager.findBySubjectDn("CN=wrong");
        assertNull("Certificate should be null", cert);
    }

    @Test
    public void testFindUserCertViaUID() throws URISyntaxException, NamingException, CertificateException {
        CertificateRepo persistenceManager = createLdapCertificateRepo();
        X509Certificate cert = persistenceManager.findBySubjectDn("dave");
        assertNotNull(cert);
    }

    @Test
    public void testFindUserCertViaWrongUID() throws URISyntaxException, NamingException, CertificateException {
        CertificateRepo persistenceManager = createLdapCertificateRepo();
        X509Certificate cert = persistenceManager.findBySubjectDn("wrong");
        assertNull("Certificate should be null", cert);
    }

    @Test
    public void testSave() throws Exception {
        CertificateRepo persistenceManager = createLdapCertificateRepo();
        URL url = this.getClass().getResource("cert1.cer");
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(url.openStream());
        assertNotNull(cert);

        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.PKIX.getUri());
        key.setIdentifier(EXPECTED_SUBJECT_DN2);
        persistenceManager.saveCertificate(cert, key);

        X509Certificate foundCert = persistenceManager.findBySubjectDn(EXPECTED_SUBJECT_DN2);
        assertNotNull(foundCert);
    }

    @Test
    public void testSaveServiceCert() throws Exception {
        CertificateRepo persistenceManager = createLdapCertificateRepo();
        URL url = this.getClass().getResource("cert1.cer");
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(url.openStream());
        assertNotNull(cert);

        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.SERVICE_NAME.getUri());
        key.setIdentifier(EXPECTED_SERVICE_URI);
        persistenceManager.saveCertificate(cert, key);

        // Search by DN
        X509Certificate foundCert = persistenceManager.findByServiceName(EXPECTED_SERVICE_URI);
        assertNotNull(foundCert);

        // Search by UID
        foundCert = persistenceManager.findByServiceName(cert.getSubjectX500Principal().getName());
        assertNotNull(foundCert);
    }

    private CertificateRepo createLdapCertificateRepo() throws CertificateException {
        LdapSearch ldapSearch = new LdapSearch("ldap://localhost:" + embeddedLdapRule.embeddedServerPort(),
            "UID=admin,DC=example,DC=com", "ldap_su", 2);

        LdapSchemaConfig ldapSchemaConfig = new LdapSchemaConfig();
        ldapSchemaConfig.setAttrCrtBinary("userCertificate");
        return new LdapCertificateRepo(ldapSearch, ldapSchemaConfig, ROOT_DN);
    }

}
