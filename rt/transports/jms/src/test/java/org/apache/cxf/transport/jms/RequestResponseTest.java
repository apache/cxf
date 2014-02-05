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
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.junit.BeforeClass;
import org.junit.Test;

public class RequestResponseTest extends AbstractJMSTester {
    private static final int MAX_RECEIVE_TIME = 10;

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
    public void testRequestQueueResponseTempQueue() throws Exception {
        EndpointInfo ei = setupServiceInfo("http://cxf.apache.org/jms_simple", "/wsdl/jms_spec_testsuite.wsdl",
                         "JMSSimpleService002X", "SimplePortQueueRequest");
        sendAndReceiveMessages(ei);
    }
    
    @Test
    public void testRequestQueueResponseStaticQueue() throws Exception {
        EndpointInfo ei = setupServiceInfo("http://cxf.apache.org/jms_simple", "/wsdl/jms_spec_testsuite.wsdl",
                         "JMSSimpleService002X", "SimplePortQueueRequestQueueResponse");
        sendAndReceiveMessages(ei);
    }
    
    @Test
    public void testRequestTopicResponseTempQueue() throws Exception {
        EndpointInfo ei = setupServiceInfo("http://cxf.apache.org/jms_simple", "/wsdl/jms_spec_testsuite.wsdl",
                         "JMSSimpleService002X", "SimplePortTopicRequest");
        sendAndReceiveMessages(ei);
    }
    
    @Test
    public void testRequestTopicResponseStaticQueue() throws Exception {
        EndpointInfo ei = setupServiceInfo("http://cxf.apache.org/jms_simple", "/wsdl/jms_spec_testsuite.wsdl",
                         "JMSSimpleService002X", "SimplePortTopicRequestQueueResponse");
        sendAndReceiveMessages(ei);
    }
    
    private Message createMessage() {
        Message outMessage = new MessageImpl();
        JMSMessageHeadersType header = new JMSMessageHeadersType();
        header.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
        header.setJMSPriority(1);
        header.setTimeToLive(1000);
        outMessage.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, header);
        outMessage.put(Message.ENCODING, "US-ASCII");
        return outMessage;
    }

    protected void sendAndReceiveMessages(EndpointInfo ei) throws IOException {
        // set up the conduit send to be true
        JMSConduit conduit = setupJMSConduit(ei, true);
        final Message outMessage = createMessage();
        final JMSDestination destination = setupJMSDestination(ei);

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
                    backConduit = destination.getBackChannel(m);
                    // wait for the message to be got from the conduit
                    Message replyMessage = new MessageImpl();
                    sendoutMessage(backConduit, replyMessage, true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        destination.setMessageObserver(observer);
        
        try {
            sendoutMessage(conduit, outMessage, false, true);
            // wait for the message to be got from the destination,
            // create the thread to handler the Destination incoming message
    
            waitForReceiveInMessage();
            verifyReceivedMessage(inMessage);
        } finally {
            conduit.close();
            destination.shutdown();
        }
    }


}
