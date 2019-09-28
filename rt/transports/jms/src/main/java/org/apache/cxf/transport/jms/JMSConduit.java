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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.ConfigurationException;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.jms.util.AbstractMessageListenerContainer;
import org.apache.cxf.transport.jms.util.JMSListenerContainer;
import org.apache.cxf.transport.jms.util.JMSSender;
import org.apache.cxf.transport.jms.util.JMSUtil;
import org.apache.cxf.transport.jms.util.MessageListenerContainer;
import org.apache.cxf.transport.jms.util.PollingMessageListenerContainer;
import org.apache.cxf.transport.jms.util.ResourceCloser;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * JMSConduit is instantiated by the JMSTransportFactory which is selected by a client if the transport
 * protocol starts with "jms:". JMSConduit converts CXF Messages to JMS Messages and sends the request
 * over a queue or a topic.
 * If the Exchange is not one way it then receives the response and converts it to
 * a CXF Message. This is then provided in the Exchange and also sent to the IncomingObserver.
 */
public class JMSConduit extends AbstractConduit implements JMSExchangeSender, MessageListener {

    static final Logger LOG = LogUtils.getL7dLogger(JMSConduit.class);

    private static final String CORRELATED = JMSConduit.class.getName() + ".correlated";

    private JMSConfiguration jmsConfig;
    private Map<String, Exchange> correlationMap = new ConcurrentHashMap<>();
    private JMSListenerContainer jmsListener;
    private String conduitId;
    private final AtomicLong messageCount = new AtomicLong(0);
    private JMSBusLifeCycleListener listener;
    private Bus bus;
    private volatile Connection connection;
    private volatile Destination staticReplyDestination;

    public JMSConduit(EndpointReferenceType target,
                      JMSConfiguration jmsConfig,
                      Bus b) {
        super(target);
        bus = b;
        this.jmsConfig = jmsConfig;
        conduitId = UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * Prepare the message to be sent. The message will be sent after the caller has written the payload to
     * the OutputStream of the message and called the stream's close method. In the JMS case the
     * JMSOutputStream will then call back the sendExchange method of this class. {@inheritDoc}
     */
    public void prepare(final Message message) throws IOException {
        boolean isTextPayload = JMSConstants.TEXT_MESSAGE_TYPE.equals(jmsConfig.getMessageType());
        MessageStreamUtil.prepareStream(message, isTextPayload, this);
    }

    @Override
    public void close(Message msg) throws IOException {
        MessageStreamUtil.closeStreams(msg);
        super.close(msg);
    }
    private Connection getConnection() throws JMSException {
        Connection result = connection;
        if (result == null) {
            synchronized (this) {
                result = connection;
                if (result == null) {
                    result = JMSFactory.createConnection(jmsConfig);
                    trySetExListener(result);
                    result.start();
                    connection = result;
                }
            }
        }
        return result;
    }

    /**
     * Register exception listener to react faster when a connection is reset.  
     * 
     * @param conn
     */
    private void trySetExListener(Connection conn) {
        try {
            conn.setExceptionListener(new ExceptionListener() {
                
                @Override
                public void onException(JMSException exception) {
                    jmsConfig.resetCachedReplyDestination();
                    staticReplyDestination = null;
                }
            });
        } catch (JMSException e) {
            // setException is not supported on all providers  
        }
    }

    /**
     * Send the JMS message and if the MEP is not oneway receive the response.
     *
     * @param exchange the Exchange containing the outgoing message
     * @param request  the payload of the outgoing JMS message
     */
    public void sendExchange(final Exchange exchange, final Object request) {
        LOG.log(Level.FINE, "JMSConduit send message");

        final Message outMessage = exchange.getOutMessage() == null
            ? exchange.getOutFaultMessage()
            : exchange.getOutMessage();
        if (outMessage == null) {
            throw new RuntimeException("Exchange to be sent has no outMessage");
        }

        jmsConfig.ensureProperlyConfigured();
        assertIsNotTextMessageAndMtom(outMessage);

        try (ResourceCloser closer = new ResourceCloser()) {
            Connection c;

            if (jmsConfig.isOneSessionPerConnection()) {
                c = closer.register(JMSFactory.createConnection(jmsConfig));
                c.start();
            } else {
                c = getConnection();
            }

            Session session = closer.register(c.createSession(false, 
                                                              Session.AUTO_ACKNOWLEDGE));

            if (exchange.isOneWay()) {
                sendMessage(request, outMessage, null, null, closer, session);
            } else {
                sendAndReceiveMessage(exchange, request, outMessage, closer, session);
            }
        } catch (JMSException e) {
            if (this.jmsListener != null) {
                this.jmsListener.shutdown();
            }
            this.jmsListener = null;
            // Close connection so it will be refreshed on next try
            if (!jmsConfig.isOneSessionPerConnection()) {
                if (exchange.get(JMSUtil.JMS_MESSAGE_CONSUMER) != null) {
                    ResourceCloser.close(exchange.get(JMSUtil.JMS_MESSAGE_CONSUMER));
                }
                ResourceCloser.close(connection);
                this.connection = null;
                jmsConfig.resetCachedReplyDestination();
            }
            this.staticReplyDestination = null;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                // Ignore
            }
            throw JMSUtil.convertJmsException(e);
        }
    }

