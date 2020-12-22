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

import java.util.logging.Level;

import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;
import jakarta.jms.Topic;

/**
 * Listen for messages on a queue or topic asynchronously by registering a
 * MessageListener.
 *
 * Warning: This class does not refresh connections when the server goes away
 * This has to be handled outside.
 */
public class MessageListenerContainer extends AbstractMessageListenerContainer {
    private MessageConsumer consumer;
    private Session session;
    public MessageListenerContainer(Connection connection, Destination destination,
                                    MessageListener listenerHandler) {
        this.connection = connection;
        this.destination = destination;
        this.listenerHandler = listenerHandler;
    }

    @Override
    public void start() {
        try {
            session = connection.createSession(transacted, acknowledgeMode);
            if (durableSubscriptionName != null && destination instanceof Topic) {
                consumer = session.createDurableSubscriber((Topic)destination, durableSubscriptionName,
                                                           messageSelector, pubSubNoLocal);
            } else {
                consumer = session.createConsumer(destination, messageSelector);
            }

            MessageListener intListener = new LocalTransactionalMessageListener(session, listenerHandler);
            // new DispachingListener(getExecutor(), listenerHandler);
            consumer.setMessageListener(intListener);

            running = true;
        } catch (JMSException e) {
            throw JMSUtil.convertJmsException(e);
        }
    }

    @Override
    public void stop() {
        running = false;
        ResourceCloser.close(consumer);
        ResourceCloser.close(session);
        consumer = null;
        session = null;
    }

    @Override
    public void shutdown() {
        stop();
        ResourceCloser.close(connection);
    }

    static class LocalTransactionalMessageListener implements MessageListener {
        private MessageListener listenerHandler;
        private Session session;

        LocalTransactionalMessageListener(Session session, MessageListener listenerHandler) {
            this.session = session;
            this.listenerHandler = listenerHandler;
        }

        @Override
        public void onMessage(Message message) {
            try {
                listenerHandler.onMessage(message);
                if (session.getTransacted()) {
                    session.commit();
                }
            } catch (Throwable e) {
                safeRollback(e);
            }
        }

        private void safeRollback(Throwable t) {
            LOG.log(Level.WARNING, "Exception while processing jms message in cxf. Rolling back", t);
            try {
                if (session.getTransacted()) {
                    session.rollback();
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Rollback of Local transaction failed", e);
            }
        }

    }

}
