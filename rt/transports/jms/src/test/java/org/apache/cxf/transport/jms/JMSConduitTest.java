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
import java.nio.charset.Charset;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;

public class JMSConduitTest extends AbstractJMSTester {

    static final Logger LOG = LogUtils.getL7dLogger(JMSConduitTest.class);

    @BeforeClass
    public static void createAndStartBroker() throws Exception {
        startBroker(new JMSBrokerSetup("tcp://localhost:61500"));
    }

    @Test
    public void testGetConfiguration() throws Exception {
        // setup the new bus to get the configuration file
        SpringBusFactory bf = new SpringBusFactory();
        BusFactory.setDefaultBus(null);
        bus = bf.createBus("/jms_test_config.xml");
        BusFactory.setDefaultBus(bus);
        setupServiceInfo("http://cxf.apache.org/jms_conf_test", "/wsdl/others/jms_test_no_addr.wsdl",
                         "HelloWorldQueueBinMsgService", "HelloWorldQueueBinMsgPort");
        JMSConduit conduit = setupJMSConduit(false, false);
        assertEquals("Can't get the right ClientReceiveTimeout", 500L, conduit.getJmsConfig()
            .getReceiveTimeout().longValue());
        bus.shutdown(false);
        BusFactory.setDefaultBus(null);
        conduit.close();
    }

    @Test
    public void testPrepareSend() throws Exception {
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HelloWorldService", "HelloWorldPort");

        JMSConduit conduit = setupJMSConduit(false, false);
        Message message = new MessageImpl();
        try {
            conduit.prepare(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        verifySentMessage(false, message);
    }

    private void verifySentMessage(boolean send, Message message) {
        OutputStream os = message.getContent(OutputStream.class);
        assertTrue("OutputStream should not be null", os != null);
    }

    @Test
    public void testSendOut() throws Exception {
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HelloWorldServiceLoop", "HelloWorldPortLoop");
        JMSConduit conduit = setupJMSConduit(true, false);
        conduit.getJmsConfig().setReceiveTimeout(Long.valueOf(10000));

        try {
            for (int c = 0; c < 10; c++) {
                LOG.info("Sending message " + c);
                inMessage = null;
                Message message = new MessageImpl();
                sendoutMessage(conduit, message, false);
                verifyReceivedMessage(message);
            }
        } finally {
            conduit.close();
        }
    }

    /**
     * Sends several messages and verfies the results. The service sends the message to itself. So it should
     * always receive the result
     * 
     * @throws Exception
     */
    @Test
    public void testTimeoutOnReceive() throws Exception {
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HelloWorldServiceLoop", "HelloWorldPortLoop");

        JMSConduit conduit = setupJMSConduit(true, false);
        // TODO IF the system is extremely fast. The message could still get through
        conduit.getJmsConfig().setReceiveTimeout(Long.valueOf(1));
        Message message = new MessageImpl();
        try {
            sendoutMessage(conduit, message, false);
            verifyReceivedMessage(message);
            throw new RuntimeException("Expected a timeout here");
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
    public void testJMSMessageMarshal() throws Exception {
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                         "HelloWorldServiceLoop", "HelloWorldPortLoop");

        String testMsg = "Test Message";
        JMSConduit conduit = setupJMSConduit(true, false);
        Message msg = new MessageImpl();
        conduit.prepare(msg);
        final byte[] testBytes = testMsg.getBytes(Charset.defaultCharset().name()); // TODO encoding
        JMSConfiguration jmsConfig = conduit.getJmsConfig();
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(jmsConfig.getOrCreateWrappedConnectionFactory());
        SessionCallback sc = new SessionCallback() {
            public Object doInJms(Session session) throws JMSException {
                return JMSUtils.createAndSetPayload(testBytes, session, JMSConstants.BYTE_MESSAGE_TYPE);
            }
        };
        javax.jms.Message message = (javax.jms.Message)jmsTemplate.execute(sc);
        
        // The ibm jdk finalizes conduit (during most runs of this test) and
        // causes it to fail unless we reference the conduit here after the
        // jmsTemplate.execute() call.
        assertNotNull("Conduit is null", conduit);

        assertTrue("Message should have been of type BytesMessage ", message instanceof BytesMessage);
    }

}
