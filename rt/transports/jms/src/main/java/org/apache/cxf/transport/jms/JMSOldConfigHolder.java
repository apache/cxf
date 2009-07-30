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
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.DeliveryMode;
import javax.naming.Context;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
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
                                                                   boolean isConduit) 
        throws IOException {
        String transportId = endpointInfo.getTransportId();
        if (transportId.equals(JMSSpecConstants.SOAP_JMS_SPECIFICIATION_TRANSPORTID)) {
            return createJMSConfigurationFromEndpointInfoForSpecification(bus, endpointInfo,
                                                                          isConduit);
        } else {
            return createJMSConfigurationFromEndpointInfoForOldJMS(bus, endpointInfo, isConduit);
        }

    }
    
    public JMSConfiguration createJMSConfigurationFromEndpointInfoForOldJMS(Bus bus,
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

    /**
     * @param bus
     * @param endpointInfo
     * @param isConduit
     * @return
     */
    private JMSConfiguration createJMSConfigurationFromEndpointInfoForSpecification(
                                                                                    Bus bus,
                                                                                    EndpointInfo endpointInfo,
                                                                                    boolean isConduit) 
        throws IOException {
        JMSEndpoint endpoint = null;
        try {           
            endpoint = JMSEndpointParser.createEndpoint(endpointInfo.getAddress());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception e) {
            IOException e2 = new IOException(e.getMessage());
            e2.initCause(e);
            throw e2;
        }
        retrieveWSDLInformation(endpoint, endpointInfo);
        //address = endpointInfo.getTraversedExtensor(new AddressType(), AddressType.class); 
        clientConfig = endpointInfo.getTraversedExtensor(new ClientConfig(), ClientConfig.class);
        runtimePolicy = endpointInfo.getTraversedExtensor(new ClientBehaviorPolicyType(),
                                                          ClientBehaviorPolicyType.class);
        serverConfig = endpointInfo.getTraversedExtensor(new ServerConfig(), ServerConfig.class);
        sessionPool = endpointInfo.getTraversedExtensor(new SessionPoolType(),
                                                        SessionPoolType.class);
        serverBehavior = endpointInfo.getTraversedExtensor(new ServerBehaviorPolicyType(),
                                                           ServerBehaviorPolicyType.class);
        String name = endpointInfo.getName().toString()
                      + (isConduit ? ".jms-conduit" : ".jms-destination");

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
            boolean pubSubDomain = false;
            pubSubDomain = endpoint.getJmsVariant().equals(JMSURIConstants.TOPIC);
            JNDIConfiguration jndiConfig = new JNDIConfiguration();
            jndiConfig.setJndiConnectionFactoryName(endpoint.getJndiConnectionFactoryName());
            jmsConfig.setJndiTemplate(jt);
            // TODO need to check the userName and Password setting the specification
            /*
             * jndiConfig.setConnectionUserName(address.getConnectionUserName());
             * jndiConfig.setConnectionPassword(address.getConnectionPassword());
             */
            jmsConfig.setJndiConfig(jndiConfig);
            /*
             * if (address.isSetReconnectOnException()) {
             * jmsConfig.setReconnectOnException(address.isReconnectOnException()); }
             */
            jmsConfig.setDurableSubscriptionName(serverBehavior.getDurableSubscriberName());
            jmsConfig.setExplicitQosEnabled(true);
            if (jmsConfig.getMessageSelector() == null) {
                jmsConfig.setMessageSelector(serverBehavior.getMessageSelector());
            }
            if (isConduit) {
                if (runtimePolicy.isSetMessageType()) {
                    jmsConfig.setMessageType(runtimePolicy.getMessageType().value());
                } else {
                    jmsConfig.setMessageType(JMSConstants.BYTE_MESSAGE_TYPE);
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
            jmsConfig.setSubscriptionDurable(serverBehavior.isSetDurableSubscriberName());
            jmsConfig.setDurableSubscriptionName(serverBehavior.getDurableSubscriberName());
            jmsConfig.setDurableSubscriptionClientId(serverConfig.getDurableSubscriptionClientId());
            if (sessionPool.isSetHighWaterMark()) {
                jmsConfig.setMaxConcurrentTasks(sessionPool.getHighWaterMark());
            }
            if (endpoint.isSetTimeToLive()) {
                long timeToLive = endpoint.getTimeToLive();
                jmsConfig.setTimeToLive(timeToLive);
            }
            /*
             * if (address.isSetUseJms11()) { jmsConfig.setUseJms11(address.isUseJms11()); }
             */
            if (serverBehavior.isSetTransactional()) {
                jmsConfig.setSessionTransacted(serverBehavior.isTransactional());
            }
            boolean useJndi = endpoint.getJmsVariant().equals(JMSURIConstants.JNDI);
            if (useJndi) {
                // Setup Destination jndi destination resolver
                final JndiDestinationResolver jndiDestinationResolver = new JndiDestinationResolver();
                jndiDestinationResolver.setJndiTemplate(jt);
                jmsConfig.setDestinationResolver(jndiDestinationResolver);
                jmsConfig.setTargetDestination(endpoint.getDestinationName());
                jmsConfig.setReplyDestination(endpoint.getReplyToName());
            } else {
                // Use the default dynamic destination resolver
                jmsConfig.setTargetDestination(endpoint.getDestinationName());
                jmsConfig.setReplyDestination(endpoint.getReplyToName());
            }
        }
        
        String requestURI = endpoint.getRequestURI();
        jmsConfig.setRequestURI(requestURI);

        String targetService = endpoint.getParameter(JMSSpecConstants.TARGETSERVICE_PARAMETER_NAME);
        jmsConfig.setTargetService(targetService);
        return jmsConfig;
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
    }

    public <T> T getWSDLExtensor(EndpointInfo ei, Class<T> cls) {
        ServiceInfo si = ei.getService();
        BindingInfo bi = ei.getBinding();
        
        Object o = ei.getExtensor(cls);
        if (o == null) {
            o = si.getExtensor(cls);
        }
        if (o == null) {
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
        env.put(Context.INITIAL_CONTEXT_FACTORY, endpoint.getJndiInitialContextFactory());
        env.put(Context.PROVIDER_URL, endpoint.getJndiURL());
        Map addParas = endpoint.getJndiParameters();
        Iterator keyIter = addParas.keySet().iterator();
        while (keyIter.hasNext()) {
            String key = (String)keyIter.next();
            env.put(key, addParas.get(key));
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
