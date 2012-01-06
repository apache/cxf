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
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RMOutInterceptorTest extends Assert {
    
    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }
    
    @Test
    public void testOrdering() {
        Phase p = new Phase(Phase.PRE_LOGICAL, 1);
        SortedSet<Phase> phases = new TreeSet<Phase>();
        phases.add(p);
        PhaseInterceptorChain chain = 
            new PhaseInterceptorChain(phases);
        MAPAggregator map = new MAPAggregator();
        RMOutInterceptor rmi = new RMOutInterceptor();        
        chain.add(rmi);
        chain.add(map);
        Iterator<Interceptor<? extends Message>> it = chain.iterator();
        assertSame("Unexpected order.", map, it.next());
        assertSame("Unexpected order.", rmi, it.next());                      
    } 
    
    @Test 
    public void testHandleRuntimeFault() throws NoSuchMethodException, SequenceFault, RMException {
        Method[] mocked = new Method[] {                                                
            RMOutInterceptor.class.getDeclaredMethod("isRuntimeFault", new Class[] {Message.class})
        };
        RMOutInterceptor interceptor = 
            EasyMock.createMockBuilder(RMOutInterceptor.class)
                .addMockedMethods(mocked).createMock(control);
        Message message = control.createMock(Message.class);        
        EasyMock.expect(interceptor.isRuntimeFault(message)).andReturn(true).anyTimes();
        control.replay();
        interceptor.handle(message);
        control.verify();
    }
    
    @Test 
    public void testHandleNoMAPs() throws NoSuchMethodException, SequenceFault, RMException {
        Method[] mocked = new Method[] {                                                
            RMOutInterceptor.class.getDeclaredMethod("isRuntimeFault", new Class[] {Message.class})
        };
        RMOutInterceptor interceptor =
            EasyMock.createMockBuilder(RMOutInterceptor.class)
                .addMockedMethods(mocked).createMock(control);
        Message message = control.createMock(Message.class);        
        EasyMock.expect(interceptor.isRuntimeFault(message)).andReturn(false).anyTimes();
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.FALSE).anyTimes();        
        EasyMock.expect(message.get(JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_OUTBOUND))
            .andReturn(null).anyTimes();
        control.replay();
        interceptor.handle(message);
        control.verify();
    }
    
    @Test
    public void testHandleApplicationMessage() throws NoSuchMethodException, SequenceFault, RMException {
        AddressingPropertiesImpl maps = createMAPs("greetMe", "localhost:9000/GreeterPort", 
            org.apache.cxf.ws.addressing.Names.WSA_NONE_ADDRESS);
        Method[] mocked = new Method[] {
            AbstractRMInterceptor.class.getDeclaredMethod("getManager", new Class[]{}),
            RMOutInterceptor.class.getDeclaredMethod("isRuntimeFault", new Class[] {Message.class}),
            RMOutInterceptor.class.getDeclaredMethod("addAcknowledgements",
                new Class[] {Destination.class, RMProperties.class, Identifier.class, 
                             AttributedURIType.class})            
        };
        RMOutInterceptor interceptor =
            EasyMock.createMockBuilder(RMOutInterceptor.class)
                .addMockedMethods(mocked).createMock(control);
        RMManager manager = control.createMock(RMManager.class);
        EasyMock.expect(interceptor.getManager()).andReturn(manager).anyTimes();
        
        Message message = control.createMock(Message.class);
        EasyMock.expect(interceptor.isRuntimeFault(message)).andReturn(false).anyTimes();
        Exchange ex = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(ex).anyTimes();
        EasyMock.expect(ex.getOutMessage()).andReturn(message).anyTimes();
        EasyMock.expect(ex.put("defer.uncorrelated.message.abort", Boolean.TRUE)).andReturn(null).anyTimes();
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.TRUE).anyTimes();        
        EasyMock.expect(message.get(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_OUTBOUND))
            .andReturn(maps).anyTimes();
        RMProperties rmpsOut = new RMProperties();
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_OUTBOUND)).
            andReturn(rmpsOut).anyTimes();
        InterceptorChain chain = control.createMock(InterceptorChain.class);
        EasyMock.expect(message.getInterceptorChain()).andReturn(chain).anyTimes();
        chain.add(EasyMock.isA(RetransmissionInterceptor.class));
        EasyMock.expectLastCall();
        RetransmissionQueue queue = control.createMock(RetransmissionQueue.class);
        EasyMock.expect(manager.getRetransmissionQueue()).andReturn(queue).anyTimes();
        queue.start();
        EasyMock.expectLastCall();
                
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(rme.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        Source source = control.createMock(Source.class);
        EasyMock.expect(source.getReliableEndpoint()).andReturn(rme).anyTimes();
        EasyMock.expect(manager.getSource(message)).andReturn(source).anyTimes();
        Destination destination = control.createMock(Destination.class);
        EasyMock.expect(manager.getDestination(message)).andReturn(destination).anyTimes();
        SourceSequence sseq = control.createMock(SourceSequence.class);
        EasyMock.expect(manager.getSequence((Identifier)EasyMock.isNull(), EasyMock.same(message), 
                                        EasyMock.same(maps))).andReturn(sseq).anyTimes();
        EasyMock.expect(sseq.nextMessageNumber((Identifier)EasyMock.isNull(), 
            (Long)EasyMock.eq(0L), EasyMock.eq(false))).andReturn(new Long(10)).anyTimes();
        EasyMock.expect(sseq.isLastMessage()).andReturn(false).anyTimes();
        interceptor.addAcknowledgements(EasyMock.same(destination), EasyMock.same(rmpsOut), 
            (Identifier)EasyMock.isNull(), EasyMock.isA(AttributedURIType.class));
        EasyMock.expectLastCall();
        Identifier sid = control.createMock(Identifier.class);
        EasyMock.expect(sseq.getIdentifier()).andReturn(sid).anyTimes();
        EasyMock.expect(sseq.getCurrentMessageNr()).andReturn(new Long(10)).anyTimes();

        
        control.replay();
        interceptor.handle(message);
        assertSame(sid, rmpsOut.getSequence().getIdentifier());        
        assertEquals(new Long(10), rmpsOut.getSequence().getMessageNumber());
        control.verify();
    }
    
    @Test
    public void testIsRuntimeFault() {
        Message message = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(exchange.getOutFaultMessage()).andReturn(message).anyTimes();
        EasyMock.expect(message.get(FaultMode.class)).andReturn(FaultMode.RUNTIME_FAULT).anyTimes();
        control.replay();
        RMOutInterceptor rmi = new RMOutInterceptor();
        assertTrue(rmi.isRuntimeFault(message));
        control.verify();
        control.reset();
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(exchange.getOutFaultMessage()).andReturn(null).anyTimes();
        control.replay();
        assertTrue(!rmi.isRuntimeFault(message));
        control.verify();
    }
    
    private AddressingPropertiesImpl createMAPs(String action, String to, String replyTo) {
        AddressingPropertiesImpl maps = new AddressingPropertiesImpl();
        maps.setTo(RMUtils.createReference(to));
        EndpointReferenceType epr = RMUtils.createReference(replyTo);
        maps.setReplyTo(epr);
        return maps;
           
    }
}
