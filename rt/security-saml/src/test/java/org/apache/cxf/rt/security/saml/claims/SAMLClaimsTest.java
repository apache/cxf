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
package org.apache.cxf.rt.security.saml.claims;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.claims.SAMLClaim;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.builder.SAML2Constants;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SAMLClaimsTest {

    @org.junit.Test
    public void testSAML2Claims() throws Exception {
        AttributeBean attributeBean = new AttributeBean();
        attributeBean.setQualifiedName(SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT);
        attributeBean.setNameFormat(SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED);
        attributeBean.addAttributeValue("employee");

        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler();
        samlCallbackHandler.setAttributes(Collections.singletonList(attributeBean));

        // Create the SAML Assertion via the CallbackHandler
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(samlCallbackHandler, samlCallback);
        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(samlCallback);

        Document doc = DOMUtils.newDocument();
        samlAssertion.toDOM(doc);

        ClaimCollection claims = SAMLUtils.getClaims(samlAssertion);
        assertEquals(claims.getDialect().toString(),
                "http://schemas.xmlsoap.org/ws/2005/05/identity");
        assertEquals(1, claims.size());

        // Check Claim values
        Claim claim = claims.get(0);
        assertEquals(claim.getClaimType(), SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT);
        assertEquals(1, claim.getValues().size());
        assertTrue(claim.getValues().contains("employee"));

        // Check SAMLClaim values
        assertTrue(claim instanceof SAMLClaim);
        assertEquals(SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT, ((SAMLClaim)claim).getName());
        assertEquals(SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED, ((SAMLClaim)claim).getNameFormat());

        // Check roles
        Set<Principal> roles =
                SAMLUtils.parseRolesFromClaims(claims,
                        SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT,
                        SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED);
        assertEquals(1, roles.size());
        Principal p = roles.iterator().next();
        assertEquals("employee", p.getName());
    }

    @org.junit.Test
    public void testSAML2MultipleRoles() throws Exception {
        AttributeBean attributeBean = new AttributeBean();
        attributeBean.setQualifiedName(SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT);
        attributeBean.setNameFormat(SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED);
        attributeBean.addAttributeValue("employee");
        attributeBean.addAttributeValue("boss");

        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler();
        samlCallbackHandler.setAttributes(Collections.singletonList(attributeBean));

        // Create the SAML Assertion via the CallbackHandler
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(samlCallbackHandler, samlCallback);
        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(samlCallback);

        Document doc = DOMUtils.newDocument();
        samlAssertion.toDOM(doc);

        ClaimCollection claims = SAMLUtils.getClaims(samlAssertion);
        assertEquals(claims.getDialect().toString(),
                "http://schemas.xmlsoap.org/ws/2005/05/identity");
        assertEquals(1, claims.size());

        // Check Claim values
        Claim claim = claims.get(0);
        assertEquals(claim.getClaimType(), SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT);
        assertEquals(2, claim.getValues().size());
        assertTrue(claim.getValues().contains("employee"));
        assertTrue(claim.getValues().contains("boss"));

        // Check SAMLClaim values
        assertTrue(claim instanceof SAMLClaim);
        assertEquals(SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT, ((SAMLClaim)claim).getName());
        assertEquals(SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED, ((SAMLClaim)claim).getNameFormat());

        // Check roles
        Set<Principal> roles =
                SAMLUtils.parseRolesFromClaims(claims,
                        SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT,
                        SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED);
        assertEquals(2, roles.size());
    }

    @org.junit.Test
    public void testSAML2MultipleClaims() throws Exception {
        AttributeBean attributeBean = new AttributeBean();
        attributeBean.setQualifiedName(SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT);
        attributeBean.setNameFormat(SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED);
        attributeBean.addAttributeValue("employee");

        AttributeBean attributeBean2 = new AttributeBean();
        attributeBean2.setQualifiedName(
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname");
        attributeBean2.setNameFormat(SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED);
        attributeBean2.addAttributeValue("smith");

        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler();
        List<AttributeBean> attributes = new ArrayList<>();
        attributes.add(attributeBean);
        attributes.add(attributeBean2);
        samlCallbackHandler.setAttributes(attributes);

        // Create the SAML Assertion via the CallbackHandler
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(samlCallbackHandler, samlCallback);
        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(samlCallback);

        Document doc = DOMUtils.newDocument();
        samlAssertion.toDOM(doc);

        ClaimCollection claims = SAMLUtils.getClaims(samlAssertion);
        assertEquals(claims.getDialect().toString(),
                "http://schemas.xmlsoap.org/ws/2005/05/identity");
        assertEquals(2, claims.size());

        // Check roles
        Set<Principal> roles =
                SAMLUtils.parseRolesFromClaims(claims,
                        SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT,
                        SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED);
        assertEquals(1, roles.size());
        Principal p = roles.iterator().next();
        assertEquals("employee", p.getName());
    }

    @org.junit.Test
    public void testSAML1Claims() throws Exception {
        AttributeBean attributeBean = new AttributeBean();
        attributeBean.setSimpleName("role");
        attributeBean.setQualifiedName("http://schemas.xmlsoap.org/ws/2005/05/identity/claims");
        attributeBean.addAttributeValue("employee");

        SamlCallbackHandler samlCallbackHandler = new SamlCallbackHandler(false);
        samlCallbackHandler.setAttributes(Collections.singletonList(attributeBean));

        // Create the SAML Assertion via the CallbackHandler
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(samlCallbackHandler, samlCallback);
        SamlAssertionWrapper samlAssertion = new SamlAssertionWrapper(samlCallback);

        Document doc = DOMUtils.newDocument();
        samlAssertion.toDOM(doc);

        ClaimCollection claims = SAMLUtils.getClaims(samlAssertion);
        assertEquals(claims.getDialect().toString(),
                "http://schemas.xmlsoap.org/ws/2005/05/identity");
        assertEquals(1, claims.size());

        // Check Claim values
        Claim claim = claims.get(0);
        assertEquals(claim.getClaimType(), SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT);
        assertEquals(1, claim.getValues().size());
        assertTrue(claim.getValues().contains("employee"));

        // Check SAMLClaim values
        assertTrue(claim instanceof SAMLClaim);
        assertEquals("role", ((SAMLClaim)claim).getName());

        // Check roles
        Set<Principal> roles = SAMLUtils.parseRolesFromClaims(claims, "role", null);
        assertEquals(1, roles.size());
        Principal p = roles.iterator().next();
        assertEquals("employee", p.getName());

    }
}