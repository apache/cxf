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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.naming.Context;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;
import org.apache.cxf.transport.jms.uri.JMSEndpoint;
import org.apache.cxf.transport.jms.util.JMSDestinationResolver;
import org.apache.cxf.transport.jms.util.JndiHelper;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class JMSOldConfigHolder {
    private static final Logger LOG = LogUtils.getL7dLogger(JMSOldConfigHolder.class);
    private ClientConfig clientConfig;
    private ClientBehaviorPolicyType runtimePolicy;

    private SessionPoolType sessionPool;
    private JMSConfiguration jmsConfig;
    private ServerConfig serverConfig;
    private ServerBehaviorPolicyType serverBehavior;
    private AddressType address;

    public void setAddress(AddressType ad) {
        address = ad;
    }
    public AddressType getAddress() {
        return address;
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
    
    public void initFromExtensorsAndSpring(Bus bus, 
                                           EndpointInfo endpointInfo, 
                                           EndpointReferenceType target,
                                           boolean isConduit) {
        address = endpointInfo.getTraversedExtensor(address, AddressType.class);
        clientConfig = endpointInfo.getTraversedExtensor(new ClientConfig(), ClientConfig.class);
        runtimePolicy = endpointInfo.getTraversedExtensor(new ClientBehaviorPolicyType(),
                                                          ClientBehaviorPolicyType.class);
        serverConfig = endpointInfo.getTraversedExtensor(new ServerConfig(), ServerConfig.class);
        sessionPool = endpointInfo.getTraversedExtensor(new SessionPoolType(),
                                                        SessionPoolType.class);
        serverBehavior = endpointInfo.getTraversedExtensor(new ServerBehaviorPolicyType(),
                                                           ServerBehaviorPolicyType.class);
        String name = endpointInfo.getName() == null ? null : endpointInfo.getName().toString()
                      + (isConduit ? ".jms-conduit" : ".jms-destination");

        // Try to retrieve configuration information from the spring
        // config. Search for a conduit or destination with name=endpoint name + ".jms-conduit"
        // or ".jms-destination"

        // Add conduit or destination config from spring
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            if (name != null) {
                configurer.configureBean(name, this);
            }
            String adr = target == null ? endpointInfo.getAddress() : target.getAddress().getValue();
            if (adr != null) {
                configurer.configureBean(adr, this);
            }
        }

    }
    
    /**
     * @param bus
     * @param endpointInfo
     * @param isConduit
     * @return
     */
    public JMSConfiguration createJMSConfigurationFromEndpointInfo(Bus bus,
                                                                   EndpointInfo endpointInfo,
                                                                   EndpointReferenceType target,
                                                                   boolean isConduit) 
        throws IOException {
        JMSEndpoint endpoint = new JMSEndpoint(endpointInfo, target);
        initFromExtensorsAndSpring(bus, endpointInfo, target, isConduit);
        ConfiguredBeanLocator locator = bus.getExtension(ConfiguredBeanLocator.class);
        if (address != null) {
            mapAddressToEndpoint(address, endpoint);
        }
        if (jmsConfig == null) {
            jmsConfig = new JMSConfiguration();
        }
        
        int deliveryMode = endpoint.getDeliveryMode() 
            == org.apache.cxf.transport.jms.uri.JMSEndpoint.DeliveryModeType.PERSISTENT
            ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
        jmsConfig.setDeliveryMode(deliveryMode);
        
        jmsConfig.setPriority(endpoint.getPriority());
        
        if (jmsConfig.isUsingEndpointInfo()) {
            jmsConfig.setReconnectOnException(endpoint.isReconnectOnException());
            jmsConfig.setDurableSubscriptionName(serverBehavior.getDurableSubscriberName());
            jmsConfig.setExplicitQosEnabled(true);
            if (jmsConfig.getMessageSelector() == null) {
                jmsConfig.setMessageSelector(serverBehavior.getMessageSelector());
            }
            if (isConduit) {
                if (runtimePolicy.isSetMessageType()) {
                    jmsConfig.setMessageType(runtimePolicy.getMessageType().value());
                } else if (address == null) {
                    if (endpoint.getMessageType() == null) {
                        // Using the byte message type by default
                        jmsConfig.setMessageType(JMSConstants.BYTE_MESSAGE_TYPE);
                    } else {
                        jmsConfig.setMessageType(endpoint.getMessageType().value());
                    }
                }
            }
            boolean pubSubDomain = endpoint.getJmsVariant().contains(JMSEndpoint.TOPIC);
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
            if (serverConfig.isSetServerReceiveTimeout()) {
                jmsConfig.setServerReceiveTimeout(serverConfig.getServerReceiveTimeout());
            }
            jmsConfig.setSubscriptionDurable(serverBehavior.isSetDurableSubscriberName());
            jmsConfig.setDurableSubscriptionName(serverBehavior.getDurableSubscriberName());
            jmsConfig.setDurableSubscriptionClientId(serverConfig.getDurableSubscriptionClientId());
            jmsConfig.setTimeToLive(endpoint.getTimeToLive());
            if (serverBehavior.isSetTransactional()) {
                jmsConfig.setSessionTransacted(serverBehavior.isTransactional());
            }
            
            if (endpoint.getJndiURL() != null) {
                // Configure Connection Factory using jndi
                JndiHelper jt = new JndiHelper(JMSOldConfigHolder.getInitialContextEnv(endpoint));
                jmsConfig.setJndiTemplate(jt);
                JNDIConfiguration jndiConfig = new JNDIConfiguration();
                jndiConfig.setJndiConnectionFactoryName(endpoint.getJndiConnectionFactoryName());
                jndiConfig.setConnectionUserName(endpoint.getUsername());
                jndiConfig.setConnectionPassword(endpoint.getPassword());
                jmsConfig.setJndiConfig(jndiConfig);
            } else {
                // Configure ConnectionFactory using locator 
                if (locator != null) {
                    // Lookup connectionFactory in context like blueprint
                    ConnectionFactory cf = locator.getBeanOfType(endpoint.getJndiConnectionFactoryName(), 
                                                                 ConnectionFactory.class);
                    if (cf != null) {
                        jmsConfig.setConnectionFactory(cf);
                    }
                }
            }
            
            boolean resolveUsingJndi = endpoint.getJmsVariant().contains(JMSEndpoint.JNDI);
            if (resolveUsingJndi) {
                // Setup Destination jndi destination resolver
                JndiHelper jt = new JndiHelper(JMSOldConfigHolder.getInitialContextEnv(endpoint));
                final JMSDestinationResolver jndiDestinationResolver = new JMSDestinationResolver();
                jndiDestinationResolver.setJndiTemplate(jt);
                jmsConfig.setDestinationResolver(jndiDestinationResolver);
                jmsConfig.setTargetDestination(endpoint.getDestinationName());
                setReplyDestination(jmsConfig, endpoint);
                if (address != null) {
                    jmsConfig.setReplyToDestination(address.getJndiReplyToDestinationName());
                }
            } else {
                // Use the default dynamic destination resolver
                jmsConfig.setTargetDestination(endpoint.getDestinationName());
                setReplyDestination(jmsConfig, endpoint);
                if (address != null) {
                    jmsConfig.setReplyToDestination(address.getJmsReplyToDestinationName());
                }
            }
        }
        
        String requestURI = endpoint.getRequestURI();
        jmsConfig.setRequestURI(requestURI);
        
        String targetService = endpoint.getParameter(JMSSpecConstants.TARGETSERVICE_PARAMETER_NAME);
        jmsConfig.setTargetService(targetService);
        return jmsConfig;
    }

    private static void setReplyDestination(JMSConfiguration jmsConfig, JMSEndpoint endpoint) {
        if (endpoint.getReplyToName() != null)  {
            jmsConfig.setReplyDestination(endpoint.getReplyToName());
            jmsConfig.setReplyPubSubDomain(false);
        } else if (endpoint.getTopicReplyToName() != null) {
            jmsConfig.setReplyDestination(endpoint.getTopicReplyToName());
            jmsConfig.setReplyPubSubDomain(true);
        }
    }

    private static void mapAddressToEndpoint(AddressType address, JMSEndpoint endpoint) {
        boolean pubSubDomain = DestinationStyleType.TOPIC == address.getDestinationStyle();
        if (address.isSetDestinationStyle()) {
            endpoint.setJmsVariant(pubSubDomain ? JMSEndpoint.TOPIC : JMSEndpoint.QUEUE);
        } else {
            endpoint.setJmsVariant(JMSEndpoint.QUEUE);
        }
        if (address.isSetJndiConnectionFactoryName()) {
            endpoint.setJndiConnectionFactoryName(address.getJndiConnectionFactoryName());
        }
        if (address.isSetConnectionUserName()) {
            endpoint.setUsername(address.getConnectionUserName());
        }
        if (address.isSetConnectionPassword()) {
            endpoint.setPassword(address.getConnectionPassword());
        }
        if (address.isSetReconnectOnException()) {
            endpoint.setReconnectOnException(address.isReconnectOnException());
        }
        if (address.isSetUseJms11()) {
            LOG.log(Level.WARNING, "Use of address[@useJms11] is no longer supported");
        }
        boolean useJndi = address.isSetJndiDestinationName();
        if (useJndi) {
            endpoint.setJmsVariant(pubSubDomain ? JMSEndpoint.JNDI_TOPIC : JMSEndpoint.JNDI);
            endpoint.setDestinationName(address.getJndiDestinationName());
            endpoint.setReplyToName(address.getJndiReplyDestinationName());
        } else {
            endpoint.setDestinationName(address.getJmsDestinationName());
            endpoint.setReplyToName(address.getJmsReplyDestinationName());
        }
        
        java.util.ListIterator<JMSNamingPropertyType> listIter 
            = address.getJMSNamingProperty().listIterator();
        while (listIter.hasNext()) {
            JMSNamingPropertyType propertyPair = listIter.next();
            String name = propertyPair.getName();
            String value = propertyPair.getValue();
            if (value != null) {
                if (name.equals(Context.PROVIDER_URL)) {
                    endpoint.setJndiURL(value);
                } else if (name.equals(Context.INITIAL_CONTEXT_FACTORY)) {
                    endpoint.setJndiInitialContextFactory(value);
                } else {
                    endpoint.putJndiParameter(propertyPair.getName(), propertyPair.getValue());
                }
            }
        }
    }

    public <T> T getWSDLExtensor(EndpointInfo ei, Class<T> cls) {
        ServiceInfo si = ei.getService();
        BindingInfo bi = ei.getBinding();
        
        Object o = ei.getExtensor(cls);
        if (o == null && si != null) {
            o = si.getExtensor(cls);
        }
        if (o == null && bi != null) {
            o = bi.getExtensor(cls);
        }
        
        if (o == null) {
            return null;
        }
        if (cls.isInstance(o)) {
            return cls.cast(o);
        }
        return null;
    }
    
    public static Properties getInitialContextEnv(JMSEndpoint endpoint) {
        Properties env = new Properties();
        if (endpoint.getJndiInitialContextFactory() != null) {
            env.put(Context.INITIAL_CONTEXT_FACTORY, endpoint.getJndiInitialContextFactory());
        }
        if (endpoint.getJndiURL() != null) {
            env.put(Context.PROVIDER_URL, endpoint.getJndiURL());
        }
        for (Map.Entry<String, String> ent : endpoint.getJndiParameters().entrySet()) {
            env.put(ent.getKey(), ent.getValue());
        }
        if (LOG.isLoggable(Level.FINE)) {
            Enumeration<?> props = env.propertyNames();
            while (props.hasMoreElements()) {
                String name = (String)props.nextElement();
                String value = env.getProperty(name);
                LOG.log(Level.FINE, "Context property: " + name + " | " + value);
            }
        }
        return env;
    }
}
