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
package org.apache.cxf.systest.jms;

import java.lang.Thread.State;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.activation.DataHandler;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import javax.xml.ws.Response;
import javax.xml.ws.soap.SOAPBinding;


import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.hello_world_jms.BadRecordLitFault;
import org.apache.cxf.hello_world_jms.HWByteMsgService;
import org.apache.cxf.hello_world_jms.HelloWorldMessageIDAsCorrelationIDAsyncService;
import org.apache.cxf.hello_world_jms.HelloWorldOneWayPort;
import org.apache.cxf.hello_world_jms.HelloWorldOneWayQueueService;
import org.apache.cxf.hello_world_jms.HelloWorldPortType;
import org.apache.cxf.hello_world_jms.HelloWorldPubSubPort;
import org.apache.cxf.hello_world_jms.HelloWorldPubSubService;
import org.apache.cxf.hello_world_jms.HelloWorldQueueDecoupledOneWaysService;
import org.apache.cxf.hello_world_jms.HelloWorldService;
import org.apache.cxf.hello_world_jms.HelloWorldServiceAppCorrelationID;
import org.apache.cxf.hello_world_jms.HelloWorldServiceAppCorrelationIDNoPrefix;
import org.apache.cxf.hello_world_jms.HelloWorldServiceAppCorrelationIDStaticPrefix;
import org.apache.cxf.hello_world_jms.HelloWorldServiceRuntimeCorrelationIDDynamicPrefix;
import org.apache.cxf.hello_world_jms.HelloWorldServiceRuntimeCorrelationIDStaticPrefix;
import org.apache.cxf.hello_world_jms.NoSuchCodeLitFault;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jms_mtom.JMSMTOMPortType;
import org.apache.cxf.jms_mtom.JMSMTOMService;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.transport.jms.AddressType;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSFactory;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;
import org.apache.cxf.transport.jms.JMSNamingPropertyType;
import org.apache.cxf.transport.jms.JMSOldConfigHolder;
import org.apache.cxf.transport.jms.JMSPropertyType;
import org.apache.cxf.transport.jms.JNDIConfiguration;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.PingMeFault;
import org.apache.hello_world_doc_lit.SOAPService2;
import org.apache.hello_world_doc_lit.SOAPService7;
import org.apache.hello_world_doc_lit.SOAPService8;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jndi.JndiTemplate;

public class JMSClientServerTest extends AbstractBusClientServerTestBase {
    static final String JMS_PORT = EmbeddedJMSBrokerLauncher.PORT;
    static final String PORT = Server.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        Map<String, String> props = new HashMap<String, String>();                
        if (System.getProperty("activemq.store.dir") != null) {
            props.put("activemq.store.dir", System.getProperty("activemq.store.dir"));
        }
        props.put("java.util.logging.config.file", 
                  System.getProperty("java.util.logging.config.file"));
        
        assertTrue("server did not launch correctly", 
                   launchServer(EmbeddedJMSBrokerLauncher.class, props, null));

