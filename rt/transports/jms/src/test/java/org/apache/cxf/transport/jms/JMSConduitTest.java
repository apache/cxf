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
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
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
        startBroker(new JMSBrokerSetup("tcp://localhost:" + JMS_PORT + "?persistent=false"));
    }

    @Test
    public void testGetConfiguration() throws Exception {
        // setup the new bus to get the configuration file
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus("/jms_test_config.xml");
        EndpointInfo ei = setupServiceInfo("http://cxf.apache.org/jms_conf_test", "/wsdl/others/jms_test_no_addr.wsdl",
                         "HelloWorldQueueBinMsgService", "HelloWorldQueueBinMsgPort");
        JMSConduit conduit = setupJMSConduit(ei, false);
        assertEquals("Can't get the right ClientReceiveTimeout", 500L, conduit.getJmsConfig()
            .getReceiveTimeout().longValue());
        conduit.close();
        bus.shutdown(false);
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
            sendMessageSync(conduit, message);
            fail("Expected a timeout here");
        } catch (RuntimeException e) {
            Assert.assertTrue("Incorrect exception", 
                              e.getMessage().startsWith("Timeout receiving message with correlationId"));
        } finally {
            conduit.close();
        }
    }

    @Test
    public void testJMSMessageMarshal() throws IOException, JMSException {
        String testMsg = "Test Message";
        final byte[] testBytes = testMsg.getBytes(Charset.defaultCharset().name()); // TODO encoding
        JMSConfiguration jmsConfig = new JMSConfiguration();
        jmsConfig.setConnectionFactory(new ActiveMQConnectionFactory("vm://tesstMarshal?broker.persistent=false"));
        
        ResourceCloser closer = new ResourceCloser();
        try {
            Connection connection = JMSFactory.createConnection(jmsConfig);
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            javax.jms.Message jmsMessage = 
                JMSUtil.createAndSetPayload(testBytes, session, JMSConstants.BYTE_MESSAGE_TYPE);
            assertTrue("Message should have been of type BytesMessage ", jmsMessage instanceof BytesMessage);
        } finally {
            closer.close();
        }
        
    }

}
