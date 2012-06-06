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

import javax.jms.DeliveryMode;
import javax.naming.Context;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;
import org.apache.cxf.transport.jms.uri.JMSEndpoint;
import org.apache.cxf.transport.jms.uri.JMSEndpointParser;
import org.apache.cxf.transport.jms.uri.JMSURIConstants;
import org.apache.cxf.transport.jms.wsdl.DeliveryModeType;
import org.apache.cxf.transport.jms.wsdl.JndiConnectionFactoryNameType;
import org.apache.cxf.transport.jms.wsdl.JndiContextParameterType;
import org.apache.cxf.transport.jms.wsdl.JndiInitialContextFactoryType;
import org.apache.cxf.transport.jms.wsdl.JndiURLType;
import org.apache.cxf.transport.jms.wsdl.PriorityType;
import org.apache.cxf.transport.jms.wsdl.ReplyToNameType;
import org.apache.cxf.transport.jms.wsdl.TimeToLiveType;
import org.apache.cxf.transport.jms.wsdl.TopicReplyToNameType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiTemplate;

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

    
    /**
     * Get the extensors from the wsdl and/or configuration that will
     * then be used to configure the JMSConfiguration object 
     * @param target 
     */
    protected JMSEndpoint getExtensorsAndConfig(Bus bus,
                           EndpointInfo endpointInfo,
                           EndpointReferenceType target,
                           boolean isConduit) throws IOException {
        JMSEndpoint endpoint = null;
        String adr = target == null ? endpointInfo.getAddress() : target.getAddress().getValue();
        try {           
            endpoint = StringUtils.isEmpty(adr) || "jms://".equals(adr) || !adr.startsWith("jms") 
                ?  new JMSEndpoint()
                : JMSEndpointParser.createEndpoint(adr);                
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception e) {
            IOException e2 = new IOException(e.getMessage());
            e2.initCause(e);
            throw e2;
        }
        retrieveWSDLInformation(endpoint, endpointInfo);

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

        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            if (name != null) {
                configurer.configureBean(name, this);
            }
            if (adr != null) {
                configurer.configureBean(adr, this);
            }
        }
        return endpoint;
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
        JMSEndpoint endpoint = getExtensorsAndConfig(bus, endpointInfo, target, isConduit);

        return configureEndpoint(isConduit, endpoint);
    }

    protected JMSConfiguration configureEndpoint(boolean isConduit, JMSEndpoint endpoint) {
        if (address != null) {
            mapAddressToEndpoint(address, endpoint);
        }
        if (jmsConfig == null) {
            jmsConfig = new JMSConfiguration();
        }

        if (endpoint.isSetDeliveryMode()) {
            int deliveryMode = endpoint.getDeliveryMode()
                .equals(JMSURIConstants.DELIVERYMODE_PERSISTENT)
                ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
            jmsConfig.setDeliveryMode(deliveryMode);
        }

        if (endpoint.isSetPriority()) {
            int priority = endpoint.getPriority();
            jmsConfig.setPriority(priority);
        }

        if (jmsConfig.isUsingEndpointInfo()) {
            JndiTemplate jt = new JndiTemplate();
            jt.setEnvironment(JMSOldConfigHolder.getInitialContextEnv(endpoint));
            boolean pubSubDomain = endpoint.getJmsVariant().contains(JMSURIConstants.TOPIC);
            JNDIConfiguration jndiConfig = new JNDIConfiguration();
            jndiConfig.setJndiConnectionFactoryName(endpoint.getJndiConnectionFactoryName());
            jmsConfig.setJndiTemplate(jt);

            jndiConfig.setConnectionUserName(endpoint.getUsername());
            jndiConfig.setConnectionPassword(endpoint.getPassword());

            jmsConfig.setJndiConfig(jndiConfig);
            if (endpoint.isSetReconnectOnException()) {
                jmsConfig.setReconnectOnException(endpoint.isReconnectOnException());
            }
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
            jmsConfig.setEnforceSpec(clientConfig.isEnforceSpec());
            jmsConfig.setSubscriptionDurable(serverBehavior.isSetDurableSubscriberName());
            jmsConfig.setDurableSubscriptionName(serverBehavior.getDurableSubscriberName());
            jmsConfig.setDurableSubscriptionClientId(serverConfig.getDurableSubscriptionClientId());
            if (sessionPool.isSetHighWaterMark()) {
                jmsConfig.setMaxConcurrentConsumers(sessionPool.getHighWaterMark());
            }
            if (sessionPool.isSetLowWaterMark()) {
                jmsConfig.setConcurrentConsumers(sessionPool.getLowWaterMark());
            }
            if (endpoint.isSetTimeToLive()) {
                long timeToLive = endpoint.getTimeToLive();
                jmsConfig.setTimeToLive(timeToLive);
            }
            if (endpoint.isSetUseJMS11()) {
                jmsConfig.setUseJms11(endpoint.isUseJMS11());
            }
            if (serverBehavior.isSetTransactional()) {
                jmsConfig.setSessionTransacted(serverBehavior.isTransactional());
            }
            boolean useJndi = endpoint.getJmsVariant().contains(JMSURIConstants.JNDI);
            if (useJndi) {
                // Setup Destination jndi destination resolver
                final JndiDestinationResolver jndiDestinationResolver = new JndiDestinationResolver();
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
            endpoint.setJmsVariant(pubSubDomain ? JMSURIConstants.TOPIC : JMSURIConstants.QUEUE);
        } else {
            endpoint.setJmsVariant(JMSURIConstants.QUEUE);
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
            endpoint.setUseJMS11(address.isUseJms11());
        }
        boolean useJndi = address.isSetJndiDestinationName();
        if (useJndi) {
            endpoint.setJmsVariant(pubSubDomain ? JMSURIConstants.JNDI_TOPIC : JMSURIConstants.JNDI);
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
            if (null != propertyPair.getValue()) {
                endpoint.putJndiParameter(propertyPair.getName(), propertyPair.getValue());
            }
        }
    }

    /**
     * @param endpoint
     * @param ei
     */
    private void retrieveWSDLInformation(JMSEndpoint endpoint, EndpointInfo ei) {
        JndiContextParameterType jndiContextParameterType = 
            getWSDLExtensor(ei, JndiContextParameterType.class);
        if (jndiContextParameterType != null 
            && endpoint.getJndiParameters().get(jndiContextParameterType.getName()) == null) {
            endpoint.putJndiParameter(jndiContextParameterType.getName().trim(),
                                              jndiContextParameterType.getValue().trim());
        }
        
        if (!endpoint.isSetJndiConnectionFactoryName()) {
            JndiConnectionFactoryNameType jndiConnectionFactoryNameType = getWSDLExtensor(
                ei, JndiConnectionFactoryNameType.class);
            if (jndiConnectionFactoryNameType != null) {
                endpoint.setJndiConnectionFactoryName(jndiConnectionFactoryNameType.getValue().trim());
            }
        }
        if (!endpoint.isSetJndiInitialContextFactory()) {
            JndiInitialContextFactoryType jndiInitialContextFactoryType = 
                getWSDLExtensor(ei, JndiInitialContextFactoryType.class);
            if (jndiInitialContextFactoryType != null) {
                endpoint.setJndiInitialContextFactory(jndiInitialContextFactoryType.getValue().trim()); 
            }
        }
        
        if (!endpoint.isSetJndiURL()) {
            JndiURLType jndiURLType = getWSDLExtensor(ei, JndiURLType.class);
            if (jndiURLType != null) {
                endpoint.setJndiURL(jndiURLType.getValue().trim());
            }
        }
        
        if (!endpoint.isSetDeliveryMode()) {
            DeliveryModeType deliveryModeType = getWSDLExtensor(ei, DeliveryModeType.class);
            if (deliveryModeType != null) {
                String deliveryMode = deliveryModeType.getValue().trim();
                endpoint.setDeliveryMode(org.apache.cxf.transport.jms.uri.DeliveryModeType
                    .valueOf(deliveryMode));
            }
        }
        
        if (!endpoint.isSetPriority()) {
            PriorityType priorityType = getWSDLExtensor(ei, PriorityType.class);
            if (priorityType != null) {
                endpoint.setPriority(priorityType.getValue());
            }
        }
        
        if (!endpoint.isSetTimeToLive()) {
            TimeToLiveType timeToLiveType = getWSDLExtensor(ei, TimeToLiveType.class);
            if (timeToLiveType != null) {
                endpoint.setTimeToLive(timeToLiveType.getValue()); 
            }
        }
        
        if (!endpoint.isSetReplyToName()) {
            ReplyToNameType replyToNameType = getWSDLExtensor(ei, ReplyToNameType.class);
            if (replyToNameType != null) {
                endpoint.setReplyToName(replyToNameType.getValue());
            }
        }
        
        if (!endpoint.isSetTopicReplyToName()) {
            TopicReplyToNameType topicReplyToNameType = getWSDLExtensor(ei, TopicReplyToNameType.class);
            if (topicReplyToNameType != null) {
                endpoint.setTopicReplyToName(topicReplyToNameType.getValue());
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
        if (endpoint.isSetJndiInitialContextFactory()) {
            env.put(Context.INITIAL_CONTEXT_FACTORY, endpoint.getJndiInitialContextFactory());
        }
        if (endpoint.isSetJndiURL()) {
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
