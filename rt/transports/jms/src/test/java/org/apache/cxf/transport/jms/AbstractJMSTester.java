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
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;

import javax.jms.ConnectionFactory;
import javax.xml.namespace.QName;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

public abstract class AbstractJMSTester extends Assert {
    protected static final String WSDL = "/jms_test.wsdl";
    protected static final String SERVICE_NS = "http://cxf.apache.org/hello_world_jms";
    protected static final int MAX_RECEIVE_TIME = 10;
    protected static final String MESSAGE_CONTENT = "HelloWorld";
    protected static Bus bus;
    protected static ConnectionFactory cf;
    protected static BrokerService broker;

    protected enum ExchangePattern { oneway, requestReply };
    
    protected EndpointReferenceType target;
    protected Message inMessage;
    protected Message destMessage;

    @BeforeClass
    public static void startSerices() throws Exception {
        broker = new BrokerService();
        broker.setPersistent(false);
        broker.setPopulateJMSXUserID(true);
        broker.setUseAuthenticatedPrincipalForJMSXUserID(true);
        broker.setUseJmx(false);
        broker.setPersistenceAdapter(new MemoryPersistenceAdapter());
        String brokerUri = "tcp://localhost:" + TestUtil.getNewPortNumber(AbstractJMSTester.class);
        broker.addConnector(brokerUri);
        broker.start();
        bus = BusFactory.getDefaultBus();
        ActiveMQConnectionFactory cf1 = new ActiveMQConnectionFactory(brokerUri);
        cf = new PooledConnectionFactory(cf1);
    }

    @AfterClass
    public static void stopServices() throws Exception {
        bus.shutdown(false);
        broker.stop();
    }
    
    protected EndpointInfo setupServiceInfo(String serviceName, String portName) {
        return setupServiceInfo(SERVICE_NS, WSDL, serviceName, portName);
    }

    protected EndpointInfo setupServiceInfo(String ns, String wsdl, String serviceName, String portName) {
        URL wsdlUrl = getClass().getResource(wsdl);
        if (wsdlUrl == null) {
            throw new IllegalArgumentException("Wsdl file not found on class path " + wsdl);
        }
        WSDLServiceFactory factory = new WSDLServiceFactory(bus, wsdlUrl.toExternalForm(), 
                                                            new QName(ns, serviceName));

        Service service = factory.create();
        return service.getEndpointInfo(new QName(ns, portName));

    }
    

    protected MessageObserver createMessageObserver() {
        return new MessageObserver() {
            public void onMessage(Message m) {
                Exchange exchange = new ExchangeImpl();
                exchange.setInMessage(m);
                m.setExchange(exchange);
                destMessage = m;
            }
        };
    }
    
    protected void sendMessageAsync(Conduit conduit, Message message) throws IOException {
        sendoutMessage(conduit, message, false, false);
    }
    
    protected void sendMessageSync(Conduit conduit, Message message) throws IOException {
        sendoutMessage(conduit, message, false, true);
    }
    
    protected void sendMessage(Conduit conduit, Message message, boolean synchronous) throws IOException {
        sendoutMessage(conduit, message, false, synchronous);
    }
    
    protected void sendOneWayMessage(Conduit conduit, Message message) throws IOException {
        sendoutMessage(conduit, message, true, true);
    }
    
    private void sendoutMessage(Conduit conduit, 
                                  Message message, 
                                  boolean isOneWay, 
                                  boolean synchronous) throws IOException {

        Exchange exchange = new ExchangeImpl();
        exchange.setOneWay(isOneWay);
        exchange.setSynchronous(synchronous);
        message.setExchange(exchange);
        exchange.setOutMessage(message);
        conduit.prepare(message);
        OutputStream os = message.getContent(OutputStream.class);
        Writer writer = message.getContent(Writer.class);
        assertTrue("The OutputStream and Writer should not both be null ", os != null || writer != null);
        if (os != null) {
            os.write(MESSAGE_CONTENT.getBytes()); // TODO encoding
            os.close();
        } else {
            writer.write(MESSAGE_CONTENT);
            writer.close();
        }
    }

    protected JMSConduit setupJMSConduit(EndpointInfo ei) throws IOException {
        JMSConfiguration jmsConfig = JMSConfigFactory.createFromEndpointInfo(bus, ei, null);
        jmsConfig.setConnectionFactory(cf);
        return new JMSConduit(target, jmsConfig, bus);
    }
    
    protected JMSConduit setupJMSConduitWithObserver(EndpointInfo ei) throws IOException {
        JMSConduit jmsConduit = setupJMSConduit(ei);
        MessageObserver observer = new MessageObserver() {
            public void onMessage(Message m) {
                inMessage = m;
            }
        };
        jmsConduit.setMessageObserver(observer);
        return jmsConduit;
    }
    
    protected JMSDestination setupJMSDestination(EndpointInfo ei) throws IOException {
        JMSConfiguration jmsConfig = JMSConfigFactory.createFromEndpointInfo(bus, ei, null);
        jmsConfig.setConnectionFactory(cf);
        return new JMSDestination(bus, ei, jmsConfig);
    }

    protected String getContent(Message message) {
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
        return response;
    }

    protected void waitForReceiveInMessage() {
        int waitTime = 0;
        while (inMessage == null && waitTime < MAX_RECEIVE_TIME) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // do nothing here
            }
            waitTime++;
        }
        assertTrue("Can't receive the Conduit Message in " + MAX_RECEIVE_TIME + " seconds",
                   inMessage != null);
    }

    protected void waitForReceiveDestMessage() {
        int waitTime = 0;
        while (destMessage == null && waitTime < MAX_RECEIVE_TIME) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // do nothing here
            }
            waitTime++;
        }
        assertNotNull("Can't receive the Destination message in " + MAX_RECEIVE_TIME 
                   + " seconds", destMessage);
    }

}
