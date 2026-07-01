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

package org.apache.cxf.ws.addressing.impl;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the WS-Addressing ReplyTo/FaultTo allowed protocols
 *
 * <p>The scheme allowlist in {@link ContextUtils} prevents CXF from opening outbound
 * connections to attacker-controlled URIs supplied in wsa:ReplyTo or wsa:FaultTo
 * headers. These tests verify that disallowed schemes (e.g. file://) are rejected
 * and that allowed schemes (e.g. http://, jms:) continue to work.
 * Both the {@link InternalContextUtils} decoupled-destination path (server ReplyTo)
 * and the {@link ContextUtils} decoupled-destination path (fault forwarding via
 * {@code DecoupledFaultHandler} / {@code RMInInterceptor}) are covered.
 */
public class DecoupledDestinationProtocolTest {

    @After
    public void clearSystemProperty() {
        System.clearProperty(ContextUtils.ALLOWED_DECOUPLED_DEST_SCHEMES_PROPERTY);
    }

    // -------------------------------------------------------------------------
    // Blocked by default
    // -------------------------------------------------------------------------

    /**
     * An attacker supplies wsa:ReplyTo = file:///etc/passwd.
     * getBackChannel() must return null and must NOT call getConduitInitiatorForUri.
     */
    @Test
    public void testFileSchemeReplyToIsRejected() throws Exception {
        final String attackerReplyTo = "file:///etc/passwd";

        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(new EndpointInfo());

        Exchange exchange = new ExchangeImpl();
        exchange.put(Bus.class, bus);
        exchange.put(Endpoint.class, endpoint);

        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination destination = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr(attackerReplyTo));
        Conduit backChannel = destination.getBackChannel(inMessage);

        assertNull("file:// ReplyTo must be blocked; no conduit should be returned", backChannel);
        // The ConduitInitiatorManager must never be consulted for a disallowed scheme.
        verify(cim, never()).getConduitInitiatorForUri(any());
    }

    /**
     * Same attack vector via wsa:FaultTo.
     */
    @Test
    public void testFileSchemeViaFaultToIsRejected() throws Exception {
        final String attackerFaultTo = "file:///var/log/app.log";

        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(new EndpointInfo());

        Exchange exchange = new ExchangeImpl();
        exchange.put(Bus.class, bus);
        exchange.put(Endpoint.class, endpoint);

        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination destination = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr(attackerFaultTo));
        Conduit backChannel = destination.getBackChannel(inMessage);

        assertNull("file:// FaultTo must be blocked; no conduit should be returned", backChannel);
        verify(cim, never()).getConduitInitiatorForUri(any());
    }

    // -------------------------------------------------------------------------
    // Allowed by default
    // -------------------------------------------------------------------------

    /**
     * A legitimate http:// ReplyTo must pass through and reach getConduitInitiatorForUri.
     */
    @Test
    public void testHttpSchemeReplyToIsAllowed() throws Exception {
        final String replyTo = "http://callback.example.com/reply";

        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        ConduitInitiator httpInitiator = mock(ConduitInitiator.class);
        Conduit mockConduit = mock(Conduit.class);

        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);
        when(cim.getConduitInitiatorForUri(replyTo)).thenReturn(httpInitiator);
        when(httpInitiator.getConduit(
                any(EndpointInfo.class), any(EndpointReferenceType.class), any(Bus.class)))
            .thenReturn(mockConduit);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(new EndpointInfo());

        Exchange exchange = new ExchangeImpl();
        exchange.put(Bus.class, bus);
        exchange.put(Endpoint.class, endpoint);

        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination destination = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr(replyTo));
        Conduit backChannel = destination.getBackChannel(inMessage);

        assertNotNull("http:// ReplyTo must be allowed", backChannel);
        assertSame(mockConduit, backChannel);

        ArgumentCaptor<EndpointReferenceType> eprCaptor =
            ArgumentCaptor.forClass(EndpointReferenceType.class);
        verify(httpInitiator).getConduit(
            any(EndpointInfo.class), eprCaptor.capture(), any(Bus.class));
        assertEquals(replyTo, eprCaptor.getValue().getAddress().getValue());
    }

    /**
     * A legitimate jms: ReplyTo must pass through.
     */
    @Test
    public void testJmsSchemeReplyToIsAllowed() throws Exception {
        final String replyTo = "jms:queue:replies?timeToLive=1000";

        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        ConduitInitiator jmsInitiator = mock(ConduitInitiator.class);
        Conduit mockConduit = mock(Conduit.class);

        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);
        when(cim.getConduitInitiatorForUri(replyTo)).thenReturn(jmsInitiator);
        when(jmsInitiator.getConduit(
                any(EndpointInfo.class), any(EndpointReferenceType.class), any(Bus.class)))
            .thenReturn(mockConduit);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(new EndpointInfo());

        Exchange exchange = new ExchangeImpl();
        exchange.put(Bus.class, bus);
        exchange.put(Endpoint.class, endpoint);

        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination destination = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr(replyTo));
        Conduit backChannel = destination.getBackChannel(inMessage);

        assertNotNull("jms: ReplyTo must be allowed", backChannel);
        assertSame(mockConduit, backChannel);
    }

    // -------------------------------------------------------------------------
    // System-property override
    // -------------------------------------------------------------------------

    /**
     * When the system property explicitly lists file://, an operator-configured
     * deployment can permit it (opt-in). The conduit must be resolved normally.
     */
    @Test
    public void testFileSchemeAllowedViaSystemProperty() throws Exception {
        System.setProperty(
            ContextUtils.ALLOWED_DECOUPLED_DEST_SCHEMES_PROPERTY, "file://,http://");

        final String replyTo = "file:///some/allowed/path";

        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        ConduitInitiator fileInitiator = mock(ConduitInitiator.class);
        Conduit mockConduit = mock(Conduit.class);

        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);
        when(cim.getConduitInitiatorForUri(replyTo)).thenReturn(fileInitiator);
        when(fileInitiator.getConduit(
                any(EndpointInfo.class), any(EndpointReferenceType.class), any(Bus.class)))
            .thenReturn(mockConduit);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(new EndpointInfo());

        Exchange exchange = new ExchangeImpl();
        exchange.put(Bus.class, bus);
        exchange.put(Endpoint.class, endpoint);

        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination destination = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr(replyTo));
        Conduit backChannel = destination.getBackChannel(inMessage);

        assertNotNull("file:// must be allowed when explicitly listed in the system property", backChannel);
        assertSame(mockConduit, backChannel);
    }

    /**
     * When the system property is set, only its schemes are allowed — the defaults
     * do not apply. An http:// address must be rejected if http:// is absent from the
     * property value.
     */
    @Test
    public void testSystemPropertyReplacesDefaults() throws Exception {
        System.setProperty(
            ContextUtils.ALLOWED_DECOUPLED_DEST_SCHEMES_PROPERTY, "jms:");

        final String replyTo = "http://callback.example.com/reply";

        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(new EndpointInfo());

        Exchange exchange = new ExchangeImpl();
        exchange.put(Bus.class, bus);
        exchange.put(Endpoint.class, endpoint);

        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination destination = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr(replyTo));
        Conduit backChannel = destination.getBackChannel(inMessage);

        assertNull("http:// must be blocked when absent from the system property", backChannel);
        verify(cim, never()).getConduitInitiatorForUri(any());
    }

    // -------------------------------------------------------------------------
    // Unit tests for the helper
    // -------------------------------------------------------------------------

    @Test
    public void testIsReplyToSchemeAllowedDefaults() {
        // Allowed by default
        for (String allowed : ContextUtils.DEFAULT_ALLOWED_DECOUPLED_DEST_SCHEMES) {
            assertEquals("Expected allowed: " + allowed,
                true, ContextUtils.isDecoupledDestinationAllowed(allowed + "host/path"));
        }
        // Blocked by default
        assertEquals(false, ContextUtils.isDecoupledDestinationAllowed("file:///etc/passwd"));
        assertEquals(false, ContextUtils.isDecoupledDestinationAllowed("corba:something"));
        assertEquals(false, ContextUtils.isDecoupledDestinationAllowed("IOR:abc"));
        assertEquals(false, ContextUtils.isDecoupledDestinationAllowed("udp://host"));
        assertEquals(false, ContextUtils.isDecoupledDestinationAllowed(null));
    }

    /**
     * Empty tokens produced by leading/trailing/consecutive commas in the system
     * property must not act as a wildcard prefix that allows every URI.
     * "".startsWith("") is always true, so empty entries must be skipped.
     */
    @Test
    public void testEmptyPropertyTokensDoNotBypassAllowlist() {
        // Leading comma — produces an empty first token
        System.setProperty(ContextUtils.ALLOWED_DECOUPLED_DEST_SCHEMES_PROPERTY,
            ",http://");
        assertEquals("leading comma must not allow file://",
            false, ContextUtils.isDecoupledDestinationAllowed("file:///etc/passwd"));
        assertEquals("leading comma must still allow http://",
            true, ContextUtils.isDecoupledDestinationAllowed("http://legit.example.com/"));

        // Trailing comma — produces an empty last token
        System.setProperty(ContextUtils.ALLOWED_DECOUPLED_DEST_SCHEMES_PROPERTY,
            "http://,");
        assertEquals("trailing comma must not allow file://",
            false, ContextUtils.isDecoupledDestinationAllowed("file:///etc/passwd"));

        // Consecutive commas — produces an empty middle token
        System.setProperty(ContextUtils.ALLOWED_DECOUPLED_DEST_SCHEMES_PROPERTY,
            "http://,,https://");
        assertEquals("consecutive commas must not allow corba:",
            false, ContextUtils.isDecoupledDestinationAllowed("corba:NameService"));

        // Only commas — every token is empty; nothing should be allowed
        System.setProperty(ContextUtils.ALLOWED_DECOUPLED_DEST_SCHEMES_PROPERTY, ",,,");
        assertEquals("all-comma property must not allow http://",
            false, ContextUtils.isDecoupledDestinationAllowed("http://example.com/"));
        assertEquals("all-comma property must not allow file://",
            false, ContextUtils.isDecoupledDestinationAllowed("file:///etc/passwd"));
    }

    // -------------------------------------------------------------------------
    // ContextUtils.createDecoupledDestination — fault-forwarding path
    // (used by DecoupledFaultHandler and RMInInterceptor)
    // -------------------------------------------------------------------------

    /**
     * A malicious server sends a SOAP fault to the client's decoupled listener
     * with wsa:FaultTo = file:///etc/passwd. The ContextUtils destination must
     * block the connection and return null without consulting ConduitInitiatorManager.
     */
    @Test
    public void testContextUtilsFaultToFileSchemeIsRejected() throws Exception {
        final String attackerFaultTo = "file:///etc/passwd";

        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(new EndpointInfo());

        Exchange exchange = new ExchangeImpl();
        exchange.put(Bus.class, bus);
        exchange.put(Endpoint.class, endpoint);

        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination destination = ContextUtils.createDecoupledDestination(
            exchange, buildEpr(attackerFaultTo));
        Conduit backChannel = destination.getBackChannel(inMessage);

        assertNull("file:// FaultTo via ContextUtils must be blocked", backChannel);
        verify(cim, never()).getConduitInitiatorForUri(any());
    }

    /**
     * An http:// FaultTo via ContextUtils.createDecoupledDestination must pass
     * the scheme check and reach getConduitInitiatorForUri normally.
     */
    @Test
    public void testContextUtilsFaultToHttpSchemeIsAllowed() throws Exception {
        final String faultTo = "http://callback.example.com/fault";

        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        ConduitInitiator httpInitiator = mock(ConduitInitiator.class);
        Conduit mockConduit = mock(Conduit.class);

        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);
        when(cim.getConduitInitiatorForUri(faultTo)).thenReturn(httpInitiator);
        when(httpInitiator.getConduit(
                any(EndpointInfo.class), any(EndpointReferenceType.class), any(Bus.class)))
            .thenReturn(mockConduit);

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(new EndpointInfo());

        Exchange exchange = new ExchangeImpl();
        exchange.put(Bus.class, bus);
        exchange.put(Endpoint.class, endpoint);

        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination destination = ContextUtils.createDecoupledDestination(
            exchange, buildEpr(faultTo));
        Conduit backChannel = destination.getBackChannel(inMessage);

        assertNotNull("http:// FaultTo via ContextUtils must be allowed", backChannel);
        assertSame(mockConduit, backChannel);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static EndpointReferenceType buildEpr(String uri) {
        AttributedURIType address = new AttributedURIType();
        address.setValue(uri);
        EndpointReferenceType epr = new EndpointReferenceType();
        epr.setAddress(address);
        return epr;
    }
}
