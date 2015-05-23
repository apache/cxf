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
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.VersionTransformer.Names200408;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RMInInterceptorTest extends Assert {
    
    private IMocksControl control;
    private RMInInterceptor interceptor;
    private RMManager manager;
    private RMEndpoint rme;
    private RMProperties rmps;
    
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        rmps = control.createMock(RMProperties.class);
    }

    @After
    public void tearDown() {
        control.verify();
    }
    
    @Test
    public void testOrdering() {
        control.replay();
        Phase p = new Phase(Phase.PRE_LOGICAL, 1);
        SortedSet<Phase> phases = new TreeSet<Phase>();
        phases.add(p);
        PhaseInterceptorChain chain = 
            new PhaseInterceptorChain(phases);
        MAPAggregator map = new MAPAggregator();
        RMInInterceptor rmi = new RMInInterceptor();        
        chain.add(rmi);
        chain.add(map);
        Iterator<Interceptor<? extends Message>> it = chain.iterator();
        assertSame("Unexpected order.", rmi, it.next());
        assertSame("Unexpected order.", map, it.next());
        
    } 
    
    
    @Test
    public void testHandleCreateSequenceOnServer() throws SequenceFault, RMException {
        interceptor = new RMInInterceptor();         
        Message message = setupInboundMessage(RM10Constants.CREATE_SEQUENCE_ACTION, true);  
        rme.receivedControlMessage();
        EasyMock.expectLastCall();
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(null);
        
        control.replay();
        interceptor.handle(message);
    }
    
    @Test
    public void testHandleCreateSequenceOnClient() throws SequenceFault, RMException {
        interceptor = new RMInInterceptor();         
        Message message = setupInboundMessage(RM10Constants.CREATE_SEQUENCE_ACTION, false); 
        rme.receivedControlMessage();
        EasyMock.expectLastCall();
        Servant servant = control.createMock(Servant.class);
        EasyMock.expect(rme.getServant()).andReturn(servant);
        CreateSequenceResponseType csr = control.createMock(CreateSequenceResponseType.class);
        EasyMock.expect(servant.createSequence(message)).andReturn(csr);
        Proxy proxy = control.createMock(Proxy.class);
        EasyMock.expect(rme.getProxy()).andReturn(proxy);
        proxy.createSequenceResponse(csr, ProtocolVariation.RM10WSA200408);
        EasyMock.expectLastCall();
        
        control.replay();
        interceptor.handle(message);
    }
    
    @Test
    public void testHandleSequenceAckOnClient() throws SequenceFault, RMException, NoSuchMethodException {
        testHandleSequenceAck(false);
    }
    
    @Test
    public void testHandleSequenceAckOnServer() throws SequenceFault, RMException, NoSuchMethodException {
        testHandleSequenceAck(true);
    }
    
    private void testHandleSequenceAck(boolean onServer) throws SequenceFault, RMException, 
    NoSuchMethodException {
        Method m = RMInInterceptor.class.getDeclaredMethod("processAcknowledgments",
            new Class[] {RMEndpoint.class, RMProperties.class, ProtocolVariation.class});
        interceptor =
            EasyMock.createMockBuilder(RMInInterceptor.class)
                .addMockedMethod(m).createMock(control);
        Message message = setupInboundMessage(RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION, onServer);
        rme.receivedControlMessage();
        EasyMock.expectLastCall();
        interceptor.processAcknowledgments(rme, rmps, ProtocolVariation.RM10WSA200408);
        EasyMock.expectLastCall();
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(null);

        control.replay();
        interceptor.handle(message);
    }
    
    @Test
    public void testHandleTerminateSequenceOnServer() throws SequenceFault, RMException {
        testHandleTerminateSequence(true);        
    }
    
    @Test
    public void testHandleTerminateSequenceOnClient() throws SequenceFault, RMException {
        testHandleTerminateSequence(false);        
    }
    
    private void testHandleTerminateSequence(boolean onServer) throws SequenceFault, RMException {
        interceptor = new RMInInterceptor();
        Message message = setupInboundMessage(RM10Constants.TERMINATE_SEQUENCE_ACTION, onServer);
        rme.receivedControlMessage();
        EasyMock.expectLastCall();
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(null);

        control.replay();
        interceptor.handle(message);
    }
    
    @Test
    public void testAppRequest() throws SequenceFault, RMException, NoSuchMethodException {
        testAppMessage(true);
    }
    
    @Test
    public void testAppResponse() throws SequenceFault, RMException, NoSuchMethodException {
        testAppMessage(false);
    }

    @Test
    public void testDefferedAbort() throws SequenceFault, RMException, NoSuchMethodException {
        testAppMessage(false, true);
    }
    
    private void testAppMessage(boolean onServer) 
        throws SequenceFault, RMException, NoSuchMethodException {
        testAppMessage(onServer, false);
    }

    private void testAppMessage(boolean onServer, boolean deferredAbort) 
        throws SequenceFault, RMException, NoSuchMethodException {
        Method m1 = RMInInterceptor.class.getDeclaredMethod("processAcknowledgments",
            new Class[] {RMEndpoint.class, RMProperties.class, ProtocolVariation.class});
        Method m2 = RMInInterceptor.class.getDeclaredMethod("processAcknowledgmentRequests",
            new Class[] {Destination.class, Message.class});
        Method m3 = RMInInterceptor.class.getDeclaredMethod("processSequence",
            new Class[] {Destination.class, Message.class});
        Method m4 = RMInInterceptor.class.getDeclaredMethod("processDeliveryAssurance",
            new Class[] {RMProperties.class});
        interceptor =
            EasyMock.createMockBuilder(RMInInterceptor.class)
                .addMockedMethods(m1, m2, m3, m4).createMock(control);
        Message message = setupInboundMessage("greetMe", true);
        Destination d = control.createMock(Destination.class);
        EasyMock.expect(manager.getDestination(message)).andReturn(d);
        interceptor.processAcknowledgments(rme, rmps, ProtocolVariation.RM10WSA200408);
        EasyMock.expectLastCall();
        interceptor.processAcknowledgmentRequests(d, message);
        EasyMock.expectLastCall();
        interceptor.processSequence(d, message);
        EasyMock.expectLastCall();
        interceptor.processDeliveryAssurance(rmps);
        EasyMock.expectLastCall();
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(null);
               
        Exchange ex = control.createMock(Exchange.class);
        message.getExchange();
        EasyMock.expectLastCall().andReturn(ex).anyTimes();
        ex.get("deferred.uncorrelated.message.abort");
        EasyMock.expectLastCall().andReturn(Boolean.TRUE);
        InterceptorChain chain = control.createMock(InterceptorChain.class);
        message.getInterceptorChain();
        EasyMock.expectLastCall().andReturn(chain);
        chain.abort();
        EasyMock.expectLastCall();

        control.replay();
        interceptor.handle(message);
    }  
    
    @Test
    public void testProcessAcknowledgments() throws RMException {
        interceptor = new RMInInterceptor();
        manager = control.createMock(RMManager.class);
        Source source = control.createMock(Source.class);
        rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(rme.getSource()).andReturn(source).anyTimes();
        interceptor.setManager(manager);
        SequenceAcknowledgement ack1 = control.createMock(SequenceAcknowledgement.class);
        SequenceAcknowledgement ack2 = control.createMock(SequenceAcknowledgement.class);
        Collection<SequenceAcknowledgement> acks = new ArrayList<SequenceAcknowledgement>();
        acks.add(ack1);
        acks.add(ack2);
        EasyMock.expect(rmps.getAcks()).andReturn(acks);
        Identifier id1 = control.createMock(Identifier.class);
        EasyMock.expect(ack1.getIdentifier()).andReturn(id1);
        SourceSequence ss1 = control.createMock(SourceSequence.class);
        EasyMock.expect(source.getSequence(id1)).andReturn(ss1);
        ss1.setAcknowledged(ack1);
        EasyMock.expectLastCall();
        Identifier id2 = control.createMock(Identifier.class);
        EasyMock.expect(ack2.getIdentifier()).andReturn(id2);
        EasyMock.expect(source.getSequence(id2)).andReturn(null);
        control.replay();
        try {
            interceptor.processAcknowledgments(rme, rmps, ProtocolVariation.RM10WSA200408);
            fail("Expected SequenceFault not thrown");
        } catch (SequenceFault sf) {
            assertEquals(RM10Constants.UNKNOWN_SEQUENCE_FAULT_QNAME, sf.getFaultCode());
        }
    }
    
    @Test
    public void testProcessAcknowledgmentRequests() {
        control.replay();
        // TODI
    }
    
    @Test
    public void testProcessSequence() throws SequenceFault, RMException {
        Destination destination = control.createMock(Destination.class);
        Message message = control.createMock(Message.class);
        destination.acknowledge(message);
        EasyMock.expectLastCall();        
        control.replay();
        interceptor = new RMInInterceptor();
        interceptor.processSequence(destination, message);
    }
    
    @Test
    public void testProcessDeliveryAssurance() {
        control.replay(); 
        // TODO
    }

    @Test
    public void testProcessInvalidMessage() throws SequenceFault, RMException {
        interceptor = new RMInInterceptor();
        
        Message message = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);
        org.apache.cxf.transport.Destination destination = 
            control.createMock(org.apache.cxf.transport.Destination.class);
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(exchange.getDestination()).andReturn(destination).anyTimes();
        EasyMock.expect(exchange.getOutMessage()).andReturn(null).anyTimes();
        EasyMock.expect(exchange.getOutFaultMessage()).andReturn(null).anyTimes();
        control.replay();

        try {
            interceptor.handle(message);
            fail("must reject the invalid rm message");
        } catch (Exception e) {
            assertTrue(e instanceof RMException);
            // verify a partial error text match to exclude an unexpected exception
            // (see WSA_REQUIRED_EXC in Messages.properties)
            final String text = "WS-Addressing is required";
            assertTrue(e.getMessage() != null 
                && e.getMessage().indexOf(text) >= 0);
        }
        
        control.reset();
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        AddressingProperties maps = control.createMock(AddressingProperties.class);
        EasyMock.expect(maps.getNamespaceURI()).andReturn(Names200408.WSA_NAMESPACE_NAME).anyTimes();
        EasyMock.expect(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND)).andReturn(maps);
        AttributedURIType actionURI = control.createMock(AttributedURIType.class);
        EasyMock.expect(maps.getAction()).andReturn(actionURI).times(2);
        EasyMock.expect(actionURI.getValue()).andReturn("foo");
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).andReturn(rmps);
        EasyMock.expect(exchange.getDestination()).andReturn(destination).anyTimes();
        EasyMock.expect(exchange.getOutMessage()).andReturn(null).anyTimes();
        EasyMock.expect(exchange.getOutFaultMessage()).andReturn(null).anyTimes();

        control.replay();
        
        try {
            interceptor.handle(message);
            fail("must reject the invalid rm message");
        } catch (Exception e) {
            assertTrue(e instanceof RMException);
            // verify a partial error text match to exclude an unexpected exception
            // (see WSRM_REQUIRED_EXC in Messages.properties)
            final String text = "WS-ReliableMessaging is required";
            assertTrue(e.getMessage() != null 
                && e.getMessage().indexOf(text) >= 0);
        }
    }
    
    @Test
    public void testProcessInvalidMessageOnFault() throws SequenceFault, RMException {
        interceptor = new RMInInterceptor();
        manager = control.createMock(RMManager.class);
        interceptor.setManager(manager);
        
        Message message = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        control.replay();
        
        try {
            interceptor.handleFault(message);
        } catch (Exception e) {
            fail("unexpected exception thrown from handleFault: " + e);
        }
        
        control.reset();
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(message.get(RMMessageConstants.DELIVERING_ROBUST_ONEWAY)).andReturn(true).anyTimes();
        control.replay();
        
        try {
            interceptor.handleFault(message);
        } catch (Exception e) {
            fail("unexpected exception thrown from handleFault: " + e);
        }
    }

    @Test
    public void testProcessValidMessageOnFault() throws SequenceFault, RMException {
        interceptor = new RMInInterceptor();
        manager = control.createMock(RMManager.class);
        Message message = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);
        AddressingProperties maps = control.createMock(AddressingProperties.class);
        
        interceptor.setManager(manager);
        
        // test 1. a normal sequence fault case without non-anonymous faultTo
        EasyMock.expect(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND)).andReturn(maps);
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(message.get(RMMessageConstants.RM_PROTOCOL_VARIATION))
            .andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        EasyMock.expect(message.getContent(Exception.class)).andReturn(new SequenceFault("test")).anyTimes();
        exchange.setOneWay(false);
        EasyMock.expectLastCall();
        control.replay();
        
        try {
            interceptor.handleFault(message);
        } catch (Exception e) {
            fail("unexpected exception thrown from handleFault: " + e);
        }

        control.verify();
        
        // 2. a sequence fault case with non anonymous faultTo
        control.reset();
        Destination d = control.createMock(Destination.class);
        Endpoint ep = control.createMock(Endpoint.class);
        EndpointInfo epi = control.createMock(EndpointInfo.class);
        EasyMock.expect(ep.getEndpointInfo()).andReturn(epi).anyTimes();
        EasyMock.expect(exchange.getEndpoint()).andReturn(ep).anyTimes();
        EasyMock.expect(maps.getFaultTo())
            .andReturn(RMUtils.createReference("http://localhost:9999/decoupled")).anyTimes();
        EasyMock.expect(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND)).andReturn(maps);
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(message.get(RMMessageConstants.RM_PROTOCOL_VARIATION))
            .andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        EasyMock.expect(message.getContent(Exception.class)).andReturn(new SequenceFault("test")).anyTimes();
        exchange.setOneWay(false);
        EasyMock.expectLastCall();
        exchange.setDestination(EasyMock.anyObject(org.apache.cxf.transport.Destination.class));
        EasyMock.expectLastCall();
        control.replay();
        
        try {
            interceptor.handleFault(message);
        } catch (Exception e) {
            fail("unexpected exception thrown from handleFault: " + e);
        }
        control.verify();
        
        // 3. a robust oneway case
        control.reset();
        EasyMock.expect(maps.getFaultTo())
            .andReturn(RMUtils.createAnonymousReference()).anyTimes();
        EasyMock.expect(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND)).andReturn(maps).anyTimes();
        EasyMock.expect(manager.getDestination(message)).andReturn(d);
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(message.get(RMMessageConstants.DELIVERING_ROBUST_ONEWAY)).andReturn(true).anyTimes();
        EasyMock.expect(message.get(RMMessageConstants.RM_PROTOCOL_VARIATION))
            .andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        control.replay();
        
        try {
            interceptor.handleFault(message);
        } catch (Exception e) {
            fail("unexpected exception thrown from handleFault: " + e);
        }

        // 4. a runtime exception case
        control.reset();
        EasyMock.expect(maps.getFaultTo())
            .andReturn(RMUtils.createAnonymousReference()).anyTimes();
        EasyMock.expect(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND)).andReturn(maps).anyTimes();
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(message.get(RMMessageConstants.RM_PROTOCOL_VARIATION))
            .andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        EasyMock.expect(message.getContent(Exception.class)).andReturn(new RuntimeException("test")).anyTimes();
        control.replay();
        
        try {
            interceptor.handleFault(message);
        } catch (Exception e) {
            fail("unexpected exception thrown from handleFault: " + e);
        }
