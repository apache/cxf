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

import java.io.InputStream;
import java.io.StringReader;
import java.security.KeyStore;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.Merlin;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.apache.ws.security.saml.ext.SAMLParms;
import org.apache.ws.security.saml.ext.bean.AudienceRestrictionBean;
import org.apache.ws.security.saml.ext.bean.ConditionsBean;
import org.apache.ws.security.saml.ext.bean.SubjectConfirmationDataBean;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.apache.ws.security.util.Loader;
import org.joda.time.DateTime;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;

/**
 * Some unit tests for the SAMLProtocolResponseValidator and the SAMLSSOResponseValidator
 */
public class CombinedValidatorTest extends org.junit.Assert {
    
    static {
        WSSConfig.init();
        OpenSAMLUtil.initSamlEngine();
    }

    @org.junit.Test
    public void testSuccessfulValidation() throws Exception {
        
        Element responseElement = createResponse();
        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(responseElement);
        
        Crypto issuerCrypto = new Merlin();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        ClassLoader loader = Loader.getClassLoader(CombinedValidatorTest.class);
        InputStream input = Merlin.loadInputStream(loader, "alice.jks");
        keyStore.load(input, "password".toCharArray());
        ((Merlin)issuerCrypto).setKeyStore(keyStore);
        
        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        validator.validateSamlResponse(
            marshalledResponse, issuerCrypto, new KeystorePasswordCallback()
        );
        
        // Test SSO validation
        SAMLSSOResponseValidator ssoValidator = new SAMLSSOResponseValidator();
        ssoValidator.setIssuerIDP("http://cxf.apache.org/issuer");
        ssoValidator.setAssertionConsumerURL("http://recipient.apache.org");
        ssoValidator.setClientAddress("http://apache.org");
        ssoValidator.setRequestId("12345");
        ssoValidator.setSpIdentifier("http://service.apache.org");
        
        // Parse the response
        SSOValidatorResponse ssoResponse = 
            ssoValidator.validateSamlResponse(marshalledResponse, false);
        Document assertionDoc = StaxUtils.read(new StringReader(ssoResponse.getAssertion()));
        AssertionWrapper parsedAssertion = 
            new AssertionWrapper(assertionDoc.getDocumentElement());
        
        assertEquals("alice", parsedAssertion.getSaml2().getSubject().getNameID().getValue());
    }
    
    @org.junit.Test
    public void testWrappingAttack3() throws Exception {
        Element responseElement = createResponse();
        
        // Get Assertion Element
        Element assertionElement = 
            (Element)responseElement.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Assertion").item(0);
        assertNotNull(assertionElement);
        
        // Clone it, strip the Signature, modify the Subject, change Subj Conf
        Element clonedAssertion = (Element)assertionElement.cloneNode(true);
        clonedAssertion.setAttributeNS(null, "ID", "_12345623562");
        Element sigElement = 
            (Element)clonedAssertion.getElementsByTagNameNS(WSConstants.SIG_NS, "Signature").item(0);
        clonedAssertion.removeChild(sigElement);
        
        Element subjElement = 
            (Element)clonedAssertion.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Subject").item(0);
        Element subjNameIdElement = 
            (Element)subjElement.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "NameID").item(0);
        subjNameIdElement.setTextContent("bob");
        
        Element subjConfElement = 
            (Element)subjElement.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "SubjectConfirmation").item(0);
        subjConfElement.setAttributeNS(null, "Method", SAML2Constants.CONF_SENDER_VOUCHES);
        
        // Now insert the modified cloned Assertion into the Response before actual assertion
        responseElement.insertBefore(clonedAssertion, assertionElement);
        
        // System.out.println(DOM2Writer.nodeToString(responseElement));
        
        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(responseElement);
        
        Crypto issuerCrypto = new Merlin();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        ClassLoader loader = Loader.getClassLoader(CombinedValidatorTest.class);
        InputStream input = Merlin.loadInputStream(loader, "alice.jks");
        keyStore.load(input, "password".toCharArray());
        ((Merlin)issuerCrypto).setKeyStore(keyStore);
        
        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        validator.validateSamlResponse(
            marshalledResponse, issuerCrypto, new KeystorePasswordCallback()
        );
        
        // Test SSO validation
        SAMLSSOResponseValidator ssoValidator = new SAMLSSOResponseValidator();
        ssoValidator.setIssuerIDP("http://cxf.apache.org/issuer");
        ssoValidator.setAssertionConsumerURL("http://recipient.apache.org");
        ssoValidator.setClientAddress("http://apache.org");
        ssoValidator.setRequestId("12345");
        ssoValidator.setSpIdentifier("http://service.apache.org");
        
        // Parse the response
        SSOValidatorResponse ssoResponse = 
            ssoValidator.validateSamlResponse(marshalledResponse, false);
        Document assertionDoc = StaxUtils.read(new StringReader(ssoResponse.getAssertion()));
        AssertionWrapper parsedAssertion = 
            new AssertionWrapper(assertionDoc.getDocumentElement());
        
        assertEquals("alice", parsedAssertion.getSaml2().getSubject().getNameID().getValue());
    }
    
    private Element createResponse() throws Exception {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        
        Status status = 
            SAML2PResponseComponentBuilder.createStatus(
                SAMLProtocolResponseValidator.SAML2_STATUSCODE_SUCCESS, null
            );
        Response response = 
            SAML2PResponseComponentBuilder.createSAMLResponse(
                "http://cxf.apache.org/saml", "http://cxf.apache.org/issuer", status
            );
        
        // Create an AuthenticationAssertion
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setStatement(SAML2CallbackHandler.Statement.AUTHN);
        callbackHandler.setIssuer("http://cxf.apache.org/issuer");
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);
        callbackHandler.setSubjectName("alice");
        
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");
        callbackHandler.setSubjectConfirmationData(subjectConfirmationData);
        
        ConditionsBean conditions = new ConditionsBean();
        conditions.setNotBefore(new DateTime());
        conditions.setNotAfter(new DateTime().plusMinutes(5));
        
        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList("http://service.apache.org"));
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));
        callbackHandler.setConditions(conditions);
        
        SAMLParms samlParms = new SAMLParms();
        samlParms.setCallbackHandler(callbackHandler);
        AssertionWrapper assertion = new AssertionWrapper(samlParms);
        
        Crypto issuerCrypto = new Merlin();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        ClassLoader loader = Loader.getClassLoader(CombinedValidatorTest.class);
        InputStream input = Merlin.loadInputStream(loader, "alice.jks");
        keyStore.load(input, "password".toCharArray());
        ((Merlin)issuerCrypto).setKeyStore(keyStore);
        
        assertion.signAssertion("alice", "password", issuerCrypto, false);
        
        response.getAssertions().add(assertion.getSaml2());
        
        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);
        
        return policyElement;
    }
}
