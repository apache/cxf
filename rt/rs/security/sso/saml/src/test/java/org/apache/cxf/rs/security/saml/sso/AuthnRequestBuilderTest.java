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

package org.apache.cxf.rs.security.saml.sso;

import java.util.Collections;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.bean.NameIDBean;
import org.apache.wss4j.common.saml.builder.SAML2ComponentBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the SamlpRequestComponentBuilder and AuthnRequestBuilder
 */
public class AuthnRequestBuilderTest {

    static {
        OpenSAMLUtil.initSamlEngine();
    }

    @org.junit.Test
    public void testCreateAuthnRequest() throws Exception {
        Document doc = DOMUtils.createDocument();

        Issuer issuer =
            SamlpRequestComponentBuilder.createIssuer("http://localhost:9001/app");
        NameIDPolicy nameIDPolicy =
            SamlpRequestComponentBuilder.createNameIDPolicy(
                true, "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent", "Issuer"
            );

        AuthnContextClassRef authnCtxClassRef =
            SamlpRequestComponentBuilder.createAuthnCtxClassRef(
                "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport"
            );
        RequestedAuthnContext authnCtx =
            SamlpRequestComponentBuilder.createRequestedAuthnCtxPolicy(
                AuthnContextComparisonTypeEnumeration.EXACT,
                Collections.singletonList(authnCtxClassRef), null
            );

        AuthnRequest authnRequest =
            SamlpRequestComponentBuilder.createAuthnRequest(
                "http://localhost:9001/sso", false, false,
                "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST", SAMLVersion.VERSION_20,
                issuer, nameIDPolicy, authnCtx
            );

        Element policyElement = OpenSAMLUtil.toDom(authnRequest, doc);
        doc.appendChild(policyElement);
        // String outputString = DOM2Writer.nodeToString(policyElement);
        assertNotNull(policyElement);
    }

    @org.junit.Test
    public void testAuthnRequestBuilder() throws Exception {
        Document doc = DOMUtils.createDocument();

        AuthnRequestBuilder authnRequestBuilder = new DefaultAuthnRequestBuilder();
        Message message = new MessageImpl();

        AuthnRequest authnRequest =
            authnRequestBuilder.createAuthnRequest(
                message, "http://localhost:9001/app", "http://localhost:9001/sso"
            );
        Element policyElement = OpenSAMLUtil.toDom(authnRequest, doc);
        doc.appendChild(policyElement);
        // String outputString = DOM2Writer.nodeToString(policyElement);
        assertNotNull(policyElement);
    }

    @org.junit.Test
    public void testAuthnRequestID() throws Exception {
        AuthnRequestBuilder authnRequestBuilder = new DefaultAuthnRequestBuilder();
        AuthnRequest authnRequest =
            authnRequestBuilder.createAuthnRequest(
                new MessageImpl(), "http://localhost:9001/app", "http://localhost:9001/sso"
            );
        assertTrue("ID must start with a letter or underscore, and can only contain letters, digits, "
            + "underscores, hyphens, and periods.", authnRequest.getID().matches("^[_a-zA-Z][-_0-9a-zA-Z\\.]+$"));
    }

    @org.junit.Test
    public void testCreateLogoutRequest() throws Exception {
        Document doc = DOMUtils.createDocument();

        Issuer issuer =
            SamlpRequestComponentBuilder.createIssuer("http://localhost:9001/app");

        NameIDBean nameIdBean = new NameIDBean();
        nameIdBean.setNameValue("uid=joe,ou=people,ou=saml-demo,o=example.com");
        nameIdBean.setNameQualifier("www.example.com");
        NameID nameID = SAML2ComponentBuilder.createNameID(nameIdBean);

        Date notOnOrAfter = new Date();
        notOnOrAfter.setTime(notOnOrAfter.getTime() + 60L * 1000L);
        LogoutRequest logoutRequest =
            SamlpRequestComponentBuilder.createLogoutRequest(SAMLVersion.VERSION_20, issuer, null, null,
                                                             notOnOrAfter, null, nameID);

        Element policyElement = OpenSAMLUtil.toDom(logoutRequest, doc);
        doc.appendChild(policyElement);
        // String outputString = DOM2Writer.nodeToString(policyElement);
        assertNotNull(policyElement);
    }
}
