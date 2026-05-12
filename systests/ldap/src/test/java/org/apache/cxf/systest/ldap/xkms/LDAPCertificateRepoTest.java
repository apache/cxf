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
import java.util.UUID;

import javax.naming.NamingException;

import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.handlers.XKMSConstants;
import org.apache.cxf.xkms.model.xkms.LocateRequestType;
import org.apache.cxf.xkms.model.xkms.QueryKeyBindingType;
import org.apache.cxf.xkms.model.xkms.UnverifiedKeyBindingType;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.x509.handlers.X509Locator;
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
    private static final String LOCATOR_SERVICE_URI = "http://x509locator.test/MyServiceName";
    private static final String LOCATOR_ENDPOINT_URI = "http://x509locator.test/endpoint";

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

    @Test
    public void testX509LocatorFindBySubjectDn() throws Exception {
        CertificateRepo persistenceManager = createLdapCertificateRepo();
        X509Locator locator = new X509Locator(persistenceManager);

        LocateRequestType request = createLocateRequest(Applications.PKIX, EXPECTED_SUBJECT_DN);
        UnverifiedKeyBindingType result = locator.locate(request);

        assertNotNull(result);
        assertNotNull(result.getKeyInfo());
    }

    @Test
    public void testX509LocatorFindBySubjectDnLDAPInjection() throws Exception {
        CertificateRepo persistenceManager = createLdapCertificateRepo();
        X509Locator locator = new X509Locator(persistenceManager);

        LocateRequestType request = createLocateRequest(Applications.PKIX, "cn=*");
        UnverifiedKeyBindingType result = locator.locate(request);

        assertNull(result);
    }

    @Test
    public void testX509LocatorFindBySubjectDnUsesEscapedDn() throws Exception {
        CapturingFindLdapCertificateRepo persistenceManager = new CapturingFindLdapCertificateRepo();
        X509Locator locator = new X509Locator(persistenceManager);

        LocateRequestType request = createLocateRequest(Applications.PKIX, "cn=bad,ou=admins");
        UnverifiedKeyBindingType result = locator.locate(request);

        assertNull(result);
        org.junit.Assert.assertEquals("cn=bad,ou=admins,dc=example,dc=com", persistenceManager.getLookedUpDn());
    }

    @Test
    public void testX509LocatorReturnsNullForUnknownSubjectDn() throws Exception {
        CertificateRepo persistenceManager = createLdapCertificateRepo();
        X509Locator locator = new X509Locator(persistenceManager);

        LocateRequestType request = createLocateRequest(Applications.PKIX, "cn=nobody,ou=users");
        UnverifiedKeyBindingType result = locator.locate(request);

        assertNull(result);
    }

    @Test
    public void testX509LocatorFindByServiceName() throws Exception {
        CertificateRepo persistenceManager = createLdapCertificateRepo();

        // Save a cert under a service name first
        URL url = this.getClass().getResource("cert1.cer");
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(url.openStream());
        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.SERVICE_NAME.getUri());
        key.setIdentifier(LOCATOR_SERVICE_URI);
        persistenceManager.saveCertificate(cert, key);

        X509Locator locator = new X509Locator(persistenceManager);
        LocateRequestType request = createLocateRequest(Applications.SERVICE_NAME, LOCATOR_SERVICE_URI);
        UnverifiedKeyBindingType result = locator.locate(request);

        assertNotNull(result);
        assertNotNull(result.getKeyInfo());
    }

    @Test
    public void testX509LocatorFindByServiceNameLDAPInjection() throws Exception {
        CertificateRepo persistenceManager = createLdapCertificateRepo();

        // Save a cert under a service name first
        URL url = this.getClass().getResource("cert1.cer");
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(url.openStream());
        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.SERVICE_NAME.getUri());
        // Ensure this test can be rerun in the same JVM without DN collisions.
        key.setIdentifier(LOCATOR_SERVICE_URI + "/" + UUID.randomUUID());
        persistenceManager.saveCertificate(cert, key);

        X509Locator locator = new X509Locator(persistenceManager);
        LocateRequestType request = createLocateRequest(Applications.SERVICE_NAME, "cn=*");
        UnverifiedKeyBindingType result = locator.locate(request);

        assertNull(result);
    }

    @Test
    public void testX509LocatorFindByEndpoint() throws Exception {
        CertificateRepo persistenceManager = createLdapCertificateRepo();

        // Save a cert with a service endpoint attribute first
        URL url = this.getClass().getResource("cert1.cer");
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(url.openStream());
        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.SERVICE_ENDPOINT.getUri());
        String identifier = LOCATOR_ENDPOINT_URI + UUID.randomUUID();
        key.setIdentifier(identifier);
        persistenceManager.saveCertificate(cert, key);

        X509Locator locator = new X509Locator(persistenceManager);
        LocateRequestType request = createLocateRequest(Applications.SERVICE_ENDPOINT, identifier);
        UnverifiedKeyBindingType result = locator.locate(request);

        assertNotNull(result);
        assertNotNull(result.getKeyInfo());
    }

    @Test
    public void testX509LocatorFindByEndpointLDAPInjection() throws Exception {
        CertificateRepo persistenceManager = createLdapCertificateRepo();

        // Save a cert with a service endpoint attribute first
        URL url = this.getClass().getResource("cert1.cer");
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(url.openStream());
        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.SERVICE_ENDPOINT.getUri());
        String identifier = LOCATOR_ENDPOINT_URI + UUID.randomUUID();
        key.setIdentifier(identifier);
        persistenceManager.saveCertificate(cert, key);

        X509Locator locator = new X509Locator(persistenceManager);
        LocateRequestType request = createLocateRequest(Applications.SERVICE_ENDPOINT, "*");
        UnverifiedKeyBindingType result = locator.locate(request);

        assertNull(result);
    }

    @Test
    public void testX509LocatorFindByIssuerSerial() throws Exception {
        CertificateRepo persistenceManager = createLdapCertificateRepo();

        // Save a cert so its issuer/serial attributes are written to LDAP
        URL url = this.getClass().getResource("cert1.cer");
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(url.openStream());
        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.PKIX.getUri());
        key.setIdentifier("cn=issuerserialtest,ou=users");
        persistenceManager.saveCertificate(cert, key);

        String issuer = cert.getIssuerX500Principal().getName();
        String serial = cert.getSerialNumber().toString(16);

        X509Locator locator = new X509Locator(persistenceManager);
        LocateRequestType request = createIssuerSerialLocateRequest(issuer, serial);
        UnverifiedKeyBindingType result = locator.locate(request);

        assertNotNull(result);
        assertNotNull(result.getKeyInfo());
    }

    @Test
    public void testX509LocatorFindByIssuerSerialLDAPInjection() throws Exception {
        CertificateRepo persistenceManager = createLdapCertificateRepo();

        // Save a cert so its issuer/serial attributes are written to LDAP
        URL url = this.getClass().getResource("cert1.cer");
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(url.openStream());
        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.PKIX.getUri());
        // Ensure this test does not collide with the non-injection issuer/serial test entry.
        key.setIdentifier("cn=issuerserialtest-" + UUID.randomUUID() + ",ou=users");
        persistenceManager.saveCertificate(cert, key);

        String serial = cert.getSerialNumber().toString(16);

        X509Locator locator = new X509Locator(persistenceManager);
        LocateRequestType request = createIssuerSerialLocateRequest("*", serial);
        UnverifiedKeyBindingType result = locator.locate(request);

        assertNull(result);
    }

    private LocateRequestType createIssuerSerialLocateRequest(String issuer, String serial) {
        org.apache.cxf.xkms.model.xkms.ObjectFactory xkmsOf =
            new org.apache.cxf.xkms.model.xkms.ObjectFactory();

        UseKeyWithType issuerKey = xkmsOf.createUseKeyWithType();
        issuerKey.setApplication(Applications.ISSUER.getUri());
        issuerKey.setIdentifier(issuer);

        UseKeyWithType serialKey = xkmsOf.createUseKeyWithType();
        serialKey.setApplication(Applications.SERIAL.getUri());
        serialKey.setIdentifier(serial);

        QueryKeyBindingType queryKeyBinding = xkmsOf.createQueryKeyBindingType();
        queryKeyBinding.getUseKeyWith().add(issuerKey);
        queryKeyBinding.getUseKeyWith().add(serialKey);

        LocateRequestType request = xkmsOf.createLocateRequestType();
        request.setQueryKeyBinding(queryKeyBinding);
        request.setService(XKMSConstants.XKMS_ENDPOINT_NAME);
        request.setId(UUID.randomUUID().toString());
        return request;
    }

    private LocateRequestType createLocateRequest(Applications application, String identifier) {
        org.apache.cxf.xkms.model.xkms.ObjectFactory xkmsOf =
            new org.apache.cxf.xkms.model.xkms.ObjectFactory();

        UseKeyWithType useKeyWithType = xkmsOf.createUseKeyWithType();
        useKeyWithType.setApplication(application.getUri());
        useKeyWithType.setIdentifier(identifier);

        QueryKeyBindingType queryKeyBinding = xkmsOf.createQueryKeyBindingType();
        queryKeyBinding.getUseKeyWith().add(useKeyWithType);

        LocateRequestType request = xkmsOf.createLocateRequestType();
        request.setQueryKeyBinding(queryKeyBinding);
        request.setService(XKMSConstants.XKMS_ENDPOINT_NAME);
        request.setId(UUID.randomUUID().toString());
        return request;
    }

    private CertificateRepo createLdapCertificateRepo() throws CertificateException {
        LdapSearch ldapSearch = new LdapSearch("ldap://localhost:" + embeddedLdapRule.embeddedServerPort(),
            "UID=admin,DC=example,DC=com", "ldap_su", 2);

        LdapSchemaConfig ldapSchemaConfig = new LdapSchemaConfig();
        ldapSchemaConfig.setAttrCrtBinary("userCertificate");
        return new LdapCertificateRepo(ldapSearch, ldapSchemaConfig, ROOT_DN);
    }

    private static class CapturingFindLdapCertificateRepo extends LdapCertificateRepo {
        private String lookedUpDn;

        CapturingFindLdapCertificateRepo() {
            super(new LdapSearch("ldap://localhost:389", "UID=admin,DC=example,DC=com", "ldap_su", 1),
                new LdapSchemaConfig(), ROOT_DN);
        }

        @Override
        protected X509Certificate getCertificateForDn(String dn) {
            this.lookedUpDn = dn;
            return null;
        }

        @Override
        protected X509Certificate getCertificateForUIDAttr(String uid) {
            return null;
        }

        String getLookedUpDn() {
            return lookedUpDn;
        }
    }

}
