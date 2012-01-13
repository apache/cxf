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
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.Duration;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxb.DatatypeFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.workqueue.SynchronousExecutor;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.RelatesToType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.OfferType;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class ProxyTest extends Assert {

    private IMocksControl control;
    private RMEndpoint rme;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        rme = control.createMock(RMEndpoint.class);
    }

    @After
    public void tearDown() {
        control.verify();
    }
    
    @Test
    public void testCtor() {
        Proxy proxy = new Proxy(rme);
        assertSame(rme, proxy.getReliableEndpoint());  
        control.replay();
    }
    
    @Test
    public void testOfferedIdentifier() { 
        OfferType offer = control.createMock(OfferType.class);        
        Identifier id = control.createMock(Identifier.class);
        EasyMock.expect(offer.getIdentifier()).andReturn(id).anyTimes();
        control.replay();
        Proxy proxy = new Proxy(rme);
        assertNull(proxy.getOfferedIdentifier());
        proxy.setOfferedIdentifier(offer);
        assertSame(id, proxy.getOfferedIdentifier());
    }
    
    @Test
    public void testAcknowledgeNotSupported() throws RMException {
        DestinationSequence ds = control.createMock(DestinationSequence.class);
        EndpointReferenceType acksToEPR = control.createMock(EndpointReferenceType.class);
        EasyMock.expect(ds.getAcksTo()).andReturn(acksToEPR).anyTimes();
        AttributedURIType acksToURI = control.createMock(AttributedURIType.class);
        EasyMock.expect(acksToEPR.getAddress()).andReturn(acksToURI).anyTimes();
        String acksToAddress = Names.WSA_ANONYMOUS_ADDRESS;
        EasyMock.expect(acksToURI.getValue()).andReturn(acksToAddress).anyTimes();
        control.replay();
        Proxy proxy = new Proxy(rme);
        proxy.acknowledge(ds);        
    }
    
    @Test
    public void testAcknowledge() throws NoSuchMethodException, RMException {
        Method m = Proxy.class.getDeclaredMethod("invoke", 
            new Class[] {OperationInfo.class, Object[].class, Map.class});
        Proxy proxy = EasyMock.createMockBuilder(Proxy.class)
            .addMockedMethod(m).createMock(control);
        proxy.setReliableEndpoint(rme);
        EasyMock.expect(rme.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        DestinationSequence ds = control.createMock(DestinationSequence.class);
        EndpointReferenceType acksToEPR = control.createMock(EndpointReferenceType.class);
        EasyMock.expect(ds.getAcksTo()).andReturn(acksToEPR).anyTimes();
        AttributedURIType acksToURI = control.createMock(AttributedURIType.class);
        EasyMock.expect(acksToEPR.getAddress()).andReturn(acksToURI).anyTimes();
        String acksToAddress = "acksTo";
        EasyMock.expect(acksToURI.getValue()).andReturn(acksToAddress).anyTimes();
        Endpoint endpoint = control.createMock(Endpoint.class);
        EasyMock.expect(rme.getEndpoint()).andReturn(endpoint).anyTimes();
        EndpointInfo epi = control.createMock(EndpointInfo.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(epi).anyTimes();
        ServiceInfo si = control.createMock(ServiceInfo.class);
        EasyMock.expect(epi.getService()).andReturn(si).anyTimes();
        InterfaceInfo ii = control.createMock(InterfaceInfo.class);
        EasyMock.expect(si.getInterface()).andReturn(ii).anyTimes();
        OperationInfo oi = control.createMock(OperationInfo.class);
        EasyMock.expect(ii.getOperation(RM10Constants.SEQUENCE_ACK_QNAME)).andReturn(oi).anyTimes();
        expectInvoke(proxy, oi, null);
        control.replay();
        
        proxy.acknowledge(ds);      
    }
    
    @Test
    public void testLastMessage() throws NoSuchMethodException, RMException {
        Method m = Proxy.class.getDeclaredMethod("invoke", 
            new Class[] {OperationInfo.class, Object[].class, Map.class});
        Proxy proxy = EasyMock.createMockBuilder(Proxy.class)
            .addMockedMethod(m).createMock(control);
        proxy.setReliableEndpoint(rme);
        SourceSequence ss = control.createMock(SourceSequence.class);
        EasyMock.expect(ss.getTarget()).andReturn(null).anyTimes();
        control.replay();
        proxy.lastMessage(ss);
        control.verify();
        
        control.reset();
        org.apache.cxf.ws.addressing.EndpointReferenceType target
            = RMUtils.createAnonymousReference();
        EasyMock.expect(ss.getTarget()).andReturn(target).anyTimes();
        control.replay();
        proxy.lastMessage(ss);
        control.verify();
        
        control.reset();
        target = RMUtils.createReference("http://localhost:9000/greeterPort");
        EasyMock.expect(ss.getTarget()).andReturn(target).anyTimes();
        Endpoint endpoint = control.createMock(Endpoint.class);
        EasyMock.expect(rme.getEndpoint()).andReturn(endpoint).anyTimes();
        EndpointInfo epi = control.createMock(EndpointInfo.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(epi).anyTimes();
        ServiceInfo si = control.createMock(ServiceInfo.class);
        EasyMock.expect(epi.getService()).andReturn(si).anyTimes();
        InterfaceInfo ii = control.createMock(InterfaceInfo.class);
        EasyMock.expect(si.getInterface()).andReturn(ii).anyTimes();
        OperationInfo oi = control.createMock(OperationInfo.class);
        EasyMock.expect(ii.getOperation(RM10Constants.CLOSE_SEQUENCE_QNAME)).andReturn(oi).anyTimes();
        expectInvokeWithContext(proxy, oi, null);
        EasyMock.expect(rme.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        control.replay();
        
        proxy.lastMessage(ss);
        
    }
    
    @Test    
    public void testTerminate() throws NoSuchMethodException, RMException {
        Method m = Proxy.class.getDeclaredMethod("invoke", 
            new Class[] {OperationInfo.class, Object[].class, Map.class});
        Proxy proxy = EasyMock.createMockBuilder(Proxy.class)
            .addMockedMethod(m).createMock(control);
        proxy.setReliableEndpoint(rme);        
        EasyMock.expect(rme.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        Endpoint endpoint = control.createMock(Endpoint.class);
        EasyMock.expect(rme.getEndpoint()).andReturn(endpoint).anyTimes();
        EndpointInfo epi = control.createMock(EndpointInfo.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(epi).anyTimes();
        ServiceInfo si = control.createMock(ServiceInfo.class);
        EasyMock.expect(epi.getService()).andReturn(si).anyTimes();
        InterfaceInfo ii = control.createMock(InterfaceInfo.class);
        EasyMock.expect(si.getInterface()).andReturn(ii).anyTimes();
        OperationInfo oi = control.createMock(OperationInfo.class);
        EasyMock.expect(ii.getOperation(RM10Constants.TERMINATE_SEQUENCE_QNAME)).andReturn(oi).anyTimes();
        SourceSequence ss = control.createMock(SourceSequence.class);
        Identifier id = control.createMock(Identifier.class);
        EasyMock.expect(ss.getIdentifier()).andReturn(id).anyTimes();
        expectInvoke(proxy, oi, null);
        control.replay();
        proxy.terminate(ss);
    }
    
    @Test
    public void testCreateSequenceResponse() throws NoSuchMethodException, RMException {
        Method m = Proxy.class.getDeclaredMethod("invoke", 
            new Class[] {OperationInfo.class, Object[].class, Map.class});
        Proxy proxy = EasyMock.createMockBuilder(Proxy.class)
            .addMockedMethod(m).createMock(control);
        proxy.setReliableEndpoint(rme);
        EasyMock.expect(rme.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        Endpoint endpoint = control.createMock(Endpoint.class);
        EasyMock.expect(rme.getEndpoint()).andReturn(endpoint).anyTimes();
        EndpointInfo epi = control.createMock(EndpointInfo.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(epi).anyTimes();
        ServiceInfo si = control.createMock(ServiceInfo.class);
        EasyMock.expect(epi.getService()).andReturn(si).anyTimes();
        InterfaceInfo ii = control.createMock(InterfaceInfo.class);
        EasyMock.expect(si.getInterface()).andReturn(ii).anyTimes();
        OperationInfo oi = control.createMock(OperationInfo.class);
        EasyMock.expect(ii.getOperation(RM10Constants.CREATE_SEQUENCE_RESPONSE_ONEWAY_QNAME))
            .andReturn(oi).anyTimes();
        org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType csr =
            control.createMock(org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType.class);
        expectInvoke(proxy, oi, null);
        control.replay();
        proxy.createSequenceResponse(csr);
    }
    
    @Test
    public void testCreateSequenceOnClient() throws NoSuchMethodException, RMException {
        testCreateSequence(false); 
    }
    
    @Test
    public void testCreateSequenceOnServer() throws NoSuchMethodException, RMException {
        testCreateSequence(true); 
    }
    
    @Test
    public void testInvoke() throws Exception {        
        Method m = Proxy.class.getDeclaredMethod("createClient", 
            new Class[] {Bus.class, Endpoint.class, Conduit.class, 
                         org.apache.cxf.ws.addressing.EndpointReferenceType.class});
        Proxy proxy = EasyMock.createMockBuilder(Proxy.class)
            .addMockedMethod(m).createMock(control);
        proxy.setReliableEndpoint(rme);
        EasyMock.expect(rme.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408).anyTimes();

        RMManager manager = control.createMock(RMManager.class);
        EasyMock.expect(rme.getManager()).andReturn(manager).anyTimes();
        Bus bus = control.createMock(Bus.class);
        EasyMock.expect(manager.getBus()).andReturn(bus).anyTimes();
        Endpoint endpoint = control.createMock(Endpoint.class);
        EasyMock.expect(rme.getEndpoint()).andReturn(endpoint).anyTimes();
        BindingInfo bi = control.createMock(BindingInfo.class);
        EasyMock.expect(rme.getBindingInfo()).andReturn(bi).anyTimes();

        Conduit conduit = control.createMock(Conduit.class);
        EasyMock.expect(rme.getConduit()).andReturn(conduit).anyTimes();
        org.apache.cxf.ws.addressing.EndpointReferenceType replyTo 
            = control.createMock(org.apache.cxf.ws.addressing.EndpointReferenceType.class);
        EasyMock.expect(rme.getReplyTo()).andReturn(replyTo).anyTimes();
        
        OperationInfo oi = control.createMock(OperationInfo.class);
        BindingOperationInfo boi = control.createMock(BindingOperationInfo.class);
        EasyMock.expect(bi.getOperation(oi)).andReturn(boi).anyTimes();
        Client client = control.createMock(Client.class);
        EasyMock.expect(proxy.createClient(bus, endpoint, conduit, replyTo)).andReturn(client).anyTimes();  
        Object[] args = new Object[] {};
        Map<String, Object> context = new HashMap<String, Object>();
        Object[] results = new Object[] {"a", "b", "c"};
        EasyMock.expect(client.invoke(boi, args, context)).andReturn(results).anyTimes();        
        
        control.replay();
        assertEquals("a", proxy.invoke(oi, args, context));
    }
    
    @Test 
    public void testRMClientConstruction() {
        Proxy proxy = new Proxy(rme);
        Bus bus = control.createMock(Bus.class);
        Endpoint endpoint = control.createMock(Endpoint.class);
        Conduit conduit = control.createMock(Conduit.class);
        org.apache.cxf.ws.addressing.EndpointReferenceType address = 
            control.createMock(org.apache.cxf.ws.addressing.EndpointReferenceType.class);
        EasyMock.expect(rme.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        control.replay();
        assertNotNull(proxy.createClient(bus, endpoint, conduit, address));
    }
    
    @Test 
    public void testRMClientGetConduit() {
        Proxy proxy = new Proxy(rme);
        Bus bus = control.createMock(Bus.class);
        Endpoint endpoint = control.createMock(Endpoint.class);
        Conduit conduit = control.createMock(Conduit.class);
        ConduitSelector cs = control.createMock(ConduitSelector.class);
        EasyMock.expect(cs.selectConduit(EasyMock.isA(Message.class))).andReturn(conduit).anyTimes();
        control.replay();
        Proxy.RMClient client = proxy.new RMClient(bus, endpoint, cs);
        assertSame(conduit, client.getConduit());    
    }
    
    
    
    private void testCreateSequence(boolean isServer) throws NoSuchMethodException, RMException {
        Method m = Proxy.class.getDeclaredMethod("invoke", 
            new Class[] {OperationInfo.class, Object[].class, Map.class});
        Proxy proxy = EasyMock.createMockBuilder(Proxy.class)
            .addMockedMethod(m).createMock(control);
        proxy.setReliableEndpoint(rme);
        EasyMock.expect(rme.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        
        RMManager manager = control.createMock(RMManager.class);
        EasyMock.expect(rme.getManager()).andReturn(manager).anyTimes();
        SourcePolicyType sp = control.createMock(SourcePolicyType.class);
        EasyMock.expect(manager.getSourcePolicy()).andReturn(sp).anyTimes();
        EasyMock.expect(sp.getAcksTo()).andReturn(null).anyTimes();
        Duration d = DatatypeFactory.createDuration("PT12H");
        EasyMock.expect(sp.getSequenceExpiration()).andReturn(d).anyTimes();
        EasyMock.expect(sp.isIncludeOffer()).andReturn(Boolean.TRUE).anyTimes();
        Duration dOffered = DatatypeFactory.createDuration("PT24H");
        EasyMock.expect(sp.getOfferedSequenceExpiration()).andReturn(dOffered).anyTimes();
        Source source = control.createMock(Source.class);
        EasyMock.expect(rme.getSource()).andReturn(source).anyTimes();
        Identifier offeredId = control.createMock(Identifier.class);
        EasyMock.expect(source.generateSequenceIdentifier()).andReturn(offeredId).anyTimes();
             
        Endpoint endpoint = control.createMock(Endpoint.class);
        EasyMock.expect(rme.getEndpoint()).andReturn(endpoint).anyTimes();
        EndpointInfo epi = control.createMock(EndpointInfo.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(epi).anyTimes();
        ServiceInfo si = control.createMock(ServiceInfo.class);
        EasyMock.expect(epi.getService()).andReturn(si).anyTimes();
        InterfaceInfo ii = control.createMock(InterfaceInfo.class);
        EasyMock.expect(si.getInterface()).andReturn(ii).anyTimes();
        OperationInfo oi = control.createMock(OperationInfo.class);
        if (isServer) {
            EasyMock.expect(ii.getOperation(RM10Constants.CREATE_SEQUENCE_ONEWAY_QNAME))
                .andReturn(oi).anyTimes();
            Endpoint ae = control.createMock(Endpoint.class);
            EasyMock.expect(rme.getApplicationEndpoint()).andReturn(ae).anyTimes();
            EasyMock.expect(ae.getExecutor()).andReturn(SynchronousExecutor.getInstance()).anyTimes();
            expectInvoke(proxy, oi, null);
        } else {
            EasyMock.expect(ii.getOperation(RM10Constants.CREATE_SEQUENCE_QNAME)).andReturn(oi).anyTimes();
            org.apache.cxf.ws.rm.v200502.CreateSequenceResponseType csr =
                new org.apache.cxf.ws.rm.v200502.CreateSequenceResponseType();
            expectInvoke(proxy, oi, csr);
        }
        
        EndpointReferenceType defaultAcksTo = control.createMock(EndpointReferenceType.class);
        AttributedURIType aut = control.createMock(AttributedURIType.class);
        EasyMock.expect(aut.getValue()).andReturn("here").anyTimes();
        EasyMock.expect(defaultAcksTo.getAddress()).andReturn(aut).anyTimes();
        RelatesToType relatesTo = control.createMock(RelatesToType.class);
        control.replay();
        if (isServer) {
            assertNull(proxy.createSequence(defaultAcksTo, relatesTo, isServer));
        } else {
            assertNotNull(proxy.createSequence(defaultAcksTo, relatesTo, isServer));
        }
    }
    
    @SuppressWarnings("unchecked")
    private void expectInvoke(Proxy proxy, OperationInfo oi, Object expectedReturn) throws RMException {
        EasyMock.expect(proxy.invoke(EasyMock.same(oi), EasyMock.isA(Object[].class), 
            (Map<String, Object>)EasyMock.isNull())).andReturn(expectedReturn).anyTimes();
    }
    
    @SuppressWarnings("unchecked")
    private void expectInvokeWithContext(Proxy proxy, OperationInfo oi, Object expectedReturn) 
        throws RMException {
        EasyMock.expect(proxy.invoke(EasyMock.same(oi), EasyMock.isA(Object[].class), 
            EasyMock.isA(Map.class))).andReturn(expectedReturn).anyTimes();
    }
}
