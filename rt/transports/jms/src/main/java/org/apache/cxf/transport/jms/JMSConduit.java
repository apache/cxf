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
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * JMSConduit is instantiated by the JMSTransportfactory which is selected by a client if the transport
 * protocol starts with jms:// JMSConduit converts CXF Messages to JMS Messages and sends the request by using
 * JMS topics or queues. If the Exchange is not oneway it then recevies the response and converts it to a CXF
 * Message. This is then provided in the Exchange and also sent to the incomingObserver
 */
public class JMSConduit extends AbstractConduit implements Configurable, JMSExchangeSender {

    protected static final String BASE_BEAN_NAME_SUFFIX = ".jms-conduit-base";

    static final Logger LOG = LogUtils.getL7dLogger(JMSConduit.class);

    protected Bus bus;
    protected EndpointInfo endpointInfo;
    protected JMSConfiguration jmsConfig;
    protected String beanNameSuffix;

    public JMSConduit(Bus b, EndpointInfo endpointInfo) {
        this(b, endpointInfo, null);
    }

    public JMSConduit(Bus b, EndpointInfo endpointInfo, EndpointReferenceType target) {
        super(target);
        this.bus = b;
        this.endpointInfo = endpointInfo;
        this.beanNameSuffix = BASE_BEAN_NAME_SUFFIX;
    }

    // prepare the message for send out , not actually send out the message
    public void prepare(Message message) throws IOException {
        message.setContent(OutputStream.class, new JMSOutputStream(this, message.getExchange(),
                                                                   isTextPayload()));
        // After this step flow will continue in JMSOutputStream.doClose()
    }

    public Destination determineReplyToDestination(final JmsTemplate jmsTemplate,
                                                   final String replyToDestinationName,
                                                   final boolean pubSubDomain, boolean isOneWay) {
        if (isOneWay) {
            return null;
        }
        return (Destination)jmsTemplate.execute(new SessionCallback() {
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
                DestinationResolver resolv = jmsTemplate.getDestinationResolver();
                return resolv.resolveDestinationName(session, replyToDestinationName, pubSubDomain);
            }
        });
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
        final JmsTemplate jmsTemplate = jmsConfig.getJmsTemplate();
        final Destination replyTo = determineReplyToDestination(jmsTemplate,
                                                                jmsConfig.getReplyDestination(), jmsConfig
                                                                    .isPubSubDomain(), exchange.isOneWay());
        final Message outMessage = exchange.getOutMessage();
        if (outMessage == null) {
            throw new RuntimeException("Exchange to be sent has no outMessage");
        }

        JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
        final String correlationId = (headers != null && headers.isSetJMSCorrelationID()) ? headers
            .getJMSCorrelationID() : JMSUtils.generateUniqueSelector();
        String selector = "JMSCorrelationID = '" + correlationId + "'";
        
        // TODO This is not thread safe
        jmsTemplate.setPriority(JMSUtils.getJMSPriority(headers));
        jmsTemplate.send(jmsConfig.getTargetDestination(), new MessageCreator() {
            public javax.jms.Message createMessage(Session session) throws JMSException {
                String messageType = jmsConfig.getMessageType();
                final javax.jms.Message jmsMessage;
                jmsMessage = JMSUtils.buildJMSMessageFromCXFMessage(outMessage, request, messageType,
                                                                    session, replyTo, correlationId);
                LOG.log(Level.FINE, "client sending request: ", jmsMessage);
                return jmsMessage;
            }
        });

        if (!exchange.isOneWay()) {
            javax.jms.Message jmsMessage = jmsTemplate.receiveSelected(replyTo, selector);
            if (jmsMessage == null) {
                throw new RuntimeException("JMS receive timed out");
            }
            Message inMessage = new MessageImpl();
            LOG.log(Level.FINE, "client received reply: ", jmsMessage);
            JMSUtils
                .populateIncomingContext(jmsMessage, inMessage, JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
            byte[] response = JMSUtils.retrievePayload(jmsMessage);
            LOG.log(Level.FINE, "The Response Message payload is : [" + response + "]");
            inMessage.setContent(InputStream.class, new ByteArrayInputStream(response));
            exchange.setInMessage(inMessage);
            if (incomingObserver != null) {
                incomingObserver.onMessage(inMessage);
            }
        }
    }

    private boolean isTextPayload() {
        return JMSConstants.TEXT_MESSAGE_TYPE.equals(jmsConfig.getMessageType());
    }

    public void close() {
        LOG.log(Level.FINE, "JMSConduit closed ");
    }

    protected Logger getLogger() {
        return LOG;
    }

    public String getBeanName() {
        return endpointInfo.getName().toString() + ".jms-conduit";
    }

    public JMSConfiguration getJmsConfig() {
        return jmsConfig;
    }

    public void setJmsConfig(JMSConfiguration jmsConfig) {
        this.jmsConfig = jmsConfig;
    }

}
