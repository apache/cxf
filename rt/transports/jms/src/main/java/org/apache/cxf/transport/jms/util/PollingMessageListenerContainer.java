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
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import javax.transaction.Status;
import javax.transaction.Transaction;

import org.apache.cxf.common.logging.LogUtils;

public class PollingMessageListenerContainer extends AbstractMessageListenerContainer {
    private static final Logger LOG = LogUtils.getL7dLogger(PollingMessageListenerContainer.class);

    public PollingMessageListenerContainer(Connection connection, Destination destination,
                                           MessageListener listenerHandler) {
        this.connection = connection;
        this.destination = destination;
        this.listenerHandler = listenerHandler;
    }

    private class Poller implements Runnable {

        @Override
        public void run() {
            while (running) {
                MessageConsumer consumer = null;
                Session session = null;
                try {
                    // Create session early to optimize performance
                    session = connection.createSession(transacted, acknowledgeMode);
                    consumer = createConsumer(session);
                    while (running) {
                        Message message = consumer.receive(1000);
                        try {
                            if (message != null) {
                                listenerHandler.onMessage(message);
                            }
                            if (session.getTransacted()) {
                                session.commit();
                            }
                        } catch (Exception e) {
                            LOG.log(Level.WARNING, "Exception while processing jms message in cxf. Rolling back", e);
                            safeRollBack(session, e);
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Unexpected exception. Restarting session and consumer", e);
                } finally {
                    ResourceCloser.close(consumer);
                    ResourceCloser.close(session);
                }
            }

        }
        
        private void safeRollBack(Session session, Exception e) {
            try {
                if (session.getTransacted()) {
                    session.rollback();
                }
            } catch (Exception e1) {
                LOG.log(Level.WARNING, "Rollback of Local transaction failed", e1);
            }
        }

    }
    
    private class XAPoller implements Runnable {

        @Override
        public void run() {
            while (running) {
                MessageConsumer consumer = null;
                Session session = null;
                try {
                    final Transaction externalTransaction = transactionManager.getTransaction();
                    if ((externalTransaction != null) && (externalTransaction.getStatus() == Status.STATUS_ACTIVE)) {
                        LOG.log(Level.SEVERE, "External transactions are not supported in XAPoller");
                        throw new IllegalStateException("External transactions are not supported in XAPoller");
                    }
                    transactionManager.begin();
                    /*
                     * Create session inside transaction to give it the 
                     * chance to enlist itself as a resource
                     */
                    session = connection.createSession(transacted, acknowledgeMode);
                    consumer = createConsumer(session);
                    Message message = consumer.receive(1000);
                    try {
                        if (message != null) {
                            listenerHandler.onMessage(message);
                        }
                        transactionManager.commit();
                    } catch (Throwable e) {
                        LOG.log(Level.WARNING, "Exception while processing jms message in cxf. Rolling back", e);
                        safeRollBack(session);
                    } finally {
                        ResourceCloser.close(consumer);
                        ResourceCloser.close(session);
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Unexpected exception. Restarting session and consumer", e);
                }

            }

        }
        
        private void safeRollBack(Session session) {
            try {
                transactionManager.rollback();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Rollback of XA transaction failed", e);
            }
        }

    }
    
    private MessageConsumer createConsumer(Session session) throws JMSException {
        if (durableSubscriptionName != null && destination instanceof Topic) {
            return session.createDurableSubscriber((Topic)destination, durableSubscriptionName,
                                                   messageSelector, pubSubNoLocal);
        } else {
            return session.createConsumer(destination, messageSelector);
        }
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        for (int c = 0; c < getConcurrentConsumers(); c++) {
            Runnable poller = (transactionManager != null) ? new XAPoller() : new Poller(); 
            getExecutor().execute(poller);
        }
    }

    @Override
    public void stop() {
        LOG.fine("Shuttting down " + this.getClass().getSimpleName());
        if (!running) {
            return;
        }
        running = false;
        super.stop();        
    }

    @Override
    public void shutdown() {
        stop();
    }

}
