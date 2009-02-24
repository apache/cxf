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
import java.math.BigInteger;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.v200408.AttributedURI;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
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
        Iterator it = chain.iterator();
        assertSame("Unexpected order.", map, it.next());
        assertSame("Unexpected order.", rmi, it.next());                      
    } 
    
    @Test 
    public void testHandleRuntimeFault() throws NoSuchMethodException, SequenceFault, RMException {
        Method[] mocked = new Method[] {                                                
            RMOutInterceptor.class.getDeclaredMethod("isRuntimeFault", new Class[] {Message.class})
        };
        RMOutInterceptor interceptor = control.createMock(RMOutInterceptor.class, mocked);
        Message message = control.createMock(Message.class);        
        EasyMock.expect(interceptor.isRuntimeFault(message)).andReturn(true);
        control.replay();
        interceptor.handle(message);
        control.verify();
    }
    
    @Test 
    public void testHandleNoMAPs() throws NoSuchMethodException, SequenceFault, RMException {
        Method[] mocked = new Method[] {                                                
            RMOutInterceptor.class.getDeclaredMethod("isRuntimeFault", new Class[] {Message.class})
        };
        RMOutInterceptor interceptor = control.createMock(RMOutInterceptor.class, mocked);
        Message message = control.createMock(Message.class);        
        EasyMock.expect(interceptor.isRuntimeFault(message)).andReturn(false);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.FALSE).anyTimes();        
        EasyMock.expect(message.get(JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_OUTBOUND))
            .andReturn(null);
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
                             AttributedURI.class})            
        };
        RMOutInterceptor interceptor = control.createMock(RMOutInterceptor.class, mocked);         
        RMManager manager = control.createMock(RMManager.class);
        EasyMock.expect(interceptor.getManager()).andReturn(manager).times(5);
        
        Message message = control.createMock(Message.class);
        EasyMock.expect(interceptor.isRuntimeFault(message)).andReturn(false);
        Exchange ex = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(ex).anyTimes();
        EasyMock.expect(ex.getOutMessage()).andReturn(message).times(1);
        EasyMock.expect(ex.put("defer.uncorrelated.message.abort", Boolean.TRUE)).andReturn(null);       
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.TRUE).anyTimes();        
        EasyMock.expect(message.get(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_OUTBOUND))
            .andReturn(maps).anyTimes();
        RMProperties rmpsOut = new RMProperties();
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_OUTBOUND)).andReturn(rmpsOut);
        InterceptorChain chain = control.createMock(InterceptorChain.class);
        EasyMock.expect(message.getInterceptorChain()).andReturn(chain);
        chain.add(EasyMock.isA(RetransmissionInterceptor.class));
        EasyMock.expectLastCall();
        RetransmissionQueue queue = control.createMock(RetransmissionQueue.class);
        EasyMock.expect(manager.getRetransmissionQueue()).andReturn(queue);
        queue.start();
        EasyMock.expectLastCall();
                
        Source source = control.createMock(Source.class);
        EasyMock.expect(manager.getSource(message)).andReturn(source);
        Destination destination = control.createMock(Destination.class);
        EasyMock.expect(manager.getDestination(message)).andReturn(destination);
        SourceSequence sseq = control.createMock(SourceSequence.class);
        EasyMock.expect(manager.getSequence((Identifier)EasyMock.isNull(), EasyMock.same(message), 
                                        EasyMock.same(maps))).andReturn(sseq);
        EasyMock.expect(sseq.nextMessageNumber((Identifier)EasyMock.isNull(), 
            (BigInteger)EasyMock.isNull(), EasyMock.eq(false))).andReturn(BigInteger.TEN);
        EasyMock.expect(sseq.isLastMessage()).andReturn(false).times(2);
        interceptor.addAcknowledgements(EasyMock.same(destination), EasyMock.same(rmpsOut), 
            (Identifier)EasyMock.isNull(), EasyMock.isA(AttributedURI.class));
        EasyMock.expectLastCall();
        Identifier sid = control.createMock(Identifier.class);
        EasyMock.expect(sseq.getIdentifier()).andReturn(sid);
        EasyMock.expect(sseq.getCurrentMessageNr()).andReturn(BigInteger.TEN);

        
        control.replay();
        interceptor.handle(message);
        assertSame(sid, rmpsOut.getSequence().getIdentifier());        
        assertEquals(BigInteger.TEN, rmpsOut.getSequence().getMessageNumber());
        assertNull(rmpsOut.getSequence().getLastMessage());
        control.verify();
    }
    
    @Test
    public void testIsRuntimeFault() {
        Message message = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(exchange).times(2);
        EasyMock.expect(exchange.getOutFaultMessage()).andReturn(message);
        EasyMock.expect(message.get(FaultMode.class)).andReturn(FaultMode.RUNTIME_FAULT);
        control.replay();
        RMOutInterceptor rmi = new RMOutInterceptor();
        assertTrue(rmi.isRuntimeFault(message));
        control.verify();
        control.reset();
        EasyMock.expect(message.getExchange()).andReturn(exchange).times(2);
        EasyMock.expect(exchange.getOutFaultMessage()).andReturn(null);
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
