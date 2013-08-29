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
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.ConfigurationException;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.JmsUtils;

/**
 * JMSConduit is instantiated by the JMSTransportFactory which is selected by a client if the transport
 * protocol starts with jms://. JMSConduit converts CXF Messages to JMS Messages and sends the request by 
 * using a JMS destination. If the Exchange is not oneway it then recevies the response and converts it to 
 * a CXF Message. This is then provided in the Exchange and also sent to the incomingObserver.
 */
public class JMSConduit extends AbstractConduit implements JMSExchangeSender, MessageListener {

    static final Logger LOG = LogUtils.getL7dLogger(JMSConduit.class);
    
    private static final String CORRELATED = JMSConduit.class.getName() + ".correlated";
    
    private EndpointInfo endpointInfo;
    private JMSConfiguration jmsConfig;
    private Map<String, Exchange> correlationMap = new ConcurrentHashMap<String, Exchange>();
    private DefaultMessageListenerContainer jmsListener;
    private DefaultMessageListenerContainer allListener;
    private String conduitId;
    private AtomicLong messageCount;
    private JMSBusLifeCycleListener listener;
    private Bus bus;

    public JMSConduit(EndpointInfo endpointInfo,
                      EndpointReferenceType target,
                      JMSConfiguration jmsConfig,
                      Bus b) {
        super(target);
        bus = b;
        this.jmsConfig = jmsConfig;
        this.endpointInfo = endpointInfo;
        conduitId = UUID.randomUUID().toString().replaceAll("-", "");
        messageCount = new AtomicLong(0);
    }
    
    /**
     * Prepare the message to be sent. The message will be sent after the caller has written the payload to
     * the OutputStream of the message and called the stream's close method. In the JMS case the
     * JMSOutputStream will then call back the sendExchange method of this class. {@inheritDoc}
     */
    public void prepare(final Message message) throws IOException {
        String name =  endpointInfo.getName().toString() + ".jms-conduit";
        org.apache.cxf.common.i18n.Message msg = 
            new org.apache.cxf.common.i18n.Message("INSUFFICIENT_CONFIGURATION_CONDUIT", LOG, name);
        jmsConfig.ensureProperlyConfigured(msg);
        boolean isTextPayload = JMSConstants.TEXT_MESSAGE_TYPE.equals(jmsConfig.getMessageType());
        if (isTextPayload) {
            message.setContent(Writer.class, new StringWriter() {
                @Override
                public void close() throws IOException {
                    super.close();
                    sendExchange(message.getExchange(), toString());
                }
            });
        } else {
            JMSOutputStream out = new JMSOutputStream(this, message.getExchange(), isTextPayload);
            message.setContent(OutputStream.class, out);
        }
    }
    @Override
    public void close(Message msg) throws IOException {
        Writer writer = msg.getContent(Writer.class);
        if (writer != null) {
            writer.close();
        }
        Reader reader = msg.getContent(Reader.class);
        if (reader != null) {
            reader.close();
        }
        super.close(msg);
    }
    private synchronized AbstractMessageListenerContainer getJMSListener() {
        if (jmsListener == null) {
            jmsListener = JMSFactory.createJmsListener(jmsConfig, 
                                                       this, 
                                                       jmsConfig.getReplyDestination(), 
                                                       conduitId, 
                                                       false);
            addBusListener();
        }
        return jmsListener;
    }
    private synchronized AbstractMessageListenerContainer getAllListener() {
        if (allListener == null) {
            allListener = JMSFactory.createJmsListener(jmsConfig, 
                                                       this, 
                                                       jmsConfig.getReplyDestination(), 
                                                       null, 
                                                       true);
            addBusListener();
        }
        return allListener;
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
        
        boolean isTextPayload = JMSConstants.TEXT_MESSAGE_TYPE.equals(jmsConfig.getMessageType());
        if (isTextPayload && MessageUtils.isTrue(outMessage.getContextualProperty(
            org.apache.cxf.message.Message.MTOM_ENABLED)) 
            && outMessage.getAttachments() != null && outMessage.getAttachments().size() > 0) {
            org.apache.cxf.common.i18n.Message msg = 
                new org.apache.cxf.common.i18n.Message("INVALID_MESSAGE_TYPE", LOG);
            throw new ConfigurationException(msg);
        }
        
        JMSMessageHeadersType headers = getOrCreateJmsHeaders(outMessage);
        String replyTo = headers.getJMSReplyTo();
        if (replyTo == null) {
            replyTo = jmsConfig.getReplyDestination();
        }
        final JmsTemplate jmsTemplate = JMSFactory.createJmsTemplate(jmsConfig, headers);
        
        String userCID = headers.getJMSCorrelationID();

        String correlationId = createCorrelationId(exchange, userCID);
        
        Destination replyToDestination = null;
        if (!exchange.isOneWay() || !jmsConfig.isEnforceSpec() && isSetReplyTo(outMessage)
            && replyTo != null) {
            if (!jmsConfig.isReplyPubSubDomain()
                && (exchange.isSynchronous() 
                    || exchange.isOneWay())) {
                replyToDestination = JMSFactory.resolveOrCreateDestination(jmsTemplate, replyTo,
                                                                           jmsConfig.isReplyPubSubDomain());
            } else {
                if (userCID == null || !jmsConfig.isUseConduitIdSelector()) { 
                    replyToDestination = getJMSListener().getDestination();
                } else {
                    replyToDestination = getAllListener().getDestination();
                }
            }
        }

        final String cid = correlationId; 
        final Destination rtd = replyToDestination;
        class JMSConduitMessageCreator implements MessageCreator {
            private javax.jms.Message jmsMessage;

            public javax.jms.Message createMessage(Session session) throws JMSException {
                String messageType = jmsConfig.getMessageType();
                Destination destination = rtd;
                String replyToAddress = jmsConfig.getReplyToDestination();
                if (rtd == null && replyToAddress != null) {
                    destination = JMSFactory.resolveOrCreateDestination(jmsTemplate, replyToAddress,
                                                                        jmsConfig.isPubSubDomain());
                }
                jmsMessage = JMSUtils.buildJMSMessageFromCXFMessage(jmsConfig, outMessage, request,
                                                                    messageType, session, destination,
                                                                    cid);
                if ((jmsConfig.isReplyPubSubDomain() || !exchange.isSynchronous()) && !exchange.isOneWay()) {
                    correlationMap.put(cid, exchange);
                }
                LOG.log(Level.FINE, "client sending request: ", jmsMessage);
                return jmsMessage;
            }

            public String getMessageID() {
                if (jmsMessage != null) {
                    try {
                        return jmsMessage.getJMSMessageID();
                    } catch (JMSException e) {
                        return null;
                    }
                }
                return null;
            }
        }
        JMSConduitMessageCreator messageCreator = new JMSConduitMessageCreator();    
        /**
         * If the message is not oneWay we will expect to receive a reply on the listener. 
         * 
         */
        if (!exchange.isOneWay()) {
            synchronized (exchange) {
                jmsTemplate.send(jmsConfig.getTargetDestination(), messageCreator);
                if (correlationId == null) {
                    correlationId = messageCreator.getMessageID();
                }
                headers.setJMSMessageID(messageCreator.getMessageID());

                final String messageSelector = "JMSCorrelationID = '" + correlationId + "'";
                if (exchange.isSynchronous() && !jmsConfig.isReplyPubSubDomain()) {
                    javax.jms.Message replyMessage = jmsTemplate.receiveSelected(replyToDestination,
                                                                                 messageSelector);
                    if (replyMessage == null) {
                        throw new RuntimeException("Timeout receiving message with correlationId "
                                                   + correlationId);
                    } else {
                        doReplyMessage(exchange, replyMessage);
                    }
                    
                    // TODO How do we delete the temp queue in case of an async request
                    // or is async with a temp queue not possible ?
                    if (replyToDestination instanceof TemporaryQueue) {
                        try {
                            ((TemporaryQueue)replyToDestination).delete();
                        } catch (JMSException e) {
                            // Only log the exception as the exchange should be able to proceed
                            LOG.log(Level.WARNING, "Unable to remove temporary queue: " + e.getMessage(), e);
                        }
                    }
                }
            }
        } else {
            jmsTemplate.send(jmsConfig.getTargetDestination(), messageCreator);
            headers.setJMSMessageID(messageCreator.getMessageID());
        }
    }

