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
import java.util.logging.Level;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.XASession;
import javax.transaction.TransactionManager;

/**
 * Listen for messages on a queue or topic asynchronously by registering a
 * MessageListener.
 * 
 * Warning: This class does not refresh connections when the server goes away
 * This has to be handled outside.
 */
public class MessageListenerContainer extends AbstractMessageListenerContainer {
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
                if (session.getTransacted()) {
                    session.commit();
                }
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
    
    static class XATransactionalMessageListener implements MessageListener {
        private TransactionManager tm;
        private MessageListener listenerHandler;
        private XASession session;
        
        public XATransactionalMessageListener(TransactionManager tm, Session session, MessageListener listenerHandler) {
            if (tm == null) {
                throw new IllegalArgumentException("Must supply a transaction manager");
            }
            if (!(session instanceof XASession)) {
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