        assertTrue("server did not launch correctly", 
                   launchServer(Server.class));
        
    }
    
    public URL getWSDLURL(String s) throws Exception {
        URL u = getClass().getResource(s);
        EmbeddedJMSBrokerLauncher.updateWsdlExtensors(getBus(), u.toString());
        return u;
    }
    public QName getServiceName(QName q) {
        return q;
    }
    public QName getPortName(QName q) {
        return q;
    }
    
    @Test
    public void testDocBasicConnection() throws Exception {
        QName serviceName = getServiceName(new QName("http://apache.org/hello_world_doc_lit", 
                                 "SOAPService2"));
        QName portName = getPortName(new QName("http://apache.org/hello_world_doc_lit", "SoapPort2"));
        URL wsdl = getWSDLURL("/wsdl/hello_world_doc_lit.wsdl");
        assertNotNull(wsdl);

        SOAPService2 service = new SOAPService2(wsdl, serviceName);
        assertNotNull(service);

        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        try {
            Greeter greeter = service.getPort(portName, Greeter.class);
            
            Client client = ClientProxy.getClient(greeter);
            EndpointInfo ei = client.getEndpoint().getEndpointInfo();
            AddressType address = ei.getTraversedExtensor(new AddressType(), AddressType.class);
            JMSNamingPropertyType name = new JMSNamingPropertyType();
            JMSNamingPropertyType password = new JMSNamingPropertyType();
            name.setName("java.naming.security.principal");
            name.setValue("ivan");
            password.setName("java.naming.security.credentials");
            password.setValue("the-terrible");
            address.getJMSNamingProperty().add(name);
            address.getJMSNamingProperty().add(password);
            for (int idx = 0; idx < 5; idx++) {

                greeter.greetMeOneWay("test String");
                
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull("no response received from service", greeting);
                String exResponse = response1 + idx;
                assertEquals(exResponse, greeting);


                
                String reply = greeter.sayHi();
                assertNotNull("no response received from service", reply);
                assertEquals(response2, reply);
                
                try {
                    greeter.pingMe();
                    fail("Should have thrown FaultException");
                } catch (PingMeFault ex) {
                    assertNotNull(ex.getFaultInfo());
                }                
              
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void docBasicJmsDestinationTest() throws Exception {
        QName serviceName = getServiceName(new QName("http://apache.org/hello_world_doc_lit", 
                                 "SOAPService6"));
        QName portName = getPortName(new QName("http://apache.org/hello_world_doc_lit", "SoapPort6"));
        URL wsdl = getWSDLURL("/wsdl/hello_world_doc_lit.wsdl");
        assertNotNull(wsdl);

        SOAPService2 service = new SOAPService2(wsdl, serviceName);
        assertNotNull(service);

        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        try {
            Greeter greeter = service.getPort(portName, Greeter.class);
            for (int idx = 0; idx < 5; idx++) {

                greeter.greetMeOneWay("test String");
                
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull("no response received from service", greeting);
                String exResponse = response1 + idx;
                assertEquals(exResponse, greeting);


                
                String reply = greeter.sayHi();
                assertNotNull("no response received from service", reply);
                assertEquals(response2, reply);
                
                try {
                    greeter.pingMe();
                    fail("Should have thrown FaultException");
                } catch (PingMeFault ex) {
                    assertNotNull(ex.getFaultInfo());
                }                
              
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void testBasicConnection() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                 "HelloWorldService"));
        QName portName = getPortName(new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort"));
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldService service = new HelloWorldService(wsdl, serviceName);
        assertNotNull(service);

        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        try {
            HelloWorldPortType greeter = service.getPort(portName, HelloWorldPortType.class);
            for (int idx = 0; idx < 5; idx++) {
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull("no response received from service", greeting);
                String exResponse = response1 + idx;
                assertEquals(exResponse, greeting);

                String reply = greeter.sayHi();
                assertNotNull("no response received from service", reply);
                assertEquals(response2, reply);
                
                try {
                    greeter.testRpcLitFault("BadRecordLitFault");
                    fail("Should have thrown BadRecoedLitFault");
                } catch (BadRecordLitFault ex) {
                    assertNotNull(ex.getFaultInfo());
                }
                
                try {
                    greeter.testRpcLitFault("NoSuchCodeLitFault");
                    fail("Should have thrown NoSuchCodeLitFault exception");
                } catch (NoSuchCodeLitFault nslf) {
                    assertNotNull(nslf.getFaultInfo());
                    assertNotNull(nslf.getFaultInfo().getCode());
                } 
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        } catch (Exception t) {
            throw t;
        }
    }
    
    @Test
    public void testByteMessage() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                 "HWByteMsgService"));
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HWByteMsgService service = new HWByteMsgService(wsdl, serviceName);
        assertNotNull(service);

        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        try {
            HelloWorldPortType greeter = service.getHWSByteMsgPort();
            for (int idx = 0; idx < 2; idx++) {
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull("no response received from service", greeting);
                String exResponse = response1 + idx;
                assertEquals(exResponse, greeting);

                String reply = greeter.sayHi();
                assertNotNull("no response received from service", reply);
                assertEquals(response2, reply);
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void testOneWayTopicConnection() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                 "HelloWorldPubSubService"));
        QName portName = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                             "HelloWorldPubSubPort"));
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldPubSubService service = new HelloWorldPubSubService(wsdl, serviceName);
        assertNotNull(service);

        try {
            HelloWorldPubSubPort greeter = service.getPort(portName, HelloWorldPubSubPort.class);
            for (int idx = 0; idx < 5; idx++) {
                greeter.greetMeOneWay("JMS:PubSub:Milestone-" + idx);
            }
            //Give some time to complete one-way calls.
            Thread.sleep(50L);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    @Test
    public void testJmsDestTopicConnection() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                 "JmsDestinationPubSubService"));
        QName portName = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                             "JmsDestinationPubSubPort"));
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldPubSubService service = new HelloWorldPubSubService(wsdl, serviceName);
        assertNotNull(service);

        try {
            HelloWorldPubSubPort greeter = service.getPort(portName, HelloWorldPubSubPort.class);
            for (int idx = 0; idx < 5; idx++) {
                greeter.greetMeOneWay("JMS:PubSub:Milestone-" + idx);
            }
            //Give some time to complete one-way calls.
            Thread.sleep(50L);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    @Test 
    public void testConnectionsWithinSpring() throws Exception {
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
        
        ClassPathXmlApplicationContext ctx = 
            new ClassPathXmlApplicationContext(
                new String[] {"/org/apache/cxf/systest/jms/JMSClients.xml"});
        EmbeddedJMSBrokerLauncher.updateWsdlExtensors((Bus)ctx.getBean("cxf"),
                                                      "classpath:wsdl/jms_test.wsdl");   
        HelloWorldPortType greeter = (HelloWorldPortType)ctx.getBean("jmsRPCClient");
        assertNotNull(greeter);
        
        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        try {
            
            for (int idx = 0; idx < 5; idx++) {
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull("no response received from service", greeting);
                String exResponse = response1 + idx;
                assertEquals(exResponse, greeting);

                String reply = greeter.sayHi();
                assertNotNull("no response received from service", reply);
                assertEquals(response2, reply);
                
                try {
                    greeter.testRpcLitFault("BadRecordLitFault");
                    fail("Should have thrown BadRecoedLitFault");
                } catch (BadRecordLitFault ex) {
                    assertNotNull(ex.getFaultInfo());
                }
                
                try {
                    greeter.testRpcLitFault("NoSuchCodeLitFault");
                    fail("Should have thrown NoSuchCodeLitFault exception");
                } catch (NoSuchCodeLitFault nslf) {
                    assertNotNull(nslf.getFaultInfo());
                    assertNotNull(nslf.getFaultInfo().getCode());
                } 
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
        
        HelloWorldOneWayPort greeter1 = (HelloWorldOneWayPort)ctx.getBean("jmsQueueOneWayServiceClient");
        assertNotNull(greeter1);
        try {
            greeter1.greetMeOneWay("hello");
        } catch (Exception ex) {
            fail("There should not throw the exception" + ex);
        }
        ctx.close();
        BusFactory.setDefaultBus(getBus());
        BusFactory.setThreadDefaultBus(getBus());
    }
    
    @Test
    public void testOneWayQueueConnection() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                 "HelloWorldOneWayQueueService"));
        QName portName = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                             "HelloWorldOneWayQueuePort"));
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldOneWayQueueService service = new HelloWorldOneWayQueueService(wsdl, serviceName);
        assertNotNull(service);

        try {
            HelloWorldOneWayPort greeter = service.getPort(portName, HelloWorldOneWayPort.class);
            for (int idx = 0; idx < 5; idx++) {
                greeter.greetMeOneWay("JMS:Queue:Milestone-" + idx);
            }
            //Give some time to complete one-way calls.
            Thread.sleep(100L);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void testQueueDecoupledOneWaysConnection() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                                     "HelloWorldQueueDecoupledOneWaysService"));
        QName portName = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                                               "HelloWorldQueueDecoupledOneWaysPort"));
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);
        EmbeddedJMSBrokerLauncher.updateWsdlExtensors(getBus(), wsdl.toString());
        EmbeddedJMSBrokerLauncher.updateWsdlExtensors(getBus(), "testutils/jms_test.wsdl");

        HelloWorldQueueDecoupledOneWaysService service = 
            new HelloWorldQueueDecoupledOneWaysService(wsdl, serviceName);
        assertNotNull(service);
        Endpoint requestEndpoint = null;
        Endpoint replyEndpoint = null;
        try {
            HelloWorldOneWayPort greeter = service.getPort(portName, HelloWorldOneWayPort.class);
            GreeterImplQueueDecoupledOneWays requestServant = new GreeterImplQueueDecoupledOneWays();
            requestEndpoint = Endpoint.publish("", requestServant);
            GreeterImplQueueDecoupledOneWaysDeferredReply replyServant = 
                new GreeterImplQueueDecoupledOneWaysDeferredReply();
            replyEndpoint = Endpoint.publish("", replyServant);
            
            BindingProvider  bp = (BindingProvider)greeter;
            Map<String, Object> requestContext = bp.getRequestContext();
            JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
            requestHeader.setJMSReplyTo("dynamicQueues/test.jmstransport.oneway.with.set.replyto.reply");
            requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);
            String expectedRequest = "JMS:Queue:Request"; 
            greeter.greetMeOneWay(expectedRequest);
            String request = requestServant.ackRequestReceived(5000);
            if (request == null) {
                if (requestServant.getException() != null) {
                    fail(requestServant.getException().getMessage());
                } else {
                    fail("The oneway call didn't reach its intended endpoint");
                }
            }
            assertEquals(expectedRequest, request);
            requestServant.proceedWithReply();
            String expectedReply = requestServant.ackReplySent(5000);
            if (expectedReply == null) {
                if (requestServant.getException() != null) {
                    fail(requestServant.getException().getMessage());
                } else {
                    fail("The decoupled one-way reply was not sent");
                }
            }
            String reply = replyServant.ackRequest(5000);
            if (reply == null) {
                if (replyServant.getException() != null) {
                    fail(replyServant.getException().getMessage());
                } else {
                    fail("The decoupled one-way reply didn't reach its intended endpoint");
                }
            }
            assertEquals(expectedReply, reply);
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (requestEndpoint != null) {
                requestEndpoint.stop();
            }
            if (replyEndpoint != null) {
                replyEndpoint.stop();
            }
        }
    }
    
    private static interface CorrelationIDFactory {
        String createCorrealtionID();
    }
    
    private static class ClientRunnable implements Runnable {
        private HelloWorldPortType port;
        private CorrelationIDFactory corrFactory;
        private String prefix;
        private Throwable ex;

        public ClientRunnable(HelloWorldPortType port) {
            this.port = port;
        }

        public ClientRunnable(HelloWorldPortType port, String prefix) {
            this.port = port;
            this.prefix = prefix;
        }

        public ClientRunnable(HelloWorldPortType port, CorrelationIDFactory factory) {
            this.port = port;
            this.corrFactory = factory;
        }
        
        public Throwable getException() {
            return ex;
        }
        
        public void run() {
            try {
                BindingProvider  bp = (BindingProvider)port;
                Map<String, Object> requestContext = bp.getRequestContext();
                JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
                requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);
     
                for (int idx = 0; idx < 5; idx++) {
                    String request = "World" + ((prefix != null) ? ":" + prefix : "");
                    String correlationID = null;
                    if (corrFactory != null) {
                        correlationID = corrFactory.createCorrealtionID();
                        requestHeader.setJMSCorrelationID(correlationID);
                        request +=  ":" + correlationID;
                    }
                    String expected = "Hello " + request;
                    String response = port.greetMe(request);
                    assertEquals("Response didn't match expected request", expected, response);
                    if (corrFactory != null) {
                        Map<String, Object> responseContext = bp.getResponseContext();
                        JMSMessageHeadersType responseHeader = 
                            (JMSMessageHeadersType)responseContext.get(
                                    JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
                        assertEquals("Request and Response CorrelationID didn't match", 
                                      correlationID, responseHeader.getJMSCorrelationID());
                    }
                }
            } catch (Throwable e) {
                ex = e;
            }
        }
    }
    
    @Test
    public void testTwoWayQueueAppCorrelationID() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                 "HelloWorldServiceAppCorrelationID"));
        QName portNameEng = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                                               "HelloWorldPortAppCorrelationIDEng"));
        QName portNameSales = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                                               "HelloWorldPortAppCorrelationIDSales"));

        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldServiceAppCorrelationID service = 
            new HelloWorldServiceAppCorrelationID(wsdl, serviceName);
        assertNotNull(service);

        ClientRunnable engClient = 
            new ClientRunnable(service.getPort(portNameEng, HelloWorldPortType.class),
                new CorrelationIDFactory() {
                    private int counter;
                    public String createCorrealtionID() {
                        return "com.mycompany.eng:" + counter++;
                    }
                });
        
        ClientRunnable salesClient = 
             new ClientRunnable(service.getPort(portNameSales, HelloWorldPortType.class),
                new CorrelationIDFactory() {
                    private int counter;
                    public String createCorrealtionID() {
                        return "com.mycompany.sales:" + counter++;
                    }
                });
        
        Thread[] threads = new Thread[] {new Thread(engClient), new Thread(salesClient)};
        
        for (Thread t : threads) {
            t.start();
        }
    
        for (Thread t : threads) {
            t.join(5000);
        }

        Throwable e = (engClient.getException() != null) 
                          ? engClient.getException() 
                          : (salesClient.getException() != null) 
                              ? salesClient.getException() : null;
                              
        if (e != null) {
            StringBuffer message = new StringBuffer();
            for (StackTraceElement ste : e.getStackTrace()) {
                message.append(ste.toString() + System.getProperty("line.separator"));
            }
            fail(message.toString());
        }
    }
    
    @Test
    public void testTwoWayQueueAppCorrelationIDStaticPrefix() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                 "HelloWorldServiceAppCorrelationIDStaticPrefix"));
        QName portNameEng = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                                               "HelloWorldPortAppCorrelationIDStaticPrefixEng"));
        QName portNameSales = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                                               "HelloWorldPortAppCorrelationIDStaticPrefixSales"));

        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldServiceAppCorrelationIDStaticPrefix service = 
            new HelloWorldServiceAppCorrelationIDStaticPrefix(wsdl, serviceName);
        assertNotNull(service);

        ClientRunnable engClient = 
            new ClientRunnable(service.getPort(portNameEng, HelloWorldPortType.class));
        
        ClientRunnable salesClient = 
             new ClientRunnable(service.getPort(portNameSales, HelloWorldPortType.class));
        
        Thread[] threads = new Thread[] {new Thread(engClient), new Thread(salesClient)};
        
        for (Thread t : threads) {
            t.start();
        }
    
        for (Thread t : threads) {
            t.join(1000);
        }

        Throwable e = (engClient.getException() != null) 
                          ? engClient.getException() 
                          : (salesClient.getException() != null) 
                              ? salesClient.getException() : null;
                              
        if (e != null) {
            StringBuffer message = new StringBuffer();
            for (StackTraceElement ste : e.getStackTrace()) {
                message.append(ste.toString() + System.getProperty("line.separator"));
            }
            fail(message.toString());
        }
    }

    /* TO DO:
     * This tests shows a missing QoS. When CXF clients share a named (persistent) reply queue
     *  with an application provided correlationID there will be a guaranteed response
     * message loss. 
     * 
     * A large number of threads is used to ensure message loss and avoid a false 
     * positive assertion
     */
    @Test
    public void testTwoWayQueueAppCorrelationIDNoPrefix() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                 "HelloWorldServiceAppCorrelationIDNoPrefix"));
        QName portName = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                                               "HelloWorldPortAppCorrelationIDNoPrefix"));
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldServiceAppCorrelationIDNoPrefix service = 
            new HelloWorldServiceAppCorrelationIDNoPrefix(wsdl, serviceName);
        assertNotNull(service);

        Collection<Thread> threads = new ArrayList<Thread>();
        Collection<ClientRunnable> clients = new ArrayList<ClientRunnable>();
        
        HelloWorldPortType port = service.getPort(portName, HelloWorldPortType.class);
        
        for (int i = 0; i < 1; ++i) {
            ClientRunnable client = new ClientRunnable(port);            
            Thread thread = new Thread(client);
            threads.add(thread);
            clients.add(client);
            thread.start();
        }

        for (Thread t : threads) {
            t.join(5000);
            assertTrue("Not terminated state: " + t.getState(), t.getState() == State.TERMINATED);
        }

        for (ClientRunnable client : clients) {
            if (client.getException() != null 
                && client.getException().getMessage().contains("Timeout")) {
                fail(client.getException().getMessage());
            }
        }
       
    }

    /*
     * This tests a use case where there is a shared request and reply queues between
     * two servers (Eng and Sales). However each server has a design time provided selector
     * which allows them to share the same queue and do not consume the other's
     * messages. 
     * 
     * The clients to these two servers use the same request and reply queues.
     * An Eng client uses a design time selector prefix to form request message 
     * correlationID and to form a reply consumer that filters only reply
     * messages originated from the Eng server. To differentiate between
     * one Eng client instance from another this suffix is supplemented by
     * a runtime value of ConduitId which has 1-1 relation to a client instance
     * This guarantees that an Eng client instance will only consume its own reply 
     * messages. 
     * 
     * In case of a single client instance being shared among multiple threads
     * the third portion of the request message correlationID, 
     * an atomic rolling message counter, ensures that each message gets a unique ID
     *  
     * So the model is:
     * 
     * Many concurrent Sales clients to a single request and reply queues (Q1, Q2) 
     * to a single Sales server
     * Many concurrent Eng clients to a single request and reply queues (Q1, Q2) 
     * to a single Eng server
     */
    @Test
    public void testTwoWayQueueRuntimeCorrelationIDStaticPrefix() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                 "HelloWorldServiceRuntimeCorrelationIDStaticPrefix"));
        
        QName portNameEng = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                                  "HelloWorldPortRuntimeCorrelationIDStaticPrefixEng"));
        QName portNameSales = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                                  "HelloWorldPortRuntimeCorrelationIDStaticPrefixSales"));

        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldServiceRuntimeCorrelationIDStaticPrefix service = 
            new HelloWorldServiceRuntimeCorrelationIDStaticPrefix(wsdl, serviceName);
        assertNotNull(service);

        Collection<Thread> threads = new ArrayList<Thread>();
        Collection<ClientRunnable> clients = new ArrayList<ClientRunnable>();
        
        HelloWorldPortType portEng = service.getPort(portNameEng, HelloWorldPortType.class);
        HelloWorldPortType portSales = service.getPort(portNameSales, HelloWorldPortType.class);
        
        for (int i = 0; i < 10; ++i) {
            ClientRunnable client =  new ClientRunnable(portEng, "com.mycompany.eng:");
            Thread thread = new Thread(client);
            threads.add(thread);
            clients.add(client);
            thread.start();
            client =  new ClientRunnable(portSales, "com.mycompany.sales:");
            thread = new Thread(client);
            threads.add(thread);
            clients.add(client);
            thread.start();
        }
    
        for (Thread t : threads) {
            t.join(1000);
        }

        for (ClientRunnable client : clients) {
            if (client.getException() != null 
                && client.getException().getMessage().contains("Timeout")) {
                fail(client.getException().getMessage());
            }
        }
    }

    @Test
    public void testTwoWayQueueRuntimeCorrelationDynamicPrefix() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                 "HelloWorldServiceRuntimeCorrelationIDDynamicPrefix"));
        
        QName portName = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                                               "HelloWorldPortRuntimeCorrelationIDDynamicPrefix"));
        
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldServiceRuntimeCorrelationIDDynamicPrefix service = 
            new HelloWorldServiceRuntimeCorrelationIDDynamicPrefix(wsdl, serviceName);
        assertNotNull(service);

        Collection<Thread> threads = new ArrayList<Thread>();
        Collection<ClientRunnable> clients = new ArrayList<ClientRunnable>();
        
        HelloWorldPortType port = service.getPort(portName, HelloWorldPortType.class);
        
        for (int i = 0; i < 10; ++i) {
            ClientRunnable client = new ClientRunnable(port);
            
            Thread thread = new Thread(client);
            threads.add(thread);
            clients.add(client);
            thread.start();
        }
    
        for (Thread t : threads) {
            t.join(1000);
        }

        for (ClientRunnable client : clients) {
            if (client.getException() != null) {
                fail(client.getException().getMessage());            
            }
        }
    }

    @Test
    public void testContextPropogation() throws Exception {
        final String testReturnPropertyName = "Test_Prop";
        final String testIgnoredPropertyName = "Test_Prop_No_Return";
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms",
                                 "HelloWorldService"));
        QName portName = getPortName(new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort"));
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldService service = new HelloWorldService(wsdl, serviceName);
        assertNotNull(service);

        try {
            HelloWorldPortType greeter = service.getPort(portName, HelloWorldPortType.class);
            Map<String, Object> requestContext = ((BindingProvider)greeter).getRequestContext();
            JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
            requestHeader.setJMSCorrelationID("JMS_SAMPLE_CORRELATION_ID");
            requestHeader.setJMSExpiration(3600000L);
            JMSPropertyType propType = new JMSPropertyType();
            propType.setName(testReturnPropertyName);
            propType.setValue("mustReturn");
            requestHeader.getProperty().add(propType);
            propType = new JMSPropertyType();
            propType.setName(testIgnoredPropertyName);
            propType.setValue("mustNotReturn");
            requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);
 
            String greeting = greeter.greetMe("Milestone-");
            assertNotNull("no response received from service", greeting);

            assertEquals("Hello Milestone-", greeting);

            Map<String, Object> responseContext = ((BindingProvider)greeter).getResponseContext();
            JMSMessageHeadersType responseHdr = 
                 (JMSMessageHeadersType)responseContext.get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
            if (responseHdr == null) {
                fail("response Header should not be null");
            }
            
            assertTrue("CORRELATION ID should match :", 
                       "JMS_SAMPLE_CORRELATION_ID".equals(responseHdr.getJMSCorrelationID()));
            assertTrue("response Headers must conain the app property set in request context.", 
                       responseHdr.getProperty() != null);
            
            boolean found = false;
            for (JMSPropertyType p : responseHdr.getProperty()) {
                if (testReturnPropertyName.equals(p.getName())) {
                    found = true;
                }
            }
            assertTrue("response Headers must match the app property set in request context.",
                         found);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    @Test
    public void testMTOM() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/jms_mtom", "JMSMTOMService");
        QName portName = new QName("http://cxf.apache.org/jms_mtom", "MTOMPort");

        URL wsdl = getWSDLURL("/wsdl/jms_test_mtom.wsdl");
        assertNotNull(wsdl);

        JMSMTOMService service = new JMSMTOMService(wsdl, serviceName);
        assertNotNull(service);

        JMSMTOMPortType mtom = service.getPort(portName, JMSMTOMPortType.class);
        Binding binding = ((BindingProvider)mtom).getBinding();
        ((SOAPBinding)binding).setMTOMEnabled(true);

        Holder<String> name = new Holder<String>("Sam");
        URL fileURL = this.getClass().getResource("/org/apache/cxf/systest/jms/JMSClientServerTest.class");
        Holder<DataHandler> handler1 = new Holder<DataHandler>();
        handler1.value = new DataHandler(fileURL);
        int size = handler1.value.getInputStream().available();
        mtom.testDataHandler(name, handler1);
        
        byte bytes[] = IOUtils.readBytesFromStream(handler1.value.getInputStream());
        assertEquals("The response file is not same with the sent file.", size, bytes.length);
    }
    

    @Test
    //@Ignore
    public void useMessageIDAsCorrelationIDTest() throws Exception {
        QName serviceName = getServiceName(new QName("http://apache.org/hello_world_doc_lit", 
                                 "SOAPService7"));
        QName portName = getPortName(new QName("http://apache.org/hello_world_doc_lit", "SoapPort7"));
        URL wsdl = getWSDLURL("/wsdl/hello_world_doc_lit.wsdl");
        assertNotNull(wsdl);

        SOAPService7 service = new SOAPService7(wsdl, serviceName);        
        Greeter greeter = service.getPort(portName, Greeter.class);
        
        Collection<Thread> threads = new ArrayList<Thread>();
        Collection<GreeterClientRunnable> clients = new ArrayList<GreeterClientRunnable>();
        
        for (int i = 0; i < 100; ++i) {
            GreeterClientRunnable client = new GreeterClientRunnable(greeter, i);
            
            Thread thread = new Thread(client);
            threads.add(thread);
            clients.add(client);
            thread.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        for (GreeterClientRunnable client : clients) {
            if (client.getException() != null) {
                fail(client.getException().getMessage());            
            }
        }
    }
    
    private static class GreeterClientRunnable implements Runnable {
        private Greeter port;
        private int client;
        private Throwable ex;

        public GreeterClientRunnable(Greeter port, int client) {
            this.port = port;
            this.client = client;
        }
        
        public Throwable getException() {
            return ex;
        }
        
        public void run() {
            try {     
                for (int idx = 0; idx < 5; idx++) {
                    String request = "Message: " + idx + " from Client: " + client;
                    String expected = "Hello " + request;
                    String response = port.greetMe(request);
                    assertEquals("Response didn't match expected request", expected, response);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                ex = e;
            }
        }
    } 
    
    @Test
    public void testAsyncCall() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldMessageIDAsCorrelationIDAsyncService"));
        QName portName = getPortName(new QName("http://cxf.apache.org/hello_world_jms",
                                               "HelloWorldMessageIDAsCorrelationIDAsyncPort"));
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);
        
        HelloWorldMessageIDAsCorrelationIDAsyncService service =
            new HelloWorldMessageIDAsCorrelationIDAsyncService(wsdl, serviceName);
        assertNotNull(service);
        HelloWorldPortType greeter = service.getPort(portName, HelloWorldPortType.class);
        final Thread thread = Thread.currentThread(); 
        
        class TestAsyncHandler implements AsyncHandler<String> {
            String expected;
            
            public TestAsyncHandler(String x) {
                expected = x;
            }
            
            public String getExpected() {
                return expected;
            }
            public void handleResponse(Response<String> response) {
                try {
                    Thread thread2 = Thread.currentThread();
                    assertNotSame(thread, thread2);
                    assertEquals("Hello " + expected, response.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
        TestAsyncHandler h1 = new TestAsyncHandler("Homer");
        TestAsyncHandler h2 = new TestAsyncHandler("Maggie");
        TestAsyncHandler h3 = new TestAsyncHandler("Bart");
        TestAsyncHandler h4 = new TestAsyncHandler("Lisa");
        TestAsyncHandler h5 = new TestAsyncHandler("Marge");
        
        Future<?> f1 = greeter.greetMeAsync("Santa's Little Helper", 
                                            new TestAsyncHandler("Santa's Little Helper"));
        f1.get();
        f1 = greeter.greetMeAsync("PauseForTwoSecs Santa's Little Helper", 
                                  new TestAsyncHandler("Santa's Little Helper"));
        long start = System.currentTimeMillis();
        f1 = greeter.greetMeAsync("PauseForTwoSecs " + h1.getExpected(), h1);
        Future<?> f2 = greeter.greetMeAsync("PauseForTwoSecs " + h2.getExpected(), h2);
        Future<?> f3 = greeter.greetMeAsync("PauseForTwoSecs " + h3.getExpected(), h3);
        Future<?> f4 = greeter.greetMeAsync("PauseForTwoSecs " + h4.getExpected(), h4);
        Future<?> f5 = greeter.greetMeAsync("PauseForTwoSecs " + h5.getExpected(), h5);

        long mid = System.currentTimeMillis();
        assertEquals("Hello " + h1.getExpected(), f1.get());
        assertEquals("Hello " + h2.getExpected(), f2.get());
        assertEquals("Hello " + h3.getExpected(), f3.get());
        assertEquals("Hello " + h4.getExpected(), f4.get());
        assertEquals("Hello " + h5.getExpected(), f5.get());
        long end = System.currentTimeMillis();

        assertTrue("Time too long: " + (mid - start), (mid - start) < 1000);
        assertTrue((end - mid) > 1000);
        f1 = null;
        f2 = null;
        f3 = null;
        f4 = null;
        f5 = null;
        
        greeter = null;
        service = null;
        
        System.gc();
    }    
    
    @Test
    public void testReplyToConfig() throws Exception {
        JMSNamingPropertyType p1 = new JMSNamingPropertyType();
        p1.setName("java.naming.factory.initial");
        p1.setValue("org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        JMSNamingPropertyType p2 = new JMSNamingPropertyType();
        p2.setName("java.naming.provider.url");
        p2.setValue("tcp://localhost:" + JMS_PORT);
        final AddressType address = new AddressType();
        address.setJndiConnectionFactoryName("ConnectionFactory");
        List<JMSNamingPropertyType> props = address.getJMSNamingProperty();
        props.add(p1);
        props.add(p2);

        final JMSConfiguration jmsConfig = new JMSConfiguration();
        
        JndiTemplate jt = new JndiTemplate();
        jt.setEnvironment(JMSOldConfigHolder.getInitialContextEnv(address));
        
        JNDIConfiguration jndiConfig = new JNDIConfiguration();
        jndiConfig.setJndiConnectionFactoryName(address.getJndiConnectionFactoryName());
        jmsConfig.setJndiTemplate(jt);
        jmsConfig.setJndiConfig(jndiConfig);
        
        jmsConfig.setTargetDestination("dynamicQueues/SoapService8.replyto.queue");
        jmsConfig.setReplyDestination("dynamicQueues/SoapService8.reply.queue");
        
        final JmsTemplate jmsTemplate = JMSFactory.createJmsTemplate(jmsConfig, null);

        Thread t = new Thread() {
            public void run() {
                Destination destination = (Destination)jmsTemplate.execute(new SessionCallback() {
                    public Object doInJms(Session session) throws JMSException {
                        DestinationResolver resolv = jmsTemplate.getDestinationResolver();
                        return resolv.resolveDestinationName(session, jmsConfig.getTargetDestination(),
                                                             false);
                    }
                });
                
                final Message message = jmsTemplate.receive(destination);
                MessageCreator messageCreator = new MessageCreator() {
                    public Message createMessage(Session session) {
                        return message;
                    }
                };
                    
                destination = (Destination)jmsTemplate.execute(new SessionCallback() {
                    public Object doInJms(Session session) throws JMSException {
                        DestinationResolver resolv = jmsTemplate.getDestinationResolver();
                        return resolv.resolveDestinationName(session,
                                                             jmsConfig.getReplyDestination(),
                                                             false);
                    }
                });
                jmsTemplate.send(destination, messageCreator);
            }
        };

        t.start();
        
        QName serviceName = getServiceName(new QName("http://apache.org/hello_world_doc_lit",
                                                     "SOAPService8"));
        QName portName = getPortName(new QName("http://apache.org/hello_world_doc_lit", "SoapPort8"));
        URL wsdl = getWSDLURL("/wsdl/hello_world_doc_lit.wsdl");
        assertNotNull(wsdl);

        SOAPService8 service = new SOAPService8(wsdl, serviceName);        
        Greeter greeter = service.getPort(portName, Greeter.class);
        String name = "FooBar";
        String reply = greeter.greetMe(name);
        assertEquals(reply, "Hello " + name);
    }    
    
}
