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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.naming.NamingException;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class JMSConduit extends AbstractConduit implements Configurable, JMSOnConnectCallback {

    protected static final String BASE_BEAN_NAME_SUFFIX = ".jms-conduit-base";

    private static final Logger LOG = LogUtils.getL7dLogger(JMSConduit.class);

    protected Destination targetDestination;
    protected Destination replyDestination;
    protected JMSSessionFactory sessionFactory;
    protected Bus bus;
    protected EndpointInfo endpointInfo;
    protected String beanNameSuffix;

    protected ClientConfig clientConfig;
    protected ClientBehaviorPolicyType runtimePolicy;
    protected AddressType address;
    protected SessionPoolType sessionPool;

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

    // prepare the message for send out , not actually send out the message
    public void prepare(Message message) throws IOException {
        getLogger().log(Level.FINE, "JMSConduit send message");

        try {
            if (null == sessionFactory) {
                JMSProviderHub.connect(this, getJMSAddress(), getSessionPool());
            }
        } catch (JMSException jmsex) {
            getLogger().log(Level.WARNING, "JMS connect failed with JMSException : ", jmsex);
            throw new IOException(jmsex.toString());
        } catch (NamingException ne) {
            getLogger().log(Level.WARNING, "JMS connect failed with NamingException : ", ne);
            throw new IOException(ne.toString());
        }

        if (sessionFactory == null) {
            throw new java.lang.IllegalStateException("JMSClientTransport not connected");
        }

        try {
            boolean isOneWay = false;
            // test if the message is oneway message
            Exchange ex = message.getExchange();
            if (null != ex) {
                isOneWay = ex.isOneWay();
            }
            // get the pooledSession with response expected
            PooledSession pooledSession = sessionFactory.get(!isOneWay);
            // put the PooledSession into the outMessage
            message.put(JMSConstants.JMS_POOLEDSESSION, pooledSession);

        } catch (JMSException jmsex) {
            throw new IOException(jmsex.getMessage());
        }

        message.setContent(OutputStream.class, new JMSOutputStream(message));

    }

    public void close() {
        getLogger().log(Level.FINE, "JMSConduit closed ");

        // ensure resources held by session factory are released
        //
        if (sessionFactory != null) {
            sessionFactory.shutdown();
        }
    }

    protected Logger getLogger() {
        return LOG;
    }

    /**
     * Receive mechanics.
     * 
     * @param pooledSession the shared JMS resources
     * @param inMessage
     * @retrun the response buffer
     */
    private Object receive(PooledSession pooledSession, Message outMessage, Message inMessage)
        throws JMSException {

        Object result = null;

        long timeout = getClientConfig().getClientReceiveTimeout();

        Long receiveTimeout = (Long)outMessage.get(JMSConstants.JMS_CLIENT_RECEIVE_TIMEOUT);

        if (receiveTimeout != null) {
            timeout = receiveTimeout.longValue();
        }

        javax.jms.Message jmsMessage = pooledSession.consumer().receive(timeout);
        getLogger().log(Level.FINE, "client received reply: ", jmsMessage);

        if (jmsMessage != null) {

            JMSUtils.populateIncomingContext(jmsMessage, inMessage, JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
            result = JMSUtils.unmarshal(jmsMessage);
            return result;
        } else {
            String error = "JMSClientTransport.receive() timed out. No message available.";
            getLogger().log(Level.SEVERE, error);
            // TODO: Review what exception should we throw.
            throw new JMSException(error);

        }
    }

    public void connected(Destination target, Destination reply, JMSSessionFactory factory) {
        this.targetDestination = target;
        this.replyDestination = reply;
        this.sessionFactory = factory;
    }

    public String getBeanName() {
        return endpointInfo.getName().toString() + ".jms-conduit";
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

    private boolean isTextPayload() {
        return JMSConstants.TEXT_MESSAGE_TYPE.equals(getRuntimePolicy().getMessageType().value());
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

    private class JMSOutputStream extends CachedOutputStream {
        private Message outMessage;
        private javax.jms.Message jmsMessage;
        private PooledSession pooledSession;
        private boolean isOneWay;

        public JMSOutputStream(Message m) {
            outMessage = m;
            pooledSession = (PooledSession)outMessage.get(JMSConstants.JMS_POOLEDSESSION);
        }

        protected void doFlush() throws IOException {
            // do nothing here
        }

        protected void doClose() throws IOException {
            try {
                isOneWay = outMessage.getExchange().isOneWay();
                commitOutputMessage();
                if (!isOneWay) {
                    handleResponse();
                }
            } catch (JMSException jmsex) {
                getLogger().log(Level.WARNING, "JMS connect failed with JMSException : ", jmsex);
                throw new IOException(jmsex.toString());
            } finally {
                sessionFactory.recycle(pooledSession);
            }
        }

        protected void onWrite() throws IOException {

        }

        private void commitOutputMessage() throws JMSException {
            javax.jms.Destination replyTo = pooledSession.destination();
            // TODO setting up the responseExpected

            // We don't want to send temp queue in
            // replyTo header for oneway calls
            if (isOneWay && (getJMSAddress().getJndiReplyDestinationName() == null)) {
                replyTo = null;
            }

            Object request = null;
            try {
                if (isTextPayload()) {
                    StringBuilder builder = new StringBuilder(2048);
                    this.writeCacheTo(builder);
                    request = builder.toString();
                } else {
                    request = getBytes();
                }
            } catch (IOException ex) {
                JMSException ex2 = new JMSException("Error creating request");
                ex2.setLinkedException(ex);
                throw ex2;
            }
            if (getLogger().isLoggable(Level.FINE)) {
                getLogger().log(Level.FINE, "Conduit Request is :[" + request + "]");
            }

            jmsMessage = JMSUtils.marshal(request, pooledSession.session(), replyTo, getRuntimePolicy()
                .getMessageType().value());

            JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
                .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);

            int deliveryMode = JMSUtils.getJMSDeliveryMode(headers);
            int priority = JMSUtils.getJMSPriority(headers);
            String correlationID = JMSUtils.getCorrelationId(headers);
            long ttl = JMSUtils.getTimeToLive(headers);
            if (ttl <= 0) {
                ttl = getClientConfig().getMessageTimeToLive();
            }

            JMSUtils.setMessageProperties(headers, jmsMessage);
            // ensure that the contentType is set to the out jms message header
            JMSUtils.setContentToProtocalHeader(outMessage);
            Map<String, List<String>> protHeaders = CastUtils.cast((Map<?, ?>)outMessage
                .get(Message.PROTOCOL_HEADERS));
            JMSUtils.addProtocolHeaders(jmsMessage, protHeaders);
            if (!isOneWay) {
                String id = pooledSession.getCorrelationID();

                if (id != null) {
                    if (correlationID != null) {
                        String error = "User cannot set JMSCorrelationID when "
                                       + "making a request/reply invocation using "
                                       + "a static replyTo Queue.";
                        throw new JMSException(error);
                    }
                    correlationID = id;
                }
            }

            if (correlationID != null) {
                jmsMessage.setJMSCorrelationID(correlationID);
            } else {
                // No message correlation id is set. Whatever comeback will be accepted as responses.
                // We assume that it will only happen in case of the temp. reply queue.
            }

            getLogger().log(Level.FINE, "client sending request: ", jmsMessage);
            // getting Destination Style
            if (JMSUtils.isDestinationStyleQueue(address)) {
                QueueSender sender = (QueueSender)pooledSession.producer();
                sender.setTimeToLive(ttl);
                sender.send((Queue)targetDestination, jmsMessage, deliveryMode, priority, ttl);
            } else {
                TopicPublisher publisher = (TopicPublisher)pooledSession.producer();
                publisher.setTimeToLive(ttl);
                publisher.publish((Topic)targetDestination, jmsMessage, deliveryMode, priority, ttl);
            }
        }

        private void handleResponse() throws IOException {
            // REVISIT distinguish decoupled case or oneway call
            Object response = null;

            // TODO if outMessage need to get the response
            Message inMessage = new MessageImpl();
            outMessage.getExchange().setInMessage(inMessage);
            // set the message header back to the incomeMessage
            // inMessage.put(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS,
            // outMessage.get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS));

            try {
                response = receive(pooledSession, outMessage, inMessage);
            } catch (JMSException jmsex) {
                getLogger().log(Level.FINE, "JMS connect failed with JMSException : ", jmsex);
                throw new IOException(jmsex.toString());
            }

            getLogger().log(Level.FINE, "The Response Message is : [" + response + "]");

            // setup the inMessage response stream
            byte[] bytes = null;
            if (response instanceof String) {
                String requestString = (String)response;
                bytes = requestString.getBytes();
            } else {
                bytes = (byte[])response;
            }
            inMessage.setContent(InputStream.class, new ByteArrayInputStream(bytes));
            getLogger().log(Level.FINE, "incoming observer is " + incomingObserver);
            incomingObserver.onMessage(inMessage);
        }
    }

    /**
     * Represented decoupled response endpoint.
     */
    protected class DecoupledDestination implements Destination {
        protected MessageObserver decoupledMessageObserver;
        private EndpointReferenceType address;

        DecoupledDestination(EndpointReferenceType ref, MessageObserver incomingObserver) {
            address = ref;
            decoupledMessageObserver = incomingObserver;
        }

        public EndpointReferenceType getAddress() {
            return address;
        }

        public Conduit getBackChannel(Message inMessage, Message partialResponse, EndpointReferenceType addr)
            throws IOException {
            // shouldn't be called on decoupled endpoint
            return null;
        }

        public void shutdown() {
            // TODO Auto-generated method stub
        }

        public synchronized void setMessageObserver(MessageObserver observer) {
            decoupledMessageObserver = observer;
        }

        public synchronized MessageObserver getMessageObserver() {
            return decoupledMessageObserver;
        }
    }

}
