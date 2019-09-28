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

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;

import org.junit.Test;

public class RequestResponseTest extends AbstractJMSTester {

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

    private void sendAndReceiveMessages(EndpointInfo ei, boolean synchronous)
            throws IOException, InterruptedException {
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
                try {
                    Conduit backConduit = destination.getBackChannel(m);
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

            verifyReceivedMessage(waitForReceiveInMessage());
        } finally {
            conduit.close();
            destination.shutdown();
        }
    }

}
