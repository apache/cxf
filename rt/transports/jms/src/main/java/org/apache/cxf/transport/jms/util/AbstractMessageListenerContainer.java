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

import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;
import jakarta.transaction.TransactionManager;
import org.apache.cxf.common.logging.LogUtils;

public abstract class AbstractMessageListenerContainer implements JMSListenerContainer {

    protected static final Logger LOG = LogUtils.getL7dLogger(MessageListenerContainer.class);
    protected Connection connection;
    protected Destination destination;
    protected MessageListener listenerHandler;
    protected boolean transacted;
    protected int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
    protected String messageSelector;
    protected volatile boolean running;
    protected String durableSubscriptionName;
    protected boolean pubSubNoLocal;
    protected TransactionManager transactionManager;
    protected Properties jndiEnvironment;

    private Executor executor;
    private int concurrentConsumers = 1;
    private boolean internalExecutor;

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
            executor = Executors.newFixedThreadPool(concurrentConsumers);
            internalExecutor = true;
        }
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setJndiEnvironment(Properties jndiEnvironment) {
        this.jndiEnvironment = jndiEnvironment;
    }

    /**
     * Creates a InitialContext if a JNDI environment has been provided.
     * This is usefull in e.g. weblogic, where interaction with JNDI JMS resources is secured.
     *
     * Be careful not to cache the return value in a non thread local scope.
     *
     * @return an initial context, with the endpoint's JNDI properties,
     * or null if none is provided or if an errur occurs
     **/
    public InitialContext createInitialContext() {
        if (jndiEnvironment != null) {
            try {
                return new InitialContext(this.jndiEnvironment);
            } catch (NamingException e) {
                LOG.log(Level.SEVERE, "Could not expose JNDI environment to JMS thread context", e);
            }
        }
        return null;
    }

    @Override
    public void stop() {
        // In case of using external executor, don't shutdown it
        if ((executor == null) || !internalExecutor) {
            return;
        }

        ExecutorService executorService = (ExecutorService)executor;
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Ignore
        }
        executorService.shutdownNow();
        executor = null;
        internalExecutor = false;
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

    public void setConcurrentConsumers(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }

    public int getConcurrentConsumers() {
        return concurrentConsumers;
    }

}
