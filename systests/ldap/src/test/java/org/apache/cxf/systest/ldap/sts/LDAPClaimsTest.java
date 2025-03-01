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

package org.apache.cxf.systest.ldap.sts;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimTypes;
import org.apache.cxf.sts.claims.ClaimsManager;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.LdapClaimsHandler;
import org.apache.cxf.sts.claims.LdapGroupClaimsHandler;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.zapodot.junit.ldap.EmbeddedLdapRule;
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

/**
 * Test the LdapClaimsHandler that ships with the STS
 */
public class LDAPClaimsTest {
    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder
        .newInstance()
        .bindingToAddress("localhost")
        .usingBindCredentials("ldap_su")
        .usingBindDSN("UID=admin,DC=example,DC=com")
        .usingDomainDsn("dc=example,dc=com")
        .importingLdifs("ldap.ldif")
        .build();

    private static Properties props;
    private static boolean portUpdated;

    private ClassPathXmlApplicationContext appContext;

    @BeforeClass
    public static void startServers() throws Exception {
        props = new Properties();

        try (InputStream is = LDAPClaimsTest.class.getResourceAsStream("/ldap.properties")) {
            props.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before
    public void updatePort() throws Exception {
        if (!portUpdated) {
            String basedir = System.getProperty("basedir");
            if (basedir == null) {
                basedir = new File(".").getCanonicalPath();
            }

            // Read in ldap.xml and substitute in the correct port
            Path path = FileSystems.getDefault().getPath(basedir, "/src/test/resources/ldap.xml");
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            content = content.replaceAll("portno", Integer.toString(embeddedLdapRule.embeddedServerPort()));

            Path path2 = FileSystems.getDefault().getPath(basedir, "/target/test-classes/ldapport.xml");
            Files.write(path2, content.getBytes());

            portUpdated = true;
        }

        appContext = new ClassPathXmlApplicationContext("ldapport.xml");
    }

    @org.junit.Test
    public void testRetrieveClaims() throws Exception {
        LdapClaimsHandler claimsHandler = (LdapClaimsHandler)appContext.getBean("testClaimsHandler");
        ClaimsManager claimsManager = new ClaimsManager();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));

        String user = props.getProperty("claimUser");
        Assert.assertNotNull(user, "Property 'claimUser' not configured");

        ClaimCollection requestedClaims = createRequestClaimCollection();

        List<String> expectedClaims = new ArrayList<>();
        expectedClaims.add(ClaimTypes.FIRSTNAME.toString());
        expectedClaims.add(ClaimTypes.LASTNAME.toString());
        expectedClaims.add(ClaimTypes.EMAILADDRESS.toString());

        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(new CustomTokenPrincipal(user));
        ProcessedClaimCollection retrievedClaims =
            claimsManager.retrieveClaimValues(requestedClaims, params);

        Assert.assertTrue(
                      "Retrieved number of claims [" + retrievedClaims.size()
                      + "] doesn't match with expected [" + expectedClaims.size() + "]",
                      retrievedClaims.size() == expectedClaims.size()
        );

        for (ProcessedClaim c : retrievedClaims) {
            if (expectedClaims.contains(c.getClaimType())) {
                expectedClaims.remove(c.getClaimType());
            } else {
                Assert.fail("Claim '" + c.getClaimType() + "' not requested");
            }
        }
    }

