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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.XAConnectionFactory;
import javax.naming.NamingException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.JmsTemplate102;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer102;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * Factory to create JmsTemplates and JmsListeners from configuration and context information
 */
public final class JMSFactory {

    private static final Logger LOG = LogUtils.getL7dLogger(JMSFactory.class);
    
    private JMSFactory() {
    }

    /**
     * Retreive connection factory from jndi, wrap it in a UserCredentialsConnectionFactoryAdapter,
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
     * @param headers context headers
     * @return
     */
    public static JmsTemplate createJmsTemplate(JMSConfiguration jmsConfig, JMSMessageHeadersType headers) {
        if (jmsConfig.getJmsTemplate() != null) {
            return jmsConfig.getJmsTemplate();
        }
        JmsTemplate jmsTemplate = jmsConfig.isUseJms11() ? new JmsTemplate() : new JmsTemplate102();
        jmsTemplate.setConnectionFactory(jmsConfig.getOrCreateWrappedConnectionFactory());
        jmsTemplate.setPubSubDomain(jmsConfig.isPubSubDomain());
        if (jmsConfig.getReceiveTimeout() != null) {
            jmsTemplate.setReceiveTimeout(jmsConfig.getReceiveTimeout());
        }
        jmsTemplate.setTimeToLive(jmsConfig.getTimeToLive());
        int priority = (headers != null && headers.isSetJMSPriority())
            ? headers.getJMSPriority() : jmsConfig.getPriority();
        jmsTemplate.setPriority(priority);
        int deliveryMode = (headers != null && headers.isSetJMSDeliveryMode()) ? headers
            .getJMSDeliveryMode() : jmsConfig.getDeliveryMode();
        jmsTemplate.setDeliveryMode(deliveryMode);
        jmsTemplate.setExplicitQosEnabled(jmsConfig.isExplicitQosEnabled());
        jmsTemplate.setSessionTransacted(jmsConfig.isSessionTransacted());
        if (jmsConfig.getDestinationResolver() != null) {
            jmsTemplate.setDestinationResolver(jmsConfig.getDestinationResolver());
        }
        return jmsTemplate;
    }

    public static AbstractMessageListenerContainer createJmsListener(EndpointInfo ei,
                                                                    JMSConfiguration jmsConfig,
                                                                    MessageListener listenerHandler,
                                                                    String destinationName, 
                                                                    String messageSelectorPrefix,
                                                                    boolean userCID) {
        
        if (jmsConfig.getMessageListenerContainer() != null) {
            AbstractMessageListenerContainer  jmsListener =  jmsConfig.getMessageListenerContainer();
            if (jmsListener.getMessageListener() == null) {
                jmsListener.setMessageListener(listenerHandler);
                jmsListener.initialize();
                jmsListener.start();
            }
            return jmsListener;
        }
        
        if (jmsConfig.getMessageListenerContainer() != null) {
            return jmsConfig.getMessageListenerContainer();
        }
        DefaultMessageListenerContainer jmsListener = null;
        
        if (jmsConfig.isUseJms11()) {
            //Check to see if transport is being used in JCA RA with XA
            Method method = ei.getProperty(JCATransactionalMessageListenerContainer.MDB_TRANSACTED_METHOD,
                                           java.lang.reflect.Method.class);
            if (method != null 
                && 
                jmsConfig.getConnectionFactory() instanceof XAConnectionFactory) {
                jmsListener = new JCATransactionalMessageListenerContainer(ei); 
            } else {
                jmsListener = new DefaultMessageListenerContainer();
            }
        } else {
            jmsListener = new DefaultMessageListenerContainer102();
        }
        
        return createJmsListener(jmsListener,
                                 jmsConfig,
                                 listenerHandler,
                                 destinationName, 
                                 messageSelectorPrefix,
                                 userCID);            
    }
    
    /**
     * Create and start listener using configuration information from jmsConfig. Uses
     * resolveOrCreateDestination to determine the destination for the listener.
     * 
     * @param jmsConfig configuration information
     * @param listenerHandler object to be called when a message arrives
     * @param destinationName null for temp dest or a destination name
     * @param messageSelectorPrefix prefix for the messageselector
     * @return
     */
    public static DefaultMessageListenerContainer createJmsListener(JMSConfiguration jmsConfig,
                                                                    MessageListener listenerHandler,
                                                                    String destinationName, 
                                                                    String messageSelectorPrefix,
                                                                    boolean userCID) {
        DefaultMessageListenerContainer jmsListener = jmsConfig.isUseJms11()
            ? new DefaultMessageListenerContainer() : new DefaultMessageListenerContainer102();
        
        return createJmsListener(jmsListener,
                                 jmsConfig,
                                 listenerHandler,
                                 destinationName, 
                                 messageSelectorPrefix,
                                 userCID);    
    }
    
