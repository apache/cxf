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

import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.service.model.EndpointInfo;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiTemplate;

public class JMSOldConfigHolder {
    private static final Logger LOG = LogUtils.getL7dLogger(JMSOldConfigHolder.class);
    private ClientConfig clientConfig;
    private ClientBehaviorPolicyType runtimePolicy;

    private AddressType address;
    private SessionPoolType sessionPool;
    private JMSConfiguration jmsConfig;
    private ServerConfig serverConfig;
    private ServerBehaviorPolicyType serverBehavior;

    public JMSConfiguration createJMSConfigurationFromEndpointInfo(Bus bus,
                                                                   EndpointInfo endpointInfo,
                                                                   boolean isConduit) {

        // Retrieve configuration information that was extracted from the WSDL
        address = endpointInfo.getTraversedExtensor(new AddressType(), AddressType.class);
        clientConfig = endpointInfo.getTraversedExtensor(new ClientConfig(), ClientConfig.class);
        runtimePolicy = endpointInfo.getTraversedExtensor(new ClientBehaviorPolicyType(),
                                                          ClientBehaviorPolicyType.class);
        serverConfig = endpointInfo.getTraversedExtensor(new ServerConfig(), ServerConfig.class);
        sessionPool = endpointInfo.getTraversedExtensor(new SessionPoolType(), SessionPoolType.class);
        serverBehavior = endpointInfo.getTraversedExtensor(new ServerBehaviorPolicyType(),
                                                           ServerBehaviorPolicyType.class);
        String name = endpointInfo.getName().toString() + (isConduit ? ".jms-conduit" : ".jms-destination");
       
        // Try to retrieve configuration information from the spring
        // config. Search for a conduit or destination with name=endpoint name + ".jms-conduit"
        // or ".jms-destination"
        
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(name, this);
        }
        
        if (jmsConfig == null) {
            jmsConfig = new JMSConfiguration();
        }

        if (jmsConfig.isUsingEndpointInfo()) {
            JndiTemplate jt = new JndiTemplate();
            jt.setEnvironment(JMSOldConfigHolder.getInitialContextEnv(address));
            boolean pubSubDomain = false;
            if (address.isSetDestinationStyle()) {
                pubSubDomain = DestinationStyleType.TOPIC == address.getDestinationStyle();
            }
            JNDIConfiguration jndiConfig = new JNDIConfiguration();
            jndiConfig.setJndiConnectionFactoryName(address.getJndiConnectionFactoryName());
            jmsConfig.setJndiTemplate(jt);
            jndiConfig.setConnectionUserName(address.getConnectionUserName());
            jndiConfig.setConnectionPassword(address.getConnectionPassword());
            jmsConfig.setJndiConfig(jndiConfig);
            if (address.isSetReconnectOnException()) {
                jmsConfig.setReconnectOnException(address.isReconnectOnException());
            }
            jmsConfig.setDurableSubscriptionName(serverBehavior.getDurableSubscriberName());
            jmsConfig.setExplicitQosEnabled(true);
            if (jmsConfig.getMessageSelector() == null) {
                jmsConfig.setMessageSelector(serverBehavior.getMessageSelector());
            }
            if (isConduit && runtimePolicy.isSetMessageType()) {
                jmsConfig.setMessageType(runtimePolicy.getMessageType().value());
            }        
            jmsConfig.setPubSubDomain(pubSubDomain);
            jmsConfig.setPubSubNoLocal(true);
            if (clientConfig.isSetClientReceiveTimeout()) {
                jmsConfig.setReceiveTimeout(clientConfig.getClientReceiveTimeout());
            }
            if (clientConfig.isSetUseConduitIdSelector()) {
                jmsConfig.setUseConduitIdSelector(clientConfig.isUseConduitIdSelector());
            }
            if (clientConfig.isSetConduitSelectorPrefix()) {
                jmsConfig.setConduitSelectorPrefix(clientConfig.getConduitSelectorPrefix());
            }
            jmsConfig.setEnforceSpec(clientConfig.isEnforceSpec());
            jmsConfig.setSubscriptionDurable(serverBehavior.isSetDurableSubscriberName());       
            jmsConfig.setDurableSubscriptionName(serverBehavior.getDurableSubscriberName());
            jmsConfig.setDurableSubscriptionClientId(serverConfig.getDurableSubscriptionClientId());
            if (sessionPool.isSetHighWaterMark()) {
                jmsConfig.setMaxConcurrentTasks(sessionPool.getHighWaterMark());
            }
            long timeToLive = isConduit ? clientConfig.getMessageTimeToLive() : serverConfig
                .getMessageTimeToLive();
            jmsConfig.setTimeToLive(timeToLive);            
            if (address.isSetUseJms11()) {                
                jmsConfig.setUseJms11(address.isUseJms11());        
            }
            if (serverBehavior.isSetTransactional()) {
                jmsConfig.setSessionTransacted(serverBehavior.isTransactional());                
            }
            boolean useJndi = address.isSetJndiDestinationName();
            if (useJndi) {
                // Setup Destination jndi destination resolver
                final JndiDestinationResolver jndiDestinationResolver = new JndiDestinationResolver();
                jndiDestinationResolver.setJndiTemplate(jt);
                jmsConfig.setDestinationResolver(jndiDestinationResolver);
                jmsConfig.setTargetDestination(address.getJndiDestinationName());
                jmsConfig.setReplyDestination(address.getJndiReplyDestinationName());
            } else {
                // Use the default dynamic destination resolver
                jmsConfig.setTargetDestination(address.getJmsDestinationName());
                jmsConfig.setReplyDestination(address.getJmsReplyDestinationName());
            }
        }
        return jmsConfig;
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public ClientBehaviorPolicyType getRuntimePolicy() {
        return runtimePolicy;
    }

    public void setRuntimePolicy(ClientBehaviorPolicyType runtimePolicy) {
        this.runtimePolicy = runtimePolicy;
    }

    public AddressType getAddress() {
        return address;
    }

    public void setAddress(AddressType address) {
        this.address = address;
    }

    public SessionPoolType getSessionPool() {
        return sessionPool;
    }

    public void setSessionPool(SessionPoolType sessionPool) {
        this.sessionPool = sessionPool;
    }

    public JMSConfiguration getJmsConfig() {
        return jmsConfig;
    }

    public void setJmsConfig(JMSConfiguration jmsConfig) {
        this.jmsConfig = jmsConfig;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public ServerBehaviorPolicyType getServerBehavior() {
        return serverBehavior;
    }

    public void setServerBehavior(ServerBehaviorPolicyType serverBehavior) {
        this.serverBehavior = serverBehavior;
    }

    public static Properties getInitialContextEnv(AddressType addrType) {
        Properties env = new Properties();
        java.util.ListIterator listIter = addrType.getJMSNamingProperty().listIterator();
        while (listIter.hasNext()) {
            JMSNamingPropertyType propertyPair = (JMSNamingPropertyType)listIter.next();
            if (null != propertyPair.getValue()) {
                env.setProperty(propertyPair.getName(), propertyPair.getValue());
            }
        }
        if (LOG.isLoggable(Level.FINE)) {
            Enumeration props = env.propertyNames();
            while (props.hasMoreElements()) {
                String name = (String)props.nextElement();
                String value = env.getProperty(name);
                LOG.log(Level.FINE, "Context property: " + name + " | " + value);
            }
        }
        return env;
    }
}
