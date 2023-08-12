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

import java.io.IOException;

import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceType;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class DestinationTest {

    private RMEndpoint rme;
    private Destination destination;

    @Before
    public void setUp() {
        rme = mock(RMEndpoint.class);
        destination = new Destination(rme);
    }

    @Test
    public void testGetSequence() {
        Identifier id = mock(Identifier.class);
        String sid = "s1";
        when(id.getValue()).thenReturn(sid);
        assertNull(destination.getSequence(id));
    }

    @Test
    public void testGetAllSequences() {
        assertEquals(0, destination.getAllSequences().size());
    }

    @Test
    public void testAddRemoveSequence() {
        DestinationSequence ds = mock(DestinationSequence.class);
        ds.setDestination(destination);

        Identifier id = mock(Identifier.class);
        when(ds.getIdentifier()).thenReturn(id);
        String sid = "s1";
        when(id.getValue()).thenReturn(sid);
        RMManager manager = mock(RMManager.class);
        when(rme.getManager()).thenReturn(manager);
        RMStore store = mock(RMStore.class);
        when(manager.getStore()).thenReturn(store);
        
        store.createDestinationSequence(ds);
        store.removeDestinationSequence(id);

        destination.addSequence(ds);
        assertEquals(1, destination.getAllSequences().size());
        assertSame(ds, destination.getSequence(id));
        destination.removeSequence(ds);
        assertEquals(0, destination.getAllSequences().size());
        
        verify(ds, times(3)).getIdentifier();
        verify(id, times(3)).getValue();
        verify(rme, times(2)).getManager();
        verify(manager, times(2)).getStore();
    }

    @Test
    public void testAcknowledgeNoSequence() throws SequenceFault, RMException, IOException {
        Message message = setupMessage();
        RMProperties rmps = mock(RMProperties.class);
        when(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).thenReturn(rmps);
        when(rmps.getSequence()).thenReturn(null);
        destination.acknowledge(message);
    }

    @Test
    public void testAcknowledgeUnknownSequence() throws RMException, IOException {
        Message message = setupMessage();
        RMProperties rmps = mock(RMProperties.class);
        when(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).thenReturn(rmps);
        when(RMContextUtils.getProtocolVariation(message))
            .thenReturn(ProtocolVariation.RM10WSA200408);
        SequenceType st = mock(SequenceType.class);
        when(rmps.getSequence()).thenReturn(st);
        Identifier id = mock(Identifier.class);
        when(st.getIdentifier()).thenReturn(id);
        String sid = "sid";
        when(id.getValue()).thenReturn(sid);
        try {
            destination.acknowledge(message);
            fail("Expected SequenceFault not thrown.");
        } catch (SequenceFault ex) {
            assertEquals(RM10Constants.UNKNOWN_SEQUENCE_FAULT_QNAME, ex.getFaultCode());
        }
        verify(st, times(2)).getIdentifier();
    }

    @Test
    public void testAcknowledgeAlreadyAcknowledgedMessage() throws SequenceFault, RMException,
        NoSuchMethodException, IOException {

        destination = mock(Destination.class);
        Message message = setupMessage();
        RMProperties rmps = mock(RMProperties.class);
        when(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).thenReturn(rmps);
        SequenceType st = mock(SequenceType.class);
        when(rmps.getSequence()).thenReturn(st);
        Identifier id = mock(Identifier.class);
        when(st.getIdentifier()).thenReturn(id);
        DestinationSequence ds = mock(DestinationSequence.class);
        when(destination.getSequence(id)).thenReturn(ds);
        long nr = 10;
        when(st.getMessageNumber()).thenReturn(nr);
        when(ds.applyDeliveryAssurance(nr, message)).thenReturn(false);
        InterceptorChain ic = mock(InterceptorChain.class);
        when(message.getInterceptorChain()).thenReturn(ic);
        destination.acknowledge(message);
    }

/*    @Test
    public void testAcknowledgeLastMessage() throws Exception {

        Method m1 = Destination.class.getDeclaredMethod("getSequence", new Class[] {Identifier.class});
        Method m2 = Destination.class.getMethod("getReliableEndpoint", new Class[] {});

        destination = mock(Destination.class, new Method[] {m1, m2});
        Message message = setupMessage();
        RMProperties rmps = mock(RMProperties.class);
        when(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).thenReturn(rmps);
        SequenceType st = mock(SequenceType.class);
        when(rmps.getSequence()).thenReturn(st);
        Identifier id = mock(Identifier.class);
        when(st.getIdentifier()).thenReturn(id);
        long nr = 10;
        when(st.getMessageNumber()).thenReturn(nr).times(3);
        DestinationSequence ds = mock(DestinationSequence.class);
        when(destination.getSequence(id)).thenReturn(ds);

        ds.applyDeliveryAssurance(nr, message);
        whenLastCall().thenReturn(Boolean.TRUE);
        ds.acknowledge(message);
        whenLastCall();
        SequenceType.LastMessage lm = mock(SequenceType.LastMessage.class);
        when(st.getLastMessage()).thenReturn(lm);
        ds.setLastMessageNumber(nr);
        whenLastCall();
        ds.scheduleImmediateAcknowledgement();
        whenLastCall();
        AddressingPropertiesImpl maps = mock(AddressingPropertiesImpl.class);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(null);
        when(message.get(JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND)).thenReturn(maps);
        EndpointReferenceType replyToEPR = mock(EndpointReferenceType.class);
        when(maps.getReplyTo()).thenReturn(replyToEPR).times(2);
        AttributedURIType replyToURI = mock(AttributedURIType.class);
        when(replyToEPR.getAddress()).thenReturn(replyToURI);
        String replyToAddress = "replyTo";
        when(replyToURI.getValue()).thenReturn(replyToAddress);
        org.apache.cxf.ws.addressing.v200408.EndpointReferenceType acksToEPR =
            mock(org.apache.cxf.ws.addressing.v200408.EndpointReferenceType.class);
        when(ds.getAcksTo()).thenReturn(acksToEPR);
        AttributedURI acksToURI = mock(AttributedURI.class);
        when(acksToEPR.getAddress()).thenReturn(acksToURI);
        String acksToAddress = "acksTo";
        when(acksToURI.getValue()).thenReturn(acksToAddress);
        when(ds.canPiggybackAckOnPartialResponse()).thenReturn(false);
        when(destination.getReliableEndpoint()).thenReturn(rme).times(2);
        RMManager manager = mock(RMManager.class);
        when(rme.getManager()).thenReturn(manager);
        RMStore store = mock(RMStore.class);
        when(manager.getStore()).thenReturn(store);
        Proxy proxy = mock(Proxy.class);
        when(rme.getProxy()).thenReturn(proxy);
        proxy.acknowledge(ds);
        whenLastCall();

        control.replay();
        destination.acknowledge(message);
    }   */

    private Message setupMessage() throws IOException {
        Message message = mock(Message.class);
        Exchange exchange = mock(Exchange.class);
        org.apache.cxf.transport.Destination tdest = mock(org.apache.cxf.transport.Destination.class);
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getOutMessage()).thenReturn(null);
        when(exchange.getOutFaultMessage()).thenReturn(null);
        when(exchange.getDestination()).thenReturn(tdest);
        when(tdest.getBackChannel(message)).thenReturn(null);
        return message;
    }
}