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

import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
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
        Iterator it = chain.iterator();
        assertSame("Unexpected order.", rmi, it.next());
        assertSame("Unexpected order.", map, it.next());
        
    } 
    
    
    @Test
    public void testHandleCreateSequenceOnServer() throws SequenceFault, RMException {
        interceptor = new RMInInterceptor();         
        Message message = setupInboundMessage(RMConstants.getCreateSequenceAction(), true);  
        rme.receivedControlMessage();
        EasyMock.expectLastCall();
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(null);
        
        control.replay();
        interceptor.handle(message);
    }
    
    @Test
    public void testHandleCreateSequenceOnClient() throws SequenceFault, RMException {
        interceptor = new RMInInterceptor();         
        Message message = setupInboundMessage(RMConstants.getCreateSequenceAction(), false); 
        rme.receivedControlMessage();
        EasyMock.expectLastCall();
        Servant servant = control.createMock(Servant.class);
        EasyMock.expect(rme.getServant()).andReturn(servant);
        CreateSequenceResponseType csr = control.createMock(CreateSequenceResponseType.class);
        EasyMock.expect(servant.createSequence(message)).andReturn(csr);
        Proxy proxy = control.createMock(Proxy.class);
        EasyMock.expect(rme.getProxy()).andReturn(proxy);
        proxy.createSequenceResponse(csr);
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
            new Class[] {Source.class, RMProperties.class});
        interceptor = control.createMock(RMInInterceptor.class, new Method[] {m});
        Message message = setupInboundMessage(RMConstants.getSequenceAckAction(), onServer);
        rme.receivedControlMessage();
        EasyMock.expectLastCall();
        Source s = control.createMock(Source.class);
        EasyMock.expect(rme.getSource()).andReturn(s);
        interceptor.processAcknowledgments(s, rmps);
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
        Message message = setupInboundMessage(RMConstants.getTerminateSequenceAction(), onServer);
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
                                                            new Class[] {Source.class, RMProperties.class});
        Method m2 = RMInInterceptor.class.getDeclaredMethod("processAcknowledgmentRequests",
                                                            new Class[] {Destination.class, Message.class});
        Method m3 = RMInInterceptor.class.getDeclaredMethod("processSequence",
                                                            new Class[] {Destination.class, Message.class});
        Method m4 = RMInInterceptor.class.getDeclaredMethod("processDeliveryAssurance",
                                                            new Class[] {RMProperties.class});
        interceptor = control
            .createMock(RMInInterceptor.class, new Method[] {m1, m2, m3, m4});
        Message message = setupInboundMessage("greetMe", true);
        Destination d = control.createMock(Destination.class);
        EasyMock.expect(manager.getDestination(message)).andReturn(d);
        Source s = control.createMock(Source.class);
        EasyMock.expect(rme.getSource()).andReturn(s);
        interceptor.processAcknowledgments(s, rmps);
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
            interceptor.processAcknowledgments(source, rmps);
            fail("Expected SequenceFault not thrown");
        } catch (SequenceFault sf) {
            assertEquals(RMConstants.getUnknownSequenceFaultCode(), sf.getSequenceFault().getFaultCode());
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
    
    
    

    private Message setupInboundMessage(String action, boolean serverSide) {
        Message message = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(exchange).times(2);
        EasyMock.expect(exchange.getOutMessage()).andReturn(null);
        EasyMock.expect(exchange.getOutFaultMessage()).andReturn(null);        
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).andReturn(rmps);
        
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(!serverSide);
        AddressingPropertiesImpl maps = control.createMock(AddressingPropertiesImpl.class);
        EasyMock.expect(message.get(JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND)).andReturn(maps);
        
        AttributedURIType actionURI = control.createMock(AttributedURIType.class);
        EasyMock.expect(maps.getAction()).andReturn(actionURI).times(2);
        EasyMock.expect(actionURI.getValue()).andReturn(action);
        
        EasyMock.expect(message.get(RMMessageConstants.ORIGINAL_REQUESTOR_ROLE)).andReturn(Boolean.FALSE);
        EasyMock.expect(message.put(Message.REQUESTOR_ROLE, Boolean.FALSE)).andReturn(null);
        
        org.apache.cxf.transport.Destination td = 
            serverSide ? control.createMock(org.apache.cxf.transport.Destination.class) : null;
        EasyMock.expect(exchange.getDestination()).andReturn(td);
        
        manager = control.createMock(RMManager.class);
        interceptor.setManager(manager);
        rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(manager.getReliableEndpoint(message)).andReturn(rme);
        return message;
    }
    
}