    private void setupReplyDestination(Session session) throws JMSException {
        if (staticReplyDestination == null) {
            synchronized (this) {
                if (staticReplyDestination == null) {
                    staticReplyDestination = jmsConfig.getReplyDestination(session);

                    String messageSelector = JMSFactory.getMessageSelector(jmsConfig, conduitId);
                    if (jmsConfig.getMessageSelector() != null) {
                        messageSelector += (messageSelector != null && !messageSelector.isEmpty() ? " AND " : "")
                                + jmsConfig.getMessageSelector();
                    }
                    if (messageSelector == null && !jmsConfig.isPubSubDomain()) {
                        // Do not open listener without selector on a queue as we then can not share the queue.
                        // An option for this might be a good idea for people who do not plan to share queues.
                        return;
                    }

                    AbstractMessageListenerContainer container;

                    if (jmsConfig.isOneSessionPerConnection()) {
                        container = new PollingMessageListenerContainer(jmsConfig, true, this);
                    } else {
                        container = new MessageListenerContainer(getConnection(), staticReplyDestination, this);
                    }

                    container.setTransactionManager(jmsConfig.getTransactionManager());
                    container.setTransacted(jmsConfig.isSessionTransacted());
                    container.setDurableSubscriptionName(jmsConfig.getDurableSubscriptionName());
                    container.setMessageSelector(messageSelector);
                    Object executor = bus.getProperty(JMSFactory.JMS_CONDUIT_EXECUTOR);
                    if (executor instanceof Executor) {
                        container.setExecutor((Executor) executor);
                    }
                    container.start();
                    jmsListener = container;
                    addBusListener();
                }
            }
        }
    }

    private void sendAndReceiveMessage(final Exchange exchange, final Object request, final Message outMessage,
                                ResourceCloser closer,
                                Session session) throws JMSException {

        setupReplyDestination(session);

        JMSMessageHeadersType headers = getOrCreateJmsHeaders(outMessage);
        String userCID = headers.getJMSCorrelationID();
        assertIsNotAsyncAndUserCID(exchange, userCID);
        String correlationId = createCorrelationId(exchange, userCID);
        if (correlationId != null) {
            correlationMap.put(correlationId, exchange);
        }

        // Synchronize on exchange early to make sure we do not miss the notify
        synchronized (exchange) {
            String replyTo = headers.getJMSReplyTo();
            String jmsMessageID = sendMessage(request, outMessage,
                                              jmsConfig.getReplyToDestination(session, replyTo),
                                              correlationId, closer, session);
            Destination replyDestination = jmsConfig.getReplyDestination(session, replyTo);
            boolean useSyncReceive = ((correlationId == null || userCID != null) && !jmsConfig.isPubSubDomain())
                || !replyDestination.equals(staticReplyDestination);
            if (correlationId == null) {
                correlationId = jmsMessageID;
                correlationMap.put(correlationId, exchange);
            }

            if (!exchange.isSynchronous()) {
                return;
            }

            try {
                if (useSyncReceive) {
                    exchange.put(JMSUtil.JMS_IGNORE_TIMEOUT, this.jmsConfig.isIgnoreTimeoutException());
                    javax.jms.Message replyMessage = JMSUtil.receive(session, replyDestination,
                                                                     correlationId,
                                                                     jmsConfig.getReceiveTimeout(),
                                                                     jmsConfig.isPubSubNoLocal(),
                                                                     exchange);
                    processReplyMessage(exchange, replyMessage);
                } else {
                    try {
                        exchange.wait(jmsConfig.getReceiveTimeout());
                    } catch (InterruptedException e) {
                        throw new JMSException("Interrupted while correlating " +  e.getMessage());
                    }
                    if (!Boolean.TRUE.equals(exchange.get(CORRELATED))) {
                        if (this.jmsConfig.isIgnoreTimeoutException()) {
                            throw new RuntimeException("Timeout receiving message with correlationId "
                                + correlationId);
                        } else {
                            throw new JMSException("Timeout receiving message with correlationId "
                                                   + correlationId);
                        }
                    }

                }
            } finally {
                correlationMap.remove(correlationId);
            }
        }
    }

