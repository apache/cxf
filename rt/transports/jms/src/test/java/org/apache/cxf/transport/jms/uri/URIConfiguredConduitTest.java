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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
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

    private enum SyncType {
        sync,
        async
    };

    @Test
    public void testSendReceive() throws Exception {
        sendAndReceive(SyncType.sync,
                       "jms:jndi:dynamicQueues/"
                           + SERVICE_QUEUE
                           + "?jndiInitialContextFactory=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
                           + "&replyToName=dynamicQueues/testreply" 
                           + "&messageType=text"
                           + "&jndiConnectionFactoryName=ConnectionFactory"
                           + "&jndiURL=" + BROKER_URI);
    }

    @Test
    public void testSendReceiveCFFromContext() throws Exception {
        sendAndReceive(SyncType.sync, "jms:queue:" + SERVICE_QUEUE + "?replyToName=testreply"
                                      + "&messageType=text" 
                                      + "&receiveTimeout=10000"
                                      + "&jndiConnectionFactoryName=ConnectionFactory");
    }

    public void sendAndReceive(SyncType syncType, String address) throws Exception {
        BusFactory bf = BusFactory.newInstance();
        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);

        // Register bean locator for cf lookup
        ConfiguredBeanLocator cbl = bus.getExtension(ConfiguredBeanLocator.class);
        MyBeanLocator registry = new MyBeanLocator(cbl);
        bus.setExtension(registry, ConfiguredBeanLocator.class);
        
        connectionFactory = new ActiveMQConnectionFactory(BROKER_URI);
        registry.register("ConnectionFactory", connectionFactory);
        TestReceiver receiver = new TestReceiver(connectionFactory, SERVICE_QUEUE, false);
        receiver.runAsync();

        EndpointInfo ei = new EndpointInfo();
        ei.setAddress(address);
        JMSOldConfigHolder holder = new JMSOldConfigHolder();
        JMSConfiguration jmsConfig = holder.createJMSConfigurationFromEndpointInfo(bus, ei, null, true);
        JMSConduit conduit = new JMSConduit(new EndpointReferenceType(), jmsConfig, bus);

        Exchange exchange = new ExchangeImpl();
        exchange.setSynchronous(syncType == SyncType.sync);
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
