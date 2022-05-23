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

import jakarta.jms.BytesMessage;
import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.XAConnection;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.transport.jms.JMSConstants;

public final class JMSUtil {

    public static final String JMS_MESSAGE_CONSUMER = "jms_message_consumer";
    public static final String JMS_IGNORE_TIMEOUT = "jms_ignore_timeout";

    private JMSUtil() {
    }

    public static Message receive(Session session,
                                  Destination replyToDestination,
                                  String correlationId,
                                  long receiveTimeout,
                                  boolean pubSubNoLocal) {
        try (ResourceCloser closer = new ResourceCloser()) {
            String messageSelector = correlationId == null ? null : "JMSCorrelationID = '" + correlationId + "'";
            MessageConsumer consumer = closer.register(session.createConsumer(replyToDestination, messageSelector,
                                                 pubSubNoLocal));
            jakarta.jms.Message replyMessage = consumer.receive(receiveTimeout);
            if (replyMessage == null) {
                throw new RuntimeException("Timeout receiving message with correlationId "
                                           + correlationId);
            }
            return replyMessage;
        } catch (JMSException e) {
            throw convertJmsException(e);
        }
    }

    public static Message receive(Session session,
                                  Destination replyToDestination,
                                  String correlationId,
                                  long receiveTimeout,
                                  boolean pubSubNoLocal,
                                  Exchange exchange) {
        try (ResourceCloser closer = new ResourceCloser()) {
            String messageSelector = correlationId == null ? null : "JMSCorrelationID = '" + correlationId + "'";
            MessageConsumer consumer = closer.register(session.createConsumer(replyToDestination, messageSelector,
                                                 pubSubNoLocal));
            if (exchange != null) {
                exchange.put(JMS_MESSAGE_CONSUMER, consumer);
            }
            jakarta.jms.Message replyMessage = consumer.receive(receiveTimeout);
            if (replyMessage == null) {
                if ((boolean)exchange.get(JMSUtil.JMS_IGNORE_TIMEOUT)) {
                    throw new RuntimeException("Timeout receiving message with correlationId "
                                           + correlationId);
                } else {
                    throw new JMSException("Timeout receiving message with correlationId "
                        + correlationId);
                }
            }
            return replyMessage;
        } catch (JMSException e) {
            throw convertJmsException(e);
        }
    }

    public static RuntimeException convertJmsException(JMSException e) {
        return new RuntimeException(e.getMessage(), e);
    }

    public static String createCorrelationId(final String prefix, long sequenceNum) {
        return prefix + StringUtils.toHexString(java.nio.ByteBuffer.allocate(Long.BYTES).putLong(sequenceNum).array());
    }

    /**
     * Create a JMS of the appropriate type populated with the given payload.
     *
     * @param payload the message payload, expected to be either of type String or byte[] depending on payload
     *            type
     * @param session the JMS session
     * @param messageType the JMS message type
     * @return a JMS of the appropriate type populated with the given payload
     */
    public static Message createAndSetPayload(Object payload, Session session, String messageType)
        throws JMSException {
        final Message message;
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

    public static Queue createQueue(Connection connection, String name) throws JMSException {
        if (connection instanceof XAConnection) { 
            try (Session session = ((XAConnection)connection).createXASession()) {
                return session.createQueue(name);
            }
        } else {
            try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                return session.createQueue(name);
            }
        }
    }

}
