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
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.MultiplexDestination;
import org.junit.Ignore;
import org.junit.Test;

public class JMSDestinationTest extends AbstractJMSTester {

    @Test
    public void testGetConfigurationFromWSDL() throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldQueueBinMsgService", "HelloWorldQueueBinMsgPort");
        JMSDestination destination = setupJMSDestination(ei);
        assertEquals("Can't get the right AddressPolicy's Destination",
                     "test.jmstransport.binary", 
                     destination.getJmsConfig().getTargetDestination());
        destination.shutdown();
    }

    @Test
    public void testDurableSubscriber() throws Exception {
        destMessage = null;
        EndpointInfo ei = setupServiceInfo("HelloWorldPubSubService", "HelloWorldPubSubPort");
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage);
        JMSDestination destination = setupJMSDestination(ei);
        destination.setMessageObserver(createMessageObserver());
        // The JMSBroker (ActiveMQ 5.x) need to take some time to setup the DurableSubscriber
        Thread.sleep(500);
        sendOneWayMessage(conduit, outMessage);
        waitForReceiveDestMessage();

        assertTrue("The destiantion should have got the message ", destMessage != null);
        verifyReceivedMessage(destMessage);
        verifyHeaders(destMessage, outMessage);
        conduit.close();
        destination.shutdown();
    }

    @Test
    public void testOneWayDestination() throws Exception {
        EndpointInfo ei = setupServiceInfo("HWStaticReplyQBinMsgService", "HWStaticReplyQBinMsgPort");
        JMSDestination destination = setupJMSDestination(ei);
        destination.setMessageObserver(createMessageObserver());
        
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage);
        
        sendOneWayMessage(conduit, outMessage);
        // wait for the message to be get from the destination
        waitForReceiveDestMessage();
        // just verify the Destination inMessage
        assertTrue("The destiantion should have got the message ", destMessage != null);
        verifyReceivedMessage(destMessage);
        verifyHeaders(destMessage, outMessage);
        conduit.close();
        destination.shutdown();
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
                response = new String(buffer, 0, i);
            } catch (IOException e) {
                assertFalse("Read the Destination recieved Message error ", false);
                e.printStackTrace();
            }
        }
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
        assertEquals("The inMessage and outMessage JMS Header's JMSPriority should be equals", outHeader
            .getJMSPriority(), inHeader.getJMSPriority());
        assertEquals("The inMessage and outMessage JMS Header's JMSDeliveryMode should be equals", outHeader
                     .getJMSDeliveryMode(), inHeader.getJMSDeliveryMode());
        assertEquals("The inMessage and outMessage JMS Header's JMSType should be equals", outHeader
            .getJMSType(), inHeader.getJMSType());
    }

    @Test
    public void testRoundTripDestination() throws Exception {
        Message msg = testRoundTripDestination(true);
        SecurityContext securityContext = msg.get(SecurityContext.class);
        
        assertNotNull("SecurityContext should be set in message received by JMSDestination", securityContext);
        assertEquals("Principal in SecurityContext should be", "testUser", 
                securityContext.getUserPrincipal().getName());
    }
    
    @Test
    public void testRoundTripDestinationDoNotCreateSecurityContext() throws Exception {
        Message msg = testRoundTripDestination(false);
        SecurityContext securityContext = msg.get(SecurityContext.class);
        assertNull("SecurityContext should not be set in message received by JMSDestination", securityContext);
    }
    
    private Message testRoundTripDestination(boolean createSecurityContext) throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldService", "HelloWorldPort");
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        conduit.getJmsConfig().setCreateSecurityContext(createSecurityContext);
        
        final Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage, null);
        final JMSDestination destination = setupJMSDestination(ei);
        
        
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
        sendMessageSync(conduit, outMessage);
        // wait for the message to be got from the destination,
        // create the thread to handler the Destination incoming message

        waitForReceiveInMessage();
        verifyReceivedMessage(inMessage);
        // wait for a while for the jms session recycling

        inMessage = null;
        // Send a second message to check for an issue
        // Where the session was closed the second time
        sendMessageSync(conduit, outMessage);
        waitForReceiveInMessage();
        verifyReceivedMessage(inMessage);

        Thread.sleep(1000);
        conduit.close();
        destination.shutdown();
        
        return inMessage;
    }

    @Test
    public void testProperty() throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldService", "HelloWorldPort");
        final String customPropertyName = "THIS_PROPERTY_WILL_NOT_BE_AUTO_COPIED";

        // set up the conduit send to be true
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        final Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage, null);

        JMSPropertyType excludeProp = new JMSPropertyType();
        excludeProp.setName(customPropertyName);
        excludeProp.setValue(customPropertyName);

        JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
        headers.getProperty().add(excludeProp);

        final JMSDestination destination = setupJMSDestination(ei);

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
                    backConduit = destination.getBackChannel(m);
                    // wait for the message to be got from the conduit
                    Message replyMessage = new MessageImpl();
                    // copy the message encoding
                    replyMessage.put(Message.ENCODING, m.get(Message.ENCODING));
                    sendOneWayMessage(backConduit, replyMessage);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        destination.setMessageObserver(observer);
        sendMessageSync(conduit, outMessage);
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
        EndpointInfo ei = setupServiceInfo("HelloWorldService", "HelloWorldPort");
        final JMSDestination destination = setupJMSDestination(ei);
        destination.setMessageObserver(createMessageObserver());
        assertTrue("is multiplex", destination instanceof MultiplexDestination);
        destination.shutdown();
    }
    
    @Test
    public void testSecurityContext() throws Exception {
        SecurityContext ctx = testSecurityContext(true);
        assertNotNull("SecurityContext should be set in message received by JMSDestination", ctx);
        assertEquals("Principal in SecurityContext should be", "testUser", 
                ctx.getUserPrincipal().getName());
    }
    
    @Test
    public void testDoNotCreateSecurityContext() throws Exception {
        SecurityContext ctx = testSecurityContext(false);
        assertNull("SecurityContext should not be set in message received by JMSDestination", ctx);
    }
    
    private SecurityContext testSecurityContext(boolean createSecurityContext) throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldService", "HelloWorldPort");
        final JMSDestination destination = setupJMSDestination(ei);
        destination.getJmsConfig().setCreateSecurityContext(createSecurityContext);
        destination.setMessageObserver(createMessageObserver());
        // set up the conduit send to be true
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        final Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage, null);
        sendOneWayMessage(conduit, outMessage);
        waitForReceiveDestMessage();
        SecurityContext securityContext = destMessage.get(SecurityContext.class);
        
        conduit.close();
        destination.shutdown();
        
        return securityContext;
    }

    
    @Test
    @Ignore
    public void testOneWayReplyToSetUnset() throws Exception {
        /* 1. Test that replyTo destination set in WSDL is NOT used 
         * in spec compliant mode */
        
        destMessage = null;
        EndpointInfo ei = setupServiceInfo(
                         "HWStaticReplyQBinMsgService", "HWStaticReplyQBinMsgPort");
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        Message outMessage = new MessageImpl();
        setupMessageHeader(outMessage);
        JMSDestination destination = setupJMSDestination(ei);
        destination.setMessageObserver(createMessageObserver());
        sendOneWayMessage(conduit, outMessage);
        waitForReceiveDestMessage();
        // just verify the Destination inMessage
        assertTrue("The destination should have got the message ", destMessage != null);
        verifyReplyToNotSet(destMessage);
        destMessage = null;
        
        /* 2. Test that replyTo destination set in WSDL IS used 
         * in spec non-compliant mode */
        
        sendOneWayMessage(conduit, outMessage);
        waitForReceiveDestMessage();
        assertTrue("The destination should have got the message ", destMessage != null);
        String exName = getQueueName(conduit.getJmsConfig().getReplyDestination());
        verifyReplyToSet(destMessage, Queue.class, exName);
        destMessage = null;
        
        /* 3. Test that replyTo destination provided via invocation context 
         * overrides the value set in WSDL and IS used in spec non-compliant mode */
        
        String contextReplyTo = conduit.getJmsConfig().getReplyDestination() + ".context";
        exName += ".context";
        setupMessageHeader(outMessage, "cidValue", contextReplyTo);
        sendOneWayMessage(conduit, outMessage);
        waitForReceiveDestMessage();
        assertTrue("The destiantion should have got the message ", destMessage != null);
        verifyReplyToSet(destMessage, Queue.class, exName);
        destMessage = null;
        
        /* 4. Test that replyTo destination provided via invocation context 
         * and the value set in WSDL are NOT used in spec non-compliant mode 
         * when JMSConstants.JMS_SET_REPLY_TO == false */

        setupMessageHeader(outMessage);
        outMessage.put(JMSConstants.JMS_SET_REPLY_TO, Boolean.FALSE);
        sendOneWayMessage(conduit, outMessage);
        waitForReceiveDestMessage();
        assertTrue("The destiantion should have got the message ", destMessage != null);
        verifyReplyToNotSet(destMessage);
        destMessage = null;
        
        /* 5. Test that replyTo destination set in WSDL IS used in spec non-compliant 
         * mode when JMSConstants.JMS_SET_REPLY_TO == true */

        setupMessageHeader(outMessage);
        outMessage.put(JMSConstants.JMS_SET_REPLY_TO, Boolean.TRUE);
        sendOneWayMessage(conduit, outMessage);
        waitForReceiveDestMessage();
        assertTrue("The destiantion should have got the message ", destMessage != null);
        exName = getQueueName(conduit.getJmsConfig().getReplyDestination());
        verifyReplyToSet(destMessage, Queue.class, exName);
        destMessage = null;
        
        conduit.close();
        destination.shutdown();
    }

    private String getQueueName(String exName) {
        if (exName == null) {
            return null;
        }
        return (exName.indexOf('/') != -1 && exName.indexOf('/') < exName.length()) 
            ? exName.substring(exName.indexOf('/') + 1) : exName;
    }

    
    protected void verifyReplyToNotSet(Message cxfMsg) {
        javax.jms.Message jmsMsg = 
            javax.jms.Message.class.cast(cxfMsg.get(JMSConstants.JMS_REQUEST_MESSAGE));
        assertNotNull("JMS Messsage must be null", jmsMsg);
    }
    
    private String getDestinationName(Destination dest) throws JMSException {
        if (dest instanceof Queue) {
            return ((Queue)dest).getQueueName();
        } else {
            return ((Topic)dest).getTopicName();
        }
    }
    
    protected void verifyReplyToSet(Message cxfMsg, 
                                    Class<? extends Destination> type, 
                                    String expectedName) throws Exception {
        javax.jms.Message jmsMsg = 
            javax.jms.Message.class.cast(cxfMsg.get(JMSConstants.JMS_REQUEST_MESSAGE));
        assertNotNull("JMS Messsage must not be null", jmsMsg);
        assertNotNull("JMS Messsage's replyTo must not be null", jmsMsg.getJMSReplyTo());
        assertTrue("JMS Messsage's replyTo type must be of type " + type.getName(), 
                   type.isAssignableFrom(jmsMsg.getJMSReplyTo().getClass()));
        String receivedName = getDestinationName(jmsMsg.getJMSReplyTo());
        assertTrue("JMS Messsage's replyTo must be named " + expectedName + " but was " + receivedName,
                   expectedName == receivedName || receivedName.equals(expectedName));
        
    }

}
