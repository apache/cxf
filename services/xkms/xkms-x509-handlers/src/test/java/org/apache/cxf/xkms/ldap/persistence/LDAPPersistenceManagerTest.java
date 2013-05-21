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
import java.io.FileNotFoundException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.naming.directory.Attributes;

import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.x509.handlers.LdapRegisterHandler;
import org.apache.cxf.xkms.x509.handlers.LdapSchemaConfig;
import org.apache.cxf.xkms.x509.handlers.LdapSearch;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.Assert;
import org.junit.Test;

public class LDAPPersistenceManagerTest {
    private static final String ROOT_DN = "dc=example,dc=com";
    private static final String EXPECTED_SUBJECT_DN = "CN=www.issuer.com,L=CGN,ST=NRW,C=DE,O=Issuer";
    private static final String EXPECTED_SERVICE_URI = "http://myservice.apache.org/MyServiceName";
    private static final String EXPECTED_DN_FOR_SERVICE =
            "cn=http:\\/\\/myservice.apache.org\\/MyServiceName,ou=services";
    private static final LdapSchemaConfig LDAP_CERT_CONFIG = new LdapSchemaConfig();

    @Test
    public void testSaveUserCert() throws Exception {
        IMocksControl c = EasyMock.createControl();
        LdapSearch ldapSearch = c.createMock(LdapSearch.class);
        ldapSearch.bind(EasyMock.eq(EXPECTED_SUBJECT_DN + "," + ROOT_DN), EasyMock.anyObject(Attributes.class));
        EasyMock.expectLastCall().once();
        LdapRegisterHandler persistenceManager = new LdapRegisterHandler(ldapSearch, LDAP_CERT_CONFIG, ROOT_DN);
        X509Certificate cert = getTestCert();

        c.replay();
        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.PKIX.getUri());
        key.setIdentifier(EXPECTED_SUBJECT_DN);
        persistenceManager.saveCertificate(cert, key);
        c.verify();
    }

    @Test
    public void testSaveServiceCert() throws Exception {
        IMocksControl c = EasyMock.createControl();
        LdapSearch ldapSearch = c.createMock(LdapSearch.class);
        ldapSearch.bind(EasyMock.eq(EXPECTED_DN_FOR_SERVICE + "," + ROOT_DN), EasyMock.anyObject(Attributes.class));
        EasyMock.expectLastCall().once();
        LdapRegisterHandler persistenceManager = new LdapRegisterHandler(ldapSearch, LDAP_CERT_CONFIG, ROOT_DN);
        X509Certificate cert = getTestCert();

        c.replay();
        UseKeyWithType key = new UseKeyWithType();
        key.setApplication(Applications.SERVICE_SOAP.getUri());
        key.setIdentifier(EXPECTED_SERVICE_URI);
        persistenceManager.saveCertificate(cert, key);
        c.verify();
    }

    private X509Certificate getTestCert() throws FileNotFoundException, CertificateException {
        File certFile = new File("src/test/resources/cert1.cer");
        Assert.assertTrue(certFile.exists());
        FileInputStream fis = new FileInputStream(certFile);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(fis);
    }

}
