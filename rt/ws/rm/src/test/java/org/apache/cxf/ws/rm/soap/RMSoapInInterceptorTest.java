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

package org.apache.cxf.ws.rm.soap;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.StartBodyInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.rm.AbstractRMInterceptor;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.apache.cxf.ws.rm.RMException;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMProperties;
import org.apache.cxf.ws.rm.v200702.AckRequestedType;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement.AcknowledgementRange;
import org.apache.cxf.ws.rm.v200702.SequenceType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RMSoapInInterceptorTest {

    private static final String SEQ_IDENTIFIER = "http://Business456.com/RM/ABC";
    private static final Long ONE = Long.valueOf(1);
    private static final Long MSG1_MESSAGE_NUMBER = ONE;
    private static final Long MSG2_MESSAGE_NUMBER = Long.valueOf(2);

    @Test
    public void testGetUnderstoodHeaders() throws Exception {
        RMSoapInInterceptor codec = new RMSoapInInterceptor();
        Set<QName> headers = codec.getUnderstoodHeaders();
        assertTrue("expected Sequence header", headers.contains(RM10Constants.SEQUENCE_QNAME));
        assertTrue("expected SequenceAcknowledgment header",
                   headers.contains(RM10Constants.SEQUENCE_ACK_QNAME));
        assertTrue("expected AckRequested header",
                   headers.contains(RM10Constants.ACK_REQUESTED_QNAME));
    }

    @Test
    public void testDecodeSequence() throws XMLStreamException {
        SoapMessage message = setUpInboundMessage("resources/Message1.xml");
        RMSoapInInterceptor codec = new RMSoapInInterceptor();
        codec.handleMessage(message);
        RMProperties rmps = RMContextUtils.retrieveRMProperties(message, false);
        SequenceType st = rmps.getSequence();
        assertNotNull(st);
        assertEquals(st.getIdentifier().getValue(), SEQ_IDENTIFIER);
        assertEquals(st.getMessageNumber(), MSG1_MESSAGE_NUMBER);

        assertNull(rmps.getAcks());
        assertNull(rmps.getAcksRequested());

    }

    @Test
    public void testDecodeAcknowledgements() throws XMLStreamException {
        SoapMessage message = setUpInboundMessage("resources/Acknowledgment.xml");
        RMSoapInInterceptor codec = new RMSoapInInterceptor();
        codec.handleMessage(message);
        RMProperties rmps = RMContextUtils.retrieveRMProperties(message, false);
        Collection<SequenceAcknowledgement> acks = rmps.getAcks();
        assertNotNull(acks);
        assertEquals(1, acks.size());
        SequenceAcknowledgement ack = acks.iterator().next();
        assertNotNull(ack);
        assertEquals(ack.getIdentifier().getValue(), SEQ_IDENTIFIER);
        assertEquals(2, ack.getAcknowledgementRange().size());
        AcknowledgementRange r1 = ack.getAcknowledgementRange().get(0);
        AcknowledgementRange r2 = ack.getAcknowledgementRange().get(1);
        verifyRange(r1, 1, 1);
        verifyRange(r2, 3, 3);
        assertNull(rmps.getSequence());
        assertNull(rmps.getAcksRequested());
    }

    @Test
    public void testDecodeAcknowledgements2() throws XMLStreamException {
        SoapMessage message = setUpInboundMessage("resources/Acknowledgment2.xml");
        RMSoapInInterceptor codec = new RMSoapInInterceptor();
        codec.handleMessage(message);
        RMProperties rmps = RMContextUtils.retrieveRMProperties(message, false);
        Collection<SequenceAcknowledgement> acks = rmps.getAcks();
        assertNotNull(acks);
        assertEquals(1, acks.size());
        SequenceAcknowledgement ack = acks.iterator().next();
        assertNotNull(ack);
        assertEquals(1, ack.getAcknowledgementRange().size());
        AcknowledgementRange r1 = ack.getAcknowledgementRange().get(0);
        verifyRange(r1, 1, 3);
        assertNull(rmps.getSequence());
        assertNull(rmps.getAcksRequested());
    }

    private void verifyRange(AcknowledgementRange r, int i, int j) {
        assertNotNull(r);
        if (i > 0) {
            assertNotNull(r.getLower());
            assertEquals(i, r.getLower().longValue());
        }
        if (j > 0) {
            assertNotNull(r.getUpper());
            assertEquals(j, r.getUpper().longValue());
        }
    }

    @Test
    public void testDecodeAcksRequested() throws XMLStreamException {
        SoapMessage message = setUpInboundMessage("resources/Retransmission.xml");
        RMSoapInInterceptor codec = new RMSoapInInterceptor();
        codec.handleMessage(message);
        RMProperties rmps = RMContextUtils.retrieveRMProperties(message, false);
        Collection<AckRequestedType> requested = rmps.getAcksRequested();
        assertNotNull(requested);
        assertEquals(1, requested.size());
        AckRequestedType ar = requested.iterator().next();
        assertNotNull(ar);
        assertEquals(ar.getIdentifier().getValue(), SEQ_IDENTIFIER);

        SequenceType s = rmps.getSequence();
        assertNotNull(s);
        assertEquals(s.getIdentifier().getValue(), SEQ_IDENTIFIER);
        assertEquals(s.getMessageNumber(), MSG2_MESSAGE_NUMBER);

        assertNull(rmps.getAcks());
    }

    @Test
    public void testPartialResponseToTwoWayRequestKeepsExchange() throws XMLStreamException {
        // e.g. the acknowledgements for a Sequence with an anonymous AcksTo, with a decoupled endpoint
        SoapMessage message = setUpPartialResponse(false);
        BindingOperationInfo boi = new BindingOperationInfo();
        message.getExchange().put(BindingOperationInfo.class, boi);
        RMSoapInInterceptor codec = new RMSoapInInterceptor();
        codec.handleMessage(message);

        // the exchange stays with the application operation, for the reply
        assertSame(boi, message.getExchange().getBindingOperationInfo());
        assertFalse(message.getExchange().isOneWay());
        // and the acknowledgements are still decoded
        RMProperties rmps = RMContextUtils.retrieveRMProperties(message, false);
        assertEquals(1, rmps.getAcks().size());
    }

    @Test
    public void testPartialResponseToOneWayRequestUpdatesExchange() throws Exception {
        SoapMessage message = setUpPartialResponse(true);
        RMManager manager = mock(RMManager.class);
        RMException failure = new RMException(new Exception("no reliable endpoint"));
        when(manager.getReliableEndpoint(message)).thenThrow(failure);
        AbstractRMInterceptor<?> rmInterceptor = mock(AbstractRMInterceptor.class);
        when(rmInterceptor.getManager()).thenReturn(manager);
        InterceptorChain chain = mock(InterceptorChain.class);
        when(chain.getIterator()).thenReturn(List.<Interceptor<? extends Message>>of(rmInterceptor).listIterator());
        message.setInterceptorChain(chain);
        RMSoapInInterceptor codec = new RMSoapInInterceptor();
        try {
            codec.handleMessage(message);
            fail("expected the exchange to be updated with the reliable endpoint");
        } catch (SoapFault ex) {
            assertSame(failure, ex.getCause());
        }
        verify(manager).getReliableEndpoint(message);
    }

    private SoapMessage setUpPartialResponse(boolean oneWay) throws XMLStreamException {
        SoapMessage message = setUpInboundMessage("resources/Acknowledgment2.xml");
        message.put(Message.PARTIAL_RESPONSE_MESSAGE, Boolean.TRUE);
        RMContextUtils.retrieveMAPs(message, false, false)
            .setAction(ContextUtils.getAttributedURI(RM10Constants.SEQUENCE_ACKNOWLEDGMENT_ACTION));

        Exchange exchange = new ExchangeImpl();
        exchange.setOneWay(oneWay);
        Message request = new MessageImpl();
        request.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        AddressingProperties requestMaps = new AddressingProperties();
        requestMaps.setAction(ContextUtils.getAttributedURI("http://cxf.apache.org/greeter_control/Greeter/greetMe"));
        RMContextUtils.storeMAPs(requestMaps, request, true, true);
        exchange.setOutMessage(request);
        message.setExchange(exchange);
        return message;
    }

    private SoapMessage setUpInboundMessage(String resource) throws XMLStreamException {
        Message message = new MessageImpl();
        SoapMessage soapMessage = new SoapMessage(message);
        RMProperties rmps = new RMProperties();
        rmps.exposeAs(RM10Constants.NAMESPACE_URI);
        RMContextUtils.storeRMProperties(soapMessage, rmps, false);
        AddressingProperties maps = new AddressingProperties();
        RMContextUtils.storeMAPs(maps, soapMessage, false, false);
        message.put(Message.SCHEMA_VALIDATION_ENABLED, false);
        InputStream is = RMSoapInInterceptorTest.class.getResourceAsStream(resource);
        assertNotNull(is);
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(is);
        soapMessage.setContent(XMLStreamReader.class, reader);
        ReadHeadersInterceptor rji = new ReadHeadersInterceptor(BusFactory.getDefaultBus());
        rji.handleMessage(soapMessage);
        StartBodyInterceptor sbi = new StartBodyInterceptor();
        sbi.handleMessage(soapMessage);
        return soapMessage;
    }
}