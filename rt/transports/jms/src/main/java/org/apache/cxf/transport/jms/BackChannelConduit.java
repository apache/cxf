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
import java.util.logging.Logger;

import javax.jms.TextMessage;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * Conduit for sending the reply back to the client
 */
class BackChannelConduit extends AbstractConduit {
    private static final Logger LOG = LogUtils.getL7dLogger(JMSDestination.class);
    protected Message inMessage;
    private JMSExchangeSender sender;

    BackChannelConduit(JMSExchangeSender sender, EndpointReferenceType ref, Message message) {
        super(ref);
        inMessage = message;
        this.sender = sender;
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

        final Exchange exchange = inMessage.getExchange();
        exchange.setOutMessage(message);
        
        boolean isTextMessage = (jmsMessage instanceof TextMessage) && !JMSMessageUtils.isMtomEnabled(message);
        MessageStreamUtil.prepareStream(message, isTextMessage, sender);
    }
    
    protected Logger getLogger() {
        return LOG;
    }
}