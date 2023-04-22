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

import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
import org.apache.cxf.sts.claims.ClaimsParser;
import org.apache.cxf.sts.common.CustomAttributeProvider;
import org.apache.cxf.sts.common.CustomClaimParser;
import org.apache.cxf.sts.common.CustomClaimsHandler;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.service.StaticService;
import org.apache.cxf.sts.token.delegation.SAMLDelegationHandler;
import org.apache.cxf.sts.token.delegation.TokenDelegationHandler;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.sts.token.provider.TokenProviderResponse;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.cxf.sts.token.realm.Relationship;
import org.apache.cxf.sts.token.validator.IssuerSAMLRealmCodec;
import org.apache.cxf.sts.token.validator.SAMLTokenValidator;
import org.apache.cxf.ws.security.sts.provider.model.ClaimsType;
import org.apache.cxf.ws.security.sts.provider.model.OnBehalfOfType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
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
import static org.junit.Assert.fail;


/**
 * Some unit tests for the issue operation to issue SAML tokens with Claims information.
 */
public class IssueSamlClaimsUnitTest {

    public static final QName REQUESTED_SECURITY_TOKEN =
        QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityToken(null).getName();

    private static final URI ROLE_CLAIM =
            URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role");

    /**
     * Test to successfully issue a Saml 1.1 token.
     */
    @org.junit.Test
    public void testIssueSaml1Token() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        addTokenProvider(issueOperation);

        addService(issueOperation);

        addSTSProperties(issueOperation);

        // Set the ClaimsManager
        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        issueOperation.setClaimsManager(claimsManager);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        Element secondaryParameters = createSecondaryParameters();
        request.getAny().add(secondaryParameters);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        Map<String, Object> msgCtx = setupMessageContext();

