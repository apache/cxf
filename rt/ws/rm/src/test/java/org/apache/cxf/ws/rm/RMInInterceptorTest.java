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
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.manager.RetryPolicyType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RMInInterceptorTest {

    private RMInInterceptor interceptor;
    private RMManager manager;
    private RMEndpoint rme;
    private RMProperties rmps;


    @Before
    public void setUp() {
        rmps = mock(RMProperties.class);
    }

    @Test
    public void testOrdering() {
        Phase p = new Phase(Phase.PRE_LOGICAL, 1);
        SortedSet<Phase> phases = new TreeSet<>();
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
        when(message.get(AssertionInfoMap.class)).thenReturn(null);

        interceptor.handle(message);
        verify(rme, times(1)).receivedControlMessage();
        verify(message, times(2)).getExchange();
    }

    @Test
    public void testHandleCreateSequenceOnClient() throws SequenceFault, RMException {
        interceptor = new RMInInterceptor();
        Message message = setupInboundMessage(RM10Constants.CREATE_SEQUENCE_ACTION, false);

        Servant servant = mock(Servant.class);
        when(rme.getServant()).thenReturn(servant);
        CreateSequenceResponseType csr = mock(CreateSequenceResponseType.class);
        when(servant.createSequence(message)).thenReturn(csr);
        Proxy proxy = mock(Proxy.class);
        when(rme.getProxy()).thenReturn(proxy);

        interceptor.handle(message);
        verify(rme, times(1)).receivedControlMessage();
        verify(proxy, times(1)).createSequenceResponse(csr, ProtocolVariation.RM10WSA200408);
        verify(message, times(2)).getExchange();
    }

    @Test
    public void testHandleSequenceAckOnClient() throws SequenceFault, RMException, NoSuchMethodException {
        testHandleSequenceAck(false);
    }

    @Test
    public void testHandleSequenceAckOnServer() throws SequenceFault, RMException, NoSuchMethodException {
        testHandleSequenceAck(true);
    }

    private void testHandleSequenceAck(boolean onServer)
        throws SequenceFault, RMException, NoSuchMethodException {
        interceptor = spy(new RMInInterceptor());
        Message message = setupInboundMessage(RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION, onServer);
        when(message.get(AssertionInfoMap.class)).thenReturn(null);

        interceptor.handle(message);
        verify(rme, times(1)).receivedControlMessage();
        verify(interceptor, times(1)).processAcknowledgments(rme, rmps, ProtocolVariation.RM10WSA200408);
        verify(message, times(2)).getExchange();
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
        when(message.get(AssertionInfoMap.class)).thenReturn(null);

        interceptor.handle(message);
        verify(message, times(2)).getExchange();
        verify(rme, times(1)).receivedControlMessage();
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
        interceptor = spy(new RMInInterceptor());
        Message message = setupInboundMessage("greetMe", true);
        Destination d = mock(Destination.class);
        when(manager.getDestination(message)).thenReturn(d);
        when(message.get(AssertionInfoMap.class)).thenReturn(null);

        Exchange ex = mock(Exchange.class);
        when(message.getExchange()).thenReturn(ex);
        when(ex.get("deferred.uncorrelated.message.abort")).thenReturn(Boolean.TRUE);
        InterceptorChain chain = mock(InterceptorChain.class);
        when(message.getInterceptorChain()).thenReturn(chain);

        interceptor.handle(message);
        verify(interceptor, times(1)).processAcknowledgments(rme, rmps, ProtocolVariation.RM10WSA200408);
        verify(interceptor, times(1)).processAcknowledgmentRequests(d, message);
        verify(interceptor, times(1)).processSequence(d, message);
        verify(interceptor, times(1)).processDeliveryAssurance(rmps);
        verify(chain, times(1)).abort();
    }

    @Test
    public void testProcessAcknowledgments() throws RMException {
        interceptor = new RMInInterceptor();
        manager = mock(RMManager.class);
        Source source = mock(Source.class);
        rme = mock(RMEndpoint.class);
        when(rme.getSource()).thenReturn(source);
        interceptor.setManager(manager);
        SequenceAcknowledgement ack1 = mock(SequenceAcknowledgement.class);
        SequenceAcknowledgement ack2 = mock(SequenceAcknowledgement.class);
        Collection<SequenceAcknowledgement> acks = new ArrayList<>();
        acks.add(ack1);
        acks.add(ack2);
        when(rmps.getAcks()).thenReturn(acks);
        Identifier id1 = mock(Identifier.class);
        when(ack1.getIdentifier()).thenReturn(id1);
        SourceSequence ss1 = mock(SourceSequence.class);
        when(source.getSequence(id1)).thenReturn(ss1);

        Identifier id2 = mock(Identifier.class);
        when(ack2.getIdentifier()).thenReturn(id2);
        when(source.getSequence(id2)).thenReturn(null);
        try {
            interceptor.processAcknowledgments(rme, rmps, ProtocolVariation.RM10WSA200408);
            fail("Expected SequenceFault not thrown");
        } catch (SequenceFault sf) {
            assertEquals(RM10Constants.UNKNOWN_SEQUENCE_FAULT_QNAME, sf.getFaultCode());
        }

        verify(ss1, times(1)).setAcknowledged(ack1);
    }

    @Test
    public void testProcessSequence() throws SequenceFault, RMException {
        Destination destination = mock(Destination.class);
        Message message = mock(Message.class);
        interceptor = new RMInInterceptor();
        interceptor.processSequence(destination, message);
        verify(destination, times(1)).acknowledge(message);
    }

    @Test
    public void testProcessInvalidMessage() throws SequenceFault, RMException {
        interceptor = new RMInInterceptor();

        Message message = mock(Message.class);
        Exchange exchange = mock(Exchange.class);
        org.apache.cxf.transport.Destination destination =
            mock(org.apache.cxf.transport.Destination.class);
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getDestination()).thenReturn(destination);
        when(exchange.getOutMessage()).thenReturn(null);
        when(exchange.getOutFaultMessage()).thenReturn(null);

        try {
            interceptor.handle(message);
            fail("must reject the invalid rm message");
        } catch (RMException e) {
            // verify a partial error text match to exclude an unexpected exception
            // (see WSA_REQUIRED_EXC in Messages.properties)
            final String text = "WS-Addressing is required";
            assertTrue(e.getMessage() != null
                && e.getMessage().indexOf(text) >= 0);
        }

        when(message.getExchange()).thenReturn(exchange);
        AddressingProperties maps = mock(AddressingProperties.class);
        when(maps.getNamespaceURI()).thenReturn(Names200408.WSA_NAMESPACE_NAME);
        when(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND)).thenReturn(maps);
        AttributedURIType actionURI = mock(AttributedURIType.class);
        when(maps.getAction()).thenReturn(actionURI);
        when(actionURI.getValue()).thenReturn("foo");
        when(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).thenReturn(rmps);
        when(exchange.getDestination()).thenReturn(destination);
        when(exchange.getOutMessage()).thenReturn(null);
        when(exchange.getOutFaultMessage()).thenReturn(null);

        try {
            interceptor.handle(message);
            fail("must reject the invalid rm message");
        } catch (RMException e) {
            // verify a partial error text match to exclude an unexpected exception
            // (see WSRM_REQUIRED_EXC in Messages.properties)
            final String text = "WS-ReliableMessaging is required";
            assertTrue(e.getMessage() != null
                && e.getMessage().indexOf(text) >= 0);
        }

        verify(maps, times(2)).getAction();
    }

    @Test
    public void testProcessInvalidMessageOnFault() throws SequenceFault, RMException {
        interceptor = new RMInInterceptor();
        manager = mock(RMManager.class);
        interceptor.setManager(manager);

        Message message = mock(Message.class);
        Exchange exchange = mock(Exchange.class);
        when(message.getExchange()).thenReturn(exchange);

        try {
            interceptor.handleFault(message);
        } catch (Exception e) {
            fail("unexpected exception thrown from handleFault: " + e);
        }

        when(message.getExchange()).thenReturn(exchange);
        when(message.get(RMMessageConstants.DELIVERING_ROBUST_ONEWAY)).thenReturn(true);

        try {
            interceptor.handleFault(message);
        } catch (Exception e) {
            fail("unexpected exception thrown from handleFault: " + e);
        }

        org.apache.cxf.transport.Destination td = mock(org.apache.cxf.transport.Destination.class);
        when(exchange.getDestination()).thenReturn(td);
        when(message.getExchange()).thenReturn(exchange);
        when(message.get(RMMessageConstants.RM_PROTOCOL_VARIATION))
            .thenReturn(ProtocolVariation.RM10WSA200408);
        when(message.getContent(Exception.class)).thenReturn(new SequenceFault("no sequence"));
        DestinationPolicyType dp = new DestinationPolicyType();
        RetryPolicyType rp = new RetryPolicyType();
        dp.setRetryPolicy(rp);
        when(manager.getDestinationPolicy()).thenReturn(dp);
        RedeliveryQueue rq = mock(RedeliveryQueue.class);
        when(manager.getRedeliveryQueue()).thenReturn(rq);
        doThrow(new RuntimeException("shouldn't be queued")).when(rq).addUndelivered(message);

        try {
            interceptor.handleFault(message);
        } catch (Exception e) {
            fail("unexpected exception thrown from handleFault: " + e);
        }
    }

    @Test
    public void testProcessValidMessageOnFault() throws SequenceFault, RMException {
        interceptor = new RMInInterceptor();
        manager = mock(RMManager.class);
        Message message = mock(Message.class);
        Exchange exchange = mock(Exchange.class);
        AddressingProperties maps = mock(AddressingProperties.class);

        interceptor.setManager(manager);

        // test 1. a normal sequence fault case without non-anonymous faultTo
        when(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND)).thenReturn(maps);
        when(message.getExchange()).thenReturn(exchange);
        when(message.get(RMMessageConstants.RM_PROTOCOL_VARIATION))
            .thenReturn(ProtocolVariation.RM10WSA200408);
        when(message.getContent(Exception.class)).thenReturn(new SequenceFault("test"));

        try {
            interceptor.handleFault(message);
        } catch (Exception e) {
            fail("unexpected exception thrown from handleFault: " + e);
        }

        verify(exchange, times(1)).setOneWay(false);

        // 2. a sequence fault case with non anonymous faultTo
        reset(exchange);
        Destination d = mock(Destination.class);
        Endpoint ep = mock(Endpoint.class);
        EndpointInfo epi = mock(EndpointInfo.class);
        when(ep.getEndpointInfo()).thenReturn(epi);
        when(exchange.getEndpoint()).thenReturn(ep);
        when(maps.getFaultTo())
            .thenReturn(RMUtils.createReference("http://localhost:9999/decoupled"));
        when(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND)).thenReturn(maps);
        when(message.getExchange()).thenReturn(exchange);
        when(message.get(RMMessageConstants.RM_PROTOCOL_VARIATION))
            .thenReturn(ProtocolVariation.RM10WSA200408);
        when(message.getContent(Exception.class)).thenReturn(new SequenceFault("test"));

        try {
            interceptor.handleFault(message);
        } catch (Exception e) {
            fail("unexpected exception thrown from handleFault: " + e);
        }
        verify(exchange, times(1)).setOneWay(false);
        verify(exchange, times(1)).setDestination(any(org.apache.cxf.transport.Destination.class));

        // 3. a robust oneway case
        reset(exchange);
        when(maps.getFaultTo())
            .thenReturn(RMUtils.createAnonymousReference());
        when(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND)).thenReturn(maps);
        when(manager.getDestination(message)).thenReturn(d);
        when(message.getExchange()).thenReturn(exchange);
        when(message.get(RMMessageConstants.DELIVERING_ROBUST_ONEWAY)).thenReturn(true);
        when(message.get(RMMessageConstants.RM_PROTOCOL_VARIATION))
            .thenReturn(ProtocolVariation.RM10WSA200408);

        try {
            interceptor.handleFault(message);
        } catch (Exception e) {
            fail("unexpected exception thrown from handleFault: " + e);
        }

        // 4. a runtime exception case
        when(maps.getFaultTo())
            .thenReturn(RMUtils.createAnonymousReference());
        when(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND)).thenReturn(maps);
        when(message.getExchange()).thenReturn(exchange);
        when(message.get(RMMessageConstants.RM_PROTOCOL_VARIATION))
            .thenReturn(ProtocolVariation.RM10WSA200408);
        when(message.getContent(Exception.class)).thenReturn(new RuntimeException("test"));

        try {
            interceptor.handleFault(message);
        } catch (Exception e) {
            fail("unexpected exception thrown from handleFault: " + e);
        }
