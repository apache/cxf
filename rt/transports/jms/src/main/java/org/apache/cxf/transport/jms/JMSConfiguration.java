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
import javax.jms.XAConnectionFactory;

import org.apache.cxf.configuration.ConfigurationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.connection.SingleConnectionFactory102;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jndi.JndiTemplate;
import org.springframework.transaction.PlatformTransactionManager;

public class JMSConfiguration implements InitializingBean {
    /**
     * The use of -1 is to make easier to determine 
     * if the setCacheLevel has been called.
     */
    public static final int DEFAULT_VALUE = -1;
    
    static final boolean DEFAULT_USEJMS11 = true;
    
    private boolean usingEndpointInfo = true;
    
    private JmsTemplate jmsTemplate;
    private AbstractMessageListenerContainer messageListenerContainer;
    
    private JndiTemplate jndiTemplate;
    private ConnectionFactory connectionFactory;
    private DestinationResolver destinationResolver;
    private PlatformTransactionManager transactionManager;
    private boolean wrapInSingleConnectionFactory = true;
    private TaskExecutor taskExecutor;
    private boolean useJms11 = DEFAULT_USEJMS11;
    private boolean reconnectOnException;
    private boolean messageIdEnabled = true;
    private boolean messageTimestampEnabled = true;
    private boolean pubSubNoLocal;
    private Long receiveTimeout;
    private boolean explicitQosEnabled;
    private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;
    private int priority = Message.DEFAULT_PRIORITY;
    private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;
    private boolean sessionTransacted;
    
    private int concurrentConsumers = 1;
    private int maxConcurrentConsumers = 1;
    private int maxConcurrentTasks = 10;
    private int maxSuspendedContinuations = DEFAULT_VALUE;

    private volatile String messageSelector;
    private boolean subscriptionDurable;
    private String durableSubscriptionClientId;
    private String durableSubscriptionName;

    private String targetDestination;
    private String replyDestination;
    private String messageType = JMSConstants.TEXT_MESSAGE_TYPE;
    private boolean pubSubDomain;
    private Boolean useConduitIdSelector;
    private String conduitSelectorPrefix;
    private boolean autoResolveDestination;
    private long recoveryInterval = DEFAULT_VALUE;
    private int cacheLevel = DEFAULT_VALUE;
    private String cacheLevelName;
    private Boolean enforceSpec;
    private boolean acceptMessagesWhileStopping;

    //For jms spec.
    private String targetService;
    private String requestURI;
    
    private ConnectionFactory wrappedConnectionFactory;
    
    private JNDIConfiguration jndiConfig;
    
    public void ensureProperlyConfigured(org.apache.cxf.common.i18n.Message msg) {
        if (targetDestination == null ||  getOrCreateWrappedConnectionFactory() == null) {
            System.out.println("targetDestination " + targetDestination);
            throw new ConfigurationException(msg);
        }
    }
    
    public String getCacheLevelName() {
        return cacheLevelName;
    }

    public void setCacheLevelName(String cacheLevelName) {
        this.cacheLevelName = cacheLevelName;
    }

    public int getCacheLevel() {
        return cacheLevel;
    }

    public void setCacheLevel(int cacheLevel) {
        this.cacheLevel = cacheLevel;
    }

    public long getRecoveryInterval() {
        return recoveryInterval;
    }

    public void setRecoveryInterval(long recoveryInterval) {
        this.recoveryInterval = recoveryInterval;
    }

    public boolean isAutoResolveDestination() {
        return autoResolveDestination;
    }

    public void setAutoResolveDestination(boolean autoResolveDestination) {
        this.autoResolveDestination = autoResolveDestination;
    }


    public boolean isUsingEndpointInfo() {
        return this.usingEndpointInfo;
    }
    
