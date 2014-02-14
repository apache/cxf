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

import java.util.HashMap;
import java.util.Map;

import javax.jms.Message;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;

/**
 * 
 */
public class JMSEndpoint {
    public static final String JNDI = "jndi";
    public static final String TOPIC = "topic";
    public static final String QUEUE = "queue";
    public static final String JNDI_TOPIC = "jndi-topic";

    // shared parameters
    public static final String DELIVERYMODE_PARAMETER_NAME = "deliveryMode";
    public static final String TIMETOLIVE_PARAMETER_NAME = "timeToLive";
    public static final String PRIORITY_PARAMETER_NAME = "priority";
    public static final String REPLYTONAME_PARAMETER_NAME = "replyToName";
    // The new configuration to set the message type of jms message body
    public static final String MESSAGE_TYPE_PARAMETER_NAME = "messageType";

    // default parameters
    public static final DeliveryModeType DELIVERYMODE_DEFAULT = DeliveryModeType.PERSISTENT;
    public static final long TIMETOLIVE_DEFAULT = Message.DEFAULT_TIME_TO_LIVE;
    public static final int PRIORITY_DEFAULT = Message.DEFAULT_PRIORITY;

    // jndi parameters ? need to be sure.
    public static final String JNDICONNECTIONFACTORYNAME_PARAMETER_NAME = "jndiConnectionFactoryName";
    public static final String JNDIINITIALCONTEXTFACTORY_PARAMETER_NAME = "jndiInitialContextFactory";
    public static final String JNDIURL_PARAMETER_NAME = "jndiURL";
    public static final String JNDI_PARAMETER_NAME_PREFIX = "jndi-";

    // queue and topic parameters
    public static final String TOPICREPLYTONAME_PARAMETER_NAME = "topicReplyToName";
    
    Map<String, String> jndiParameters = new HashMap<String, String>();
    Map<String, String> parameters = new HashMap<String, String>();
    
    private String endpointUri;
    private String jmsVariant;
    private String destinationName;
    private DeliveryModeType deliveryMode;
    private MessageType messageType;
    private long timeToLive;
    private Integer priority;
    private String replyToName;
    private String topicReplyToName;
    private String jndiConnectionFactoryName;
    private String jndiInitialContextFactory;
    private String jndiURL;
    private String username;
    private String password;
    private boolean reconnectOnException = true;

    /**
     * @param uri
     * @param subject
     */
    public JMSEndpoint(String endpointUri) {
        this();
        if (!(StringUtils.isEmpty(endpointUri) || "jms://".equals(endpointUri) || !endpointUri.startsWith("jms"))) {
            this.endpointUri = endpointUri;
            JMSURIParser parsed = new JMSURIParser(endpointUri);
            setJmsVariant(parsed.getVariant());
            this.destinationName = parsed.getDestination();
            configureProperties(this, parsed.parseQuery());
        }
    }
    
    public JMSEndpoint() {
        this.jmsVariant = JMSEndpoint.QUEUE;
    }

