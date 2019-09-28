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

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.claims.ClaimTypes;
import org.apache.cxf.sts.claims.ClaimsAttributeStatementProvider;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsManager;
import org.apache.cxf.sts.common.CustomUserClaimsHandler;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedCredential;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.service.StaticService;
import org.apache.cxf.sts.token.delegation.HOKDelegationHandler;
import org.apache.cxf.sts.token.delegation.SAMLDelegationHandler;
import org.apache.cxf.sts.token.delegation.TokenDelegationHandler;
import org.apache.cxf.sts.token.delegation.UsernameTokenDelegationHandler;
import org.apache.cxf.sts.token.provider.AttributeStatementProvider;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.cxf.sts.token.validator.IssuerSAMLRealmCodec;
import org.apache.cxf.sts.token.validator.SAMLTokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.UsernameTokenValidator;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.ClaimsType;
import org.apache.cxf.ws.security.sts.provider.model.OnBehalfOfType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.secext.AttributedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.PasswordString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some unit tests for the issue operation.
 */
public class IssueOnbehalfofUnitTest {

    public static final QName REQUESTED_SECURITY_TOKEN =
        QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityToken(null).getName();

    /**
     * Test to successfully issue a SAML 2 token on-behalf-of a SAML 2 token
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfSaml2() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<>();
        providerList.add(new SAMLTokenProvider());
        issueOperation.setTokenProviders(providerList);

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        validatorList.add(new SAMLTokenValidator());
        issueOperation.setTokenValidators(validatorList);

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        issueOperation.setStsProperties(stsProperties);

        TokenDelegationHandler delegationHandler = new SAMLDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                    QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey", callbackHandler);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        OnBehalfOfType onbehalfof = new OnBehalfOfType();
        onbehalfof.setAny(samlToken);

        JAXBElement<OnBehalfOfType> onbehalfofType =
            new JAXBElement<OnBehalfOfType>(
                    QNameConstants.ON_BEHALF_OF, OnBehalfOfType.class, onbehalfof
            );
        request.getAny().add(onbehalfofType);

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
     * Test to successfully issue a SAML 2 token on-behalf-of a SAML 1 token
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfSaml1() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<>();
        providerList.add(new SAMLTokenProvider());
        issueOperation.setTokenProviders(providerList);

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        validatorList.add(new SAMLTokenValidator());
        issueOperation.setTokenValidators(validatorList);

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        issueOperation.setStsProperties(stsProperties);

        TokenDelegationHandler delegationHandler = new SAMLDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                    QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        OnBehalfOfType onbehalfof = new OnBehalfOfType();
        onbehalfof.setAny(samlToken);

        JAXBElement<OnBehalfOfType> onbehalfofType =
            new JAXBElement<OnBehalfOfType>(
                    QNameConstants.ON_BEHALF_OF, OnBehalfOfType.class, onbehalfof
            );
        request.getAny().add(onbehalfofType);

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
     * Test to successfully issue a SAML 2 token on-behalf-of a SAML 2 Symmetric HOK token
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfSaml2SymmetricHOK() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<>();
        providerList.add(new SAMLTokenProvider());
        issueOperation.setTokenProviders(providerList);

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        validatorList.add(new SAMLTokenValidator());
        issueOperation.setTokenValidators(validatorList);

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                    QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey",
                            callbackHandler, null, STSConstants.SYMMETRIC_KEY_TYPE);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        OnBehalfOfType onbehalfof = new OnBehalfOfType();
        onbehalfof.setAny(samlToken);

        JAXBElement<OnBehalfOfType> onbehalfofType =
            new JAXBElement<OnBehalfOfType>(
                    QNameConstants.ON_BEHALF_OF, OnBehalfOfType.class, onbehalfof
            );
        request.getAny().add(onbehalfofType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token

        // This should fail as the default DelegationHandler does not allow HolderOfKey
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected as HolderOfKey is not allowed by default");
        } catch (STSException ex) {
            // expected
        }

        TokenDelegationHandler delegationHandler = new HOKDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
    }

    /**
     * Test to successfully issue a SAML 2 token on-behalf-of a SAML 1 Symmetric HOK token
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfSaml1SymmetricHOK() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<>();
        providerList.add(new SAMLTokenProvider());
        issueOperation.setTokenProviders(providerList);

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        validatorList.add(new SAMLTokenValidator());
        issueOperation.setTokenValidators(validatorList);

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                    QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey",
                            callbackHandler, null, STSConstants.SYMMETRIC_KEY_TYPE);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        OnBehalfOfType onbehalfof = new OnBehalfOfType();
        onbehalfof.setAny(samlToken);

        JAXBElement<OnBehalfOfType> onbehalfofType =
            new JAXBElement<OnBehalfOfType>(
                    QNameConstants.ON_BEHALF_OF, OnBehalfOfType.class, onbehalfof
            );
        request.getAny().add(onbehalfofType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token

        // This should fail as the default DelegationHandler does not allow HolderOfKey
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected as HolderOfKey is not allowed by default");
        } catch (STSException ex) {
            // expected
        }

        TokenDelegationHandler delegationHandler = new HOKDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
    }

    /**
     * Test to successfully issue a SAML 2 token on-behalf-of a SAML 2 PublicKey HOK token
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfSaml2PublicKeyHOK() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<>();
        providerList.add(new SAMLTokenProvider());
        issueOperation.setTokenProviders(providerList);

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        validatorList.add(new SAMLTokenValidator());
        issueOperation.setTokenValidators(validatorList);

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                    QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey",
                            callbackHandler, null, STSConstants.PUBLIC_KEY_KEYTYPE);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        OnBehalfOfType onbehalfof = new OnBehalfOfType();
        onbehalfof.setAny(samlToken);

        JAXBElement<OnBehalfOfType> onbehalfofType =
            new JAXBElement<OnBehalfOfType>(
                    QNameConstants.ON_BEHALF_OF, OnBehalfOfType.class, onbehalfof
            );
        request.getAny().add(onbehalfofType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token

        // This should fail as the default DelegationHandler does not allow HolderOfKey
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected as HolderOfKey is not allowed by default");
        } catch (STSException ex) {
            // expected
        }

        TokenDelegationHandler delegationHandler = new HOKDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
    }

    /**
     * Test to successfully issue a SAML 2 token on-behalf-of a SAML 1 PublicKey HOK token
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfSaml1PublicKeyHOK() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<>();
        providerList.add(new SAMLTokenProvider());
        issueOperation.setTokenProviders(providerList);

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        validatorList.add(new SAMLTokenValidator());
        issueOperation.setTokenValidators(validatorList);

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                    QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey",
                            callbackHandler, null, STSConstants.PUBLIC_KEY_KEYTYPE);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        OnBehalfOfType onbehalfof = new OnBehalfOfType();
        onbehalfof.setAny(samlToken);

        JAXBElement<OnBehalfOfType> onbehalfofType =
            new JAXBElement<OnBehalfOfType>(
                    QNameConstants.ON_BEHALF_OF, OnBehalfOfType.class, onbehalfof
            );
        request.getAny().add(onbehalfofType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token

        // This should fail as the default DelegationHandler does not allow HolderOfKey
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected as HolderOfKey is not allowed by default");
        } catch (STSException ex) {
            // expected
        }

        TokenDelegationHandler delegationHandler = new HOKDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
    }

    /**
     * Test to unsuccessfully issue a SAML 2 token on-behalf-of a SAML 2 token. The
     * problem is that the Audience Restriction URLs in the original token do not
     * match the AppliesTo address.
     */
    @org.junit.Test
    public void testSaml2AudienceRestriction() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<>();
        providerList.add(new SAMLTokenProvider());
        issueOperation.setTokenProviders(providerList);

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        validatorList.add(new SAMLTokenValidator());
        issueOperation.setTokenValidators(validatorList);

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        issueOperation.setStsProperties(stsProperties);

