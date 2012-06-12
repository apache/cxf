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

import java.util.concurrent.Executors;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;



import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;


/**
 * Checks if a CXF client works correlates requests and responses correctly if the server sets the message
 * id as correlation id on the response message 
 */
public class MessageIdAsCorrelationIdJMSConduitTest {
    private static final String BROKER_URI = "vm:localhost?broker.persistent=false";
    private ConnectionFactory connectionFactory;
    private String requestMessageId;

    
    public void sendAndReceive(String replyDestination) throws Exception {
        BusFactory bf = BusFactory.newInstance();
        Bus bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        EndpointInfo endpointInfo = new EndpointInfo();
        EndpointReferenceType target = new EndpointReferenceType();

        connectionFactory = new PooledConnectionFactory(BROKER_URI);
        
        runReceiver();

        JMSConfiguration jmsConfig = new JMSConfiguration();
        jmsConfig.setTargetDestination("queue:test");
        jmsConfig.setConnectionFactory(connectionFactory);
        jmsConfig.setReplyDestination(replyDestination);

        JMSConduit conduit = new JMSConduit(endpointInfo, target, jmsConfig, bus);
        Exchange exchange = new ExchangeImpl();
        Message message = new MessageImpl();
        exchange.setOutMessage(message);
        conduit.sendExchange(exchange, "Request");
        JMSMessageHeadersType headers = (JMSMessageHeadersType)exchange.getInMessage()
            .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
        Assert.assertEquals(requestMessageId, headers.getJMSCorrelationID());
        conduit.close();
        bus.shutdown(true);
    }

    @Test
    public void testSendReceiveWithTempReplyQueue() throws Exception {
        sendAndReceive(null);
    }
    
    @Test
    public void testSendReceive() throws Exception {
        sendAndReceive("queue:testreply");
    }

    private void runReceiver() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            public void run() {
                try {
                    receiveAndRespondWithMessageIdAsCorrelationId();
                } catch (Exception e) {
                    // Ignore
                }
            }
        });
    }
    
    public void receiveAndRespondWithMessageIdAsCorrelationId() throws JmsException, JMSException {
        JmsTemplate template = new JmsTemplate(connectionFactory);        
        final javax.jms.Message message = template.receive("queue:test");
        requestMessageId = message.getJMSMessageID();
        template.send(message.getJMSReplyTo(), new MessageCreator() {

            public javax.jms.Message createMessage(Session session) throws JMSException {
                TextMessage replyMessage =  session.createTextMessage("Result");
                replyMessage.setJMSCorrelationID(message.getJMSMessageID());
                return replyMessage;
            }
        });
    }
}
