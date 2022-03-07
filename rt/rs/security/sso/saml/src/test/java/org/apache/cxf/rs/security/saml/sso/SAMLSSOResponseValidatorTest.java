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
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.AudienceRestrictionBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.SubjectConfirmationDataBean;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.Loader;
import org.joda.time.DateTime;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Some unit tests for the SAMLSSOResponseValidator.
 */
public class SAMLSSOResponseValidatorTest {

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
        validator.setEnforceAssertionsSigned(false);
        validator.setIssuerIDP("http://cxf.apache.org/issuer");
        validator.setAssertionConsumerURL("http://recipient.apache.org");
        validator.setClientAddress("http://apache.org");
        validator.setRequestId("12345");
        validator.setSpIdentifier("http://service.apache.org");

        SSOValidatorResponse validateSamlResponse = validator.validateSamlResponse(response, false);
        assertEquals(response.getID(), validateSamlResponse.getResponseId());
        assertNotNull(validateSamlResponse.getAssertionElement());
        assertNotNull(validateSamlResponse.getCreated());
        assertNotNull(validateSamlResponse.getSessionNotOnOrAfter());
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
        validator.setEnforceAssertionsSigned(false);
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
        validator.setEnforceAssertionsSigned(false);
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
        validator.setEnforceAssertionsSigned(false);
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
        validator.setEnforceAssertionsSigned(false);
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
        validator.setEnforceAssertionsSigned(false);
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
    public void testSignedResponseInvalidDestination() throws Exception {
        Document doc = DOMUtils.createDocument();

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

        ConditionsBean conditions = new ConditionsBean();
        conditions.setNotBefore(new DateTime());
        conditions.setNotAfter(new DateTime().plusMinutes(5));
        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList("http://service.apache.org"));
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));
        callbackHandler.setConditions(conditions);

        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");
        callbackHandler.setSubjectConfirmationData(subjectConfirmationData);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        response.getAssertions().add(assertion.getSaml2());
        response.setDestination("xyz");

        Crypto issuerCrypto = new Merlin();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        ClassLoader loader = Loader.getClassLoader(SAMLResponseValidatorTest.class);
        InputStream input = Merlin.loadInputStream(loader, "alice.jks");
        keyStore.load(input, "password".toCharArray());
        ((Merlin)issuerCrypto).setKeyStore(keyStore);

        signResponse(response, "alice", "password", issuerCrypto, true);

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setIssuerIDP("http://cxf.apache.org/issuer");
        validator.setAssertionConsumerURL("http://recipient.apache.org");
        validator.setClientAddress("http://apache.org");
        validator.setRequestId("12345");
        validator.setSpIdentifier("http://service.apache.org");
        try {
            validator.validateSamlResponse(marshalledResponse, false);
            fail("Expected failure on bad response");
        } catch (WSSecurityException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testResponseInvalidIssuer() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");

        Response response = createResponse(subjectConfirmationData);
        response.setIssuer(SAML2PResponseComponentBuilder.createIssuer("xyz"));

        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setEnforceAssertionsSigned(false);
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
    public void testMissingAuthnStatement() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");

        Response response = createResponse(subjectConfirmationData);
        response.getAssertions().get(0).getAuthnStatements().clear();

        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setEnforceAssertionsSigned(false);
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
    public void testNoSubjectConfirmationData() throws Exception {
        Response response = createResponse(null);

        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setEnforceAssertionsSigned(false);
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
    public void testEmptyAudienceRestriction() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");

        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        Response response =
            createResponse(subjectConfirmationData,
                           Collections.singletonList(audienceRestriction),
                           null);

        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setEnforceAssertionsSigned(false);
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
    public void testBadAudienceRestriction() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");

        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(
            Collections.singletonList("http://unknown-service.apache.org"));
        Response response =
            createResponse(subjectConfirmationData,
                           Collections.singletonList(audienceRestriction),
                           null);

        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setEnforceAssertionsSigned(false);
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
    public void testAudienceRestrictionMultipleValues() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");

        List<String> values = new ArrayList<>();
        values.add("http://unknown-service.apache.org");
        values.add("http://service.apache.org");

        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(values);
        Response response =
            createResponse(subjectConfirmationData,
                           Collections.singletonList(audienceRestriction),
                           null);

        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setEnforceAssertionsSigned(false);
        validator.setIssuerIDP("http://cxf.apache.org/issuer");
        validator.setAssertionConsumerURL("http://recipient.apache.org");
        validator.setClientAddress("http://apache.org");
        validator.setRequestId("12345");
        validator.setSpIdentifier("http://service.apache.org");

        validator.validateSamlResponse(response, false);
    }

    @org.junit.Test
    public void testMultipleAudienceRestrictions() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");

        List<AudienceRestrictionBean> audienceRestrictions =
            new ArrayList<>();

        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(
            Collections.singletonList("http://unknown-service.apache.org"));
        audienceRestrictions.add(audienceRestriction);

        audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(
            Collections.singletonList("http://service.apache.org"));
        audienceRestrictions.add(audienceRestriction);

        Response response =
            createResponse(subjectConfirmationData, audienceRestrictions, null);

        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setEnforceAssertionsSigned(false);
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
    public void testAssertionBadIssuer() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");

        // Create a AuthenticationAssertion
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setStatement(SAML2CallbackHandler.Statement.AUTHN);
        callbackHandler.setIssuer("http://cxf.apache.org/bad-issuer");
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);

        callbackHandler.setSubjectConfirmationData(subjectConfirmationData);

        ConditionsBean conditions = new ConditionsBean();
        conditions.setNotBefore(new DateTime());
        conditions.setNotAfter(new DateTime().plusMinutes(5));

        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList("http://service.apache.org"));
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));
        callbackHandler.setConditions(conditions);

        Response response = createResponse(subjectConfirmationData, callbackHandler);

        // Validate the Response
        SAMLSSOResponseValidator validator = new SAMLSSOResponseValidator();
        validator.setEnforceAssertionsSigned(false);
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
    public void testEnforceAssertionsSigned() throws Exception {

        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(new DateTime().plusMinutes(5));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");

        Response response = createResponse(subjectConfirmationData);

        Crypto issuerCrypto = new Merlin();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        ClassLoader loader = Loader.getClassLoader(CombinedValidatorTest.class);
        InputStream input = Merlin.loadInputStream(loader, "alice.jks");
        keyStore.load(input, "password".toCharArray());
        ((Merlin)issuerCrypto).setKeyStore(keyStore);

        // Test SSO validation
        SAMLSSOResponseValidator ssoValidator = new SAMLSSOResponseValidator();
        ssoValidator.setIssuerIDP("http://cxf.apache.org/issuer");
        ssoValidator.setAssertionConsumerURL("http://recipient.apache.org");
        ssoValidator.setClientAddress("http://apache.org");
        ssoValidator.setRequestId("12345");
        ssoValidator.setSpIdentifier("http://service.apache.org");

        // Parse the response
        try {
            ssoValidator.validateSamlResponse(response, false);
            fail("Failure expected on an unsigned Assertion");
        } catch (WSSecurityException ex) {
            // expected
        }
    }

    private Response createResponse(
        SubjectConfirmationDataBean subjectConfirmationData
    ) throws Exception {
        return createResponse(subjectConfirmationData, null, null);
    }

    private Response createResponse(
        SubjectConfirmationDataBean subjectConfirmationData,
        List<AudienceRestrictionBean> audienceRestrictions,
        String authnClassRef
    ) throws Exception {
        Document doc = DOMUtils.createDocument();

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

        if (audienceRestrictions == null) {
            AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
            audienceRestriction.setAudienceURIs(Collections.singletonList("http://service.apache.org"));
            conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));
        } else {
            conditions.setAudienceRestrictions(audienceRestrictions);
        }
        callbackHandler.setConditions(conditions);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        response.getAssertions().add(assertion.getSaml2());

        if (authnClassRef != null) {
            AuthnStatement authnStatement =
                response.getAssertions().get(0).getAuthnStatements().get(0);
            authnStatement.getAuthnContext().setAuthnContextClassRef(
                SAML2PResponseComponentBuilder.createAuthnContextClassRef(authnClassRef));
        }

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        return (Response)OpenSAMLUtil.fromDom(policyElement);
    }

    private Response createResponse(
        SubjectConfirmationDataBean subjectConfirmationData,
        SAML2CallbackHandler callbackHandler
    ) throws Exception {
        Document doc = DOMUtils.createDocument();

        Status status =
            SAML2PResponseComponentBuilder.createStatus(
                SAMLProtocolResponseValidator.SAML2_STATUSCODE_SUCCESS, null
            );
        Response response =
            SAML2PResponseComponentBuilder.createSAMLResponse(
                "http://cxf.apache.org/saml", "http://cxf.apache.org/issuer", status
            );

        // Create an AuthenticationAssertion
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        response.getAssertions().add(assertion.getSaml2());

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        return (Response)OpenSAMLUtil.fromDom(policyElement);
    }

    /**
     * Sign a SAML Response
     * @throws Exception
     */
    private void signResponse(
        Response response,
        String issuerKeyName,
        String issuerKeyPassword,
        Crypto issuerCrypto,
        boolean useKeyInfo
    ) throws Exception {
        //
        // Create the signature
        //
        Signature signature = OpenSAMLUtil.buildSignature();
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        // prepare to sign the SAML token
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(issuerKeyName);
        X509Certificate[] issuerCerts = issuerCrypto.getX509Certificates(cryptoType);
        if (issuerCerts == null) {
            throw new Exception(
                    "No issuer certs were found to sign the SAML Assertion using issuer name: "
                            + issuerKeyName);
        }

        String sigAlgo = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1;
        String pubKeyAlgo = issuerCerts[0].getPublicKey().getAlgorithm();

        if ("DSA".equalsIgnoreCase(pubKeyAlgo)) {
            sigAlgo = SignatureConstants.ALGO_ID_SIGNATURE_DSA;
        }

        PrivateKey privateKey = issuerCrypto.getPrivateKey(issuerKeyName, issuerKeyPassword);

        signature.setSignatureAlgorithm(sigAlgo);

        BasicX509Credential signingCredential = new BasicX509Credential(issuerCerts[0], privateKey);

        signature.setSigningCredential(signingCredential);

        if (useKeyInfo) {
            X509KeyInfoGeneratorFactory kiFactory = new X509KeyInfoGeneratorFactory();
            kiFactory.setEmitEntityCertificate(true);

            try {
                KeyInfo keyInfo = kiFactory.newInstance().generate(signingCredential);
                signature.setKeyInfo(keyInfo);
            } catch (org.opensaml.security.SecurityException ex) {
                throw new Exception(
                        "Error generating KeyInfo from signing credential", ex);
            }
        }

        // add the signature to the assertion
        SignableSAMLObject signableObject = response;
        signableObject.setSignature(signature);
        signableObject.releaseDOM();
        signableObject.releaseChildrenDOM(true);
    }
}