    private String sendMessage(final Object request, final Message outMessage,
                               Destination replyToDestination, String correlationId,
                               ResourceCloser closer, Session session) throws JMSException {
        JMSMessageHeadersType headers = getOrCreateJmsHeaders(outMessage);
        javax.jms.Message message = JMSMessageUtils.asJMSMessage(jmsConfig,
                                                                 outMessage,
                                                                 request,
                                                                 jmsConfig.getMessageType(),
                                                                 session,
                                                                 correlationId,
                                                                 JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
        if (replyToDestination == null && headers.isSetJMSReplyTo()) {
            String replyTo = headers.getJMSReplyTo();
            replyToDestination = jmsConfig.getReplyDestination(session, replyTo);
        }
        if (replyToDestination != null) {
            message.setJMSReplyTo(replyToDestination);
        }

        JMSSender sender = JMSFactory.createJmsSender(jmsConfig, headers);

        Destination targetDest = jmsConfig.getTargetDestination(session);
        sender.sendMessage(session, targetDest, message);
        String jmsMessageID = message.getJMSMessageID();
        LOG.log(Level.FINE, "client sending request message "
            + jmsMessageID + " to " + targetDest);
        headers.setJMSMessageID(jmsMessageID);
        return jmsMessageID;
    }

    private void assertIsNotAsyncAndUserCID(Exchange exchange, String userCID) {
        if (!exchange.isSynchronous() && userCID != null) {
            throw new IllegalArgumentException("User CID can not be used for asynchronous exchanges");
        }
    }

    private void assertIsNotTextMessageAndMtom(final Message outMessage) {
        boolean isTextPayload = JMSConstants.TEXT_MESSAGE_TYPE.equals(jmsConfig.getMessageType());
        if (isTextPayload && MessageUtils.getContextualBoolean(outMessage, org.apache.cxf.message.Message.MTOM_ENABLED)
            && outMessage.getAttachments() != null && outMessage.getAttachments().size() > 0) {
            org.apache.cxf.common.i18n.Message msg =
                new org.apache.cxf.common.i18n.Message("INVALID_MESSAGE_TYPE", LOG);
            throw new ConfigurationException(msg);
        }
    }

    private String createCorrelationId(final Exchange exchange, String userCID) {
        if (userCID != null) {
            return userCID;
        } else if (!jmsConfig.isSetConduitSelectorPrefix() && !jmsConfig.isReplyPubSubDomain()
                   && exchange.isSynchronous()
                   && (!jmsConfig.isUseConduitIdSelector())) {
            // in this case the correlation id will be set to
            // the message id later
            return null;
        } else {
            String prefix = (jmsConfig.isUseConduitIdSelector()) ? jmsConfig.getConduitSelectorPrefix()
                                                                   + conduitId : jmsConfig.getConduitSelectorPrefix();
            return JMSUtil.createCorrelationId(prefix, messageCount.incrementAndGet());
        }
    }

    private JMSMessageHeadersType getOrCreateJmsHeaders(final Message outMessage) {
        JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
        if (headers == null) {
            headers = new JMSMessageHeadersType();
            outMessage.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, headers);
        }
        return headers;
    }

    static class JMSBusLifeCycleListener implements BusLifeCycleListener {
        final WeakReference<JMSConduit> ref;
        BusLifeCycleManager blcm;
        JMSBusLifeCycleListener(JMSConduit c, BusLifeCycleManager b) {
            ref = new WeakReference<>(c);
            blcm = b;
            blcm.registerLifeCycleListener(this);
        }

