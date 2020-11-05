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
package org.apache.cxf.sts.token.provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.w3c.dom.Element;

import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.CustomAttributeProvider;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for creating custom SAML Tokens.
 */
public class SAMLProviderCustomTest {

    /**
     * Create a custom Saml1 Attribute Assertion.
     */
    @org.junit.Test
    public void testCustomSaml1AttributeAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);

        List<AttributeStatementProvider> customProviderList = Collections.singletonList(new CustomAttributeProvider());
        ((SAMLTokenProvider)samlTokenProvider).setAttributeStatementProviders(customProviderList);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains("http://cxf.apache.org/sts/custom"));
    }

    /**
     * Create a custom Saml2 Authentication Assertion.
     */
    @org.junit.Test
    public void testCustomSaml2AuthenticationAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);

        List<AuthenticationStatementProvider> customProviderList = Collections.singletonList(
            new CustomAuthenticationProvider());
        ((SAMLTokenProvider)samlTokenProvider).setAuthenticationStatementProviders(customProviderList);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertFalse(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("AuthnStatement"));
        assertTrue(tokenString.contains(SAML2Constants.AUTH_CONTEXT_CLASS_REF_X509));
        assertTrue(tokenString.contains("alice"));
    }

    /**
     * Create a custom Saml1 Authentication Assertion.
     */
    @org.junit.Test
    public void testCustomSaml1AuthenticationAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);

        List<AuthenticationStatementProvider> customProviderList = Collections.singletonList(
            new CustomAuthenticationProvider());
        ((SAMLTokenProvider)samlTokenProvider).setAuthenticationStatementProviders(customProviderList);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertFalse(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains(SAML1Constants.AUTH_METHOD_X509));
        assertTrue(tokenString.contains("alice"));
    }

    /**
     * Create a custom Saml2 Authentication and Attribute Assertion.
     */
    @org.junit.Test
    public void testCustomSaml2CombinedAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);

        List<AuthenticationStatementProvider> customProviderList = Collections.singletonList(
            new CustomAuthenticationProvider());
        ((SAMLTokenProvider)samlTokenProvider).setAuthenticationStatementProviders(customProviderList);

        List<AttributeStatementProvider> customAttributeProviderList = Collections.singletonList(
            new CustomAttributeProvider());
        ((SAMLTokenProvider)samlTokenProvider).setAttributeStatementProviders(customAttributeProviderList);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("AuthnStatement"));
        assertTrue(tokenString.contains("alice"));
    }

    /**
     * Create a custom Saml1 (Multiple) Attribute Assertion.
     */
    @org.junit.Test
    public void testCustomSaml1MultipleAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);

        List<AttributeStatementProvider> customProviderList = Arrays.asList(
            new CustomAttributeProvider(),
            new CustomAttributeProvider());
        ((SAMLTokenProvider)samlTokenProvider).setAttributeStatementProviders(customProviderList);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains("http://cxf.apache.org/sts/custom"));
    }

    /**
     * Create a custom Saml2 AuthDecision Assertion.
     */
    @org.junit.Test
    public void testCustomSaml2AuthDecisionAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);

        List<AuthDecisionStatementProvider> customProviderList = Collections.singletonList(
            new CustomAuthDecisionProvider());
        ((SAMLTokenProvider)samlTokenProvider).setAuthDecisionStatementProviders(customProviderList);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertFalse(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthnStatement"));
        assertTrue(tokenString.contains("AuthzDecisionStatement"));
        assertTrue(tokenString.contains("alice"));
    }

    /**
     * Create a Saml1 Attribute Assertion with a custom Subject
     */
    @org.junit.Test
    public void testCustomSaml1SubjectAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);

        ((SAMLTokenProvider)samlTokenProvider).setSubjectProvider(new CustomSubjectProvider());

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains("http://cxf.apache.org/sts/custom"));
    }

    /**
     * Create a Saml1 Assertion with a custom NameID Format of the Subject
     */
    @org.junit.Test
    public void testCustomSaml1SubjectNameIDFormat() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);

        DefaultSubjectProvider subjectProvider = new DefaultSubjectProvider();
        subjectProvider.setSubjectNameIDFormat(SAML1Constants.NAMEID_FORMAT_EMAIL_ADDRESS);
        ((SAMLTokenProvider)samlTokenProvider).setSubjectProvider(subjectProvider);

        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML1Constants.NAMEID_FORMAT_EMAIL_ADDRESS));
    }

    private TokenProviderParameters createProviderParameters(
        String tokenType, String keyType
    ) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        keyRequirements.setKeyType(keyType);
        parameters.setKeyRequirements(keyRequirements);

        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        parameters.setMessageContext(msgCtx);

        parameters.setAppliesToAddress("http://dummy-service.com/dummy");

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);

        parameters.setEncryptionProperties(new EncryptionProperties());

        return parameters;
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



}
