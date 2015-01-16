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
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.transaction.TransactionManager;

import org.apache.cxf.common.logging.LogUtils;

public abstract class AbstractMessageListenerContainer implements JMSListenerContainer {

    protected static final Logger LOG = LogUtils.getL7dLogger(MessageListenerContainer.class);
    protected Connection connection;
    protected Destination destination;
    protected MessageListener listenerHandler;
    protected boolean transacted;
    protected int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
    protected String messageSelector;
    protected boolean running;
    protected Executor executor;
    protected String durableSubscriptionName;
    protected boolean pubSubNoLocal;
    protected TransactionManager transactionManager;

    public AbstractMessageListenerContainer() {
        super();
    }

    public Connection getConnection() {
        return connection;
    }

    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
        if (this.transacted) {
            this.acknowledgeMode = Session.SESSION_TRANSACTED;
        }
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

}
