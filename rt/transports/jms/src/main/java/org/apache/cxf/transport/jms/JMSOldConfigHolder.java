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
import javax.naming.NamingException;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.service.model.EndpointInfo;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiTemplate;

public class JMSOldConfigHolder {
    protected ClientConfig clientConfig;
    protected ClientBehaviorPolicyType runtimePolicy;
    protected AddressType address;
    protected SessionPoolType sessionPool;

    private ConnectionFactory getConnectionFactoryFromJndi(String connectionFactoryName, String userName,
                                                           String password, JndiTemplate jt) {
        if (connectionFactoryName == null) {
            return null;
        }
        try {

            ConnectionFactory connectionFactory = (ConnectionFactory)jt.lookup(connectionFactoryName);
            UserCredentialsConnectionFactoryAdapter uccf = new UserCredentialsConnectionFactoryAdapter();
            uccf.setUsername(userName);
            uccf.setPassword(password);
            uccf.setTargetConnectionFactory(connectionFactory);

            SingleConnectionFactory scf = new SingleConnectionFactory();
            scf.setTargetConnectionFactory(uccf);
            return scf;
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public JMSConfiguration createJMSConfigurationFromEndpointInfo(Bus bus, EndpointInfo endpointInfo) {
        JMSConfiguration jmsConf = new JMSConfiguration();

        // Retrieve configuration information that was extracted from the wsdl
        address = endpointInfo.getTraversedExtensor(new AddressType(), AddressType.class);
        clientConfig = endpointInfo.getTraversedExtensor(new ClientConfig(), ClientConfig.class);
        runtimePolicy = endpointInfo.getTraversedExtensor(new ClientBehaviorPolicyType(),
                                                          ClientBehaviorPolicyType.class);

        // Try to retrieve configuration information from the spring
        // config. Search for a tag <jms:conduit> with name=endpoint name + ".jms-conduit"
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            String name = endpointInfo.getName().toString() + ".jms-conduit";
            configurer.configureBean(name, this);
        }

        JndiTemplate jt = new JndiTemplate();
        jt.setEnvironment(JMSUtils.getInitialContextEnv(address));
        ConnectionFactory cf = getConnectionFactoryFromJndi(address.getJndiConnectionFactoryName(), address
            .getConnectionUserName(), address.getConnectionPassword(), jt);

        // TODO Use JmsTemplate102 in case JMS 1.1 is not available
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(cf);
        boolean pubSubDomain = false;
        if (address.isSetDestinationStyle()) {
            pubSubDomain = DestinationStyleType.TOPIC == address.getDestinationStyle();
        }
        jmsTemplate.setPubSubDomain(pubSubDomain);
        jmsTemplate.setReceiveTimeout(clientConfig.getClientReceiveTimeout());
        jmsTemplate.setTimeToLive(clientConfig.getMessageTimeToLive());
        jmsTemplate.setPriority(Message.DEFAULT_PRIORITY);
        jmsTemplate.setDeliveryMode(Message.DEFAULT_DELIVERY_MODE);
        jmsTemplate.setExplicitQosEnabled(true);

        if (address.isSetJndiDestinationName()) {
            // Setup Destination jndi destination resolver
            final JndiDestinationResolver jndiDestinationResolver = new JndiDestinationResolver();
            jndiDestinationResolver.setJndiTemplate(jt);
            jmsTemplate.setDestinationResolver(jndiDestinationResolver);
            jmsConf.setTargetDestination(address.getJndiDestinationName());
            jmsConf.setReplyDestination(address.getJndiReplyDestinationName());
        } else {
            // Use the default dynamic destination resolver
            jmsConf.setTargetDestination(address.getJmsDestinationName());
            jmsConf.setReplyDestination(address.getJmsReplyDestinationName());
        }
        if (runtimePolicy.isSetMessageType()) {
            jmsConf.setMessageType(runtimePolicy.getMessageType().value());
        }

        jmsConf.setJmsTemplate(jmsTemplate);
        return jmsConf;
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
}
