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

import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.ConfigurationException;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.JmsUtils;

/**
 * JMSConduit is instantiated by the JMSTransportfactory which is selected by a client if the transport
 * protocol starts with jms:// JMSConduit converts CXF Messages to JMS Messages and sends the request by using
 * a JMS destination. If the Exchange is not oneway it then recevies the response and converts it to a CXF
 * Message. This is then provided in the Exchange and also sent to the incomingObserver
 */
public class JMSConduit extends AbstractConduit implements JMSExchangeSender, MessageListener {
    static final Logger LOG = LogUtils.getL7dLogger(JMSConduit.class);

    private EndpointInfo endpointInfo;
    private JMSConfiguration jmsConfig;
    private Map<String, Exchange> correlationMap;
    private DefaultMessageListenerContainer jmsListener;

    public JMSConduit(EndpointInfo endpointInfo, EndpointReferenceType target, JMSConfiguration jmsConfig) {
        super(target);
        this.jmsConfig = jmsConfig;
        this.endpointInfo = endpointInfo;
        correlationMap = new ConcurrentHashMap<String, Exchange>();
    }

    /**
     * Prepare the message for send out. The message will be sent after the caller has written the payload to
     * the OutputStream of the message and calls the close method of the stream. In the JMS case the
     * JMSOutputStream will then call back the sendExchange method of this class. {@inheritDoc}
     */
    public void prepare(Message message) throws IOException {
        if (jmsConfig.getTargetDestination() == null || jmsConfig.getConnectionFactory() == null) {
            String name =  endpointInfo.getName().toString() + ".jms-conduit";
            throw new ConfigurationException(
                new org.apache.cxf.common.i18n.Message("INSUFFICIENT_CONFIGURATION_CONDUIT", LOG, name));
        }
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

        JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);

        JmsTemplate jmsTemplate = JMSFactory.createJmsTemplate(jmsConfig, headers);
        if (!exchange.isOneWay() && jmsListener == null) {
            jmsListener = JMSFactory.createJmsListener(jmsConfig, this, jmsConfig.getReplyDestination());
        }
        
        final javax.jms.Destination replyTo = exchange.isOneWay() ? null : jmsListener.getDestination();

        final String correlationId = (headers != null && headers.isSetJMSCorrelationID()) ? headers
            .getJMSCorrelationID() : JMSUtils.generateCorrelationId();
            
        MessageCreator messageCreator = new MessageCreator() {
            public javax.jms.Message createMessage(Session session) throws JMSException {
                String messageType = jmsConfig.getMessageType();
                final javax.jms.Message jmsMessage;
                jmsMessage = JMSUtils.buildJMSMessageFromCXFMessage(outMessage, request, messageType,
                                                                    session, replyTo,
                                                                    correlationId);
                LOG.log(Level.FINE, "client sending request: ", jmsMessage);
                return jmsMessage;
            }
        };

        /**
         * If the message is not oneWay we will expect to receive a reply on the listener. To receive this
         * reply we add the correlationId and an empty CXF Message to the correlationMap. The listener will
         * fill to Message and notify this thread
         */
        if (!exchange.isOneWay()) {
            synchronized (exchange) {
                correlationMap.put(correlationId, exchange);
                jmsTemplate.send(jmsConfig.getTargetDestination(), messageCreator);
                
                if (exchange.isSynchronous()) {
                    try {
                        exchange.wait(jmsTemplate.getReceiveTimeout());
                    } catch (InterruptedException e) {
                        correlationMap.remove(correlationId);
                        throw new RuntimeException(e);
                    }
                    correlationMap.remove(correlationId);
                    if (exchange.getInMessage() == null
                        && exchange.getInFaultMessage() == null) {
                        throw new RuntimeException("Timeout receiving message with correlationId "
                                                   + correlationId);
                    }
                    
                    
                }
            }
        } else {
            jmsTemplate.send(jmsConfig.getTargetDestination(), messageCreator);
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
        Message inMessage = new MessageImpl();
        exchange.setInMessage(inMessage);
        LOG.log(Level.FINE, "client received reply: ", jmsMessage);
        JMSUtils.populateIncomingContext(jmsMessage, inMessage, JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
        byte[] response = JMSUtils.retrievePayload(jmsMessage);
        LOG.log(Level.FINE, "The Response Message payload is : [" + response + "]");
        inMessage.setContent(InputStream.class, new ByteArrayInputStream(response));

        if (exchange.isSynchronous()) {
            synchronized (exchange) {
                exchange.notifyAll();
            }
        }
        
        //REVISIT: put on a workqueue?
        if (incomingObserver != null) {
            incomingObserver.onMessage(exchange.getInMessage());
        }
    }

    public void close() {
        if (jmsListener != null) {
            jmsListener.shutdown();
        }
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
        if (jmsListener != null) {
            jmsListener.shutdown();
        }
        super.finalize();
    }

}
