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

import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.VersionTransformer.Names200408;
import org.apache.cxf.ws.rm.v200702.Identifier;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RMOutInterceptorTest {
    @Test
    public void testHandleRuntimeFault() throws NoSuchMethodException, SequenceFault, RMException {
        RMOutInterceptor interceptor = spy(new RMOutInterceptor());
        Message message = mock(Message.class);
        when(interceptor.isRuntimeFault(message)).thenReturn(true);
        interceptor.handle(message);
    }

    @Test
    public void testHandleNoMAPs() throws NoSuchMethodException, SequenceFault, RMException {
        RMOutInterceptor interceptor = spy(new RMOutInterceptor());
        Message message = mock(Message.class);
        when(interceptor.isRuntimeFault(message)).thenReturn(false);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.FALSE);
        when(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND))
            .thenReturn(null);
        interceptor.handle(message);
    }

    @Test
    public void testHandleApplicationMessage() throws NoSuchMethodException, SequenceFault, RMException {
        AddressingProperties maps = createMAPs("greetMe", "localhost:9000/GreeterPort",
            org.apache.cxf.ws.addressing.Names.WSA_NONE_ADDRESS);
        RMOutInterceptor interceptor = spy(new RMOutInterceptor());
        RMManager manager = mock(RMManager.class);
        doReturn(manager).when(interceptor).getManager();

        Message message = mock(Message.class);
        when(interceptor.isRuntimeFault(message)).thenReturn(false);
        Exchange ex = mock(Exchange.class);
        when(message.getExchange()).thenReturn(ex);
        when(ex.getOutMessage()).thenReturn(message);
        when(ex.put("defer.uncorrelated.message.abort", Boolean.TRUE)).thenReturn(null);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.TRUE);
        when(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND))
            .thenReturn(maps);
        RMProperties rmpsOut = new RMProperties();
        when(message.get(RMMessageConstants.RM_PROPERTIES_OUTBOUND)).
            thenReturn(rmpsOut);
        InterceptorChain chain = mock(InterceptorChain.class);
        when(message.getInterceptorChain()).thenReturn(chain);

        RMEndpoint rme = mock(RMEndpoint.class);
        RMConfiguration config = new RMConfiguration();
        config.setRMNamespace(RM10Constants.NAMESPACE_URI);
        config.setRM10AddressingNamespace(Names200408.WSA_NAMESPACE_NAME);
        when(rme.getConfiguration()).thenReturn(config);
        when(manager.getEffectiveConfiguration(message)).thenReturn(config);
        Source source = mock(Source.class);
        when(source.getReliableEndpoint()).thenReturn(rme);
        when(manager.getSource(message)).thenReturn(source);
        Destination destination = mock(Destination.class);
        when(manager.getDestination(message)).thenReturn(destination);
        SourceSequence sseq = mock(SourceSequence.class);
        when(sseq.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);
        when(manager.getSequence(isNull(Identifier.class), same(message),
                                        same(maps))).thenReturn(sseq);
        when(sseq.nextMessageNumber(isNull(Identifier.class),
            eq(0L), eq(false))).thenReturn(Long.valueOf(10));
        when(sseq.isLastMessage()).thenReturn(false);

        Identifier sid = mock(Identifier.class);
        when(sseq.getIdentifier()).thenReturn(sid);
        when(sseq.getCurrentMessageNr()).thenReturn(Long.valueOf(10));

        interceptor.handle(message);
        verify(interceptor, times(1)).addAcknowledgements(same(destination), same(rmpsOut),
            isNull(Identifier.class), isA(AttributedURIType.class));
    }

    @Test
    public void testIsRuntimeFault() {
        Message message = mock(Message.class);
        Exchange exchange = mock(Exchange.class);
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getOutFaultMessage()).thenReturn(message);
        when(message.get(FaultMode.class)).thenReturn(FaultMode.RUNTIME_FAULT);
        RMOutInterceptor rmi = new RMOutInterceptor();
        assertTrue(rmi.isRuntimeFault(message));

        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getOutFaultMessage()).thenReturn(null);
        assertFalse(rmi.isRuntimeFault(message));
    }

    @Test
    public void testRM11TerminateSequence() throws RMException, SequenceFault {
        testRMTerminateSequence(RM11Constants.NAMESPACE_URI, Names.WSA_NAMESPACE_NAME,
                                RM11Constants.TERMINATE_SEQUENCE_ACTION,
                                org.apache.cxf.ws.addressing.Names.WSA_ANONYMOUS_ADDRESS,
                                org.apache.cxf.ws.addressing.Names.WSA_ANONYMOUS_ADDRESS);
    }

    @Test
    public void testRM10TerminateSequence() throws RMException, SequenceFault {
        testRMTerminateSequence(RM10Constants.NAMESPACE_URI, Names.WSA_NAMESPACE_NAME,
                                RM10Constants.TERMINATE_SEQUENCE_ACTION,
                                org.apache.cxf.ws.addressing.Names.WSA_ANONYMOUS_ADDRESS,
                                org.apache.cxf.ws.addressing.Names.WSA_NONE_ADDRESS);
    }

    private void testRMTerminateSequence(String wsrmnsuri, String wsansuri,
                                         String action, String breplyto, String areplyto)
        throws RMException, SequenceFault {
        AddressingProperties maps = createMAPs(action, "localhost:9000/GreeterPort", breplyto);

        Message message = mock(Message.class);
        Exchange exchange = mock(Exchange.class);
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getOutMessage()).thenReturn(message);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.TRUE);
        when(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND))
            .thenReturn(maps);
        RMManager manager = mock(RMManager.class);
        RMConfiguration config = new RMConfiguration();
        config.setRMNamespace(wsrmnsuri);
        config.setRM10AddressingNamespace(wsansuri);
        when(manager.getEffectiveConfiguration(message)).thenReturn(config);

        RMOutInterceptor rmi = new RMOutInterceptor();
        rmi.setManager(manager);
        rmi.handle(message);

        assertEquals(areplyto,
                     maps.getReplyTo().getAddress().getValue());
    }

    private AddressingProperties createMAPs(String action, String to, String replyTo) {
        AddressingProperties maps = new AddressingProperties();
        AttributedURIType actionuri = new AttributedURIType();
        actionuri.setValue(action);
        maps.setAction(actionuri);
        maps.setTo(RMUtils.createReference(to));
        EndpointReferenceType epr = RMUtils.createReference(replyTo);
        maps.setReplyTo(epr);
        return maps;

    }
}
