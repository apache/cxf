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

import java.util.Set;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.junit.EmbeddedActiveMQResource;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JMSHeaderTypeTest {
    private static final String TEST_VALUE = "test";
    private static final String CONVERTED_RESPONSE_KEY = "org__apache__cxf__message__Message__RESPONSE_CODE";

    @Rule public EmbeddedActiveMQResource server = new EmbeddedActiveMQResource(0);

    @Test
    public void testConversionIn() throws JMSException {
        Message message = createMessage();
        message.setStringProperty(CONVERTED_RESPONSE_KEY, TEST_VALUE);
        JMSMessageHeadersType messageHeaders = JMSMessageHeadersType.from(message);
        Set<String> keys = messageHeaders.getPropertyKeys();
        assertEquals(2, keys.size());
        assertEquals(TEST_VALUE, messageHeaders.getProperty(org.apache.cxf.message.Message.RESPONSE_CODE));
    }
    
    @Test
    public void testConversionOut() throws JMSException {
        Message message = createMessage();
        JMSMessageHeadersType messageHeaders = new JMSMessageHeadersType();
        messageHeaders.putProperty(org.apache.cxf.message.Message.RESPONSE_CODE, TEST_VALUE);
        messageHeaders.writeTo(message);
        
        assertEquals(CONVERTED_RESPONSE_KEY, message.getPropertyNames().nextElement());
        
    }

    private Message createMessage() throws JMSException {
        try (ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://test?broker.persistent=false")) {
            Connection connection = cf.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Message message = session.createMessage();
            connection.stop();
            return message;
        }
    }
}
