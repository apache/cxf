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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.MultiplexDestination;
import org.junit.BeforeClass;
import org.junit.Test;

public class JMSDestinationTest extends AbstractJMSTester {
    private static final int MAX_RECEIVE_TIME = 10;
    private Message destMessage;

    public JMSDestinationTest() {
        
    }
    
    @BeforeClass
    public static void createAndStartBroker() throws Exception {
        startBroker(new JMSBrokerSetup("tcp://localhost:61500"));
    }

    private void waitForReceiveInMessage() {
        int waitTime = 0;
        while (inMessage == null && waitTime < MAX_RECEIVE_TIME) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // do nothing here
            }
            waitTime++;
        }
        assertTrue("Can't receive the Conduit Message in " + MAX_RECEIVE_TIME + " seconds",
                   inMessage != null);
    }

    private void waitForReceiveDestMessage() {
        int waitTime = 0;
        while (destMessage == null && waitTime < MAX_RECEIVE_TIME) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // do nothing here
            }
            waitTime++;
        }
        assertTrue("Can't receive the Destination message in " + MAX_RECEIVE_TIME 
                   + " seconds", destMessage != null);
    }

    public JMSDestination setupJMSDestination(boolean send) throws IOException {
        JMSConfiguration jmsConfig = new JMSOldConfigHolder()
            .createJMSConfigurationFromEndpointInfo(bus, endpointInfo, false);
        JMSDestination jmsDestination = new JMSDestination(bus, endpointInfo, jmsConfig);

        if (send) {
            // setMessageObserver
            observer = new MessageObserver() {
                public void onMessage(Message m) {
                    Exchange exchange = new ExchangeImpl();
                    exchange.setInMessage(m);
                    m.setExchange(exchange);
                    destMessage = m;
                }
            };
            jmsDestination.setMessageObserver(observer);
        }
        return jmsDestination;
    }
    
    
    @Test
    public void testGetConfigurationFromSpring() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        BusFactory.setDefaultBus(null);
        bus = bf.createBus("/jms_test_config.xml");
        BusFactory.setDefaultBus(bus);
        setupServiceInfo("http://cxf.apache.org/jms_conf_test", "/wsdl/others/jms_test_no_addr.wsdl",
                         "HelloWorldQueueBinMsgService", "HelloWorldQueueBinMsgPort");
        JMSDestination destination = setupJMSDestination(false);        
        JMSConfiguration jmsConfig = destination.getJmsConfig();        
        //JmsTemplate jmsTemplate = destination.getJmsTemplate();
        //AbstractMessageListenerContainer jmsListener = destination.getJmsListener();
        assertEquals("Can't get the right ServerConfig's MessageTimeToLive ", 500L, jmsConfig
            .getTimeToLive());
        assertEquals("Can't get the right Server's MessageSelector", "cxf_message_selector", jmsConfig
            .getMessageSelector());
        // assertEquals("Can't get the right SessionPoolConfig's LowWaterMark", 10,
        // jmsListener.getLowWaterMark());
        // assertEquals("Can't get the right AddressPolicy's ConnectionPassword", "testPassword",
        // .getConnectionPassword());
        assertEquals("Can't get the right DurableSubscriberName", "cxf_subscriber", jmsConfig
            .getDurableSubscriptionName());
        
        /*setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HelloWorldQueueBinMsgService", "HelloWorldQueueBinMsgPort");
        destination = setupJMSDestination(false);
        jmsConfig = destination.getJmsConfig();*/
        assertEquals("The receiveTimeout should be set", jmsConfig.getReceiveTimeout().longValue(), 1500L);
        assertEquals("The concurrentConsumer should be set", jmsConfig.getConcurrentConsumers(), 3);
        assertEquals("The maxConcurrentConsumer should be set", jmsConfig.getMaxConcurrentConsumers(), 5);
        assertEquals("The maxSuspendedContinuations should be set", 
                     jmsConfig.getMaxSuspendedContinuations(), 2);
        assertTrue("The acceptMessagesWhileStopping should be set to true",
                   jmsConfig.isAcceptMessagesWhileStopping());
        assertNotNull("The connectionFactory should not be null", jmsConfig.getConnectionFactory());
        assertTrue("Should get the instance of ActiveMQConnectionFactory", 
                   jmsConfig.getConnectionFactory() instanceof ActiveMQConnectionFactory);
        ActiveMQConnectionFactory cf = (ActiveMQConnectionFactory)jmsConfig.getConnectionFactory();
        assertEquals("The borker URL is wrong", cf.getBrokerURL(), "tcp://localhost:61500");
        assertEquals("Get a wrong TargetDestination", jmsConfig.getTargetDestination(), "queue:test");
        assertEquals("Get the wrong pubSubDomain value", jmsConfig.isPubSubDomain(), false);
        
        BusFactory.setDefaultBus(null);

    }

    @Test
    public void testGetConfigurationFromWSDL() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        BusFactory.setDefaultBus(null);
        bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HelloWorldQueueBinMsgService", "HelloWorldQueueBinMsgPort");

        JMSDestination destination = setupJMSDestination(false);

        assertEquals("Can't get the right DurableSubscriberName", "CXF_subscriber", destination
            .getJmsConfig().getDurableSubscriptionName());

        assertEquals("Can't get the right AddressPolicy's Destination",
                     "dynamicQueues/test.jmstransport.binary", destination.getJmsConfig()
                         .getTargetDestination());

        BusFactory.setDefaultBus(null);

    }

    @Test
    public void testDurableSubscriber() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        BusFactory.setDefaultBus(null);
        bus = bf.createBus("jms_test_config.xml");
        BusFactory.setDefaultBus(bus);
        destMessage = null;
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HelloWorldPubSubService", "HelloWorldPubSubPort");
        JMSConduit conduit = setupJMSConduit(true, false);
        Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage);
        JMSDestination destination = setupJMSDestination(true);
        sendoutMessage(conduit, outMessage, true);
        // wait for the message to be get from the destination
        //long time = System.currentTimeMillis();
        waitForReceiveDestMessage();
        //System.out.println("time: " + (System.currentTimeMillis() - time));
        // just verify the Destination inMessage
        assertTrue("The destiantion should have got the message ", destMessage != null);
        verifyReceivedMessage(destMessage);
        verifyHeaders(destMessage, outMessage);
        conduit.close();
        destination.shutdown();
    }

    @Test
    public void testOneWayDestination() throws Exception {
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HWStaticReplyQBinMsgService", "HWStaticReplyQBinMsgPort");
        JMSConduit conduit = setupJMSConduit(true, false);
        Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage);
        JMSDestination destination = setupJMSDestination(true);
        sendoutMessage(conduit, outMessage, true);
        // wait for the message to be get from the destination
        waitForReceiveDestMessage();
        // just verify the Destination inMessage
        assertTrue("The destiantion should have got the message ", destMessage != null);
        verifyReceivedMessage(destMessage);
        verifyHeaders(destMessage, outMessage);
        conduit.close();
        destination.shutdown();
    }

    @Test
    public void testOneWayReplyToSetUnset() throws Exception {
        /* 1. Test that replyTo destination set in WSDL is NOT used 
         * in spec compliant mode */
        
        destMessage = null;
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HWStaticReplyQBinMsgService", "HWStaticReplyQBinMsgPort");
        JMSConduit conduit = setupJMSConduit(true, false);
        Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage);
        JMSDestination destination = setupJMSDestination(true);
        sendoutMessage(conduit, outMessage, true);
        // wait for the message to be get from the destination
        waitForReceiveDestMessage();
        // just verify the Destination inMessage
        assertTrue("The destiantion should have got the message ", destMessage != null);
        verifyReplyToNotSet(destMessage);
        destMessage = null;
        
        /* 2. Test that replyTo destination set in WSDL IS used 
         * in spec non-compliant mode */
        
        conduit.getJmsConfig().setEnforceSpec(false);
        sendoutMessage(conduit, outMessage, true);
        waitForReceiveDestMessage();
        assertTrue("The destiantion should have got the message ", destMessage != null);
        String exName = conduit.getJmsConfig().getReplyDestination();
        exName = (exName.indexOf('/') != -1 && exName.indexOf('/') < exName.length()) 
            ? exName.substring(exName.indexOf('/') + 1) : exName;
        verifyReplyToSet(destMessage, Queue.class, exName);
        destMessage = null;
        
        /* 3. Test that replyTo destination provided via invocation context 
         * overrides the value set in WSDL and IS used in spec non-compliant mode */
        
        String contextReplyTo = conduit.getJmsConfig().getReplyDestination() + ".context";
        exName += ".context";
        setupMessageHeader(outMessage, "cidValue", contextReplyTo);
        sendoutMessage(conduit, outMessage, true);
        waitForReceiveDestMessage();
        assertTrue("The destiantion should have got the message ", destMessage != null);
        verifyReplyToSet(destMessage, Queue.class, exName);
        destMessage = null;
        
        /* 4. Test that replyTo destination provided via invocation context 
         * and the value set in WSDL are NOT used in spec non-compliant mode 
         * when JMSConstants.JMS_SET_REPLY_TO == false */

        setupMessageHeader(outMessage);
        outMessage.put(JMSConstants.JMS_SET_REPLY_TO, Boolean.FALSE);
        sendoutMessage(conduit, outMessage, true);
        waitForReceiveDestMessage();
        assertTrue("The destiantion should have got the message ", destMessage != null);
        verifyReplyToNotSet(destMessage);
        destMessage = null;
        
        /* 5. Test that replyTo destination set in WSDL IS used in spec non-compliant 
         * mode when JMSConstants.JMS_SET_REPLY_TO == true */

        setupMessageHeader(outMessage);
        outMessage.put(JMSConstants.JMS_SET_REPLY_TO, Boolean.TRUE);
        sendoutMessage(conduit, outMessage, true);
        waitForReceiveDestMessage();
        assertTrue("The destiantion should have got the message ", destMessage != null);
        exName = conduit.getJmsConfig().getReplyDestination();
        exName = (exName.indexOf('/') != -1 && exName.indexOf('/') < exName.length()) 
            ? exName.substring(exName.indexOf('/') + 1) : exName;
        verifyReplyToSet(destMessage, Queue.class, exName);
        destMessage = null;
        
        conduit.close();
        destination.shutdown();
    }

    
    protected void verifyReplyToNotSet(Message cxfMsg) {
        javax.jms.Message jmsMsg = 
            javax.jms.Message.class.cast(cxfMsg.get(JMSConstants.JMS_REQUEST_MESSAGE));
        assertNotNull("JMS Messsage must be null", jmsMsg);
    }
    
    protected void verifyReplyToSet(Message cxfMsg, 
                                    Class<? extends Destination> type, 
                                    String name) throws Exception {
        javax.jms.Message jmsMsg = 
            javax.jms.Message.class.cast(cxfMsg.get(JMSConstants.JMS_REQUEST_MESSAGE));
        assertNotNull("JMS Messsage must not be null", jmsMsg);
        assertNotNull("JMS Messsage's replyTo must not be null", jmsMsg.getJMSReplyTo());
        assertTrue("JMS Messsage's replyTo type must be of type " + type.getName(), 
                   type.isAssignableFrom(jmsMsg.getJMSReplyTo().getClass()));
        String receivedName = null; 
        if (type == Queue.class) {
            receivedName = ((Queue)jmsMsg.getJMSReplyTo()).getQueueName();
        } else if (type == Topic.class) {
            receivedName = ((Topic)jmsMsg.getJMSReplyTo()).getTopicName();
        }
        assertTrue("JMS Messsage's replyTo must be named " + name + " but was " + receivedName,
                   name == receivedName || receivedName.equals(name));
        
    }
    private void setupMessageHeader(Message outMessage, String correlationId, String replyTo) {
        JMSMessageHeadersType header = new JMSMessageHeadersType();
        header.setJMSCorrelationID(correlationId);
        header.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
        header.setJMSPriority(1);
        header.setTimeToLive(1000);
        header.setJMSReplyTo(replyTo != null ? replyTo : null);
        outMessage.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, header);
        outMessage.put(Message.ENCODING, "US-ASCII");
    }

    private void setupMessageHeader(Message outMessage) {
        setupMessageHeader(outMessage, "Destination test", null);
    }

    private void setupMessageHeader(Message outMessage, String correlationId) {
        setupMessageHeader(outMessage, correlationId, null);
    }

    private void verifyReceivedMessage(Message message) {
        ByteArrayInputStream bis = (ByteArrayInputStream)message.getContent(InputStream.class);
        byte bytes[] = new byte[bis.available()];
        try {
            bis.read(bytes);
        } catch (IOException ex) {
            assertFalse("Read the Destination recieved Message error ", false);
            ex.printStackTrace();
        }
        String response = IOUtils.newStringFromBytes(bytes);
        assertEquals("The response content should be equal", AbstractJMSTester.MESSAGE_CONTENT, response);
    }

    private void verifyRequestResponseHeaders(Message msgIn, Message msgOut) {
        JMSMessageHeadersType outHeader = (JMSMessageHeadersType)msgOut
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
        String inEncoding = (String) msgIn.get(Message.ENCODING);
        String outEncoding = (String) msgOut.get(Message.ENCODING);
        
        assertEquals("The message encoding should be equal", inEncoding, outEncoding);

        JMSMessageHeadersType inHeader = (JMSMessageHeadersType)msgIn
            .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);

        verifyJmsHeaderEquality(outHeader, inHeader);

    }

    private void verifyHeaders(Message msgIn, Message msgOut) {
        JMSMessageHeadersType outHeader = (JMSMessageHeadersType)msgOut
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);

        JMSMessageHeadersType inHeader = (JMSMessageHeadersType)msgIn
            .get(JMSConstants.JMS_SERVER_REQUEST_HEADERS);

        verifyJmsHeaderEquality(outHeader, inHeader);

    }

    private void verifyJmsHeaderEquality(JMSMessageHeadersType outHeader, JMSMessageHeadersType inHeader) {
        /*
         * if (outHeader.getJMSCorrelationID() != null) { // only check if the correlation id was explicitly
         * set as // otherwise the in header will contain an automatically // generated correlation id
         * assertEquals("The inMessage and outMessage JMS Header's CorrelationID should be equals", outHeader
         * .getJMSCorrelationID(), inHeader.getJMSCorrelationID()); }
         */
        assertEquals("The inMessage and outMessage JMS Header's JMSPriority should be equals", outHeader
            .getJMSPriority(), inHeader.getJMSPriority());
        assertEquals("The inMessage and outMessage JMS Header's JMSDeliveryMode should be equals", outHeader
                     .getJMSDeliveryMode(), inHeader.getJMSDeliveryMode());
        assertEquals("The inMessage and outMessage JMS Header's JMSType should be equals", outHeader
            .getJMSType(), inHeader.getJMSType());
    }

    @Test
    public void testRoundTripDestination() throws Exception {

        setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HelloWorldService", "HelloWorldPort");
        // set up the conduit send to be true
        JMSConduit conduit = setupJMSConduit(true, false);
        final Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage, null);
        final JMSDestination destination = setupJMSDestination(false);

        // set up MessageObserver for handling the conduit message
        MessageObserver observer = new MessageObserver() {
            public void onMessage(Message m) {
                Exchange exchange = new ExchangeImpl();
                exchange.setInMessage(m);
                m.setExchange(exchange);
                verifyReceivedMessage(m);
                verifyHeaders(m, outMessage);
                // setup the message for
                Conduit backConduit;
                try {
                    backConduit = destination.getBackChannel(m, null, null);
                    // wait for the message to be got from the conduit
                    Message replyMessage = new MessageImpl();
                    sendoutMessage(backConduit, replyMessage, true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        destination.setMessageObserver(observer);
        // set is oneway false for get response from destination
        sendoutMessage(conduit, outMessage, false);
        // wait for the message to be got from the destination,
        // create the thread to handler the Destination incoming message

        waitForReceiveInMessage();
        verifyReceivedMessage(inMessage);
        // wait for a while for the jms session recycling

        inMessage = null;
        // Send a second message to check for an issue
        // Where the session was closed the second time
        sendoutMessage(conduit, outMessage, false);
        waitForReceiveInMessage();
        verifyReceivedMessage(inMessage);

        Thread.sleep(1000);
        conduit.close();
        destination.shutdown();
    }

    @Test
    public void testProperty() throws Exception {

        final String customPropertyName = "THIS_PROPERTY_WILL_NOT_BE_AUTO_COPIED";

        setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HelloWorldService", "HelloWorldPort");
        // set up the conduit send to be true
        JMSConduit conduit = setupJMSConduit(true, false);
        final Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage, null);

        JMSPropertyType excludeProp = new JMSPropertyType();
        excludeProp.setName(customPropertyName);
        excludeProp.setValue(customPropertyName);

        JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
        headers.getProperty().add(excludeProp);

        final JMSDestination destination = setupJMSDestination(false);

        // set up MessageObserver for handling the conduit message
        MessageObserver observer = new MessageObserver() {
            public void onMessage(Message m) {
                Exchange exchange = new ExchangeImpl();
                exchange.setInMessage(m);
                m.setExchange(exchange);
                verifyReceivedMessage(m);
                verifyHeaders(m, outMessage);
                // setup the message for
                Conduit backConduit;
                try {
                    backConduit = destination.getBackChannel(m, null, null);
                    // wait for the message to be got from the conduit
                    Message replyMessage = new MessageImpl();
                    // copy the message encoding
                    replyMessage.put(Message.ENCODING, m.get(Message.ENCODING));
                    sendoutMessage(backConduit, replyMessage, true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        destination.setMessageObserver(observer);
        // set is oneway false for get response from destination
        sendoutMessage(conduit, outMessage, false);
        // wait for the message to be got from the destination,
        // create the thread to handler the Destination incoming message

        waitForReceiveInMessage();
        verifyReceivedMessage(inMessage);

        verifyRequestResponseHeaders(inMessage, outMessage);

        JMSMessageHeadersType inHeader = (JMSMessageHeadersType)inMessage
            .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
        assertNotNull("The inHeader should not be null", inHeader);
        assertNotNull("The property should not be null " + inHeader.getProperty());
        // TODO we need to check the SOAP JMS transport properties here
        
        // wait for a while for the jms session recycling
        Thread.sleep(1000);
        conduit.close();
        destination.shutdown();
    }

    @Test
    public void testIsMultiplexCapable() throws Exception {
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HelloWorldService", "HelloWorldPort");
        final JMSDestination destination = setupJMSDestination(true);
        assertTrue("is multiplex", destination instanceof MultiplexDestination);
        destination.shutdown();
    }
    
    @Test
    public void testSecurityContext() throws Exception {
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HelloWorldService", "HelloWorldPort");
        final JMSDestination destination = setupJMSDestination(true);
        // set up the conduit send to be true
        JMSConduit conduit = setupJMSConduit(true, false);
        final Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage, null);
        sendoutMessage(conduit, outMessage, true);
        waitForReceiveDestMessage();
        SecurityContext securityContext = destMessage.get(SecurityContext.class);
        assertNotNull("SecurityContext should be set in message received by JMSDestination", securityContext);
        assertEquals("Principal in SecurityContext should be", "testUser", 
                     securityContext.getUserPrincipal().getName());
        conduit.close();
        destination.shutdown();
    }

}
