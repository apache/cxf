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
import javax.management.JMException;
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
import org.apache.cxf.management.InstrumentationManager;
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
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.RelatesToType;
import org.apache.cxf.ws.rm.manager.DeliveryAssuranceType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.manager.RM10AddressingNamespaceType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.soap.RetransmissionQueueImpl;
import org.apache.cxf.ws.rm.soap.SoapFaultFactory;
import org.apache.cxf.ws.rm.v200702.CloseSequenceType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceType;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;
import org.apache.cxf.ws.rmp.v200502.RMAssertion.BaseRetransmissionInterval;
import org.apache.cxf.ws.rmp.v200502.RMAssertion.ExponentialBackoff;

/**
 * 
 */
public class RMManager {
    
    /**
     * Message contextual property giving WS-ReliableMessaging namespace.
     */
    public static final String WSRM_VERSION_PROPERTY = "org.apache.cxf.ws.rm.namespace";
    
    /**
     * Message contextual property giving addressing namespace to be used by WS-RM implementation.
     */
    public static final String WSRM_WSA_VERSION_PROPERTY = "org.apache.cxf.ws.rm.wsa-namespace";

    /**
     * Message contextual property giving the last message.
     */
    public static final String WSRM_LAST_MESSAGE_PROPERTY = 
        "org.apache.cxf.ws.rm.last-message";

    private static final Logger LOG = LogUtils.getL7dLogger(RMManager.class);


    private Bus bus;
    private RMStore store;
    private SequenceIdentifierGenerator idGenerator;
    private RetransmissionQueue retransmissionQueue;
    private Map<ProtocolVariation, Map<Endpoint, RMEndpoint>> endpointMaps;
    private AtomicReference<Timer> timer = new AtomicReference<Timer>();
    private RMAssertion rmAssertion;
    private DeliveryAssuranceType deliveryAssurance;
    private SourcePolicyType sourcePolicy;
    private DestinationPolicyType destinationPolicy;
    private InstrumentationManager instrumentationManager;
    private ManagedRMManager managedManager;
    private String rmNamespace = RM10Constants.NAMESPACE_URI;
    private RM10AddressingNamespaceType rm10AddressingNamespace;
    
    public RMManager() {
        setEndpointMaps(new HashMap<ProtocolVariation, Map<Endpoint, RMEndpoint>>());
    }
    
    // ServerLifeCycleListener
    
    public void startServer(Server server) {
        for (ProtocolVariation protocol : ProtocolVariation.values()) {
            recoverReliableEndpoint(server.getEndpoint(), (Conduit)null, protocol);
        }
    }

    public void stopServer(Server server) {
        shutdownReliableEndpoint(server.getEndpoint());
    }
    
    // ClientLifeCycleListener
    
    public void clientCreated(Client client) {
        if (null == store || null == retransmissionQueue) {
            return;
        }        
        String id = RMUtils.getEndpointIdentifier(client.getEndpoint(), getBus());
        ProtocolVariation protocol = getConfiguredProtocol();
        Collection<SourceSequence> sss = store.getSourceSequences(id, protocol);
        if (null == sss || 0 == sss.size()) {                        
            return;
        }
        LOG.log(Level.FINE, "Number of source sequences: {0}", sss.size());
        recoverReliableEndpoint(client.getEndpoint(), client.getConduit(), protocol);
    }

    private ProtocolVariation getConfiguredProtocol() {
        String addrns = rm10AddressingNamespace == null ? null : rm10AddressingNamespace.getUri();
        return ProtocolVariation.findVariant(getRMNamespace(), addrns);
    }
    
    public void clientDestroyed(Client client) {
        shutdownReliableEndpoint(client.getEndpoint());
    }

    // Configuration

    public String getRMNamespace() {
        return rmNamespace;
    }

    public void setRMNamespace(String uri) {
        rmNamespace = uri;
    }

    public RM10AddressingNamespaceType getRMAddressingNamespace() {
        return rm10AddressingNamespace;
    }

    public void setRM10AddressingNamespace(RM10AddressingNamespaceType addrns) {
        rm10AddressingNamespace = addrns;
    }
    
    public Bus getBus() {
        return bus;
    }

