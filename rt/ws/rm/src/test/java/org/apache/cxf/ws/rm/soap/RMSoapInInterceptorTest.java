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
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.StartBodyInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.apache.cxf.ws.rm.RMProperties;
import org.apache.cxf.ws.rm.v200702.AckRequestedType;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement.AcknowledgementRange;
import org.apache.cxf.ws.rm.v200702.SequenceType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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