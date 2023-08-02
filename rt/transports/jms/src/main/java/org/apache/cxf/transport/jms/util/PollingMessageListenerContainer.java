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

import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import jakarta.transaction.Status;
import jakarta.transaction.Transaction;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.apache.cxf.transport.jms.JMSFactory;

public class PollingMessageListenerContainer extends AbstractMessageListenerContainer {
    private static final Logger LOG = LogUtils.getL7dLogger(PollingMessageListenerContainer.class);
    private ExceptionListener exceptionListener;

    private JMSConfiguration jmsConfig;
    private boolean reply;

    public PollingMessageListenerContainer(JMSConfiguration jmsConfig, boolean isReply,
                                           MessageListener listenerHandler) {
        this(jmsConfig, isReply, listenerHandler, null);
    }

    public PollingMessageListenerContainer(JMSConfiguration jmsConfig, boolean isReply,
                                           MessageListener listenerHandler, ExceptionListener exceptionListener) {
        this.jmsConfig = jmsConfig;
        this.reply = isReply;
        this.listenerHandler = listenerHandler;
        this.exceptionListener = exceptionListener;
    }

    public PollingMessageListenerContainer(Connection connection, Destination destination,
                                           MessageListener listenerHandler, ExceptionListener exceptionListener) {
        this.connection = connection;
        this.destination = destination;
        this.listenerHandler = listenerHandler;
        this.exceptionListener = exceptionListener;
    }

    private final class Poller implements Runnable {

        @Override
        public void run() {
            while (running) {
                try (ResourceCloser closer = new ResourceCloser()) {
                    closer.register(createInitialContext());
                    Connection connection;
                    if (jmsConfig != null && jmsConfig.isOneSessionPerConnection()) {
                        connection = closer.register(createConnection());
                    } else {
                        connection = PollingMessageListenerContainer.this.connection;
                    }
                    // Create session early to optimize performance
                    Session session = closer.register(connection.createSession(transacted, acknowledgeMode));
                    MessageConsumer consumer = closer.register(createConsumer(connection, session));

                    while (running) {
                        Message message = consumer.receive(1000);
                        try {
                            if (message != null) {
                                listenerHandler.onMessage(message);

                                if (session.getTransacted()) {
                                    session.commit();
                                }
                            }
                        } catch (Throwable e) {
                            LOG.log(Level.WARNING, "Exception while processing jms message in cxf. Rolling back", e);
                            safeRollBack(session);
                        }
                    }
                } catch (Throwable e) {
                    handleException(e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        // Ignore
                    }
                }
            }
        }

        protected void safeRollBack(Session session) {
            try {
                if (session != null && session.getTransacted()) {
                    session.rollback();
                }
            } catch (Throwable e1) {
                LOG.log(Level.WARNING, "Rollback of Local transaction failed", e1);
            }
        }

    }

    private final class XAPoller implements Runnable {

        @Override
        public void run() {
            while (running) {
                try (ResourceCloser closer = new ResourceCloser()) {
                    closer.register(createInitialContext());
                    final Transaction externalTransaction = transactionManager.getTransaction();
                    if ((externalTransaction != null) && (externalTransaction.getStatus() == Status.STATUS_ACTIVE)) {
                        LOG.log(Level.SEVERE, "External transactions are not supported in XAPoller");
                        throw new IllegalStateException("External transactions are not supported in XAPoller");
                    }
                    transactionManager.begin();

                    Connection connection;
                    if (getConnection() == null) {
                        connection = closer.register(createConnection());
                    } else {
                        connection = getConnection();
                    }

                    /*
                     * Create session inside transaction to give it the
                     * chance to enlist itself as a resource
                     */
                    Session session = closer.register(connection.createSession(transacted, acknowledgeMode));
                    MessageConsumer consumer = closer.register(createConsumer(connection, session));
                    Message message = consumer.receive(1000);
                    try {
                        if (message != null) {
                            listenerHandler.onMessage(message);
                        }
                        transactionManager.commit();
                    } catch (Throwable e) {
                        LOG.log(Level.WARNING, "Exception while processing jms message in cxf. Rolling back", e);
                        safeRollBack();
                    }
                } catch (Throwable e) {
                    safeRollBack();
                    handleException(e);
                }
            }

        }

        private void safeRollBack() {
            try {
                transactionManager.rollback();
            } catch (Throwable e) {
                LOG.log(Level.WARNING, "Rollback of XA transaction failed", e);
            }
        }

    }

    private MessageConsumer createConsumer(final Connection connection, final Session session)
            throws JMSException {
        final MessageConsumer consumer;

        if (jmsConfig != null && jmsConfig.isOneSessionPerConnection()) {
            Destination destination;
            if (!isReply()) {
                destination = jmsConfig.getTargetDestination(session);
            } else {
                destination = jmsConfig.getReplyDestination(session);
            }
            consumer = createConsumer(destination, session);
            connection.start();
        } else {
            consumer = createConsumer(session);
        }

        return consumer;
    }

    private MessageConsumer createConsumer(Session session) throws JMSException {
        return createConsumer(this.destination, session);
    }

    private MessageConsumer createConsumer(Destination destination, Session session) throws JMSException {
        if (durableSubscriptionName != null && destination instanceof Topic) {
            return session.createDurableSubscriber((Topic)destination, durableSubscriptionName,
                                                   messageSelector, pubSubNoLocal);
        }
        return session.createConsumer(destination, messageSelector);
    }

    protected void handleException(Throwable e) {
        running = false;
        JMSException wrapped;
        if (e  instanceof JMSException) {
            wrapped = (JMSException) e;
        } else {
            wrapped = new JMSException("Wrapped exception. " + e.getMessage());
            wrapped.addSuppressed(e);
        }
        if (this.exceptionListener != null) {
            this.exceptionListener.onException(wrapped);
        }
    }

    private boolean isReply() {
        return reply;
    }

    private Connection createConnection() {
        try {
            return JMSFactory.createConnection(jmsConfig);
        } catch (JMSException e) {
            handleException(e);
            throw JMSUtil.convertJmsException(e);
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
        LOG.fine("Shutting down " + this.getClass().getSimpleName());
        running = false;
        super.stop();
    }

    @Override
    public void shutdown() {
        stop();
    }

}