//      verified in tearDown
    }

    private Message setupInboundMessage(String action, boolean serverSide) throws RMException {
        Message message = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(exchange).times(2);
        EasyMock.expect(exchange.getOutMessage()).andReturn(null);
        EasyMock.expect(exchange.getOutFaultMessage()).andReturn(null);        
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).andReturn(rmps);
        
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(!serverSide);
        AddressingProperties maps = control.createMock(AddressingProperties.class);
        EasyMock.expect(maps.getNamespaceURI()).andReturn(Names200408.WSA_NAMESPACE_NAME).anyTimes();
        EasyMock.expect(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND)).andReturn(maps);
        
        AttributedURIType actionURI = control.createMock(AttributedURIType.class);
        EasyMock.expect(maps.getAction()).andReturn(actionURI).times(2);
        EasyMock.expect(actionURI.getValue()).andReturn(action);
        
        EasyMock.expect(message.get(RMMessageConstants.ORIGINAL_REQUESTOR_ROLE)).andReturn(Boolean.FALSE);
        EasyMock.expect(message.put(Message.REQUESTOR_ROLE, Boolean.FALSE)).andReturn(null);
        
        org.apache.cxf.transport.Destination td = 
            serverSide ? control.createMock(org.apache.cxf.transport.Destination.class) : null;
        EasyMock.expect(exchange.getDestination()).andReturn(td);
        
        manager = control.createMock(RMManager.class);
        RMConfiguration config = new RMConfiguration();
        config.setRMNamespace(RM10Constants.NAMESPACE_URI);
        config.setRM10AddressingNamespace(RM10Constants.NAMESPACE_URI);
        EasyMock.expect(manager.getEffectiveConfiguration(message)).andReturn(config).anyTimes();
        interceptor.setManager(manager);
        rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(manager.getReliableEndpoint(message)).andReturn(rme);
        
        EasyMock.expect(rmps.getNamespaceURI()).andReturn(RM10Constants.NAMESPACE_URI).anyTimes();
        
        return message;
    }
}