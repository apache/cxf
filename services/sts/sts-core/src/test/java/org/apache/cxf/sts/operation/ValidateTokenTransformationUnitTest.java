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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.claims.ClaimTypes;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsManager; 
import org.apache.cxf.sts.common.CustomAttributeProvider;
import org.apache.cxf.sts.common.CustomClaimsHandler;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.token.provider.AttributeStatementProvider;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.realm.SAMLRealm;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.UsernameTokenValidator;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.ClaimsType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.StatusType;
import org.apache.cxf.ws.security.sts.provider.model.ValidateTargetType;
import org.apache.cxf.ws.security.sts.provider.model.secext.AttributedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.PasswordString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.ws.security.CustomTokenPrincipal;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.apache.ws.security.util.DOM2Writer;

/**
 * In this test, a token (UsernameToken) is validated and transformed into a SAML Assertion.
 */
public class ValidateTokenTransformationUnitTest extends org.junit.Assert {
    
    public static final QName REQUESTED_SECURITY_TOKEN = 
        QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityToken(null).getName();
    private static final QName QNAME_WST_STATUS = 
        QNameConstants.WS_TRUST_FACTORY.createStatus(null).getName();
    
    /**
     * Test to successfully validate a UsernameToken and transform it into a SAML Assertion.
     */
    @org.junit.Test
    public void testUsernameTokenTransformation() throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();
        
        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<TokenValidator>();
        validatorList.add(new UsernameTokenValidator());
        validateOperation.setTokenValidators(validatorList);

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        providerList.add(new SAMLTokenProvider());
        validateOperation.setTokenProviders(providerList);
        
        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        validateOperation.setStsProperties(stsProperties);
        
        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType = 
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, WSConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        
        // Create a UsernameToken
        JAXBElement<UsernameTokenType> usernameTokenType = createUsernameToken("alice", "clarinet");
        ValidateTargetType validateTarget = new ValidateTargetType();
        validateTarget.setAny(usernameTokenType);
        
        JAXBElement<ValidateTargetType> validateTargetType = 
            new JAXBElement<ValidateTargetType>(
                QNameConstants.VALIDATE_TARGET, ValidateTargetType.class, validateTarget
            );
        request.getAny().add(validateTargetType);
        
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put(
            SecurityContext.class.getName(), 
            createSecurityContext(new CustomTokenPrincipal("alice"))
        );
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Validate a token
        RequestSecurityTokenResponseType response = 
            validateOperation.validate(request, webServiceContext);
        assertTrue(validateResponse(response));
        
        // Test the generated token.
        Element assertion = null;
        for (Object tokenObject : response.getAny()) {
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
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
    }
    
    /**
     * Test to successfully validate a UsernameToken (which was issued in realm "A") and 
     * transform it into a SAML Assertion in Realm "B".
     */
    @org.junit.Test
    public void testUsernameTokenTransformationRealm() throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();
        
        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<TokenValidator>();
        UsernameTokenValidator validator = new UsernameTokenValidator();
        validator.setUsernameTokenRealmCodec(new CustomUsernameTokenRealmCodec());
        validatorList.add(validator);
        validateOperation.setTokenValidators(validatorList);

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        providerList.add(samlTokenProvider);
        validateOperation.setTokenProviders(providerList);
        
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
        validateOperation.setStsProperties(stsProperties);
        
        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType = 
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, WSConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        
        // Create a UsernameToken
        JAXBElement<UsernameTokenType> usernameTokenType = createUsernameToken("alice", "clarinet");
        ValidateTargetType validateTarget = new ValidateTargetType();
        validateTarget.setAny(usernameTokenType);
        
        JAXBElement<ValidateTargetType> validateTargetType = 
            new JAXBElement<ValidateTargetType>(
                QNameConstants.VALIDATE_TARGET, ValidateTargetType.class, validateTarget
            );
        request.getAny().add(validateTargetType);
        
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put(
            SecurityContext.class.getName(), 
            createSecurityContext(new CustomTokenPrincipal("alice"))
        );
        msgCtx.put("url", "https");
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Validate a token - this will fail as the tokenProvider doesn't understand how to handle
        // realm "B"
        try {
            validateOperation.validate(request, webServiceContext);
        } catch (STSException ex) {
            // expected
        }
        
        samlTokenProvider.setRealmMap(getSamlRealms());
        RequestSecurityTokenResponseType response = validateOperation.validate(request, webServiceContext);
        assertTrue(validateResponse(response));
        
