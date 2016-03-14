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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBElement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.common.TestUtils;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.service.StaticService;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.dom.WSConstants;

/**
 * Some unit tests for issuing encrypted tokens.
 */
public class IssueEncryptedUnitTest extends org.junit.Assert {
    
    private static boolean unrestrictedPoliciesInstalled;
    
    static {
        unrestrictedPoliciesInstalled = TestUtils.checkUnrestrictedPoliciesInstalled();
    };
    
    /**
     * Test to successfully issue a (dummy) encrypted token.
     */
    @org.junit.Test
    public void testIssueEncryptedToken() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setEncryptIssuedToken(true);
        
        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        providerList.add(new DummyTokenProvider());
        issueOperation.setTokenProviders(providerList);
        
        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        if (!unrestrictedPoliciesInstalled) {
            encryptionProperties.setEncryptionAlgorithm(WSConstants.AES_128);
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
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Issue a token
        RequestSecurityTokenResponseCollectionType response = 
            issueOperation.issue(request, webServiceContext);
        List<RequestSecurityTokenResponseType> securityTokenResponse = 
            response.getRequestSecurityTokenResponse();
        assertTrue(!securityTokenResponse.isEmpty());
    }
    
    /**
     * Test for various options relating to specifying a name for encryption
     */
    @org.junit.Test
    public void testEncryptionName() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setEncryptIssuedToken(true);
        
        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        providerList.add(new DummyTokenProvider());
        issueOperation.setTokenProviders(providerList);
        
        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        if (!unrestrictedPoliciesInstalled) {
            encryptionProperties.setEncryptionAlgorithm(WSConstants.AES_128);
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
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Issue a token - as no encryption name has been specified the token will not be encrypted
        RequestSecurityTokenResponseCollectionType response = 
            issueOperation.issue(request, webServiceContext);
        List<RequestSecurityTokenResponseType> securityTokenResponse = 
            response.getRequestSecurityTokenResponse();
        assertTrue(!securityTokenResponse.isEmpty());
        
        encryptionProperties.setEncryptionName("myservicekey");
        service.setEncryptionProperties(encryptionProperties);
        
        // Issue a (encrypted) token
        response = issueOperation.issue(request, webServiceContext);
        securityTokenResponse = response.getRequestSecurityTokenResponse();
        assertTrue(!securityTokenResponse.isEmpty());
    }
    
    
    /**
     * Test for various options relating to configuring an algorithm for encryption
     */
    @org.junit.Test
    public void testConfiguredEncryptionAlgorithm() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setEncryptIssuedToken(true);
        
        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        providerList.add(new DummyTokenProvider());
        issueOperation.setTokenProviders(providerList);
        
        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEncryptionName("myservicekey");
        encryptionProperties.setEncryptionAlgorithm(WSConstants.AES_128);
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
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Issue a token - this should use a (new) default encryption algorithm as configured
        RequestSecurityTokenResponseCollectionType response = 
            issueOperation.issue(request, webServiceContext);
        List<RequestSecurityTokenResponseType> securityTokenResponse = 
            response.getRequestSecurityTokenResponse();
        assertTrue(!securityTokenResponse.isEmpty());
        
        encryptionProperties.setEncryptionAlgorithm(WSConstants.KEYTRANSPORT_RSA15);
        try {
            issueOperation.issue(request, webServiceContext);
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
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        providerList.add(new DummyTokenProvider());
        issueOperation.setTokenProviders(providerList);
        
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
                QNameConstants.ENCRYPTION_ALGORITHM, String.class, WSConstants.AES_128
            );
        request.getAny().add(encryptionAlgorithmType);
        
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Issue a token
        RequestSecurityTokenResponseCollectionType response = 
            issueOperation.issue(request, webServiceContext);
        List<RequestSecurityTokenResponseType> securityTokenResponse = 
            response.getRequestSecurityTokenResponse();
        assertTrue(!securityTokenResponse.isEmpty());
        
