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

import javax.jms.ConnectionFactory;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.jms.JMSConduit;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;
import org.apache.cxf.transport.jms.JMSOldConfigHolder;
import org.apache.cxf.transport.jms.util.TestReceiver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Checks if a CXF client works correlates requests and responses correctly if the server sets the message id
 * as correlation id on the response message
 */
public class URIConfiguredConduitTest {
    private static final String SERVICE_QUEUE = "test";
    private static final String BROKER_URI = "vm://localhost?broker.persistent=false";
    private ConnectionFactory connectionFactory;

    @Test
    public void testSendReceive() throws Exception {
        sendAndReceive(true, "testreply");
    }
    
    public void sendAndReceive(boolean synchronous, String replyDestination) throws Exception {
        BusFactory bf = BusFactory.newInstance();
        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        EndpointReferenceType target = new EndpointReferenceType();

        connectionFactory = new PooledConnectionFactory(BROKER_URI);
        TestReceiver receiver = new TestReceiver(connectionFactory, SERVICE_QUEUE, true);
        receiver.runAsync();

        JMSOldConfigHolder holder = new JMSOldConfigHolder();
        EndpointInfo ei = new EndpointInfo();
        String address = "jms:jndi:dynamicQueues/" + SERVICE_QUEUE
            + "?jndiInitialContextFactory=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
            + "&replyToName=dynamicQueues/" + replyDestination
            + "&messageType=text"
            + "&jndiConnectionFactoryName=ConnectionFactory"
            + "&jndiURL=" + BROKER_URI;
        ei.setAddress(address);
        JMSConfiguration jmsConfig = holder.createJMSConfigurationFromEndpointInfo(bus, ei , null, true);
        JMSConduit conduit = new JMSConduit(target, jmsConfig, bus);
        Exchange exchange = new ExchangeImpl();
        exchange.setSynchronous(synchronous);
        Message message = new MessageImpl();
        exchange.setOutMessage(message);
        conduit.sendExchange(exchange, "Request");
        waitForAsyncReply(exchange);
        receiver.close();
        if (exchange.getInMessage() == null) {
            throw new RuntimeException("No reply received within 2 seconds");
        }
        JMSMessageHeadersType inHeaders = (JMSMessageHeadersType)exchange.getInMessage()
            .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
        Assert.assertEquals(receiver.getRequestMessageId(), inHeaders.getJMSCorrelationID());
        conduit.close();
        bus.shutdown(true);
    }

    private void waitForAsyncReply(Exchange exchange) throws InterruptedException {
        int count = 0;
        while (exchange.getInMessage() == null && count <= 20) {
            Thread.sleep(100);
            count++;
        }
    }

}
