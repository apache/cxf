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

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientLifeCycleListener;
import org.apache.cxf.endpoint.ClientLifeCycleManager;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.apache.cxf.endpoint.ServerLifeCycleManager;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.RelatesToType;
import org.apache.cxf.ws.addressing.VersionTransformer;
import org.apache.cxf.ws.addressing.v200408.EndpointReferenceType;
import org.apache.cxf.ws.rm.manager.DeliveryAssuranceType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.policy.RMAssertion;
import org.apache.cxf.ws.rm.policy.RMAssertion.BaseRetransmissionInterval;
import org.apache.cxf.ws.rm.soap.RetransmissionQueueImpl;
import org.apache.cxf.ws.rm.soap.SoapFaultFactory;

/**
 * 
 */
public class RMManager implements ServerLifeCycleListener, ClientLifeCycleListener {

    private static final Logger LOG = LogUtils.getL7dLogger(RMManager.class);

    private Bus bus;
    private RMStore store;
    private SequenceIdentifierGenerator idGenerator;
    private RetransmissionQueue retransmissionQueue;
    private Map<Endpoint, RMEndpoint> reliableEndpoints = new HashMap<Endpoint, RMEndpoint>();
    private AtomicReference<Timer> timer = new AtomicReference<Timer>();
    private RMAssertion rmAssertion;
    private DeliveryAssuranceType deliveryAssurance;
    private SourcePolicyType sourcePolicy;
    private DestinationPolicyType destinationPolicy;
    
    // ServerLifeCycleListener
    
    public void startServer(Server server) {
        recoverReliableEndpoint(server.getEndpoint(), (Conduit)null);
    }

    public void stopServer(Server server) {
        shutdownReliableEndpoint(server.getEndpoint());
    }
    
    // ClientLifeCycleListener
    
    public void clientCreated(Client client) {
        if (null == store || null == retransmissionQueue) {
            return;
        }        
        String id = RMUtils.getEndpointIdentifier(client.getEndpoint());
        
        Collection<SourceSequence> sss = store.getSourceSequences(id);
        if (null == sss || 0 == sss.size()) {                        
            return;
        }
        LOG.log(Level.FINE, "Number of source sequences: {0}", sss.size());
        
        recoverReliableEndpoint(client.getEndpoint(), client.getConduit());
    }
    
    public void clientDestroyed(Client client) {
        shutdownReliableEndpoint(client.getEndpoint());
    }

    // Configuration
    
    public Bus getBus() {
        return bus;
    }

    @Resource
    public void setBus(Bus b) {
        bus = b;
    }

    @PostConstruct
    public void register() {
        if (null != bus) { 
            bus.setExtension(this, RMManager.class);
        }
    }

    public RMStore getStore() {
        return store;
    }

    public void setStore(RMStore s) {
        store = s;
    }

    public RetransmissionQueue getRetransmissionQueue() {
        return retransmissionQueue;
    }

    public void setRetransmissionQueue(RetransmissionQueue rq) {
        retransmissionQueue = rq;
    }

    public SequenceIdentifierGenerator getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(SequenceIdentifierGenerator generator) {
        idGenerator = generator;
    }

    private Timer getTimer(boolean create) {
        Timer ret = timer.get();
        if (ret == null && create) {
            Timer newt = new Timer("RMManager-Timer-" + System.identityHashCode(this), true);
            if (!timer.compareAndSet(null, newt)) {
                newt.cancel();
            }
        }
        return timer.get();
    }
    public Timer getTimer() {
        return getTimer(true);
    }

    public BindingFaultFactory getBindingFaultFactory(Binding binding) {
        return new SoapFaultFactory(binding);
    }
    
    /**  
     * @return Returns the deliveryAssurance.
     */
    public DeliveryAssuranceType getDeliveryAssurance() {
        return deliveryAssurance;
    }

    /**
     * @param deliveryAssurance The deliveryAssurance to set.
     */
    public void setDeliveryAssurance(DeliveryAssuranceType deliveryAssurance) {
        this.deliveryAssurance = deliveryAssurance;
    }

    /**
     * @return Returns the destinationPolicy.
     */
    public DestinationPolicyType getDestinationPolicy() {
        return destinationPolicy;
    }

