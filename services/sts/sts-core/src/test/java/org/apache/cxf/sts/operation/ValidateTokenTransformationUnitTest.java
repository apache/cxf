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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
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
import org.apache.cxf.sts.claims.ClaimsMapper;
import org.apache.cxf.sts.common.CustomAttributeProvider;
import org.apache.cxf.sts.common.CustomClaimsHandler;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.service.StaticService;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.cxf.sts.token.realm.Relationship;
import org.apache.cxf.sts.token.validator.IssuerSAMLRealmCodec;
import org.apache.cxf.sts.token.validator.SAMLTokenValidator;
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
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * In this test, a token (UsernameToken or SAMLToken) is validated and transformed into a SAML Assertion.
 */
public class ValidateTokenTransformationUnitTest {

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
        validateOperation.setTokenValidators(Collections.singletonList(
            new UsernameTokenValidator()));

        // Add Token Provider
        validateOperation.setTokenProviders(Collections.singletonList(
            new SAMLTokenProvider()));

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
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
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
        Principal principal = new CustomTokenPrincipal("alice");
        msgCtx.put(
            SecurityContext.class.getName(),
            createSecurityContext(principal)
        );

        // Validate a token
        RequestSecurityTokenResponseType response =
            validateOperation.validate(request, principal, msgCtx);
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
        UsernameTokenValidator validator = new UsernameTokenValidator();
        validator.setUsernameTokenRealmCodec(new CustomUsernameTokenRealmCodec());
        validateOperation.setTokenValidators(Collections.singletonList(validator));

        // Add Token Provider
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        validateOperation.setTokenProviders(Collections.singletonList(samlTokenProvider));

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
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
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
        Principal principal = new CustomTokenPrincipal("alice");
        msgCtx.put(
            SecurityContext.class.getName(),
            createSecurityContext(principal)
        );
        msgCtx.put("url", "https");

        // Validate a token - this will fail as the tokenProvider doesn't understand how to handle
        // realm "B"
        try {
            validateOperation.validate(request, principal, msgCtx);
        } catch (STSException ex) {
            // expected
        }

        samlTokenProvider.setRealmMap(createSamlRealms());
        RequestSecurityTokenResponseType response = validateOperation.validate(request, principal, msgCtx);
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
     * Test to successfully validate a SAML 2 Token issued by realm "A" and
     * transform it into a SAML 2 token (realm "B")
     * The relationship type between realm A and B is: FederateIdentity
     * IdentityMapper is configured globally in STSPropertiesMBean
     */
    @org.junit.Test
    public void testValidateSaml2TokenOnBehalfOfSaml2DifferentRealmFederateIdentityGlobalConfig()
        throws Exception {
        runValidateSaml2TokenOnBehalfOfSaml2DifferentRealmFederateIdentity(true);
    }

    /**
     * Test to successfully validate a SAML 2 Token issued by realm "A" and
     * transform it into a SAML 2 token (realm "B")
     * The relationship type between realm A and B is: FederateIdentity
     * IdentityMapper is configured in the Relationship
     */
    @org.junit.Test
    //[TODO] should work after Relationship support in validateoperation
    public void testValidateSaml2TokenOnBehalfOfSaml2DifferentRealmFederateIdentityRelationshipConfig()
        throws Exception {
        runValidateSaml2TokenOnBehalfOfSaml2DifferentRealmFederateIdentity(false);
    }




    /**
     * Test to successfully validate a SAML 2 Token issued by realm "A" and
     * transform it into a SAML 2 token (realm "B")
     * The relationship type between realm A and B is: FederateClaims
     */
    @org.junit.Test
    public void testValidateSaml2TokenOnBehalfOfSaml2DifferentRealmFederateClaims() throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();

        Map<String, RealmProperties> realms = createSamlRealms();

        // Add Token Provider
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        samlTokenProvider.setRealmMap(realms);
        samlTokenProvider.setAttributeStatementProviders(Collections.singletonList(
            new ClaimsAttributeStatementProvider()));
        validateOperation.setTokenProviders(Collections.singletonList(samlTokenProvider));

        // Add Token Validator
        SAMLTokenValidator samlTokenValidator = new SAMLTokenValidator();
        samlTokenValidator.setSamlRealmCodec(new IssuerSAMLRealmCodec());
        validateOperation.setTokenValidators(Collections.singletonList(samlTokenValidator));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        validateOperation.setServices(Collections.singletonList(service));

        // Add Relationship list
        Relationship rs = createRelationship();

