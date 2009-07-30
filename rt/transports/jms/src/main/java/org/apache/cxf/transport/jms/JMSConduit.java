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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

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

/**
 * JMSConduit is instantiated by the JMSTransportfactory which is selected by a client if the transport
 * protocol starts with jms:// JMSConduit converts CXF Messages to JMS Messages and sends the request by using
 * a JMS destination. If the Exchange is not oneway it then recevies the response and converts it to a CXF
 * Message. This is then provided in the Exchange and also sent to the incomingObserver
 */
public class JMSConduit extends AbstractConduit implements JMSExchangeSender {

    static final Logger LOG = LogUtils.getL7dLogger(JMSConduit.class);
    
    private static final String CORRELATED = JMSConduit.class.getName() + ".correlated";
    
    private EndpointInfo endpointInfo;
    private JMSConfiguration jmsConfig;
    private String conduitId;
    private AtomicLong messageCount;

    public JMSConduit(EndpointInfo endpointInfo, EndpointReferenceType target, JMSConfiguration jmsConfig) {
        super(target);
        this.jmsConfig = jmsConfig;
        this.endpointInfo = endpointInfo;
        conduitId = UUID.randomUUID().toString().replaceAll("-", "");
        messageCount = new AtomicLong(0);
    }
    
    /**
     * Prepare the message for send out. The message will be sent after the caller has written the payload to
     * the OutputStream of the message and calls the close method of the stream. In the JMS case the
     * JMSOutputStream will then call back the sendExchange method of this class. {@inheritDoc}
     */
    public void prepare(Message message) throws IOException {
        String name =  endpointInfo.getName().toString() + ".jms-conduit";
        org.apache.cxf.common.i18n.Message msg = 
            new org.apache.cxf.common.i18n.Message("INSUFFICIENT_CONFIGURATION_CONDUIT", LOG, name);
        jmsConfig.ensureProperlyConfigured(msg);
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
        
        boolean isTextPayload = JMSConstants.TEXT_MESSAGE_TYPE.equals(jmsConfig.getMessageType());
        if (isTextPayload && MessageUtils.isTrue(outMessage.getContextualProperty(
            org.apache.cxf.message.Message.MTOM_ENABLED)) 
            && outMessage.getAttachments() != null && outMessage.getAttachments().size() > 0) {
            org.apache.cxf.common.i18n.Message msg = 
                new org.apache.cxf.common.i18n.Message("INVALID_MESSAGE_TYPE", LOG);
            throw new ConfigurationException(msg);
        }
        
        JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
        if (headers == null) {
            headers = new JMSMessageHeadersType();
            outMessage.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, headers);
        }
        String replyTo = headers.getJMSReplyTo();
        if (replyTo == null) {
            replyTo = jmsConfig.getReplyDestination();
        }
        final JmsTemplate jmsTemplate = JMSFactory.createJmsTemplate(jmsConfig, headers);
        
        String userCID = headers.getJMSCorrelationID();
        boolean messageIdPattern = false;
        String correlationId = null;
        if (!exchange.isOneWay()) {
            if (userCID != null) {
                correlationId = userCID;
            } else if (!jmsConfig.isSetConduitSelectorPrefix()
                       && (!jmsConfig.isSetUseConduitIdSelector() || !jmsConfig
                           .isUseConduitIdSelector())) {
                messageIdPattern = true;
            } else { 
                if (jmsConfig.isUseConduitIdSelector()) {
                    correlationId = JMSUtils.createCorrelationId(jmsConfig
                        .getConduitSelectorPrefix()
                                                                 + conduitId, messageCount
                        .incrementAndGet());
                } else {
                    correlationId = JMSUtils.createCorrelationId(jmsConfig
                        .getConduitSelectorPrefix(), messageCount.incrementAndGet());
                }
            }
        }
        
        Destination replyToDestination = null;
        if (!exchange.isOneWay() || !jmsConfig.isEnforceSpec() && isSetReplyTo(outMessage)
            && replyTo != null) {
            replyToDestination = JMSFactory.resolveOrCreateDestination(jmsTemplate, replyTo,
                                                                       jmsConfig.isPubSubDomain());
        }
        
        final String cid = correlationId; 
        final Destination rtd = replyToDestination;
        class JMSConduitMessageCreator implements MessageCreator {
            private javax.jms.Message jmsMessage;

            public javax.jms.Message createMessage(Session session) throws JMSException {
                String messageType = jmsConfig.getMessageType();
                jmsMessage = JMSUtils.buildJMSMessageFromCXFMessage(jmsConfig, outMessage, request,
                                                                    messageType, session, rtd,
                                                                    cid);
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
                if (messageIdPattern) {
                    correlationId = messageCreator.getMessageID();
                }
                headers.setJMSMessageID(messageCreator.getMessageID());
                
                String messageSelector = "JMSCorrelationID = '" + correlationId + "'";
                javax.jms.Message replyMessage = jmsTemplate.receiveSelected(replyToDestination,
                                                                             messageSelector);
                if (replyMessage == null) {
                    throw new RuntimeException("Timeout receiving message with correlationId "
                                               + correlationId);
                } else {
                    doReplyMessage(exchange, replyMessage);
                }
            }
        } else {
            jmsTemplate.send(jmsConfig.getTargetDestination(), messageCreator);
            headers.setJMSMessageID(messageCreator.getMessageID());
        }
    }

    /**
     * Here we just deal with the reply message
     */
    public void doReplyMessage(Exchange exchange, javax.jms.Message jmsMessage) {
        Message inMessage = new MessageImpl();
        exchange.setInMessage(inMessage);
        LOG.log(Level.FINE, "client received reply: ", jmsMessage);
        try {
            JMSUtils.populateIncomingContext(jmsMessage, inMessage, JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
        
            byte[] response = JMSUtils.retrievePayload(jmsMessage, (String)inMessage.get(Message.ENCODING));
            LOG.log(Level.FINE, "The Response Message payload is : [" + response + "]");
            inMessage.setContent(InputStream.class, new ByteArrayInputStream(response));

            if (exchange.isSynchronous()) {
                synchronized (exchange) {
                    exchange.put(CORRELATED, Boolean.TRUE);
                    exchange.notifyAll();
                }
            }
        
            //REVISIT: put on a workqueue?
            if (incomingObserver != null) {
                incomingObserver.onMessage(exchange.getInMessage());
            }
        } catch (UnsupportedEncodingException ex) {
            getLogger().log(Level.WARNING, "can't get the right encoding information " + ex);
        }
    }

    public void close() {
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
        super.finalize();
    }
}