        TokenDelegationHandler delegationHandler = new SAMLDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                    QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey", callbackHandler);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        OnBehalfOfType onbehalfof = new OnBehalfOfType();
        onbehalfof.setAny(samlToken);

        JAXBElement<OnBehalfOfType> onbehalfofType =
            new JAXBElement<OnBehalfOfType>(
                    QNameConstants.ON_BEHALF_OF, OnBehalfOfType.class, onbehalfof
            );
        request.getAny().add(onbehalfofType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token - this should work
        issueOperation.issue(request, null, msgCtx);

        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy2"));
        // This should fail
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected due to AudienceRestriction");
        } catch (STSException ex) {
            // expected
        }
    }

    /**
     * Test to unsuccessfully issue a SAML 2 token on-behalf-of a SAML 1 token. The
     * problem is that the Audience Restriction URLs in the original token do not
     * match the AppliesTo address.
     */
    @org.junit.Test
    public void testSaml1AudienceRestriction() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<>();
        providerList.add(new SAMLTokenProvider());
        issueOperation.setTokenProviders(providerList);

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        validatorList.add(new SAMLTokenValidator());
        issueOperation.setTokenValidators(validatorList);

        TokenDelegationHandler delegationHandler = new SAMLDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                    QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML_TOKEN_TYPE, crypto, "mystskey", callbackHandler);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        OnBehalfOfType onbehalfof = new OnBehalfOfType();
        onbehalfof.setAny(samlToken);

        JAXBElement<OnBehalfOfType> onbehalfofType =
            new JAXBElement<OnBehalfOfType>(
                    QNameConstants.ON_BEHALF_OF, OnBehalfOfType.class, onbehalfof
            );
        request.getAny().add(onbehalfofType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token - this should work
        issueOperation.issue(request, null, msgCtx);

        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy2"));
        // This should fail
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected due to AudienceRestriction");
        } catch (STSException ex) {
            // expected
        }
    }

    /**
     * Test to successfully issue a SAML 2 token on-behalf-of a UsernameToken
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfUsernameToken() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<>();
        providerList.add(new SAMLTokenProvider());
        issueOperation.setTokenProviders(providerList);

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        validatorList.add(new UsernameTokenValidator());
        issueOperation.setTokenValidators(validatorList);

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                    QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);


        // Create a UsernameToken
        JAXBElement<UsernameTokenType> usernameTokenType = createUsernameToken("alice", "clarinet");
        OnBehalfOfType onbehalfof = new OnBehalfOfType();
        onbehalfof.setAny(usernameTokenType);

        JAXBElement<OnBehalfOfType> onbehalfofType =
            new JAXBElement<OnBehalfOfType>(
                    QNameConstants.ON_BEHALF_OF, OnBehalfOfType.class, onbehalfof
            );
        request.getAny().add(onbehalfofType);


        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token

        // This should fail as the default DelegationHandler does not allow UsernameTokens
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected as UsernameTokens are not accepted for OnBehalfOf by default");
        } catch (STSException ex) {
            // expected
        }

        TokenDelegationHandler delegationHandler = new UsernameTokenDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
    }

    /**
     * Test to successfully issue a SAML 2 token on-behalf-of a UsernameToken
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfInvalidUsernameToken() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<>();
        providerList.add(new SAMLTokenProvider());
        issueOperation.setTokenProviders(providerList);

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        validatorList.add(new UsernameTokenValidator());
        issueOperation.setTokenValidators(validatorList);

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                    QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);


        // Create a UsernameToken without password
        JAXBElement<UsernameTokenType> usernameTokenType = createUsernameToken("alice", null);
        OnBehalfOfType onbehalfof = new OnBehalfOfType();
        onbehalfof.setAny(usernameTokenType);

        JAXBElement<OnBehalfOfType> onbehalfofType =
            new JAXBElement<OnBehalfOfType>(
                    QNameConstants.ON_BEHALF_OF, OnBehalfOfType.class, onbehalfof
            );
        request.getAny().add(onbehalfofType);


        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        TokenDelegationHandler delegationHandler = new UsernameTokenDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        // Issue a token - this will fail as the UsernameToken validation fails
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected as no principal is available to create SAML assertion");
        } catch (STSException ex) {
            // expected
        }
    }


    /**
     * Test to successfully issue a SAML 2 token (realm "B") on-behalf-of a SAML 2 token
     * on-behalf-of token is issued by realm "A".
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfSaml2DifferentRealm() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<>();
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        providerList.add(samlTokenProvider);
        issueOperation.setTokenProviders(providerList);

        TokenDelegationHandler delegationHandler = new SAMLDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        SAMLTokenValidator samlTokenValidator = new SAMLTokenValidator();
        samlTokenValidator.setSamlRealmCodec(new IssuerSAMLRealmCodec());
        validatorList.add(samlTokenValidator);
        issueOperation.setTokenValidators(validatorList);

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        stsProperties.setRealmParser(new CustomRealmParser());
        stsProperties.setIdentityMapper(new CustomIdentityMapper());
        issueOperation.setStsProperties(stsProperties);

        Map<String, RealmProperties> realms = createSamlRealms();

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey",
                    callbackHandler, realms, STSConstants.BEARER_KEY_KEYTYPE);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        OnBehalfOfType onbehalfof = new OnBehalfOfType();
        onbehalfof.setAny(samlToken);

        JAXBElement<OnBehalfOfType> onbehalfofType =
            new JAXBElement<OnBehalfOfType>(
                    QNameConstants.ON_BEHALF_OF, OnBehalfOfType.class, onbehalfof
            );
        request.getAny().add(onbehalfofType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put("url", "https");

        // Validate a token - this will fail as the tokenProvider doesn't understand how to handle
        // realm "B"
        //try {
        //    issueOperation.issue(request, msgCtx);
        //} catch (STSException ex) {
        //    // expected
        //}

        samlTokenProvider.setRealmMap(realms);

        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponseList =
            response.getRequestSecurityTokenResponse();

        assertFalse(securityTokenResponseList.isEmpty());
        RequestSecurityTokenResponseType securityTokenResponse = securityTokenResponseList.get(0);

        // Test the generated token.
        Element assertion = null;
        for (Object tokenObject : securityTokenResponse.getAny()) {
            if (tokenObject instanceof JAXBElement<?>
                && REQUESTED_SECURITY_TOKEN.equals(((JAXBElement<?>)tokenObject).getName())) {
                RequestedSecurityTokenType rstType =
                    (RequestedSecurityTokenType)((JAXBElement<?>)tokenObject).getValue();
                assertion = (Element)rstType.getAny();
                break;
            }
        }

        assertNotNull(assertion);
        String tokenString = DOM2Writer.nodeToString(assertion);
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("ALICE"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
    }

    /**
     * Test to successfully issue a SAML 2 token on-behalf-of a SAML 2 token
     * but WS-Security user different than on-behalf-of subject
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfSaml2DifferentWSUser() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<>();
        providerList.add(new SAMLTokenProvider());
        issueOperation.setTokenProviders(providerList);

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        validatorList.add(new SAMLTokenValidator());
        issueOperation.setTokenValidators(validatorList);

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        issueOperation.setStsProperties(stsProperties);

        TokenDelegationHandler delegationHandler = new SAMLDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                    QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey", callbackHandler);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        OnBehalfOfType onbehalfof = new OnBehalfOfType();
        onbehalfof.setAny(samlToken);

        JAXBElement<OnBehalfOfType> onbehalfofType =
            new JAXBElement<OnBehalfOfType>(
                    QNameConstants.ON_BEHALF_OF, OnBehalfOfType.class, onbehalfof
            );
        request.getAny().add(onbehalfofType);

        // Mock up message context with user 'bob'
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        Principal principal = new CustomTokenPrincipal("bob");
        msgCtx.put(
                SecurityContext.class.getName(),
                createSecurityContext(principal)
        );

        // Issue a token
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, principal, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());

        // Test the generated token.
        Element assertion = null;
        for (Object tokenObject : securityTokenResponse.get(0).getAny()) {
            if (tokenObject instanceof JAXBElement<?>
                && REQUESTED_SECURITY_TOKEN.equals(((JAXBElement<?>)tokenObject).getName())) {
                RequestedSecurityTokenType rstType =
                    (RequestedSecurityTokenType)((JAXBElement<?>)tokenObject).getValue();
                assertion = (Element)rstType.getAny();
                break;
            }
        }

        assertNotNull(assertion);
        String tokenString = DOM2Writer.nodeToString(assertion);
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
        SamlAssertionWrapper assertionWrapper = new SamlAssertionWrapper(assertion);
        assertEquals(assertionWrapper.getSaml2().getSubject().getNameID().getValue().toLowerCase(), "alice");
    }

    /**
     * Test to successfully issue a SAML 2 token on-behalf-of a SAML 2 token
     * but WS-Security user different than on-behalf-of subject
     * and request claims
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfSaml2DifferentWSUserAndClaims() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<>();
        List<AttributeStatementProvider> customProviderList =
            new ArrayList<>();
        customProviderList.add(new ClaimsAttributeStatementProvider());
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        samlTokenProvider.setAttributeStatementProviders(customProviderList);
        providerList.add(samlTokenProvider);
        issueOperation.setTokenProviders(providerList);

        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<>();
        validatorList.add(new SAMLTokenValidator());
        issueOperation.setTokenValidators(validatorList);

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        issueOperation.setStsProperties(stsProperties);

        TokenDelegationHandler delegationHandler = new SAMLDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        // Set the ClaimsManager
        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomUserClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        issueOperation.setClaimsManager(claimsManager);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                    QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);

        // Add a ClaimsType
        ClaimsType claimsType = new ClaimsType();
        claimsType.setDialect(STSConstants.IDT_NS_05_05);

        Document docx = DOMUtils.createDocument();
        Element claimType = createClaimsType(docx);
        claimsType.getAny().add(claimType);

        JAXBElement<ClaimsType> claimsTypeJaxb =
            new JAXBElement<ClaimsType>(
                    QNameConstants.CLAIMS, ClaimsType.class, claimsType
            );
        request.getAny().add(claimsTypeJaxb);

        // Get a SAML Token via the SAMLTokenProvider
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey", callbackHandler);
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        OnBehalfOfType onbehalfof = new OnBehalfOfType();
        onbehalfof.setAny(samlToken);

        JAXBElement<OnBehalfOfType> onbehalfofType =
            new JAXBElement<OnBehalfOfType>(
                    QNameConstants.ON_BEHALF_OF, OnBehalfOfType.class, onbehalfof
            );
        request.getAny().add(onbehalfofType);

        // Mock up message context with user 'bob'
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        Principal principal = new CustomTokenPrincipal("bob");
        msgCtx.put(
                SecurityContext.class.getName(),
                createSecurityContext(principal)
        );

        // Issue a token
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, principal, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());

        // Test the generated token.
        Element assertion = null;
        for (Object tokenObject : securityTokenResponse.get(0).getAny()) {
            if (tokenObject instanceof JAXBElement<?>
                && REQUESTED_SECURITY_TOKEN.equals(((JAXBElement<?>)tokenObject).getName())) {
                RequestedSecurityTokenType rstType =
                    (RequestedSecurityTokenType)((JAXBElement<?>)tokenObject).getValue();
                assertion = (Element)rstType.getAny();
                break;
            }
        }

        assertNotNull(assertion);
        String tokenString = DOM2Writer.nodeToString(assertion);
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
        assertTrue(tokenString.toLowerCase().contains("aliceclaim"));
        SamlAssertionWrapper assertionWrapper = new SamlAssertionWrapper(assertion);
        assertEquals(assertionWrapper.getSaml2().getSubject().getNameID().getValue().toLowerCase(), "alice");
    }


    /*
     * Mock up an SAML assertion element
     */
    private Element createSAMLAssertion(
            String tokenType, Crypto crypto, String signatureUsername, CallbackHandler callbackHandler
    ) throws WSSecurityException {
        return createSAMLAssertion(tokenType, crypto, signatureUsername,
                                   callbackHandler, null, STSConstants.BEARER_KEY_KEYTYPE);
    }

    /*
     * Mock up an SAML assertion element
     */
    private Element createSAMLAssertion(
            String tokenType, Crypto crypto, String signatureUsername, CallbackHandler callbackHandler,
            Map<String, RealmProperties> realms, String keyType
    ) throws WSSecurityException {
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        samlTokenProvider.setRealmMap(realms);

        TokenProviderParameters providerParameters =
            createProviderParameters(
                    tokenType, keyType, crypto, signatureUsername, callbackHandler
            );
        if (realms != null) {
            providerParameters.setRealm("A");
        }
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        return (Element)providerResponse.getToken();
    }

    private TokenProviderParameters createProviderParameters(
            String tokenType, String keyType, Crypto crypto,
            String signatureUsername, CallbackHandler callbackHandler
    ) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        keyRequirements.setKeyType(keyType);

        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("myclientkey");
        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
        ReceivedCredential receivedCredential = new ReceivedCredential();
        receivedCredential.setX509Cert(certs[0]);
        keyRequirements.setReceivedCredential(receivedCredential);

        parameters.setKeyRequirements(keyRequirements);

        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        parameters.setMessageContext(msgCtx);

        parameters.setAppliesToAddress("http://dummy-service.com/dummy");

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setSignatureUsername(signatureUsername);
        stsProperties.setCallbackHandler(callbackHandler);
        stsProperties.setIssuer("STS");
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setEncryptionCrypto(crypto);
        parameters.setStsProperties(stsProperties);

        parameters.setEncryptionProperties(new EncryptionProperties());

        return parameters;
    }

    private JAXBElement<UsernameTokenType> createUsernameToken(String name, String password) {
        UsernameTokenType usernameToken = new UsernameTokenType();
        AttributedString username = new AttributedString();
        username.setValue(name);
        usernameToken.setUsername(username);

        // Add a password
        if (password != null) {
            PasswordString passwordString = new PasswordString();
            passwordString.setValue(password);
            passwordString.setType(WSS4JConstants.PASSWORD_TEXT);
            JAXBElement<PasswordString> passwordType =
                new JAXBElement<PasswordString>(
                        QNameConstants.PASSWORD, PasswordString.class, passwordString
                );
            usernameToken.getAny().add(passwordType);
        }

        return new JAXBElement<UsernameTokenType>(
                    QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameToken
            );
    }

    private Map<String, RealmProperties> createSamlRealms() {
        // Create Realms
        Map<String, RealmProperties> samlRealms = new HashMap<>();
        RealmProperties samlRealm = new RealmProperties();
        samlRealm.setIssuer("A-Issuer");
        samlRealms.put("A", samlRealm);
        samlRealm = new RealmProperties();
        samlRealm.setIssuer("B-Issuer");
        samlRealms.put("B", samlRealm);
        return samlRealms;
    }

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
                "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "keys/stsstore.jks");

        return properties;
    }

    /*
     * Create a security context object
     */
    private SecurityContext createSecurityContext(final Principal p) {
        return new SecurityContext() {
            public Principal getUserPrincipal() {
                return p;
            }
            public boolean isUserInRole(String role) {
                return false;
            }
        };
    }

    private Element createClaimsType(Document doc) {
        Element claimType = doc.createElementNS(STSConstants.IDT_NS_05_05, "ClaimType");
        claimType.setAttributeNS(
                null, "Uri", ClaimTypes.FIRSTNAME.toString()
        );
        claimType.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns", STSConstants.IDT_NS_05_05);

        return claimType;
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
}
