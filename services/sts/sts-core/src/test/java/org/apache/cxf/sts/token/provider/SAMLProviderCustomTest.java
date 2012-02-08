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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.w3c.dom.Element;

import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.CustomAttributeProvider;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.ws.security.CustomTokenPrincipal;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.saml.ext.builder.SAML1Constants;
import org.apache.ws.security.util.DOM2Writer;

/**
 * Some unit tests for creating custom SAML Tokens.
 */
public class SAMLProviderCustomTest extends org.junit.Assert {
    
    /**
     * Create a custom Saml1 Attribute Assertion.
     */
    @org.junit.Test
    public void testCustomSaml1AttributeAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters = 
            createProviderParameters(WSConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        
        List<AttributeStatementProvider> customProviderList = new ArrayList<AttributeStatementProvider>();
        customProviderList.add(new CustomAttributeProvider());
        ((SAMLTokenProvider)samlTokenProvider).setAttributeStatementProviders(customProviderList);
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        
        Element token = providerResponse.getToken();
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
            createProviderParameters(WSConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        
        List<AuthenticationStatementProvider> customProviderList = 
            new ArrayList<AuthenticationStatementProvider>();
        customProviderList.add(new CustomAuthenticationProvider());
        ((SAMLTokenProvider)samlTokenProvider).setAuthenticationStatementProviders(customProviderList);
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        
        Element token = providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertFalse(tokenString.contains("AttributeStatement"));
        assertTrue(tokenString.contains("AuthnStatement"));
        assertTrue(tokenString.contains("alice"));
    }
    
    /**
     * Create a custom Saml2 Authentication and Attribute Assertion.
     */
    @org.junit.Test
    public void testCustomSaml2CombinedAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters = 
            createProviderParameters(WSConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        
        List<AuthenticationStatementProvider> customProviderList = 
            new ArrayList<AuthenticationStatementProvider>();
        customProviderList.add(new CustomAuthenticationProvider());
        ((SAMLTokenProvider)samlTokenProvider).setAuthenticationStatementProviders(customProviderList);
        
        List<AttributeStatementProvider> customAttributeProviderList = 
            new ArrayList<AttributeStatementProvider>();
        customAttributeProviderList.add(new CustomAttributeProvider());
        ((SAMLTokenProvider)samlTokenProvider).setAttributeStatementProviders(customAttributeProviderList);
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        
        Element token = providerResponse.getToken();
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
            createProviderParameters(WSConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        
        List<AttributeStatementProvider> customProviderList = new ArrayList<AttributeStatementProvider>();
        customProviderList.add(new CustomAttributeProvider());
        customProviderList.add(new CustomAttributeProvider());
        ((SAMLTokenProvider)samlTokenProvider).setAttributeStatementProviders(customProviderList);
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        
        Element token = providerResponse.getToken();
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
            createProviderParameters(WSConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        
        List<AuthDecisionStatementProvider> customProviderList = 
            new ArrayList<AuthDecisionStatementProvider>();
        customProviderList.add(new CustomAuthDecisionProvider());
        ((SAMLTokenProvider)samlTokenProvider).setAuthDecisionStatementProviders(customProviderList);
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        
        Element token = providerResponse.getToken();
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
            createProviderParameters(WSConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        
        ((SAMLTokenProvider)samlTokenProvider).setSubjectProvider(new CustomSubjectProvider());
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        
        Element token = providerResponse.getToken();
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
            createProviderParameters(WSConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        
        DefaultSubjectProvider subjectProvider = new DefaultSubjectProvider();
        subjectProvider.setSubjectNameIDFormat(SAML1Constants.NAMEID_FORMAT_EMAIL_ADDRESS);
        ((SAMLTokenProvider)samlTokenProvider).setSubjectProvider(subjectProvider);
        
        assertTrue(samlTokenProvider.canHandleToken(WSConstants.WSS_SAML_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
        
        Element token = providerResponse.getToken();
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
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        parameters.setWebServiceContext(webServiceContext);
        
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
            "org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin"
        );
        properties.put("org.apache.ws.security.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.ws.security.crypto.merlin.keystore.file", "stsstore.jks");
        
        return properties;
    }
    
  
    
}
