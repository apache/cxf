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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
import javax.jms.XASession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import org.apache.cxf.common.logging.LogUtils;

public class MessageListenerContainer implements JMSListenerContainer {
    private static final Logger LOG = LogUtils.getL7dLogger(MessageListenerContainer.class);

    private Connection connection;
    private Destination destination;
    private MessageListener listenerHandler;
    private boolean transacted;
    private int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
    private String messageSelector;
    private boolean running;
    private MessageConsumer consumer;
    private Session session;
    private Executor executor;
    private String durableSubscriptionName;
    private boolean pubSubNoLocal;
    private TransactionManager transactionManager;

    public MessageListenerContainer(Connection connection, Destination destination,
                                    MessageListener listenerHandler) {
        this.connection = connection;
        this.destination = destination;
        this.listenerHandler = listenerHandler;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }

    public void setAcknowledgeMode(int acknowledgeMode) {
        this.acknowledgeMode = acknowledgeMode;
    }

    public void setMessageSelector(String messageSelector) {
        this.messageSelector = messageSelector;
    }

    protected Executor getExecutor() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(10);
        }
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setDurableSubscriptionName(String durableSubscriptionName) {
        this.durableSubscriptionName = durableSubscriptionName;
    }

    public void setPubSubNoLocal(boolean pubSubNoLocal) {
        this.pubSubNoLocal = pubSubNoLocal;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void start() {
        try {
            session = connection.createSession(transacted, acknowledgeMode);
            if (durableSubscriptionName != null) {
                consumer = session.createDurableSubscriber((Topic)destination, durableSubscriptionName,
                                                           messageSelector, pubSubNoLocal);
            } else {
                consumer = session.createConsumer(destination, messageSelector);
            }
            
            MessageListener intListener = (transactionManager != null)
                ? new XATransactionalMessageListener(transactionManager, session, listenerHandler)
                : new LocalTransactionalMessageListener(session, listenerHandler); 
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

    protected TransactionManager getTransactionManager() {
        if (this.transactionManager == null) {
            try {
                InitialContext ctx = new InitialContext();
                this.transactionManager = (TransactionManager)ctx
                    .lookup("javax.transaction.TransactionManager");
            } catch (NamingException e) {
                // Ignore
            }
        }
        return this.transactionManager;
    }

    static class DispachingListener implements MessageListener {
        private Executor executor;
        private MessageListener listenerHandler;

        public DispachingListener(Executor executor, MessageListener listenerHandler) {
            this.executor = executor;
            this.listenerHandler = listenerHandler;
        }

        @Override
        public void onMessage(final Message message) {
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    listenerHandler.onMessage(message);
                }

            });
        }

    }
    
    static class LocalTransactionalMessageListener implements MessageListener {
        private MessageListener listenerHandler;
        private Session session;
        
        public LocalTransactionalMessageListener(Session session, MessageListener listenerHandler) {
            this.session = session;
            this.listenerHandler = listenerHandler;
        }

        @Override
        public void onMessage(Message message) {
            try {
                listenerHandler.onMessage(message);
                session.commit();
            } catch (Throwable e) {
                safeRollback(e);
            }
        }
        
        private void safeRollback(Throwable t) {
            LOG.log(Level.WARNING, "Exception while processing jms message in cxf. Rolling back" , t);
            try {
                session.rollback();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Rollback of Local transaction failed", e);
            }
        }
        
    }
    
    @SuppressWarnings("PMD")
    static class XATransactionalMessageListener implements MessageListener {
        private TransactionManager tm;
        private MessageListener listenerHandler;
        private XASession session;
        
        public XATransactionalMessageListener(TransactionManager tm, Session session, MessageListener listenerHandler) {
            if (tm == null) {
                throw new IllegalArgumentException("Must supply a transaction manager");
            }
            if (session == null || !(session instanceof XASession)) {
                throw new IllegalArgumentException("Must supply an XASession");
            }
            this.tm = tm;
            this.session = (XASession)session;
            this.listenerHandler = listenerHandler;
        }

        @Override
        public void onMessage(Message message) {
            try {
                tm.begin();
                tm.getTransaction().enlistResource(session.getXAResource());
                listenerHandler.onMessage(message);
                tm.commit();
            } catch (Throwable e) {
                safeRollback(e);
            }
        }
        
        private void safeRollback(Throwable t) {
            LOG.log(Level.WARNING, "Exception while processing jms message in cxf. Rolling back" , t);
            try {
                tm.rollback();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Rollback of JTA transaction failed", e);
            }
        }
        
    }
}
