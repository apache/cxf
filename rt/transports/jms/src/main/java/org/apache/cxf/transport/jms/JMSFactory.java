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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.naming.NamingException;

import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.jms.util.JMSListenerContainer;
import org.apache.cxf.transport.jms.util.JMSSender;
import org.apache.cxf.transport.jms.util.JMSUtil;
import org.apache.cxf.transport.jms.util.JndiHelper;
import org.apache.cxf.transport.jms.util.MessageListenerContainer;
import org.apache.cxf.transport.jms.util.ResourceCloser;

/**
 * Factory to create jms helper objects from configuration and context information
 */
public final class JMSFactory {
    static final String MESSAGE_ENDPOINT_FACTORY = "MessageEndpointFactory";
    static final String MDB_TRANSACTED_METHOD = "MDBTransactedMethod";

    //private static final Logger LOG = LogUtils.getL7dLogger(JMSFactory.class);
    
    private JMSFactory() {
    }

    /**
     * Retrieve connection factory from JNDI
     * 
     * @param jmsConfig
     * @param jndiConfig
     * @return
     */
    static ConnectionFactory getConnectionFactoryFromJndi(JMSConfiguration jmsConfig) {
        if (jmsConfig.getJndiEnvironment() == null || jmsConfig.getConnectionFactoryName() == null) {
            return null;
        }
        try {
            ConnectionFactory cf = new JndiHelper(jmsConfig.getJndiEnvironment()).
                lookup(jmsConfig.getConnectionFactoryName(), ConnectionFactory.class);
            return cf;
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Create JmsSender from configuration information. Most settings are taken from jmsConfig. The QoS
     * settings in messageProperties override the settings from jmsConfig
     * 
     * @param jmsConfig configuration information
     * @param messageProperties context headers override config settings
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

    private static String getMessageSelector(JMSConfiguration jmsConfig, String conduitId) {
        String staticSelectorPrefix = jmsConfig.getConduitSelectorPrefix();
        String conduitIdSt = jmsConfig.isUseConduitIdSelector() && conduitId != null ? conduitId : "";
        String correlationIdPrefix = staticSelectorPrefix + conduitIdSt;
        return correlationIdPrefix.isEmpty() ? null : "JMSCorrelationID LIKE '" + correlationIdPrefix + "%'";
    }
    
    public static JMSListenerContainer createTargetDestinationListener(EndpointInfo ei, 
                                                                       JMSConfiguration jmsConfig,
                                                                       MessageListener listenerHandler) {
        Session session = null;
        try {
            Connection connection = createConnection(jmsConfig);
            connection.start();
            session = connection.createSession(jmsConfig.isSessionTransacted(), Session.AUTO_ACKNOWLEDGE);
            Destination destination = jmsConfig.getTargetDestination(session);
            MessageListenerContainer container = new MessageListenerContainer(connection, destination, listenerHandler);
            container.setMessageSelector(jmsConfig.getMessageSelector());
            container.start();
            return container;
        } catch (JMSException e) {
            throw JMSUtil.convertJmsException(e);
        } finally {
            ResourceCloser.close(session);
        }
    }

    public static JMSListenerContainer createListenerContainer(JMSConfiguration jmsConfig,
                                                               Connection connection,
                                                               MessageListener listenerHandler, 
                                                               Destination destination,
                                                               String conduitId) {
        MessageListenerContainer container = new MessageListenerContainer(connection, destination, listenerHandler);
        String messageSelector = getMessageSelector(jmsConfig, conduitId);
        container.setMessageSelector(messageSelector);
        container.start();
        return container;
    }

    public static Connection createConnection(JMSConfiguration jmsConfig) throws JMSException {
        Connection connection = jmsConfig.getConnectionFactory().createConnection(jmsConfig.getUserName(),
                                                                                  jmsConfig.getPassword());
        if (jmsConfig.getDurableSubscriptionClientId() != null) {
            connection.setClientID(jmsConfig.getDurableSubscriptionClientId());
        }
        return connection;
    }
    
}
