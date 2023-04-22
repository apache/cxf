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

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.xml.namespace.QName;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
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
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.RelatesToType;
import org.apache.cxf.ws.rm.RMConfiguration.DeliveryAssurance;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.manager.DeliveryAssuranceType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.manager.RM10AddressingNamespaceType;
import org.apache.cxf.ws.rm.manager.SequenceTerminationPolicyType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.cxf.ws.rm.persistence.PersistenceUtils;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.policy.RMPolicyUtilities;
import org.apache.cxf.ws.rm.soap.RedeliveryQueueImpl;
import org.apache.cxf.ws.rm.soap.RetransmissionQueueImpl;
import org.apache.cxf.ws.rm.soap.SoapFaultFactory;
import org.apache.cxf.ws.rm.v200702.CloseSequenceType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceType;
import org.apache.cxf.ws.security.SecurityConstants;

/**
 *
 */
public class RMManager {

    /** Message contextual property giving WS-ReliableMessaging namespace. */
    public static final String WSRM_VERSION_PROPERTY = "org.apache.cxf.ws.rm.namespace";

    /** Message contextual property giving addressing namespace to be used by WS-RM implementation. */
    public static final String WSRM_WSA_VERSION_PROPERTY = "org.apache.cxf.ws.rm.wsa-namespace";

    /** Message contextual property giving the last message flag (Boolean). */
    public static final String WSRM_LAST_MESSAGE_PROPERTY = "org.apache.cxf.ws.rm.last-message";

    /** Message contextual property giving WS-ReliableMessaging inactivity timeout (Long). */
    public static final String WSRM_INACTIVITY_TIMEOUT_PROPERTY = "org.apache.cxf.ws.rm.inactivity-timeout";

    /** Message contextual property giving WS-ReliableMessaging base retransmission interval (Long). */
    public static final String WSRM_RETRANSMISSION_INTERVAL_PROPERTY = "org.apache.cxf.ws.rm.retransmission-interval";

    /** Message contextual property giving WS-ReliableMessaging exponential backoff flag (Boolean). */
    public static final String WSRM_EXPONENTIAL_BACKOFF_PROPERTY = "org.apache.cxf.ws.rm.exponential-backoff";

    /** Message contextual property giving WS-ReliableMessaging acknowledgement interval (Long). */
    public static final String WSRM_ACKNOWLEDGEMENT_INTERVAL_PROPERTY = "org.apache.cxf.ws.rm.acknowledgement-interval";

    private static final Logger LOG = LogUtils.getL7dLogger(RMManager.class);
    private static final String WSRM_RETRANSMIT_CHAIN = RMManager.class.getName() + ".retransmitChain";


    private Bus bus;
    private RMStore store;
    private SequenceIdentifierGenerator idGenerator;
    private RetransmissionQueue retransmissionQueue;
    private RedeliveryQueue redeliveryQueue;
    private Map<Endpoint, RMEndpoint> reliableEndpoints = new ConcurrentHashMap<>();
    private AtomicReference<Timer> timer = new AtomicReference<>();
    private RMConfiguration configuration;
    private SourcePolicyType sourcePolicy;
    private DestinationPolicyType destinationPolicy;
    private InstrumentationManager instrumentationManager;
    private ManagedRMManager managedManager;

    // ServerLifeCycleListener

    public void startServer(Server server) {
        recoverReliableEndpoint(server.getEndpoint(), (Conduit)null);
    }

    public void stopServer(Server server) {
    }

    // ClientLifeCycleListener

    public void clientCreated(final Client client) {
        if (null == store || null == retransmissionQueue) {
            return;
        }
        String id = RMUtils.getEndpointIdentifier(client.getEndpoint(), getBus());
        Collection<SourceSequence> sss = store.getSourceSequences(id/*, protocol*/);
        if (null == sss || sss.isEmpty()) {
            return;
        }
        LOG.log(Level.FINE, "Number of source sequences: {0}", sss.size());
        recoverReliableEndpoint(client.getEndpoint(), client.getConduit()/*, protocol*/);
    }

    public void clientDestroyed(Client client) {
    }

    // Configuration

    public void setRMNamespace(String uri) {
        getConfiguration().setRMNamespace(uri);
    }

