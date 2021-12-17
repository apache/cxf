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

package org.apache.cxf.transport.jms.uri;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * Parses and holds configuration retrieved from a SOAP/JMS spec URI
 */
public class JMSEndpoint {
    // JMS Variants
    public static final String JNDI = "jndi";
    public static final String TOPIC = "topic";
    public static final String QUEUE = "queue";
    public static final String JNDI_TOPIC = "jndi-topic";

    // default values
    public static final DeliveryModeType DELIVERYMODE_DEFAULT = DeliveryModeType.PERSISTENT;
    public static final long TIMETOLIVE_DEFAULT = Message.DEFAULT_TIME_TO_LIVE;
    public static final int PRIORITY_DEFAULT = Message.DEFAULT_PRIORITY;

    /**
     * All parameters with this prefix will go to jndiParameters and be used
     * as the jndi inital context properties
     */
    public static final String JNDI_PARAMETER_NAME_PREFIX = "jndi-";

    public static final String JAXWS_PROPERTY_PREFIX = "jms.";

    private Map<String, String> jndiParameters = new HashMap<>();
    private Map<String, String> parameters = new HashMap<>();

    private String endpointUri;
    private ConnectionFactory connectionFactory;
    private String jmsVariant;
    private String destinationName;

    /**
     * URI parameters
     * Will be filled from URI query parameters with matching names
     */
    private String conduitIdSelectorPrefix;
    private DeliveryModeType deliveryMode;
    private String durableSubscriptionClientId;
    private String durableSubscriptionName;
    private String jndiConnectionFactoryName = "ConnectionFactory";
    private String jndiInitialContextFactory;
    private String jndiTransactionManagerName;
    private String jndiURL;
    private MessageType messageType;
    private String password;
    private Integer priority;
    private long receiveTimeout = 60000L;
    private String replyToName;
    private boolean sessionTransacted;
    private String targetService;
    private long timeToLive;
    private String topicReplyToName;
    private boolean useConduitIdSelector = true;
    private String username;
    private int concurrentConsumers = 1;
    private String messageSelector;
    private int retryInterval = 5000;
    private boolean oneSessionPerConnection;
    private boolean ignoreTimeoutException;

    /**
     * @param endpointUri
     */
    public JMSEndpoint(String endpointUri) {
        this(null, endpointUri);
    }

    /**
     * Get the extensors from the wsdl and/or configuration that will
     * then be used to configure the JMSConfiguration object
     * @param endpointInfo
     * @param target
     */
    public JMSEndpoint(EndpointInfo endpointInfo, EndpointReferenceType target) {
        this(endpointInfo,  target == null ? endpointInfo.getAddress() : target.getAddress().getValue());
    }

    /**
     * @param endpointInfo
     * @param endpointUri
     */
    public JMSEndpoint(EndpointInfo endpointInfo, String endpointUri) {
        this.jmsVariant = JMSEndpoint.QUEUE;

        if (endpointInfo != null) {
            JMSEndpointWSDLUtil.retrieveWSDLInformation(this, endpointInfo);
        }
        if (!(StringUtils.isEmpty(endpointUri) || "jms://".equals(endpointUri) || !endpointUri.startsWith("jms"))) {
            this.endpointUri = endpointUri;
            JMSURIParser parsed = new JMSURIParser(endpointUri);
            setJmsVariant(parsed.getVariant());
            this.destinationName = parsed.getDestination();
            Map<String, Object> query = parsed.parseQuery();
            configureProperties(query);

            // Use the properties like e.g. from JAXWS properties with "jms." prefix
            Map<String, Object> jmsProps = new HashMap<>();
            if (endpointInfo != null) {
                getJaxWsJmsProps(endpointInfo.getProperties(), jmsProps);
            }
            if (endpointInfo != null && endpointInfo.getBinding() != null) {
                getJaxWsJmsProps(endpointInfo.getBinding().getProperties(), jmsProps);
            }
            configureProperties(jmsProps);
        }
    }

    private void getJaxWsJmsProps(Map<String, Object> jaxwsProps, Map<String, Object> jmsProps) {
        if (jaxwsProps == null) {
            return;
        }
        for (Entry<String, Object> entry : jaxwsProps.entrySet()) {
            if (entry.getKey().startsWith(JAXWS_PROPERTY_PREFIX)) {
                jmsProps.put(entry.getKey().substring(JAXWS_PROPERTY_PREFIX.length()), entry.getValue());
            }
        }
    }

