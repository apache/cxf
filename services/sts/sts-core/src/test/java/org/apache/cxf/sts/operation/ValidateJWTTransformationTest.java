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

import javax.security.auth.callback.CallbackHandler;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.claims.ClaimTypes;
import org.apache.cxf.sts.claims.ClaimsAttributeStatementProvider;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsManager;
import org.apache.cxf.sts.common.CustomClaimsHandler;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.token.provider.AttributeStatementProvider;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.provider.jwt.JWTTokenProvider;
import org.apache.cxf.sts.token.realm.JWTRealmCodec;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.cxf.sts.token.validator.SAMLTokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.jwt.JWTTokenValidator;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.StatusType;
import org.apache.cxf.ws.security.sts.provider.model.ValidateTargetType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.dom.WSConstants;
import org.junit.Assert;

/**
 * This tests validating a JWT Token + transforming into a SAML token, and vice versa.
 */
public class ValidateJWTTransformationTest extends org.junit.Assert {
    
    public static final QName REQUESTED_SECURITY_TOKEN = 
        QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityToken(null).getName();
    private static final QName QNAME_WST_STATUS = 
        QNameConstants.WS_TRUST_FACTORY.createStatus(null).getName();
    
    @org.junit.Test
    public void testJWTToSAMLTransformation() throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();
        
        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<TokenValidator>();
        validatorList.add(new JWTTokenValidator());
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
        
        // Create a JWTToken
        TokenProviderResponse providerResponse = createJWT();
        Element wrapper = createTokenWrapper((String)providerResponse.getToken());
        Document doc = wrapper.getOwnerDocument();
        wrapper = (Element)doc.appendChild(wrapper);
        
        ValidateTargetType validateTarget = new ValidateTargetType();
        validateTarget.setAny(wrapper);
        
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
    
    @org.junit.Test
    public void testJWTToSAMLTransformationRealm() throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();
        
        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<TokenValidator>();
        JWTTokenValidator validator = new JWTTokenValidator();
        validator.setRealmCodec(new CustomJWTRealmCodec());
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
        
        // Create a JWTToken
        TokenProviderResponse providerResponse = createJWT();
        Element wrapper = createTokenWrapper((String)providerResponse.getToken());
        Document doc = wrapper.getOwnerDocument();
        wrapper = (Element)doc.appendChild(wrapper);
        
        ValidateTargetType validateTarget = new ValidateTargetType();
        validateTarget.setAny(wrapper);
        
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
        
        samlTokenProvider.setRealmMap(createSamlRealms());
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
    
    @org.junit.Test
    public void testSAMLToJWTTransformation() throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();
        
        // Add Token Validator
        List<TokenValidator> validatorList = new ArrayList<TokenValidator>();
        validatorList.add(new SAMLTokenValidator());
        validateOperation.setTokenValidators(validatorList);

        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        providerList.add(new JWTTokenProvider());
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
                QNameConstants.TOKEN_TYPE, String.class, JWTTokenProvider.JWT_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        
        // Create a SAML Token
        Element samlToken = 
            createSAMLAssertion(WSConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey",
                                new PasswordCallbackHandler());
        Document doc = samlToken.getOwnerDocument();
        samlToken = (Element)doc.appendChild(samlToken);
        
        ValidateTargetType validateTarget = new ValidateTargetType();
        validateTarget.setAny(samlToken);
        
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
        Element token = null;
        for (Object tokenObject : response.getAny()) {
            if (tokenObject instanceof JAXBElement<?>
                && REQUESTED_SECURITY_TOKEN.equals(((JAXBElement<?>)tokenObject).getName())) {
                RequestedSecurityTokenType rstType = 
                    (RequestedSecurityTokenType)((JAXBElement<?>)tokenObject).getValue();
                token = (Element)rstType.getAny();
                break;
            }
        }
        
        assertNotNull(token);
        JwsJwtCompactConsumer jwtConsumer = new JwsJwtCompactConsumer(token.getTextContent());
        JwtToken jwt = jwtConsumer.getJwtToken();
        Assert.assertEquals("alice", jwt.getClaim(JwtConstants.CLAIM_SUBJECT));
    }
    
    private Element createTokenWrapper(String token) {
        Document doc = DOMUtils.newDocument();
        Element tokenWrapper = doc.createElementNS(null, "TokenWrapper");
        tokenWrapper.setTextContent(token);
        return tokenWrapper;
    }
    
    private TokenProviderResponse createJWT() throws WSSecurityException {
        TokenProvider tokenProvider = new JWTTokenProvider();
        
        TokenProviderParameters providerParameters = 
            createProviderParameters(JWTTokenProvider.JWT_TOKEN_TYPE);
        
        assertTrue(tokenProvider.canHandleToken(JWTTokenProvider.JWT_TOKEN_TYPE));
        TokenProviderResponse providerResponse = tokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        return providerResponse;
    }

    private TokenProviderParameters createProviderParameters(String tokenType) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();
        
        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);
        parameters.setTokenRequirements(tokenRequirements);
        
        KeyRequirements keyRequirements = new KeyRequirements();
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
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);
        
        parameters.setEncryptionProperties(new EncryptionProperties());
        
        return parameters;
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
    
    private Map<String, RealmProperties> createSamlRealms() {
        // Create Realms
        Map<String, RealmProperties> samlRealms = new HashMap<String, RealmProperties>();
        RealmProperties samlRealm = new RealmProperties();
        samlRealm.setIssuer("A-Issuer");
        samlRealms.put("A", samlRealm);
        samlRealm = new RealmProperties();
        samlRealm.setIssuer("B-Issuer");
        samlRealms.put("B", samlRealm);
        return samlRealms;
    }
    
    private static class CustomJWTRealmCodec implements JWTRealmCodec {
        
        public String getRealmFromToken(JwtToken token) {
            if ("alice".equals(token.getClaim(JwtConstants.CLAIM_SUBJECT))) {
                return "A";
            }
            return null;
        }
        
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
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "stsstore.jks");
        
        return properties;
    }
    
    private Element createSAMLAssertion(
        String tokenType, Crypto crypto, String signatureUsername, CallbackHandler callbackHandler
    ) throws WSSecurityException {

        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        List<AttributeStatementProvider> customProviderList = 
            new ArrayList<AttributeStatementProvider>();
        customProviderList.add(new ClaimsAttributeStatementProvider());
        samlTokenProvider.setAttributeStatementProviders(customProviderList);

        TokenProviderParameters providerParameters = 
            createProviderParameters(
                tokenType, STSConstants.BEARER_KEY_KEYTYPE, crypto, signatureUsername, callbackHandler
            );

        // Set the ClaimsManager
        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        providerParameters.setClaimsManager(claimsManager);

        ClaimCollection requestedClaims = new ClaimCollection();
        Claim requestClaim = new Claim();
        requestClaim.setClaimType(ClaimTypes.LASTNAME);
        requestClaim.setOptional(false);
        requestedClaims.add(requestClaim);
        providerParameters.setRequestedSecondaryClaims(requestedClaims);

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertTrue(providerResponse != null);
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
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setSignatureUsername(signatureUsername);
        stsProperties.setCallbackHandler(callbackHandler);
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);

        parameters.setEncryptionProperties(new EncryptionProperties());

        return parameters;
    }


}
