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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimerTask;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.RelatesToType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.manager.SequenceTerminationPolicyType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType;
import org.apache.cxf.ws.rm.v200702.Identifier;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RMManagerTest {

    private static final String MULTIPART_TYPE = "multipart/related; type=\"text/xml\";"
        + " boundary=\"uuid:74b6a245-2e17-40eb-a86c-308664e18460\"; start=\"<root."
        + "message@cxf.apache.org>\"; start-info=\"application/soap+xml\"";
    private RMManager manager;


    @Test
    public void testAccessors() {
        manager = new RMManager();
        assertNull(manager.getBus());
        assertNull(manager.getStore());
        assertNull(manager.getRetransmissionQueue());
        assertNotNull(manager.getTimer());

        Bus bus = mock(Bus.class);
        RMStore store = mock(RMStore.class);
        RetransmissionQueue queue = mock(RetransmissionQueue.class);

        manager.setBus(bus);
        manager.setStore(store);
        manager.setRetransmissionQueue(queue);
        assertSame(bus, manager.getBus());
        assertSame(store, manager.getStore());
        assertSame(queue, manager.getRetransmissionQueue());
    }

    @Test
    public void testInitialisation() {
        manager = new RMManager();
        assertNull("sourcePolicy is set.", manager.getSourcePolicy());
        assertNull("destinationPolicy is set.", manager.getDestinationPolicy());

        manager.initialise();

        RMConfiguration cfg = manager.getConfiguration();
        assertNotNull("RMConfiguration is not set.", cfg);
        assertNotNull("sourcePolicy is not set.", manager.getSourcePolicy());
        assertNotNull("destinationPolicy is not set.", manager.getDestinationPolicy());
        assertNotNull("deliveryAssirance is not set.", cfg.getDeliveryAssurance());

        assertTrue(cfg.isExponentialBackoff());
        assertEquals(3000L, cfg.getBaseRetransmissionInterval().longValue());
        assertNull(cfg.getAcknowledgementInterval());
        assertNull(cfg.getInactivityTimeout());

        SourcePolicyType sp = manager.getSourcePolicy();
        assertEquals(0L, sp.getSequenceExpiration().getTimeInMillis(new Date()));
        assertEquals(0L, sp.getOfferedSequenceExpiration().getTimeInMillis(new Date()));
        assertNull(sp.getAcksTo());
        assertTrue(sp.isIncludeOffer());
        SequenceTerminationPolicyType stp = sp.getSequenceTerminationPolicy();
        assertEquals(0, stp.getMaxRanges());
        assertEquals(0, stp.getMaxUnacknowledged());
        assertTrue(stp.isTerminateOnShutdown());
        assertEquals(0, stp.getMaxLength());

        DestinationPolicyType dp = manager.getDestinationPolicy();
        assertNotNull(dp.getAcksPolicy());
        assertEquals(dp.getAcksPolicy().getIntraMessageThreshold(), 10);
    }

    @Test
    public void testCustom() {
        Bus bus = new SpringBusFactory().createBus("org/apache/cxf/ws/rm/custom-rmmanager.xml", false);
        manager = bus.getExtension(RMManager.class);
        assertNotNull("sourcePolicy is not set.", manager.getSourcePolicy());
        assertNotNull("destinationPolicy is not set.", manager.getDestinationPolicy());

        manager.initialise();

        RMConfiguration cfg = manager.getConfiguration();
        assertNotNull("RMConfiguration is not set.", cfg);
        assertNotNull("deliveryAssurance is not set.", cfg.getDeliveryAssurance());

        assertFalse(cfg.isExponentialBackoff());
        assertEquals(10000L, cfg.getBaseRetransmissionInterval().longValue());
        assertEquals(10000L, cfg.getAcknowledgementIntervalTime());
        assertNull(cfg.getInactivityTimeout());

        SourcePolicyType sp = manager.getSourcePolicy();
        assertEquals(0L, sp.getSequenceExpiration().getTimeInMillis(new Date()));
        assertEquals(0L, sp.getOfferedSequenceExpiration().getTimeInMillis(new Date()));
        assertNull(sp.getAcksTo());
        assertTrue(sp.isIncludeOffer());
        SequenceTerminationPolicyType stp = sp.getSequenceTerminationPolicy();
        assertEquals(0, stp.getMaxRanges());
        assertEquals(0, stp.getMaxUnacknowledged());
        assertFalse(stp.isTerminateOnShutdown());
        assertEquals(0, stp.getMaxLength());

        DestinationPolicyType dp = manager.getDestinationPolicy();
        assertNotNull(dp.getAcksPolicy());
        assertEquals(dp.getAcksPolicy().getIntraMessageThreshold(), 0);
    }

    @Test
    public void testStartServer() throws NoSuchMethodException {
        manager = spy(new RMManager());
        Server s = mock(Server.class);
        Endpoint e = mock(Endpoint.class);
        when(s.getEndpoint()).thenReturn(e);

        manager.startServer(s);
        verify(manager, times(1)).recoverReliableEndpoint(e, (Conduit)null);
    }

    @Test
    public void testClientCreated() throws NoSuchMethodException {
        manager = spy(new RMManager());
        Client client = mock(Client.class);

        //none of this is called if no store

        //Endpoint endpoint = mock(Endpoint.class);
        //when(client.getEndpoint()).thenReturn(endpoint);
        //Conduit conduit = mock(Conduit.class);
        //when(client.getConduit()).thenReturn(conduit);
        //manager.recoverReliableEndpoint(endpoint, conduit);
        //whenLastCall();
        manager.clientCreated(client);
    }

    @Test
    public void testGetBindingFaultFactory() {
        SoapBinding binding = mock(SoapBinding.class);
        assertNotNull(new RMManager().getBindingFaultFactory(binding));
    }

    @Test
    public void testGetReliableEndpointServerSideCreate() throws NoSuchMethodException, RMException {
        manager = spy(new RMManager());
        manager.setReliableEndpointsMap(new HashMap<Endpoint, RMEndpoint>());
        Message message = mock(Message.class);
        Exchange exchange = mock(Exchange.class);
        when(message.getExchange()).thenReturn(exchange);
        WrappedEndpoint wre = mock(WrappedEndpoint.class);
        when(exchange.getEndpoint()).thenReturn(wre);
        EndpointInfo ei = mock(EndpointInfo.class);
        when(wre.getEndpointInfo()).thenReturn(ei);
        QName name = RM10Constants.PORT_NAME;
        when(ei.getName()).thenReturn(name);
        Endpoint e = mock(Endpoint.class);
        when(wre.getWrappedEndpoint()).thenReturn(e);
        RMEndpoint rme = mock(RMEndpoint.class);
        doReturn(rme).when(manager).createReliableEndpoint(e);
        org.apache.cxf.transport.Destination destination = mock(org.apache.cxf.transport.Destination.class);
        when(exchange.getDestination()).thenReturn(destination);
        AddressingProperties maps = mock(AddressingProperties.class);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(null);
        when(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND))
            .thenReturn(maps);
        EndpointReferenceType replyTo = RMUtils.createAnonymousReference();
        when(maps.getReplyTo()).thenReturn(replyTo);
        when(exchange.getConduit(message)).thenReturn(null);

        assertSame(rme, manager.getReliableEndpoint(message));
        verify(rme, times(1)).initialise(manager.getConfiguration(), null, replyTo, null, message);

        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getEndpoint()).thenReturn(wre);
        when(wre.getEndpointInfo()).thenReturn(ei);
        when(ei.getName()).thenReturn(name);
        when(wre.getWrappedEndpoint()).thenReturn(e);

        assertSame(rme, manager.getReliableEndpoint(message));
    }

    @Test
    public void testGetReliableEndpointClientSideCreate() throws NoSuchMethodException, RMException {
        manager = spy(new RMManager());
        manager.setReliableEndpointsMap(new HashMap<Endpoint, RMEndpoint>());
        Message message = mock(Message.class);
        Exchange exchange = mock(Exchange.class);
        when(message.getExchange()).thenReturn(exchange);
        Endpoint endpoint = mock(Endpoint.class);
        when(exchange.getEndpoint()).thenReturn(endpoint);
        EndpointInfo ei = mock(EndpointInfo.class);
        when(endpoint.getEndpointInfo()).thenReturn(ei);
        QName name = new QName("http://x.y.z/a", "GreeterPort");
        when(ei.getName()).thenReturn(name);
        RMEndpoint rme = mock(RMEndpoint.class);
        when(manager.createReliableEndpoint(endpoint))
            .thenReturn(rme);
        when(exchange.getDestination()).thenReturn(null);
        Conduit conduit = mock(Conduit.class);
        when(exchange.getConduit(message)).thenReturn(conduit);

        assertSame(rme, manager.getReliableEndpoint(message));
        verify(rme, times(1)).initialise(manager.getConfiguration(), conduit, null, null, message);

        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getEndpoint()).thenReturn(endpoint);
        when(endpoint.getEndpointInfo()).thenReturn(ei);
        when(ei.getName()).thenReturn(name);

        assertSame(rme, manager.getReliableEndpoint(message));
    }

    @Test
    public void testGetReliableEndpointExisting() throws NoSuchMethodException, RMException {
        manager = spy(new RMManager());
        manager.setReliableEndpointsMap(new HashMap<Endpoint, RMEndpoint>());
        Message message = mock(Message.class);
        Exchange exchange = mock(Exchange.class);
        when(message.getExchange()).thenReturn(exchange);

        RMConfiguration config = new RMConfiguration();
        config.setRMNamespace(RM10Constants.NAMESPACE_URI);
        config.setRM10AddressingNamespace(RM10Constants.NAMESPACE_URI);
        when(manager.getEffectiveConfiguration(message)).thenReturn(config);
        Endpoint endpoint = mock(Endpoint.class);
        when(exchange.getEndpoint()).thenReturn(endpoint);
        EndpointInfo ei = mock(EndpointInfo.class);
        when(endpoint.getEndpointInfo()).thenReturn(ei);
        QName name = new QName("http://x.y.z/a", "GreeterPort");
        when(ei.getName()).thenReturn(name);
        RMEndpoint rme = mock(RMEndpoint.class);
        manager.getReliableEndpointsMap().put(endpoint, rme);

        assertSame(rme, manager.getReliableEndpoint(message));
    }

    @Test
    public void testGetDestination() throws NoSuchMethodException, RMException {
        manager = spy(new RMManager());
        Message message = mock(Message.class);
        RMEndpoint rme = mock(RMEndpoint.class);
        doReturn(rme).when(manager).getReliableEndpoint(message);
        Destination destination = mock(Destination.class);
        when(rme.getDestination()).thenReturn(destination);

        assertSame(destination, manager.getDestination(message));

        when(manager.getReliableEndpoint(message)).thenReturn(null);
        assertNull(manager.getDestination(message));
    }

    @Test
    public void testGetSource() throws NoSuchMethodException, RMException {
        manager = spy(new RMManager());
        Message message = mock(Message.class);
        RMEndpoint rme = mock(RMEndpoint.class);
        doReturn(rme).when(manager).getReliableEndpoint(message);
        Source source = mock(Source.class);
        when(rme.getSource()).thenReturn(source);

        assertSame(source, manager.getSource(message));

        when(manager.getReliableEndpoint(message)).thenReturn(null);
        assertNull(manager.getSource(message));
    }

    @Test
    public void testGetExistingSequence() throws NoSuchMethodException, SequenceFault, RMException {
        manager = spy(new RMManager());
        Message message = mock(Message.class);
        Identifier inSid = mock(Identifier.class);

        Source source = mock(Source.class);
        doReturn(source).when(manager).getSource(message);
        SourceSequence sseq = mock(SourceSequence.class);
        when(source.getCurrent(inSid)).thenReturn(sseq);
        assertSame(sseq, manager.getSequence(inSid, message, null));
    }

    @Test
    public void testGetNewSequence() throws NoSuchMethodException, SequenceFault, RMException {
        manager = spy(new RMManager());
        Message message = mock(Message.class);
        Exchange exchange = mock(Exchange.class);
        when(message.getContextualPropertyKeys()).thenReturn(new HashSet<>());
        when(message.getExchange()).thenReturn(exchange);
        when(exchange.getOutMessage()).thenReturn(message);
        when(exchange.getInMessage()).thenReturn(null);
        when(exchange.getOutFaultMessage()).thenReturn(null);
        Conduit conduit = mock(Conduit.class);
        when(exchange.getConduit(message)).thenReturn(conduit);
        Identifier inSid = mock(Identifier.class);
        AddressingProperties maps = mock(AddressingProperties.class);
        Source source = mock(Source.class);
        doReturn(source).when(manager).getSource(message);
        when(source.getCurrent(inSid)).thenReturn(null);
        AttributedURIType uri = mock(AttributedURIType.class);
        when(maps.getTo()).thenReturn(uri);
        when(uri.getValue()).thenReturn("http://localhost:9001/TestPort");
        EndpointReferenceType epr = RMUtils.createNoneReference();
        when(maps.getReplyTo()).thenReturn(epr);
        RMEndpoint rme = mock(RMEndpoint.class);
        when(source.getReliableEndpoint()).thenReturn(rme);
        Proxy proxy = mock(Proxy.class);
        when(rme.getProxy()).thenReturn(proxy);
        CreateSequenceResponseType createResponse = mock(CreateSequenceResponseType.class);
        when(proxy.createSequence(isA(EndpointReferenceType.class),
                             isNull(RelatesToType.class),
                             eq(false),
                             isA(ProtocolVariation.class),
                             isA(Exchange.class),
                             CastUtils.cast(isA(Map.class), String.class, Object.class)))
            .thenReturn(createResponse);
        Servant servant = mock(Servant.class);
        when(rme.getServant()).thenReturn(servant);
        SourceSequence sseq = mock(SourceSequence.class);
        when(source.awaitCurrent(inSid)).thenReturn(sseq);

        assertSame(sseq, manager.getSequence(inSid, message, maps));
        verify(source, times(2)).getReliableEndpoint();
        verify(servant, times(1)).createSequenceResponse(createResponse, ProtocolVariation.RM10WSA200408);
        verify(sseq, times(1)).setTarget(isA(EndpointReferenceType.class));
    }

    @Test
    public void testShutdown() {
        Bus bus = new SpringBusFactory().createBus("org/apache/cxf/ws/rm/rmmanager.xml", false);
        manager = bus.getExtension(RMManager.class);
        Endpoint e = mock(Endpoint.class);
        RMEndpoint rme = mock(RMEndpoint.class);
        manager.getReliableEndpointsMap().put(e, rme);
        manager.getTimer(); //start the timer
        assertNotNull(manager);
        class TestTask extends TimerTask {
            public void run() {
            }
        }

        bus.shutdown(true);
        try {
            manager.getTimer().schedule(new TestTask(), 5000);
            fail("Timer has not been cancelled.");
        } catch (IllegalStateException ex) {
            // expected
        }
        verify(rme, times(1)).shutdown();
    }

    @Test
    public void testShutdownReliableEndpoint() {
        manager = new RMManager();
        Endpoint e = mock(Endpoint.class);
        RMEndpoint rme = mock(RMEndpoint.class);
        manager.shutdownReliableEndpoint(e);

        manager.getReliableEndpointsMap().put(e, rme);
        manager.shutdownReliableEndpoint(e);

        assertNull(manager.getReliableEndpointsMap().get(e));
        verify(rme, times(1)).shutdown();
    }

    @Test
    public void testRecoverReliableEndpoint() {
        manager = new RMManager();
        Endpoint endpoint = mock(Endpoint.class);
        Conduit conduit = mock(Conduit.class);

        manager.recoverReliableEndpoint(endpoint, conduit);

        RMStore store = mock(RMStore.class);
        manager.setStore(store);

        manager.recoverReliableEndpoint(endpoint, conduit);
    }

    @Test
    public void testRecoverReliableClientEndpoint() throws NoSuchMethodException, IOException {
        manager = spy(new RMManager());
        manager.setReliableEndpointsMap(new HashMap<Endpoint, RMEndpoint>());
        Endpoint endpoint = mock(Endpoint.class);
        EndpointInfo ei = mock(EndpointInfo.class);
        ServiceInfo si = mock(ServiceInfo.class);
        BindingInfo bi = mock(BindingInfo.class);
        InterfaceInfo ii = mock(InterfaceInfo.class);
        setUpEndpointForRecovery(endpoint, ei, si, bi, ii);
        Conduit conduit = mock(Conduit.class);
        var verifications = setUpRecoverReliableEndpoint(endpoint, conduit, null, null, null);
        manager.recoverReliableEndpoint(endpoint, conduit);
        verifications.run();

        setUpEndpointForRecovery(endpoint, ei, si, bi, ii);
        SourceSequence ss = mock(SourceSequence.class);
        DestinationSequence ds = mock(DestinationSequence.class);
        verifications = setUpRecoverReliableEndpoint(endpoint, conduit, ss, ds, null);
        manager.recoverReliableEndpoint(endpoint, conduit);
        verifications.run();

        reset(ss);
        setUpEndpointForRecovery(endpoint, ei, si, bi, ii);
        RMMessage m = mock(RMMessage.class);
        verifications = setUpRecoverReliableEndpoint(endpoint, conduit, ss, ds, m);
        manager.recoverReliableEndpoint(endpoint, conduit);
        verifications.run();
    }

    @Test
    public void testRecoverReliableClientEndpointWithAttachment() throws NoSuchMethodException, IOException {
        manager = spy(new RMManager());
        manager.setReliableEndpointsMap(new HashMap<Endpoint, RMEndpoint>());
        Endpoint endpoint = mock(Endpoint.class);
        EndpointInfo ei = mock(EndpointInfo.class);
        ServiceInfo si = mock(ServiceInfo.class);
        BindingInfo bi = mock(BindingInfo.class);
        InterfaceInfo ii = mock(InterfaceInfo.class);
        setUpEndpointForRecovery(endpoint, ei, si, bi, ii);
        Conduit conduit = mock(Conduit.class);
        SourceSequence ss = mock(SourceSequence.class);
        DestinationSequence ds = mock(DestinationSequence.class);
        RMMessage m1 = new RMMessage();
        InputStream fis = getClass().getResourceAsStream("persistence/SerializedRMMessage.txt");
        CachedOutputStream cos = new CachedOutputStream();
        IOUtils.copyAndCloseInput(fis, cos);
        cos.flush();
        m1.setContent(cos);
        m1.setTo("toAddress");
        m1.setMessageNumber(Long.valueOf(10));
        m1.setContentType(MULTIPART_TYPE);
        ArgumentCaptor<Message> mc = ArgumentCaptor.forClass(Message.class);

        var verification = setUpRecoverReliableEndpointWithAttachment(endpoint, conduit, ss, ds, m1, mc);
        manager.recoverReliableEndpoint(endpoint, conduit);
        verification.run();

        Message msg = mc.getValue();
        assertNotNull(msg);
        assertNotNull(msg.getExchange());
        assertSame(msg, msg.getExchange().getOutMessage());

        CachedOutputStream cos1 = (CachedOutputStream) msg.get(RMMessageConstants.SAVED_CONTENT);
        assertStartsWith(cos1.getInputStream(), "<soap:Envelope");
        assertEquals(1, msg.getAttachments().size());
    }

    Runnable setUpRecoverReliableEndpointWithAttachment(Endpoint endpoint,
                                      Conduit conduit,
                                      SourceSequence ss,
                                      DestinationSequence ds, RMMessage m,
                                      ArgumentCaptor<Message> mc) throws IOException {
        RMStore store = mock(RMStore.class);
        RetransmissionQueue oqueue = mock(RetransmissionQueue.class);
        RedeliveryQueue iqueue = mock(RedeliveryQueue.class);
        manager.setStore(store);
        manager.setRetransmissionQueue(oqueue);
        manager.setRedeliveryQueue(iqueue);

        Collection<SourceSequence> sss = new ArrayList<>();
        if (null != ss) {
            sss.add(ss);
        }
        when(store.getSourceSequences("{S}s.{P}p@cxf"))
            .thenReturn(sss);
        if (null == ss) {
            return () -> { };
        }

        Collection<DestinationSequence> dss = new ArrayList<>();
        if (null != ds) {
            dss.add(ds);
        }
        when(store.getDestinationSequences("{S}s.{P}p@cxf"))
            .thenReturn(dss);
        if (null == ds) {
            return () -> { };
        }

        Collection<RMMessage> ms = new ArrayList<>();
        if (null != m) {
            ms.add(m);
        }
        Identifier id = new Identifier();
        id.setValue("S1");
        when(ss.getIdentifier()).thenReturn(id);
        when(ss.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);
        when(store.getMessages(id, true)).thenReturn(ms);


        RMEndpoint rme = mock(RMEndpoint.class);
        when(manager.createReliableEndpoint(endpoint))
            .thenReturn(rme);
        Source source = mock(Source.class);
        when(rme.getSource()).thenReturn(source);

        Destination destination = mock(Destination.class);
        when(rme.getDestination()).thenReturn(destination);

        Service service = mock(Service.class);
        when(endpoint.getService()).thenReturn(service);
        Binding binding = mock(Binding.class);
        when(endpoint.getBinding()).thenReturn(binding);

        when(ss.isLastMessage()).thenReturn(true);
        when(ss.getCurrentMessageNr()).thenReturn(Long.valueOf(10));
        if (null == m) {
            return () -> { };
        }

        return () -> {
            verify(ss, times(null == m ? 1 : 3)).getIdentifier();
            verify(destination, times(1)).addSequence(ds, false);
            verify(oqueue, times(1)).addUnacknowledged(mc.capture());
            verify(oqueue, times(1)).start();
            verify(iqueue, times(1)).start();
        };
    }



    Endpoint setUpEndpointForRecovery(Endpoint endpoint,
                                      EndpointInfo ei,
                                    ServiceInfo si,
                                    BindingInfo bi,
                                    InterfaceInfo ii) {
        when(endpoint.getEndpointInfo()).thenReturn(ei);
        when(ei.getService()).thenReturn(si);
        when(si.getName()).thenReturn(new QName("S", "s"));
        when(ei.getName()).thenReturn(new QName("P", "p"));
        when(si.getInterface()).thenReturn(ii);
        when(ei.getBinding()).thenReturn(bi);
        return endpoint;
    }

    Runnable setUpRecoverReliableEndpoint(Endpoint endpoint,
                                      Conduit conduit,
                                      SourceSequence ss,
                                      DestinationSequence ds, RMMessage m)
                                          throws IOException  {
        RMStore store = mock(RMStore.class);
        RetransmissionQueue oqueue = mock(RetransmissionQueue.class);
        RedeliveryQueue iqueue = mock(RedeliveryQueue.class);
        manager.setStore(store);
        manager.setRetransmissionQueue(oqueue);
        manager.setRedeliveryQueue(iqueue);

        Collection<SourceSequence> sss = new ArrayList<>();
        if (null != ss) {
            sss.add(ss);
        }
        when(store.getSourceSequences("{S}s.{P}p@cxf"))
            .thenReturn(sss);
        if (null == ss) {
            return () -> { };
        }

        Collection<DestinationSequence> dss = new ArrayList<>();
        if (null != ds) {
            dss.add(ds);
        }
        when(store.getDestinationSequences("{S}s.{P}p@cxf"))
            .thenReturn(dss);
        if (null == ds) {
            return () -> { };
        }
        Collection<RMMessage> ms = new ArrayList<>();
        if (null != m) {
            ms.add(m);
        }
        Identifier id = new Identifier();
        id.setValue("S1");
        when(ss.getIdentifier()).thenReturn(id);
        when(ss.getProtocol()).thenReturn(ProtocolVariation.RM10WSA200408);
        when(store.getMessages(id, true)).thenReturn(ms);


        RMEndpoint rme = mock(RMEndpoint.class);
        when(manager.createReliableEndpoint(endpoint))
            .thenReturn(rme);
        Source source = mock(Source.class);
        when(rme.getSource()).thenReturn(source);

        Destination destination = mock(Destination.class);
        when(rme.getDestination()).thenReturn(destination);

        Service service = mock(Service.class);
        when(endpoint.getService()).thenReturn(service);
        Binding binding = mock(Binding.class);
        when(endpoint.getBinding()).thenReturn(binding);

        when(ss.isLastMessage()).thenReturn(true);
        when(ss.getCurrentMessageNr()).thenReturn(Long.valueOf(10));
        if (null == m) {
            return () -> { };
        }
        when(m.getMessageNumber()).thenReturn(Long.valueOf(10));
        if (null == conduit) {
            when(m.getTo()).thenReturn("toAddress");
        }
        InputStream is = new ByteArrayInputStream(new byte[0]);
        CachedOutputStream cos = new CachedOutputStream();
        IOUtils.copy(is, cos);
        cos.flush();
        is.close();
        when(m.getContent()).thenReturn(cos);

        return () -> {
            verify(m, times(2)).getMessageNumber();
            verify(ss, times(null == m ? 1 : 3)).getIdentifier();
            verify(destination, times(1)).addSequence(ds, false);
            verify(oqueue, times(1)).addUnacknowledged(isA(Message.class));
            verify(oqueue, times(1)).start();
            verify(iqueue, times(1)).start();
        };
    }

    @Test
    public void testDefaultSequenceIdentifierGenerator() {
        manager = new RMManager();
        assertNull(manager.getIdGenerator());
        SequenceIdentifierGenerator generator = manager.new DefaultSequenceIdentifierGenerator();
        manager.setIdGenerator(generator);
        assertSame(generator, manager.getIdGenerator());
        Identifier id1 = generator.generateSequenceIdentifier();
        assertNotNull(id1);
        assertNotNull(id1.getValue());
        Identifier id2 = generator.generateSequenceIdentifier();
        assertTrue(id1 != id2);
        assertNotEquals(id1.getValue(), id2.getValue());
    }

    // just read the begining of the input and compare it against the specified string
    private static boolean assertStartsWith(InputStream in, String starting) {
        assertNotNull(in);
        byte[] buf = new byte[starting.length()];
        try {
            in.read(buf, 0, buf.length);
            assertEquals(starting, new String(buf, StandardCharsets.UTF_8));
            in.close();
            return true;
        } catch (IOException e) {
            // ignore
        }
        return false;
    }
}