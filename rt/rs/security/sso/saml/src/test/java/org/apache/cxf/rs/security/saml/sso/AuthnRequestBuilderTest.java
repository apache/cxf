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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;

/**
 * Some unit tests for the SamlpRequestComponentBuilder and AuthnRequestBuilder
 */
public class AuthnRequestBuilderTest extends org.junit.Assert {
    
    static {
        OpenSAMLUtil.initSamlEngine();
    }

    @org.junit.Test
    public void testCreateAuthnRequest() throws Exception {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        
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
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        
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
    
}