    /**
     * @param destinationPolicy The destinationPolicy to set.
     */
    public void setDestinationPolicy(DestinationPolicyType destinationPolicy) {
        this.destinationPolicy = destinationPolicy;
    }

    /** 
     * @return Returns the rmAssertion.
     */
    public RMAssertion getRMAssertion() {
        return rmAssertion;
    }

    /**
     * @param rma The rmAssertion to set.
     */
    public void setRMAssertion(RMAssertion rma) {
        org.apache.cxf.ws.rm.policy.ObjectFactory factory = new org.apache.cxf.ws.rm.policy.ObjectFactory();
        if (null == rma) {
            rma = factory.createRMAssertion();
            rma.setExponentialBackoff(factory.createRMAssertionExponentialBackoff());
        }
        BaseRetransmissionInterval bri = rma.getBaseRetransmissionInterval();
        if (null == bri) {
            bri = factory.createRMAssertionBaseRetransmissionInterval();
            rma.setBaseRetransmissionInterval(bri);
        }
        if (null == bri.getMilliseconds()) {
            bri.setMilliseconds(new BigInteger(RetransmissionQueue.DEFAULT_BASE_RETRANSMISSION_INTERVAL));
        }

        rmAssertion = rma;
    }

    /** 
     * @return Returns the sourcePolicy.
     */
    public SourcePolicyType getSourcePolicy() {
        return sourcePolicy;
    }
    
    /**
     * @param sp The sourcePolicy to set.
     */
    public void setSourcePolicy(SourcePolicyType sp) {
        org.apache.cxf.ws.rm.manager.ObjectFactory factory = new org.apache.cxf.ws.rm.manager.ObjectFactory();
        if (null == sp) {
            sp = factory.createSourcePolicyType();
        }
        if (!sp.isSetSequenceTerminationPolicy()) {
            sp.setSequenceTerminationPolicy(factory.createSequenceTerminationPolicyType());
        }
        sourcePolicy = sp;
    }
    
    // The real stuff ...

