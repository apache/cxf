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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.naming.Context;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * JMSConduit is instantiated by the JMSTransportfactory which is selected by a client if the transport
 * protocol starts with jms:// JMSConduit converts CXF Messages to JMS Messages and sends the request by using
 * JMS topics or queues. If the Exchange is not oneway it then recevies the response and converts it to a CXF
 * Message. This is then provided in the Exchange and also sent to the incomingObserver
 */
public class JMSConduit extends AbstractConduit implements Configurable, JMSExchangeSender {

    protected static final String BASE_BEAN_NAME_SUFFIX = ".jms-conduit-base";

    static final Logger LOG = LogUtils.getL7dLogger(JMSConduit.class);

    protected Destination targetDestination;
    protected JMSSessionFactory sessionFactory;
    protected Bus bus;
    protected EndpointInfo endpointInfo;
    protected String beanNameSuffix;

    protected ClientConfig clientConfig;
    protected ClientBehaviorPolicyType runtimePolicy;
    protected AddressType address;
    protected SessionPoolType sessionPool;

    private Queue replyDestination;

    private Context context;

    public JMSConduit(Bus b, EndpointInfo endpointInfo) {
        this(b, endpointInfo, null);
    }

    public JMSConduit(Bus b, EndpointInfo endpointInfo, EndpointReferenceType target) {
        super(target);
        this.bus = b;
        this.endpointInfo = endpointInfo;
        this.beanNameSuffix = BASE_BEAN_NAME_SUFFIX;
        initConfig();
    }

