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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;

public class MessageListenerContainer implements JMSListenerContainer {
    
    private Connection connection;
    private Destination replyTo;
    private MessageListener listenerHandler;
    private boolean transacted;
    private int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
    private String messageSelector;
    private boolean running;
    private MessageConsumer consumer;
    private Session session;
    private ExecutorService executor;
    private String durableSubscriptionName;
    private boolean pubSubNoLocal;

    public MessageListenerContainer(Connection connection, 
                                    Destination replyTo,
                                    MessageListener listenerHandler) {
        this.connection = connection;
        this.replyTo = replyTo;
        this.listenerHandler = listenerHandler;
        executor = Executors.newFixedThreadPool(20);
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
    
    private ExecutorService getExecutor() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(10);
        }
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
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
    
    @Override
    public void start() {
        try {
            session = connection.createSession(transacted, acknowledgeMode);
            if (durableSubscriptionName != null) {
                consumer = session.createDurableSubscriber((Topic)replyTo, durableSubscriptionName,
                                                           messageSelector, pubSubNoLocal);
            } else {
                consumer = session.createConsumer(replyTo, messageSelector);
            }
            consumer.setMessageListener(listenerHandler);
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

    class DispachingListener implements MessageListener {

        @Override
        public void onMessage(final Message message) {
            getExecutor().execute(new Runnable() {
                
                @Override
                public void run() {
                    try {
                        listenerHandler.onMessage(message);
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            });
        }
        
    }
}
