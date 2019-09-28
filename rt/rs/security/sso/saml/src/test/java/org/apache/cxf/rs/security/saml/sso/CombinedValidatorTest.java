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
import java.util.Collections;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.wss4j.common.WSS4JConstants;
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
import org.joda.time.DateTime;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.common.xml.SAMLConstants;
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
 * Some unit tests for the SAMLProtocolResponseValidator and the SAMLSSOResponseValidator
 */
public class CombinedValidatorTest {

    static {
        WSSConfig.init();
        OpenSAMLUtil.initSamlEngine();
    }

    @org.junit.Test
    public void testSuccessfulValidation() throws Exception {

        Document doc = DOMUtils.createDocument();

        Response response = createResponse(doc);

        Element responseElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(responseElement);
        assertNotNull(responseElement);

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
        SamlAssertionWrapper parsedAssertion =
            new SamlAssertionWrapper(ssoResponse.getAssertionElement());

        assertEquals("alice", parsedAssertion.getSubjectName());
    }

    @org.junit.Test
    public void testWrappingAttack3() throws Exception {
        Document doc = DOMUtils.createDocument();
        Response response = createResponse(doc);

        Element responseElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(responseElement);
        assertNotNull(responseElement);

        // Get Assertion Element
        Element assertionElement =
            (Element)responseElement.getElementsByTagNameNS(SAMLConstants.SAML20_NS, "Assertion").item(0);
        assertNotNull(assertionElement);

        // Clone it, strip the Signature, modify the Subject, change Subj Conf
        Element clonedAssertion = (Element)assertionElement.cloneNode(true);
        clonedAssertion.setAttributeNS(null, "ID", "_12345623562");
        Element sigElement =
            (Element)clonedAssertion.getElementsByTagNameNS(WSS4JConstants.SIG_NS, "Signature").item(0);
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
        ssoValidator.setEnforceAssertionsSigned(false);
        ssoValidator.setIssuerIDP("http://cxf.apache.org/issuer");
        ssoValidator.setAssertionConsumerURL("http://recipient.apache.org");
        ssoValidator.setClientAddress("http://apache.org");
        ssoValidator.setRequestId("12345");
        ssoValidator.setSpIdentifier("http://service.apache.org");

        // Parse the response
        SSOValidatorResponse ssoResponse =
            ssoValidator.validateSamlResponse(marshalledResponse, false);
        SamlAssertionWrapper parsedAssertion =
            new SamlAssertionWrapper(ssoResponse.getAssertionElement());

        assertEquals("alice", parsedAssertion.getSubjectName());
    }

    @org.junit.Test
    public void testSuccessfulSignedValidation() throws Exception {

        Document doc = DOMUtils.createDocument();
        Response response = createResponse(doc);

        Crypto issuerCrypto = new Merlin();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        ClassLoader loader = Loader.getClassLoader(CombinedValidatorTest.class);
        InputStream input = Merlin.loadInputStream(loader, "alice.jks");
        keyStore.load(input, "password".toCharArray());
        ((Merlin)issuerCrypto).setKeyStore(keyStore);

        signResponse(response, "alice", "password", issuerCrypto, true);

        Element responseElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(responseElement);
        assertNotNull(responseElement);

        Response marshalledResponse = (Response)OpenSAMLUtil.fromDom(responseElement);

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
        SamlAssertionWrapper parsedAssertion =
            new SamlAssertionWrapper(ssoResponse.getAssertionElement());

        assertEquals("alice", parsedAssertion.getSubjectName());
    }

    @org.junit.Test
    public void testEnforceResponseSigned() throws Exception {

        Document doc = DOMUtils.createDocument();

        Response response = createResponse(doc);

        Element responseElement = OpenSAMLUtil.toDom(response, doc);
        doc.appendChild(responseElement);
        assertNotNull(responseElement);

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
        ssoValidator.setEnforceResponseSigned(true);

        // Parse the response
        try {
            ssoValidator.validateSamlResponse(marshalledResponse, false);
            fail("Failure expected on an unsigned Response");
        } catch (WSSecurityException ex) {
            // expected
        }
    }

    private Response createResponse(Document doc) throws Exception {
        Status status =
            SAML2PResponseComponentBuilder.createStatus(
                SAMLProtocolResponseValidator.SAML2_STATUSCODE_SUCCESS, null
            );
        Response response =
            SAML2PResponseComponentBuilder.createSAMLResponse(
                "http://cxf.apache.org/saml", "http://cxf.apache.org/issuer", status
            );
        response.setDestination("http://recipient.apache.org");

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

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);

        Crypto issuerCrypto = new Merlin();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        ClassLoader loader = Loader.getClassLoader(CombinedValidatorTest.class);
        InputStream input = Merlin.loadInputStream(loader, "alice.jks");
        keyStore.load(input, "password".toCharArray());
        ((Merlin)issuerCrypto).setKeyStore(keyStore);

        assertion.signAssertion("alice", "password", issuerCrypto, false);

        response.getAssertions().add(assertion.getSaml2());

        return response;
    }

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
                "No issuer certs were found to sign the SAML Assertion using issuer name: " + issuerKeyName);
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
                throw new Exception("Error generating KeyInfo from signing credential", ex);
            }
        }

        // add the signature to the assertion
        SignableSAMLObject signableObject = response;
        signableObject.setSignature(signature);
        signableObject.releaseDOM();
        signableObject.releaseChildrenDOM(true);
    }
}
