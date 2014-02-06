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
package org.apache.cxf.transport.jms.util;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.cxf.transport.jms.JMSConstants;

public final class JMSUtil {
    private static final char[] CORRELATTION_ID_PADDING = {
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0'
    };
    
    private JMSUtil() {
    }
    
    public static Message receive(Session session,
                                  Destination replyToDestination,
                                  String correlationId,
                                  long receiveTimeout,
                                  boolean pubSubNoLocal) {
        ResourceCloser closer = new ResourceCloser();
        try {
            String messageSelector = correlationId == null ? null : "JMSCorrelationID = '" + correlationId + "'";
            MessageConsumer consumer = closer.register(session.createConsumer(replyToDestination, messageSelector,
                                                 pubSubNoLocal));
            javax.jms.Message replyMessage = consumer.receive(receiveTimeout);
            if (replyMessage == null) {
                throw new RuntimeException("Timeout receiving message with correlationId "
                                           + correlationId);
            }
            return replyMessage;
        } catch (JMSException e) {
            throw convertJmsException(e);
        } finally {
            closer.close();
        }
    }

    public static RuntimeException convertJmsException(JMSException e) {
        return new RuntimeException(e.getMessage(), e);
    }

    public static String createCorrelationId(final String prefix, long sequenceNUm) {
        String index = Long.toHexString(sequenceNUm);
        StringBuilder id = new StringBuilder(prefix);
        id.append(CORRELATTION_ID_PADDING, 0, 16 - index.length());
        id.append(index);
        return id.toString();
    }

    /**
     * Create a JMS of the appropriate type populated with the given payload.
     * 
     * @param payload the message payload, expected to be either of type String or byte[] depending on payload
     *            type
     * @param session the JMS session
     * @param replyTo the ReplyTo destination if any
     * @return a JMS of the appropriate type populated with the given payload
     */
    public static Message createAndSetPayload(Object payload, Session session, String messageType)
        throws JMSException {
        Message message = null;
        if (JMSConstants.TEXT_MESSAGE_TYPE.equals(messageType)) {
            message = session.createTextMessage((String)payload);
        } else if (JMSConstants.BYTE_MESSAGE_TYPE.equals(messageType)) {
            message = session.createBytesMessage();
            ((BytesMessage)message).writeBytes((byte[])payload);
        } else {
            message = session.createObjectMessage();
            ((ObjectMessage)message).setObject((byte[])payload);
        }
        return message;
    }
    

}
