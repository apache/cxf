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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.JmsTemplate102;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * JMSConduit is instantiated by the JMSTransportfactory which is selected by a client if the transport
 * protocol starts with jms:// JMSConduit converts CXF Messages to JMS Messages and sends the request by using
 * a JMS destination. If the Exchange is not oneway it then recevies the response and converts it to a CXF
 * Message. This is then provided in the Exchange and also sent to the incomingObserver
 */
public class JMSConduit extends AbstractConduit implements JMSExchangeSender, MessageListener,
    InitializingBean {
    static final Logger LOG = LogUtils.getL7dLogger(JMSConduit.class);

    private JMSConfiguration jmsConfig;
    private Map<String, Message> correlationMap;

    private DefaultMessageListenerContainer jmsListener;
    private JmsTemplate jmsTemplate;

    public JMSConduit(EndpointReferenceType target, JMSConfiguration jmsConfig) {
        super(target);
        this.jmsConfig = jmsConfig;
        correlationMap = new ConcurrentHashMap<String, Message>();
    }
    
    private Destination determineReplyToDestination(final JmsTemplate jmsTemplate2,
                                                   final String replyToDestinationName,
                                                   final boolean pubSubDomain) {
        return (Destination)jmsTemplate2.execute(new SessionCallback() {
            public Object doInJms(Session session) throws JMSException {
                if (replyToDestinationName == null) {
                    if (session instanceof QueueSession) {
                        // For JMS 1.0.2
                        return ((QueueSession)session).createTemporaryQueue();
                    } else {
                        // For JMS 1.1
                        return session.createTemporaryQueue();
                    }
                }
                DestinationResolver resolv = jmsTemplate2.getDestinationResolver();
                return resolv.resolveDestinationName(session, replyToDestinationName, pubSubDomain);
            }
        });
    }

    /**
     * Initialize jmsTemplate and jmsListener from jms configuration data in jmsConfig {@inheritDoc}
     */
    public void afterPropertiesSet() {
        jmsTemplate = jmsConfig.isUseJms11() ? new JmsTemplate() : new JmsTemplate102();
        jmsTemplate.setDefaultDestinationName(jmsConfig.getTargetDestination());
        jmsTemplate.setConnectionFactory(jmsConfig.getConnectionFactory());
        jmsTemplate.setPubSubDomain(jmsConfig.isPubSubDomain());
        jmsTemplate.setReceiveTimeout(jmsConfig.getReceiveTimeout());
        jmsTemplate.setTimeToLive(jmsConfig.getTimeToLive());
        jmsTemplate.setPriority(jmsConfig.getPriority());
        jmsTemplate.setDeliveryMode(jmsConfig.getDeliveryMode());
        jmsTemplate.setExplicitQosEnabled(jmsConfig.isExplicitQosEnabled());
        jmsTemplate.setSessionTransacted(jmsConfig.isSessionTransacted());

        jmsListener = new DefaultMessageListenerContainer();
        jmsListener.setPubSubDomain(jmsConfig.isPubSubDomain());
        jmsListener.setAutoStartup(false);
        jmsListener.setConnectionFactory(jmsConfig.getConnectionFactory());
        jmsListener.setMessageSelector(jmsConfig.getMessageSelector());
        jmsListener.setDurableSubscriptionName(jmsConfig.getDurableSubscriptionName());
        jmsListener.setSessionTransacted(jmsConfig.isSessionTransacted());
        jmsListener.setTransactionManager(jmsConfig.getTransactionManager());
        
        jmsListener.setMessageListener(this);

        if (jmsConfig.getDestinationResolver() != null) {
            jmsTemplate.setDestinationResolver(jmsConfig.getDestinationResolver());
            jmsListener.setDestinationResolver(jmsConfig.getDestinationResolver());
        }
    }

    /**
     * Prepare the message for send out. The message will be sent after the caller has written the payload to
     * the OutputStream of the message and calls the close method of the stream. In the JMS case the
     * JMSOutputStream will then call back the sendExchange method of this class. {@inheritDoc}
     */
    public void prepare(Message message) throws IOException {
        boolean isTextPayload = JMSConstants.TEXT_MESSAGE_TYPE.equals(jmsConfig.getMessageType());
        JMSOutputStream out = new JMSOutputStream(this, message.getExchange(), isTextPayload);
        message.setContent(OutputStream.class, out);
    }

    /**
     * Send the JMS Request out and if not oneWay receive the response
     * 
     * @param outMessage
     * @param request
     * @return inMessage
     */
    public void sendExchange(final Exchange exchange, final Object request) {
        LOG.log(Level.FINE, "JMSConduit send message");
        final Message outMessage = exchange.getOutMessage();
        if (outMessage == null) {
            throw new RuntimeException("Exchange to be sent has no outMessage");
        }
        
        if (!exchange.isOneWay() && !jmsListener.isRunning()) {
            Destination replyTo = determineReplyToDestination(jmsTemplate, 
                                                              jmsConfig.getReplyDestination(), 
                                                              jmsConfig.isPubSubDomain());
            jmsListener.setDestination(replyTo);
            jmsListener.start();
            jmsListener.initialize();
        }

        JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
        final String correlationId = (headers != null && headers.isSetJMSCorrelationID()) ? headers
            .getJMSCorrelationID() : JMSUtils.generateCorrelationId();
        // String selector = "JMSCorrelationID = '" + correlationId + "'";

        jmsTemplate.send(new MessageCreator() {
            public javax.jms.Message createMessage(Session session) throws JMSException {
                String messageType = jmsConfig.getMessageType();
                final javax.jms.Message jmsMessage;
                jmsMessage = JMSUtils.buildJMSMessageFromCXFMessage(outMessage, request, messageType,
                                                                    session, jmsListener.getDestination(),
                                                                    correlationId);
                LOG.log(Level.FINE, "client sending request: ", jmsMessage);
                return jmsMessage;
            }
        });

        /**
         * If the message is not oneWay we will expect to receive a reply on the listener.
         * To receive this reply we add the correlationId and an empty CXF Message to the
         * correlationMap. The listener will fill to Message and notify this thread
         */
        if (!exchange.isOneWay()) {
            Message inMessage = new MessageImpl();
            synchronized (inMessage) {
                correlationMap.put(correlationId, inMessage);
                try {
                    inMessage.wait(jmsTemplate.getReceiveTimeout());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                correlationMap.remove(correlationId);
            }
            exchange.setInMessage(inMessage);
            if (incomingObserver != null) {
                incomingObserver.onMessage(inMessage);
            }      
        }
    }

    /**
     * When a message is received on the reply destination the correlation map is searched
     * for the correlationId. If it is found the message is converted to a CXF message and the
     * thread sending the request is notified
     * 
     * {@inheritDoc}
     */
    public void onMessage(javax.jms.Message jmsMessage) {
        String correlationId;
        try {
            correlationId = jmsMessage.getJMSCorrelationID();
        } catch (JMSException e) {
            throw JmsUtils.convertJmsAccessException(e);
        }
        Message inMessage = correlationMap.get(correlationId);
        if (inMessage == null) {
            LOG.log(Level.WARNING, "Could not correlate message with correlationId " + correlationId);
        }
        LOG.log(Level.FINE, "client received reply: ", jmsMessage);
        JMSUtils.populateIncomingContext(jmsMessage, inMessage, JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
        byte[] response = JMSUtils.retrievePayload(jmsMessage);
        LOG.log(Level.FINE, "The Response Message payload is : [" + response + "]");
        inMessage.setContent(InputStream.class, new ByteArrayInputStream(response));
        
        synchronized (inMessage) {
            inMessage.notifyAll();
        }

    }

    public void close() {
        jmsListener.shutdown();
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

    @Override
    protected void finalize() throws Throwable {
        if (jmsListener.isRunning()) {
            jmsListener.shutdown();
        }
        super.finalize();
    }
    
    

}