    @Resource
    public void setBus(Bus b) {
        bus = b;
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
        if (null == rma) {
            rma = new RMAssertion();
            rma.setExponentialBackoff(new ExponentialBackoff());
        }
        BaseRetransmissionInterval bri = rma.getBaseRetransmissionInterval();
        if (null == bri) {
            bri = new BaseRetransmissionInterval();
            rma.setBaseRetransmissionInterval(bri);
        }
        if (null == bri.getMilliseconds()) {
            bri.setMilliseconds(new Long(RetransmissionQueue.DEFAULT_BASE_RETRANSMISSION_INTERVAL));
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
        if (sp.getSequenceTerminationPolicy() == null) {
            sp.setSequenceTerminationPolicy(factory.createSequenceTerminationPolicyType());
        }
        sourcePolicy = sp;
    }
    
    // The real stuff ...

    public synchronized RMEndpoint getReliableEndpoint(Message message) throws RMException {
        Endpoint endpoint = message.getExchange().get(Endpoint.class);
        QName name = endpoint.getEndpointInfo().getName();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Getting RMEndpoint for endpoint with info: " + name);
        }
        if (name.equals(RM10Constants.PORT_NAME) || name.equals(RM11Constants.PORT_NAME)) {
            WrappedEndpoint wrappedEndpoint = (WrappedEndpoint)endpoint;
            endpoint = wrappedEndpoint.getWrappedEndpoint();
        }
        String rmUri = getRMNamespace(message);
        String addrUri = getAddressingNamespace(message);
        ProtocolVariation protocol = ProtocolVariation.findVariant(rmUri, addrUri);
        Map<Endpoint, RMEndpoint> endpointMap = endpointMaps.get(protocol);
        if (endpointMap == null) {
            org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(
                "UNSUPPORTED_NAMESPACE", LOG, addrUri, rmUri);
            LOG.log(Level.INFO, msg.toString());
            throw new RMException(msg);
        }
        RMEndpoint rme = endpointMap.get(endpoint);
        if (null == rme) {
            rme = createReliableEndpoint(endpoint, protocol);
            org.apache.cxf.transport.Destination destination = message.getExchange().getDestination();
            EndpointReferenceType replyTo = null;
            if (null != destination) {
                AddressingPropertiesImpl maps = RMContextUtils.retrieveMAPs(message, false, false);
                replyTo = maps.getReplyTo();
            }
            Endpoint ei = message.getExchange().get(Endpoint.class);
            org.apache.cxf.transport.Destination dest 
                = ei == null ? null : ei.getEndpointInfo()
                    .getProperty(MAPAggregator.DECOUPLED_DESTINATION, 
                             org.apache.cxf.transport.Destination.class);
            rme.initialise(message.getExchange().getConduit(message), replyTo, dest);
            endpointMap.put(endpoint, rme);
            LOG.fine("Created new RMEndpoint.");
        }
        return rme;
    }

    /**
     * Get the WS-Addressing namespace being used for a message. If the WS-Addressing namespace has not been
     * set, this returns the default configured for this manager.
     * 
     * @param message
     * @return namespace URI
     */
    public String getAddressingNamespace(Message message) {
        String addrUri = (String)message.getContextualProperty(WSRM_WSA_VERSION_PROPERTY);
        if (addrUri == null) {
            AddressingPropertiesImpl maps = RMContextUtils.retrieveMAPs(message, false, false);
            if (maps != null) {
                addrUri = maps.getNamespaceURI();
            }
            if (addrUri == null) {
                addrUri = getConfiguredProtocol().getWSANamespace();
            }
        }
        return addrUri;
    }

    /**
     * Get the WS-RM namespace being used for a message. If the WS-RM namespace has not been set, this returns
     * the default configured for this manager.
     * 
     * @param message
     * @return namespace URI
     */
    String getRMNamespace(Message message) {
        String rmUri = (String)message.getContextualProperty(WSRM_VERSION_PROPERTY);
        if (rmUri == null) {
            RMProperties rmps = RMContextUtils.retrieveRMProperties(message, false);
            if (rmps != null) {
                rmUri = rmps.getNamespaceURI();
            }
            if (rmUri == null) {
                rmUri = getRMNamespace();
            }
        }
        return rmUri;
    }

    public Destination getDestination(Message message) throws RMException {
        RMEndpoint rme = getReliableEndpoint(message);
        if (null != rme) {
            return rme.getDestination();
        }
        return null;
    }

    public Source getSource(Message message) throws RMException {
        RMEndpoint rme = getReliableEndpoint(message);
        if (null != rme) {
            return rme.getSource();
        }
        return null;
    }