        public void initComplete() {
        }

        public void postShutdown() {
        }

        public void preShutdown() {
            unreg();
            blcm = null;
            JMSConduit c = ref.get();
            if (c != null) {
                c.listener = null;
                c.close();
            }
        }
        public void unreg() {
            if (blcm != null) {
                blcm.unregisterLifeCycleListener(this);
            }
        }
    }
    private synchronized void addBusListener() {
        if (listener == null && bus != null) {
            BusLifeCycleManager blcm = bus.getExtension(BusLifeCycleManager.class);
            if (blcm != null) {
                listener = new JMSBusLifeCycleListener(this,
                                                       blcm);
            }
        }
    }

    /**
     * When a message is received on the reply destination the correlation map is searched for the
     * correlationId. If it is found the message is converted to a CXF message and the thread sending the
     * request is notified {@inheritDoc}
     */
    public void onMessage(javax.jms.Message jmsMessage) {
        try {
            String correlationId = jmsMessage.getJMSCorrelationID();
            LOG.log(Level.FINE, "Received reply message with correlation id " + correlationId);

            Exchange exchange = getExchange(correlationId);
            if (exchange == null) {
                LOG.log(Level.WARNING, "Could not correlate message with correlationId " + correlationId);
            } else {
                processReplyMessage(exchange, jmsMessage);
            }
        } catch (JMSException e) {
            throw JMSUtil.convertJmsException(e);
        }

    }

    /**
     *  Try to correlate the incoming message with some timeout as it may have been
     *  added to the map after the message was sent
     *  
     * @param correlationId
     * @return exchange for correlationId or null if none was found
     */
    private Exchange getExchange(String correlationId) {
        int count = 0;
        Exchange exchange = null;
        while (exchange == null && count < 100) {
            exchange = correlationMap.remove(correlationId);
            if (exchange == null) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while correlating", e);
                }
            }
            count++;
        }
        return exchange;
    }

    /**
     * Process the reply message
     * @throws JMSException
     */
    protected void processReplyMessage(Exchange exchange, javax.jms.Message jmsMessage) throws JMSException {

        LOG.log(Level.FINE, "client received reply: ", jmsMessage);
        try {
            Message inMessage = JMSMessageUtils.asCXFMessage(jmsMessage,
                                                             JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
            if (jmsConfig.isCreateSecurityContext()) {
                SecurityContext securityContext = SecurityContextFactory.buildSecurityContext(jmsMessage, jmsConfig);
                inMessage.put(SecurityContext.class, securityContext);
            }
            exchange.setInMessage(inMessage);
            Object responseCode = inMessage.get(org.apache.cxf.message.Message.RESPONSE_CODE);
            exchange.put(org.apache.cxf.message.Message.RESPONSE_CODE, responseCode);

            if (exchange.isSynchronous()) {
                synchronized (exchange) {
                    exchange.put(CORRELATED, Boolean.TRUE);
                    exchange.notifyAll();
                }
            }

            if (incomingObserver != null) {
                incomingObserver.onMessage(exchange.getInMessage());
            }
        } catch (UnsupportedEncodingException ex) {
            getLogger().log(Level.WARNING, "can't get the right encoding information " + ex);
        }
    }

    private synchronized void shutdownListeners() {
        if (listener != null) {
            listener.unreg();
            listener = null;
        }
        if (jmsListener != null) {
            jmsListener.stop();
            jmsListener.shutdown();
            jmsListener = null;
            staticReplyDestination = null;
        }
    }
    public synchronized void close() {
        shutdownListeners();
        ResourceCloser.close(connection);
        connection = null;
        LOG.log(Level.FINE, "JMSConduit closed ");
    }

    protected Logger getLogger() {
        return LOG;
    }

    public JMSConfiguration getJmsConfig() {
        return jmsConfig;
    }

    public void setJmsConfig(JMSConfiguration jmsConfig) {
        this.jmsConfig = jmsConfig;
    }

    protected static boolean isSetReplyTo(Message message) {
        Boolean ret = (Boolean)message.get(JMSConstants.JMS_SET_REPLY_TO);
        return ret == null || ret.booleanValue();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}
