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

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.junit.EmbeddedActiveMQResource;
import org.apache.cxf.BusFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.jms.util.TestReceiver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Checks if a CXF client works correlates requests and responses correctly if the server sets the message id
 * as correlation id on the response message
 */
public class MessageIdAsCorrelationIdJMSConduitTest {
    private static final String SERVICE_QUEUE = "test";
    private static final String BROKER_URI = "vm://0?broker.persistent=false";
    
    @Rule public EmbeddedActiveMQResource server = new EmbeddedActiveMQResource(0);
    private ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URI);

    @Test
    public void testSendReceiveWithTempReplyQueue() throws Exception {
        sendAndReceive(true, null);
    }

    @Test
    public void testSendReceive() throws Exception {
        sendAndReceive(true, "testreply");
    }

    private void sendAndReceive(boolean synchronous, String replyDestination) throws InterruptedException {
        EndpointReferenceType target = new EndpointReferenceType();

        TestReceiver receiver = new TestReceiver(connectionFactory, SERVICE_QUEUE, true);
        receiver.runAsync();

        JMSConfiguration jmsConfig = new JMSConfiguration();
        jmsConfig.setTargetDestination(SERVICE_QUEUE);
        jmsConfig.setConnectionFactory(connectionFactory);
        jmsConfig.setUseConduitIdSelector(false);
        jmsConfig.setReplyDestination(replyDestination);

        JMSConduit conduit = new JMSConduit(target, jmsConfig, BusFactory.getDefaultBus());
        Exchange exchange = new ExchangeImpl();
        exchange.setSynchronous(synchronous);
        Message message = new MessageImpl();
        exchange.setOutMessage(message);
        conduit.sendExchange(exchange, "Request");
        waitForAsyncReply(exchange);
        receiver.close();
        assertNotNull("No reply received within 2 seconds", exchange.getInMessage());
        JMSMessageHeadersType inHeaders = (JMSMessageHeadersType)exchange.getInMessage()
            .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
        assertEquals(receiver.getRequestMessageId(), inHeaders.getJMSCorrelationID());
        conduit.close();
    }

    private static void waitForAsyncReply(Exchange exchange) throws InterruptedException {
        for (int count = 0; exchange.getInMessage() == null && count <= 20; count++) {
            Thread.sleep(100L);
        }
    }

}