        List<RequestSecurityTokenResponseType> securityTokenResponse = issueToken(issueOperation, request,
                                                                                  new CustomTokenPrincipal("alice"),
                                                                                  msgCtx);

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
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML1Constants.CONF_BEARER));
        assertTrue(tokenString.contains(ClaimTypes.LASTNAME.toString()));
        assertTrue(tokenString.contains(ROLE_CLAIM.toString()));
        assertTrue(tokenString.contains("administrator"));
    }

    /**
     * Test to successfully issue a Saml 2 token.
     */
    @org.junit.Test
    public void testIssueSaml2Token() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        addTokenProvider(issueOperation);

        // Add Service
        addService(issueOperation);

        // Add STSProperties object
        addSTSProperties(issueOperation);

        // Set the ClaimsManager
        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        issueOperation.setClaimsManager(claimsManager);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        Element secondaryParameters = createSecondaryParameters();
        request.getAny().add(secondaryParameters);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        Map<String, Object> msgCtx = setupMessageContext();

        List<RequestSecurityTokenResponseType> securityTokenResponse = issueToken(issueOperation, request,
                                                                                  new CustomTokenPrincipal("alice"),
                                                                                  msgCtx);

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
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
        assertTrue(tokenString.contains(ClaimTypes.LASTNAME.toString()));
        assertTrue(tokenString.contains(ROLE_CLAIM.toString()));
        assertTrue(tokenString.contains("administrator"));
    }

    /**
     * Test custom claim parser and handler.
     */
    @org.junit.Test
    public void testCustomClaimDialect() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        addTokenProvider(issueOperation);

        // Add Service
        addService(issueOperation);

        // Add STSProperties object
        addSTSProperties(issueOperation);

        // Set the ClaimsManager
        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        ClaimsParser claimsParser = new CustomClaimParser();
        claimsManager.setClaimParsers(Collections.singletonList(claimsParser));
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        issueOperation.setClaimsManager(claimsManager);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        Element secondaryParameters = createCustomSecondaryParameters();
        request.getAny().add(secondaryParameters);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        Map<String, Object> msgCtx = setupMessageContext();

        // Issue a token
        List<RequestSecurityTokenResponseType> securityTokenResponse = issueToken(issueOperation,
                request, new CustomTokenPrincipal("alice"), msgCtx);

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
        assertTrue(tokenString.contains("bob@custom"));
    }

    @org.junit.Test
    public void testIssueTokenUnknownClaim() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        addTokenProvider(issueOperation);

        // Add Service
        addService(issueOperation);

        // Add STSProperties object
        addSTSProperties(issueOperation);

        // Set the ClaimsManager
        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        issueOperation.setClaimsManager(claimsManager);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);

        // Add a custom claim (unknown to the CustomClaimsHandler)
        Element secondaryParameters = createSecondaryParameters();
        Node claims =
            secondaryParameters.getElementsByTagNameNS(STSConstants.WST_NS_05_12, "Claims").item(0);
        Element claimType = claims.getOwnerDocument().createElementNS(STSConstants.IDT_NS_05_05, "ClaimType");
        claimType.setAttributeNS(
            null, "Uri", ClaimTypes.COUNTRY.toString()
        );
        claimType.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns", STSConstants.IDT_NS_05_05);
        claims.appendChild(claimType);

        request.getAny().add(secondaryParameters);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        Map<String, Object> msgCtx = setupMessageContext();

        try {
            issueToken(issueOperation, request, new CustomTokenPrincipal("alice"), msgCtx);
            fail("Failure expected on an unknown non-optional claims type");
        } catch (Exception ex) {
            // expected
        }
    }

    /**
     * @param issueOperation
     * @param request
     * @param webServiceContext
     * @return
     */
    private List<RequestSecurityTokenResponseType> issueToken(TokenIssueOperation issueOperation,
            RequestSecurityTokenType request, Principal principal, Map<String, Object> msgCtx) {
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, principal, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
        return securityTokenResponse;
    }

    /**
     * @return
     */
    private Map<String, Object> setupMessageContext() {
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put(
            SecurityContext.class.getName(),
            createSecurityContext(new CustomTokenPrincipal("alice"))
        );
        return msgCtx;
    }

    /**
     * @param issueOperation
     * @throws WSSecurityException
     */
    private void addSTSProperties(TokenIssueOperation issueOperation) throws WSSecurityException {
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        issueOperation.setStsProperties(stsProperties);
    }

    /**
     * @param issueOperation
     */
    private void addService(TokenIssueOperation issueOperation) {
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));
    }

    /**
     * @param issueOperation
     */
    private static void addTokenProvider(TokenIssueOperation issueOperation) {
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        samlTokenProvider.setAttributeStatementProviders(Collections.singletonList(
            new CustomAttributeProvider()));
        issueOperation.setTokenProviders(Collections.singletonList(samlTokenProvider));
    }

    /**
     * Test to successfully issue a Saml 1.1 token. The claims information is included as a
     * JAXB Element under RequestSecurityToken, rather than as a child of SecondaryParameters.
     */
    @org.junit.Test
    public void testIssueJaxbSaml1Token() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        addTokenProvider(issueOperation);

        addService(issueOperation);

        addSTSProperties(issueOperation);

        // Set the ClaimsManager
        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
        claimsManager.setClaimHandlers(Collections.singletonList(claimsHandler));
        issueOperation.setClaimsManager(claimsManager);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML_TOKEN_TYPE
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

        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        Map<String, Object> msgCtx = setupMessageContext();

        List<RequestSecurityTokenResponseType> securityTokenResponse = issueToken(issueOperation, request,
                                                                                  new CustomTokenPrincipal("alice"),
                                                                                  msgCtx);

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
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML1Constants.CONF_BEARER));
        assertTrue(tokenString.contains(ClaimTypes.LASTNAME.toString()));
    }

    /**
     * Test to successfully issue a SAML 2 token (realm "B") on-behalf-of a SAML 2 token
     * which was issued by realm "A".
     * The relationship type between realm A and B is: FederateClaims
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfSaml2DifferentRealmFederateClaims()
        throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        Map<String, RealmProperties> realms = createSamlRealms();

        // Add Token Provider
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        samlTokenProvider.setRealmMap(realms);
        samlTokenProvider.setAttributeStatementProviders(Collections.singletonList(
            new ClaimsAttributeStatementProvider()));
        issueOperation.setTokenProviders(Collections.singletonList(samlTokenProvider));

        TokenDelegationHandler delegationHandler = new SAMLDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        // Add Token Validator
        SAMLTokenValidator samlTokenValidator = new SAMLTokenValidator();
        samlTokenValidator.setSamlRealmCodec(new IssuerSAMLRealmCodec());
        issueOperation.setTokenValidators(Collections.singletonList(
            samlTokenValidator));

        addService(issueOperation);

        // Add Relationship list
        Relationship rs = createRelationship();

        // Add STSProperties object
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        STSPropertiesMBean stsProperties = createSTSPropertiesMBean(crypto);
        stsProperties.setRealmParser(new CustomRealmParser());
        stsProperties.setIdentityMapper(new CustomIdentityMapper());
        stsProperties.setRelationships(Collections.singletonList(rs));
        issueOperation.setStsProperties(stsProperties);

        // Set the ClaimsManager
        ClaimsManager claimsManager = new ClaimsManager();
        ClaimsHandler claimsHandler = new CustomClaimsHandler();
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

        // add SAML token as On-Behalf-Of element
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

        List<RequestSecurityTokenResponseType> securityTokenResponseList = issueToken(issueOperation,
                request, null, msgCtx);
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
        assertTrue(tokenString.contains("alice"));  //subject unchanged
        assertTrue(tokenString.contains("DOE")); //transformed claim (to uppercase)
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
    }

    /**
     * Test to successfully issue a SAML 2 token (realm "B") on-behalf-of a SAML 2 token
     * which was issued by realm "A".
     * The relationship type between realm A and B is: FederateIdentity
     * IdentityMapper is configured globally in STSPropertiesMBean
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfSaml2DifferentRealmFederateIdentityGlobalConfig()
        throws Exception {
        runIssueSaml2TokenOnBehalfOfSaml2DifferentRealmFederateIdentity(true);
    }


    /**
     * Test to successfully issue a SAML 2 token (realm "B") on-behalf-of a SAML 2 token
     * which was issued by realm "A".
     * The relationship type between realm A and B is: FederateIdentity
     * IdentityMapper is configured in the Relationship
     */
    @org.junit.Test
    public void testIssueSaml2TokenOnBehalfOfSaml2DifferentRealmFederateIdentityRelationshipConfig()
        throws Exception {
        runIssueSaml2TokenOnBehalfOfSaml2DifferentRealmFederateIdentity(false);
    }

    private void runIssueSaml2TokenOnBehalfOfSaml2DifferentRealmFederateIdentity(
            boolean useGlobalIdentityMapper) throws WSSecurityException {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        Map<String, RealmProperties> realms = createSamlRealms();

        // Add Token Provider
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        samlTokenProvider.setRealmMap(realms);
        samlTokenProvider.setAttributeStatementProviders(Collections.singletonList(
            new ClaimsAttributeStatementProvider()));
        issueOperation.setTokenProviders(Collections.singletonList(samlTokenProvider));

        TokenDelegationHandler delegationHandler = new SAMLDelegationHandler();
        issueOperation.setDelegationHandlers(Collections.singletonList(delegationHandler));

        // Add Token Validator
        SAMLTokenValidator samlTokenValidator = new SAMLTokenValidator();
        samlTokenValidator.setSamlRealmCodec(new IssuerSAMLRealmCodec());
        issueOperation.setTokenValidators(Collections.singletonList(samlTokenValidator));

        addService(issueOperation);

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

        issueOperation.setStsProperties(stsProperties);

        // Set the ClaimsManager
        ClaimsManager claimsManager = new ClaimsManager();
        claimsManager.setClaimHandlers(Collections.singletonList((ClaimsHandler)new CustomClaimsHandler()));
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

        // add SAML token as On-Behalf-Of element
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

        List<RequestSecurityTokenResponseType> securityTokenResponseList = issueToken(issueOperation,
                request, null, msgCtx);
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
        assertTrue(tokenString.contains("ALICE"));  //subject changed (to uppercase)
        assertTrue(tokenString.contains("doe"));  //claim unchanged but requested
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
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
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "keys/stsstore.jks");

        return properties;
    }

    /*
     * Mock up a SecondaryParameters DOM Element containing some claims
     */
    private Element createSecondaryParameters() {
        Document doc = DOMUtils.getEmptyDocument();
        Element secondary = doc.createElementNS(STSConstants.WST_NS_05_12, "SecondaryParameters");
        secondary.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns", STSConstants.WST_NS_05_12);

        Element claims = doc.createElementNS(STSConstants.WST_NS_05_12, "Claims");
        claims.setAttributeNS(null, "Dialect", STSConstants.IDT_NS_05_05);

        Element claimType = createClaimsType(doc);
        claims.appendChild(claimType);
        Element claimValue = createClaimValue(doc);
        claims.appendChild(claimValue);
        secondary.appendChild(claims);

        return secondary;
    }

    /*
     * Mock up a SecondaryParameters DOM Element containing a custom claim dialect.
     */
    private Element createCustomSecondaryParameters() {
        Document doc = DOMUtils.getEmptyDocument();
        Element secondary = doc.createElementNS(STSConstants.WST_NS_05_12, "SecondaryParameters");
        secondary.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns", STSConstants.WST_NS_05_12);

        Element claims = doc.createElementNS(STSConstants.WST_NS_05_12, "Claims");
        claims.setAttributeNS(null, "Dialect", CustomClaimParser.CLAIMS_DIALECT);

        Element claim = doc.createElementNS(CustomClaimParser.CLAIMS_DIALECT, "MyElement");
        claim.setAttributeNS(null, "Uri", ClaimTypes.FIRSTNAME.toString());
        claim.setAttributeNS(null, "value", "bob");
        claim.setAttributeNS(null, "scope", "custom");

        claims.appendChild(claim);
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

    private Element createClaimValue(Document doc) {
        Element claimValue = doc.createElementNS(STSConstants.IDT_NS_05_05, "ClaimValue");
        claimValue.setAttributeNS(null, "Uri", ROLE_CLAIM.toString());
        claimValue.setAttributeNS(WSS4JConstants.XMLNS_NS, "xmlns", STSConstants.IDT_NS_05_05);
        Element value = doc.createElementNS(STSConstants.IDT_NS_05_05, "Value");
        value.setTextContent("administrator");
        claimValue.appendChild(value);
        return claimValue;
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

    /*
     * Mock up an SAML assertion element
     */
    private Element createSAMLAssertion(
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