    public void setRM10AddressingNamespace(RM10AddressingNamespaceType addrns) {
        getConfiguration().setRM10AddressingNamespace(addrns.getUri());
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

    public RedeliveryQueue getRedeliveryQueue() {
        return redeliveryQueue;
    }

    public void setRedeliveryQueue(RedeliveryQueue redeliveryQueue) {
        this.redeliveryQueue = redeliveryQueue;
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
     * @param dat The deliveryAssurance to set.
     */
    public void setDeliveryAssurance(DeliveryAssuranceType dat) {
        RMConfiguration cfg = getConfiguration();
        cfg.setInOrder(dat.isSetInOrder());
        DeliveryAssurance da = null;
        if (dat.isSetExactlyOnce() || (dat.isSetAtLeastOnce() && dat.isSetAtMostOnce())) {
            da = DeliveryAssurance.EXACTLY_ONCE;
        } else if (dat.isSetAtLeastOnce()) {
            da = DeliveryAssurance.AT_LEAST_ONCE;
        } else if (dat.isSetAtMostOnce()) {
            da = DeliveryAssurance.AT_MOST_ONCE;
        }
        cfg.setDeliveryAssurance(da);
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
     * Get base configuration for manager. This needs to be modified by endpoint policies to get the effective
     * configuration.
     * @return configuration (non-<code>null</code>)
     */
    public RMConfiguration getConfiguration() {
        if (configuration == null) {
            setConfiguration(new RMConfiguration());
        }
        return configuration;
    }

    /**
     * @param configuration (non-<code>null</code>)
     */
    public void setConfiguration(RMConfiguration configuration) {
        if (configuration.getBaseRetransmissionInterval() == null) {
            Long value = Long.valueOf(RetransmissionQueue.DEFAULT_BASE_RETRANSMISSION_INTERVAL);
            configuration.setBaseRetransmissionInterval(value);
        }
        if (configuration.getRMNamespace() == null) {
            configuration.setRMNamespace(RM10Constants.NAMESPACE_URI);
        }
        this.configuration = configuration;
    }

    /**
     * Get configuration after applying policies.
     *
     * @param msg
     * @return configuration (non-<code>null</code>)
     */
    public RMConfiguration getEffectiveConfiguration(Message msg) {
        return RMPolicyUtilities.getRMConfiguration(getConfiguration(), msg);
    }

    /**
     * @param rma The rmAssertion to set.
     */
    public void setRMAssertion(org.apache.cxf.ws.rmp.v200502.RMAssertion rma) {
        setConfiguration(RMPolicyUtilities.intersect(rma, getConfiguration()));
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
        if (null == sp) {
            sp = new SourcePolicyType();
        }
        if (sp.getSequenceTerminationPolicy() == null) {
            SequenceTerminationPolicyType term = new SequenceTerminationPolicyType();
            term.setTerminateOnShutdown(true);
            sp.setSequenceTerminationPolicy(term);
        }
        sourcePolicy = sp;
    }

    // The real stuff ...

    public RMEndpoint getReliableEndpoint(Message message) throws RMException {
        Endpoint endpoint = message.getExchange().getEndpoint();
        QName name = endpoint.getEndpointInfo().getName();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Getting RMEndpoint for endpoint with info: " + name);
        }
        if (name.equals(RM10Constants.PORT_NAME) || name.equals(RM11Constants.PORT_NAME)) {
            WrappedEndpoint wrappedEndpoint = (WrappedEndpoint)endpoint;
            endpoint = wrappedEndpoint.getWrappedEndpoint();
        }
        String rmUri = (String)message.getContextualProperty(WSRM_VERSION_PROPERTY);
        if (rmUri == null) {
            RMProperties rmps = RMContextUtils.retrieveRMProperties(message, false);
            if (rmps != null) {
                rmUri = rmps.getNamespaceURI();
            }
        }
        String addrUri = (String)message.getContextualProperty(WSRM_WSA_VERSION_PROPERTY);
        if (addrUri == null) {
            AddressingProperties maps = ContextUtils.retrieveMAPs(message, false, false, false);
            if (maps != null) {
                addrUri = maps.getNamespaceURI();
            }
        }

        RMConfiguration config = getConfiguration();
        if (rmUri != null) {
            config.setRMNamespace(rmUri);
            ProtocolVariation protocol = ProtocolVariation.findVariant(rmUri, addrUri);
            if (protocol == null) {
                org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(
                    "UNSUPPORTED_NAMESPACE", LOG, addrUri, rmUri);
                LOG.log(Level.INFO, msg.toString());
                throw new RMException(msg);
            }
        }
        if (addrUri != null) {
            config.setRM10AddressingNamespace(addrUri);
        }
        Long timeout = (Long)message.getContextualProperty(WSRM_INACTIVITY_TIMEOUT_PROPERTY);
        if (timeout != null) {
            config.setInactivityTimeout(timeout);
        }
        Long interval = (Long)message.getContextualProperty(WSRM_RETRANSMISSION_INTERVAL_PROPERTY);
        if (interval != null) {
            config.setBaseRetransmissionInterval(interval);
        }
        Boolean exponential = (Boolean)message.getContextualProperty(WSRM_EXPONENTIAL_BACKOFF_PROPERTY);
        if (exponential != null) {
            config.setExponentialBackoff(exponential);
        }
        interval = (Long)message.getContextualProperty(WSRM_ACKNOWLEDGEMENT_INTERVAL_PROPERTY);
        if (interval != null) {
            config.setAcknowledgementInterval(interval);
        }
        RMEndpoint rme = reliableEndpoints.get(endpoint);
        if (null == rme) {
            synchronized (endpoint) {
                rme = reliableEndpoints.get(endpoint);
                if (rme != null) {
                    return rme;
                }
                rme = createReliableEndpoint(endpoint);
                org.apache.cxf.transport.Destination destination = message.getExchange().getDestination();
                EndpointReferenceType replyTo = null;
                if (null != destination) {
                    AddressingProperties maps = RMContextUtils.retrieveMAPs(message, false, false);
                    replyTo = maps.getReplyTo();
                }
                Endpoint ei = message.getExchange().getEndpoint();
                org.apache.cxf.transport.Destination dest
                    = ei == null ? null : ei.getEndpointInfo()
                        .getProperty(MAPAggregator.DECOUPLED_DESTINATION,
                                 org.apache.cxf.transport.Destination.class);
                config = RMPolicyUtilities.getRMConfiguration(config, message);
                rme.initialise(config, message.getExchange().getConduit(message), replyTo, dest, message);
                reliableEndpoints.put(endpoint, rme);
                LOG.fine("Created new RMEndpoint.");
            }
        }
        return rme;
    }
    public RMEndpoint findReliableEndpoint(QName qn) {
        for (RMEndpoint rpe : reliableEndpoints.values()) {
            if (qn.equals(rpe.getApplicationEndpoint().getService().getName())) {
                return rpe;
            }
        }
        return null;
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
        RMConfiguration config = getEffectiveConfiguration(message);
        if (null == seq || seq.isExpired()) {
            // TODO: better error handling
            final EndpointReferenceType to;
            boolean isServer = RMContextUtils.isServerSide(message);
            EndpointReferenceType acksTo;
            RelatesToType relatesTo = null;
            if (isServer) {
                AddressingProperties inMaps = RMContextUtils.retrieveMAPs(message, false, false);
                inMaps.exposeAs(config.getAddressingNamespace());
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
                    Endpoint ei = message.getExchange().getEndpoint();
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
            ProtocolVariation protocol = config.getProtocolVariation();
            Exchange exchange = new ExchangeImpl();
            Map<String, Object> context = new HashMap<>(16);
            for (String key : message.getContextualPropertyKeys()) {
                //copy other properties?
                if (key.startsWith("ws-security") || key.startsWith("security.")) {
                    context.put(key, message.getContextualProperty(key));
                }
            }

            CreateSequenceResponseType createResponse =
                proxy.createSequence(acksTo, relatesTo, isServer, protocol, exchange, context);
            if (!isServer) {
                Servant servant = source.getReliableEndpoint().getServant();
                servant.createSequenceResponse(createResponse, protocol);

                // propagate security properties to application endpoint, in case we're using WS-SecureConversation
                Exchange appex = message.getExchange();
                if (appex.get(SecurityConstants.TOKEN) == null) {
                    appex.put(SecurityConstants.TOKEN, exchange.get(SecurityConstants.TOKEN));
                    appex.put(SecurityConstants.TOKEN_ID, exchange.get(SecurityConstants.TOKEN_ID));
                }
            }

            seq = source.awaitCurrent(inSeqId);
            seq.setTarget(to);
        }

        return seq;
    }

    @PreDestroy
    public void shutdown() {
        // shutdown remaining endpoints
        if (!reliableEndpoints.isEmpty()) {
            LOG.log(Level.FINE,
                    "Shutting down RMManager with {0} remaining endpoints.",
                    new Object[] {Integer.valueOf(reliableEndpoints.size())});
            for (RMEndpoint rme : reliableEndpoints.values()) {
                rme.shutdown();
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

    void shutdownReliableEndpoint(Endpoint e) {
        RMEndpoint rme = reliableEndpoints.get(e);
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

        reliableEndpoints.remove(e);
    }

    void recoverReliableEndpoint(Endpoint endpoint, Conduit conduit) {
        if (null == store || null == retransmissionQueue) {
            return;
        }

        String id = RMUtils.getEndpointIdentifier(endpoint, getBus());

        Collection<SourceSequence> sss = store.getSourceSequences(id);
        Collection<DestinationSequence> dss = store.getDestinationSequences(id);
        if ((null == sss || sss.isEmpty()) && (null == dss || dss.isEmpty())) {
            return;
        }
        LOG.log(Level.FINE, "Number of source sequences: {0}", sss.size());
        LOG.log(Level.FINE, "Number of destination sequences: {0}", dss.size());

        LOG.log(Level.FINE, "Recovering {0} endpoint with id: {1}",
                new Object[] {null == conduit ? "client" : "server", id});
        RMEndpoint rme = createReliableEndpoint(endpoint);
        rme.initialise(getConfiguration(), conduit, null, null, null);
        synchronized (reliableEndpoints) {
            reliableEndpoints.put(endpoint, rme);
        }
        for (SourceSequence ss : sss) {
            recoverSourceSequence(endpoint, conduit, rme.getSource(), ss);
        }

        for (DestinationSequence ds : dss) {
            recoverDestinationSequence(endpoint, conduit, rme.getDestination(), ds);
        }
        retransmissionQueue.start();
        redeliveryQueue.start();
    }

    private void recoverSourceSequence(Endpoint endpoint, Conduit conduit, Source s,
                                       SourceSequence ss) {
        Collection<RMMessage> ms = store.getMessages(ss.getIdentifier(), true);
        if (null == ms || ms.isEmpty()) {
            store.removeSourceSequence(ss.getIdentifier());
            return;
        }
        LOG.log(Level.FINE, "Number of messages in sequence: {0}", ms.size());
        // only recover the sequence if there are pending messages
        s.addSequence(ss, false);
        // choosing an arbitrary valid source sequence as the current source sequence
        if (s.getAssociatedSequence(null) == null && !ss.isExpired() && !ss.isLastMessage()) {
            s.setCurrent(ss);
        }
        //make sure this is associated with the offering id
        s.setCurrent(ss.getOfferingSequenceIdentifier(), ss);
        for (RMMessage m : ms) {

            Message message = new MessageImpl();
            Exchange exchange = new ExchangeImpl();
            message.setExchange(exchange);
            exchange.setOutMessage(message);
            if (null != conduit) {
                exchange.setConduit(conduit);
                message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
            }
            exchange.put(Endpoint.class, endpoint);
            exchange.put(Service.class, endpoint.getService());
            exchange.put(Binding.class, endpoint.getBinding());
            exchange.put(Bus.class, bus);

            SequenceType st = new SequenceType();
            st.setIdentifier(ss.getIdentifier());
            st.setMessageNumber(m.getMessageNumber());
            RMProperties rmps = new RMProperties();
            rmps.setSequence(st);
            rmps.setCreatedTime(m.getCreatedTime());
            rmps.exposeAs(ss.getProtocol().getWSRMNamespace());
            if (ss.isLastMessage() && ss.getCurrentMessageNr() == m.getMessageNumber()) {
                CloseSequenceType close = new CloseSequenceType();
                close.setIdentifier(ss.getIdentifier());
                rmps.setCloseSequence(close);
            }
            RMContextUtils.storeRMProperties(message, rmps, true);
            if (null == conduit) {
                String to = m.getTo();
                AddressingProperties maps = new AddressingProperties();
                maps.setTo(RMUtils.createReference(to));
                RMContextUtils.storeMAPs(maps, message, true, false);
            }

            try {
                // RMMessage is stored in a serialized way, therefore
                // RMMessage content must be splitted into soap root message
                // and attachments
                PersistenceUtils.decodeRMContent(m, message);
                RMContextUtils.setProtocolVariation(message, ss.getProtocol());
                retransmissionQueue.addUnacknowledged(message);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Error reading persisted message data", e);
            }
        }
    }

    private void recoverDestinationSequence(Endpoint endpoint, Conduit conduit, Destination d,
                                             DestinationSequence ds) {
        // always recover the sequence
        d.addSequence(ds, false);

        Collection<RMMessage> ms = store.getMessages(ds.getIdentifier(), false);
        if (null == ms || ms.isEmpty()) {
            return;
        }
        LOG.log(Level.FINE, "Number of messages in sequence: {0}", ms.size());

        for (RMMessage m : ms) {
            Message message = new MessageImpl();
            Exchange exchange = new ExchangeImpl();
            message.setExchange(exchange);
            if (null != conduit) {
                exchange.setConduit(conduit);
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
            st.setIdentifier(ds.getIdentifier());
            st.setMessageNumber(m.getMessageNumber());
            RMProperties rmps = new RMProperties();
            rmps.setSequence(st);
            rmps.setCreatedTime(m.getCreatedTime());
            RMContextUtils.storeRMProperties(message, rmps, false);
            try {
                // RMMessage is stored in a serialized way, therefore
                // RMMessage content must be splitted into soap root message
                // and attachments
                PersistenceUtils.decodeRMContent(m, message);
                redeliveryQueue.addUndelivered(message);
                // add acknowledged undelivered message
                ds.addDeliveringMessageNumber(m.getMessageNumber());
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Error reading persisted message data", e);
            }
        }

        // if no messages are recovered and the sequence has been already terminated, remove the sequence
        if (ds.isTerminated() && ds.allAcknowledgedMessagesDelivered()) {
            d.removeSequence(ds);
            store.removeDestinationSequence(ds.getIdentifier());
        }
    }

    RMEndpoint createReliableEndpoint(final Endpoint endpoint) {
        endpoint.addCleanupHook(new Closeable() {
            public void close() throws IOException {
                shutdownReliableEndpoint(endpoint);
            }
        });
        return new RMEndpoint(this, endpoint);
    }

    public void init(Bus b) {
        setBus(b);
        initialise();
        registerListeners();
    }

    @PostConstruct
    void initialise() {
        if (configuration == null) {
            getConfiguration().setExponentialBackoff(true);
        }
        DeliveryAssurance da = configuration.getDeliveryAssurance();
        if (da == null) {
            configuration.setDeliveryAssurance(DeliveryAssurance.AT_LEAST_ONCE);
        }
        if (null == sourcePolicy) {
            setSourcePolicy(null);
        }
        if (null == destinationPolicy) {
            DestinationPolicyType dp = new DestinationPolicyType();
            dp.setAcksPolicy(new AcksPolicyType());
            setDestinationPolicy(dp);
        }
        if (null == retransmissionQueue) {
            retransmissionQueue = new RetransmissionQueueImpl(this);
        }
        if (null == redeliveryQueue) {
            redeliveryQueue = new RedeliveryQueueImpl(this);
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

    Map<Endpoint, RMEndpoint> getReliableEndpointsMap() {
        return reliableEndpoints;
    }

    void setReliableEndpointsMap(Map<Endpoint, RMEndpoint> map) {
        reliableEndpoints = map;
    }

    class DefaultSequenceIdentifierGenerator implements SequenceIdentifierGenerator {

        public Identifier generateSequenceIdentifier() {
            String sequenceID = RMContextUtils.generateUUID();
            Identifier sid = new Identifier();
            sid.setValue(sequenceID);
            return sid;
        }
    }

    /**
     * Clones and saves the interceptor chain the first time this is called, so that it can be used for retransmission.
     * Calls after the first are ignored.
     *
     * @param msg
     */
    public void initializeInterceptorChain(Message msg) {
        Endpoint ep = msg.getExchange().getEndpoint();
        synchronized (ep) {
            if (ep.get(WSRM_RETRANSMIT_CHAIN) == null) {
                LOG.info("Setting retransmit chain from message");
                PhaseInterceptorChain chain = (PhaseInterceptorChain)msg.getInterceptorChain();
                chain = chain.cloneChain();
                ep.put(WSRM_RETRANSMIT_CHAIN, chain);
            }
        }
    }

    /**
     * Get interceptor chain for retransmitting a message.
     *
     * @return chain (<code>null</code> if none set)
     */
    public PhaseInterceptorChain getRetransmitChain(Message msg) {
        Endpoint ep = msg.getExchange().getEndpoint();
        PhaseInterceptorChain pic = (PhaseInterceptorChain)ep.get(WSRM_RETRANSMIT_CHAIN);
        if (pic == null) {
            return null;
        }
        return pic.cloneChain();
    }
}