    /**
     * @param endpoint
     * @param params
     */
    private static void configureProperties(JMSEndpoint endpoint, Map<String, String> params) {
        String deliveryMode = getAndRemoveParameter(params,
                                                    JMSEndpoint.DELIVERYMODE_PARAMETER_NAME);
        String timeToLive = getAndRemoveParameter(params,
                                                  JMSEndpoint.TIMETOLIVE_PARAMETER_NAME);
        String priority = getAndRemoveParameter(params, JMSEndpoint.PRIORITY_PARAMETER_NAME);
        String replyToName = getAndRemoveParameter(params,
                                                   JMSEndpoint.REPLYTONAME_PARAMETER_NAME);
        String topicReplyToName = getAndRemoveParameter(params,
                                                   JMSEndpoint.TOPICREPLYTONAME_PARAMETER_NAME);
        String jndiConnectionFactoryName = getAndRemoveParameter(
                                                                 params,
                                                JMSEndpoint.JNDICONNECTIONFACTORYNAME_PARAMETER_NAME);
        String jndiInitialContextFactory = getAndRemoveParameter(
                                                                 params,
                                                JMSEndpoint.JNDIINITIALCONTEXTFACTORY_PARAMETER_NAME);
        String jndiUrl = getAndRemoveParameter(params, JMSEndpoint.JNDIURL_PARAMETER_NAME);

        String messageType = getAndRemoveParameter(params, JMSEndpoint.MESSAGE_TYPE_PARAMETER_NAME);
        
        if (deliveryMode != null) {
            endpoint.setDeliveryMode(DeliveryModeType.valueOf(deliveryMode));
        }
        if (timeToLive != null) {
            endpoint.setTimeToLive(Long.valueOf(timeToLive));
        }
        if (priority != null) {
            endpoint.setPriority(Integer.valueOf(priority));
        }
        if (replyToName != null && topicReplyToName != null) {
            throw new IllegalArgumentException(
                "The replyToName and topicReplyToName should not be defined at the same time.");
        }
        if (replyToName != null) {
            endpoint.setReplyToName(replyToName);
        }
        if (topicReplyToName != null) {
            endpoint.setTopicReplyToName(topicReplyToName);
        }
        if (jndiConnectionFactoryName != null) {
            endpoint.setJndiConnectionFactoryName(jndiConnectionFactoryName);
        }
        if (jndiInitialContextFactory != null) {
            endpoint.setJndiInitialContextFactory(jndiInitialContextFactory);
        }
        if (jndiUrl != null) {
            endpoint.setJndiURL(jndiUrl);
        }
        if (messageType != null) {
            endpoint.setMessageType(MessageType.fromValue(messageType));
        }

        for (String key : params.keySet()) {
            String value = params.get(key);
            if (value == null || value.equals("")) {
                continue;
            }
            if (key.startsWith(JMSEndpoint.JNDI_PARAMETER_NAME_PREFIX)) {
                key = key.substring(5);
                endpoint.putJndiParameter(key, value);
            } else {
                endpoint.putParameter(key, value);
            }
        }
    }

    /**
     * @param parameters
     * @param deliverymodeParameterName
     * @return
     */
    private static String getAndRemoveParameter(Map<String, String> parameters,
                                                String parameterName) {
        String value = parameters.get(parameterName);
        parameters.remove(parameterName);
        return value;
    }

    public String getRequestURI() {
        StringBuilder requestUri = new StringBuilder("jms:");
        if (jmsVariant == JNDI_TOPIC) {
            requestUri.append("jndi");
        } else {
            requestUri.append(jmsVariant);
        }
        requestUri.append(":" + destinationName);
        boolean first = true;
        for (String key : parameters.keySet()) {
            // now we just skip the MESSAGE_TYPE_PARAMETER_NAME 
            // and TARGETSERVICE_PARAMETER_NAME
            if (JMSSpecConstants.TARGETSERVICE_PARAMETER_NAME.equals(key) 
                || MESSAGE_TYPE_PARAMETER_NAME.equals(key)) {
                continue;
            }
            String value = parameters.get(key);
            if (first) {
                requestUri.append("?" + key + "=" + value);
                first = false;
            } else {
                requestUri.append("&" + key + "=" + value);
            }
        }
        return requestUri.toString();
    }

    /**
     * @param key
     * @param value
     */
    public void putJndiParameter(String key, String value) {
        jndiParameters.put(key, value);
    }

    public void putParameter(String key, String value) {
        parameters.put(key, value);
    }

    /**
     * @param targetserviceParameterName
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
    public String getJmsVariant() {
        return jmsVariant;
    }
    public void setJmsVariant(String jmsVariant) {
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
    public MessageType getMessageType() {
        return messageType == null ? MessageType.BYTE : messageType;
    }
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    public long getTimeToLive() {
        return timeToLive;
    }
    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
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
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public boolean isReconnectOnException() {
        return reconnectOnException;
    }
    public void setReconnectOnException(boolean reconnectOnException) {
        this.reconnectOnException = reconnectOnException;
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
}
