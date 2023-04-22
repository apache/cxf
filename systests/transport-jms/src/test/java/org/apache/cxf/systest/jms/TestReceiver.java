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
package org.apache.cxf.systest.jms;

import java.util.concurrent.CompletableFuture;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.cxf.transport.jms.util.JMSUtil;
import org.apache.cxf.transport.jms.util.ResourceCloser;

/**
 * Receiver for integration tests. It simulates the server side of the service
 * Copy of org.apache.cxf.transport.jms.util.TestReceiver
 */
public class TestReceiver {
    private ConnectionFactory connectionFactory;
    private String receiveQueueName;
    private String requestMessageId;
    private String staticReplyQueue;
    private Throwable ex;
    private boolean forceMessageIdAsCorrelationId;

    /**
     *
     * @param connectionFactory
     * @param receiveQueueName listen on this queue
     * @param forceMessageIdAsCorrelationId force the usage of messageId even if correlationId is set
     */
    public TestReceiver(ConnectionFactory connectionFactory,
                        String receiveQueueName,
                        boolean forceMessageIdAsCorrelationId) {
        this.connectionFactory = connectionFactory;
        this.receiveQueueName = receiveQueueName;
        this.forceMessageIdAsCorrelationId = forceMessageIdAsCorrelationId;
        assert this.connectionFactory != null;
        assert this.receiveQueueName != null;
    }

    public String getRequestMessageId() {
        return requestMessageId;
    }

    public void setStaticReplyQueue(String staticReplyQueue) {
        this.staticReplyQueue = staticReplyQueue;
    }

    private void drainQueue() {
        try (ResourceCloser closer = new ResourceCloser()) {
            Connection connection = closer.register(connectionFactory.createConnection());
            connection.start();
            Session session = closer.register(connection.createSession(false, Session.AUTO_ACKNOWLEDGE));
            MessageConsumer consumer = closer.register(session.createConsumer(session.createQueue(receiveQueueName)));
            jakarta.jms.Message message;
            do {
                message = consumer.receive(100);
            } while (message != null);
        } catch (JMSException e) {
            throw JMSUtil.convertJmsException(e);
        }
    }

    private void receiveAndRespond() {
        try (ResourceCloser closer = new ResourceCloser()) {
            Connection connection = closer.register(connectionFactory.createConnection());
            connection.start();
            Session session = closer.register(connection.createSession(false, Session.AUTO_ACKNOWLEDGE));
            MessageConsumer consumer = closer.register(session.createConsumer(session
                .createQueue(receiveQueueName)));
            final jakarta.jms.Message inMessage = consumer.receive(10000);
            if (inMessage == null) {
                //System.out.println("TestReceiver timed out");
                throw new RuntimeException("No message received on destination " + receiveQueueName);
            }
            requestMessageId = inMessage.getJMSMessageID();
            //System.out.println("Received message " + requestMessageId);
            final TextMessage replyMessage = session.createTextMessage("Result");
            String correlationId = (forceMessageIdAsCorrelationId || inMessage.getJMSCorrelationID() == null)
                ? inMessage.getJMSMessageID() : inMessage.getJMSCorrelationID();
            replyMessage.setJMSCorrelationID(correlationId);
            Destination replyDest = staticReplyQueue != null
                ? session.createQueue(staticReplyQueue) : inMessage.getJMSReplyTo();
            if (replyDest != null) {
                final MessageProducer producer = closer
                    .register(session.createProducer(replyDest));
                //System.out.println("Sending reply with correlation id " + correlationId + " to " + replyDest);
                producer.send(replyMessage);
            }
        } catch (Throwable e) {
            ex = e;
        }
    }

    public void runAsync() {
        drainQueue();
        CompletableFuture.runAsync(() -> {
            receiveAndRespond();
        });
    }

    public void close() {
        if (ex != null) {
            throw new RuntimeException("Error while receiving message or sending reply", ex);
        }
    }
}