    public static DefaultMessageListenerContainer 
    createJmsListener(DefaultMessageListenerContainer jmsListener,
                      JMSConfiguration jmsConfig,
                      MessageListener listenerHandler,
                      String destinationName, 
                      String messageSelectorPrefix,
                      boolean userCID) {
        
        jmsListener.setConcurrentConsumers(jmsConfig.getConcurrentConsumers());
        jmsListener.setMaxConcurrentConsumers(jmsConfig.getMaxConcurrentConsumers());
        jmsListener.setPubSubDomain(jmsConfig.isPubSubDomain());
        jmsListener.setPubSubNoLocal(jmsConfig.isPubSubNoLocal());
        jmsListener.setAutoStartup(true);
        jmsListener.setConnectionFactory(jmsConfig.getOrCreateWrappedConnectionFactory());
        jmsListener.setMessageSelector(jmsConfig.getMessageSelector());
        jmsListener.setSubscriptionDurable(jmsConfig.isSubscriptionDurable());
        jmsListener.setDurableSubscriptionName(jmsConfig.getDurableSubscriptionName());
        jmsListener.setSessionTransacted(jmsConfig.isSessionTransacted());
        jmsListener.setTransactionManager(jmsConfig.getTransactionManager());
        jmsListener.setMessageListener(listenerHandler);
        if (jmsConfig.getReceiveTimeout() != null) {
            jmsListener.setReceiveTimeout(jmsConfig.getReceiveTimeout());
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
        if (!userCID && messageSelectorPrefix != null && jmsConfig.isUseConduitIdSelector()) {
            jmsListener.setMessageSelector("JMSCorrelationID LIKE '" 
                                        + staticSelectorPrefix 
                                        + messageSelectorPrefix + "%'");
        } else if (staticSelectorPrefix.length() > 0) {
            jmsListener.setMessageSelector("JMSCorrelationID LIKE '" 
                                        + staticSelectorPrefix +  "%'");
        }
        if (jmsConfig.getDestinationResolver() != null) {
            jmsListener.setDestinationResolver(jmsConfig.getDestinationResolver());
        }
        if (jmsConfig.getTaskExecutor() != null) {
            setTaskExecutor(jmsListener, jmsConfig.getTaskExecutor());
            jmsListener.setTaskExecutor(jmsConfig.getTaskExecutor());
        } 
        
        if (jmsConfig.isAutoResolveDestination()) {
            jmsListener.setDestinationName(destinationName);
        } else {
            JmsTemplate jmsTemplate = createJmsTemplate(jmsConfig, null);
            Destination dest = JMSFactory.resolveOrCreateDestination(jmsTemplate, destinationName, jmsConfig
                .isPubSubDomain());
            jmsListener.setDestination(dest);
        }
        jmsListener.initialize();
        jmsListener.start();
        return jmsListener;
    }
    
    private static void setTaskExecutor(DefaultMessageListenerContainer jmsListener, TaskExecutor exec) {
        //CXF-2630 - The method sig for DefaultMessageListenerContainer.setTaskExecutor changed between 
        //Spring 2.5 and 3.0 and code compiled for one won't run on the other.   Thus, we need
        //to revert to using some reflection to make this call
        Exception ex = null;
        for (Method m : jmsListener.getClass().getMethods()) {
            if ("setTaskExecutor".equals(m.getName())
                && m.getParameterTypes().length == 1
                && m.getParameterTypes()[0].isInstance(exec)) {
                try {
                    m.invoke(jmsListener, exec);
                    return;
                } catch (Exception e) {
                    ex = e;
                }
            }
        }
        //if we get here, we couldn't find a valid method or something else went wrong
        if (ex != null) {
            LOG.log(Level.WARNING, "ERROR_SETTING_TASKEXECUTOR", ex);
        } else {
            LOG.log(Level.WARNING, "NO_SETTASKEXECUTOR_METHOD", jmsListener.getClass().getName());
        }
    }

    /**
     * If the destinationName given is null then a temporary destination is created else the destination name
     * is resolved using the resolver from the jmsConfig
     * 
     * @param jmsTemplate template to use for session and resolver
     * @param replyToDestinationName null for temporary destination or a destination name
     * @param pubSubDomain true=pubSub, false=Queues
     * @return resolved destination
     */
    protected static Destination resolveOrCreateDestination(final JmsTemplate jmsTemplate,
                                                          final String replyToDestinationName,
                                                          final boolean pubSubDomain) {
        return (Destination)jmsTemplate.execute(new SessionCallback() {
            public Object doInJms(Session session) throws JMSException {
                if (replyToDestinationName == null) {
                    if (session instanceof QueueSession) {
                        // For JMS 1.0.2
                        return ((QueueSession)session).createTemporaryQueue();
                    } else {
                        // For JMS 1.1
                        return session.createTemporaryQueue();
                    }
                }
                DestinationResolver resolv = jmsTemplate.getDestinationResolver();
                return resolv.resolveDestinationName(session, replyToDestinationName, pubSubDomain);
            }
        });
    }

}