        // Now specify a non-supported algorithm
        List<String> acceptedAlgorithms = Collections.singletonList(WSConstants.KEYTRANSPORT_RSA15);
        encryptionProperties.setAcceptedEncryptionAlgorithms(acceptedAlgorithms);
        request.getAny().remove(request.getAny().size() - 1);
        encryptionAlgorithmType = 
            new JAXBElement<String>(
                QNameConstants.ENCRYPTION_ALGORITHM, String.class, WSConstants.KEYTRANSPORT_RSA15
            );
        request.getAny().add(encryptionAlgorithmType);
        try {
            issueOperation.issue(request, webServiceContext);
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
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        providerList.add(new DummyTokenProvider());
        issueOperation.setTokenProviders(providerList);
        
        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEncryptionName("myservicekey");
        if (!unrestrictedPoliciesInstalled) {
            encryptionProperties.setEncryptionAlgorithm(WSConstants.AES_128);
        }
        encryptionProperties.setKeyWrapAlgorithm(WSConstants.KEYTRANSPORT_RSAOEP);
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
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Issue a token - this should use a (new) default key-wrap algorithm as configured
        RequestSecurityTokenResponseCollectionType response = 
            issueOperation.issue(request, webServiceContext);
        List<RequestSecurityTokenResponseType> securityTokenResponse = 
            response.getRequestSecurityTokenResponse();
        assertTrue(!securityTokenResponse.isEmpty());
        
        encryptionProperties.setKeyWrapAlgorithm(WSConstants.AES_128);
        try {
            issueOperation.issue(request, webServiceContext);
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
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        providerList.add(new DummyTokenProvider());
        issueOperation.setTokenProviders(providerList);
        
        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEncryptionName("myservicekey");
        if (!unrestrictedPoliciesInstalled) {
            encryptionProperties.setEncryptionAlgorithm(WSConstants.AES_128);
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
                QNameConstants.KEYWRAP_ALGORITHM, String.class, WSConstants.KEYTRANSPORT_RSAOEP
            );
        request.getAny().add(encryptionAlgorithmType);
        
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Issue a token
        RequestSecurityTokenResponseCollectionType response = 
            issueOperation.issue(request, webServiceContext);
        List<RequestSecurityTokenResponseType> securityTokenResponse = 
            response.getRequestSecurityTokenResponse();
        assertTrue(!securityTokenResponse.isEmpty());
        
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
            issueOperation.issue(request, webServiceContext);
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
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        providerList.add(new DummyTokenProvider());
        issueOperation.setTokenProviders(providerList);
        
        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        encryptionProperties.setEncryptionName("myservicekey");
        if (!unrestrictedPoliciesInstalled) {
            encryptionProperties.setEncryptionAlgorithm(WSConstants.AES_128);
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
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Issue a token - use various KeyIdentifiers
        RequestSecurityTokenResponseCollectionType response = 
            issueOperation.issue(request, webServiceContext);
        List<RequestSecurityTokenResponseType> securityTokenResponse = 
            response.getRequestSecurityTokenResponse();
        assertTrue(!securityTokenResponse.isEmpty());
        
        encryptionProperties.setKeyIdentifierType(WSConstants.SKI_KEY_IDENTIFIER);
        issueOperation.issue(request, webServiceContext);
        
        encryptionProperties.setKeyIdentifierType(WSConstants.THUMBPRINT_IDENTIFIER);
        issueOperation.issue(request, webServiceContext);
        
        encryptionProperties.setKeyIdentifierType(WSConstants.ENCRYPTED_KEY_SHA1_IDENTIFIER);
        issueOperation.issue(request, webServiceContext);
        
        try {
            encryptionProperties.setKeyIdentifierType(WSConstants.BST);
            issueOperation.issue(request, webServiceContext);
            fail("Failure expected on a bad key identifier");
        } catch (STSException ex) {
            // expected
        }
    }
    
    
    /*
     * Mock up an AppliesTo element using the supplied address
     */
    private Element createAppliesToElement(String addressUrl) {
        Document doc = DOMUtils.createDocument();
        Element appliesTo = doc.createElementNS(STSConstants.WSP_NS, "wsp:AppliesTo");
        appliesTo.setAttributeNS(WSConstants.XMLNS_NS, "xmlns:wsp", STSConstants.WSP_NS);
        Element endpointRef = doc.createElementNS(STSConstants.WSA_NS_05, "wsa:EndpointReference");
        endpointRef.setAttributeNS(WSConstants.XMLNS_NS, "xmlns:wsa", STSConstants.WSA_NS_05);
        Element address = doc.createElementNS(STSConstants.WSA_NS_05, "wsa:Address");
        address.setAttributeNS(WSConstants.XMLNS_NS, "xmlns:wsa", STSConstants.WSA_NS_05);
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
            properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "stsstore.jks");
        } else {
            properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "restricted/stsstore.jks");
        }
        
        return properties;
    }
    
}
