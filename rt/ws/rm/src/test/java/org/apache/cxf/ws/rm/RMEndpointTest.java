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

import java.util.ArrayList;
import java.util.List;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyEngineImpl;
import org.apache.cxf.ws.rm.v200702.Identifier;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RMEndpointTest {

    private RMManager manager;
    private Endpoint ae;
    private RMEndpoint rme;

    @Before
    public void setUp() {
        manager = mock(RMManager.class);
        ae = mock(Endpoint.class);
        EndpointInfo ei = new EndpointInfo();
        ei.setBinding(new SoapBindingInfo(null, null, Soap12.getInstance()));
        when(ae.getEndpointInfo()).thenReturn(ei);
        rme = new RMEndpoint(manager, ae);
    }

    @Test
    public void testConstructor() {
        assertNotNull(rme);
        assertNull(rme.getEndpoint(ProtocolVariation.RM10WSA200408));
        assertNull(rme.getService(ProtocolVariation.RM10WSA200408));
        assertNull(rme.getConduit());
        assertNull(rme.getReplyTo());
    }

    @Test
    public void testGetManager() {
        assertSame(manager, rme.getManager());
    }

    @Test
    public void testGetApplicationEndpoint() {
        assertSame(ae, rme.getApplicationEndpoint());
    }

    @Test
    public void testGetProxy() {
        assertSame(rme, rme.getProxy().getReliableEndpoint());
    }

    @Test
    public void testGetServant() {
        assertNotNull(rme.getServant());
    }

    @Test
    public void testGetSetDestination() {
        Destination d = mock(Destination.class);
        assertSame(rme, rme.getDestination().getReliableEndpoint());
        rme.setDestination(d);
        assertSame(d, rme.getDestination());
    }

    @Test
    public void testGetSetSource() {
        Source s = mock(Source.class);
        assertSame(rme, rme.getSource().getReliableEndpoint());
        rme.setSource(s);
        assertSame(s, rme.getSource());
    }

    @Test
    public void testInitialise() throws NoSuchMethodException {
        Message m = new MessageImpl();
        Bus bus = mock(Bus.class);
        when(manager.getBus()).thenReturn(bus);

        rme = spy(new RMEndpoint(manager, ae));
        rme.createServices();
        rme.createEndpoints(null);
        rme.setPolicies(m);

        Conduit c = mock(Conduit.class);
        EndpointReferenceType epr = mock(EndpointReferenceType.class);

        rme.initialise(new RMConfiguration(), c, epr, null, m);
        assertSame(c, rme.getConduit());
        assertSame(epr, rme.getReplyTo());
    }

    @Test
    public void testCreateService() {
        Service as = mock(Service.class);
        when(ae.getService()).thenReturn(as);
        rme.createServices();
        Service s = rme.getService(ProtocolVariation.RM10WSA200408);
        assertNotNull(s);
        WrappedService ws = (WrappedService)s;
        assertSame(as, ws.getWrappedService());
        assertSame(rme.getServant(), s.getInvoker());
        verifyService();
    }

    @Test
    public void testCreateEndpoint() throws NoSuchMethodException, EndpointException {
        Service as = mock(Service.class);
        EndpointInfo aei = new EndpointInfo();
        ae = new EndpointImpl(null, as, aei);
        rme = spy(new RMEndpoint(manager, ae));
        rme.setAplicationEndpoint(ae);
        rme.setManager(manager);
        SoapBindingInfo bi = mock(SoapBindingInfo.class);
        aei.setBinding(bi);
        SoapVersion sv = Soap11.getInstance();
        when(bi.getSoapVersion()).thenReturn(sv);
        String ns = "http://schemas.xmlsoap.org/wsdl/soap/";
        when(bi.getBindingId()).thenReturn(ns);
        aei.setTransportId(ns);
        String addr = "addr";
        aei.setAddress(addr);
        Object ua = new Object();
        when(rme.getUsingAddressing(aei)).thenReturn(ua);

        rme.createServices();
        rme.createEndpoints(null);
        Endpoint e = rme.getEndpoint(ProtocolVariation.RM10WSA200408);
        WrappedEndpoint we = (WrappedEndpoint)e;
        assertSame(ae, we.getWrappedEndpoint());
        Service s = rme.getService(ProtocolVariation.RM10WSA200408);
        assertEquals(1, s.getEndpoints().size());
        assertSame(e, s.getEndpoints().get(RM10Constants.PORT_NAME));
    }

    @Test
    public void testGetUsingAddressing() {
        EndpointInfo ei = null;
        assertNull(rme.getUsingAddressing(ei));

        ExtensibilityElement ua = mock(ExtensibilityElement.class);
        ei = mock(EndpointInfo.class);
        List<ExtensibilityElement> noExts = new ArrayList<>();
        List<ExtensibilityElement> exts = new ArrayList<>();
        exts.add(ua);
        when(ei.getExtensors(ExtensibilityElement.class)).thenReturn(noExts);
        BindingInfo bi = mock(BindingInfo.class);
        when(ei.getBinding()).thenReturn(bi);
        when(bi.getExtensors(ExtensibilityElement.class)).thenReturn(noExts);
        ServiceInfo si = mock(ServiceInfo.class);
        when(ei.getService()).thenReturn(si);
        when(si.getExtensors(ExtensibilityElement.class)).thenReturn(exts);
        when(ua.getElementType()).thenReturn(Names.WSAW_USING_ADDRESSING_QNAME);

        assertSame(ua, rme.getUsingAddressing(ei));
        verify(ei, times(2)).getBinding();
        verify(ei, times(2)).getService();
    }

    @Test
    public void testGetUsingAddressingFromExtensions() {
        List<ExtensibilityElement> exts = new ArrayList<>();
        ExtensibilityElement ua = mock(ExtensibilityElement.class);
        exts.add(ua);
        when(ua.getElementType()).thenReturn(Names.WSAW_USING_ADDRESSING_QNAME);
        assertSame(ua, rme.getUsingAddressing(exts));
    }

    @Test
    public void testMessageArrivals() {
        assertEquals(0L, rme.getLastApplicationMessage());
        assertEquals(0L, rme.getLastControlMessage());
        rme.receivedControlMessage();
        assertEquals(0L, rme.getLastApplicationMessage());
        assertTrue(rme.getLastControlMessage() > 0);
        rme.receivedApplicationMessage();
        assertTrue(rme.getLastApplicationMessage() > 0);
        assertTrue(rme.getLastControlMessage() > 0);
    }

    @Test
    public void testSetPoliciesNoEngine() {
        Message m = new MessageImpl();
        Bus bus = mock(Bus.class);
        when(manager.getBus()).thenReturn(bus);
        when(bus.getExtension(PolicyEngine.class)).thenReturn(null);
        rme.setPolicies(m);
    }

    @Test
    public void testSetPoliciesEngineDisabled() {
        Message m = new MessageImpl();
        Bus bus = mock(Bus.class);
        when(manager.getBus()).thenReturn(bus);
        PolicyEngineImpl pe = mock(PolicyEngineImpl.class);
        when(bus.getExtension(PolicyEngine.class)).thenReturn(pe);
        when(pe.isEnabled()).thenReturn(false);
        rme.setPolicies(m);
    }

    @Test
    public void testShutdown() {
        DestinationSequence ds = mock(DestinationSequence.class);
        Identifier did = mock(Identifier.class);
        when(ds.getIdentifier()).thenReturn(did);
        when(ds.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);
        String d = "d";
        when(did.getValue()).thenReturn(d);
        SourceSequence ss = mock(SourceSequence.class);
        Identifier sid = mock(Identifier.class);
        when(ss.getIdentifier()).thenReturn(sid);
        when(ss.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);
        String s = "s";
        when(sid.getValue()).thenReturn(s);
        RetransmissionQueue queue = mock(RetransmissionQueue.class);
        when(manager.getRetransmissionQueue()).thenReturn(queue);
        RedeliveryQueue dqueue = mock(RedeliveryQueue.class);
        when(manager.getRedeliveryQueue()).thenReturn(dqueue);

        rme.getDestination().addSequence(ds, false);
        rme.getSource().addSequence(ss, false);
        rme.shutdown();

        verify(ds, atLeastOnce()).cancelDeferredAcknowledgments();
        verify(ds, atLeastOnce()).cancelTermination();
        verify(queue, atLeastOnce()).stop(ss);
        verify(dqueue, atLeastOnce()).stop(ds);
    }

    private void verifyService() {
        Service service = rme.getService(ProtocolVariation.RM10WSA200408);
        ServiceInfo si = service.getServiceInfos().get(0);
        assertNotNull("service info is null", si);

        InterfaceInfo intf = si.getInterface();

        assertEquals(8, intf.getOperations().size());

        String ns = RM10Constants.NAMESPACE_URI;
        OperationInfo oi = intf.getOperation(new QName(ns, "CreateSequence"));
        assertNotNull("No operation info.", oi);
        assertFalse("Operation is oneway.", oi.isOneWay());
        assertFalse("Operation is unwrapped.", oi.isUnwrapped());
        assertFalse("Operation is unwrappedCapable.", oi.isUnwrappedCapable());
        assertNull("Unexpected unwrapped operation.", oi.getUnwrappedOperation());

        oi = intf.getOperation(new QName(ns, "TerminateSequence"));
        assertNotNull("No operation info.", oi);
        assertTrue("Operation is toway.", oi.isOneWay());

        oi = intf.getOperation(new QName(ns, "TerminateSequenceAnonymous"));
        assertNotNull("No operation info.", oi);
        assertFalse("Operation is oneway.", oi.isOneWay());

        oi = intf.getOperation(new QName(ns, "SequenceAcknowledgement"));
        assertNotNull("No operation info.", oi);
        assertTrue("Operation is toway.", oi.isOneWay());

        oi = intf.getOperation(new QName(ns, "CloseSequence"));
        assertNotNull("No operation info.", oi);
        assertTrue("Operation is toway.", oi.isOneWay());

        oi = intf.getOperation(new QName(ns, "AckRequested"));
        assertNotNull("No operation info.", oi);
        assertTrue("Operation is toway.", oi.isOneWay());

        oi = intf.getOperation(new QName(ns, "CreateSequenceOneway"));
        assertNotNull("No operation info.", oi);
        assertTrue("Operation is toway.", oi.isOneWay());

        oi = intf.getOperation(new QName(ns, "CreateSequenceResponseOneway"));
        assertNotNull("No operation info.", oi);
        assertTrue("Operation is toway.", oi.isOneWay());
    }
}
