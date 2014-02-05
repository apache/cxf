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

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.XAConnectionFactory;
import javax.naming.NamingException;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.jms.util.JMSSender;
import org.apache.cxf.transport.jms.util.ResourceCloser;
import org.apache.cxf.transport.jms.util.SessionFactory;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * Factory to create JmsTemplates and JmsListeners from configuration and context information
 */
public final class JMSFactory {
    static final String MESSAGE_ENDPOINT_FACTORY = "MessageEndpointFactory";
    static final String MDB_TRANSACTED_METHOD = "MDBTransactedMethod";

    private static final Logger LOG = LogUtils.getL7dLogger(JMSFactory.class);
    
    private JMSFactory() {
    }

    /**
     * Retrieve connection factory from jndi, wrap it in a UserCredentialsConnectionFactoryAdapter,
     * set username and password and return the ConnectionFactory
     * 
     * @param jmsConfig
     * @param jndiConfig
     * @return
     */
    static ConnectionFactory getConnectionFactoryFromJndi(JMSConfiguration jmsConfig) {
        JNDIConfiguration jndiConfig = jmsConfig.getJndiConfig();
        if (jndiConfig == null) {
            return null;
        }
        String connectionFactoryName = jndiConfig.getJndiConnectionFactoryName();
        if (connectionFactoryName == null) {
            return null;
        }
        String userName = jndiConfig.getConnectionUserName();
        String password = jndiConfig.getConnectionPassword();
        try {
            ConnectionFactory cf = (ConnectionFactory)jmsConfig.getJndiTemplate().
                lookup(connectionFactoryName);
            if (!(cf instanceof SingleConnectionFactory)) {
                UserCredentialsConnectionFactoryAdapter uccf = new UserCredentialsConnectionFactoryAdapter();
                uccf.setUsername(userName);
                uccf.setPassword(password);
                uccf.setTargetConnectionFactory(cf);
                cf = uccf;
            }
            
            return cf;
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Create JmsTemplate from configuration information. Most settings are taken from jmsConfig. The QoS
     * settings in headers override the settings from jmsConfig
     * 
     * @param jmsConfig configuration information
     * @param messageProperties context headers
     * @return
     */
    public static JMSSender createJmsSender(JMSConfiguration jmsConfig,
                                            JMSMessageHeadersType messageProperties) {
        JMSSender sender = new JMSSender();
        long timeToLive = (messageProperties != null && messageProperties.isSetTimeToLive())
            ? messageProperties.getTimeToLive() : jmsConfig.getTimeToLive();
        sender.setTimeToLive(timeToLive);
        int priority = (messageProperties != null && messageProperties.isSetJMSPriority())
            ? messageProperties.getJMSPriority() : jmsConfig.getPriority();
        sender.setPriority(priority);
        int deliveryMode = (messageProperties != null && messageProperties.isSetJMSDeliveryMode())
            ? messageProperties.getJMSDeliveryMode() : jmsConfig.getDeliveryMode();
        sender.setDeliveryMode(deliveryMode);
        sender.setExplicitQosEnabled(jmsConfig.isExplicitQosEnabled());
        return sender;
    }

    /**
     * Create and start listener using configuration information from jmsConfig. Uses
     * resolveOrCreateDestination to determine the destination for the listener.
     * 
     * @param ei the EndpointInfo for the listener
     * @param jmsConfig configuration information
     * @param listenerHandler object to be called when a message arrives
     * @param destination to listen on
     * @return
     */
    public static AbstractMessageListenerContainer createJmsListener(EndpointInfo ei,
                                                                    JMSConfiguration jmsConfig,
                                                                    MessageListener listenerHandler,
                                                                    Destination destination) {
        
        if (jmsConfig.getMessageListenerContainer() != null) {
            AbstractMessageListenerContainer  jmsListener =  jmsConfig.getMessageListenerContainer();
            if (jmsListener.getMessageListener() == null) {
                jmsListener.setMessageListener(listenerHandler);
                jmsListener.initialize();
                jmsListener.start();
            }
            return jmsListener;
        }
        
        DefaultMessageListenerContainer jmsListener = null;
        
        //Check to see if transport is being used in JCA RA with XA
        Method method = ei.getProperty(MDB_TRANSACTED_METHOD,
                                       java.lang.reflect.Method.class);
        MessageEndpointFactory factory = ei.getProperty(MESSAGE_ENDPOINT_FACTORY, 
                                      MessageEndpointFactory.class);
        if (method != null 
            && jmsConfig.getConnectionFactory() instanceof XAConnectionFactory) {
            jmsListener = new JCATransactionalMessageListenerContainer(factory, method); 
        } else {
            jmsListener = new DefaultMessageListenerContainer();
        }
        
        return createJmsListener(jmsListener,
                                 jmsConfig,
                                 listenerHandler,
                                 destination,
                                 null);            
    }

    /**
     * Create and start listener using configuration information from jmsConfig. Uses
     * resolveOrCreateDestination to determine the destination for the listener.
     * 
     * @param jmsConfig configuration information
     * @param listenerHandler object to be called when a message arrives
     * @param destinationName null for temp dest or a destination name
     * @param conduitId id for message selector
     * @return
     */
    public static DefaultMessageListenerContainer createJmsListener(JMSConfiguration jmsConfig,
                                                                    MessageListener listenerHandler,
                                                                    Destination destination, 
                                                                    String conduitId) {
        DefaultMessageListenerContainer jmsListener = new DefaultMessageListenerContainer(); 
        return createJmsListener(jmsListener,
                                 jmsConfig,
                                 listenerHandler,
                                 destination,
                                 conduitId);    
    }

    private static DefaultMessageListenerContainer createJmsListener(
                          DefaultMessageListenerContainer jmsListener,
                          JMSConfiguration jmsConfig,
                          MessageListener listenerHandler,
                          Destination destination,
                          String conduitId) {
        
        jmsListener.setConcurrentConsumers(jmsConfig.getConcurrentConsumers());
        jmsListener.setMaxConcurrentConsumers(jmsConfig.getMaxConcurrentConsumers());
        
        jmsListener.setPubSubNoLocal(jmsConfig.isPubSubNoLocal());
        
        jmsListener.setConnectionFactory(jmsConfig.getConnectionFactory());
        jmsListener.setSubscriptionDurable(jmsConfig.isSubscriptionDurable());
        jmsListener.setClientId(jmsConfig.getDurableSubscriptionClientId());
        jmsListener.setDurableSubscriptionName(jmsConfig.getDurableSubscriptionName());
        jmsListener.setSessionTransacted(jmsConfig.isSessionTransacted());
        jmsListener.setTransactionManager(jmsConfig.getTransactionManager());
        jmsListener.setMessageListener(listenerHandler);
        if (listenerHandler instanceof JMSDestination) {
            //timeout on server side?
            if (jmsConfig.getServerReceiveTimeout() != null) {
                jmsListener.setReceiveTimeout(jmsConfig.getServerReceiveTimeout());
            }
            jmsListener.setPubSubDomain(jmsConfig.isPubSubDomain());
        } else {
            if (jmsConfig.getReceiveTimeout() != null) {
                jmsListener.setReceiveTimeout(jmsConfig.getReceiveTimeout());
            }
            jmsListener.setPubSubDomain(jmsConfig.isReplyPubSubDomain());
        }
        if (jmsConfig.getRecoveryInterval() != JMSConfiguration.DEFAULT_VALUE) {
            jmsListener.setRecoveryInterval(jmsConfig.getRecoveryInterval());
        }
        if (jmsConfig.getCacheLevelName() != null && (jmsConfig.getCacheLevelName().trim().length() > 0)) {
            jmsListener.setCacheLevelName(jmsConfig.getCacheLevelName());
        } else if (jmsConfig.getCacheLevel() != JMSConfiguration.DEFAULT_VALUE) {
            jmsListener.setCacheLevel(jmsConfig.getCacheLevel());
        }
        if (jmsListener.getCacheLevel() >= DefaultMessageListenerContainer.CACHE_CONSUMER
            && jmsConfig.getMaxSuspendedContinuations() > 0) {
            LOG.info("maxSuspendedContinuations value will be ignored - "
                     + ", please set cacheLevel to the value less than "
                     + " org.springframework.jms.listener.DefaultMessageListenerContainer.CACHE_CONSUMER");
        }
        if (jmsConfig.isAcceptMessagesWhileStopping()) {
            jmsListener.setAcceptMessagesWhileStopping(jmsConfig.isAcceptMessagesWhileStopping());
        }
        String staticSelectorPrefix = jmsConfig.getConduitSelectorPrefix();
        String conduitIdSt = jmsConfig.isUseConduitIdSelector() && conduitId != null ? conduitId : "";
        String correlationIdPrefix = staticSelectorPrefix + conduitIdSt;
        
        if (!correlationIdPrefix.isEmpty()) {
            String messageSelector = "JMSCorrelationID LIKE '" + correlationIdPrefix + "%'";
            jmsListener.setMessageSelector(messageSelector);
        }
        
        jmsListener.setTaskExecutor(jmsConfig.getTaskExecutor());

        jmsListener.setDestination(destination);
        jmsListener.initialize();
        jmsListener.start();
        return jmsListener;
    }
    
    public static SessionFactory createJmsSessionFactory(JMSConfiguration jmsConfig, ResourceCloser closer) {
        SessionFactory sf = new SessionFactory(jmsConfig.getConnectionFactory(), closer);
        sf.setAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
        sf.setSessionTransacted(jmsConfig.isSessionTransacted());
        sf.setDurableSubscriptionClientId(jmsConfig.getDurableSubscriptionClientId());
        return sf;
    }
    
}
