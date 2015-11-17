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
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.service.StaticService;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.provider.jwt.JWTTokenProvider;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.dom.WSConstants;
import org.junit.Assert;

/**
 * Some unit tests for the issue operation to issue JWT tokens in a specific realm.
 */
public class IssueJWTRealmUnitTest extends org.junit.Assert {
    
    public static final QName REQUESTED_SECURITY_TOKEN = 
        QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityToken(null).getName();
    public static final QName ATTACHED_REFERENCE = 
        QNameConstants.WS_TRUST_FACTORY.createRequestedAttachedReference(null).getName();
    public static final QName UNATTACHED_REFERENCE = 
        QNameConstants.WS_TRUST_FACTORY.createRequestedUnattachedReference(null).getName();
    
    /**
     * Test to successfully issue a JWT token in realm "A".
     */
    @org.junit.Test
    public void testIssueJWTTokenRealmA() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        
        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        JWTTokenProvider provider = new JWTTokenProvider();
        provider.setRealmMap(createRealms());
        providerList.add(provider);
        issueOperation.setTokenProviders(providerList);
        
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
        issueOperation.setStsProperties(stsProperties);
        
        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType = 
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, JWTTokenProvider.JWT_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));
        
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put("url", "ldap");
        msgCtx.put(
            SecurityContext.class.getName(), 
            createSecurityContext(new CustomTokenPrincipal("alice"))
        );
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Issue a token
        RequestSecurityTokenResponseCollectionType response = 
            issueOperation.issue(request, webServiceContext);
        List<RequestSecurityTokenResponseType> securityTokenResponse = 
            response.getRequestSecurityTokenResponse();
        assertTrue(!securityTokenResponse.isEmpty());
        
        // Test the generated token.
        Element token = null;
        for (Object tokenObject : securityTokenResponse.get(0).getAny()) {
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
        Assert.assertEquals("A-Issuer", jwt.getClaim(JwtConstants.CLAIM_ISSUER));
    }
    
    /**
     * Test to successfully issue a JWT token in realm "B".
     */
    @org.junit.Test
    public void testIssueJWTTokenRealmB() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        
        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        JWTTokenProvider provider = new JWTTokenProvider();
        provider.setRealmMap(createRealms());
        providerList.add(provider);
        issueOperation.setTokenProviders(providerList);
        
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
        issueOperation.setStsProperties(stsProperties);
        
        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType = 
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, JWTTokenProvider.JWT_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));
        
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put("url", "https");
        msgCtx.put(
            SecurityContext.class.getName(), 
            createSecurityContext(new CustomTokenPrincipal("alice"))
        );
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Issue a token
        RequestSecurityTokenResponseCollectionType response = 
            issueOperation.issue(request, webServiceContext);
        List<RequestSecurityTokenResponseType> securityTokenResponse = 
            response.getRequestSecurityTokenResponse();
        assertTrue(!securityTokenResponse.isEmpty());
        
        // Test the generated token.
        Element token = null;
        for (Object tokenObject : securityTokenResponse.get(0).getAny()) {
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
        Assert.assertEquals("B-Issuer", jwt.getClaim(JwtConstants.CLAIM_ISSUER));
    }
    
    /**
     * Test to successfully issue a JWT token in the default realm.
     */
    @org.junit.Test
    public void testIssueJWTTokenDefaultRealm() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        
        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        JWTTokenProvider provider = new JWTTokenProvider();
        provider.setRealmMap(createRealms());
        providerList.add(provider);
        issueOperation.setTokenProviders(providerList);
        
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
        issueOperation.setStsProperties(stsProperties);
        
        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType = 
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, JWTTokenProvider.JWT_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));
        
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put("url", "unknown");
        msgCtx.put(
            SecurityContext.class.getName(), 
            createSecurityContext(new CustomTokenPrincipal("alice"))
        );
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Issue a token
        RequestSecurityTokenResponseCollectionType response = 
            issueOperation.issue(request, webServiceContext);
        List<RequestSecurityTokenResponseType> securityTokenResponse = 
            response.getRequestSecurityTokenResponse();
        assertTrue(!securityTokenResponse.isEmpty());
        
        // Test the generated token.
        Element token = null;
        for (Object tokenObject : securityTokenResponse.get(0).getAny()) {
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
        Assert.assertEquals("STS", jwt.getClaim(JwtConstants.CLAIM_ISSUER));
    }
    
    
    /**
     * Test to successfully issue a JWT token in realm "B"
     * using crypto definition in RealmProperties
     */
    @org.junit.Test
    public void testIssueJWTTokenRealmBCustomCrypto() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        
        // Add Token Provider
        List<TokenProvider> providerList = new ArrayList<TokenProvider>();
        JWTTokenProvider provider = new JWTTokenProvider();
        provider.setRealmMap(createRealms());
        providerList.add(provider);
        issueOperation.setTokenProviders(providerList);
        
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
        issueOperation.setStsProperties(stsProperties);
        
        // Set signature properties in Realm B
        Map<String, RealmProperties> realms = provider.getRealmMap();
        RealmProperties realm = realms.get("B");
        realm.setSignatureCrypto(crypto);
        realm.setCallbackHandler(new PasswordCallbackHandler());
        
        
        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType = 
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, JWTTokenProvider.JWT_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));
        
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put("url", "https");
        msgCtx.put(
            SecurityContext.class.getName(), 
            createSecurityContext(new CustomTokenPrincipal("alice"))
        );
        WebServiceContextImpl webServiceContext = new WebServiceContextImpl(msgCtx);
        
        // Issue a token - this will fail as the RealmProperties configuration is inconsistent
        // no signature alias defined
        try {
            issueOperation.issue(request, webServiceContext);
            fail("Failure expected on no encryption name");
        } catch (STSException ex) {
            // expected
        }
        
        realm.setSignatureAlias("mystskey");
        
        // Issue a token
        RequestSecurityTokenResponseCollectionType response = 
            issueOperation.issue(request, webServiceContext);
        List<RequestSecurityTokenResponseType> securityTokenResponse = 
            response.getRequestSecurityTokenResponse();
        assertTrue(!securityTokenResponse.isEmpty());
        
     // Test the generated token.
        Element token = null;
        for (Object tokenObject : securityTokenResponse.get(0).getAny()) {
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
        Assert.assertEquals("B-Issuer", jwt.getClaim(JwtConstants.CLAIM_ISSUER));
    }
    
    /**
     * Create some JWT Realms
     */
    private Map<String, RealmProperties> createRealms() {
        Map<String, RealmProperties> realms = new HashMap<String, RealmProperties>();
        RealmProperties realm = new RealmProperties();
        realm.setIssuer("A-Issuer");
        realms.put("A", realm);
        realm = new RealmProperties();
        realm.setIssuer("B-Issuer");
        realms.put("B", realm);
        
        return realms;
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
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "stsstore.jks");
        
        return properties;
    }
    
}
