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

import java.io.IOException;

import javax.jms.DeliveryMode;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.junit.Test;

public class RequestResponseTest extends AbstractJMSTester {

    private void verifyReceivedMessage(Message message) {
        String response = getContent(message);
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
        sendAndReceiveMessages(ei, true);
        sendAndReceiveMessages(ei, false);
    }
    
    @Test
    public void testRequestQueueResponseStaticQueue() throws Exception {
        EndpointInfo ei = setupServiceInfo("http://cxf.apache.org/jms_simple", "/wsdl/jms_spec_testsuite.wsdl",
                         "JMSSimpleService002X", "SimplePortQueueRequestQueueResponse");
        sendAndReceiveMessages(ei, true);
        sendAndReceiveMessages(ei, false);
    }
    
    @Test
    public void testRequestTopicResponseTempQueue() throws Exception {
        EndpointInfo ei = setupServiceInfo("http://cxf.apache.org/jms_simple", "/wsdl/jms_spec_testsuite.wsdl",
                         "JMSSimpleService002X", "SimplePortTopicRequest");
        sendAndReceiveMessages(ei, true);
    }
    
    @Test
    public void testRequestTopicResponseStaticQueue() throws Exception {
        EndpointInfo ei = setupServiceInfo("http://cxf.apache.org/jms_simple", "/wsdl/jms_spec_testsuite.wsdl",
                         "JMSSimpleService002X", "SimplePortTopicRequestQueueResponse");
        sendAndReceiveMessages(ei, true);
        sendAndReceiveMessages(ei, false);
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

    protected void sendAndReceiveMessages(EndpointInfo ei, boolean synchronous) throws IOException {
        inMessage = null;
        // set up the conduit send to be true
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
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
                    sendOneWayMessage(backConduit, replyMessage);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        destination.setMessageObserver(observer);
        
        try {
            sendMessage(conduit, outMessage, synchronous);
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
