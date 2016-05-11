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
import java.io.Reader;
import java.io.StringReader;

import javax.jms.DeliveryMode;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("too volatile")
public class RequestResponseTest extends AbstractJMSTester {
    private static final int MAX_RECEIVE_TIME = 30;

    public RequestResponseTest() {
    }
    
    @BeforeClass
    public static void createAndStartBroker() throws Exception {
        startBroker(new JMSBrokerSetup("tcp://localhost:" + JMS_PORT));
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

    private JMSDestination setupJMSDestination(boolean send) throws IOException {

        adjustEndpointInfoURL();
        JMSConfiguration jmsConfig = new JMSOldConfigHolder()
            .createJMSConfigurationFromEndpointInfo(bus, endpointInfo, null, false);
        
        JMSDestination jmsDestination = new JMSDestination(bus, endpointInfo, jmsConfig);

        if (send) {
            // setMessageObserver
            observer = new MessageObserver() {
                public void onMessage(Message m) {
                    Exchange exchange = new ExchangeImpl();
                    exchange.setInMessage(m);
                    m.setExchange(exchange);
                }
            };
            jmsDestination.setMessageObserver(observer);
        }
        return jmsDestination;
    }
    
    private void setupMessageHeader(Message outMessage, String correlationId, String replyTo) {
        JMSMessageHeadersType header = new JMSMessageHeadersType();
        header.setJMSCorrelationID(correlationId);
        header.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
        header.setJMSPriority(1);
        header.setTimeToLive(5000);
        header.setJMSReplyTo(replyTo != null ? replyTo : null);
        outMessage.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, header);
        outMessage.put(Message.ENCODING, "US-ASCII");
    }

    private void setupMessageHeader(Message outMessage, String correlationId) {
        setupMessageHeader(outMessage, correlationId, null);
    }

    private void verifyReceivedMessage(Message message) {
        ByteArrayInputStream bis = (ByteArrayInputStream)message.getContent(InputStream.class);
        String response = "<not found>";
        if (bis != null) {
            byte bytes[] = new byte[bis.available()];
            try {
                bis.read(bytes);
            } catch (IOException ex) {
                assertFalse("Read the Destination recieved Message error ", false);
                ex.printStackTrace();
            }
            response = IOUtils.newStringFromBytes(bytes);
        } else {
            StringReader reader = (StringReader)message.getContent(Reader.class);
            char buffer[] = new char[5000];
            try {
                int i = reader.read(buffer);
                response = new String(buffer, 0 , i);
            } catch (IOException e) {
                assertFalse("Read the Destination recieved Message error ", false);
                e.printStackTrace();
            }
        }
        assertEquals("The response content should be equal", AbstractJMSTester.MESSAGE_CONTENT, response);
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
    public void testRequestQueueResponseDynamicQueue() throws Exception {
        setupServiceInfo("http://cxf.apache.org/jms_simple", "/wsdl/jms_spec_testsuite.wsdl",
                         "JMSSimpleService002X", "SimplePortQueueRequest");
        sendAndReceiveMessages();
    }
    
    @Test
    public void testRequestQueueResponseStaticQueue() throws Exception {
        setupServiceInfo("http://cxf.apache.org/jms_simple", "/wsdl/jms_spec_testsuite.wsdl",
                         "JMSSimpleService002X", "SimplePortQueueRequestQueueResponse");
        sendAndReceiveMessages();
    }
    
    @Test
    public void testRequestQueueResponseTopic() throws Exception {
        setupServiceInfo("http://cxf.apache.org/jms_simple", "/wsdl/jms_spec_testsuite.wsdl",
                         "JMSSimpleService002X", "SimplePortQueueRequestTopicResponse");
        sendAndReceiveMessages();
    }
    
    @Test
    public void testRequestTopicResponseDynamicQueue() throws Exception {
        setupServiceInfo("http://cxf.apache.org/jms_simple", "/wsdl/jms_spec_testsuite.wsdl",
                         "JMSSimpleService002X", "SimplePortTopicRequest");
        sendAndReceiveMessages();
    }
    
    @Test
    public void testRequestTopicResponseStaticQueue() throws Exception {
        setupServiceInfo("http://cxf.apache.org/jms_simple", "/wsdl/jms_spec_testsuite.wsdl",
                         "JMSSimpleService002X", "SimplePortTopicRequestQueueResponse");
        sendAndReceiveMessages();
    }
    
    @Test
    public void testRequestTopicResponseTopic() throws Exception {
        setupServiceInfo("http://cxf.apache.org/jms_simple", "/wsdl/jms_spec_testsuite.wsdl",
                         "JMSSimpleService002X", "SimplePortTopicRequestTopicResponse");
        sendAndReceiveMessages();
    }

    protected void sendAndReceiveMessages() throws IOException {
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

        conduit.close();
        destination.shutdown();
    }


}
