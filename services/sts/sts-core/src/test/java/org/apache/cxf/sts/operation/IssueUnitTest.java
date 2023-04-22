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

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.service.StaticService;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.LifetimeType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.utility.AttributedDateTime;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.util.DateUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Some unit tests for the issue operation.
 */
public class IssueUnitTest {

    /**
     * Test to successfully issue a (dummy) token.
     */
    @org.junit.Test
    public void testIssueToken() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
    }

    /**
     * Test to successfully issue multiple (dummy) tokens.
     */
    @org.junit.Test
    public void testIssueMultipleTokens() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenCollectionType requestCollection =
            new RequestSecurityTokenCollectionType();
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));
        requestCollection.getRequestSecurityToken().add(request);

        request = new RequestSecurityTokenType();
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));
        requestCollection.getRequestSecurityToken().add(request);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(requestCollection, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertEquals(securityTokenResponse.size(), 2);
    }

    /**
     * Test to issue a token of an unknown or missing TokenType value.
     */
    @org.junit.Test
    public void testTokenType() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, "UnknownTokenType"
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token - failure expected on an unknown token type
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected on an unknown token type");
        } catch (STSException ex) {
            // expected
        }

        // Issue a token - failure expected as no token type is sent
        request.getAny().remove(0);
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected on no token type");
        } catch (STSException ex) {
            // expected
        }

        // Issue a token - this time it defaults to a known token type
        service.setTokenType(DummyTokenProvider.TOKEN_TYPE);
        issueOperation.setServices(Collections.singletonList(service));

        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
    }


    /**
     * Test the endpoint address sent to the STS as part of AppliesTo. If the STS does not
     * recognise the endpoint address it does not issue a token.
     */
    @org.junit.Test
    public void testIssueEndpointAddress() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy-unknown"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token - failure expected on an unknown address
        try {
            issueOperation.issue(request, null, msgCtx);
            fail("Failure expected on an unknown address");
        } catch (STSException ex) {
            // expected
        }

        // Issue a token - This should work as wildcards are used
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy.*"));
        issueOperation.setServices(Collections.singletonList(service));

        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
    }

    /**
     * Test that a request with no AppliesTo is not rejected
     */
    @org.junit.Test
    public void testNoAppliesTo() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
    }


    /**
     * Test that sends a Context attribute when requesting a token, and checks it gets
     * a response with the Context attribute properly set.
     */
    @org.junit.Test
    public void testContext() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));
        request.setContext("AuthenticationContext");

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
        assertEquals("AuthenticationContext", securityTokenResponse.get(0).getContext());
    }

    /**
     * Test to successfully issue a (dummy) token with a supplied lifetime. It only tests that
     * the lifetime can be successfully processed by the RequestParser for now.
     */
    @org.junit.Test
    public void testLifetime() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));
        LifetimeType lifetime = createLifetime(300L * 5L);
        JAXBElement<LifetimeType> lifetimeJaxb =
            new JAXBElement<>(QNameConstants.LIFETIME, LifetimeType.class, lifetime);
        request.getAny().add(lifetimeJaxb);

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token
        RequestSecurityTokenResponseCollectionType response =
            issueOperation.issue(request, null, msgCtx);
        List<RequestSecurityTokenResponseType> securityTokenResponse =
            response.getRequestSecurityTokenResponse();
        assertFalse(securityTokenResponse.isEmpty());
    }

    /**
     * Test to successfully issue a single (dummy) token.
     */
    @org.junit.Test
    public void testIssueSingleToken() throws Exception {
        TokenIssueOperation issueOperation = new TokenIssueOperation();

        // Add Token Provider
        issueOperation.setTokenProviders(Collections.singletonList(
            new DummyTokenProvider()));

        // Add Service
        ServiceMBean service = new StaticService();
        service.setEndpoints(Collections.singletonList("http://dummy-service.com/dummy"));
        issueOperation.setServices(Collections.singletonList(service));

        // Add STSProperties object
        STSPropertiesMBean stsProperties = new StaticSTSProperties();
        issueOperation.setStsProperties(stsProperties);

        // Mock up a request
        RequestSecurityTokenType request = new RequestSecurityTokenType();
        JAXBElement<String> tokenType =
            new JAXBElement<String>(
                QNameConstants.TOKEN_TYPE, String.class, DummyTokenProvider.TOKEN_TYPE
            );
        request.getAny().add(tokenType);
        request.getAny().add(createAppliesToElement("http://dummy-service.com/dummy"));

        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);

        // Issue a token
        RequestSecurityTokenResponseType response =
            issueOperation.issueSingle(request, null, msgCtx);
        assertFalse(response.getAny().isEmpty());
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

    /**
     * Create a LifetimeType object given a lifetime in seconds
     */
    private LifetimeType createLifetime(long lifetime) {
        AttributedDateTime created = QNameConstants.UTIL_FACTORY.createAttributedDateTime();
        AttributedDateTime expires = QNameConstants.UTIL_FACTORY.createAttributedDateTime();

        if (lifetime <= 0) {
            lifetime = 300L;
        }
        Instant creationTime = Instant.now();
        Instant expirationTime = creationTime.plusSeconds(lifetime);

        created.setValue(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        expires.setValue(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));

        LifetimeType lifetimeType = QNameConstants.WS_TRUST_FACTORY.createLifetimeType();
        lifetimeType.setCreated(created);
        lifetimeType.setExpires(expires);
        return lifetimeType;
    }


}