    @org.junit.Test
    public void testRetrieveClaimsUsingLDAPLookup() throws Exception {
        LdapClaimsHandler claimsHandler = (LdapClaimsHandler)appContext.getBean("testClaimsHandler");
        ClaimsManager claimsManager = new ClaimsManager();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));

        ClaimCollection requestedClaims = createRequestClaimCollection();

        List<String> expectedClaims = new ArrayList<>();
        expectedClaims.add(ClaimTypes.FIRSTNAME.toString());
        expectedClaims.add(ClaimTypes.LASTNAME.toString());
        expectedClaims.add(ClaimTypes.EMAILADDRESS.toString());

        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(new CustomTokenPrincipal("cn=alice,ou=users,dc=example,dc=com"));
        ProcessedClaimCollection retrievedClaims =
            claimsManager.retrieveClaimValues(requestedClaims, params);

        Assert.assertTrue(
                      "Retrieved number of claims [" + retrievedClaims.size()
                      + "] doesn't match with expected [" + expectedClaims.size() + "]",
                      retrievedClaims.size() == expectedClaims.size()
        );

        for (ProcessedClaim c : retrievedClaims) {
            if (expectedClaims.contains(c.getClaimType())) {
                expectedClaims.remove(c.getClaimType());
            } else {
                Assert.fail("Claim '" + c.getClaimType() + "' not requested");
            }
        }
    }

    @org.junit.Test
    public void testMultiUserBaseDNs() throws Exception {
        LdapClaimsHandler claimsHandler = (LdapClaimsHandler)appContext.getBean("testClaimsHandlerMultipleUserBaseDNs");
        ClaimsManager claimsManager = new ClaimsManager();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));

        String user = props.getProperty("claimUser");
        Assert.assertNotNull(user, "Property 'claimUser' not configured");
        String otherUser = props.getProperty("otherClaimUser");
        Assert.assertNotNull(otherUser, "Property 'otherClaimUser' not configured");

        ClaimCollection requestedClaims = createRequestClaimCollection();

        List<String> expectedClaims = new ArrayList<>();
        expectedClaims.add(ClaimTypes.FIRSTNAME.toString());
        expectedClaims.add(ClaimTypes.LASTNAME.toString());
        expectedClaims.add(ClaimTypes.EMAILADDRESS.toString());

        // First user
        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(new CustomTokenPrincipal(user));
        ProcessedClaimCollection retrievedClaims =
            claimsManager.retrieveClaimValues(requestedClaims, params);

        Assert.assertTrue(
                      "Retrieved number of claims [" + retrievedClaims.size()
                      + "] doesn't match with expected [" + expectedClaims.size() + "]",
                      retrievedClaims.size() == expectedClaims.size()
        );

        for (ProcessedClaim c : retrievedClaims) {
            if (expectedClaims.contains(c.getClaimType())) {
                expectedClaims.remove(c.getClaimType());
            } else {
                Assert.fail("Claim '" + c.getClaimType() + "' not requested");
            }
        }

        // Second user
        params.setPrincipal(new CustomTokenPrincipal(otherUser));
        retrievedClaims = claimsManager.retrieveClaimValues(requestedClaims, params);

        expectedClaims.add(ClaimTypes.FIRSTNAME.toString());
        expectedClaims.add(ClaimTypes.LASTNAME.toString());
        expectedClaims.add(ClaimTypes.EMAILADDRESS.toString());

        Assert.assertTrue(
                      "Retrieved number of claims [" + retrievedClaims.size()
                      + "] doesn't match with expected [" + expectedClaims.size() + "]",
                      retrievedClaims.size() == expectedClaims.size()
        );

        for (ProcessedClaim c : retrievedClaims) {
            if (expectedClaims.contains(c.getClaimType())) {
                expectedClaims.remove(c.getClaimType());
            } else {
                Assert.fail("Claim '" + c.getClaimType() + "' not requested");
            }
        }
    }

    @org.junit.Test(expected = STSException.class)
    public void testRetrieveClaimsWithUnsupportedMandatoryClaimType() throws Exception {
        LdapClaimsHandler claimsHandler = (LdapClaimsHandler)appContext.getBean("testClaimsHandler");
        ClaimsManager claimsManager = new ClaimsManager();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));

        String user = props.getProperty("claimUser");
        Assert.assertNotNull(user, "Property 'claimUser' not configured");

        ClaimCollection requestedClaims = createRequestClaimCollection();
        // add unsupported but mandatory claim
        Claim claim = new Claim();
        claim.setClaimType(ClaimTypes.GENDER);
        claim.setOptional(false);
        requestedClaims.add(claim);

        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(new CustomTokenPrincipal(user));
        claimsManager.retrieveClaimValues(requestedClaims, params);
    }

    @org.junit.Test
    public void testRetrieveClaimsWithUnsupportedOptionalClaimType() throws Exception {
        LdapClaimsHandler claimsHandler = (LdapClaimsHandler)appContext.getBean("testClaimsHandler");
        ClaimsManager claimsManager = new ClaimsManager();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));

        String user = props.getProperty("claimUser");
        Assert.assertNotNull(user, "Property 'claimUser' not configured");

        ClaimCollection requestedClaims = createRequestClaimCollection();
        // add unsupported but optional unsupported claim
        Claim claim = new Claim();
        claim.setClaimType(ClaimTypes.GENDER);
        claim.setOptional(true);
        requestedClaims.add(claim);

        // Gender is not expected to be returned because not supported
        List<String> expectedClaims = new ArrayList<>();
        expectedClaims.add(ClaimTypes.FIRSTNAME.toString());
        expectedClaims.add(ClaimTypes.LASTNAME.toString());
        expectedClaims.add(ClaimTypes.EMAILADDRESS.toString());

        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(new CustomTokenPrincipal(user));
        ProcessedClaimCollection retrievedClaims =
            claimsManager.retrieveClaimValues(requestedClaims, params);

        Assert.assertTrue(
                      "Retrieved number of claims [" + retrievedClaims.size()
                      + "] doesn't match with expected [" + expectedClaims.size() + "]",
                      retrievedClaims.size() == expectedClaims.size()
        );

        for (ProcessedClaim c : retrievedClaims) {
            if (expectedClaims.contains(c.getClaimType())) {
                expectedClaims.remove(c.getClaimType());
            } else {
                Assert.fail("Claim '" + c.getClaimType() + "' not requested");
            }
        }
    }

    @org.junit.Test
    public void testSupportedClaims() throws Exception {

        Map<String, String> mapping
            = CastUtils.cast((Map<?, ?>)appContext.getBean("claimsToLdapAttributeMapping"));

        LdapClaimsHandler cHandler = new LdapClaimsHandler();
        cHandler.setClaimsLdapAttributeMapping(mapping);

        List<String> supportedClaims = cHandler.getSupportedClaimTypes();

        Assert.assertTrue(
                      "Supported claims and claims/ldap attribute mapping size different",
                      mapping.size() == supportedClaims.size()
        );

        for (String claim : mapping.keySet()) {
            Assert.assertTrue(
                          "Claim '" + claim + "' not listed in supported list",
                          supportedClaims.contains(claim)
            );
        }
    }

    @org.junit.Test
    public void testRetrieveBinaryClaims() throws Exception {
        LdapClaimsHandler claimsHandler = (LdapClaimsHandler)appContext.getBean("testClaimsHandler");
        ClaimsManager claimsManager = new ClaimsManager();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));

        String user = props.getProperty("binaryClaimUser");
        Assert.assertNotNull(user, "Property 'binaryClaimUser' not configured");

        ClaimCollection requestedClaims = createRequestClaimCollection();
        // Ask for the (binary) cert as well
        Claim claim = new Claim();
        claim.setClaimType("http://custom/x509");
        claim.setOptional(true);
        requestedClaims.add(claim);

        List<String> expectedClaims = new ArrayList<>();
        expectedClaims.add(ClaimTypes.FIRSTNAME.toString());
        expectedClaims.add(ClaimTypes.LASTNAME.toString());
        expectedClaims.add(ClaimTypes.EMAILADDRESS.toString());
        expectedClaims.add("http://custom/x509");

        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(new CustomTokenPrincipal(user));
        ProcessedClaimCollection retrievedClaims =
            claimsManager.retrieveClaimValues(requestedClaims, params);

        Assert.assertTrue(
                      "Retrieved number of claims [" + retrievedClaims.size()
                      + "] doesn't match with expected [" + expectedClaims.size() + "]",
                      retrievedClaims.size() == expectedClaims.size()
        );

        boolean foundCert = false;
        for (ProcessedClaim c : retrievedClaims) {
            if ("http://custom/x509".equals(c.getClaimType())) {
                foundCert = true;
                Assert.assertTrue(c.getValues().get(0) instanceof byte[]);
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                InputStream in = new ByteArrayInputStream((byte[])c.getValues().get(0));
                X509Certificate cert = (X509Certificate)certFactory.generateCertificate(in);
                Assert.assertNotNull(cert);
            }
        }

        Assert.assertTrue(foundCert);
    }

    @org.junit.Test
    public void testRetrieveRolesForAlice() throws Exception {
        LdapGroupClaimsHandler claimsHandler =
            (LdapGroupClaimsHandler)appContext.getBean("testGroupClaimsHandler");
        ClaimsManager claimsManager = new ClaimsManager();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));

        String user = props.getProperty("claimUser");
        Assert.assertNotNull(user, "Property 'claimUser' not configured");

        ClaimCollection requestedClaims = new ClaimCollection();
        Claim claim = new Claim();
        String roleURI = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
        claim.setClaimType(roleURI);
        requestedClaims.add(claim);

        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(new CustomTokenPrincipal(user));
        ProcessedClaimCollection retrievedClaims =
            claimsManager.retrieveClaimValues(requestedClaims, params);

        Assert.assertTrue(retrievedClaims.size() == 1);
        Assert.assertEquals(retrievedClaims.get(0).getClaimType(), roleURI);
        Assert.assertTrue(retrievedClaims.get(0).getValues().size() == 2);
    }

    @org.junit.Test
    public void testRetrieveRolesForAliceUsingLDAPLookup() throws Exception {
        LdapGroupClaimsHandler claimsHandler =
            (LdapGroupClaimsHandler)appContext.getBean("testGroupClaimsHandler");
        ClaimsManager claimsManager = new ClaimsManager();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));

        ClaimCollection requestedClaims = new ClaimCollection();
        Claim claim = new Claim();
        String roleURI = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
        claim.setClaimType(roleURI);
        requestedClaims.add(claim);

        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(new CustomTokenPrincipal("cn=alice,ou=users,dc=example,dc=com"));
        ProcessedClaimCollection retrievedClaims =
            claimsManager.retrieveClaimValues(requestedClaims, params);

        Assert.assertTrue(retrievedClaims.size() == 1);
        Assert.assertEquals(retrievedClaims.get(0).getClaimType(), roleURI);
        Assert.assertTrue(retrievedClaims.get(0).getValues().size() == 2);
    }

    @org.junit.Test
    public void testRetrieveRolesForBob() throws Exception {
        LdapGroupClaimsHandler claimsHandler =
            (LdapGroupClaimsHandler)appContext.getBean("testGroupClaimsHandlerOtherUsers");
        ClaimsManager claimsManager = new ClaimsManager();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));

        String user = props.getProperty("otherClaimUser");
        Assert.assertNotNull(user, "Property 'claimUser' not configured");

        ClaimCollection requestedClaims = new ClaimCollection();
        Claim claim = new Claim();
        String roleURI = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
        claim.setClaimType(roleURI);
        requestedClaims.add(claim);

        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(new CustomTokenPrincipal(user));
        ProcessedClaimCollection retrievedClaims =
            claimsManager.retrieveClaimValues(requestedClaims, params);

        Assert.assertTrue(retrievedClaims.size() == 1);
        Assert.assertEquals(retrievedClaims.get(0).getClaimType(), roleURI);
        Assert.assertTrue(retrievedClaims.get(0).getValues().size() == 2);
    }

    @org.junit.Test
    public void testRetrieveRolesForBobInBusinessCategoryWidgets() throws Exception {
        LdapGroupClaimsHandler claimsHandler =
            (LdapGroupClaimsHandler)appContext.getBean("testGroupClaimsHandlerFilter");
        ClaimsManager claimsManager = new ClaimsManager();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));

        String user = props.getProperty("otherClaimUser");
        Assert.assertNotNull(user, "Property 'claimUser' not configured");

        ClaimCollection requestedClaims = new ClaimCollection();
        Claim claim = new Claim();
        String roleURI = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
        claim.setClaimType(roleURI);
        requestedClaims.add(claim);

        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(new CustomTokenPrincipal(user));
        ProcessedClaimCollection retrievedClaims =
            claimsManager.retrieveClaimValues(requestedClaims, params);

        Assert.assertTrue(retrievedClaims.size() == 1);
        Assert.assertEquals(retrievedClaims.get(0).getClaimType(), roleURI);
        Assert.assertTrue(retrievedClaims.get(0).getValues().size() == 1);
    }

    private ClaimCollection createRequestClaimCollection() {
        ClaimCollection claims = new ClaimCollection();
        Claim claim = new Claim();
        claim.setClaimType(ClaimTypes.FIRSTNAME);
        claim.setOptional(true);
        claims.add(claim);
        claim = new Claim();
        claim.setClaimType(ClaimTypes.LASTNAME);
        claim.setOptional(true);
        claims.add(claim);
        claim = new Claim();
        claim.setClaimType(ClaimTypes.EMAILADDRESS);
        claim.setOptional(true);
        claims.add(claim);
        return claims;
    }

}
