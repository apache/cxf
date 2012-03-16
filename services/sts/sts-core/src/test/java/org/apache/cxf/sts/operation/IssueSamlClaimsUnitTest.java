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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.apache.cxf.sts.claims.ClaimsParser;
import org.apache.cxf.sts.common.CustomAttributeProvider;
import org.apache.cxf.sts.common.CustomClaimParser;
import org.apache.cxf.sts.common.CustomClaimsHandler;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.service.StaticService;
import org.apache.cxf.sts.token.provider.AttributeStatementProvider;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.ws.security.sts.provider.model.ClaimsType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.ws.security.CustomTokenPrincipal;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.saml.ext.builder.SAML1Constants;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.apache.ws.security.util.DOM2Writer;

/**
 * Some unit tests for the issue operation to issue SAML tokens with Claims information.
 */
public class IssueSamlClaimsUnitTest extends org.junit.Assert {
    
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
                QNameConstants.TOKEN_TYPE, String.class, WSConstants.WSS_SAML_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        Element secondaryParameters = createSecondaryParameters();
        request.getAny().add(secondaryParameters);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));
        
        WebServiceContextImpl webServiceContext = setupMessageContext();
        
        List<RequestSecurityTokenResponseType> securityTokenResponse = issueToken(issueOperation, request,
                webServiceContext);
        
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
                QNameConstants.TOKEN_TYPE, String.class, WSConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        Element secondaryParameters = createSecondaryParameters();
        request.getAny().add(secondaryParameters);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));
        
        WebServiceContextImpl webServiceContext = setupMessageContext();
        
        List<RequestSecurityTokenResponseType> securityTokenResponse = issueToken(issueOperation, request,
                webServiceContext);
        
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
                QNameConstants.TOKEN_TYPE, String.class, WSConstants.WSS_SAML2_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        Element secondaryParameters = createCustomSecondaryParameters();
        request.getAny().add(secondaryParameters);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));
        
        // Mock up message context
        WebServiceContextImpl webServiceContext = setupMessageContext();
        
        // Issue a token
        List<RequestSecurityTokenResponseType> securityTokenResponse = issueToken(issueOperation, 
                request, webServiceContext);
        
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

    /**
     * @param issueOperation
     * @param request
     * @param webServiceContext
     * @return
     */
    private List<RequestSecurityTokenResponseType> issueToken(TokenIssueOperation issueOperation,
            RequestSecurityTokenType request, WebServiceContextImpl webServiceContext) {
        RequestSecurityTokenResponseCollectionType response = 
            issueOperation.issue(request, webServiceContext);
        List<RequestSecurityTokenResponseType> securityTokenResponse = 
            response.getRequestSecurityTokenResponse();
        assertTrue(!securityTokenResponse.isEmpty());
        return securityTokenResponse;
    }

    /**
     * @return
     */
    private WebServiceContextImpl setupMessageContext() {
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put(
            SecurityContext.class.getName(), 
            createSecurityContext(new CustomTokenPrincipal("alice"))
        );
        return new WebServiceContextImpl(msgCtx);
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
    private void addTokenProvider(TokenIssueOperation issueOperation) {
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        
        List<AttributeStatementProvider> customProviderList = 
            new ArrayList<AttributeStatementProvider>();
        customProviderList.add(new CustomAttributeProvider());
        SAMLTokenProvider samlTokenProvider = new SAMLTokenProvider();
        samlTokenProvider.setAttributeStatementProviders(customProviderList);
        providerList.add(samlTokenProvider);
        issueOperation.setTokenProviders(providerList);
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
                QNameConstants.TOKEN_TYPE, String.class, WSConstants.WSS_SAML_TOKEN_TYPE
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
        
        WebServiceContextImpl webServiceContext = setupMessageContext();
        
        List<RequestSecurityTokenResponseType> securityTokenResponse = issueToken(issueOperation, request,
                webServiceContext);
        
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
            "org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin"
        );
        properties.put("org.apache.ws.security.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.ws.security.crypto.merlin.keystore.file", "stsstore.jks");
        
        return properties;
    }
    
    /*
     * Mock up a SecondaryParameters DOM Element containing some claims
     */
    private Element createSecondaryParameters() {
        Document doc = DOMUtils.createDocument();
        Element secondary = doc.createElementNS(STSConstants.WST_NS_05_12, "SecondaryParameters");
        secondary.setAttributeNS(WSConstants.XMLNS_NS, "xmlns", STSConstants.WST_NS_05_12);
        
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
        Document doc = DOMUtils.createDocument();
        Element secondary = doc.createElementNS(STSConstants.WST_NS_05_12, "SecondaryParameters");
        secondary.setAttributeNS(WSConstants.XMLNS_NS, "xmlns", STSConstants.WST_NS_05_12);
        
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
        claimType.setAttributeNS(WSConstants.XMLNS_NS, "xmlns", STSConstants.IDT_NS_05_05);
        
        return claimType;
    }
    
    private Element createClaimValue(Document doc) {
        Element claimValue = doc.createElementNS(STSConstants.IDT_NS_05_05, "ClaimValue");
        claimValue.setAttributeNS(null, "Uri", ROLE_CLAIM.toString());
        claimValue.setAttributeNS(WSConstants.XMLNS_NS, "xmlns", STSConstants.IDT_NS_05_05);
        Element value = doc.createElementNS(STSConstants.IDT_NS_05_05, "Value");
        value.setTextContent("administrator");
        claimValue.appendChild(value);
        return claimValue;
    }
    
}
