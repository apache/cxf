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

import javax.jms.BytesMessage;

import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.BeforeClass;
import org.junit.Test;


public class JMSConduitTest extends AbstractJMSTester {
     
    
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
        setupServiceInfo("http://cxf.apache.org/jms_conf_test",
                         "/wsdl/others/jms_test_no_addr.wsdl",
                         "HelloWorldQueueBinMsgService",
                         "HelloWorldQueueBinMsgPort");
        JMSConduit conduit = setupJMSConduit(false, false);
        assertEquals("Can't get the right ClientReceiveTimeout",
                     500L,
                     conduit.getClientConfig().getClientReceiveTimeout());
        assertEquals("Can't get the right SessionPoolConfig's LowWaterMark",
                     10,
                     conduit.getSessionPool().getLowWaterMark());
        assertEquals("Can't get the right AddressPolicy's ConnectionPassword",
                     "testPassword",
                     conduit.getJMSAddress().getConnectionPassword());
        bus.shutdown(false);
        BusFactory.setDefaultBus(null);
        
    }
    
    @Test
    public void testPrepareSend() throws Exception {
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", 
                         "/wsdl/jms_test.wsdl", 
                         "HelloWorldService", 
                         "HelloWorldPort");

        JMSConduit conduit = setupJMSConduit(false, false);
        Message message = new MessageImpl();
        try {
            conduit.prepare(message);
        } catch (Exception ex) {
            ex.printStackTrace();            
        }
        verifySentMessage(false, message);        
    }
    
    public void verifySentMessage(boolean send, Message message) {
        PooledSession pooledSession = (PooledSession)message.get(JMSConstants.JMS_POOLEDSESSION);
        OutputStream os = message.getContent(OutputStream.class);
        assertTrue("pooled Session should not be null ", pooledSession != null);
        assertTrue("OutputStream should not be null", os != null);
        
    }
    
    @Test
    public void testSendOut() throws Exception {
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", 
                         "/wsdl/jms_test.wsdl", 
                         "HelloWorldServiceLoop", 
                         "HelloWorldPortLoop");

        JMSConduit conduit = setupJMSConduit(true, false); 
        Message message = new MessageImpl();
        // set the isOneWay to false
        sendoutMessage(conduit, message, false);        
        verifyReceivedMessage(message);        
    }
    
    public void verifyReceivedMessage(Message message)  {
        ByteArrayInputStream bis = 
            (ByteArrayInputStream) inMessage.getContent(InputStream.class);
        byte bytes[] = new byte[bis.available()];
        try {
            bis.read(bytes);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String reponse = new String(bytes);
        assertEquals("The reponse date should be equals", reponse, "HelloWorld");
                
        JMSMessageHeadersType inHeader =
            (JMSMessageHeadersType)inMessage.get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS); 
        
        assertTrue("The inMessage JMS Header should not be null", inHeader != null);
        
               
    }
    
    @Test
    public void testJMSMessageMarshal() throws Exception {
        setupServiceInfo("http://cxf.apache.org/hello_world_jms", 
                         "/wsdl/jms_test.wsdl", 
                         "HelloWorldServiceLoop", 
                         "HelloWorldPortLoop");

        String testMsg = "Test Message"; 
        JMSConduit conduit = setupJMSConduit(true, false); 
        Message msg = new MessageImpl();
        conduit.prepare(msg);
        PooledSession sess = conduit.base.sessionFactory.get(true);
        byte [] b = testMsg.getBytes();
        javax.jms.Message message = conduit.base.marshal(b, 
                                                         sess.session(), 
                                                         null, JMSConstants.BYTE_MESSAGE_TYPE);
        
        assertTrue("Message should have been of type BytesMessage ", 
                   message instanceof BytesMessage);
//        byte[] returnBytes = new byte[(int)((BytesMessage) message).getBodyLength()];
//        ((BytesMessage) message).readBytes(returnBytes);
//        assertTrue("Message marshalled was incorrect", 
//                   testMsg.equals(new String(returnBytes)));
    }
    
}
