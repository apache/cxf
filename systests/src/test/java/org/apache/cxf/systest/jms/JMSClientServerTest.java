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
import java.util.Map;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.hello_world_jms.BadRecordLitFault;
import org.apache.cxf.hello_world_jms.HWByteMsgService;
import org.apache.cxf.hello_world_jms.HelloWorldOneWayPort;
import org.apache.cxf.hello_world_jms.HelloWorldOneWayQueueService;
import org.apache.cxf.hello_world_jms.HelloWorldPortType;
import org.apache.cxf.hello_world_jms.HelloWorldPubSubPort;
import org.apache.cxf.hello_world_jms.HelloWorldPubSubService;
import org.apache.cxf.hello_world_jms.HelloWorldQueueDecoupledOneWaysService;
import org.apache.cxf.hello_world_jms.HelloWorldService;
import org.apache.cxf.hello_world_jms.HelloWorldServiceAppCorrelationIDNoPrefix;
import org.apache.cxf.hello_world_jms.HelloWorldServiceAppCorrelationIDStaticPrefix;
import org.apache.cxf.hello_world_jms.HelloWorldServiceRuntimeCorrelationIDDynamicPrefix;
import org.apache.cxf.hello_world_jms.HelloWorldServiceRuntimeCorrelationIDStaticPrefix;
import org.apache.cxf.hello_world_jms.NoSuchCodeLitFault;
import org.apache.cxf.jms_mtom.JMSMTOMPortType;
import org.apache.cxf.jms_mtom.JMSMTOMService;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.jms.AddressType;
import org.apache.cxf.transport.jms.JMSConduit;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;
import org.apache.cxf.transport.jms.JMSNamingPropertyType;
import org.apache.cxf.transport.jms.JMSPropertyType;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.PingMeFault;
import org.apache.hello_world_doc_lit.SOAPService2;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JMSClientServerTest extends AbstractBusClientServerTestBase {
    
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
                   launchServer(Server.class, false));
    }
    
    public URL getWSDLURL(String s) throws Exception {
        return getClass().getResource(s);
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
        URL wsdl = getClass().getResource("/wsdl/jms_test.wsdl");
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
        URL wsdl = getClass().getResource("/wsdl/jms_test.wsdl");
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
        ClassPathXmlApplicationContext ctx = 
            new ClassPathXmlApplicationContext(
                new String[] {"/org/apache/cxf/systest/jms/JMSClients.xml"});
               
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
        
    }
    
    @Test
    public void testOneWayQueueConnection() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                 "HelloWorldOneWayQueueService"));
        QName portName = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                             "HelloWorldOneWayQueuePort"));
        URL wsdl = getClass().getResource("/wsdl/jms_test.wsdl");
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
        URL wsdl = getClass().getResource("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

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
    
    @Test
    public void testQueueOneWaySpecCompliantConnection() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                                     "HelloWorldQueueDecoupledOneWaysService"));
        QName portName = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                                               "HelloWorldQueueDecoupledOneWaysPort"));
        URL wsdl = getClass().getResource("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldQueueDecoupledOneWaysService service = 
            new HelloWorldQueueDecoupledOneWaysService(wsdl, serviceName);
        assertNotNull(service);
        Endpoint requestEndpoint = null;
        try {
            HelloWorldOneWayPort greeter = service.getPort(portName, HelloWorldOneWayPort.class);
            GreeterImplQueueDecoupledOneWays requestServant = new GreeterImplQueueDecoupledOneWays(true);
            requestEndpoint = Endpoint.publish("", requestServant);
            
            Client client = ClientProxy.getClient(greeter);
            ((JMSConduit)client.getConduit()).getJmsConfig().setEnforceSpec(true);
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
            boolean ack = requestServant.ackNoReplySent(5000);
            if (!ack) {
                if (requestServant.getException() != null) {
                    fail(requestServant.getException().getMessage());
                } else {
                    fail("The decoupled one-way reply was sent");
                }
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (requestEndpoint != null) {
                requestEndpoint.stop();
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
    public void testTwoWayQueueAppCorrelationIDStaticPrefix() throws Exception {
        QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                 "HelloWorldServiceAppCorrelationIDStaticPrefix"));
        QName portNameEng = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                                               "HelloWorldPortAppCorrelationIDStaticPrefixEng"));
        QName portNameSales = getPortName(new QName("http://cxf.apache.org/hello_world_jms", 
                                               "HelloWorldPortAppCorrelationIDStaticPrefixSales"));

        URL wsdl = getClass().getResource("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldServiceAppCorrelationIDStaticPrefix service = 
            new HelloWorldServiceAppCorrelationIDStaticPrefix(wsdl, serviceName);
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
            t.join(10000);
        }
        for (Thread t : threads) {
            t.join(1000);
            assertTrue("No terminated state: " + t.getState(), t.getState() == State.TERMINATED);
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
        URL wsdl = getClass().getResource("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldServiceAppCorrelationIDNoPrefix service = 
            new HelloWorldServiceAppCorrelationIDNoPrefix(wsdl, serviceName);
        assertNotNull(service);

        Collection<Thread> threads = new ArrayList<Thread>();
        Collection<ClientRunnable> clients = new ArrayList<ClientRunnable>();
        
        HelloWorldPortType port = service.getPort(portName, HelloWorldPortType.class);
        
        for (int i = 0; i < 10; ++i) {
            ClientRunnable client =  
                new ClientRunnable(port,
                    new CorrelationIDFactory() {
                        public String createCorrealtionID() {
                            return UUID.randomUUID().toString();
                        }
                    });
            
            Thread thread = new Thread(client);
            threads.add(thread);
            clients.add(client);
            thread.start();
        }

        //Sleep for up to 10 seconds.   The timeout should be at 5 seconds so this
        //should bail earlier
        for (int x = 0; x < 10; x++) {
            Thread.sleep(1000);

            for (ClientRunnable client : clients) {
                if (client.getException() != null 
                    && client.getException().getMessage().contains("Timeout")) {
                    // exceptions expected
                    return;
                }
            }
        }
       
        fail("This is a negative pass. If this test passed this means that the missing QoS" 
             + " has been added to the runtime or an unexpected exception received. " 
             + " If latter is true, then read method comments for details on missing QoS"
             + " and change this test to fail on exception");
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

        URL wsdl = getClass().getResource("/wsdl/jms_test.wsdl");
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
            t.join(2000);
        }

        for (Thread t : threads) {
            t.join(1000);
            assertTrue("Not terminated state: " + t.getState(), t.getState() == State.TERMINATED);
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
        
        URL wsdl = getClass().getResource("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);

        HelloWorldServiceRuntimeCorrelationIDDynamicPrefix service = 
            new HelloWorldServiceRuntimeCorrelationIDDynamicPrefix(wsdl, serviceName);
        assertNotNull(service);

        Collection<Thread> threads = new ArrayList<Thread>();
        Collection<ClientRunnable> clients = new ArrayList<ClientRunnable>();
        
        HelloWorldPortType port = service.getPort(portName, HelloWorldPortType.class);
        
        for (int i = 0; i < 10; ++i) {
            ClientRunnable client =  
                new ClientRunnable(port);
            
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
        URL wsdl = getClass().getResource("/wsdl/jms_test.wsdl");
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

        URL wsdl = getClass().getResource("/wsdl/jms_test_mtom.wsdl");
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
        int size2 = handler1.value.getInputStream().available();
        assertTrue("The response file is not same with the sent file.", size == size2);
    }
}
