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
import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;

public abstract class AbstractJMSTester extends Assert {
    protected static final String MESSAGE_CONTENT = "HelloWorld";

    private static JMSBrokerSetup broker;

    protected Bus bus;
    protected EndpointInfo endpointInfo;
    protected EndpointReferenceType target;
    protected MessageObserver observer;
    protected Message inMessage;

    public static void startBroker(JMSBrokerSetup b) throws Exception {
        assertNotNull(b);
        broker = b;
        broker.start();
    }

    @AfterClass
    public static void stopBroker() throws Exception {
        broker.stop();
        broker = null;
    }

    @Before
    public void setUp() {
        BusFactory bf = BusFactory.newInstance();
        bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
    }

    @After
    public void tearDown() {
        bus.shutdown(true);
        if (System.getProperty("cxf.config.file") != null) {
            System.clearProperty("cxf.config.file");
        }
    }

    protected void setupServiceInfo(String ns, String wsdl, String serviceName, String portName) {
        URL wsdlUrl = getClass().getResource(wsdl);
        assertNotNull(wsdlUrl);
        WSDLServiceFactory factory = new WSDLServiceFactory(bus, wsdlUrl, new QName(ns, serviceName));

        Service service = factory.create();
        endpointInfo = service.getEndpointInfo(new QName(ns, portName));

    }

    protected void sendoutMessage(Conduit conduit, Message message, Boolean isOneWay) throws IOException {

        Exchange exchange = new ExchangeImpl();
        exchange.setOneWay(isOneWay);
        exchange.setSynchronous(true);
        message.setExchange(exchange);
        exchange.setOutMessage(message);
        try {
            conduit.prepare(message);
        } catch (IOException ex) {
            assertFalse("JMSConduit can't perpare to send out message", false);
            ex.printStackTrace();
        }
        OutputStream os = message.getContent(OutputStream.class);
        assertTrue("The OutputStream should not be null ", os != null);
        os.write(MESSAGE_CONTENT.getBytes()); // TODO encoding
        os.close();
    }

    protected JMSConduit setupJMSConduit(boolean send, boolean decoupled) {
        if (decoupled) {
            // setup the reference type
        } else {
            target = EasyMock.createMock(EndpointReferenceType.class);
        }
        
        JMSConfiguration jmsConfig = new JMSOldConfigHolder()
            .createJMSConfigurationFromEndpointInfo(bus, endpointInfo, true);
        JMSConduit jmsConduit = new JMSConduit(endpointInfo, target, jmsConfig);
        if (send) {
            // setMessageObserver
            observer = new MessageObserver() {
                public void onMessage(Message m) {
                    inMessage = m;
                }
            };
            jmsConduit.setMessageObserver(observer);
        }

        return jmsConduit;
    }

}