//      verified in tearDown
    }

    private Message setupInboundMessage(String action, boolean serverSide) throws RMException {
        Message message = mock(Message.class);
        Exchange exchange = mock(Exchange.class);
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getOutMessage()).thenReturn(null);
        when(exchange.getOutFaultMessage()).thenReturn(null);
        when(message.get(RMMessageConstants.RM_PROPERTIES_INBOUND)).thenReturn(rmps);

        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(!serverSide);
        AddressingProperties maps = mock(AddressingProperties.class);
        when(maps.getNamespaceURI()).thenReturn(Names200408.WSA_NAMESPACE_NAME);
        when(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND)).thenReturn(maps);

        AttributedURIType actionURI = mock(AttributedURIType.class);
        when(maps.getAction()).thenReturn(actionURI);
        when(actionURI.getValue()).thenReturn(action);

        when(message.get(RMMessageConstants.ORIGINAL_REQUESTOR_ROLE)).thenReturn(Boolean.FALSE);
        when(message.put(Message.REQUESTOR_ROLE, Boolean.FALSE)).thenReturn(null);

        org.apache.cxf.transport.Destination td =
            serverSide ? mock(org.apache.cxf.transport.Destination.class) : null;
        when(exchange.getDestination()).thenReturn(td);

        manager = mock(RMManager.class);
        RMConfiguration config = new RMConfiguration();
        config.setRMNamespace(RM10Constants.NAMESPACE_URI);
        config.setRM10AddressingNamespace(RM10Constants.NAMESPACE_URI);
        when(manager.getEffectiveConfiguration(message)).thenReturn(config);
        interceptor.setManager(manager);
        rme = mock(RMEndpoint.class);
        when(manager.getReliableEndpoint(message)).thenReturn(rme);

        when(rmps.getNamespaceURI()).thenReturn(RM10Constants.NAMESPACE_URI);

        return message;
    }
}
