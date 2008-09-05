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

import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.MultiplexDestination;
import org.easymock.classextension.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;

public class JMSDestinationTest extends AbstractJMSTester {
    private Message destMessage;

    @BeforeClass
    public static void createAndStartBroker() throws Exception {
        startBroker(new JMSBrokerSetup("tcp://localhost:61500"));
    }

    private void waitForReceiveInMessage() {
        int waitTime = 0;
        while (inMessage == null && waitTime < 3000) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // do nothing here
            }
            waitTime = waitTime + 1000;
        }
        assertTrue("Can't receive the Conduit Message in 3 seconds", inMessage != null);
    }

    private void waitForReceiveDestMessage() {
        int waitTime = 0;
        while (destMessage == null && waitTime < 3000) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // do nothing here
            }
            waitTime = waitTime + 1000;
        }
        assertTrue("Can't receive the Destination message in 3 seconds", destMessage != null);
    }



    public JMSDestination setupJMSDestination(boolean send) throws IOException {
        ConduitInitiator conduitInitiator = EasyMock.createMock(ConduitInitiator.class);
        JMSDestination jmsDestination = new JMSDestination(bus, conduitInitiator, endpointInfo);
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
        setupServiceInfo("http://cxf.apache.org/jms_conf_test",
                         "/wsdl/others/jms_test_no_addr.wsdl",
                         "HelloWorldQueueBinMsgService",
                         "HelloWorldQueueBinMsgPort");
        JMSDestination destination = setupJMSDestination(false);
        assertEquals("Can't get the right ServerConfig's MessageTimeToLive ",
                     500L,
                     destination.getServerConfig().getMessageTimeToLive());
        assertEquals("Can't get the right Server's MessageSelector",
                     "cxf_message_selector",
                     destination.getRuntimePolicy().getMessageSelector());
        assertEquals("Can't get the right SessionPoolConfig's LowWaterMark",
                     10,
                     destination.getSessionPool().getLowWaterMark());
        assertEquals("Can't get the right AddressPolicy's ConnectionPassword",
                     "testPassword",
                     destination.getJMSAddress().getConnectionPassword());
        assertEquals("Can't get the right DurableSubscriberName",
                     "cxf_subscriber",
                     destination.getRuntimePolicy().getDurableSubscriberName());
        assertEquals("Can't get the right MessageSelectorName",
                     "cxf_message_selector",
                     destination.getRuntimePolicy().getMessageSelector());
        BusFactory.setDefaultBus(null);

    }

    @Test
    public void testGetConfigurationFormWSDL() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        BusFactory.setDefaultBus(null);
        bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        setupServiceInfo("http://cxf.apache.org/hello_world_jms",
                         "/wsdl/jms_test.wsdl",
                         "HelloWorldQueueBinMsgService",
                         "HelloWorldQueueBinMsgPort");

        JMSDestination destination = setupJMSDestination(false);

        assertEquals("Can't get the right DurableSubscriberName",
                     "CXF_subscriber",
                     destination.getRuntimePolicy().getDurableSubscriberName());

        assertEquals("Can't get the right AddressPolicy's ConnectionPassword",
                     "dynamicQueues/test.jmstransport.binary",
                     destination.getJMSAddress().getJndiDestinationName());

        BusFactory.setDefaultBus(null);

    }

    @Test
    public void testDurableSubscriber() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        BusFactory.setDefaultBus(null);
        bus = bf.createBus("/wsdl/jms_test_config.xml");
        BusFactory.setDefaultBus(bus);
        destMessage = null;
        inMessage = null;
        setupServiceInfo("http://cxf.apache.org/hello_world_jms",
                         "/wsdl/jms_test.wsdl",
                         "HelloWorldPubSubService",
                         "HelloWorldPubSubPort");
        JMSConduit conduit = setupJMSConduit(true, false);
        Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage);
        JMSDestination destination = null;
        try {
            destination = setupJMSDestination(true);
            destination.activate();
        } catch (IOException e) {
            assertFalse("The JMSDestination activate should not through exception ", false);
            e.printStackTrace();
        }
        sendoutMessage(conduit, outMessage, true);
        // wait for the message to be get from the destination
        waitForReceiveDestMessage();
        // just verify the Destination inMessage
        assertTrue("The destiantion should have got the message ", destMessage != null);
        verifyReceivedMessage(destMessage);
        verifyHeaders(destMessage, outMessage);
        destination.shutdown();
    }

    @Test
    public void testOneWayDestination() throws Exception {
        destMessage = null;
        inMessage = null;
        setupServiceInfo("http://cxf.apache.org/hello_world_jms",
                         "/wsdl/jms_test.wsdl",
                         "HWStaticReplyQBinMsgService",
                         "HWStaticReplyQBinMsgPort");
        JMSConduit conduit = setupJMSConduit(true, false);
        Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage);
        JMSDestination destination = null;
        try {
            destination = setupJMSDestination(true);
            destination.activate();
        } catch (IOException e) {
            assertFalse("The JMSDestination activate should not throw exception ", false);
            e.printStackTrace();
        }
        sendoutMessage(conduit, outMessage, true);
        // wait for the message to be get from the destination
        waitForReceiveDestMessage();
        // just verify the Destination inMessage
        assertTrue("The destiantion should have got the message ", destMessage != null);
        verifyReceivedMessage(destMessage);
        verifyHeaders(destMessage, outMessage);
        destination.shutdown();
    }

    private void setupMessageHeader(Message outMessage) {
        JMSMessageHeadersType header = new JMSMessageHeadersType();
        header.setJMSCorrelationID("Destination test");
        header.setJMSDeliveryMode(3);
        header.setJMSPriority(1);
        header.setTimeToLive(1000);
        outMessage.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, header);
    }

    private void verifyReceivedMessage(Message inMessage) {
        ByteArrayInputStream bis =
            (ByteArrayInputStream) inMessage.getContent(InputStream.class);
        byte bytes[] = new byte[bis.available()];
        try {
            bis.read(bytes);
        } catch (IOException ex) {
            assertFalse("Read the Destination recieved Message error ", false);
            ex.printStackTrace();
        }
        String reponse = IOUtils.newStringFromBytes(bytes);
        assertEquals("The reponse date should be equals", reponse, "HelloWorld");
    }

    private void verifyRequestResponseHeaders(Message inMessage, Message outMessage) {
        JMSMessageHeadersType outHeader =
            (JMSMessageHeadersType)outMessage.get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);

        JMSMessageHeadersType inHeader =
            (JMSMessageHeadersType)inMessage.get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);

        verifyJmsHeaderEquality(outHeader, inHeader);

    }

    private void verifyHeaders(Message inMessage, Message outMessage) {
        JMSMessageHeadersType outHeader =
            (JMSMessageHeadersType)outMessage.get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);

        JMSMessageHeadersType inHeader =
            (JMSMessageHeadersType)inMessage.get(JMSConstants.JMS_SERVER_REQUEST_HEADERS);

        verifyJmsHeaderEquality(outHeader, inHeader);

    }

    private void verifyJmsHeaderEquality(JMSMessageHeadersType outHeader, JMSMessageHeadersType inHeader) {
        assertEquals("The inMessage and outMessage JMS Header's CorrelationID should be equals",
                     outHeader.getJMSCorrelationID(), inHeader.getJMSCorrelationID());
        assertEquals("The inMessage and outMessage JMS Header's JMSPriority should be equals",
                     outHeader.getJMSPriority(), inHeader.getJMSPriority());
        assertEquals("The inMessage and outMessage JMS Header's JMSType should be equals",
                     outHeader.getJMSType(), inHeader.getJMSType());

    }



    @Test
    public void testRoundTripDestination() throws Exception {

        inMessage = null;
        setupServiceInfo("http://cxf.apache.org/hello_world_jms",
                         "/wsdl/jms_test.wsdl",
                         "HelloWorldService",
                         "HelloWorldPort");
        //set up the conduit send to be true
        JMSConduit conduit = setupJMSConduit(true, false);
        final Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage);
        final JMSDestination destination = setupJMSDestination(true);

        //set up MessageObserver for handling the conduit message
        MessageObserver observer = new MessageObserver() {
            public void onMessage(Message m) {
                Exchange exchange = new ExchangeImpl();
                exchange.setInMessage(m);
                m.setExchange(exchange);
                verifyReceivedMessage(m);
                verifyHeaders(m, outMessage);
                //setup the message for
                Conduit backConduit;
                try {
                    backConduit = destination.getBackChannel(m, null, null);
                    //wait for the message to be got from the conduit
                    Message replyMessage = new MessageImpl();
                    sendoutMessage(backConduit, replyMessage, true);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        destination.setMessageObserver(observer);
        //set is oneway false for get response from destination
        sendoutMessage(conduit, outMessage, false);
        //wait for the message to be got from the destination,
        // create the thread to handler the Destination incoming message

        waitForReceiveInMessage();
        verifyReceivedMessage(inMessage);
        // wait for a while for the jms session recycling

        // Send a second message to check for an issue
        // Where the session was closed the second time
        sendoutMessage(conduit, outMessage, false);
        waitForReceiveInMessage();
        verifyReceivedMessage(inMessage);

        Thread.sleep(1000);
        destination.shutdown();
    }

    @Test
    public void testPropertyExclusion() throws Exception {

        final String customPropertyName =
            "THIS_PROPERTY_WILL_NOT_BE_AUTO_COPIED";

        inMessage = null;
        setupServiceInfo("http://cxf.apache.org/hello_world_jms",
                         "/wsdl/jms_test.wsdl",
                         "HelloWorldService",
                         "HelloWorldPort");
        //set up the conduit send to be true
        JMSConduit conduit = setupJMSConduit(true, false);
        final Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage);

        JMSPropertyType excludeProp = new JMSPropertyType();
        excludeProp.setName(customPropertyName);
        excludeProp.setValue(customPropertyName);

        JMSMessageHeadersType headers = (JMSMessageHeadersType)
            outMessage.get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
        headers.getProperty().add(excludeProp);


        final JMSDestination destination = setupJMSDestination(true);

        //set up MessageObserver for handling the conduit message
        MessageObserver observer = new MessageObserver() {
            public void onMessage(Message m) {
                Exchange exchange = new ExchangeImpl();
                exchange.setInMessage(m);
                m.setExchange(exchange);
                verifyReceivedMessage(m);
                verifyHeaders(m, outMessage);
                //setup the message for
                Conduit backConduit;
                try {
                    backConduit = destination.getBackChannel(m, null, null);
                    //wait for the message to be got from the conduit
                    Message replyMessage = new MessageImpl();
                    sendoutMessage(backConduit, replyMessage, true);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        destination.setMessageObserver(observer);
        //set is oneway false for get response from destination
        sendoutMessage(conduit, outMessage, false);
        //wait for the message to be got from the destination,
        // create the thread to handler the Destination incoming message

        waitForReceiveInMessage();
        verifyReceivedMessage(inMessage);


        verifyRequestResponseHeaders(inMessage, outMessage);

        JMSMessageHeadersType inHeader =
            (JMSMessageHeadersType)inMessage.get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);

        assertTrue("property has been excluded, only CONTENT_TYPE should be here",
                inHeader.getProperty().size() == 1);
        assertTrue("property has been excluded, only CONTENT_TYPE should be here",
                inHeader.getProperty().get(0).getName().equals(Message.CONTENT_TYPE));
        // wait for a while for the jms session recycling
        Thread.sleep(1000);
        destination.shutdown();
    }

    @Test
    public void testIsMultiplexCapable() throws Exception {
        inMessage = null;
        setupServiceInfo("http://cxf.apache.org/hello_world_jms",
                         "/wsdl/jms_test.wsdl",
                         "HelloWorldService",
                         "HelloWorldPort");
        final JMSDestination destination = setupJMSDestination(true);
        assertTrue("is multiplex", destination instanceof MultiplexDestination);
    }
}