    private void initConfig() {
        this.address = endpointInfo.getTraversedExtensor(new AddressType(), AddressType.class);
        this.sessionPool = endpointInfo.getTraversedExtensor(new SessionPoolType(), SessionPoolType.class);
        this.clientConfig = endpointInfo.getTraversedExtensor(new ClientConfig(), ClientConfig.class);
        this.runtimePolicy = endpointInfo.getTraversedExtensor(new ClientBehaviorPolicyType(),
                                                               ClientBehaviorPolicyType.class);

        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(this);
        }
    }

    public JMSSessionFactory getOrCreateSessionFactory() {
        if (this.sessionFactory == null) {
            try {
                this.context = JMSUtils.getInitialContext(address);
                this.sessionFactory = JMSSessionFactory
                    .connect(getJMSAddress(), getSessionPool(), null);
                this.targetDestination = JMSUtils.resolveRequestDestination(sessionFactory
                    .getInitialContext(), sessionFactory.getConnection(), address);
                this.replyDestination = JMSUtils.resolveReplyDestination(context, sessionFactory
                    .getConnection(), address);
            } catch (Exception jmsex) {
                throw new RuntimeException("JMS connect failed: ", jmsex);
            }
        }
        if (this.targetDestination == null) {
            throw new RuntimeException("Failed to lookup or create requestDestination");
        }
        return this.sessionFactory;
    }

    // prepare the message for send out , not actually send out the message
    public void prepare(Message message) throws IOException {
        if (this.address == null || this.address.getJndiConnectionFactoryName() == null) {
            throw new RuntimeException("Insufficient configuration for Conduit. "
                + "Did you configure a <jms:conduit name=\"" 
                + getBeanName() + "\"> and set the jndiConnectionFactoryName ?");
        }
        message.setContent(OutputStream.class, new JMSOutputStream(this, 
            message.getExchange(), isTextPayload()));
        // After this step flow will continue in JMSOutputStream.doClose()
    }

    /**
     * Send the JMS Request out and if not oneWay receive the response
     * 
     * @param outMessage
     * @param request
     * @return inMessage
     */
    public void sendExchange(Exchange exchange, Object request) {
        LOG.log(Level.FINE, "JMSConduit send message");

        sessionFactory = getOrCreateSessionFactory();
        PooledSession pooledSession = null;
        try {
            pooledSession = sessionFactory.get();
            Destination replyTo = null;
            if (!exchange.isOneWay()) {
                pooledSession.initConsumerAndReplyDestination(replyDestination);
                replyTo = pooledSession.getReplyDestination();
            }

            // TODO setting up the responseExpected

            // We don't want to send temp queue in
            // replyTo header for oneway calls
            if (exchange.isOneWay() && (getJMSAddress().getJndiReplyDestinationName() == null)) {
                replyTo = null;
            }
            Message outMessage = exchange.getOutMessage();
            if (outMessage == null) {
                throw new RuntimeException("Exchange to be sent has no outMessage");
            }
            sendMessage(outMessage, request, pooledSession, replyTo);

            if (!exchange.isOneWay()) {
                long receiveTimeout = clientConfig.getClientReceiveTimeout();
                Long messageReceiveTimeout = (Long)exchange.getOutMessage()
                    .get(JMSConstants.JMS_CLIENT_RECEIVE_TIMEOUT);
                if (messageReceiveTimeout != null) {
                    receiveTimeout = messageReceiveTimeout.longValue();
                }
                Message inMessage = receiveResponse(pooledSession.consumer(), receiveTimeout);
                exchange.setInMessage(inMessage);
                incomingObserver.onMessage(inMessage);
            }
        } finally {
            sessionFactory.recycle(pooledSession);
        }
    }

    private void sendMessage(Message outMessage, Object request, PooledSession pooledSession,
                             Destination replyTo) {
        try {
            String messageType = runtimePolicy.getMessageType().value();
            javax.jms.Message jmsMessage;
            jmsMessage = JMSUtils.buildJMSMessageFromCXFMessage(outMessage, request, messageType,
                                                                pooledSession.session(), replyTo,
                                                                pooledSession.getCorrelationID());

            // Retrieve JMS QoS parameters from CXF message headers
            JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
                .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
            long ttl = JMSUtils.getTimeToLive(headers);
            if (ttl <= 0) {
                ttl = clientConfig.getMessageTimeToLive();
            }
            int deliveryMode = JMSUtils.getJMSDeliveryMode(headers);
            int priority = JMSUtils.getJMSPriority(headers);

            LOG.log(Level.FINE, "client sending request: ", jmsMessage);
            JMSUtils.sendMessage(pooledSession.producer(), targetDestination, jmsMessage, ttl, deliveryMode,
                                 priority);
        } catch (JMSException e) {
            throw new RuntimeException("Problem while sending JMS message", e);
        }
    }

    private Message receiveResponse(MessageConsumer consumer, long receiveTimeout) {
        // TODO if outMessage need to get the response
        try {
            Message inMessage = new MessageImpl();
            // set the message header back to the incomeMessage
            // inMessage.put(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS,
            // outMessage.get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS));

            byte[] response = null;
            javax.jms.Message jmsMessage = consumer.receive(receiveTimeout);
            if (jmsMessage == null) {
                // TODO: Review what exception should we throw.
                throw new JMSException("JMS receive timed out");
            }
            LOG.log(Level.FINE, "client received reply: ", jmsMessage);
            JMSUtils.populateIncomingContext(jmsMessage, inMessage, JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
            response = JMSUtils.retrievePayload(jmsMessage);
            LOG.log(Level.FINE, "The Response Message payload is : [" + response + "]");

            // setup the inMessage response stream
            inMessage.setContent(InputStream.class, new ByteArrayInputStream(response));
            LOG.log(Level.FINE, "incoming observer is " + incomingObserver);

            return inMessage;
        } catch (JMSException e) {
            throw new RuntimeException("Problem while receiving JMS message", e);
        }

    }

    private boolean isTextPayload() {
        return JMSConstants.TEXT_MESSAGE_TYPE.equals(runtimePolicy.getMessageType().value());
    }

    public void close() {
        getLogger().log(Level.FINE, "JMSConduit closed ");
        // ensure resources held by session factory are released
        if (sessionFactory != null) {
            sessionFactory.shutdown();
        }
    }

    protected Logger getLogger() {
        return LOG;
    }

    public String getBeanName() {
        return endpointInfo.getName().toString() + ".jms-conduit";
    }

    public AddressType getJMSAddress() {
        return address;
    }

    public void setJMSAddress(AddressType a) {
        this.address = a;
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public ClientBehaviorPolicyType getRuntimePolicy() {
        return runtimePolicy;
    }

    public void setRuntimePolicy(ClientBehaviorPolicyType runtimePolicy) {
        this.runtimePolicy = runtimePolicy;
    }

    public SessionPoolType getSessionPool() {
        return sessionPool;
    }

    public void setSessionPool(SessionPoolType sessionPool) {
        this.sessionPool = sessionPool;
    }
}
