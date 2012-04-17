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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistry;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RMEndpointTest extends Assert {

    private IMocksControl control;
    private RMManager manager;
    private Endpoint ae;
    private RMEndpoint rme;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        manager = control.createMock(RMManager.class);
        ae = control.createMock(Endpoint.class);
        rme = new RMEndpoint(manager, ae, ProtocolVariation.RM10WSA200408);
    }

    @After
    public void tearDown() {
        control.verify();
    }

    @Test
    public void testConstructor() {
        control.replay();
        assertNotNull(rme);
        assertNull(rme.getEndpoint());
        assertNull(rme.getService());
        assertNull(rme.getConduit());
        assertNull(rme.getReplyTo());
    }

    @Test
    public void testGetManager() {
        control.replay();
        assertSame(manager, rme.getManager());
    }

    @Test
    public void testGetApplicationEndpoint() {
        control.replay();
        assertSame(ae, rme.getApplicationEndpoint());
    }

    @Test
    public void testGetProxy() {
        control.replay();
        assertSame(rme, rme.getProxy().getReliableEndpoint());
    }

    @Test
    public void testGetServant() {
        control.replay();
        assertNotNull(rme.getServant());
    }

    @Test
    public void testGetSetDestination() {
        Destination d = control.createMock(Destination.class);
        control.replay();
        assertSame(rme, rme.getDestination().getReliableEndpoint());
        rme.setDestination(d);
        assertSame(d, rme.getDestination());
    }

    @Test
    public void testGetSetSource() {
        Source s = control.createMock(Source.class);
        control.replay();
        assertSame(rme, rme.getSource().getReliableEndpoint());
        rme.setSource(s);
        assertSame(s, rme.getSource());
    }

    @Test
    public void testInitialise() throws NoSuchMethodException {
        Method m1 = RMEndpoint.class.getDeclaredMethod("createService", new Class[] {});
        Method m2 = RMEndpoint.class
            .getDeclaredMethod("createEndpoint", new Class[] {org.apache.cxf.transport.Destination.class});
        Method m3 = RMEndpoint.class.getDeclaredMethod("setPolicies", new Class[] {});

        rme = EasyMock.createMockBuilder(RMEndpoint.class)
            .addMockedMethods(m1, m2 , m3).createMock(control);
        rme.createService();
        EasyMock.expectLastCall();
        rme.createEndpoint(null);
        EasyMock.expectLastCall();
        rme.setPolicies();
        EasyMock.expectLastCall();
        Conduit c = control.createMock(Conduit.class);
        EndpointReferenceType epr = control.createMock(EndpointReferenceType.class);
        control.replay();
        rme.initialise(c, epr, null);
        assertSame(c, rme.getConduit());
        assertSame(epr, rme.getReplyTo());
    }

    @Test
    public void testCreateService() {
        Service as = control.createMock(Service.class);
        EasyMock.expect(ae.getService()).andReturn(as);
        control.replay();
        rme.createService();
        Service s = rme.getService();
        assertNotNull(s);
        WrappedService ws = (WrappedService)s;
        assertSame(as, ws.getWrappedService());
        assertSame(rme.getServant(), s.getInvoker());
        verifyService();
    }

    @Test
    public void testCreateEndpoint() throws NoSuchMethodException {
        Method m = RMEndpoint.class.getDeclaredMethod("getUsingAddressing", new Class[] {EndpointInfo.class});
        rme = EasyMock.createMockBuilder(RMEndpoint.class)
            .addMockedMethod(m).createMock(control);
        rme.setAplicationEndpoint(ae);
        rme.setManager(manager);
        rme.setProtocol(ProtocolVariation.RM10WSA200408);
        Service as = control.createMock(Service.class);
        EasyMock.expect(ae.getService()).andReturn(as);
        EndpointInfo aei = control.createMock(EndpointInfo.class);
        EasyMock.expect(ae.getEndpointInfo()).andReturn(aei).times(2);
        SoapBindingInfo bi = control.createMock(SoapBindingInfo.class);
        EasyMock.expect(aei.getBinding()).andReturn(bi); 
        SoapVersion sv = Soap11.getInstance();
        EasyMock.expect(bi.getSoapVersion()).andReturn(sv);
        String ns = "http://schemas.xmlsoap.org/wsdl/soap/";
        EasyMock.expect(bi.getBindingId()).andReturn(ns);
        EasyMock.expect(aei.getTransportId()).andReturn(ns);
        String addr = "addr";
        EasyMock.expect(aei.getAddress()).andReturn(addr);
        Object ua = new Object();
        EasyMock.expect(rme.getUsingAddressing(aei)).andReturn(ua);
        control.replay();
        rme.createService();
        rme.createEndpoint(null);
        Endpoint e = rme.getEndpoint();
        WrappedEndpoint we = (WrappedEndpoint)e;
        assertSame(ae, we.getWrappedEndpoint());
        Service s = rme.getService();
        assertEquals(1, s.getEndpoints().size());
        assertSame(e, s.getEndpoints().get(RM10Constants.PORT_NAME));
    }

    @Test
    public void testGetUsingAddressing() {
        EndpointInfo ei = null;
        control.replay();
        assertNull(rme.getUsingAddressing(ei));
        control.verify();

        control.reset();
        ExtensibilityElement ua = control.createMock(ExtensibilityElement.class);
        ei = control.createMock(EndpointInfo.class);
        List<ExtensibilityElement> noExts = new ArrayList<ExtensibilityElement>();
        List<ExtensibilityElement> exts = new ArrayList<ExtensibilityElement>();
        exts.add(ua);
        EasyMock.expect(ei.getExtensors(ExtensibilityElement.class)).andReturn(noExts);
        BindingInfo bi = control.createMock(BindingInfo.class);
        EasyMock.expect(ei.getBinding()).andReturn(bi).times(2);
        EasyMock.expect(bi.getExtensors(ExtensibilityElement.class)).andReturn(noExts);
        ServiceInfo si = control.createMock(ServiceInfo.class);
        EasyMock.expect(ei.getService()).andReturn(si).times(2);
        EasyMock.expect(si.getExtensors(ExtensibilityElement.class)).andReturn(exts);
        EasyMock.expect(ua.getElementType()).andReturn(Names.WSAW_USING_ADDRESSING_QNAME);
        control.replay();
        assertSame(ua, rme.getUsingAddressing(ei));
    }

    @Test
    public void testGetUsingAddressingFromExtensions() {
        List<ExtensibilityElement> exts = new ArrayList<ExtensibilityElement>();
        ExtensibilityElement ua = control.createMock(ExtensibilityElement.class);
        exts.add(ua);
        EasyMock.expect(ua.getElementType()).andReturn(Names.WSAW_USING_ADDRESSING_QNAME);
        control.replay();
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
        control.replay();
    }

    @Test
    public void testSetPoliciesNoEngine() {
        Bus bus = control.createMock(Bus.class);
        EasyMock.expect(manager.getBus()).andReturn(bus);
        EasyMock.expect(bus.getExtension(PolicyEngine.class)).andReturn(null);
        control.replay();
        rme.setPolicies();
    }

    @Test
    public void testSetPoliciesEngineDisabled() {
        Bus bus = control.createMock(Bus.class);
        EasyMock.expect(manager.getBus()).andReturn(bus);
        PolicyEngine pe = control.createMock(PolicyEngine.class);
        EasyMock.expect(bus.getExtension(PolicyEngine.class)).andReturn(pe);
        EasyMock.expect(pe.isEnabled()).andReturn(false);
        control.replay();
        rme.setPolicies();
    }

    @Test
    public void testSetPolicies() throws NoSuchMethodException {
        Method m = RMEndpoint.class.getDeclaredMethod("getEndpoint", new Class[] {});
        rme = EasyMock.createMockBuilder(RMEndpoint.class)
            .addMockedMethod(m).createMock(control);
        rme.setAplicationEndpoint(ae);
        rme.setManager(manager);
        Endpoint e = control.createMock(Endpoint.class);
        EasyMock.expect(rme.getEndpoint()).andReturn(e);
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        EasyMock.expect(e.getEndpointInfo()).andReturn(ei);
        Bus bus = control.createMock(Bus.class);
        EasyMock.expect(manager.getBus()).andReturn(bus).times(2);
        PolicyEngine pe = control.createMock(PolicyEngine.class);
        EasyMock.expect(bus.getExtension(PolicyEngine.class)).andReturn(pe);
        EasyMock.expect(pe.isEnabled()).andReturn(true);
        PolicyInterceptorProviderRegistry reg = control.createMock(PolicyInterceptorProviderRegistry.class);
        EasyMock.expect(bus.getExtension(PolicyInterceptorProviderRegistry.class)).andReturn(reg);
        EndpointInfo aei = control.createMock(EndpointInfo.class);
        EasyMock.expect(ae.getEndpointInfo()).andReturn(aei);
        EndpointPolicy epi = control.createMock(EndpointPolicy.class);
        EasyMock.expect(pe.getServerEndpointPolicy(aei, null)).andReturn(epi);
        EasyMock.expect(epi.getChosenAlternative()).andReturn(new ArrayList<Assertion>());

        pe.setServerEndpointPolicy(ei, epi);
        EasyMock.expectLastCall();
        BindingInfo bi = control.createMock(BindingInfo.class);
        EasyMock.expect(ei.getBinding()).andReturn(bi);
        BindingOperationInfo boi = control.createMock(BindingOperationInfo.class);
        EasyMock.expect(bi.getOperations()).andReturn(Collections.singletonList(boi));
        pe.setEffectiveServerRequestPolicy(EasyMock.eq(ei), EasyMock.eq(boi), EasyMock
            .isA(EffectivePolicy.class));
        EasyMock.expectLastCall();
        pe.setEffectiveServerResponsePolicy(EasyMock.eq(ei), EasyMock.eq(boi), EasyMock
            .isA(EffectivePolicy.class));
        EasyMock.expectLastCall();
        pe.setEffectiveClientRequestPolicy(EasyMock.eq(ei), EasyMock.eq(boi), EasyMock
            .isA(EffectivePolicy.class));
        EasyMock.expectLastCall();
        pe.setEffectiveClientResponsePolicy(EasyMock.eq(ei), EasyMock.eq(boi), EasyMock
            .isA(EffectivePolicy.class));
        EasyMock.expectLastCall();
        control.replay();
        rme.setPolicies();
    }

    @Test
    public void testShutdown() {
        DestinationSequence ds = control.createMock(DestinationSequence.class);
        Identifier did = control.createMock(Identifier.class);
        EasyMock.expect(ds.getIdentifier()).andReturn(did).anyTimes();
        EasyMock.expect(ds.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        String d = "d";
        EasyMock.expect(did.getValue()).andReturn(d).anyTimes();        
        SourceSequence ss = control.createMock(SourceSequence.class);
        Identifier sid = control.createMock(Identifier.class);
        EasyMock.expect(ss.getIdentifier()).andReturn(sid).anyTimes();
        EasyMock.expect(ss.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        String s = "s";
        EasyMock.expect(sid.getValue()).andReturn(s).anyTimes();        
        ds.cancelDeferredAcknowledgments();
        EasyMock.expectLastCall().anyTimes();
        ds.cancelTermination();
        EasyMock.expectLastCall().anyTimes();
        RetransmissionQueue queue = control.createMock(RetransmissionQueue.class);
        EasyMock.expect(manager.getRetransmissionQueue()).andReturn(queue).anyTimes();
        queue.stop(ss);
        EasyMock.expectLastCall().anyTimes();
        control.replay();
        rme.getDestination().addSequence(ds, false);
        rme.getSource().addSequence(ss, false);
        rme.shutdown();
    }

    @Test
    public void testEffectivePolicyImpl() {
        EndpointPolicy ep = control.createMock(EndpointPolicy.class);
        Collection<Assertion> alt = new ArrayList<Assertion>();
        EasyMock.expect(ep.getChosenAlternative()).andReturn(alt).times(2);
        PolicyInterceptorProviderRegistry reg = control.createMock(PolicyInterceptorProviderRegistry.class);
        List<Interceptor<? extends Message>> li = new ArrayList<Interceptor<? extends Message>>();
        EasyMock.expect(reg.getInterceptors(alt, true, false)).andReturn(li);
        Policy p = control.createMock(Policy.class);
        EasyMock.expect(ep.getPolicy()).andReturn(p);
        control.replay();
        EffectivePolicy effective = rme.new EffectivePolicyImpl(ep, reg, true, false);
        assertSame(alt, effective.getChosenAlternative());
        assertSame(li, effective.getInterceptors());
        assertSame(p, effective.getPolicy());
    }

    private void verifyService() {
        Service service = rme.getService();
        ServiceInfo si = service.getServiceInfos().get(0);
        assertNotNull("service info is null", si);

        InterfaceInfo intf = si.getInterface();

        assertEquals(8, intf.getOperations().size());

        String ns = RM10Constants.NAMESPACE_URI;
        OperationInfo oi = intf.getOperation(new QName(ns, "CreateSequence"));
        assertNotNull("No operation info.", oi);
        assertTrue("Operation is oneway.", !oi.isOneWay());
        assertTrue("Operation is unwrapped.", !oi.isUnwrapped());
        assertTrue("Operation is unwrappedCapable.", !oi.isUnwrappedCapable());
        assertNull("Unexpected unwrapped operation.", oi.getUnwrappedOperation());

        oi = intf.getOperation(new QName(ns, "TerminateSequence"));
        assertNotNull("No operation info.", oi);
        assertTrue("Operation is toway.", oi.isOneWay());

        oi = intf.getOperation(new QName(ns, "TerminateSequenceAnonymous"));
        assertNotNull("No operation info.", oi);
        assertTrue("Operation is oneway.", !oi.isOneWay());

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