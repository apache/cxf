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

package org.apache.cxf.transport.jms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.AbstractMultiplexDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.workqueue.SynchronousExecutor;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

public class JMSDestination extends AbstractMultiplexDestination implements Configurable, MessageListener,
    JMSExchangeSender {

    protected static final String BASE_BEAN_NAME_SUFFIX = ".jms-destination-base";

    private static final Logger LOG = LogUtils.getL7dLogger(JMSDestination.class);

    protected ServerConfig serverConfig;
    protected ServerBehaviorPolicyType runtimePolicy;
    protected AddressType address;
    protected SessionPoolType sessionPool;
    protected Destination targetDestination;
    protected Destination replyToDestination;
    protected JMSSessionFactory sessionFactory;
    protected Bus bus;
    protected EndpointInfo endpointInfo;
    protected String beanNameSuffix;

    final ConduitInitiator conduitInitiator;
    Session listenerSession;
    JMSListenerThread listenerThread;

    public JMSDestination(Bus b, ConduitInitiator ci, EndpointInfo info) throws IOException {
        super(b, getTargetReference(info, b), info);

        this.bus = b;
        this.endpointInfo = info;
        this.beanNameSuffix = BASE_BEAN_NAME_SUFFIX;
        conduitInitiator = ci;

        initConfig();
    }

    private void initConfig() {
        this.runtimePolicy = endpointInfo.getTraversedExtensor(new ServerBehaviorPolicyType(),
                                                               ServerBehaviorPolicyType.class);
        this.serverConfig = endpointInfo.getTraversedExtensor(new ServerConfig(), ServerConfig.class);
        this.address = endpointInfo.getTraversedExtensor(new AddressType(), AddressType.class);
        this.sessionPool = endpointInfo.getTraversedExtensor(new SessionPoolType(), SessionPoolType.class);
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(this);
        }
    }

    /**
     * @param inMessage the incoming message
     * @return the inbuilt backchannel
     */
    protected Conduit getInbuiltBackChannel(Message inMessage) {
        EndpointReferenceType anon = EndpointReferenceUtils.getAnonymousEndpointReference();
        return new BackChannelConduit(this, anon, inMessage);
    }

    private Executor getExecutor(WorkQueueManager wqm, QName name) {
        // Fallback if no Workqueuemanager
        Executor executor = SynchronousExecutor.getInstance();
        if (wqm != null) {
            if (name != null) {
                executor = wqm.getNamedWorkQueue("{" + name.getNamespaceURI() + "}"
                                                 + name.getLocalPart());
            }
            if (executor == null) {
                executor = wqm.getNamedWorkQueue("jms");
            }
            if (executor == null) {
                executor = wqm.getAutomaticWorkQueue();
            }
        }
        return executor;
    }

    /**
     * Initialize Sessionfactory, Initialize and start ListenerThread {@inheritDoc}
     */
    public void activate() {
        getLogger().log(Level.INFO, "JMSDestination activate().... ");

        if (this.address == null || this.address.getJndiConnectionFactoryName() == null) {
            throw new RuntimeException("Insufficient configuration for Destination. "
                                       + "Did you configure a <jms:destination name=\"" + getBeanName()
                                       + "\"> and set the jndiConnectionFactoryName ?");
        }

        try {
            getLogger().log(Level.FINE, "establishing JMS connection");
            sessionFactory = JMSSessionFactory.connect(getJMSAddress(), getSessionPool(), serverConfig);
            Connection connection = sessionFactory.getConnection();
            Context context = sessionFactory.getInitialContext();
            this.targetDestination = JMSUtils.resolveRequestDestination(context, connection, address);
            this.replyToDestination = JMSUtils.resolveRequestDestination(context, connection, address);
            WorkQueueManager wqm = bus.getExtension(WorkQueueManager.class);
            QName name = null;
            if (endpointInfo != null) {
                name = endpointInfo.getName();
            }
            Executor executor = getExecutor(wqm, name);
            String messageSelector = runtimePolicy.getMessageSelector();
            String durableName = runtimePolicy.getDurableSubscriberName();
            listenerThread = new JMSListenerThread(executor, this);
            listenerThread.start(connection, targetDestination, messageSelector, durableName);
        } catch (JMSException ex) {
            getLogger().log(Level.SEVERE, "JMS connect failed with JMSException : ", ex);
        } catch (NamingException nex) {
            getLogger().log(Level.SEVERE, "JMS connect failed with NamingException : ", nex);
        }
    }

    public void deactivate() {
        if (listenerThread != null) {
            listenerThread.close();
        }
        sessionFactory.shutdown();
    }

    public void shutdown() {
        getLogger().log(Level.FINE, "JMSDestination shutdown()");
        this.deactivate();
    }

    public Queue getReplyToDestination(Message inMessage) throws JMSException, NamingException {
        javax.jms.Message message = (javax.jms.Message)inMessage.get(JMSConstants.JMS_REQUEST_MESSAGE);
        // If WS-Addressing had set the replyTo header.
        String replyToName = (String)inMessage.get(JMSConstants.JMS_REBASED_REPLY_TO);
        if (replyToName != null) {
            Context context = sessionFactory.getInitialContext();
            return (Queue)context.lookup(replyToName);
        } else if (message.getJMSReplyTo() != null) {
            return (Queue)message.getJMSReplyTo();
        } else {
            return (Queue)replyToDestination;
        }
    }

    public void setReplyCorrelationID(javax.jms.Message request, javax.jms.Message reply)
        throws JMSException {

        String correlationID = request.getJMSCorrelationID();

        if (correlationID == null || "".equals(correlationID)
            && getRuntimePolicy().isUseMessageIDAsCorrelationID()) {
            correlationID = request.getJMSMessageID();
        }

        if (correlationID != null && !"".equals(correlationID)) {
            reply.setJMSCorrelationID(correlationID);
        }
    }

    /**
     * Convert JMS message received by ListenerThread to CXF message and inform incomingObserver that a
     * message was received. The observer will call the service and then send the response CXF message by
     * using the BackChannelConduit
     * 
     * @param message
     * @throws IOException
     */
    public void onMessage(javax.jms.Message message) {
        try {
            getLogger().log(Level.FINE, "server received request: ", message);

            byte[] request = JMSUtils.retrievePayload(message);
            getLogger().log(Level.FINE, "The Request Message is [ " + request + "]");

            // Build CXF message from JMS message
            MessageImpl inMessage = new MessageImpl();
            inMessage.setContent(InputStream.class, new ByteArrayInputStream(request));
            JMSUtils.populateIncomingContext(message, inMessage, JMSConstants.JMS_SERVER_REQUEST_HEADERS);
            inMessage.put(JMSConstants.JMS_SERVER_RESPONSE_HEADERS, new JMSMessageHeadersType());
            inMessage.put(JMSConstants.JMS_REQUEST_MESSAGE, message);
            inMessage.setDestination(this);

            BusFactory.setThreadDefaultBus(bus);

            // handle the incoming message
            incomingObserver.onMessage(inMessage);
        } catch (JMSException e) {
            throw new RuntimeException("Error handling JMS message", e);
        } finally {
            BusFactory.setThreadDefaultBus(null);
        }
    }

    public void sendExchange(Exchange exchange, Object replyObj) {
        Message inMessage = exchange.getInMessage();
        Message outMessage = exchange.getOutMessage();
        if (!JMSUtils.isDestinationStyleQueue(address)) {
            // we will never receive a non-oneway invocation in pub-sub
            // domain from CXF client - however a mis-behaving pure JMS
            // client could conceivably make suce an invocation, in which
            // case we silently discard the reply
            getLogger().log(Level.WARNING, "discarding reply for non-oneway invocation ",
                            "with 'topic' destinationStyle");
            return;
        }
        PooledSession replySession = null;
        try {
            // setup the reply message
            replySession = sessionFactory.get();
            javax.jms.Message request = (javax.jms.Message)inMessage.get(JMSConstants.JMS_REQUEST_MESSAGE);
            String msgType = null;
            if (request instanceof TextMessage) {
                msgType = JMSConstants.TEXT_MESSAGE_TYPE;
            } else if (request instanceof BytesMessage) {
                msgType = JMSConstants.BYTE_MESSAGE_TYPE;
            } else {
                msgType = JMSConstants.BINARY_MESSAGE_TYPE;
            }
            javax.jms.Message reply = JMSUtils
                .createAndSetPayload(replyObj, replySession.session(), msgType);

            setReplyCorrelationID(request, reply);
            JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
                .get(JMSConstants.JMS_SERVER_RESPONSE_HEADERS);
            JMSUtils.setMessageProperties(headers, reply);
            // ensure that the contentType is set to the out jms message header
            JMSUtils.setContentToProtocolHeader(outMessage);
            Map<String, List<String>> protHeaders = CastUtils.cast((Map<?, ?>)outMessage
                .get(Message.PROTOCOL_HEADERS));
            JMSUtils.addProtocolHeaders(reply, protHeaders);
            Destination replyTo = getReplyToDestination(inMessage);

            JMSMessageHeadersType inHeaders = (JMSMessageHeadersType)inMessage
                .get(JMSConstants.JMS_SERVER_REQUEST_HEADERS);

            long timeToLive = 0;
            if (request.getJMSExpiration() > 0) {
                TimeZone tz = new SimpleTimeZone(0, "GMT");
                Calendar cal = new GregorianCalendar(tz);
                timeToLive = request.getJMSExpiration() - cal.getTimeInMillis();
            }

            if (timeToLive < 0) {
                getLogger().log(Level.INFO, "Message time to live is already expired skipping response.");
                return;
            }

            int deliveryMode = JMSUtils.getJMSDeliveryMode(inHeaders);
            int priority = JMSUtils.getJMSPriority(inHeaders);
            long ttl = JMSUtils.getTimeToLive(headers);
            if (ttl <= 0) {
                ttl = getServerConfig().getMessageTimeToLive();
            }
            if (ttl <= 0) {
                ttl = timeToLive;
            }
            getLogger().log(Level.FINE, "send out the message!");
            replySession.producer().send(replyTo, reply, deliveryMode, priority, ttl);

            getLogger().log(Level.FINE, "just server sending reply: ", reply);
            // Check the reply time limit Stream close will call for this
        } catch (JMSException ex) {
            getLogger().log(Level.WARNING, "Failed in post dispatch ...", ex);
            throw new RuntimeException(ex.getMessage());
        } catch (NamingException nex) {
            getLogger().log(Level.WARNING, "Failed in post dispatch ...", nex);
            throw new RuntimeException(nex.getMessage());
        } finally {
            sessionFactory.recycle(replySession);
        }
    }

    protected Logger getLogger() {
        return LOG;
    }

    public String getBeanName() {
        return endpointInfo.getName().toString() + ".jms-destination";
    }

    public AddressType getJMSAddress() {
        return address;
    }

    public void setJMSAddress(AddressType a) {
        this.address = a;
    }

    public ServerBehaviorPolicyType getRuntimePolicy() {
        return runtimePolicy;
    }

    public void setRuntimePolicy(ServerBehaviorPolicyType runtimePolicy) {
        this.runtimePolicy = runtimePolicy;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public SessionPoolType getSessionPool() {
        return sessionPool;
    }

    public void setSessionPool(SessionPoolType sessionPool) {
        this.sessionPool = sessionPool;
    }

    // this should deal with the cxf message
    protected class BackChannelConduit extends AbstractConduit {

        protected Message inMessage;
        private JMSExchangeSender sender;

        BackChannelConduit(JMSExchangeSender sender, EndpointReferenceType ref, Message message) {
            super(ref);
            inMessage = message;
            this.sender = sender;
        }

        /**
         * Register a message observer for incoming messages.
         * 
         * @param observer the observer to notify on receipt of incoming
         */
        public void setMessageObserver(MessageObserver observer) {
            // shouldn't be called for a back channel conduit
        }

        /**
         * Send an outbound message, assumed to contain all the name-value mappings of the corresponding input
         * message (if any).
         * 
         * @param message the message to be sent.
         */
        public void prepare(Message message) throws IOException {
            // setup the message to be send back
            javax.jms.Message jmsMessage = (javax.jms.Message)inMessage
                .get(JMSConstants.JMS_REQUEST_MESSAGE);
            message.put(JMSConstants.JMS_REQUEST_MESSAGE, jmsMessage);

            if (!message.containsKey(JMSConstants.JMS_SERVER_RESPONSE_HEADERS)
                && inMessage.containsKey(JMSConstants.JMS_SERVER_RESPONSE_HEADERS)) {
                message.put(JMSConstants.JMS_SERVER_RESPONSE_HEADERS, inMessage
                    .get(JMSConstants.JMS_SERVER_RESPONSE_HEADERS));
            }

            Exchange exchange = inMessage.getExchange();
            exchange.setOutMessage(message);
            message.setContent(OutputStream.class, new JMSOutputStream(sender, exchange,
                                                                       jmsMessage instanceof TextMessage));
        }

        protected Logger getLogger() {
            return LOG;
        }
    }

}
