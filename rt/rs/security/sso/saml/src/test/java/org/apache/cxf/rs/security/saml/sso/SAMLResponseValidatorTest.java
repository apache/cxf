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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.apache.wss4j.dom.engine.WSSConfig;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some unit tests for the SAMLProtocolResponseValidator.
 */
public class SAMLResponseValidatorTest {

    static {
        WSSConfig.init();
        OpenSAMLUtil.initSamlEngine();
    }

    @org.junit.Test
    public void testCreateAndValidateResponse() throws Exception {
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
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        response.getAssertions().add(assertion.getSaml2());

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        validator.validateSamlResponse(marshalledResponse, null, null);
    }

    @org.junit.Test
    public void testInvalidStatusCode() throws Exception {
        Document doc = DOMUtils.createDocument();

        Status status =
            SAML2PResponseComponentBuilder.createStatus(
                SAMLProtocolResponseValidator.SAML1_STATUSCODE_SUCCESS, null
            );
        Response response =
            SAML2PResponseComponentBuilder.createSAMLResponse(
                "http://cxf.apache.org/saml", "http://cxf.apache.org/issuer", status
            );

        // Create an AuthenticationAssertion
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setStatement(SAML2CallbackHandler.Statement.AUTHN);
        callbackHandler.setIssuer("http://cxf.apache.org/issuer");
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        response.getAssertions().add(assertion.getSaml2());

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        try {
            validator.validateSamlResponse(marshalledResponse, null, null);
            fail("Expected failure on an invalid SAML code");
        } catch (WSSecurityException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testRequestDeniedStatusCode() throws Exception {
        Document doc = DOMUtils.createDocument();

        Status status =
            SAML2PResponseComponentBuilder.createStatus(
                "urn:oasis:names:tc:SAML:2.0:status:RequestDenied", null
            );
        Response response =
            SAML2PResponseComponentBuilder.createSAMLResponse(
                "http://cxf.apache.org/saml", "http://cxf.apache.org/issuer", status
            );

        // Create an AuthenticationAssertion
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setStatement(SAML2CallbackHandler.Statement.AUTHN);
        callbackHandler.setIssuer("http://cxf.apache.org/issuer");
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        response.getAssertions().add(assertion.getSaml2());

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        try {
            validator.validateSamlResponse(marshalledResponse, null, null);
            fail("Expected failure on an invalid SAML code");
        } catch (WSSecurityException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testResponseSignedAssertion() throws Exception {
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
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        Crypto issuerCrypto = new Merlin();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        ClassLoader loader = Loader.getClassLoader(SAMLResponseValidatorTest.class);
        InputStream input = Merlin.loadInputStream(loader, "alice.jks");
        keyStore.load(input, "password".toCharArray());
        ((Merlin)issuerCrypto).setKeyStore(keyStore);

        assertion.signAssertion("alice", "password", issuerCrypto, false);

        response.getAssertions().add(assertion.getSaml2());

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        try {
            validator.validateSamlResponse(marshalledResponse, null, new KeystorePasswordCallback());
            fail("Expected failure on no Signature Crypto");
        } catch (WSSecurityException ex) {
            // expected
        }

        // Validate the Response
        validator.validateSamlResponse(
            marshalledResponse, issuerCrypto, new KeystorePasswordCallback()
        );
    }

    @org.junit.Test
    public void testResponseModifiedSignedAssertion() throws Exception {
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
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        Crypto issuerCrypto = new Merlin();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        ClassLoader loader = Loader.getClassLoader(SAMLResponseValidatorTest.class);
        InputStream input = Merlin.loadInputStream(loader, "alice.jks");
        keyStore.load(input, "password".toCharArray());
        ((Merlin)issuerCrypto).setKeyStore(keyStore);

        assertion.signAssertion("alice", "password", issuerCrypto, false);

        response.getAssertions().add(assertion.getSaml2());

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        List<Element> assertions =
            DOMUtils.findAllElementsByTagNameNS(policyElement, SAMLConstants.SAML20_NS, "Assertion");
        assertNotNull(assertions);
        assertTrue(assertions.size() == 1);
        assertions.get(0).setAttributeNS(null, "newattr", "http://apache.org");

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        try {
           // Validate the Response
            validator.validateSamlResponse(
                marshalledResponse, issuerCrypto, new KeystorePasswordCallback()
            );
            fail("Expected failure on a bad signature");
        } catch (WSSecurityException ex) {
            // expected
        }

    }

    @org.junit.Test
    public void testSignedResponse() throws Exception {
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
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        Crypto issuerCrypto = new Merlin();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        ClassLoader loader = Loader.getClassLoader(SAMLResponseValidatorTest.class);
        InputStream input = Merlin.loadInputStream(loader, "alice.jks");
        keyStore.load(input, "password".toCharArray());
        ((Merlin)issuerCrypto).setKeyStore(keyStore);

        response.getAssertions().add(assertion.getSaml2());
        signResponse(response, "alice", "password", issuerCrypto, true);

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        try {
            validator.validateSamlResponse(marshalledResponse, null, new KeystorePasswordCallback());
            fail("Expected failure on no Signature Crypto");
        } catch (WSSecurityException ex) {
            // expected
        }

        // Validate the Response
        validator.validateSamlResponse(
            marshalledResponse, issuerCrypto, new KeystorePasswordCallback()
        );
    }

    @org.junit.Test
    public void testModifiedSignedResponse() throws Exception {
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
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        Crypto issuerCrypto = new Merlin();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        ClassLoader loader = Loader.getClassLoader(SAMLResponseValidatorTest.class);
        InputStream input = Merlin.loadInputStream(loader, "alice.jks");
        keyStore.load(input, "password".toCharArray());
        ((Merlin)issuerCrypto).setKeyStore(keyStore);

        response.getAssertions().add(assertion.getSaml2());
        signResponse(response, "alice", "password", issuerCrypto, true);

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        policyElement.setAttributeNS(null, "newattr", "http://apache.org");

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        try {
            // Validate the Response
            validator.validateSamlResponse(
                marshalledResponse, issuerCrypto, new KeystorePasswordCallback()
            );
            fail("Expected failure on a bad signature");
        } catch (WSSecurityException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testSignedResponseNoKeyInfo() throws Exception {
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
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        Crypto issuerCrypto = new Merlin();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        ClassLoader loader = Loader.getClassLoader(SAMLResponseValidatorTest.class);
        InputStream input = Merlin.loadInputStream(loader, "alice.jks");
        keyStore.load(input, "password".toCharArray());
        ((Merlin)issuerCrypto).setKeyStore(keyStore);
        issuerCrypto.setDefaultX509Identifier("alice");

        response.getAssertions().add(assertion.getSaml2());
        signResponse(response, "alice", "password", issuerCrypto, false);

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        validator.setKeyInfoMustBeAvailable(false);
        try {
            validator.validateSamlResponse(marshalledResponse, null, new KeystorePasswordCallback());
            fail("Expected failure on no Signature Crypto");
        } catch (WSSecurityException ex) {
            // expected
        }

        // Validate the Response
        validator.validateSamlResponse(
            marshalledResponse, issuerCrypto, new KeystorePasswordCallback()
        );
    }

    @org.junit.Test
    public void testResponseInvalidVersion() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(Instant.now().plus(5, ChronoUnit.MINUTES));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");

        // Create a AuthenticationAssertion
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setStatement(SAML2CallbackHandler.Statement.AUTHN);
        callbackHandler.setIssuer("http://cxf.apache.org/issuer");
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);

        callbackHandler.setSubjectConfirmationData(subjectConfirmationData);

        ConditionsBean conditions = new ConditionsBean();
        conditions.setNotBefore(Instant.now());
        conditions.setNotAfter(Instant.now().plus(5, ChronoUnit.MINUTES));

        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList("http://service.apache.org"));
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));
        callbackHandler.setConditions(conditions);

        Response response = createResponse(subjectConfirmationData, callbackHandler);
        response.setVersion(SAMLVersion.VERSION_10);

        // Validate the Response
        SAMLProtocolResponseValidator protocolValidator = new SAMLProtocolResponseValidator();

        try {
            protocolValidator.validateSamlResponse(response, null, null);
            fail("Expected failure on bad response");
        } catch (WSSecurityException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testAssertionBadSubjectConfirmationMethod() throws Exception {
        SubjectConfirmationDataBean subjectConfirmationData = new SubjectConfirmationDataBean();
        subjectConfirmationData.setAddress("http://apache.org");
        subjectConfirmationData.setInResponseTo("12345");
        subjectConfirmationData.setNotAfter(Instant.now().plus(5, ChronoUnit.MINUTES));
        subjectConfirmationData.setRecipient("http://recipient.apache.org");

        // Create a AuthenticationAssertion
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setStatement(SAML2CallbackHandler.Statement.AUTHN);
        callbackHandler.setIssuer("http://cxf.apache.org/issuer");
        callbackHandler.setConfirmationMethod("xyz");

        callbackHandler.setSubjectConfirmationData(subjectConfirmationData);

        ConditionsBean conditions = new ConditionsBean();
        conditions.setNotBefore(Instant.now());
        conditions.setNotAfter(Instant.now().plus(5, ChronoUnit.MINUTES));

        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList("http://service.apache.org"));
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));
        callbackHandler.setConditions(conditions);

        Response response = createResponse(subjectConfirmationData, callbackHandler);

        // Validate the Response
        SAMLProtocolResponseValidator protocolValidator = new SAMLProtocolResponseValidator();

        try {
            protocolValidator.validateSamlResponse(response, null, null);
            fail("Expected failure on bad response");
        } catch (WSSecurityException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testResponseIssueInstant() throws Exception {
        Document doc = DOMUtils.createDocument();

        Status status =
            SAML2PResponseComponentBuilder.createStatus(
                SAMLProtocolResponseValidator.SAML2_STATUSCODE_SUCCESS, null
            );
        Response response =
            SAML2PResponseComponentBuilder.createSAMLResponse(
                "http://cxf.apache.org/saml", "http://cxf.apache.org/issuer", status
            );

        response.setIssueInstant(Instant.now().plus(5, ChronoUnit.MINUTES));

        // Create an AuthenticationAssertion
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        callbackHandler.setStatement(SAML2CallbackHandler.Statement.AUTHN);
        callbackHandler.setIssuer("http://cxf.apache.org/issuer");
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        response.getAssertions().add(assertion.getSaml2());

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        try {
            validator.validateSamlResponse(marshalledResponse, null, null);
            fail("Expected failure on an invalid Response IssueInstant");
        } catch (WSSecurityException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testAssertionIssueInstant() throws Exception {
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
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        assertion.getSaml2().setIssueInstant(Instant.now().plus(5, ChronoUnit.MINUTES));

        response.getAssertions().add(assertion.getSaml2());

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        try {
            validator.validateSamlResponse(marshalledResponse, null, null);
            fail("Expected failure on an invalid Assertion IssueInstant");
        } catch (WSSecurityException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testFutureAuthnInstant() throws Exception {
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
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);
        callbackHandler.setAuthnInstant(Instant.now().plus(1, ChronoUnit.DAYS));

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        response.getAssertions().add(assertion.getSaml2());

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        try {
            validator.validateSamlResponse(marshalledResponse, null, null);
            fail("Expected failure on an invalid Assertion AuthnInstant");
        } catch (WSSecurityException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testStaleSessionNotOnOrAfter() throws Exception {
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
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);
        callbackHandler.setSessionNotOnOrAfter(Instant.now().minus(1, ChronoUnit.DAYS));

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        response.getAssertions().add(assertion.getSaml2());

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        try {
            validator.validateSamlResponse(marshalledResponse, null, null);
            fail("Expected failure on an invalid SessionNotOnOrAfter");
        } catch (WSSecurityException ex) {
            // expected
        }
    }

    @org.junit.Test
    public void testInvalidSubjectLocality() throws Exception {
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
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);
        callbackHandler.setSubjectLocality("xyz.123", null);

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        response.getAssertions().add(assertion.getSaml2());

        Element policyElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(policyElement);
        assertNotNull(policyElement);

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(policyElement);

        // Validate the Response
        SAMLProtocolResponseValidator validator = new SAMLProtocolResponseValidator();
        try {
            validator.validateSamlResponse(marshalledResponse, null, null);
            fail("Expected failure on an invalid SessionNotOnOrAfter");
        } catch (WSSecurityException ex) {
            // expected
        }
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

        BasicX509Credential signingCredential =
            new BasicX509Credential(issuerCerts[0], privateKey);
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
}
