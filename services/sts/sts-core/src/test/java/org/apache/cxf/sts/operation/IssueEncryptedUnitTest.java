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
package org.apache.cxf.sts.operation;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.service.StaticService;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.dom.WSConstants;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Some unit tests for issuing encrypted tokens.
 */
public class IssueEncryptedUnitTest {

    private static boolean unrestrictedPoliciesInstalled;

    static {
        unrestrictedPoliciesInstalled = TestUtilities.checkUnrestrictedPoliciesInstalled();
    };

    /**
     * Test to successfully issue a (dummy) encrypted token.
     */
    @org.junit.Test
    public void testIssueEncryptedToken() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setEncryptIssuedToken(true);

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        if (!unrestrictedPoliciesInstalled) {
            encryptionProperties.setEncryptionAlgorithm(WSS4JConstants.AES_128);
        }
        service.setEncryptionProperties(encryptionProperties);
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto encryptionCrypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(encryptionCrypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
    }

    /**
     * Test for various options relating to specifying a name for encryption
     */
    @org.junit.Test
    public void testEncryptionName() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setEncryptIssuedToken(true);

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        if (!unrestrictedPoliciesInstalled) {
            encryptionProperties.setEncryptionAlgorithm(WSS4JConstants.AES_128);
        }
        service.setEncryptionProperties(encryptionProperties);
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto encryptionCrypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(encryptionCrypto);
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token - as no encryption name has been specified the token will not be encrypted
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());

        encryptionProperties.setEncryptionName("myservicekey");
        service.setEncryptionProperties(encryptionProperties);

        // Issue a (encrypted) token
        response = issueOperation.issue(request, null, msgCtx);
        securityTokenResponse = response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
    }


    /**
     * Test for various options relating to configuring an algorithm for encryption
     */
    @org.junit.Test
    public void testConfiguredEncryptionAlgorithm() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setEncryptIssuedToken(true);

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEncryptionName("myservicekey");
        encryptionProperties.setEncryptionAlgorithm(WSS4JConstants.AES_128);
        service.setEncryptionProperties(encryptionProperties);
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto encryptionCrypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(encryptionCrypto);
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token - this should use a (new) default encryption algorithm as configured
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());

        encryptionProperties.setEncryptionAlgorithm(WSS4JConstants.KEYTRANSPORT_RSA15);
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected on a bad encryption algorithm");
        } catch (STSException ex) {
            // expected
        }
    }

    /**
     * Test for various options relating to receiving an algorithm for encryption
     */
    @org.junit.Test
    public void testReceivedEncryptionAlgorithm() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setEncryptIssuedToken(true);

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEncryptionName("myservicekey");
        service.setEncryptionProperties(encryptionProperties);
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto encryptionCrypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(encryptionCrypto);
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));
        JAXBElement<String> encryptionAlgorithmType =
            new JAXBElement<String>(
                QNameConstants.ENCRYPTION_ALGORITHM, String.class, WSS4JConstants.AES_128
            );
        request.getAny().add(encryptionAlgorithmType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());

        // Now specify a non-supported algorithm
        List<String> acceptedAlgorithms = Collections.singletonList(WSS4JConstants.KEYTRANSPORT_RSA15);
        encryptionProperties.setAcceptedEncryptionAlgorithms(acceptedAlgorithms);
        request.getAny().remove(request.getAny().size() - 1);
        encryptionAlgorithmType =
            new JAXBElement<String>(
                QNameConstants.ENCRYPTION_ALGORITHM, String.class, WSS4JConstants.KEYTRANSPORT_RSA15
            );
        request.getAny().add(encryptionAlgorithmType);
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected on a bad encryption algorithm");
        } catch (STSException ex) {
            // expected
        }
    }


    /**
     * Test for various options relating to configuring a key-wrap algorithm
     */
    @org.junit.Test
    public void testConfiguredKeyWrapAlgorithm() throws Exception {
        //
        // This test fails (sometimes) with the IBM JDK
        // See https://www-304.ibm.com/support/docview.wss?uid=swg1IZ76737
        //
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }

        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setEncryptIssuedToken(true);

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEncryptionName("myservicekey");
        if (!unrestrictedPoliciesInstalled) {
            encryptionProperties.setEncryptionAlgorithm(WSS4JConstants.AES_128);
        }
        encryptionProperties.setKeyWrapAlgorithm(WSS4JConstants.KEYTRANSPORT_RSAOAEP);
        service.setEncryptionProperties(encryptionProperties);
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto encryptionCrypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(encryptionCrypto);
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token - this should use a (new) default key-wrap algorithm as configured
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());

        encryptionProperties.setKeyWrapAlgorithm(WSS4JConstants.AES_128);
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected on a bad key-wrap algorithm");
        } catch (STSException ex) {
            // expected
        }
    }

    /**
     * Test for various options relating to configuring a key-wrap algorithm
     */
    @org.junit.Test
    public void testSpecifiedKeyWrapAlgorithm() throws Exception {
        //
        // This test fails (sometimes) with the IBM JDK
        // See https://www-304.ibm.com/support/docview.wss?uid=swg1IZ76737
        //
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            return;
        }

        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setEncryptIssuedToken(true);

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEncryptionName("myservicekey");
        if (!unrestrictedPoliciesInstalled) {
            encryptionProperties.setEncryptionAlgorithm(WSS4JConstants.AES_128);
        }
        service.setEncryptionProperties(encryptionProperties);
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto encryptionCrypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(encryptionCrypto);
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));
        JAXBElement<String> encryptionAlgorithmType =
            new JAXBElement<String>(
                QNameConstants.KEYWRAP_ALGORITHM, String.class, WSS4JConstants.KEYTRANSPORT_RSAOAEP
            );
        request.getAny().add(encryptionAlgorithmType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());

        // Now specify a non-supported algorithm
        String aesKw = "http://www.w3.org/2001/04/xmlenc#kw-aes128";
        List<String> acceptedAlgorithms = Collections.singletonList(aesKw);
        encryptionProperties.setAcceptedKeyWrapAlgorithms(acceptedAlgorithms);
        request.getAny().remove(request.getAny().size() - 1);
        encryptionAlgorithmType =
            new JAXBElement<String>(
                QNameConstants.KEYWRAP_ALGORITHM, String.class, aesKw
            );
        request.getAny().add(encryptionAlgorithmType);
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected on a bad key-wrap algorithm");
        } catch (STSException ex) {
            // expected
        }
    }

    /**
     * Test for various options relating to configuring a KeyIdentifier
     */
    @org.junit.Test
    public void testConfiguredKeyIdentifiers() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setEncryptIssuedToken(true);

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEncryptionName("myservicekey");
        if (!unrestrictedPoliciesInstalled) {
            encryptionProperties.setEncryptionAlgorithm(WSS4JConstants.AES_128);
        }
        encryptionProperties.setKeyIdentifierType(WSConstants.SKI_KEY_IDENTIFIER);
        service.setEncryptionProperties(encryptionProperties);
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto encryptionCrypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(encryptionCrypto);
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token - use various KeyIdentifiers
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());

        encryptionProperties.setKeyIdentifierType(WSConstants.SKI_KEY_IDENTIFIER);
        issueOperation.issue(request, null, msgCtx);

        encryptionProperties.setKeyIdentifierType(WSConstants.THUMBPRINT_IDENTIFIER);
        issueOperation.issue(request, null, msgCtx);

        encryptionProperties.setKeyIdentifierType(WSConstants.ENCRYPTED_KEY_SHA1_IDENTIFIER);
        issueOperation.issue(request, null, msgCtx);

        try {
            encryptionProperties.setKeyIdentifierType(WSConstants.BST);
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected on a bad key identifier");
        } catch (STSException ex) {
            // expected
        }
    }


    /*
     * Mock up an AppliesTo element using the supplied address
     */
    private Element createAppliesToElement(String addressUrl) {
        Document doc = DOMUtils.getEmptyDocument();
        Element appliesTo = doc.createElementNS(STSConstants.WSP_NS, "wsp:AppliesTo");
        appliesTo.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns:wsp", STSConstants.WSP_NS);
        Element endpointRef = doc.createElementNS(STSConstants.WSA_NS_05, "wsa:EndpointReference");
        endpointRef.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns:wsa", STSConstants.WSA_NS_05);
        Element address = doc.createElementNS(STSConstants.WSA_NS_05, "wsa:Address");
        address.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns:wsa", STSConstants.WSA_NS_05);
        address.setTextContent(addressUrl);
        endpointRef.appendChild(address);
        appliesTo.appendChild(endpointRef);
        return appliesTo;
    }

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "stsspass");
        if (unrestrictedPoliciesInstalled) {
            properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "keys/stsstore.jks");
        } else {
            properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "restricted/stsstore.jks");
        }

        return properties;
    }

}
