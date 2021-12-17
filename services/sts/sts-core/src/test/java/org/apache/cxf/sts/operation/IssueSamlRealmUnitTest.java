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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.service.StaticService;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.util.DOM2Writer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some unit tests for the issue operation to issue SAML tokens in a specific realm.
 */
public class IssueSamlRealmUnitTest {

    public static final QName REQUESTED_SECURITY_TOKEN =
        QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityToken(null).getName();
    public static final QName ATTACHED_REFERENCE =
        QNameConstants.WS_TRUST_FACTORY.createRequestedAttachedReference(null).getName();
    public static final QName UNATTACHED_REFERENCE =
        QNameConstants.WS_TRUST_FACTORY.createRequestedUnattachedReference(null).getName();

    /**
     * Test to successfully issue a Saml 1.1 token in realm "A".
     */
    @org.junit.Test
    public void testIssueSaml1TokenRealmA() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        SAMLTokenProvider provider = new SAMLTokenProvider();
        provider.setRealmMap(createRealms());
        issueOperation.setTokenProviders(Collections.singletonList(provider));

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
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put("url", "ldap");
        Principal principal = new CustomTokenPrincipal("alice");
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
        assertTrue(tokenString.contains("Issuer=\"A-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"B-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"STS\""));
    }

    /**
     * Test to successfully issue a Saml 1.1 token in realm "B".
     */
    @org.junit.Test
    public void testIssueSaml1TokenRealmB() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        SAMLTokenProvider provider = new SAMLTokenProvider();
        provider.setRealmMap(createRealms());
        issueOperation.setTokenProviders(Collections.singletonList(provider));

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
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put("url", "https");
        Principal principal = new CustomTokenPrincipal("alice");
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
        assertFalse(tokenString.contains("Issuer=\"A-Issuer\""));
        assertTrue(tokenString.contains("Issuer=\"B-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"STS\""));
    }

    /**
     * Test to successfully issue a Saml 1.1 token in the default realm.
     */
    @org.junit.Test
    public void testIssueSaml1TokenDefaultRealm() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        SAMLTokenProvider provider = new SAMLTokenProvider();
        provider.setRealmMap(createRealms());
        issueOperation.setTokenProviders(Collections.singletonList(provider));

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
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put("url", "unknown");
        Principal principal = new CustomTokenPrincipal("alice");
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
        assertFalse(tokenString.contains("Issuer=\"A-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"B-Issuer\""));
        assertTrue(tokenString.contains("Issuer=\"STS\""));
    }


    /**
     * Test to successfully issue a Saml 1.1 token in realm "B"
     * using crypto definition in SAMLRealm
     */
    @org.junit.Test
    public void testIssueSaml1TokenRealmBCustomCrypto() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        SAMLTokenProvider provider = new SAMLTokenProvider();
        provider.setRealmMap(createRealms());
        issueOperation.setTokenProviders(Collections.singletonList(provider));

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

        // Set signature properties in SAMLRealm B
        Map<String, RealmProperties> samlRealms = provider.getRealmMap();
        RealmProperties realm = samlRealms.get("B");
        realm.setSignatureCrypto(crypto);
        realm.setCallbackHandler(new PasswordCallbackHandler());


        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put("url", "https");
        Principal principal = new CustomTokenPrincipal("alice");
        msgCtx.put(
            SecurityContext.class.getName(),
            createSecurityContext(principal)
        );

        // Issue a token - this will fail as the SAMLRealm configuration is inconsistent
        // no signature alias defined
        try {
            issueOperation.issue(request, principal, msgCtx);
            fail("Failure expected on no encryption name");
        } catch (STSException ex) {
            // expected
        }

        realm.setSignatureAlias("mystskey");

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
        assertFalse(tokenString.contains("Issuer=\"A-Issuer\""));
        assertTrue(tokenString.contains("Issuer=\"B-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"STS\""));
    }


    /**
     * Test to successfully issue a Saml 1.1 token in realm "B"
     * using PKCS12 crypto definition with default key in SAMLRealm
     */
    @org.junit.Test
    public void testIssueSaml1TokenRealmBCustomCryptoPKCS12() throws Exception {
        if (!TestUtilities.checkUnrestrictedPoliciesInstalled()) {
            return;
        }
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        SAMLTokenProvider provider = new SAMLTokenProvider();
        provider.setRealmMap(createRealms());
        issueOperation.setTokenProviders(Collections.singletonList(provider));

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

        // Set signature properties in SAMLRealm B
        Map<String, RealmProperties> samlRealms = provider.getRealmMap();
        RealmProperties realm = samlRealms.get("B");
        realm.setSignatureCrypto(CryptoFactory.getInstance(getEncryptionPropertiesPKCS12()));
        realm.setCallbackHandler(new PasswordCallbackHandler());

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, WSS4JConstants.WSS_SAML_TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        msgCtx.put("url", "https");
        Principal principal = new CustomTokenPrincipal("alice");
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
        assertFalse(tokenString.contains("Issuer=\"A-Issuer\""));
        assertTrue(tokenString.contains("Issuer=\"B-Issuer\""));
        assertFalse(tokenString.contains("Issuer=\"STS\""));
    }


    /**
     * Create some SAML Realms
     */
    private Map<String, RealmProperties> createRealms() {
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

    private Properties getEncryptionPropertiesPKCS12() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "security");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "x509.p12");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.type", "pkcs12");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.private.password", "security");

        return properties;
    }


}
