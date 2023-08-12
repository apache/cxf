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

import java.util.List;

import javax.xml.datatype.Duration;

import org.apache.cxf.jaxb.DatatypeFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.v200502.CreateSequenceResponseType;
import org.apache.cxf.ws.rm.v200502.CreateSequenceType;
import org.apache.cxf.ws.rm.v200502.Expires;
import org.apache.cxf.ws.rm.v200502.Identifier;
import org.apache.cxf.ws.rm.v200502.OfferType;
import org.apache.cxf.ws.rm.v200502.TerminateSequenceType;
import org.apache.cxf.ws.rm.v200702.CloseSequenceType;
import org.apache.cxf.ws.rm.v200702.TerminateSequenceResponseType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ServantTest {
    private static final String SERVICE_URL = "http://localhost:9000/SoapContext/GreeterPort";
    private static final String DECOUPLED_URL = "http://localhost:9990/decoupled_endpoint";

    private static final org.apache.cxf.ws.rm.manager.ObjectFactory RMMANGER_FACTORY =
        new org.apache.cxf.ws.rm.manager.ObjectFactory();
    private static final Duration DURATION_SHORT = DatatypeFactory.createDuration("PT5S");
    private static final Duration DURATION_VERY_SHORT = DatatypeFactory.createDuration("PT2S");
    private static final Duration DURATION_DEFAULT = DatatypeFactory.createDuration("P0Y0M0DT0H0M0.0S");

    @Test
    public void testCreateSequence() throws SequenceFault {
        RMEndpoint rme = mock(RMEndpoint.class);
        RMManager manager = new RMManager();
        Destination destination = new Destination(rme);
        SequenceIdentifierGenerator generator = manager.new DefaultSequenceIdentifierGenerator();
        manager.setIdGenerator(generator);

        when(rme.getDestination()).thenReturn(destination);
        when(rme.getManager()).thenReturn(manager);

        Servant servant = new Servant(rme);

        verifyCreateSequenceDefault(servant, manager);

        verifyCreateSequenceExpiresSetAtDestination(servant, manager);

        verifyCreateSequenceExpiresSetAtSource(servant, manager);

        verifyCreateSequenceExpiresSetAtBoth(servant, manager);

    }

    private void verifyCreateSequenceDefault(Servant servant, RMManager manager) throws SequenceFault {
        DestinationPolicyType dp = RMMANGER_FACTORY.createDestinationPolicyType();
        AcksPolicyType ap = RMMANGER_FACTORY.createAcksPolicyType();
        dp.setAcksPolicy(ap);

        manager.setDestinationPolicy(dp);

        Expires expires = new Expires();
        expires.setValue(DatatypeFactory.createDuration("P0Y0M0DT0H0M0.0S"));
        Message message = createTestCreateSequenceMessage(expires, null);

        CreateSequenceResponseType csr = (CreateSequenceResponseType)servant.createSequence(message);

        Expires expires2 = csr.getExpires();

        assertNotNull(expires2);
        assertEquals(DatatypeFactory.PT0S, expires2.getValue());
    }

    private void verifyCreateSequenceExpiresSetAtDestination(Servant servant, RMManager manager)
        throws SequenceFault {
        DestinationPolicyType dp = RMMANGER_FACTORY.createDestinationPolicyType();
        AcksPolicyType ap = RMMANGER_FACTORY.createAcksPolicyType();
        dp.setAcksPolicy(ap);
        dp.setSequenceExpiration(DURATION_SHORT);
        manager.setDestinationPolicy(dp);

        Expires expires = new Expires();
        expires.setValue(DURATION_DEFAULT);
        Message message = createTestCreateSequenceMessage(expires, null);

        CreateSequenceResponseType csr = (CreateSequenceResponseType)servant.createSequence(message);

        Expires expires2 = csr.getExpires();

        assertNotNull(expires2);
        assertEquals(DURATION_SHORT, expires2.getValue());
    }

    private void verifyCreateSequenceExpiresSetAtSource(Servant servant, RMManager manager)
        throws SequenceFault {
        DestinationPolicyType dp = RMMANGER_FACTORY.createDestinationPolicyType();
        AcksPolicyType ap = RMMANGER_FACTORY.createAcksPolicyType();
        dp.setAcksPolicy(ap);
        manager.setDestinationPolicy(dp);

        Expires expires = new Expires();
        expires.setValue(DURATION_SHORT);

        Message message = createTestCreateSequenceMessage(expires, null);

        CreateSequenceResponseType csr = (CreateSequenceResponseType)servant.createSequence(message);

        Expires expires2 = csr.getExpires();

        assertNotNull(expires2);
        assertEquals(DURATION_SHORT, expires2.getValue());
    }

    private void verifyCreateSequenceExpiresSetAtBoth(Servant servant, RMManager manager)
        throws SequenceFault {
        DestinationPolicyType dp = RMMANGER_FACTORY.createDestinationPolicyType();
        AcksPolicyType ap = RMMANGER_FACTORY.createAcksPolicyType();
        dp.setAcksPolicy(ap);
        dp.setSequenceExpiration(DURATION_SHORT);
        manager.setDestinationPolicy(dp);

        Expires expires = new Expires();
        expires.setValue(DURATION_VERY_SHORT);

        Message message = createTestCreateSequenceMessage(expires, null);

        CreateSequenceResponseType csr = (CreateSequenceResponseType)servant.createSequence(message);

        Expires expires2 = csr.getExpires();

        assertNotNull(expires2);
        assertEquals(DURATION_VERY_SHORT, expires2.getValue());
    }

    private static Message createTestCreateSequenceMessage(Expires expires, OfferType offer) {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        exchange.setInMessage(message);
//        exchange.setOutMessage(new MessageImpl());

        message.put(Message.REQUESTOR_ROLE, Boolean.FALSE);

        AddressingProperties maps = new AddressingProperties();
        String msgId = "urn:uuid:12345-" + Math.random();
        AttributedURIType id = ContextUtils.getAttributedURI(msgId);
        maps.setMessageID(id);

        maps.setAction(ContextUtils.getAttributedURI(RM10Constants.INSTANCE.getCreateSequenceAction()));
        maps.setTo(ContextUtils.getAttributedURI(SERVICE_URL));

        maps.setReplyTo(RMUtils.createReference(DECOUPLED_URL));

        message.put(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND, maps);

        CreateSequenceType cs = new CreateSequenceType();
        cs.setAcksTo(org.apache.cxf.ws.addressing.VersionTransformer
            .convert(RMUtils.createReference(DECOUPLED_URL)));

        cs.setExpires(expires);
        cs.setOffer(offer);

        MessageContentsList contents = new MessageContentsList();
        contents.add(cs);
        message.setContent(List.class, contents);

        RMContextUtils.setProtocolVariation(message, ProtocolVariation.RM10WSA200408);

        return message;
    }

    @Test
    public void testTerminateSequence() throws SequenceFault {
        RMEndpoint rme = mock(RMEndpoint.class);
        RMManager manager = new RMManager();
        Destination destination = new Destination(rme);
        Source source = new Source(rme);
        DestinationSequence seq = mock(DestinationSequence.class);
        org.apache.cxf.ws.rm.v200702.Identifier sid = new org.apache.cxf.ws.rm.v200702.Identifier();
        sid.setValue("123");
        when(seq.getIdentifier()).thenReturn(sid);

        when(rme.getDestination()).thenReturn(destination);
        when(rme.getManager()).thenReturn(manager);
        when(rme.getSource()).thenReturn(source);

        Servant servant = new Servant(rme);

        destination.addSequence(seq, false);
        verifyTerminateSequenceDefault(servant, manager, "123", ProtocolVariation.RM10WSA200408);

        destination.addSequence(seq, false);
        verifyTerminateSequenceDefault(servant, manager, "123", ProtocolVariation.RM11WSA200508);
    }

    private static Message createTestTerminateSequenceMessage(String sidstr, ProtocolVariation protocol) {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        exchange.setInMessage(message);

        message.put(Message.REQUESTOR_ROLE, Boolean.FALSE);

        AddressingProperties maps = new AddressingProperties();
        String msgId = "urn:uuid:12345-" + Math.random();
        AttributedURIType id = ContextUtils.getAttributedURI(msgId);
        maps.setMessageID(id);

        maps.setAction(ContextUtils.getAttributedURI(RM10Constants.INSTANCE.getTerminateSequenceAction()));
        maps.setTo(ContextUtils.getAttributedURI(SERVICE_URL));

        maps.setReplyTo(RMUtils.createReference(DECOUPLED_URL));

        message.put(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND, maps);

        TerminateSequenceType ts = new TerminateSequenceType();
        Identifier sid = new Identifier();
        sid.setValue(sidstr);
        ts.setIdentifier(sid);
        Object tst = ProtocolVariation.RM10WSA200408.getWSRMNamespace().equals(protocol.getWSRMNamespace())
            ? ts : ProtocolVariation.RM10WSA200408.getCodec().convertReceivedTerminateSequence(ts);
        MessageContentsList contents = new MessageContentsList();
        contents.add(tst);
        message.setContent(List.class, contents);

        RMContextUtils.setProtocolVariation(message, protocol);

        return message;
    }

    private static Message createTestCloseSequenceMessage(String sidstr) {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        exchange.setInMessage(message);

        message.put(Message.REQUESTOR_ROLE, Boolean.FALSE);

        AddressingProperties maps = new AddressingProperties();
        String msgId = "urn:uuid:12345-" + Math.random();
        AttributedURIType id = ContextUtils.getAttributedURI(msgId);
        maps.setMessageID(id);

        maps.setAction(ContextUtils.getAttributedURI(RM10Constants.INSTANCE.getTerminateSequenceAction()));
        maps.setTo(ContextUtils.getAttributedURI(SERVICE_URL));

        maps.setReplyTo(RMUtils.createReference(DECOUPLED_URL));

        message.put(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND, maps);

        CloseSequenceType cs = new CloseSequenceType();
        org.apache.cxf.ws.rm.v200702.Identifier sid = new  org.apache.cxf.ws.rm.v200702.Identifier();
        sid.setValue(sidstr);
        cs.setIdentifier(sid);
        MessageContentsList contents = new MessageContentsList();
        contents.add(cs);
        message.setContent(List.class, contents);

        RMContextUtils.setProtocolVariation(message, ProtocolVariation.RM11WSA200508);

        return message;
    }

    private void verifyTerminateSequenceDefault(Servant servant, RMManager manager,
                                                String sidstr, ProtocolVariation protocol) throws SequenceFault {
        DestinationPolicyType dp = RMMANGER_FACTORY.createDestinationPolicyType();
        AcksPolicyType ap = RMMANGER_FACTORY.createAcksPolicyType();
        dp.setAcksPolicy(ap);

        manager.setDestinationPolicy(dp);

        Message message = createTestTerminateSequenceMessage(sidstr, protocol);

        Object tsr = servant.terminateSequence(message);

        if (ProtocolVariation.RM10WSA200408.getWSRMNamespace().equals(protocol.getWSRMNamespace())) {
            // rm 1.0
            assertNull(tsr);
        } else {
            // rm 1.1
            assertTrue(tsr instanceof TerminateSequenceResponseType);
            org.apache.cxf.ws.rm.v200702.Identifier sid = ((TerminateSequenceResponseType)tsr).getIdentifier();
            assertNotNull(sid);
            assertEquals(sidstr, sid.getValue());
        }

    }

    @Test
    public void testInvokeForCloseSequence() {
        RMEndpoint rme = mock(RMEndpoint.class);
        RMManager manager = new RMManager();
        Destination destination = new Destination(rme);
        Source source = new Source(rme);
        DestinationSequence seq = mock(DestinationSequence.class);
        org.apache.cxf.ws.rm.v200702.Identifier sid = new org.apache.cxf.ws.rm.v200702.Identifier();
        sid.setValue("123");
        when(seq.getIdentifier()).thenReturn(sid);

        when(rme.getDestination()).thenReturn(destination);
        when(rme.getManager()).thenReturn(manager);
        when(rme.getSource()).thenReturn(source);
        Message message = createTestCloseSequenceMessage(sid.getValue());

        BindingOperationInfo boi = mock(BindingOperationInfo.class);
        OperationInfo oi = mock(OperationInfo.class);
        when(boi.getOperationInfo()).thenReturn(oi);
        when(oi.getName()).thenReturn(RM11Constants.INSTANCE.getCloseSequenceOperationName());
        message.getExchange().put(BindingOperationInfo.class, boi);

        TestServant servant = new TestServant(rme);

        servant.invoke(message.getExchange(), message.getContent(List.class).get(0));
        assertTrue(servant.called);
    }

    private static class TestServant extends Servant {
        boolean called;

        TestServant(RMEndpoint rme) {
            super(rme);
        }

        @Override
        public Object closeSequence(Message message) {
            called = true;
            return null;
        }
    }
}