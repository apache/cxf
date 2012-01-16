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

package org.apache.cxf.sts.ldap;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.sts.claims.Claim;
import org.apache.cxf.sts.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimTypes;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.LdapClaimsHandler;
import org.apache.cxf.sts.claims.RequestClaim;
import org.apache.cxf.sts.claims.RequestClaimCollection;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.ws.security.CustomTokenPrincipal;
import org.junit.BeforeClass;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.util.Assert;

public class LDAPClaimsTest {

    private static ClassPathXmlApplicationContext appContext;
    private static Properties props;

    @BeforeClass
    public static void setUpLdap() throws Exception {
        appContext = new ClassPathXmlApplicationContext("ldap.xml");
        props = new Properties();

        InputStream is = null;
        try {
            is = LDAPClaimsTest.class.getResourceAsStream("/ldap.properties");
            props.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }


    @org.junit.Test
    @org.junit.Ignore
    public void testRetrieveClaims() throws Exception {
        LdapClaimsHandler claimsHandler = (LdapClaimsHandler)appContext.getBean("testClaimsHandler");

        String user = props.getProperty("claimUser");
        Assert.notNull(user, "Property 'claimUser' not configured");

        RequestClaimCollection requestedClaims = createRequestClaimCollection();

        List<URI> expectedClaims = new ArrayList<URI>();
        expectedClaims.add(ClaimTypes.FIRSTNAME);
        expectedClaims.add(ClaimTypes.LASTNAME);
        expectedClaims.add(ClaimTypes.EMAILADDRESS);
       
        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(new CustomTokenPrincipal(user));
        ClaimCollection retrievedClaims = 
            claimsHandler.retrieveClaimValues(requestedClaims, params);

        Assert.isTrue(
                      retrievedClaims.size() == expectedClaims.size(), 
                      "Retrieved number of claims [" + retrievedClaims.size() 
                      + "] doesn't match with expected [" + expectedClaims.size() + "]"
        );

        for (Claim c : retrievedClaims) {
            if (expectedClaims.contains(c.getClaimType())) {
                expectedClaims.remove(c.getClaimType());
            } else {
                Assert.isTrue(false, "Claim '" + c.getClaimType() + "' not requested");
            }
        }

    }


    @org.junit.Test(expected = STSException.class)
    @org.junit.Ignore
    public void testRetrieveClaimsWithUnsupportedMandatoryClaimType() throws Exception {
        LdapClaimsHandler claimsHandler = (LdapClaimsHandler)appContext.getBean("testClaimsHandler");

        String user = props.getProperty("claimUser");
        Assert.notNull(user, "Property 'claimUser' not configured");

        RequestClaimCollection requestedClaims = createRequestClaimCollection();
        // add unsupported but mandatory claim
        RequestClaim claim = new RequestClaim();
        claim.setClaimType(ClaimTypes.GENDER);
        claim.setOptional(false);
        requestedClaims.add(claim);

        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(new CustomTokenPrincipal(user));
        claimsHandler.retrieveClaimValues(requestedClaims, params);
    }

    @org.junit.Test
    @org.junit.Ignore
    public void testRetrieveClaimsWithUnsupportedOptionalClaimType() throws Exception {
        LdapClaimsHandler claimsHandler = (LdapClaimsHandler)appContext.getBean("testClaimsHandler");

        String user = props.getProperty("claimUser");
        Assert.notNull(user, "Property 'claimUser' not configured");

        RequestClaimCollection requestedClaims = createRequestClaimCollection();
        // add unsupported but optional unsupported claim
        RequestClaim claim = new RequestClaim();
        claim.setClaimType(ClaimTypes.GENDER);
        claim.setOptional(true);
        requestedClaims.add(claim);

        // Gender is not expected to be returned because not supported
        List<URI> expectedClaims = new ArrayList<URI>();
        expectedClaims.add(ClaimTypes.FIRSTNAME);
        expectedClaims.add(ClaimTypes.LASTNAME);
        expectedClaims.add(ClaimTypes.EMAILADDRESS);
        
        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(new CustomTokenPrincipal(user));
        ClaimCollection retrievedClaims = 
            claimsHandler.retrieveClaimValues(requestedClaims, params);

        Assert.isTrue(
                      retrievedClaims.size() == expectedClaims.size(), 
                      "Retrieved number of claims [" + retrievedClaims.size() 
                      + "] doesn't match with expected [" + expectedClaims.size() + "]"
        );

        for (Claim c : retrievedClaims) {
            if (expectedClaims.contains(c.getClaimType())) {
                expectedClaims.remove(c.getClaimType());
            } else {
                Assert.isTrue(false, "Claim '" + c.getClaimType() + "' not requested");
            }
        }
    }

    private RequestClaimCollection createRequestClaimCollection() {
        RequestClaimCollection claims = new RequestClaimCollection();
        RequestClaim claim = new RequestClaim();
        claim.setClaimType(ClaimTypes.FIRSTNAME);
        claim.setOptional(true);
        claims.add(claim);
        claim = new RequestClaim();
        claim.setClaimType(ClaimTypes.LASTNAME);
        claim.setOptional(true);
        claims.add(claim);
        claim = new RequestClaim();
        claim.setClaimType(ClaimTypes.EMAILADDRESS);
        claim.setOptional(true);
        claims.add(claim);
        return claims;
    }

    @org.junit.Test    
    public void testSupportedClaims() throws Exception {

        Map<String, String> mapping 
            = CastUtils.cast((Map<?, ?>)appContext.getBean("claimsToLdapAttributeMapping"));

        LdapClaimsHandler cHandler = new LdapClaimsHandler();
        cHandler.setClaimsLdapAttributeMapping(mapping);

        List<URI> supportedClaims = cHandler.getSupportedClaimTypes();

        Assert.isTrue(
                      mapping.size() == supportedClaims.size(), 
                      "Supported claims and claims/ldap attribute mapping size different"
        );

        for (String claim : mapping.keySet()) {
            Assert.isTrue(
                          supportedClaims.contains(new URI(claim)), 
                          "Claim '" + claim + "' not listed in supported list"
            );
        }
    }


    @org.junit.Test
    @org.junit.Ignore    
    public void testLdapTemplate() throws Exception {

        try {
            LdapTemplate ldap = (LdapTemplate)appContext.getBean("ldapTemplate");

            String user = props.getProperty("claimUser");
            Assert.notNull(user, "Property 'claimUser' not configured");

            String dn = null;

            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectclass", "person")).and(new EqualsFilter("cn", user));

            //find DN of user
            AttributesMapper mapper = 
                new AttributesMapper() {
                    public Object mapFromAttributes(Attributes attrs) throws NamingException {
                        return attrs.get("distinguishedName").get();
                    }
                };
            @SuppressWarnings("rawtypes")
            List users = 
                ldap.search(
                            "OU=users,DC=emea,DC=mycompany,DC=com", 
                            filter.toString(), 
                            SearchControls.SUBTREE_SCOPE,
                            mapper
                );

            Assert.isTrue(users.size() == 1, "Only one user expected");
            dn = (String)users.get(0);

            // get attributes
            AttributesMapper mapper2 = 
                new AttributesMapper() {
                    public Object mapFromAttributes(Attributes attrs) throws NamingException {
                        Map<String, String> map = new HashMap<String, String>();
                        NamingEnumeration<? extends Attribute> attrEnum = attrs.getAll();
                        while (attrEnum.hasMore()) {
                            Attribute att = attrEnum.next();
                            System.out.println(att.toString());
                        }
    
                        map.put("cn", (String)attrs.get("cn").get());
                        map.put("mail", (String)attrs.get("mail").get());
                        map.put("sn", (String)attrs.get("sn").get());
                        map.put("givenName", (String)attrs.get("givenName").get());
                        return map;
                    }
                };
            ldap.lookup(dn, new String[] {"cn", "mail", "sn", "givenName", "c"}, mapper2);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
