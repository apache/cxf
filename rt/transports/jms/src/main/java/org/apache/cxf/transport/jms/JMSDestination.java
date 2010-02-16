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
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.OneWayProcessorInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.AbstractMultiplexDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.jms.continuations.JMSContinuation;
import org.apache.cxf.transport.jms.continuations.JMSContinuationProvider;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class JMSDestination extends AbstractMultiplexDestination 
    implements SessionAwareMessageListener, MessageListener, JMSExchangeSender {

    private static final Logger LOG = LogUtils.getL7dLogger(JMSDestination.class);

    private JMSConfiguration jmsConfig;
    private Bus bus;
    private EndpointInfo ei;
    private AbstractMessageListenerContainer jmsListener;
    private Collection<JMSContinuation> continuations = 
        new ConcurrentLinkedQueue<JMSContinuation>();

    public JMSDestination(Bus b, EndpointInfo info, JMSConfiguration jmsConfig) {
        super(b, getTargetReference(info, b), info);
        this.bus = b;
        this.ei = info;
        this.jmsConfig = jmsConfig;
        info.setProperty(OneWayProcessorInterceptor.USE_ORIGINAL_THREAD, Boolean.TRUE);
    }

    /**
     * @param inMessage the incoming message
     * @return the inbuilt backchannel
     */
    protected Conduit getInbuiltBackChannel(Message inMessage) {
        EndpointReferenceType anon = EndpointReferenceUtils.getAnonymousEndpointReference();
        return new BackChannelConduit(this, anon, inMessage);
    }

    /**
     * Initialize jmsTemplate and jmsListener from jms configuration data in jmsConfig {@inheritDoc}
     */
    public void activate() {
        getLogger().log(Level.FINE, "JMSDestination activate().... ");
        String name = endpointInfo.getName().toString() + ".jms-destination";
        org.apache.cxf.common.i18n.Message msg = 
            new org.apache.cxf.common.i18n.Message("INSUFFICIENT_CONFIGURATION_DESTINATION", LOG, name);
        jmsConfig.ensureProperlyConfigured(msg);
        jmsListener = JMSFactory.createJmsListener(ei, jmsConfig, this, 
                                                   jmsConfig.getTargetDestination(), null, false);
    }

    public void deactivate() {
        if (jmsListener != null) {
            jmsListener.shutdown();
        }
    }

    public void shutdown() {
        getLogger().log(Level.FINE, "JMSDestination shutdown()");
        this.deactivate();
    }

    private Destination resolveDestinationName(final JmsTemplate jmsTemplate, final String name) {
        SessionCallback sc = new SessionCallback() {
            public Object doInJms(Session session) throws JMSException {
                DestinationResolver resolv = jmsTemplate.getDestinationResolver();
                return resolv.resolveDestinationName(session, name, jmsConfig.isPubSubDomain());
            }
        };
        return (Destination)jmsTemplate.execute(sc);
    }

    public Destination getReplyToDestination(JmsTemplate jmsTemplate, Message inMessage) throws JMSException {
        javax.jms.Message message = (javax.jms.Message)inMessage.get(JMSConstants.JMS_REQUEST_MESSAGE);
        // If WS-Addressing had set the replyTo header.
        final String replyToName = (String)inMessage.get(JMSConstants.JMS_REBASED_REPLY_TO);
        if (replyToName != null) {
            return resolveDestinationName(jmsTemplate, replyToName);
        } else if (message.getJMSReplyTo() != null) {
            return message.getJMSReplyTo();
        } else if (!StringUtils.isEmpty(jmsConfig.getReplyDestination())) {
            return resolveDestinationName(jmsTemplate, jmsConfig.getReplyDestination());
        } else {
            throw new RuntimeException("No replyTo destination set on request message or cxf message");
        }
    }

    /**
     * Decides what correlationId to use for the reply by looking at the request headers. If the request has a
     * correlationId set this is taken. Else the messageId from the request message is used as correlation Id
     * 
     * @param request
     * @return
     * @throws JMSException
     */
    public String determineCorrelationID(javax.jms.Message request) throws JMSException {
        String correlationID = request.getJMSCorrelationID();
        if (correlationID == null || "".equals(correlationID)) {
            correlationID = request.getJMSMessageID();
        }
        return correlationID;
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
        onMessage(message, null);
    }
    public void onMessage(javax.jms.Message message, Session session) {
        try {
            getLogger().log(Level.FINE, "server received request: ", message);
             // Build CXF message from JMS message
            MessageImpl inMessage = new MessageImpl();            
            JMSUtils.populateIncomingContext(message, inMessage, 
                                             JMSConstants.JMS_SERVER_REQUEST_HEADERS, jmsConfig);
            
            byte[] request = JMSUtils.retrievePayload(message, (String)inMessage.get(Message.ENCODING));
            if (request != null) {
                getLogger().log(Level.FINE, "The Request Message is [ " + request + "]");
                inMessage.setContent(InputStream.class, new ByteArrayInputStream(request));
            }
            inMessage.put(JMSConstants.JMS_SERVER_RESPONSE_HEADERS, new JMSMessageHeadersType());
            inMessage.put(JMSConstants.JMS_REQUEST_MESSAGE, message);
            inMessage.setDestination(this);
            if (jmsConfig.getMaxSuspendedContinuations() != 0) {
                inMessage.put(ContinuationProvider.class.getName(), 
                              new JMSContinuationProvider(bus,
                                                          inMessage,
                                                          incomingObserver,
                                                          continuations,
                                                          jmsListener,
                                                          jmsConfig));
            }
            
            BusFactory.setThreadDefaultBus(bus);

            
            Map<Class<?>, ?> mp = JCATransactionalMessageListenerContainer.ENDPOINT_LOCAL.get();
            if (mp != null) {
                for (Map.Entry<Class<?>, ?> ent : mp.entrySet()) {
                    inMessage.setContent(ent.getKey(), ent.getValue());
                }
                JCATransactionalMessageListenerContainer.ENDPOINT_LOCAL.remove();
            }

            // handle the incoming message
            incomingObserver.onMessage(inMessage);
            
            //need to propagate any exceptions back to Spring container 
            //so transactions can occur
            if (inMessage.getContent(Exception.class) != null && session != null) {
                PlatformTransactionManager m = jmsConfig.getTransactionManager();
                if (m != null) {
                    TransactionStatus status = m.getTransaction(null);
                    JmsResourceHolder resourceHolder =
                        (JmsResourceHolder) TransactionSynchronizationManager
                            .getResource(jmsConfig.getConnectionFactory());
                    boolean trans = resourceHolder == null 
                        || !resourceHolder.containsSession(session);
                    if (status != null && !status.isCompleted() && trans) {
                        Exception ex = inMessage.getContent(Exception.class);
                        if (ex.getCause() instanceof RuntimeException) {
                            throw (RuntimeException)ex.getCause();
                        } else {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }
            
        } catch (SuspendedInvocationException ex) {
            getLogger().log(Level.FINE, "Request message has been suspended");
        } catch (UnsupportedEncodingException ex) {
            getLogger().log(Level.WARNING, "can't get the right encoding information. " + ex);
        } finally {
            BusFactory.setThreadDefaultBus(null);
        }
    }

    public void sendExchange(Exchange exchange, final Object replyObj) {
        if (exchange.isOneWay()) {
            //Don't need to send anything
            return;
        }
        Message inMessage = exchange.getInMessage();
        final Message outMessage = exchange.getOutMessage();
        if (jmsConfig.isPubSubDomain()) {
            // we will never receive a non-oneway invocation in pub-sub
            // domain from CXF client - however a mis-behaving pure JMS
            // client could conceivably make suce an invocation, in which
            // case we silently discard the reply
            getLogger().log(Level.WARNING, "discarding reply for non-oneway invocation ",
                            "with 'topic' destinationStyle");
            return;
        }
        try {
            final JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
                .get(JMSConstants.JMS_SERVER_RESPONSE_HEADERS);
            JMSMessageHeadersType inHeaders = (JMSMessageHeadersType)inMessage
                .get(JMSConstants.JMS_SERVER_REQUEST_HEADERS);
            JmsTemplate jmsTemplate = JMSFactory.createJmsTemplate(jmsConfig, inHeaders);

            // setup the reply message
            final javax.jms.Message request = (javax.jms.Message)inMessage
                .get(JMSConstants.JMS_REQUEST_MESSAGE);
            final String msgType;
            if (request instanceof TextMessage) {
                msgType = JMSConstants.TEXT_MESSAGE_TYPE;
            } else if (request instanceof BytesMessage) {
                msgType = JMSConstants.BYTE_MESSAGE_TYPE;
            } else {
                msgType = JMSConstants.BINARY_MESSAGE_TYPE;
            }

            Destination replyTo = getReplyToDestination(jmsTemplate, inMessage);

            if (request.getJMSExpiration() > 0) {
                TimeZone tz = new SimpleTimeZone(0, "GMT");
                Calendar cal = new GregorianCalendar(tz);
                long timeToLive = request.getJMSExpiration() - cal.getTimeInMillis();
                if (timeToLive < 0) {
                    getLogger()
                        .log(Level.INFO, "Message time to live is already expired skipping response.");
                    return;
                }
            }

            getLogger().log(Level.FINE, "send out the message!");
            jmsTemplate.send(replyTo, new MessageCreator() {
                public javax.jms.Message createMessage(Session session) throws JMSException {
                    javax.jms.Message reply = JMSUtils.createAndSetPayload(replyObj, session, msgType);

                    reply.setJMSCorrelationID(determineCorrelationID(request));

                    JMSUtils.setMessageProperties(headers, reply);
                    // ensure that the contentType is set to the out jms message header
                    JMSUtils.addContentTypeToProtocolHeader(outMessage);
                    Map<String, List<String>> protHeaders = CastUtils.cast((Map<?, ?>)outMessage
                        .get(Message.PROTOCOL_HEADERS));
                    JMSUtils.addProtocolHeaders(reply, protHeaders);

                    LOG.log(Level.FINE, "server sending reply: ", reply);
                    return reply;
                }
            });

        } catch (JMSException ex) {
            JmsUtils.convertJmsAccessException(ex);
        }
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

    /**
     * Conduit for sending the reply back to the client
     */
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
