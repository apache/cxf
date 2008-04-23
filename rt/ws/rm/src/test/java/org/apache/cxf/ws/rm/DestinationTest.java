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

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.v200408.AttributedURI;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class DestinationTest extends Assert {

    private IMocksControl control;
    private RMEndpoint rme;
    private Destination destination;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        rme = control.createMock(RMEndpoint.class);
        destination = new Destination(rme);
    }

    @After
    public void tearDown() {
        control.verify();
    }
    
    @Test
    public void testGetSequence() {
        Identifier id = control.createMock(Identifier.class);
        String sid = "s1";
        EasyMock.expect(id.getValue()).andReturn(sid);
        control.replay();
        assertNull(destination.getSequence(id));
    }
    
    @Test
    public void testGetAllSequences() {
        control.replay();
        assertEquals(0, destination.getAllSequences().size());
    }
   
    @Test
    public void testAddRemoveSequence() {
        DestinationSequence ds = control.createMock(DestinationSequence.class);
        ds.setDestination(destination);
        EasyMock.expectLastCall();
        Identifier id = control.createMock(Identifier.class);
        EasyMock.expect(ds.getIdentifier()).andReturn(id).times(3);
        String sid = "s1";
        EasyMock.expect(id.getValue()).andReturn(sid).times(3);
        RMManager manager = control.createMock(RMManager.class);
        EasyMock.expect(rme.getManager()).andReturn(manager).times(2);        
        RMStore store = control.createMock(RMStore.class);
        EasyMock.expect(manager.getStore()).andReturn(store).times(2);
        store.createDestinationSequence(ds);
        EasyMock.expectLastCall();
        store.removeDestinationSequence(id);
        EasyMock.expectLastCall();
        control.replay();
        destination.addSequence(ds);
        assertEquals(1, destination.getAllSequences().size());
        assertSame(ds, destination.getSequence(id));
        destination.removeSequence(ds);
        assertEquals(0, destination.getAllSequences().size());
    }
    
    @Test
    public void testAcknowledgeNoSequence() throws SequenceFault, RMException {
        Message message = setupMessage();
        RMProperties rmps = control.createMock(RMProperties.class);
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).andReturn(rmps);
        EasyMock.expect(rmps.getSequence()).andReturn(null);
        control.replay();
        destination.acknowledge(message);        
    }
    
    @Test
    public void testAcknowledgeUnknownSequence() throws RMException {
        Message message = setupMessage();
        RMProperties rmps = control.createMock(RMProperties.class);
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).andReturn(rmps);
        SequenceType st = control.createMock(SequenceType.class);
        EasyMock.expect(rmps.getSequence()).andReturn(st);
        Identifier id = control.createMock(Identifier.class);
        EasyMock.expect(st.getIdentifier()).andReturn(id).times(2);
        String sid = "sid";
        EasyMock.expect(id.getValue()).andReturn(sid); 
        control.replay();
        try {
            destination.acknowledge(message);   
            fail("Expected SequenceFault not thrown.");
        } catch (SequenceFault ex) {
            assertEquals(RMConstants.getUnknownSequenceFaultCode(), ex.getSequenceFault().getFaultCode());
        }
    }
    
    @Test
    public void testAcknowledgeAlreadyAcknowledgedMessage() throws SequenceFault, RMException, 
    NoSuchMethodException {
        
        Method m1 = Destination.class.getDeclaredMethod("getSequence", new Class[] {Identifier.class});
        destination = control.createMock(Destination.class, new Method[] {m1});
        Message message = setupMessage();
        RMProperties rmps = control.createMock(RMProperties.class);
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).andReturn(rmps);
        SequenceType st = control.createMock(SequenceType.class);
        EasyMock.expect(rmps.getSequence()).andReturn(st);
        Identifier id = control.createMock(Identifier.class);
        EasyMock.expect(st.getIdentifier()).andReturn(id);
        DestinationSequence ds = control.createMock(DestinationSequence.class);
        EasyMock.expect(destination.getSequence(id)).andReturn(ds);
        BigInteger nr = BigInteger.TEN;
        EasyMock.expect(st.getMessageNumber()).andReturn(nr);  
        RMException ex = new RMException(new RuntimeException("already acknowledged"));
        ds.applyDeliveryAssurance(nr);
        EasyMock.expectLastCall().andThrow(ex);
        control.replay();
        try {
            destination.acknowledge(message); 
            fail("Expected RMEcception not thrown.");
        } catch (RMException e) {
            assertSame(ex, e);
        }
        
    }
    
    @Test
    public void testAcknowledgeLastMessage() throws Exception {
        
        Method m1 = Destination.class.getDeclaredMethod("getSequence", new Class[] {Identifier.class});
        Method m2 = Destination.class.getMethod("getReliableEndpoint", new Class[] {});
        
        destination = control.createMock(Destination.class, new Method[] {m1, m2});
        Message message = setupMessage();
        RMProperties rmps = control.createMock(RMProperties.class);
        EasyMock.expect(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).andReturn(rmps);
        SequenceType st = control.createMock(SequenceType.class);
        EasyMock.expect(rmps.getSequence()).andReturn(st);
        Identifier id = control.createMock(Identifier.class);
        EasyMock.expect(st.getIdentifier()).andReturn(id); 
        BigInteger nr = BigInteger.TEN;
        EasyMock.expect(st.getMessageNumber()).andReturn(nr).times(2);
        DestinationSequence ds = control.createMock(DestinationSequence.class);
        EasyMock.expect(destination.getSequence(id)).andReturn(ds);
        
        ds.applyDeliveryAssurance(nr);
        EasyMock.expectLastCall();
        ds.acknowledge(message);
        EasyMock.expectLastCall();
        SequenceType.LastMessage lm = control.createMock(SequenceType.LastMessage.class);
        EasyMock.expect(st.getLastMessage()).andReturn(lm);
        ds.setLastMessageNumber(nr);
        EasyMock.expectLastCall();
        ds.scheduleImmediateAcknowledgement();
        EasyMock.expectLastCall();
        AddressingPropertiesImpl maps = control.createMock(AddressingPropertiesImpl.class);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(null);
        EasyMock.expect(message.get(JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND)).andReturn(maps);
        EndpointReferenceType replyToEPR = control.createMock(EndpointReferenceType.class);
        EasyMock.expect(maps.getReplyTo()).andReturn(replyToEPR).times(2);
        AttributedURIType replyToURI = control.createMock(AttributedURIType.class);
        EasyMock.expect(replyToEPR.getAddress()).andReturn(replyToURI);
        String replyToAddress = "replyTo";        
        EasyMock.expect(replyToURI.getValue()).andReturn(replyToAddress);        
        org.apache.cxf.ws.addressing.v200408.EndpointReferenceType acksToEPR =
            control.createMock(org.apache.cxf.ws.addressing.v200408.EndpointReferenceType.class);
        EasyMock.expect(ds.getAcksTo()).andReturn(acksToEPR);
        AttributedURI acksToURI = control.createMock(AttributedURI.class);
        EasyMock.expect(acksToEPR.getAddress()).andReturn(acksToURI);
        String acksToAddress = "acksTo";
        EasyMock.expect(acksToURI.getValue()).andReturn(acksToAddress);
        EasyMock.expect(ds.canPiggybackAckOnPartialResponse()).andReturn(false);
        EasyMock.expect(destination.getReliableEndpoint()).andReturn(rme);
        Proxy proxy = control.createMock(Proxy.class);
        EasyMock.expect(rme.getProxy()).andReturn(proxy);
        proxy.acknowledge(ds);
        EasyMock.expectLastCall();
        
        control.replay();
        destination.acknowledge(message);   
    }
    
    private Message setupMessage() {
        Message message = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(exchange.getOutMessage()).andReturn(null);
        EasyMock.expect(exchange.getOutFaultMessage()).andReturn(null);
        return message;
    }
    

    
}
