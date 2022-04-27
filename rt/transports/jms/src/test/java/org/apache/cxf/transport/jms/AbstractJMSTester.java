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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.xml.namespace.QName;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.wsdl11.WSDLServiceFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.apache.activemq.artemis.spi.core.security.jaas.InVMLoginModule;

public abstract class AbstractJMSTester {
    protected static final String WSDL = "/jms_test.wsdl";
    protected static final String SERVICE_NS = "http://cxf.apache.org/hello_world_jms";
    protected static final int MAX_RECEIVE_TIME = 10;
    protected static Bus bus;
    protected static ActiveMQConnectionFactory cf1;
    protected static ConnectionFactory cf;
    protected static EmbeddedActiveMQ broker;
    private static final String MESSAGE_CONTENT = "HelloWorld";

    protected enum ExchangePattern { oneway, requestReply };

    private final AtomicReference<Message> inMessage = new AtomicReference<>();
    private final AtomicReference<Message> destMessage = new AtomicReference<>();

    @BeforeClass
    public static void startServices() throws Exception {
        final String brokerUri = "tcp://localhost:" + TestUtil.getNewPortNumber(AbstractJMSTester.class);
        final Configuration config = new ConfigurationImpl();
        config.setPersistenceEnabled(false);
        config.setJMXManagementEnabled(false);
        config.setSecurityEnabled(true);
        config.addAcceptorConfiguration("tcp", brokerUri);
        config.setPopulateValidatedUser(true);
        config.putSecurityRoles("#", Collections.singleton(
                new Role("guest", true, true, true, true, true, true, true, true, true, true)));
        config.setAddressQueueScanPeriod(10);
        AddressSettings addressSettings = new AddressSettings();
        addressSettings.setAutoCreateAddresses(true);
        addressSettings.setAutoCreateQueues(true);
        addressSettings.setAutoDeleteQueues(false);
        addressSettings.setAutoDeleteAddresses(false);
        config.setAddressesSettings(Collections.singletonMap("#", new AddressSettings()));
        broker = new EmbeddedActiveMQ();
        broker.setConfiguration(config);
        SecurityConfiguration securityConfig = new SecurityConfiguration();
        securityConfig.addUser("guest", "guest");
        securityConfig.addUser("testUser", "testPassword");
        securityConfig.addRole("testUser", "guest");
        securityConfig.addRole("guest", "guest");
        securityConfig.setDefaultUser("guest");
        ActiveMQJAASSecurityManager securityManager = 
                new ActiveMQJAASSecurityManager(InVMLoginModule.class.getName(), securityConfig);
        broker.setSecurityManager(securityManager);
        broker.start();
        bus = BusFactory.getDefaultBus();
        cf1 = new ActiveMQConnectionFactory(brokerUri);
        cf1.setUser("guest");
        cf1.setPassword("guest");
        cf1.setDeserializationWhiteList("org.apache.cxf.security");
        cf = cf1;
    }

    @AfterClass
    public static void stopServices() throws Exception {
        bus.shutdown(false);
        broker.stop();
    }

    protected static EndpointInfo setupServiceInfo(String serviceName, String portName) {
        return setupServiceInfo(SERVICE_NS, WSDL, serviceName, portName);
    }

