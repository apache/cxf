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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.apache.ws.security.saml.ext.SAMLParms;
import org.apache.ws.security.saml.ext.bean.ConditionsBean;
import org.apache.ws.security.saml.ext.bean.SubjectConfirmationDataBean;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;

/**
 * Some unit tests for the SAMLSSOResponseValidator.
 */
public class SAMLSSOResponseValidatorTest extends org.junit.Assert {
    
    static {
        OpenSAMLUtil.initSamlEngine();
    }

    @org.junit.Test
    public void testCreateAndValidateResponse() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");
        
        Response response = createResponse(subjectConfirmationData);
        
        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setIssuerIDP("http://cxf.apache.org/issuer");
        validator.setAssertionConsumerURL("http://recipient.apache.org");
        validator.setClientAddress("http://apache.org");
        validator.setRequestId("12345");
        validator.setSpIdentifier("http://service.apache.org");
        validator.validateSamlResponse(response, false);
    }
    
    @org.junit.Test
    public void testInvalidAddress() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://bad.apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");
        
        Response response = createResponse(subjectConfirmationData);
        
        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setIssuerIDP("http://cxf.apache.org/issuer");
        validator.setAssertionConsumerURL("http://recipient.apache.org");
        validator.setClientAddress("http://apache.org");
        validator.setRequestId("12345");
        validator.setSpIdentifier("http://service.apache.org");
        try {
            validator.validateSamlResponse(response, false);
            fail("Expected failure on bad response");
        } catch (WSSecurityException ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testInvalidRequestId() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345-bad");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");
        
        Response response = createResponse(subjectConfirmationData);
        
        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setIssuerIDP("http://cxf.apache.org/issuer");
        validator.setAssertionConsumerURL("http://recipient.apache.org");
        validator.setClientAddress("http://apache.org");
        validator.setRequestId("12345");
        validator.setSpIdentifier("http://service.apache.org");
        try {
            validator.validateSamlResponse(response, false);
            fail("Expected failure on bad response");
        } catch (WSSecurityException ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testInvalidRecipient() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://bad.recipient.apache.org");
        
        Response response = createResponse(subjectConfirmationData);
        
        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setIssuerIDP("http://cxf.apache.org/issuer");
        validator.setAssertionConsumerURL("http://recipient.apache.org");
        validator.setClientAddress("http://apache.org");
        validator.setRequestId("12345");
        validator.setSpIdentifier("http://service.apache.org");
        try {
            validator.validateSamlResponse(response, false);
            fail("Expected failure on bad response");
        } catch (WSSecurityException ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testInvalidNotOnOrAfter() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().minusSeconds(1));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");
        
        Response response = createResponse(subjectConfirmationData);
        
        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setIssuerIDP("http://cxf.apache.org/issuer");
        validator.setAssertionConsumerURL("http://recipient.apache.org");
        validator.setClientAddress("http://apache.org");
        validator.setRequestId("12345");
        validator.setSpIdentifier("http://service.apache.org");
        try {
            validator.validateSamlResponse(response, false);
            fail("Expected failure on bad response");
        } catch (WSSecurityException ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testInvalidNotBefore() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setNotBefore(new DateTime());
        subjectConfirmationData.setRecipient("http://recipient.apache.org");
        
        Response response = createResponse(subjectConfirmationData);
        
        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setIssuerIDP("http://cxf.apache.org/issuer");
        validator.setAssertionConsumerURL("http://recipient.apache.org");
        validator.setClientAddress("http://apache.org");
        validator.setRequestId("12345");
        validator.setSpIdentifier("http://service.apache.org");
        try {
            validator.validateSamlResponse(response, false);
            fail("Expected failure on bad response");
        } catch (WSSecurityException ex) {
            // expected
        }
    }
    
    private Response createResponse(
        SubjectConfirmationDataBean subjectConfirmationData
    ) throws Exception {
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
        
        callbackHandler.setSubjectConfirmationData(subjectConfirmationData);
        
        ConditionsBean conditions = new ConditionsBean();
        conditions.setNotBefore(new DateTime());
        conditions.setNotAfter(new DateTime().plusMinutes(5));
        conditions.setAudienceURI("http://service.apache.org");
        callbackHandler.setConditions(conditions);
        
        SAMLParms samlParms = new SAMLParms();
        samlParms.setCallbackHandler(callbackHandler);
        AssertionWrapper assertion = new AssertionWrapper(samlParms);
        
        response.getAssertions().add(assertion.getSaml2());
        
        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);
        
        return (Response)OpenSAMLUtil.fromDom(policyElement);
    }
    
}