    public synchronized RMEndpoint getReliableEndpoint(Message message) {
        Endpoint endpoint = message.getExchange().get(Endpoint.class);
        QName name = endpoint.getEndpointInfo().getName();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Getting RMEndpoint for endpoint with info: " + name);
        }
        if (name.equals(RMConstants.getPortName())) {
            WrappedEndpoint wrappedEndpoint = (WrappedEndpoint)endpoint;
            endpoint = wrappedEndpoint.getWrappedEndpoint();
        }
        RMEndpoint rme = reliableEndpoints.get(endpoint);
        if (null == rme) {
            rme = createReliableEndpoint(endpoint);
            org.apache.cxf.transport.Destination destination = message.getExchange().getDestination();
            org.apache.cxf.ws.addressing.EndpointReferenceType replyTo = null;
            if (null != destination) {
                AddressingPropertiesImpl maps = RMContextUtils.retrieveMAPs(message, false, false);
                replyTo = maps.getReplyTo();
            }
            rme.initialise(message.getExchange().getConduit(message), replyTo);
            reliableEndpoints.put(endpoint, rme);
            LOG.fine("Created new RMEndpoint.");
        }
        return rme;
    }

    public Destination getDestination(Message message) {
        RMEndpoint rme = getReliableEndpoint(message);
        if (null != rme) {
            return rme.getDestination();
        }
        return null;
    }

    public Source getSource(Message message) {
        RMEndpoint rme = getReliableEndpoint(message);
        if (null != rme) {
            return rme.getSource();
        }
        return null;
    }

    public SourceSequence getSequence(Identifier inSeqId, Message message, AddressingProperties maps)
        throws SequenceFault, RMException {

        Source source = getSource(message);
        SourceSequence seq = source.getCurrent(inSeqId);
        if (null == seq) {
            // TODO: better error handling
            org.apache.cxf.ws.addressing.EndpointReferenceType to = null;
            boolean isServer = RMContextUtils.isServerSide(message);
            EndpointReferenceType acksTo = null;
            RelatesToType relatesTo = null;
            if (isServer) {

                AddressingPropertiesImpl inMaps = RMContextUtils.retrieveMAPs(message, false, false);
                inMaps.exposeAs(VersionTransformer.Names200408.WSA_NAMESPACE_NAME);
                acksTo = RMUtils.createReference2004(inMaps.getTo().getValue());
                to = inMaps.getReplyTo();
                source.getReliableEndpoint().getServant().setUnattachedIdentifier(inSeqId);
                relatesTo = (new org.apache.cxf.ws.addressing.ObjectFactory()).createRelatesToType();
                Destination destination = getDestination(message);
                DestinationSequence inSeq = inSeqId == null ? null : destination.getSequence(inSeqId);
                relatesTo.setValue(inSeq != null ? inSeq.getCorrelationID() : null);

            } else {
                to = RMUtils.createReference(maps.getTo().getValue());
                acksTo = VersionTransformer.convert(maps.getReplyTo());
                if (RMConstants.getNoneAddress().equals(acksTo.getAddress().getValue())) {
                    org.apache.cxf.transport.Destination dest = message.getExchange().getConduit(message)
                        .getBackChannel();
                    if (null == dest) {
                        acksTo = RMUtils.createAnonymousReference2004();
                    } else {
                        acksTo = VersionTransformer.convert(dest.getAddress());
                    }
                }
            }

            Proxy proxy = source.getReliableEndpoint().getProxy();
            CreateSequenceResponseType createResponse = proxy.createSequence(acksTo, relatesTo, isServer);
            if (!isServer) {
                Servant servant = source.getReliableEndpoint().getServant();
                servant.createSequenceResponse(createResponse);
            }

            seq = source.awaitCurrent(inSeqId);
            seq.setTarget(to);
        }

        return seq;
    }    

    @PreDestroy
    public void shutdown() {
        // shutdown remaining endpoints 
        
        LOG.log(Level.FINE, "Shutting down RMManager with {0} remaining endpoints.",
                reliableEndpoints.size());
        for (RMEndpoint rme : reliableEndpoints.values()) {            
            rme.shutdown();
        }

        // remove references to timer tasks cancelled above to make them
        // eligible for garbage collection
        Timer t = getTimer(false);
        if (t != null) {
            t.purge();
            t.cancel();
        }
    }
    
    synchronized void shutdownReliableEndpoint(Endpoint e) {
        RMEndpoint rme = reliableEndpoints.get(e);
        if (null == rme) {
            // not interested
            return;
        }
        
        rme.shutdown();        
        
        // remove references to timer tasks cancelled above to make them
        // eligible for garbage collection
        Timer t = getTimer(false);
        if (t != null) {
            t.purge();
        }
        
        reliableEndpoints.remove(e);
    }
    
    void recoverReliableEndpoint(Endpoint endpoint, Conduit conduit) {
        if (null == store || null == retransmissionQueue) {
            return;
        }        
        
        String id = RMUtils.getEndpointIdentifier(endpoint);
        
        Collection<SourceSequence> sss = store.getSourceSequences(id);
        if (null == sss || 0 == sss.size()) {                        
            return;
        }
        LOG.log(Level.FINE, "Number of source sequences: {0}", sss.size());
        
        RMEndpoint rme = null;
        for (SourceSequence ss : sss) {            
 
            Collection<RMMessage> ms = store.getMessages(ss.getIdentifier(), true);
            if (null == ms || 0 == ms.size()) {
                continue;
            }
            LOG.log(Level.FINE, "Number of messages in sequence: {0}", ms.size());
            
            if (null == rme) {
                LOG.log(Level.FINE, "Recovering {0} endpoint with id: {1}",
                        new Object[] {null == conduit ? "client" : "server", id});
                rme = createReliableEndpoint(endpoint);
                rme.initialise(conduit, null);
                reliableEndpoints.put(endpoint, rme);
            }
            rme.getSource().addSequence(ss, false);
            
            for (RMMessage m : ms) {                
                
                Message message = new MessageImpl();
                Exchange exchange = new ExchangeImpl();
                message.setExchange(exchange);
                if (null != conduit) {
                    exchange.setConduit(conduit);
                    message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
                }
                exchange.put(Endpoint.class, endpoint);
                exchange.put(Service.class, endpoint.getService());
                if (endpoint.getEndpointInfo().getService() != null) {
                    exchange.put(ServiceInfo.class, endpoint.getEndpointInfo().getService());
                    exchange.put(InterfaceInfo.class, endpoint.getEndpointInfo().getService().getInterface());
                }
                exchange.put(Binding.class, endpoint.getBinding());
                exchange.put(BindingInfo.class, endpoint.getEndpointInfo().getBinding());
                exchange.put(Bus.class, bus);
                
                SequenceType st = RMUtils.getWSRMFactory().createSequenceType();
                st.setIdentifier(ss.getIdentifier());
                st.setMessageNumber(m.getMessageNumber());
                if (ss.isLastMessage() && ss.getCurrentMessageNr().equals(m.getMessageNumber())) {
                    st.setLastMessage(RMUtils.getWSRMFactory().createSequenceTypeLastMessage());
                }
                RMProperties rmps = new RMProperties();
                rmps.setSequence(st);
                RMContextUtils.storeRMProperties(message, rmps, true);                
                if (null == conduit) {
                    String to = m.getTo();
                    AddressingProperties maps = new AddressingPropertiesImpl();
                    maps.setTo(RMUtils.createReference(to));
                    RMContextUtils.storeMAPs(maps, message, true, false);
                }
                                    
                message.setContent(byte[].class, m.getContent());
                          
                retransmissionQueue.addUnacknowledged(message);
            }            
        }
        retransmissionQueue.start();
        
    }
    
    RMEndpoint createReliableEndpoint(Endpoint endpoint) {
        return new RMEndpoint(this, endpoint);
    }  
    
    public void init(Bus b) {
        setBus(b);
        initialise();
        registerListeners();
    }
    
    @PostConstruct
    void initialise() {
        if (null == rmAssertion) {
            setRMAssertion(null);
        }
        org.apache.cxf.ws.rm.manager.ObjectFactory factory = new org.apache.cxf.ws.rm.manager.ObjectFactory();
        DeliveryAssuranceType da = factory.createDeliveryAssuranceType();
        if (null == deliveryAssurance) {
            da.setAtLeastOnce(factory.createDeliveryAssuranceTypeAtLeastOnce());
            setDeliveryAssurance(da);
        } else if (deliveryAssurance.isSetExactlyOnce()) {
            if (!deliveryAssurance.isSetAtMostOnce()) {
                deliveryAssurance.setAtMostOnce(
                    factory.createDeliveryAssuranceTypeAtMostOnce());
            }
            if (!deliveryAssurance.isSetAtLeastOnce()) {
                deliveryAssurance.setAtLeastOnce(
                    factory.createDeliveryAssuranceTypeAtLeastOnce());
            }
        }
        if (null == sourcePolicy) {
            setSourcePolicy(null);

        }       
        if (null == destinationPolicy) {
            DestinationPolicyType dp = factory.createDestinationPolicyType();
            dp.setAcksPolicy(factory.createAcksPolicyType());
            setDestinationPolicy(dp);
        }
        if (null == retransmissionQueue) {
            retransmissionQueue = new RetransmissionQueueImpl(this);
        }
        if (null == idGenerator) {
            idGenerator = new DefaultSequenceIdentifierGenerator();
        }
    }
    
    @PostConstruct
    void registerListeners() {
        if (null == bus) {
            return;
        }
        ServerLifeCycleManager slm = bus.getExtension(ServerLifeCycleManager.class);
        if (null != slm) {
            slm.registerListener(this);
        }
        ClientLifeCycleManager clm = bus.getExtension(ClientLifeCycleManager.class);
        if (null != clm) {
            clm.registerListener(this);
        }
    }

   
    Map<Endpoint, RMEndpoint> getReliableEndpointsMap() {
        return reliableEndpoints;
    }
    
    void setReliableEndpointsMap(Map<Endpoint, RMEndpoint> map) {
        reliableEndpoints = map;
    }

    class DefaultSequenceIdentifierGenerator implements SequenceIdentifierGenerator {

        public Identifier generateSequenceIdentifier() {
            String sequenceID = RMContextUtils.generateUUID();
            Identifier sid = RMUtils.getWSRMFactory().createIdentifier();
            sid.setValue(sequenceID);
            return sid;
        }
    }

    

}