        // Add STSProperties object
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        STSPropertiesMBean stsProperties = createSTSPropertiesMBean(crypto);
        stsProperties.setRealmParser(new CustomRealmParser());
        stsProperties.setIdentityMapper(new CustomIdentityMapper());
        stsProperties.setRelationships(Collections.singletonList(rs));
        validateOperation.setStsProperties(stsProperties);

        // Set the ClaimsManager
        ClaimsManager claimsManager = new ClaimsManager();
        claimsManager.setClaimHandlers(Collections.singletonList((ClaimsHandler)new CustomClaimsHandler()));
        validateOperation.setClaimsManager(claimsManager);

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

        Document doc = DOMUtils.createDocument();
        Element claimType = createClaimsType(doc);
        claimsType.getAny().add(claimType);

        JAXBElement<ClaimsType> claimsTypeJaxb =
            new JAXBElement<ClaimsType>(
                QNameConstants.CLAIMS, ClaimsType.class, claimsType
            );
        request.getAny().add(claimsTypeJaxb);

        //request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // create a SAML Token via the SAMLTokenProvider which contains claims
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey",
                    callbackHandler, realms);
        Document docToken = samlToken.getOwnerDocument();
        samlToken = (Element)docToken.appendChild(samlToken);
        String samlString = DOM2Writer.nodeToString(samlToken);
        assertTrue(samlString.contains("AttributeStatement"));
        assertTrue(samlString.contains("alice"));
        assertTrue(samlString.contains("doe"));
        assertTrue(samlString.contains(SAML2Constants.CONF_BEARER));

        // Add SAML token as ValidateTarget element
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
        msgCtx.put("url", "https");

        // Validate a token
        RequestSecurityTokenResponseType response =
            validateOperation.validate(request, null, msgCtx);
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
        assertTrue(tokenString.contains("alice"));  //subject unchanged
        assertTrue(tokenString.contains("DOE"));  //claim changed (to uppercase)
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
    }



    /**
     * Test to successfully validate a UsernameToken and transform it into a SAML Assertion with claims.
     */
    private void runUsernameTokenTransformationClaims(boolean useSecondaryParameters) throws Exception {
        TokenValidateOperation validateOperation = new TokenValidateOperation();

        // Add Token Validator
        validateOperation.setTokenValidators(Collections.singletonList(
            new UsernameTokenValidator()));

        // Add Token Provider
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        samlTokenProvider.setAttributeStatementProviders(Collections.singletonList(
            new CustomAttributeProvider()));
        validateOperation.setTokenProviders(Collections.singletonList(samlTokenProvider));

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
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
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
        Principal principal = new CustomTokenPrincipal("ted");
        msgCtx.put(
            SecurityContext.class.getName(),
            createSecurityContext(principal)
        );

        // Validate a token
        RequestSecurityTokenResponseType response =
            validateOperation.validate(request, principal, msgCtx);
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
        assertTrue(tokenString.contains(ClaimTypes.LASTNAME.toString()));
    }


    private void runValidateSaml2TokenOnBehalfOfSaml2DifferentRealmFederateIdentity(
            boolean useGlobalIdentityMapper) throws WSSecurityException {
        TokenValidateOperation validateOperation = new TokenValidateOperation();

        Map<String, RealmProperties> realms = createSamlRealms();

        // Add Token Provider
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        samlTokenProvider.setRealmMap(realms);
        samlTokenProvider.setAttributeStatementProviders(Collections.singletonList(
            new ClaimsAttributeStatementProvider()));
        validateOperation.setTokenProviders(Collections.singletonList(samlTokenProvider));

        // Add Token Validator
        SAMLTokenValidator samlTokenValidator = new SAMLTokenValidator();
        samlTokenValidator.setSamlRealmCodec(new IssuerSAMLRealmCodec());
        validateOperation.setTokenValidators(Collections.singletonList(samlTokenValidator));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        validateOperation.setServices(Collections.singletonList(service));

        // Add Relationship list
        Relationship rs = createRelationship();
        rs.setType(Relationship.FED_TYPE_IDENTITY);
        rs.setIdentityMapper(new CustomIdentityMapper());

        // Add STSProperties object
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        STSPropertiesMBean stsProperties = createSTSPropertiesMBean(crypto);
        stsProperties.setRealmParser(new CustomRealmParser());
        if (useGlobalIdentityMapper) {
            stsProperties.setIdentityMapper(new CustomIdentityMapper());
        } else {
            stsProperties.setRelationships(Collections.singletonList(rs));
        }
        validateOperation.setStsProperties(stsProperties);

        // Set the ClaimsManager
        ClaimsManager claimsManager = new ClaimsManager();
        claimsManager.setClaimHandlers(Collections.singletonList((ClaimsHandler)new CustomClaimsHandler()));
        validateOperation.setClaimsManager(claimsManager);

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

        Document doc = DOMUtils.createDocument();
        Element claimType = createClaimsType(doc);
        claimsType.getAny().add(claimType);

        JAXBElement<ClaimsType> claimsTypeJaxb =
            new JAXBElement<ClaimsType>(
                QNameConstants.CLAIMS, ClaimsType.class, claimsType
            );
        request.getAny().add(claimsTypeJaxb);

        //request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // create a SAML Token via the SAMLTokenProvider which contains claims
        CallbackHandler callbackHandler = new PasswordCallbackHandler();
        Element samlToken =
            createSAMLAssertion(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, crypto, "mystskey",
                    callbackHandler, realms);
        Document docToken = samlToken.getOwnerDocument();
        samlToken = (Element)docToken.appendChild(samlToken);
        String samlString = DOM2Writer.nodeToString(samlToken);
        assertTrue(samlString.contains("AttributeStatement"));
        assertTrue(samlString.contains("alice"));
        assertTrue(samlString.contains("doe"));
        assertTrue(samlString.contains(SAML2Constants.CONF_BEARER));

        // Add SAML token as ValidateTarget element
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
        msgCtx.put("url", "https");

        // Validate a token
        RequestSecurityTokenResponseType response =
            validateOperation.validate(request, null, msgCtx);
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
        assertTrue(tokenString.contains("ALICE"));  //subject changed (to uppercase)
        assertTrue(tokenString.contains("doe"));  //claim unchanged but requested
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
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

    private Relationship createRelationship() {
        Relationship rs = new Relationship();
        ClaimsMapper claimsMapper = new CustomClaimsMapper();
        rs.setClaimsMapper(claimsMapper);
        rs.setSourceRealm("A");
        rs.setTargetRealm("B");
        rs.setType(Relationship.FED_TYPE_CLAIMS);
        return rs;
    }


    /*
     * Create STSPropertiesMBean object
     */
    private STSPropertiesMBean createSTSPropertiesMBean(Crypto crypto) throws WSSecurityException {
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        return stsProperties;
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
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "keys/stsstore.jks");

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
        passwordString.setType(WSS4JConstants.PASSWORD_TEXT);
        JAXBElement<PasswordString> passwordType =
            new JAXBElement<PasswordString>(
                QNameConstants.PASSWORD, PasswordString.class, passwordString
            );
        usernameToken.getAny().add(passwordType);

        return new JAXBElement<UsernameTokenType>(
                QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameToken
            );
    }

    /*
     * Mock up a DOM Element containing some claims
     */
    private JAXBElement<ClaimsType> createClaimsElement() {
        Document doc = DOMUtils.getEmptyDocument();

        Element claimType = createClaimsType(doc);

        ClaimsType claims = new ClaimsType();
        claims.setDialect(STSConstants.IDT_NS_05_05);
        claims.getAny().add(claimType);

        return new JAXBElement<ClaimsType>(
                    QNameConstants.CLAIMS, ClaimsType.class, claims
            );
    }

    /*
     * Mock up a SecondaryParameters DOM Element containing some claims
     */
    private Element createClaimsElementInSecondaryParameters() {
        Document doc = DOMUtils.getEmptyDocument();
        Element secondary = doc.createElementNS(STSConstants.WST_NS_05_12, "SecondaryParameters");
        secondary.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns", STSConstants.WST_NS_05_12);

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
            null, "Uri", ClaimTypes.LASTNAME.toString()
        );
        claimType.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns", STSConstants.IDT_NS_05_05);

        return claimType;
    }

    /*
     * Mock up an SAML assertion element
     */
    private static Element createSAMLAssertion(
            String tokenType, Crypto crypto, String signatureUsername, CallbackHandler callbackHandler,
            Map<String, RealmProperties> realms
    ) throws WSSecurityException {

        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        samlTokenProvider.setRealmMap(realms);
        samlTokenProvider.setAttributeStatementProviders(Collections.singletonList(
            new ClaimsAttributeStatementProvider()));

        TokenProviderParameters providerParameters =
            createProviderParameters(
                    tokenType, STSConstants.BEARER_KEY_KEYTYPE, crypto, signatureUsername, callbackHandler
            );
        if (realms != null) {
            providerParameters.setRealm("A");
        }

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
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        return (Element)providerResponse.getToken();
    }

    private static TokenProviderParameters createProviderParameters(
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
        parameters.setMessageContext(msgCtx);

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