    public SourceSequence getSequence(Identifier inSeqId, Message message, AddressingProperties maps)
        throws RMException {

        Source source = getSource(message);
        SourceSequence seq = source.getCurrent(inSeqId);
        if (null == seq || seq.isExpired()) {
            // TODO: better error handling
            EndpointReferenceType to = null;
            boolean isServer = RMContextUtils.isServerSide(message);
            EndpointReferenceType acksTo = null;
            RelatesToType relatesTo = null;
            if (isServer) {

                AddressingPropertiesImpl inMaps = RMContextUtils.retrieveMAPs(message, false, false);
                inMaps.exposeAs(getConfiguredProtocol().getWSANamespace());
                acksTo = RMUtils.createReference(inMaps.getTo().getValue());
                to = inMaps.getReplyTo();
                source.getReliableEndpoint().getServant().setUnattachedIdentifier(inSeqId);
                relatesTo = (new org.apache.cxf.ws.addressing.ObjectFactory()).createRelatesToType();
                Destination destination = getDestination(message);
                DestinationSequence inSeq = inSeqId == null ? null : destination.getSequence(inSeqId);
                relatesTo.setValue(inSeq != null ? inSeq.getCorrelationID() : null);

            } else {
                to = RMUtils.createReference(maps.getTo().getValue());
                acksTo = maps.getReplyTo();
                if (RMUtils.getAddressingConstants().getNoneURI().equals(acksTo.getAddress().getValue())) {
                    Endpoint ei = message.getExchange().get(Endpoint.class);
                    org.apache.cxf.transport.Destination dest 
                        = ei == null ? null : ei.getEndpointInfo()
                                .getProperty(MAPAggregator.DECOUPLED_DESTINATION, 
                                         org.apache.cxf.transport.Destination.class);
                    if (null == dest) {
                        acksTo = RMUtils.createAnonymousReference();
                    } else {
                        acksTo = dest.getAddress();
                    }
                }
            }

            if (ContextUtils.isGenericAddress(to)) {
                org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(
                    "CREATE_SEQ_ANON_TARGET", LOG, 
                    to != null && to.getAddress() != null 
                    ? to.getAddress().getValue() : null);
                LOG.log(Level.INFO, msg.toString());
                throw new RMException(msg);
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
        for (ProtocolVariation protocol : ProtocolVariation.values()) {
            Map<Endpoint, RMEndpoint> map = endpointMaps.get(protocol);
            if (map.size() > 0) {
                LOG.log(Level.FINE,
                    "Shutting down RMManager with {0} remaining endpoints for protocol variation {1}.",
                    new Object[] {new Integer(map.size()), protocol });
                for (RMEndpoint rme : map.values()) {            
                    rme.shutdown();
                }
            }
        }

        // remove references to timer tasks cancelled above to make them
        // eligible for garbage collection
        Timer t = getTimer(false);
        if (t != null) {
            t.purge();
            t.cancel();
        }

        // unregistring of this managed bean from the server is done by the bus itself
    }
    
    synchronized void shutdownReliableEndpoint(Endpoint e) {
        RMEndpoint rme = null;
        for (ProtocolVariation protocol : ProtocolVariation.values()) {
            Map<Endpoint, RMEndpoint> map = endpointMaps.get(protocol);
            rme = map.get(e);
            if (rme != null) {
                break;
            }
        }
        if (rme == null) {
            // not found
            return;
        }
        rme.shutdown();        
        
        // remove references to timer tasks cancelled above to make them
        // eligible for garbage collection
        Timer t = getTimer(false);
        if (t != null) {
            t.purge();
        }
        
        for (ProtocolVariation protocol : ProtocolVariation.values()) {
            endpointMaps.get(protocol).remove(e);
        }
    }
    
    void recoverReliableEndpoint(Endpoint endpoint, Conduit conduit, ProtocolVariation protocol) {
        if (null == store || null == retransmissionQueue) {
            return;
        }        
        
        String id = RMUtils.getEndpointIdentifier(endpoint, getBus());
        
        Collection<SourceSequence> sss = store.getSourceSequences(id, protocol);
        Collection<DestinationSequence> dss = store.getDestinationSequences(id, protocol);
        if ((null == sss || 0 == sss.size()) && (null == dss || 0 == dss.size())) {                        
            return;
        }
        LOG.log(Level.FINE, "Number of source sequences: {0}", sss.size());
        LOG.log(Level.FINE, "Number of destination sequences: {0}", dss.size());
        
        LOG.log(Level.FINE, "Recovering {0} endpoint with id: {1}",
                new Object[] {null == conduit ? "client" : "server", id});
        RMEndpoint rme = createReliableEndpoint(endpoint, protocol);
        rme.initialise(conduit, null, null);
        endpointMaps.get(protocol).put(endpoint, rme);
        SourceSequence css = null;
        for (SourceSequence ss : sss) {            
 
            Collection<RMMessage> ms = store.getMessages(ss.getIdentifier(), true);
            if (null == ms || 0 == ms.size()) {
                continue;
            }
            LOG.log(Level.FINE, "Number of messages in sequence: {0}", ms.size());
            
            rme.getSource().addSequence(ss, false);
            // choosing an arbitrary valid source sequence as the current source sequence
            if (css == null && !ss.isExpired() && !ss.isLastMessage()) {
                css = ss;
                rme.getSource().setCurrent(css);
            }
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
                
                SequenceType st = new SequenceType();
                st.setIdentifier(ss.getIdentifier());
                st.setMessageNumber(m.getMessageNumber());
                RMProperties rmps = new RMProperties();
                rmps.setSequence(st);
                if (ss.isLastMessage() && ss.getCurrentMessageNr() == m.getMessageNumber()) {
                    CloseSequenceType close = new CloseSequenceType();
                    close.setIdentifier(ss.getIdentifier());
                    rmps.setCloseSequence(close);
                }
                RMContextUtils.storeRMProperties(message, rmps, true);                
                if (null == conduit) {
                    String to = m.getTo();
                    AddressingProperties maps = new AddressingPropertiesImpl();
                    maps.setTo(RMUtils.createReference(to));
                    RMContextUtils.storeMAPs(maps, message, true, false);
                }
                                    
                message.put(RMMessageConstants.SAVED_CONTENT, m.getCachedOutputStream());
                          
                retransmissionQueue.addUnacknowledged(message);
            }            
        }
        
        for (DestinationSequence ds : dss) {
            rme.getDestination().addSequence(ds, false);        
        }
        retransmissionQueue.start();
        
    }
    
    RMEndpoint createReliableEndpoint(Endpoint endpoint, ProtocolVariation protocol) {
        return new RMEndpoint(this, endpoint, protocol);
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
        } else if (deliveryAssurance.getExactlyOnce() != null) {
            if (deliveryAssurance.getAtMostOnce() == null) {
                deliveryAssurance.setAtMostOnce(factory.createDeliveryAssuranceTypeAtMostOnce());
            }
            if (deliveryAssurance.getAtLeastOnce() == null) {
                deliveryAssurance.setAtLeastOnce(factory.createDeliveryAssuranceTypeAtLeastOnce());
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
        if (null != bus) {
            managedManager = new ManagedRMManager(this);
            instrumentationManager = bus.getExtension(InstrumentationManager.class);        
            if (instrumentationManager != null) {   
                try {
                    instrumentationManager.register(managedManager);
                } catch (JMException jmex) {
                    LOG.log(Level.WARNING, "Registering ManagedRMManager failed.", jmex);
                }
            }
        }
    }
    
    @PostConstruct
    void registerListeners() {
        if (null == bus) {
            return;
        }
        ServerLifeCycleManager slm = bus.getExtension(ServerLifeCycleManager.class);
        if (null != slm) {
            slm.registerListener(new ServerLifeCycleListener() {
                public void startServer(Server server) {
                    RMManager.this.startServer(server);
                }
                public void stopServer(Server server) {
                    RMManager.this.stopServer(server);
                }
            });
        }
        ClientLifeCycleManager clm = bus.getExtension(ClientLifeCycleManager.class);
        if (null != clm) {
            clm.registerListener(new ClientLifeCycleListener() {
                public void clientCreated(Client client) {
                    RMManager.this.clientCreated(client);
                }
                public void clientDestroyed(Client client) {
                    RMManager.this.clientDestroyed(client);
                }
            });
        }
    }

    class DefaultSequenceIdentifierGenerator implements SequenceIdentifierGenerator {

        public Identifier generateSequenceIdentifier() {
            String sequenceID = RMContextUtils.generateUUID();
            Identifier sid = new Identifier();
            sid.setValue(sequenceID);
            return sid;
        }
    }

    Map<ProtocolVariation, Map<Endpoint, RMEndpoint>> getEndpointMaps() {
        return endpointMaps;
    }

    final void setEndpointMaps(Map<ProtocolVariation, Map<Endpoint, RMEndpoint>> endpointMaps) {
        endpointMaps.put(ProtocolVariation.RM10WSA200408, new HashMap<Endpoint, RMEndpoint>());
        endpointMaps.put(ProtocolVariation.RM10WSA200508, new HashMap<Endpoint, RMEndpoint>());
        endpointMaps.put(ProtocolVariation.RM11WSA200508, new HashMap<Endpoint, RMEndpoint>());
        this.endpointMaps = endpointMaps;
    }
}