    private String createCorrelationId(final Exchange exchange, String userCID) {
        String correlationId = null;
        if (!exchange.isOneWay()) {
            if (userCID != null) {
                correlationId = userCID;
            } else if (!jmsConfig.isSetConduitSelectorPrefix()
                       && !jmsConfig.isReplyPubSubDomain()
                       && (exchange.isSynchronous() || exchange.isOneWay())
                       && (!jmsConfig.isSetUseConduitIdSelector() 
                           || !jmsConfig.isUseConduitIdSelector())) {
                // in this case the correlation id will be set to
                // the message id later
                correlationId = null;
            } else { 
                String prefix = (jmsConfig.isUseConduitIdSelector()) 
                    ? jmsConfig.getConduitSelectorPrefix() + conduitId 
                    : jmsConfig.getConduitSelectorPrefix();
                correlationId = JMSUtils.createCorrelationId(prefix, messageCount.incrementAndGet());
            }
        }
        return correlationId;
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
            ref = new WeakReference<JMSConduit>(c);
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
        String correlationId;
        try {
            correlationId = jmsMessage.getJMSCorrelationID();
        } catch (JMSException e) {
            throw JmsUtils.convertJmsAccessException(e);
        }

        Exchange exchange = correlationMap.remove(correlationId);
        if (exchange == null) {
            LOG.log(Level.WARNING, "Could not correlate message with correlationId " + correlationId);
            return;
        }
        doReplyMessage(exchange, jmsMessage);
    }

    /**
     * Process the reply message
     */
    public void doReplyMessage(Exchange exchange, javax.jms.Message jmsMessage) {
        Message inMessage = new MessageImpl();
        exchange.setInMessage(inMessage);
        LOG.log(Level.FINE, "client received reply: ", jmsMessage);
        try {
            JMSUtils.populateIncomingContext(jmsMessage, inMessage, 
                                             JMSConstants.JMS_CLIENT_RESPONSE_HEADERS, jmsConfig);
        
            JMSUtils.retrieveAndSetPayload(inMessage, jmsMessage, (String)inMessage.get(Message.ENCODING));

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

    public synchronized void shutdownListeners() {
        if (listener != null) {
            listener.unreg();
            listener = null;
        }
        if (jmsListener != null) {
            jmsListener.shutdown();
            jmsListener = null;
        }
        if (allListener != null) {
            allListener.shutdown();
            allListener = null;
        }        
    }
    public synchronized void close() {
        shutdownListeners();
        jmsConfig.destroyWrappedConnectionFactory();
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
        return ret == null || (ret != null && ret.booleanValue());
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

}
