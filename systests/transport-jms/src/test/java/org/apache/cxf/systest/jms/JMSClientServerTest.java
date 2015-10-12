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

import java.io.Closeable;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Response;
import javax.xml.ws.soap.AddressingFeature;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.interceptor.TibcoSoapActionInterceptor;
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
import org.apache.cxf.hello_world_jms.NoSuchCodeLitFault;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;
import org.apache.cxf.transport.jms.JMSPropertyType;
import org.apache.cxf.transport.jms.util.TestReceiver;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.PingMeFault;
import org.apache.hello_world_doc_lit.SOAPService2;
import org.apache.hello_world_doc_lit.SOAPService7;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JMSClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(JMSClientServerTest.class);
 
    private static EmbeddedJMSBrokerLauncher broker;
    private List<String> wsdlStrings = new ArrayList<String>();
    
    @BeforeClass
    public static void startServers() throws Exception {
        broker = new EmbeddedJMSBrokerLauncher("tcp://localhost:" + PORT);
        launchServer(broker);
        launchServer(new Server(broker));
        createStaticBus();
    }
    
    @Before
    public void setUp() throws Exception {
        assertSame(getStaticBus(), BusFactory.getThreadDefaultBus(false));
    }
   
    @After 
    public void tearDown() throws Exception {
        wsdlStrings.clear();
    }
    
    public URL getWSDLURL(String s) throws Exception {
        URL u = getClass().getResource(s);
        if (u == null) {
            throw new IllegalArgumentException("WSDL classpath resource not found " + s);
        }
        String wsdlString = u.toString().intern();
        wsdlStrings.add(wsdlString);
        broker.updateWsdl(getBus(), wsdlString);
        return u;
    }

    @Test
    public void testDocBasicConnection() throws Exception {
        QName serviceName = new QName("http://apache.org/hello_world_doc_lit", "SOAPService2");
        QName portName = new QName("http://apache.org/hello_world_doc_lit", "SoapPort2");
        URL wsdl = getWSDLURL("/wsdl/hello_world_doc_lit.wsdl");
        SOAPService2 service = new SOAPService2(wsdl, serviceName);

        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        Greeter greeter = service.getPort(portName, Greeter.class);

        Client client = ClientProxy.getClient(greeter);
        client.getEndpoint().getOutInterceptors().add(new TibcoSoapActionInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        client.getInInterceptors().add(new LoggingInInterceptor());
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

        ((java.io.Closeable)greeter).close();
    }

    @Test
    public void docBasicJmsDestinationTest() throws Exception {
        QName serviceName = new QName("http://apache.org/hello_world_doc_lit", "SOAPService6");
        QName portName = new QName("http://apache.org/hello_world_doc_lit", "SoapPort6");
        URL wsdl = getWSDLURL("/wsdl/hello_world_doc_lit.wsdl");

        SOAPService2 service = new SOAPService2(wsdl, serviceName);
        Greeter greeter = service.getPort(portName, Greeter.class);
        for (int idx = 0; idx < 5; idx++) {

            greeter.greetMeOneWay("test String");

            String greeting = greeter.greetMe("Milestone-" + idx);
            assertEquals("Hello Milestone-" + idx, greeting);

            String reply = greeter.sayHi();
            assertEquals("Bonjour", reply);

            try {
                greeter.pingMe();
                fail("Should have thrown FaultException");
            } catch (PingMeFault ex) {
                assertNotNull(ex.getFaultInfo());
            }

        }
        ((java.io.Closeable)greeter).close();
    }

    @Ignore
    @Test
    public void testAsyncCall() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldService service = new HelloWorldService(wsdl, serviceName);
        HelloWorldPortType greeter = service.getPort(portName, HelloWorldPortType.class);
        final Thread thread = Thread.currentThread(); 
        
        class TestAsyncHandler implements AsyncHandler<String> {
            String expected;
            
            TestAsyncHandler(String x) {
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
        
        ((java.io.Closeable)greeter).close();
        greeter = null;
        service = null;
        
        System.gc();
    }
    
    @Test
    public void testBasicConnection() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldService service = new HelloWorldService(wsdl, serviceName);

        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
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
        ((java.io.Closeable)greeter).close();
    }
    
    @Test
    public void testByteMessage() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HWByteMsgService");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HWByteMsgService service = new HWByteMsgService(wsdl, serviceName);

        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
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
        ((java.io.Closeable)greeter).close();
    }

    @Test
    public void testOneWayTopicConnection() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPubSubService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPubSubPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldPubSubService service = new HelloWorldPubSubService(wsdl, serviceName);

        HelloWorldPubSubPort greeter = service.getPort(portName, HelloWorldPubSubPort.class);
        for (int idx = 0; idx < 5; idx++) {
            greeter.greetMeOneWay("JMS:PubSub:Milestone-" + idx);
        }
        // Give some time to complete one-way calls.
        Thread.sleep(50L);
        ((java.io.Closeable)greeter).close();
    }
    
    @Test
    public void testJmsDestTopicConnection() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "JmsDestinationPubSubService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "JmsDestinationPubSubPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldPubSubService service = new HelloWorldPubSubService(wsdl, serviceName);

        HelloWorldPubSubPort greeter = service.getPort(portName, HelloWorldPubSubPort.class);
        for (int idx = 0; idx < 5; idx++) {
            greeter.greetMeOneWay("JMS:PubSub:Milestone-" + idx);
        }
        // Give some time to complete one-way calls.
        Thread.sleep(50L);
        ((java.io.Closeable)greeter).close();
    }
    
    @Test 
    public void testConnectionsWithinSpring() throws Exception {
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
        
        ClassPathXmlApplicationContext ctx = 
            new ClassPathXmlApplicationContext(
                new String[] {"/org/apache/cxf/systest/jms/JMSClients.xml"});
        try {
            String wsdlString2 = "classpath:wsdl/jms_test.wsdl";
            wsdlStrings.add(wsdlString2);
            broker.updateWsdl((Bus)ctx.getBean("cxf"), wsdlString2);
            HelloWorldPortType greeter = (HelloWorldPortType)ctx.getBean("jmsRPCClient");
            
            try {
                
                for (int idx = 0; idx < 5; idx++) {
                    String greeting = greeter.greetMe("Milestone-" + idx);
                    assertNotNull("no response received from service", greeting);
                    String exResponse = "Hello Milestone-" + idx;
                    assertEquals(exResponse, greeting);
    
                    String reply = greeter.sayHi();
                    assertEquals("Bonjour", reply);
                    
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
                ctx.close();
                throw (Exception)ex.getCause();
            }
            
            HelloWorldOneWayPort greeter1 = (HelloWorldOneWayPort)ctx.getBean("jmsQueueOneWayServiceClient");
            assertNotNull(greeter1);
            try {
                greeter1.greetMeOneWay("hello");
            } catch (Exception ex) {
                fail("There should not throw the exception" + ex);
            }
        } finally {
            ctx.close();
            BusFactory.setDefaultBus(getBus());
            BusFactory.setThreadDefaultBus(getBus());
        }
    }
    
    @Test
    public void testOneWayQueueConnection() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldOneWayQueueService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldOneWayQueuePort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldOneWayQueueService service = new HelloWorldOneWayQueueService(wsdl, serviceName);

        HelloWorldOneWayPort greeter = service.getPort(portName, HelloWorldOneWayPort.class,
                                                       new AddressingFeature(true, true));
        for (int idx = 0; idx < 5; idx++) {
            greeter.greetMeOneWay("JMS:Queue:Milestone-" + idx);
        }
        // Give some time to complete one-way calls.
        Thread.sleep(100L);
        ((java.io.Closeable)greeter).close();
    }

    @Test
    @Ignore // FIXME
    public void testQueueDecoupledOneWaysConnection() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", 
                                      "HelloWorldQueueDecoupledOneWaysService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldQueueDecoupledOneWaysPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        String wsdl2 = "testutils/jms_test.wsdl".intern();
        wsdlStrings.add(wsdl2);
        broker.updateWsdl(getBus(), wsdl2);

        HelloWorldQueueDecoupledOneWaysService service = 
            new HelloWorldQueueDecoupledOneWaysService(wsdl, serviceName);
        Endpoint requestEndpoint = null;
        Endpoint replyEndpoint = null;
        HelloWorldOneWayPort greeter = service.getPort(portName, HelloWorldOneWayPort.class);
        try {
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
            ((java.io.Closeable)greeter).close();
        }
    }
    
    @Test
    public void testQueueOneWaySpecCompliantConnection() throws Throwable {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldQueueDecoupledOneWaysService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldQueueDecoupledOneWaysPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);
        String wsdlString2 = "testutils/jms_test.wsdl";
        wsdlStrings.add(wsdlString2);
        broker.updateWsdl(getBus(), wsdlString2);

        HelloWorldQueueDecoupledOneWaysService service = 
            new HelloWorldQueueDecoupledOneWaysService(wsdl, serviceName);
        assertNotNull(service);
        Endpoint requestEndpoint = null;
        HelloWorldOneWayPort greeter = service.getPort(portName, HelloWorldOneWayPort.class);
        try {
            GreeterImplQueueDecoupledOneWays requestServant = new GreeterImplQueueDecoupledOneWays(true);
            requestEndpoint = Endpoint.publish(null, requestServant);
            
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
                    throw requestServant.getException();
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
            ((java.io.Closeable)greeter).close();
        }
    }

    @Test
    public void testContextPropagation() throws Exception {
        final String testReturnPropertyName = "Test_Prop";
        final String testIgnoredPropertyName = "Test_Prop_No_Return";
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldService service = new HelloWorldService(wsdl, serviceName);
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
        JMSMessageHeadersType responseHdr = (JMSMessageHeadersType)responseContext
            .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
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
        assertTrue("response Headers must match the app property set in request context.", found);
        ((Closeable)greeter).close();
    }

    @Test
    public void testReplyToConfig() throws Exception {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(broker.getBrokerURL());
        TestReceiver receiver = new TestReceiver(cf, "SoapService7.replyto.queue", false);
        receiver.setStaticReplyQueue("SoapService7.reply.queue");
        receiver.runAsync();

        QName serviceName = new QName("http://apache.org/hello_world_doc_lit", "SOAPService7");
        QName portName = new QName("http://apache.org/hello_world_doc_lit", "SoapPort7");
        URL wsdl = getWSDLURL("/wsdl/hello_world_doc_lit.wsdl");
        SOAPService7 service = new SOAPService7(wsdl, serviceName);
        Greeter greeter = service.getPort(portName, Greeter.class);

        String name = "FooBar";
        String reply = greeter.greetMe(name);
        Assert.assertEquals("Hello " + name, reply);
        ((Closeable)greeter).close();
    }

}
