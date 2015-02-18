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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.ConfigurationException;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.jms.util.JMSSender;
import org.apache.cxf.transport.jms.util.JMSUtil;
import org.apache.cxf.transport.jms.util.ResourceCloser;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;

/**
 * Conduit for sending the reply back to the client
 */
class BackChannelConduit extends AbstractConduit implements JMSExchangeSender {
    private static final Logger LOG = LogUtils.getL7dLogger(JMSDestination.class);
    private JMSConfiguration jmsConfig;
    private Message inMessage;
    private Connection connection;
    
    BackChannelConduit(Message inMessage, JMSConfiguration jmsConfig, Connection connection) {
        super(EndpointReferenceUtils.getAnonymousEndpointReference());
        this.inMessage = inMessage;
        this.jmsConfig = jmsConfig;
        this.connection = connection;
    }
    @Override
    public void close(Message msg) throws IOException {
        MessageStreamUtil.closeStreams(msg);
        super.close(msg);
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
    public void prepare(final Message message) throws IOException {
        // setup the message to be sent back
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

        boolean isTextMessage = (jmsMessage instanceof TextMessage) && !JMSMessageUtils.isMtomEnabled(message);
        MessageStreamUtil.prepareStream(message, isTextMessage, this);
    }
    
    protected Logger getLogger() {
        return LOG;
    }

    public void sendExchange(Exchange exchange, final Object replyObj) {
        if (exchange.isOneWay()) {
            //Don't need to send anything
            return;
        }

        final Message outMessage = exchange.getOutMessage();

        try (ResourceCloser closer = new ResourceCloser()) {
            Session session = closer
                .register(connection.createSession(false, Session.AUTO_ACKNOWLEDGE));

            final JMSMessageHeadersType messageProperties = (JMSMessageHeadersType)outMessage
                .get(JMSConstants.JMS_SERVER_RESPONSE_HEADERS);
            JMSMessageHeadersType inMessageProperties = (JMSMessageHeadersType)inMessage
                .get(JMSConstants.JMS_SERVER_REQUEST_HEADERS);
            initResponseMessageProperties(messageProperties, inMessageProperties);

            // setup the reply message
            final javax.jms.Message request = (javax.jms.Message)inMessage
                .get(JMSConstants.JMS_REQUEST_MESSAGE);
            final String msgType;
            if (JMSMessageUtils.isMtomEnabled(outMessage) 
                && !jmsConfig.getMessageType().equals(JMSConstants.TEXT_MESSAGE_TYPE)) {
                //get chance to set messageType from JMSConfiguration with MTOM enabled
                msgType = jmsConfig.getMessageType();
            } else {
                msgType = JMSMessageUtils.getMessageType(request);
            }
            if (JMSConstants.TEXT_MESSAGE_TYPE.equals(msgType) 
                && JMSMessageUtils.isMtomEnabled(outMessage)) {
                org.apache.cxf.common.i18n.Message msg = 
                    new org.apache.cxf.common.i18n.Message("INVALID_MESSAGE_TYPE", LOG);
                throw new ConfigurationException(msg);
            }
            
            if (isTimedOut(request)) {
                return;
            }

            Destination replyTo = getReplyToDestination(session, inMessage);
            if (replyTo == null) {
                throw new RuntimeException("No replyTo destination set");
            }

            getLogger().log(Level.FINE, "send out the message!");

            String correlationId = determineCorrelationID(request);
            javax.jms.Message reply = JMSMessageUtils.asJMSMessage(jmsConfig, 
                                      outMessage, 
                                      replyObj, 
                                      msgType,
                                      session,
                                      correlationId, JMSConstants.JMS_SERVER_RESPONSE_HEADERS);
            JMSSender sender = JMSFactory.createJmsSender(jmsConfig, messageProperties);
            LOG.log(Level.FINE, "server sending reply: ", reply);
            sender.sendMessage(session, replyTo, reply);
        } catch (JMSException ex) {
            throw JMSUtil.convertJmsException(ex);
        }
    }
    
    /**
     * @param messageProperties
     * @param inMessageProperties
     */
    public static void initResponseMessageProperties(JMSMessageHeadersType messageProperties,
                                                     JMSMessageHeadersType inMessageProperties) {
        messageProperties.setJMSDeliveryMode(inMessageProperties.getJMSDeliveryMode());
        messageProperties.setJMSPriority(inMessageProperties.getJMSPriority());
        messageProperties.setSOAPJMSRequestURI(inMessageProperties.getSOAPJMSRequestURI());
        messageProperties.setSOAPJMSBindingVersion("1.0");
    }

    private boolean isTimedOut(final javax.jms.Message request) throws JMSException {
        if (request.getJMSExpiration() > 0) {
            TimeZone tz = new SimpleTimeZone(0, "GMT");
            Calendar cal = new GregorianCalendar(tz);
            long timeToLive = request.getJMSExpiration() - cal.getTimeInMillis();
            if (timeToLive < 0) {
                getLogger()
                    .log(Level.INFO, "Message time to live is already expired skipping response.");
                return true;
            }
        }
        return false;
    }
    
    private Destination getReplyToDestination(Session session, Message inMessage2) throws JMSException {
        javax.jms.Message message = (javax.jms.Message)inMessage2.get(JMSConstants.JMS_REQUEST_MESSAGE);
        // If WS-Addressing had set the replyTo header.
        final String replyToName = (String)inMessage2.get(JMSConstants.JMS_REBASED_REPLY_TO);
        if (replyToName != null) {
            return jmsConfig.getReplyDestination(session, replyToName);
        } else if (message.getJMSReplyTo() != null) {
            return message.getJMSReplyTo();
        } else {
            return jmsConfig.getReplyDestination(session);
        }
    }
    
    /**
     * Decides what correlationId to use for the reply by looking at the request headers
     * 
     * @param request jms request message
     * @return correlation id of request if set else message id from request
     * @throws JMSException
     */
    public String determineCorrelationID(javax.jms.Message request) throws JMSException {
        return StringUtils.isEmpty(request.getJMSCorrelationID())
            ? request.getJMSMessageID() 
            : request.getJMSCorrelationID();
    }

}