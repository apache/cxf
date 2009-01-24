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
package org.apache.cxf.transport.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Message;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.transaction.PlatformTransactionManager;

public class JMSConfiguration implements InitializingBean {
    static final boolean DEFAULT_USEJMS11 = true;
    
    private boolean usingEndpointInfo = true;
    private ConnectionFactory connectionFactory;
    private DestinationResolver destinationResolver;
    private PlatformTransactionManager transactionManager;
    private TaskExecutor taskExecutor;
    private boolean useJms11 = DEFAULT_USEJMS11;
    private boolean useJndi;
    private boolean messageIdEnabled = true;
    private boolean messageTimestampEnabled = true;
    private boolean pubSubNoLocal;
    private long receiveTimeout = JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT;
    private boolean explicitQosEnabled;
    private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;
    private int priority = Message.DEFAULT_PRIORITY;
    private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;
    private boolean sessionTransacted;
    
    private int concurrentConsumers = 1;
    private int maxConcurrentConsumers = 1;
    private int maxConcurrentTasks = 10;

    private volatile String messageSelector;
    private boolean subscriptionDurable;
    private String durableSubscriptionName;

    private String targetDestination;
    private String replyDestination;
    private String messageType = JMSConstants.TEXT_MESSAGE_TYPE;
    private boolean pubSubDomain;
    private boolean useConduitIdSelector = true;

    public boolean isUsingEndpointInfo() {
        return this.usingEndpointInfo;
    }
    
    public void setUsingEndpointInfo(boolean usingEndpointInfo) {
        this.usingEndpointInfo = usingEndpointInfo;
    }
    public boolean isUseJndi() {
        return useJndi;
    }

    public void setUseJndi(boolean useJndi) {
        this.useJndi = useJndi;
    }

    public boolean isMessageIdEnabled() {
        return messageIdEnabled;
    }

    public void setMessageIdEnabled(boolean messageIdEnabled) {
        this.messageIdEnabled = messageIdEnabled;
    }

    public boolean isMessageTimestampEnabled() {
        return messageTimestampEnabled;
    }

    public void setMessageTimestampEnabled(boolean messageTimestampEnabled) {
        this.messageTimestampEnabled = messageTimestampEnabled;
    }

    public boolean isPubSubNoLocal() {
        return pubSubNoLocal;
    }

    public void setPubSubNoLocal(boolean pubSubNoLocal) {
        this.pubSubNoLocal = pubSubNoLocal;
    }

    public long getReceiveTimeout() {
        return receiveTimeout;
    }

    public void setReceiveTimeout(long receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
    }

    public boolean isExplicitQosEnabled() {
        return explicitQosEnabled;
    }

    public void setExplicitQosEnabled(boolean explicitQosEnabled) {
        this.explicitQosEnabled = explicitQosEnabled;
    }

    public int getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(int deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    public String getMessageSelector() {
        return messageSelector;
    }

    public void setMessageSelector(String messageSelector) {
        this.messageSelector = messageSelector;
    }

    public boolean isSubscriptionDurable() {
        return subscriptionDurable;
    }

    public void setSubscriptionDurable(boolean subscriptionDurable) {
        this.subscriptionDurable = subscriptionDurable;
    }

    public String getDurableSubscriptionName() {
        return durableSubscriptionName;
    }

    public void setDurableSubscriptionName(String durableSubscriptionName) {
        this.durableSubscriptionName = durableSubscriptionName;
    }

    public void afterPropertiesSet() throws Exception {
        if (connectionFactory == null) {
            throw new RuntimeException("Required property connectionfactory was not set");
        }
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    @Required
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public String getTargetDestination() {
        return targetDestination;
    }

    public void setTargetDestination(String targetDestination) {
        this.targetDestination = targetDestination;
    }

    public String getReplyDestination() {
        return replyDestination;
    }

    public void setReplyDestination(String replyDestination) {
        this.replyDestination = replyDestination;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public boolean isPubSubDomain() {
        return pubSubDomain;
    }

    public void setPubSubDomain(boolean pubSubDomain) {
        this.pubSubDomain = pubSubDomain;
    }

    public boolean isUseJms11() {
        return useJms11;
    }

    public void setUseJms11(boolean useJms11) {
        this.useJms11 = useJms11;
    }

    public DestinationResolver getDestinationResolver() {
        return destinationResolver;
    }

    public void setDestinationResolver(DestinationResolver destinationResolver) {
        this.destinationResolver = destinationResolver;
    }

    public boolean isSessionTransacted() {
        return sessionTransacted;
    }

    public void setSessionTransacted(boolean sessionTransacted) {
        this.sessionTransacted = sessionTransacted;
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public int getConcurrentConsumers() {
        return concurrentConsumers;
    }

    public void setConcurrentConsumers(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }

    public int getMaxConcurrentConsumers() {
        return maxConcurrentConsumers;
    }

    public void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
        this.maxConcurrentConsumers = maxConcurrentConsumers;
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void setUseConduitIdSelector(boolean useConduitIdSelector) {
        this.useConduitIdSelector = useConduitIdSelector;
    }

    public boolean isUseConduitIdSelector() {
        return useConduitIdSelector;
    }

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public void setMaxConcurrentTasks(int maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

}
