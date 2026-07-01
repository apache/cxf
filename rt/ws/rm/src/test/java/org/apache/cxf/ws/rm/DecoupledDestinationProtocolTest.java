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

package org.apache.cxf.ws.rm;

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
 * Verifies that the WS-RM decoupled-destination scheme allowlist prevents
 * SSRF via attacker-controlled wsa:ReplyTo / wsa:FaultTo addresses carried
 * in WS-RM messages. The guard is {@link ContextUtils#isDecoupledDestinationAllowed}
 * called from {@link InternalContextUtils}'s internal {@code DecoupledDestination}.
 */
public class DecoupledDestinationProtocolTest {

    @After
    public void clearSystemProperty() {
        System.clearProperty(ContextUtils.ALLOWED_DECOUPLED_DEST_SCHEMES_PROPERTY);
    }

    // -------------------------------------------------------------------------
    // Blocked by default
    // -------------------------------------------------------------------------

    @Test
    public void testFileSchemeIsRejected() throws Exception {
        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);

        Exchange exchange = buildExchange(bus);
        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination dest = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr("file:///etc/passwd"));
        Conduit backChannel = dest.getBackChannel(inMessage);

        assertNull("file:// must be blocked", backChannel);
        verify(cim, never()).getConduitInitiatorForUri(any());
    }

    @Test
    public void testCorbaSchemeIsRejected() throws Exception {
        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);

        Exchange exchange = buildExchange(bus);
        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination dest = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr("corba:NameService"));
        Conduit backChannel = dest.getBackChannel(inMessage);

        assertNull("corba: must be blocked", backChannel);
        verify(cim, never()).getConduitInitiatorForUri(any());
    }

    // -------------------------------------------------------------------------
    // Allowed by default
    // -------------------------------------------------------------------------

    @Test
    public void testHttpSchemeIsAllowed() throws Exception {
        final String replyTo = "http://callback.example.com/rm/reply";

        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        ConduitInitiator initiator = mock(ConduitInitiator.class);
        Conduit mockConduit = mock(Conduit.class);

        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);
        when(cim.getConduitInitiatorForUri(replyTo)).thenReturn(initiator);
        when(initiator.getConduit(
                any(EndpointInfo.class), any(EndpointReferenceType.class), any(Bus.class)))
            .thenReturn(mockConduit);

        Exchange exchange = buildExchange(bus);
        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination dest = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr(replyTo));
        Conduit backChannel = dest.getBackChannel(inMessage);

        assertNotNull("http:// must be allowed", backChannel);
        assertSame(mockConduit, backChannel);

        ArgumentCaptor<EndpointReferenceType> eprCaptor =
            ArgumentCaptor.forClass(EndpointReferenceType.class);
        verify(initiator).getConduit(
            any(EndpointInfo.class), eprCaptor.capture(), any(Bus.class));
        assertEquals(replyTo, eprCaptor.getValue().getAddress().getValue());
    }

    @Test
    public void testHttpsSchemeIsAllowed() throws Exception {
        final String replyTo = "https://callback.example.com/rm/reply";

        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        ConduitInitiator initiator = mock(ConduitInitiator.class);
        Conduit mockConduit = mock(Conduit.class);

        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);
        when(cim.getConduitInitiatorForUri(replyTo)).thenReturn(initiator);
        when(initiator.getConduit(
                any(EndpointInfo.class), any(EndpointReferenceType.class), any(Bus.class)))
            .thenReturn(mockConduit);

        Exchange exchange = buildExchange(bus);
        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination dest = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr(replyTo));
        Conduit backChannel = dest.getBackChannel(inMessage);

        assertNotNull("https:// must be allowed", backChannel);
        assertSame(mockConduit, backChannel);
    }

    // -------------------------------------------------------------------------
    // System-property override
    // -------------------------------------------------------------------------

    @Test
    public void testSystemPropertyOverrideAllowsFile() throws Exception {
        System.setProperty(
            ContextUtils.ALLOWED_DECOUPLED_DEST_SCHEMES_PROPERTY, "file://,http://");

        final String replyTo = "file:///some/permitted/path";

        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        ConduitInitiator initiator = mock(ConduitInitiator.class);
        Conduit mockConduit = mock(Conduit.class);

        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);
        when(cim.getConduitInitiatorForUri(replyTo)).thenReturn(initiator);
        when(initiator.getConduit(
                any(EndpointInfo.class), any(EndpointReferenceType.class), any(Bus.class)))
            .thenReturn(mockConduit);

        Exchange exchange = buildExchange(bus);
        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination dest = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr(replyTo));
        Conduit backChannel = dest.getBackChannel(inMessage);

        assertNotNull("file:// must be allowed when explicitly listed in the system property",
            backChannel);
    }

    @Test
    public void testSystemPropertyReplacesDefaults() throws Exception {
        System.setProperty(
            ContextUtils.ALLOWED_DECOUPLED_DEST_SCHEMES_PROPERTY, "jms:");

        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);

        Exchange exchange = buildExchange(bus);
        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        Destination dest = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr("http://callback.example.com/rm/reply"));
        Conduit backChannel = dest.getBackChannel(inMessage);

        assertNull("http:// must be blocked when absent from the system property", backChannel);
        verify(cim, never()).getConduitInitiatorForUri(any());
    }

    /**
     * Empty tokens from leading/trailing/consecutive commas in the system property
     * must not bypass the SSRF allowlist. An empty prefix makes
     * {@code uri.startsWith("")} true for every URI.
     */
    @Test
    public void testEmptyPropertyTokensDoNotBypassAllowlist() throws Exception {
        Bus bus = mock(Bus.class);
        ConduitInitiatorManager cim = mock(ConduitInitiatorManager.class);
        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(cim);

        Exchange exchange = buildExchange(bus);
        Message inMessage = new MessageImpl();
        inMessage.setExchange(exchange);

        // Leading comma: ",http://" — empty first token must not allow file://
        System.setProperty(ContextUtils.ALLOWED_DECOUPLED_DEST_SCHEMES_PROPERTY, ",http://");
        Destination dest = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr("file:///etc/passwd"));
        assertNull("leading comma must not allow file:// via empty prefix bypass",
            dest.getBackChannel(inMessage));
        verify(cim, never()).getConduitInitiatorForUri(any());

        // All-comma value: every token is empty; nothing should be allowed
        System.setProperty(ContextUtils.ALLOWED_DECOUPLED_DEST_SCHEMES_PROPERTY, ",,,");
        dest = InternalContextUtils.createDecoupledDestination(
            exchange, buildEpr("http://callback.example.com/"));
        assertNull("all-comma property must not allow any URI",
            dest.getBackChannel(inMessage));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Exchange buildExchange(Bus bus) {
        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getEndpointInfo()).thenReturn(new EndpointInfo());

        Exchange exchange = new ExchangeImpl();
        exchange.put(Bus.class, bus);
        exchange.put(Endpoint.class, endpoint);
        return exchange;
    }

    private static EndpointReferenceType buildEpr(String uri) {
        AttributedURIType address = new AttributedURIType();
        address.setValue(uri);
        EndpointReferenceType epr = new EndpointReferenceType();
        epr.setAddress(address);
        return epr;
    }
}
