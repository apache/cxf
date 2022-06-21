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

package org.apache.cxf.transport.jms.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.jms.BytesMessage;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.junit.EmbeddedActiveMQResource;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSFactory;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JMSUtilTest {
    @Rule public EmbeddedActiveMQResource server = new EmbeddedActiveMQResource(0);

    @Test
    public void testCorrelationIDGeneration() {
        final String conduitId = UUID.randomUUID().toString().replaceAll("-", "");

        // test min edge case
        AtomicLong messageMinCount = new AtomicLong(0);
        createAndCheck(conduitId, "0000000000000000", messageMinCount.get());

        // test max edge case
        AtomicLong messageMaxCount = new AtomicLong(0xFFFFFFFFFFFFFFFFL);
        createAndCheck(conduitId, "ffffffffffffffff", messageMaxCount.get());

        // test overflow case
        AtomicLong overflowCount = new AtomicLong(0xFFFFFFFFFFFFFFFFL);
        createAndCheck(conduitId, "0000000000000000", overflowCount.incrementAndGet());

        // Test sequence
        AtomicLong sequence = new AtomicLong(0);
        createAndCheck(conduitId, "0000000000000001", sequence.incrementAndGet());
        createAndCheck(conduitId, "0000000000000002", sequence.incrementAndGet());
    }

    private void createAndCheck(String prefix, final String expectedIndex, long sequenceNum) {
        String correlationID = JMSUtil.createCorrelationId(prefix, sequenceNum);
        assertEquals("The correlationID value does not match expected value",
                     prefix + expectedIndex, correlationID);
    }

    @Test
    public void testJMSMessageMarshal() throws IOException, JMSException {
        String testMsg = "Test Message";
        final byte[] testBytes = testMsg.getBytes(Charset.defaultCharset().name()); // TODO encoding
        JMSConfiguration jmsConfig = new JMSConfiguration();
        jmsConfig.setConnectionFactory(new ActiveMQConnectionFactory("vm://0?broker.persistent=false"));

        try (ResourceCloser closer = new ResourceCloser()) {
            Connection connection = JMSFactory.createConnection(jmsConfig);
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            jakarta.jms.Message jmsMessage =
                JMSUtil.createAndSetPayload(testBytes, session, JMSConstants.BYTE_MESSAGE_TYPE);
            assertTrue("Message should have been of type BytesMessage ", jmsMessage instanceof BytesMessage);
        }

    }
}