    private boolean trySetProperty(String name, Object value) {
        try {
            Method method = this.getClass().getMethod(getPropSetterName(name), value.getClass());
            method.invoke(this, value);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error setting property " + name + ":" + e.getMessage(), e);
        }
    }

    private static String getPropSetterName(String name) {
        return "set" + StringUtils.capitalize(name);
    }

    /**
     * Configure properties form map.
     * For each key of the map first a property with the same name in the endpoint is tried.
     * If that does not match then the value is either stored in the jndiParameters or the parameters
     * depending on the prefix of the key. If it matches JNDI_PARAMETER_NAME_PREFIX it is stored in the
     * jndiParameters else in the parameters
     *
     * @param params
     */
    private void configureProperties(Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value == null || value.equals("")) {
                continue;
            }
            String key = entry.getKey();
            if (trySetProperty(key, value)) {
                continue;
            }
            if (!(value instanceof String)) {
                continue;
            }
            String valueSt = (String)value;
            if (key.startsWith(JMSEndpoint.JNDI_PARAMETER_NAME_PREFIX)) {
                key = key.substring(5);
                putJndiParameter(key, valueSt);
            } else {
                putParameter(key, valueSt);
            }
        }

        if (replyToName != null && topicReplyToName != null) {
            throw new IllegalArgumentException(
                "The replyToName and topicReplyToName should not be defined at the same time.");
        }
    }

    public String getRequestURI() {
        StringBuilder requestUri = new StringBuilder("jms:");
        if (JNDI_TOPIC.equals(jmsVariant)) {
            requestUri.append("jndi");
        } else {
            requestUri.append(jmsVariant);
        }
        requestUri.append(':').append(destinationName);
        boolean first = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String value = entry.getValue();
            if (first) {
                requestUri.append('?').append(entry.getKey()).append('=').append(value);
                first = false;
            } else {
                requestUri.append('&').append(entry.getKey()).append('=').append(value);
            }
        }
        return requestUri.toString();
    }

    /**
     * @param key
     * @param value
     */
    public final void putJndiParameter(String key, String value) {
        jndiParameters.put(key, value);
    }

    public final void putParameter(String key, String value) {
        parameters.put(key, value);
    }

    /**
     * @param key
     * @return
     */
    public String getParameter(String key) {
        return parameters.get(key);
    }

    public Map<String, String> getJndiParameters() {
        return jndiParameters;
    }

    /**
     * @return
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getEndpointUri() {
        return endpointUri;
    }
    public void setEndpointUri(String endpointUri) {
        this.endpointUri = endpointUri;
    }
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public String getJmsVariant() {
        return jmsVariant;
    }
    public final void setJmsVariant(String jmsVariant) {
        if (jmsVariant == null) {
            this.jmsVariant = QUEUE;
        }
        if (!(QUEUE.equals(jmsVariant) || TOPIC.equals(jmsVariant)
            || JNDI.equals(jmsVariant) || JNDI_TOPIC.equals(jmsVariant))) {
            throw new IllegalArgumentException("Unknow JMS Variant " + jmsVariant);
        }
        this.jmsVariant = jmsVariant;
    }
    public String getDestinationName() {
        return destinationName;
    }
    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }
    public boolean isSetDeliveryMode() {
        return deliveryMode != null;
    }
    public DeliveryModeType getDeliveryMode() {
        return deliveryMode == null ? DeliveryModeType.PERSISTENT : deliveryMode;
    }
    public void setDeliveryMode(DeliveryModeType deliveryMode) {
        this.deliveryMode = deliveryMode;
    }
    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = DeliveryModeType.valueOf(deliveryMode);
    }
    public MessageType getMessageType() {
        return messageType == null ? MessageType.BYTE : messageType;
    }
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    public void setMessageType(String messageType) {
        this.messageType = MessageType.fromValue(messageType);
    }
    public long getTimeToLive() {
        return timeToLive;
    }
    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }
    public void setTimeToLive(String timeToLive) {
        this.timeToLive = Long.parseLong(timeToLive);
    }
    public boolean isSetPriority() {
        return priority != null;
    }
    public int getPriority() {
        return priority == null ?  Message.DEFAULT_PRIORITY : priority;
    }
    public void setPriority(int priority) {
        this.priority = priority;
    }
    public void setPriority(String priority) {
        this.priority = Integer.valueOf(priority);
    }
    public String getReplyToName() {
        return replyToName;
    }
    public void setReplyToName(String replyToName) {
        this.replyToName = replyToName;
    }
    public String getTopicReplyToName() {
        return topicReplyToName;
    }
    public void setTopicReplyToName(String topicReplyToName) {
        this.topicReplyToName = topicReplyToName;
    }
    public String getJndiConnectionFactoryName() {
        return jndiConnectionFactoryName;
    }
    public void setJndiConnectionFactoryName(String jndiConnectionFactoryName) {
        this.jndiConnectionFactoryName = jndiConnectionFactoryName;
    }
    public String getJndiInitialContextFactory() {
        return jndiInitialContextFactory;
    }
    public void setJndiInitialContextFactory(String jndiInitialContextFactory) {
        this.jndiInitialContextFactory = jndiInitialContextFactory;
    }
    public String getJndiURL() {
        return jndiURL;
    }
    public void setJndiURL(String jndiURL) {
        this.jndiURL = jndiURL;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public int getConcurrentConsumers() {
        return concurrentConsumers;
    }

    public void setConcurrentConsumers(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }

    public void setConcurrentConsumers(String concurrentConsumers) {
        this.concurrentConsumers = Integer.parseInt(concurrentConsumers);
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getDurableSubscriptionClientId() {
        return durableSubscriptionClientId;
    }

    public void setDurableSubscriptionClientId(String durableSubscriptionClientId) {
        this.durableSubscriptionClientId = durableSubscriptionClientId;
    }

    public String getDurableSubscriptionName() {
        return durableSubscriptionName;
    }

    public void setDurableSubscriptionName(String durableSubscriptionName) {
        this.durableSubscriptionName = durableSubscriptionName;
    }

    public long getReceiveTimeout() {
        return receiveTimeout;
    }

    public void setReceiveTimeout(long receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
    }

    public void setReceiveTimeout(String receiveTimeout) {
        this.receiveTimeout = Long.parseLong(receiveTimeout);
    }
    public String getTargetService() {
        return targetService;
    }
    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }
    public boolean isSessionTransacted() {
        return sessionTransacted;
    }
    public void setSessionTransacted(boolean sessionTransacted) {
        this.sessionTransacted = sessionTransacted;
    }
    public void setSessionTransacted(String sessionTransacted) {
        this.sessionTransacted = Boolean.valueOf(sessionTransacted);
    }
    public String getConduitIdSelectorPrefix() {
        return conduitIdSelectorPrefix;
    }
    public void setConduitIdSelectorPrefix(String conduitIdSelectorPrefix) {
        this.conduitIdSelectorPrefix = conduitIdSelectorPrefix;
    }
    public boolean isUseConduitIdSelector() {
        return useConduitIdSelector;
    }

    public void setUseConduitIdSelector(String useConduitIdSelectorSt) {
        this.useConduitIdSelector = Boolean.valueOf(useConduitIdSelectorSt);
    }

    public void setUseConduitIdSelector(boolean useConduitIdSelector) {
        this.useConduitIdSelector = useConduitIdSelector;
    }

    public String getJndiTransactionManagerName() {
        return jndiTransactionManagerName;
    }

    public void setJndiTransactionManagerName(String jndiTransactionManagerName) {
        this.jndiTransactionManagerName = jndiTransactionManagerName;
    }

    public enum DeliveryModeType { PERSISTENT, NON_PERSISTENT };

    public enum MessageType {
        BYTE("byte"),
        BINARY("binary"),
        TEXT("text");
        private final String value;

        MessageType(String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        public static MessageType fromValue(String v) {
            for (MessageType c: MessageType.values()) {
                if (c.value.equals(v)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(v);
        }
    }

    public String getMessageSelector() {
        return messageSelector;
    }

    public void setMessageSelector(String messageSelector) {
        this.messageSelector = messageSelector;
    }



    public int getRetryInterval() {
        return retryInterval;
    }
    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }
    public void setRetryInterval(String retryInterval) {
        this.retryInterval = Integer.parseInt(retryInterval);
    }

    public boolean isOneSessionPerConnection() {
        return oneSessionPerConnection;
    }

    public void setOneSessionPerConnection(String oneSessionPerConnection) {
        this.oneSessionPerConnection = Boolean.parseBoolean(oneSessionPerConnection);
    }

    public void setOneSessionPerConnection(boolean oneSessionPerConnection) {
        this.oneSessionPerConnection = oneSessionPerConnection;
    }

    public boolean isIgnoreTimeoutException() {
        return ignoreTimeoutException;
    }

    public void setIgnoreTimeoutException(boolean ignoreTimeoutException) {
        this.ignoreTimeoutException = ignoreTimeoutException;
    }

}