        // Test the generated token.
        Element assertion = null;
        for (Object tokenObject : response.getAny()) {
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
     * Test to successfully validate a UsernameToken and transform it into a SAML Assertion.
     * Required claims are a child of RST element
     */
    @org.junit.Test
    public void testUsernameTokenTransformationClaims() throws Exception {
        runUsernameTokenTransformationClaims(false);
    }
    
    /**
     * Test to successfully validate a UsernameToken and transform it into a SAML Assertion.
     * Required claims are a child of SecondaryParameters element
     */
    @org.junit.Test
    public void testUsernameTokenTransformationClaimsSecondaryParameters() throws Exception {
        runUsernameTokenTransformationClaims(true);
    }
    
    /**
     * Test to successfully validate a UsernameToken and transform it into a SAML Assertion with claims.
     */
    private void runUsernameTokenTransformationClaims(boolean useSecondaryParameters) throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();
        
        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<TokenValidator>();
        validatorList.add(new UsernameTokenValidator());
        validateOperation.setTokenValidators(validatorList);

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
       
        List<AttributeStatementProvider> customProviderList = 
            new ArrayList<AttributeStatementProvider>();
        customProviderList.add(new CustomAttributeProvider());
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        samlTokenProvider.setAttributeStatementProviders(customProviderList);
        providerList.add(samlTokenProvider);
        validateOperation.setTokenProviders(providerList);
        
        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        validateOperation.setStsProperties(stsProperties);
        
        // Set the ClaimsManager
        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        validateOperation.setClaimsManager(claimsManager);
        
        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType = 
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, WSConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        Object claims = 
            useSecondaryParameters ? createClaimsElementInSecondaryParameters() : createClaimsElement();
            
        request.getAny().add(claims);
        
        // Create a UsernameToken
        JAXBElement<UsernameTokenType> usernameTokenType = createUsernameToken("alice", "clarinet");
        ValidateTargetType validateTarget = new ValidateTargetType();
        validateTarget.setAny(usernameTokenType);
        
        JAXBElement<ValidateTargetType> validateTargetType = 
            new JAXBElement<ValidateTargetType>(
                QNameConstants.VALIDATE_TARGET, ValidateTargetType.class, validateTarget
            );
        request.getAny().add(validateTargetType);
        
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put(
            SecurityContext.class.getName(), 
            createSecurityContext(new CustomTokenPrincipal("alice"))
        );
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Validate a token
        RequestSecurityTokenResponseType response = 
            validateOperation.validate(request, webServiceContext);
        assertTrue(validateResponse(response));
        
        // Test the generated token.
        Element assertion = null;
        for (Object tokenObject : response.getAny()) {
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
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
        assertTrue(tokenString.contains(ClaimTypes.FIRSTNAME.toString()));
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
    
    private Map<String, SAMLRealm> getSamlRealms() {
        // Create Realms
        Map<String, SAMLRealm> samlRealms = new HashMap<String, SAMLRealm>();
        SAMLRealm samlRealm = new SAMLRealm();
        samlRealm.setIssuer("A-Issuer");
        samlRealms.put("A", samlRealm);
        samlRealm = new SAMLRealm();
        samlRealm.setIssuer("B-Issuer");
        samlRealms.put("B", samlRealm);
        return samlRealms;
    }
    
    /**
     * Return true if the response has a valid status, false otherwise
     */
    private boolean validateResponse(RequestSecurityTokenResponseType response) {
        assertTrue(response != null && response.getAny() != null && !response.getAny().isEmpty());
        
        for (Object requestObject : response.getAny()) {
            if (requestObject instanceof JAXBElement<?>) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>) requestObject;
                if (QNAME_WST_STATUS.equals(jaxbElement.getName())) {
                    StatusType status = (StatusType)jaxbElement.getValue();
                    if (STSConstants.VALID_CODE.equals(status.getCode())) {
                        return true;
                    }
                }
            }
        }
        return false;
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
    
    private JAXBElement<UsernameTokenType> createUsernameToken(String name, String password) {
        UsernameTokenType usernameToken = new UsernameTokenType();
        AttributedString username = new AttributedString();
        username.setValue(name);
        usernameToken.setUsername(username);
        
        // Add a password
        PasswordString passwordString = new PasswordString();
        passwordString.setValue(password);
        passwordString.setType(WSConstants.PASSWORD_TEXT);
        JAXBElement<PasswordString> passwordType = 
            new JAXBElement<PasswordString>(
                QNameConstants.PASSWORD, PasswordString.class, passwordString
            );
        usernameToken.getAny().add(passwordType);
        
        JAXBElement<UsernameTokenType> tokenType = 
            new JAXBElement<UsernameTokenType>(
                QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameToken
            );
        
        return tokenType;
    }
    
    /*
     * Mock up a DOM Element containing some claims
     */
    private JAXBElement<ClaimsType> createClaimsElement() {
        Document doc = DOMUtils.createDocument();
        
        Element claimType = createClaimsType(doc);        
       
        ClaimsType claims = new ClaimsType();
        claims.setDialect(STSConstants.IDT_NS_05_05);
        claims.getAny().add(claimType);
        
        JAXBElement<ClaimsType> claimsType =
            new JAXBElement<ClaimsType>(
                    QNameConstants.CLAIMS, ClaimsType.class, claims
            );
        
        return claimsType;
    }
    
    /*
     * Mock up a SecondaryParameters DOM Element containing some claims
     */
    private Element createClaimsElementInSecondaryParameters() {
        Document doc = DOMUtils.createDocument();
        Element secondary = doc.createElementNS(STSConstants.WST_NS_05_12, "SecondaryParameters");
        secondary.setAttributeNS(WSConstants.XMLNS_NS, "xmlns", STSConstants.WST_NS_05_12);
        
        Element claims = doc.createElementNS(STSConstants.WST_NS_05_12, "Claims");
        claims.setAttributeNS(null, "Dialect", STSConstants.IDT_NS_05_05);
        
        Element claimType = createClaimsType(doc);
        
        claims.appendChild(claimType);
        secondary.appendChild(claims);

        return secondary;
    }
    
    private Element createClaimsType(Document doc) {
        Element claimType = doc.createElementNS(STSConstants.IDT_NS_05_05, "ClaimType");
        claimType.setAttributeNS(
            null, "Uri", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname"
        );
        claimType.setAttributeNS(WSConstants.XMLNS_NS, "xmlns", STSConstants.IDT_NS_05_05);
        
        return claimType;
    }
    
}
