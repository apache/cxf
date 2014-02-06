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
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.jms.util.JMSUtil;
import org.apache.cxf.transport.jms.util.ResourceCloser;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JMSConduitTest extends AbstractJMSTester {

    static final Logger LOG = LogUtils.getL7dLogger(JMSConduitTest.class);

    @BeforeClass
    public static void createAndStartBroker() throws Exception {
        startBroker(new JMSBrokerSetup("tcp://localhost:" + JMS_PORT));
    }

    @Test
    public void testGetConfiguration() throws Exception {
        // setup the new bus to get the configuration file
        SpringBusFactory bf = new SpringBusFactory();
        BusFactory.setDefaultBus(null);
        bus = bf.createBus("/jms_test_config.xml");
        BusFactory.setDefaultBus(bus);
        EndpointInfo ei = setupServiceInfo("http://cxf.apache.org/jms_conf_test", "/wsdl/others/jms_test_no_addr.wsdl",
                         "HelloWorldQueueBinMsgService", "HelloWorldQueueBinMsgPort");
        JMSConduit conduit = setupJMSConduit(ei, false);
        assertEquals("Can't get the right ClientReceiveTimeout", 500L, conduit.getJmsConfig()
            .getReceiveTimeout().longValue());
        bus.shutdown(false);
        BusFactory.setDefaultBus(null);
        conduit.close();
    }

    @Test
    public void testPrepareSend() throws Exception {
        EndpointInfo ei = setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HelloWorldService", "HelloWorldPort");

        JMSConduit conduit = setupJMSConduit(ei, false);
        Message message = new MessageImpl();
        conduit.prepare(message);
        OutputStream os = message.getContent(OutputStream.class);
        Writer writer = message.getContent(Writer.class);
        assertTrue("The OutputStream and Writer should not both be null ", os != null || writer != null);
    }

    /**
     * Sends several messages and verifies the results. The service sends the message to itself. So it should
     * always receive the result
     * 
     * @throws Exception
     */
    @Test
    public void testTimeoutOnReceive() throws Exception {
        EndpointInfo ei = setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HelloWorldServiceLoop", "HelloWorldPortLoop");

        JMSConduit conduit = setupJMSConduit(ei, true);
        // TODO IF the system is extremely fast. The message could still get through
        conduit.getJmsConfig().setReceiveTimeout(Long.valueOf(1));
        Message message = new MessageImpl();
        try {
            sendoutMessage(conduit, message, false);
            verifyReceivedMessage(message);
            fail("Expected a timeout here");
        } catch (RuntimeException e) {
            LOG.info("Received exception. This is expected");
        } finally {
            conduit.close();
        }
    }

    private void verifyReceivedMessage(Message message) throws InterruptedException {
        while (inMessage == null) {
            //the send has completed, but the response might not be back yet.
            //wait for it.
            synchronized (this) {
                wait(10);
            }
        }
        ByteArrayInputStream bis = (ByteArrayInputStream)inMessage.getContent(InputStream.class);
        Assert.assertNotNull("The received message input stream should not be null", bis);
        byte bytes[] = new byte[bis.available()];
        try {
            bis.read(bytes);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String response = IOUtils.newStringFromBytes(bytes);
        assertEquals("The response data should be equal", "HelloWorld", response);

        JMSMessageHeadersType inHeader = (JMSMessageHeadersType)inMessage
            .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);

        assertTrue("The inMessage JMS Header should not be null", inHeader != null);

    }

    @Test
    public void testJMSMessageMarshal() throws IOException, JMSException {
        String testMsg = "Test Message";
        final byte[] testBytes = testMsg.getBytes(Charset.defaultCharset().name()); // TODO encoding
        JMSConfiguration jmsConfig = new JMSConfiguration();
        jmsConfig.setConnectionFactory(new ActiveMQConnectionFactory("vm://tesstMarshal?broker.persistent=false"));
        
        ResourceCloser closer = new ResourceCloser();
        try {
            Session session = JMSFactory.createJmsSessionFactory(jmsConfig, closer).createSession();
            javax.jms.Message jmsMessage = 
                JMSUtil.createAndSetPayload(testBytes, session, JMSConstants.BYTE_MESSAGE_TYPE);
            assertTrue("Message should have been of type BytesMessage ", jmsMessage instanceof BytesMessage);
        } finally {
            closer.close();
        }
        
    }

}
