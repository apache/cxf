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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractJmsListeningContainer;

public class JMSConfiguration implements InitializingBean {
    private ConnectionFactory connectionFactory;
    private JmsTemplate jmsTemplate;
    private AbstractJmsListeningContainer jmsListener;
    private String targetDestination;
    private String replyDestination;
    private String messageType;
    private boolean pubSubDomain;

    public JMSConfiguration() {
        targetDestination = null;
        replyDestination = null;
        messageType = JMSConstants.TEXT_MESSAGE_TYPE;
        pubSubDomain = false;
    }

    public void afterPropertiesSet() throws Exception {
        /*
         * if (connectionFactory == null) { throw new RuntimeException("Required property connectionfactory
         * was not set"); } jmsTemplate.setConnectionFactory(connectionFactory);
         * jmsListener.setConnectionFactory(connectionFactory);
         */
    }

    public JmsTemplate getJmsTemplate() {
        return jmsTemplate;
    }

    @Required
    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public AbstractJmsListeningContainer getJmsListener() {
        return jmsListener;
    }

    @Required
    public void setJmsListener(AbstractJmsListeningContainer jmsListener) {
        this.jmsListener = jmsListener;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    @Required
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public String getTargetDestination() {
        return targetDestination;
    }

    public void setTargetDestination(String targetDestination) {
        this.targetDestination = targetDestination;
    }

    public String getReplyDestination() {
        return replyDestination;
    }

    public void setReplyDestination(String replyDestination) {
        this.replyDestination = replyDestination;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public boolean isPubSubDomain() {
        return pubSubDomain;
    }

    public void setPubSubDomain(boolean pubSubDomain) {
        this.pubSubDomain = pubSubDomain;
    }

}
