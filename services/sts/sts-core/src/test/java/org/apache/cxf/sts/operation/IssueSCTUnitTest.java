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
import java.util.List;
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
import org.apache.cxf.sts.cache.DefaultInMemoryTokenStore;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.service.StaticService;
import org.apache.cxf.sts.token.provider.SCTProvider;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.util.DOM2Writer;

import org.junit.BeforeClass;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some unit tests for the issue operation to issue SecurityContextTokens.
 */
public class IssueSCTUnitTest {

    public static final QName REQUESTED_SECURITY_TOKEN =
        QNameConstants.WS_TRUST_FACTORY.createRequestedSecurityToken(null).getName();
    public static final QName ATTACHED_REFERENCE =
        QNameConstants.WS_TRUST_FACTORY.createRequestedAttachedReference(null).getName();
    public static final QName UNATTACHED_REFERENCE =
        QNameConstants.WS_TRUST_FACTORY.createRequestedUnattachedReference(null).getName();

    private static TokenStore tokenStore;

    private static boolean unrestrictedPoliciesInstalled;

    static {
        unrestrictedPoliciesInstalled = TestUtilities.checkUnrestrictedPoliciesInstalled();
    };

    @BeforeClass
    public static void init() throws TokenStoreException {
        tokenStore = new DefaultInMemoryTokenStore();
    }

    /**
     * Test to successfully issue a SecurityContextToken
     */
    @org.junit.Test
    public void testIssueSCT() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setTokenStore(tokenStore);

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new SCTProvider()));

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
                QNameConstants.TOKEN_TYPE, String.class, STSUtils.TOKEN_TYPE_SCT_05_12
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
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
        Element securityContextToken = null;
        for (Object tokenObject : securityTokenResponse.get(0).getAny()) {
            if (tokenObject instanceof JAXBElement<?>
                && REQUESTED_SECURITY_TOKEN.equals(((JAXBElement<?>)tokenObject).getName())) {
                RequestedSecurityTokenType rstType =
                    (RequestedSecurityTokenType)((JAXBElement<?>)tokenObject).getValue();
                securityContextToken = (Element)rstType.getAny();
                break;
            }
        }

        assertNotNull(securityContextToken);
        String tokenString = DOM2Writer.nodeToString(securityContextToken);
        assertTrue(tokenString.contains("SecurityContextToken"));
        assertTrue(tokenString.contains("Identifier"));
    }

    /**
     * Test to successfully issue an encrypted SecurityContextToken
     */
    @org.junit.Test
    public void testIssueEncryptedSCT() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setTokenStore(tokenStore);
        issueOperation.setEncryptIssuedToken(true);

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new SCTProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        EncryptionProperties encryptionProperties = new EncryptionProperties();
        if (!unrestrictedPoliciesInstalled) {
            encryptionProperties.setEncryptionAlgorithm(WSS4JConstants.AES_128);
        }
        service.setEncryptionProperties(encryptionProperties);
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
                QNameConstants.TOKEN_TYPE, String.class, STSUtils.TOKEN_TYPE_SCT_05_12
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
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
        Element securityContextToken = null;
        for (Object tokenObject : securityTokenResponse.get(0).getAny()) {
            if (tokenObject instanceof JAXBElement<?>
                && REQUESTED_SECURITY_TOKEN.equals(((JAXBElement<?>)tokenObject).getName())) {
                RequestedSecurityTokenType rstType =
                    (RequestedSecurityTokenType)((JAXBElement<?>)tokenObject).getValue();
                securityContextToken = (Element)rstType.getAny();
                break;
            }
        }

        assertNotNull(securityContextToken);
        String tokenString = DOM2Writer.nodeToString(securityContextToken);
        assertFalse(tokenString.contains("SecurityContextToken"));
        assertFalse(tokenString.contains("Identifier"));
        assertTrue(tokenString, tokenString.contains("EncryptedData"));
    }

    /**
     * Test to successfully issue a SecurityContextToken with no references
     */
    @org.junit.Test
    public void testIssueSCTNoReferences() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        issueOperation.setTokenStore(tokenStore);
        issueOperation.setReturnReferences(false);

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new SCTProvider()));

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
                QNameConstants.TOKEN_TYPE, String.class, STSUtils.TOKEN_TYPE_SCT_05_12
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
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

        // Test that no references were returned
        boolean foundReference = false;
        for (Object tokenObject : securityTokenResponse.get(0).getAny()) {
            if (tokenObject instanceof JAXBElement<?>
                && (ATTACHED_REFERENCE.equals(((JAXBElement<?>)tokenObject).getName())
                || UNATTACHED_REFERENCE.equals(((JAXBElement<?>)tokenObject).getName()))) {
                foundReference = true;
                break;
            }
        }

        assertFalse(foundReference);
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


}