    public void setUsingEndpointInfo(boolean usingEndpointInfo) {
        this.usingEndpointInfo = usingEndpointInfo;
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

    public Long getReceiveTimeout() {
        return receiveTimeout;
    }

    public void setReceiveTimeout(Long receiveTimeout) {
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

    public void setConduitSelectorPrefix(String conduitSelectorPrefix) {
        this.conduitSelectorPrefix = conduitSelectorPrefix;
    }

    public String getConduitSelectorPrefix() {
        if (conduitSelectorPrefix == null) {
            return "";
        }
        return conduitSelectorPrefix;
    }

    public boolean isSetConduitSelectorPrefix() {
        return conduitSelectorPrefix != null;
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
    
    public int getMaxSuspendedContinuations() {
        return maxSuspendedContinuations;
    }

    public void setMaxSuspendedContinuations(int maxSuspendedContinuations) {
        this.maxSuspendedContinuations = maxSuspendedContinuations;
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
        if (useConduitIdSelector == null) {
            return true;
        }
        return useConduitIdSelector;
    }
    public boolean isSetUseConduitIdSelector() {
        return useConduitIdSelector != null;
    }
    
    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public void setMaxConcurrentTasks(int maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
    }

    public void setJndiTemplate(JndiTemplate jndiTemplate) {
        this.jndiTemplate = jndiTemplate;
    }

    public JndiTemplate getJndiTemplate() {
        return jndiTemplate;
    }

    public JNDIConfiguration getJndiConfig() {
        return jndiConfig;
    }

    public void setJndiConfig(JNDIConfiguration jndiConfig) {
        this.jndiConfig = jndiConfig;
    }

    public boolean isReconnectOnException() {
        return reconnectOnException;
    }

    public void setReconnectOnException(boolean reconnectOnException) {
        this.reconnectOnException = reconnectOnException;
    }

    public boolean isAcceptMessagesWhileStopping() {
        return acceptMessagesWhileStopping;
    }
    
    public void setAcceptMessagesWhileStopping(boolean acceptMessagesWhileStopping) {
        this.acceptMessagesWhileStopping = acceptMessagesWhileStopping;
    }
    
    /**
     * Tries to creates a ConnectionFactory from jndi if none was set as a property
     * by using the jndConfig. Then it determiens if the connectionFactory should be wrapped
     * into a SingleConnectionFactory and wraps it if necessary. After the first call the
     * same connectionFactory will be returned for all subsequent calls
     * 
     * @return usable connectionFactory
     */
    public ConnectionFactory getOrCreateWrappedConnectionFactory() {
        if (wrappedConnectionFactory == null) {
            if (connectionFactory == null) {
                connectionFactory = JMSFactory.getConnectionFactoryFromJndi(this);
            }
            if (wrapInSingleConnectionFactory && !(connectionFactory instanceof SingleConnectionFactory)) {
                SingleConnectionFactory scf;
                if (useJms11) {
                    if (connectionFactory instanceof XAConnectionFactory) {
                        scf = new XASingleConnectionFactory(connectionFactory);
                    } else {
                        scf = new SingleConnectionFactory(connectionFactory);
                    }
                } else {
                    scf = new SingleConnectionFactory102(connectionFactory, pubSubDomain);
                }
                if (getDurableSubscriptionClientId() != null) {
                    scf.setClientId(getDurableSubscriptionClientId());
                }
                scf.setReconnectOnException(isReconnectOnException());
                wrappedConnectionFactory = scf;
            } else {
                wrappedConnectionFactory = connectionFactory;
            }
        }
        return wrappedConnectionFactory;
    }

    /**
     * Only for tests
     * @return
     */
    protected ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public boolean isWrapInSingleConnectionFactory() {
        return wrapInSingleConnectionFactory;
    }

    public void setWrapInSingleConnectionFactory(boolean wrapInSingleConnectionFactory) {
        this.wrapInSingleConnectionFactory = wrapInSingleConnectionFactory;
    }

    public String getDurableSubscriptionClientId() {
        return durableSubscriptionClientId;
    }

    public void setDurableSubscriptionClientId(String durableSubscriptionClientId) {
        this.durableSubscriptionClientId = durableSubscriptionClientId;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    public String getTargetService() {
        return targetService;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public String getRequestURI() {
        return requestURI;
    }
    
    public boolean isEnforceSpec() {
        if (!isSetEnforceSpec()) {
            return true;
        }
        return enforceSpec;
    }

    public void setEnforceSpec(boolean enforceSpec) {
        this.enforceSpec = enforceSpec;
    }
    
    public boolean isSetEnforceSpec() {
        return this.enforceSpec != null;
    }

    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public JmsTemplate getJmsTemplate() {
        return jmsTemplate;
    }

    public AbstractMessageListenerContainer getMessageListenerContainer() {
        return messageListenerContainer;
    }

    public void setMessageListenerContainer(AbstractMessageListenerContainer messageListenerContainer) {
        this.messageListenerContainer = messageListenerContainer;
    }
}
