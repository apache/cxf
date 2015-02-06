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
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RMManagerTest extends Assert {
    
    private MyControl control;
    private RMManager manager;
    
    @Before
    public void setUp() {
        // control = EasyMock.createNiceControl();
        control = new MyControl();
    }
   
    @Test
    public void testAccessors() {
        manager = new RMManager();
        assertNull(manager.getBus());
        assertNull(manager.getStore());
        assertNull(manager.getRetransmissionQueue());
        assertNotNull(manager.getTimer());

        Bus bus = control.createMock(Bus.class);
        RMStore store = control.createMock(RMStore.class);
        RetransmissionQueue queue = control.createMock(RetransmissionQueue.class);
        
        manager.setBus(bus);
        manager.setStore(store);
        manager.setRetransmissionQueue(queue);
        assertSame(bus, manager.getBus());
        assertSame(store, manager.getStore());
        assertSame(queue, manager.getRetransmissionQueue());
        control.replay();
        control.reset();
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
        Method m = RMManager.class.getDeclaredMethod("recoverReliableEndpoint",
            new Class[] {Endpoint.class, Conduit.class});
        manager = control.createMock(RMManager.class, new Method[] {m});
        Server s = control.createMock(Server.class);
        Endpoint e = control.createMock(Endpoint.class);
        EasyMock.expect(s.getEndpoint()).andReturn(e);
        manager.recoverReliableEndpoint(e, (Conduit)null);
        EasyMock.expectLastCall();
        control.replay();
        manager.startServer(s);
        control.verify();
    }

    @Test
    public void testClientCreated() throws NoSuchMethodException {
        Method m = RMManager.class.getDeclaredMethod("recoverReliableEndpoint",
            new Class[] {Endpoint.class, Conduit.class});
        manager = control.createMock(RMManager.class, new Method[] {m});
        Client client = control.createMock(Client.class);
        
        //none of this is called if no store
        
        //Endpoint endpoint = control.createMock(Endpoint.class);
        //EasyMock.expect(client.getEndpoint()).andReturn(endpoint);
        //Conduit conduit = control.createMock(Conduit.class);
        //EasyMock.expect(client.getConduit()).andReturn(conduit).anyTimes();
        //manager.recoverReliableEndpoint(endpoint, conduit);
        //EasyMock.expectLastCall();
        control.replay();
        manager.clientCreated(client);
        control.verify();
    }
    
    @Test
    public void testGetBindingFaultFactory() {
        SoapBinding binding = control.createMock(SoapBinding.class);
        assertNotNull(new RMManager().getBindingFaultFactory(binding));
    }
    
    @Test
    public void testGetReliableEndpointServerSideCreate() throws NoSuchMethodException, RMException {
        Method m1 = RMManager.class.getDeclaredMethod("createReliableEndpoint", 
            new Class[] {Endpoint.class});
        manager = control.createMock(RMManager.class, new Method[] {m1});
        manager.setReliableEndpointsMap(new HashMap<Endpoint, RMEndpoint>());
        Message message = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        WrappedEndpoint wre = control.createMock(WrappedEndpoint.class);
        EasyMock.expect(exchange.get(Endpoint.class)).andReturn(wre).anyTimes();
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        EasyMock.expect(wre.getEndpointInfo()).andReturn(ei).anyTimes();
        QName name = RM10Constants.PORT_NAME;
        EasyMock.expect(ei.getName()).andReturn(name).anyTimes();
        Endpoint e = control.createMock(Endpoint.class);
        EasyMock.expect(wre.getWrappedEndpoint()).andReturn(e).anyTimes();        
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(manager.createReliableEndpoint(e))
            .andReturn(rme).anyTimes();
        org.apache.cxf.transport.Destination destination = control
            .createMock(org.apache.cxf.transport.Destination.class);
        EasyMock.expect(exchange.getDestination()).andReturn(destination).anyTimes();
        AddressingProperties maps = control.createMock(AddressingProperties.class);
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(null);
        EasyMock.expect(message.get(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND))
            .andReturn(maps).anyTimes();
        EndpointReferenceType replyTo = RMUtils.createAnonymousReference();
        EasyMock.expect(maps.getReplyTo()).andReturn(replyTo).anyTimes();
        EasyMock.expect(exchange.getConduit(message)).andReturn(null).anyTimes();
        rme.initialise(manager.getConfiguration(), null, replyTo, null, message);
        EasyMock.expectLastCall();

        control.replay();
        assertSame(rme, manager.getReliableEndpoint(message));
        control.verify();

        control.reset();
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(exchange.get(Endpoint.class)).andReturn(wre);
        EasyMock.expect(wre.getEndpointInfo()).andReturn(ei);
        EasyMock.expect(ei.getName()).andReturn(name);
        EasyMock.expect(wre.getWrappedEndpoint()).andReturn(e); 

        control.replay();
        assertSame(rme, manager.getReliableEndpoint(message));
        control.verify();
    }
    
    @Test
    public void testGetReliableEndpointClientSideCreate() throws NoSuchMethodException, RMException {
        Method m1 = RMManager.class.getDeclaredMethod("createReliableEndpoint", 
            new Class[] {Endpoint.class});
        manager = control.createMock(RMManager.class, new Method[] {m1});
        manager.setReliableEndpointsMap(new HashMap<Endpoint, RMEndpoint>());
        Message message = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        Endpoint endpoint = control.createMock(Endpoint.class);
        EasyMock.expect(exchange.get(Endpoint.class)).andReturn(endpoint);
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(ei);
        QName name = new QName("http://x.y.z/a", "GreeterPort");
        EasyMock.expect(ei.getName()).andReturn(name);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(manager.createReliableEndpoint(endpoint))
            .andReturn(rme);
        EasyMock.expect(exchange.getDestination()).andReturn(null);
        Conduit conduit = control.createMock(Conduit.class);
        EasyMock.expect(exchange.getConduit(message)).andReturn(conduit);
        rme.initialise(manager.getConfiguration(), conduit, null, null, message);
        EasyMock.expectLastCall();

        control.replay();
        assertSame(rme, manager.getReliableEndpoint(message));
        control.verify();

        control.reset();
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        EasyMock.expect(exchange.get(Endpoint.class)).andReturn(endpoint);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(ei);
        EasyMock.expect(ei.getName()).andReturn(name);
    
        control.replay();
        assertSame(rme, manager.getReliableEndpoint(message));
        control.verify();
    }
    
    @Test
    public void testGetReliableEndpointExisting() throws NoSuchMethodException, RMException {
        Method m1 = RMManager.class.getDeclaredMethod("createReliableEndpoint", 
            new Class[] {Endpoint.class});
        manager = control.createMock(RMManager.class, new Method[] {m1});
        manager.setReliableEndpointsMap(new HashMap<Endpoint, RMEndpoint>());
        Message message = control.createMock(Message.class);
        RMConfiguration config = new RMConfiguration();
        config.setRMNamespace(RM10Constants.NAMESPACE_URI);
        config.setRM10AddressingNamespace(RM10Constants.NAMESPACE_URI);
        EasyMock.expect(manager.getEffectiveConfiguration(message)).andReturn(config).anyTimes();
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        Endpoint endpoint = control.createMock(Endpoint.class);
        EasyMock.expect(exchange.get(Endpoint.class)).andReturn(endpoint);
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(ei);
        QName name = new QName("http://x.y.z/a", "GreeterPort");
        EasyMock.expect(ei.getName()).andReturn(name);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        manager.getReliableEndpointsMap().put(endpoint, rme);

        control.replay();
        assertSame(rme, manager.getReliableEndpoint(message));
        control.verify();
    }
    
    @Test
    public void testGetDestination() throws NoSuchMethodException, RMException {
        Method  m = RMManager.class
            .getDeclaredMethod("getReliableEndpoint", new Class[] {Message.class});        
        manager = control.createMock(RMManager.class, new Method[] {m});
        Message message = control.createMock(Message.class);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(manager.getReliableEndpoint(message)).andReturn(rme);    
        Destination destination = control.createMock(Destination.class);
        EasyMock.expect(rme.getDestination()).andReturn(destination);
       
        control.replay();
        assertSame(destination, manager.getDestination(message));
        control.verify();
        
        control.reset();
        EasyMock.expect(manager.getReliableEndpoint(message)).andReturn(null);
        control.replay();
        assertNull(manager.getDestination(message));
        control.verify();        
    }
    
    @Test
    public void testGetSource() throws NoSuchMethodException, RMException {
        Method m = RMManager.class
            .getDeclaredMethod("getReliableEndpoint", new Class[] {Message.class});
        manager = control.createMock(RMManager.class, new Method[] {m});
        Message message = control.createMock(Message.class);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(manager.getReliableEndpoint(message)).andReturn(rme);
        Source source = control.createMock(Source.class);
        EasyMock.expect(rme.getSource()).andReturn(source);

        control.replay();
        assertSame(source, manager.getSource(message));
        control.verify();

        control.reset();
        EasyMock.expect(manager.getReliableEndpoint(message)).andReturn(null);
        control.replay();
        assertNull(manager.getSource(message));
        control.verify();
    }
     
    @Test
    public void testGetExistingSequence() throws NoSuchMethodException, SequenceFault, RMException {
        Method m = RMManager.class.getDeclaredMethod("getSource", new Class[] {Message.class});
        manager = control.createMock(RMManager.class, new Method[] {m});
        Message message = control.createMock(Message.class);
        Identifier inSid = control.createMock(Identifier.class);
        
        Source source = control.createMock(Source.class);
        EasyMock.expect(manager.getSource(message)).andReturn(source);
        SourceSequence sseq = control.createMock(SourceSequence.class);
        EasyMock.expect(source.getCurrent(inSid)).andReturn(sseq);
        control.replay();
        assertSame(sseq, manager.getSequence(inSid, message, null));
        control.verify();
    }
    
    @Test
    public void testGetNewSequence() throws NoSuchMethodException, SequenceFault, RMException {
        Method m = RMManager.class.getDeclaredMethod("getSource", new Class[] {Message.class});
        manager = control.createMock(RMManager.class, new Method[] {m});
        Message message = control.createMock(Message.class);
        Exchange exchange = control.createMock(Exchange.class);
        EasyMock.expect(message.getContextualPropertyKeys()).andReturn(new HashSet<String>()).anyTimes();
        EasyMock.expect(message.getExchange()).andReturn(exchange).anyTimes();
        EasyMock.expect(exchange.getOutMessage()).andReturn(message).anyTimes();
        EasyMock.expect(exchange.getInMessage()).andReturn(null).anyTimes();
        EasyMock.expect(exchange.getOutFaultMessage()).andReturn(null).anyTimes();
        Conduit conduit = control.createMock(Conduit.class);
        EasyMock.expect(exchange.getConduit(message)).andReturn(conduit).anyTimes();
        Identifier inSid = control.createMock(Identifier.class);        
        AddressingProperties maps = control.createMock(AddressingProperties.class);
        Source source = control.createMock(Source.class);
        EasyMock.expect(manager.getSource(message)).andReturn(source);
        EasyMock.expect(source.getCurrent(inSid)).andReturn(null);
        AttributedURIType uri = control.createMock(AttributedURIType.class);
        EasyMock.expect(maps.getTo()).andReturn(uri);
        EasyMock.expect(uri.getValue()).andReturn("http://localhost:9001/TestPort");
        EndpointReferenceType epr = RMUtils.createNoneReference();
        EasyMock.expect(maps.getReplyTo()).andReturn(epr);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(source.getReliableEndpoint()).andReturn(rme).times(2);
        Proxy proxy = control.createMock(Proxy.class);
        EasyMock.expect(rme.getProxy()).andReturn(proxy);
        CreateSequenceResponseType createResponse = control.createMock(CreateSequenceResponseType.class);
        proxy.createSequence(EasyMock.isA(EndpointReferenceType.class),
                             (RelatesToType)EasyMock.isNull(),
                             EasyMock.eq(false),
                             EasyMock.isA(ProtocolVariation.class),
                             EasyMock.isA(Exchange.class), 
                             CastUtils.cast(EasyMock.isA(HashMap.class), String.class, Object.class));
        EasyMock.expectLastCall().andReturn(createResponse);
        Servant servant = control.createMock(Servant.class);
        EasyMock.expect(rme.getServant()).andReturn(servant);
        servant.createSequenceResponse(createResponse, ProtocolVariation.RM10WSA200408);
        EasyMock.expectLastCall();
        SourceSequence sseq = control.createMock(SourceSequence.class);
        EasyMock.expect(source.awaitCurrent(inSid)).andReturn(sseq);
        sseq.setTarget(EasyMock.isA(EndpointReferenceType.class));
        EasyMock.expectLastCall();
        
        control.replay();
        assertSame(sseq, manager.getSequence(inSid, message, maps));
        control.verify();
    }
    
    @Test
    public void testShutdown() {
        Bus bus = new SpringBusFactory().createBus("org/apache/cxf/ws/rm/rmmanager.xml", false);
        manager = bus.getExtension(RMManager.class);        
        Endpoint e = control.createMock(Endpoint.class);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        manager.getReliableEndpointsMap().put(e, rme);
        manager.getTimer(); //start the timer
        rme.shutdown();
        EasyMock.expectLastCall();
        assertNotNull(manager);
        class TestTask extends TimerTask {
            public void run() {
            }
        }
        control.replay();
        bus.shutdown(true);
        try {
            manager.getTimer().schedule(new TestTask(), 5000); 
            fail("Timer has not been cancelled.");
        } catch (IllegalStateException ex) {
            // expected
        }
        control.verify();
    }
    
    @Test
    public void testShutdownReliableEndpoint() {
        manager = new RMManager();
        Endpoint e = control.createMock(Endpoint.class);
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        control.replay();
        manager.shutdownReliableEndpoint(e);
        control.verify();
        
        control.reset();
        manager.getReliableEndpointsMap().put(e, rme);
        rme.shutdown();
        EasyMock.expectLastCall();
        control.replay();
        manager.shutdownReliableEndpoint(e);
        assertNull(manager.getReliableEndpointsMap().get(e));  
        control.verify();
    }
    
    @Test
    public void testRecoverReliableEndpoint() {
        manager = new RMManager();
        Endpoint endpoint = control.createMock(Endpoint.class);
        Conduit conduit = control.createMock(Conduit.class);
                
        control.replay();
        manager.recoverReliableEndpoint(endpoint, conduit);
        control.verify();
        
        control.reset();
        
        RMStore store = control.createMock(RMStore.class);
        manager.setStore(store);
       
        control.replay();
        manager.recoverReliableEndpoint(endpoint, conduit);
        control.verify();           
    }
    
    @Test
    public void testRecoverReliableClientEndpoint() throws NoSuchMethodException {
        Method method = RMManager.class.getDeclaredMethod("createReliableEndpoint", 
            new Class[] {Endpoint.class});
        manager = control.createMock(RMManager.class, new Method[] {method});
        manager.setReliableEndpointsMap(new HashMap<Endpoint, RMEndpoint>());
        Endpoint endpoint = control.createMock(Endpoint.class);
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        ServiceInfo si = control.createMock(ServiceInfo.class);  
        BindingInfo bi = control.createMock(BindingInfo.class);
        InterfaceInfo ii = control.createMock(InterfaceInfo.class);
        setUpEndpointForRecovery(endpoint, ei, si, bi, ii);          
        Conduit conduit = control.createMock(Conduit.class);        
        setUpRecoverReliableEndpoint(endpoint, conduit, null, null, null, null);
        control.replay();
        manager.recoverReliableEndpoint(endpoint, conduit);
        control.verify();
        
        control.reset();
        setUpEndpointForRecovery(endpoint, ei, si, bi, ii);
        SourceSequence ss = control.createMock(SourceSequence.class);
        DestinationSequence ds = control.createMock(DestinationSequence.class);
        setUpRecoverReliableEndpoint(endpoint, conduit, ss, ds, null, null);
        control.replay();
        manager.recoverReliableEndpoint(endpoint, conduit);
        control.verify();
        
        control.reset();
        setUpEndpointForRecovery(endpoint, ei, si, bi, ii);  
        RMMessage m = control.createMock(RMMessage.class);
        Capture<Message> mc = Capture.newInstance();
        setUpRecoverReliableEndpoint(endpoint, conduit, ss, ds, m, mc);        
        control.replay();
        manager.recoverReliableEndpoint(endpoint, conduit);
        control.verify();
        
        Message msg = mc.getValue();
        assertNotNull(msg);
        assertNotNull(msg.getExchange());
        assertSame(msg, msg.getExchange().getOutMessage());
    }
    
    Endpoint setUpEndpointForRecovery(Endpoint endpoint, 
                                      EndpointInfo ei, 
                                    ServiceInfo si,
                                    BindingInfo bi,
                                    InterfaceInfo ii) {   
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(ei).anyTimes();     
        EasyMock.expect(ei.getService()).andReturn(si).anyTimes();
        EasyMock.expect(si.getName()).andReturn(new QName("S", "s")).anyTimes();
        EasyMock.expect(ei.getName()).andReturn(new QName("P", "p")).anyTimes();
        EasyMock.expect(si.getInterface()).andReturn(ii).anyTimes();
        return endpoint;
    }
    
    void setUpRecoverReliableEndpoint(Endpoint endpoint,
                                      Conduit conduit, 
                                      SourceSequence ss, 
                                      DestinationSequence ds, RMMessage m, Capture<Message> mc)  {                
        RMStore store = control.createMock(RMStore.class);
        RetransmissionQueue queue = control.createMock(RetransmissionQueue.class);
        manager.setStore(store);
        manager.setRetransmissionQueue(queue);
        
        Collection<SourceSequence> sss = new ArrayList<SourceSequence>();
        if (null != ss) {
            sss.add(ss);            
        }
        EasyMock.expect(store.getSourceSequences("{S}s.{P}p@cxf"))
            .andReturn(sss);
        if (null == ss) {
            return;
        }         
        
        Collection<DestinationSequence> dss = new ArrayList<DestinationSequence>();
        if (null != ds) {
            dss.add(ds);            
        }
        EasyMock.expect(store.getDestinationSequences("{S}s.{P}p@cxf"))
            .andReturn(dss);
        if (null == ds) {
            return;
        }
        Collection<RMMessage> ms = new ArrayList<RMMessage>();
        if (null != m) {
            ms.add(m);
        }
        Identifier id = new Identifier();
        id.setValue("S1");
        EasyMock.expect(ss.getIdentifier()).andReturn(id).times(null == m ? 1 : 2);
        EasyMock.expect(ss.getProtocol()).andReturn(ProtocolVariation.RM10WSA200408).anyTimes();
        EasyMock.expect(store.getMessages(id, true)).andReturn(ms);
        
        
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        EasyMock.expect(manager.createReliableEndpoint(endpoint))
            .andReturn(rme);
        Source source = control.createMock(Source.class);
        EasyMock.expect(rme.getSource()).andReturn(source).anyTimes();
                
        Destination destination = control.createMock(Destination.class);
        EasyMock.expect(rme.getDestination()).andReturn(destination);
        destination.addSequence(ds, false);
        EasyMock.expectLastCall();
        
        Service service = control.createMock(Service.class);
        EasyMock.expect(endpoint.getService()).andReturn(service).anyTimes();
        Binding binding = control.createMock(Binding.class);
        EasyMock.expect(endpoint.getBinding()).andReturn(binding).anyTimes();
       
        EasyMock.expect(ss.isLastMessage()).andReturn(true).anyTimes();
        EasyMock.expect(ss.getCurrentMessageNr()).andReturn(new Long(10)).anyTimes();
        if (null == m) {
            return;
        }
        EasyMock.expect(m.getMessageNumber()).andReturn(new Long(10)).times(2);
        if (null == conduit) {
            EasyMock.expect(m.getTo()).andReturn("toAddress");
        }
        InputStream is = new ByteArrayInputStream(new byte[0]);
        EasyMock.expect(m.getContent()).andReturn(is).anyTimes();

        if (mc != null) {
            queue.addUnacknowledged(EasyMock.capture(mc));
        } else {
            queue.addUnacknowledged(EasyMock.isA(Message.class));
        }
        EasyMock.expectLastCall();
        queue.start();
        EasyMock.expectLastCall();
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
        assertTrue(!id1.getValue().equals(id2.getValue()));     
        control.replay();
    }   
    
    class MyControl {
        private IMocksControl c;
        private List<Object> mocks;
        
        MyControl() {
            c = EasyMock.createNiceControl();
            mocks = new ArrayList<Object>();
        }
        
        void replay() {
            c.replay();
        }
        
        void reset() {
            c.reset();
        }
        
        void verify() {
            c.verify();
        }
        
        <T> T createMock(Class<T> cls) {
            T mock = c.createMock(cls);
            mocks.add(mock);
            return mock;
        }
        
        <T> T createMock(Class<T> cls, Method[] m) {
            T mock = EasyMock.createMockBuilder(cls).addMockedMethods(m).createMock(c);
            mocks.add(mock);
            return mock;
        }
        
         
    }
} 