    protected static EndpointInfo setupServiceInfo(String ns, String wsdl, String serviceName, String portName) {
        URL wsdlUrl = AbstractJMSTester.class.getResource(wsdl);
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
//                Exchange exchange = new ExchangeImpl();
//                exchange.setInMessage(m);
//                m.setExchange(exchange);
                destMessage.set(m);
                synchronized (destMessage) {
                    destMessage.notifyAll();
                }
            }
        };
    }

    protected static void sendMessageAsync(Conduit conduit, Message message) throws IOException {
        sendoutMessage(conduit, message, false, false);
    }

    protected static void sendMessageSync(Conduit conduit, Message message) throws IOException {
        sendoutMessage(conduit, message, false, true);
    }

    protected static void sendMessage(Conduit conduit, Message message, boolean synchronous) throws IOException {
        sendoutMessage(conduit, message, false, synchronous);
    }

    protected static void sendOneWayMessage(Conduit conduit, Message message) throws IOException {
        sendoutMessage(conduit, message, true, true);
    }

    private static void sendoutMessage(Conduit conduit,
                                  Message message,
                                  boolean isOneWay,
                                  boolean synchronous) throws IOException {
        final Exchange exchange = new ExchangeImpl();
        exchange.setOneWay(isOneWay);
        exchange.setSynchronous(synchronous);
        message.setExchange(exchange);
        exchange.setOutMessage(message);
        conduit.prepare(message);
        try (OutputStream os = message.getContent(OutputStream.class)) {
            if (os != null) {
                os.write(MESSAGE_CONTENT.getBytes()); // TODO encoding
                return;
            }
        }
        try (Writer writer = message.getContent(Writer.class)) {
            if (writer != null) {
                writer.write(MESSAGE_CONTENT);
                return;
            }
        }
        fail("The OutputStream and Writer should not both be null");
    }

    protected static JMSConduit setupJMSConduit(EndpointInfo ei) throws IOException {
        JMSConfiguration jmsConfig = JMSConfigFactory.createFromEndpointInfo(bus, ei, null);
        jmsConfig.setConnectionFactory(cf);
        return new JMSConduit(null, jmsConfig, bus);
    }


    protected JMSConduit setupJMSConduitWithObserver(EndpointInfo ei) throws IOException {
        JMSConduit jmsConduit = setupJMSConduit(ei);
        MessageObserver observer = new MessageObserver() {
            public void onMessage(Message m) {
                inMessage.set(m);
                synchronized (inMessage) {
                    inMessage.notifyAll();
                }
            }
        };
        jmsConduit.setMessageObserver(observer);
        return jmsConduit;
    }

    protected JMSDestination setupJMSDestination(EndpointInfo ei, 
            Function<ConnectionFactory, ConnectionFactory> wrapper) throws IOException {
        JMSConfiguration jmsConfig = JMSConfigFactory.createFromEndpointInfo(bus, ei, null);
        jmsConfig.setConnectionFactory(wrapper.apply(cf));
        return new JMSDestination(bus, ei, jmsConfig);
    }
    
    protected JMSDestination setupJMSDestination(EndpointInfo ei) throws IOException {
        JMSConfiguration jmsConfig = JMSConfigFactory.createFromEndpointInfo(bus, ei, null);
        jmsConfig.setConnectionFactory(cf);
        return new JMSDestination(bus, ei, jmsConfig);
    }

    protected static Message createMessage() {
        return createMessage(null);
    }

    protected static Message createMessage(String correlationId) {
        Message outMessage = new MessageImpl();
        JMSMessageHeadersType header = new JMSMessageHeadersType();
        header.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
        header.setJMSPriority(1);
        header.setTimeToLive(1000L);
        outMessage.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, header);
        outMessage.put(Message.ENCODING, "US-ASCII");
        return outMessage;
    }

    protected static void verifyReceivedMessage(Message message) {
        String response = "<not found>";
        InputStream bis = message.getContent(InputStream.class);
        if (bis != null) {
            try {
                byte[] bytes = new byte[bis.available()];
                bis.read(bytes);
                response = IOUtils.newStringFromBytes(bytes);
            } catch (IOException ex) {
                fail("Read the Destination recieved Message error: " + ex.getMessage());
            }
        } else {
            Reader reader = message.getContent(Reader.class);
            char[] buffer = new char[5000];
            try {
                int i = reader.read(buffer);
                response = new String(buffer, 0, i);
            } catch (IOException e) {
                fail("Read the Destination recieved Message error: " + e.getMessage());
            }
        }
        assertEquals("The response content should be equal", MESSAGE_CONTENT, response);
    }

    protected static void verifyHeaders(Message msgIn, Message msgOut) {
        JMSMessageHeadersType outHeader = (JMSMessageHeadersType)msgOut
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);

        JMSMessageHeadersType inHeader = (JMSMessageHeadersType)msgIn
            .get(JMSConstants.JMS_SERVER_REQUEST_HEADERS);

        verifyJmsHeaderEquality(outHeader, inHeader);

    }

    protected static void verifyJmsHeaderEquality(JMSMessageHeadersType outHeader, JMSMessageHeadersType inHeader) {
        assertEquals("The inMessage and outMessage JMS Header's JMSPriority should be equals", outHeader
            .getJMSPriority(), inHeader.getJMSPriority());
        assertEquals("The inMessage and outMessage JMS Header's JMSDeliveryMode should be equals", outHeader
                     .getJMSDeliveryMode(), inHeader.getJMSDeliveryMode());
        assertEquals("The inMessage and outMessage JMS Header's JMSType should be equals", outHeader
            .getJMSType(), inHeader.getJMSType());
    }


    protected Message waitForReceiveInMessage() throws InterruptedException {
        if (null == inMessage.get()) {
            synchronized (inMessage) {
                inMessage.wait(MAX_RECEIVE_TIME * 1000L);
            }
            assertNotNull("Can't receive the Conduit Message in " + MAX_RECEIVE_TIME + " seconds", inMessage.get());
        }
        return inMessage.getAndSet(null);
    }

    protected Message waitForReceiveDestMessage() throws InterruptedException {
        if (null == destMessage.get()) {
            synchronized (destMessage) {
                destMessage.wait(MAX_RECEIVE_TIME * 1000L);
            }
            assertNotNull("Can't receive the Destination message in " + MAX_RECEIVE_TIME + " seconds",
                    destMessage.get());
        }
        return destMessage.getAndSet(null);
    }

}