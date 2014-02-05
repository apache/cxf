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

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.activation.DataHandler;
import javax.jms.DeliveryMode;
import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import javax.xml.ws.Response;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;

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
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jms_greeter.JMSGreeterPortType;
import org.apache.cxf.jms_greeter.JMSGreeterService;
import org.apache.cxf.jms_greeter.JMSGreeterService2;
import org.apache.cxf.jms_mtom.JMSMTOMPortType;
import org.apache.cxf.jms_mtom.JMSMTOMService;
import org.apache.cxf.jms_mtom.JMSOutMTOMService;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.transport.jms.AddressType;
import org.apache.cxf.transport.jms.JMSConduit;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;
import org.apache.cxf.transport.jms.JMSNamingPropertyType;
import org.apache.cxf.transport.jms.JMSOldConfigHolder;
import org.apache.cxf.transport.jms.JMSPropertyType;
import org.apache.cxf.transport.jms.JNDIConfiguration;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;
import org.apache.cxf.transport.jms.uri.JMSEndpoint;
import org.apache.cxf.transport.jms.util.TestReceiver;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.PingMeFault;
import org.apache.hello_world_doc_lit.SOAPService2;
import org.apache.hello_world_doc_lit.SOAPService7;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jndi.JndiTemplate;

public class JMSClientServerTest extends AbstractBusClientServerTestBase {
    private static final String BROKER_URI = "vm://JMSClientServerTest" 
        + "?jms.watchTopicAdvisories=false&broker.persistent=false";
 
    private static EmbeddedJMSBrokerLauncher broker;
    private String wsdlString;
    
    @BeforeClass
    public static void startServers() throws Exception {
        broker = new EmbeddedJMSBrokerLauncher(BROKER_URI);
        launchServer(broker);
        launchServer(new Server(broker));
        createStaticBus();
    }
    
    public URL getWSDLURL(String s) throws Exception {
        URL u = getClass().getResource(s);
        if (u == null) {
            throw new IllegalArgumentException("WSDL classpath resource not found " + s);
        }
        wsdlString = u.toString().intern();
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
        try {
            Greeter greeter = service.getPort(portName, Greeter.class);
            
            Client client = ClientProxy.getClient(greeter);
            client.getEndpoint().getOutInterceptors().add(new TibcoSoapActionInterceptor());
            client.getOutInterceptors().add(new LoggingOutInterceptor());
            client.getInInterceptors().add(new LoggingInInterceptor());
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
            
            ((java.io.Closeable)greeter).close();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void docBasicJmsDestinationTest() throws Exception {
        QName serviceName = new QName("http://apache.org/hello_world_doc_lit", "SOAPService6");
        QName portName = new QName("http://apache.org/hello_world_doc_lit", "SoapPort6");
        URL wsdl = getWSDLURL("/wsdl/hello_world_doc_lit.wsdl");

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
            ((java.io.Closeable)greeter).close();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    /*
     * This test seems to do async calls with a temp queue. Which does not seem to work anymore.
     * Not sure if it was ever intended that a DefaultMessageListenerContainer can listen on a temp
     * queue
     */
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

        /*
        int count = 20;
        TestAsyncHandler handlers[] = new TestAsyncHandler[count];
        Future<?> futures[] = new Future<?>[count];
        for (int x = 0; x < count; x++) {
            handlers[x] = new TestAsyncHandler("Handler" + x);
            futures[x] = greeter.greetMeAsync("PauseForTwoSecs " + handlers[x].getExpected(),
                                              handlers[x]);
            //intersperse some sync calls in there....
            if (x == 2 || x == 5) {
                assertEquals("Hello World", greeter.greetMe("World"));
            }
            if (x == 10) {
                assertEquals("Hello World", greeter.greetMe("PauseForTwoSecs World"));                
            }
        }
        int countDone = 0;
        for (int x = 0; x < count; x++) {
            if (futures[x].isDone()) {
                countDone++;
            }
        }
        assertTrue("Should not all be done.", countDone < count);
        for (int x = 0; x < count; x++) {
            assertEquals("Hello " + handlers[x].getExpected(), futures[x].get());
        }
        countDone = 0;
        for (int x = 0; x < count; x++) {
            if (futures[x].isDone()) {
                countDone++;
            }
        }
        assertEquals(count, countDone);
        */
        
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
            ((java.io.Closeable)greeter).close();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    @Test
    public void testByteMessage() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HWByteMsgService");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HWByteMsgService service = new HWByteMsgService(wsdl, serviceName);

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
            ((java.io.Closeable)greeter).close();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void testOneWayTopicConnection() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPubSubService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPubSubPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldPubSubService service = new HelloWorldPubSubService(wsdl, serviceName);

        try {
            HelloWorldPubSubPort greeter = service.getPort(portName, HelloWorldPubSubPort.class);
            for (int idx = 0; idx < 5; idx++) {
                greeter.greetMeOneWay("JMS:PubSub:Milestone-" + idx);
            }
            //Give some time to complete one-way calls.
            Thread.sleep(50L);
            ((java.io.Closeable)greeter).close();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    @Test
    public void testJmsDestTopicConnection() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "JmsDestinationPubSubService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "JmsDestinationPubSubPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldPubSubService service = new HelloWorldPubSubService(wsdl, serviceName);

        try {
            HelloWorldPubSubPort greeter = service.getPort(portName, HelloWorldPubSubPort.class);
            for (int idx = 0; idx < 5; idx++) {
                greeter.greetMeOneWay("JMS:PubSub:Milestone-" + idx);
            }
            //Give some time to complete one-way calls.
            Thread.sleep(50L);
            ((java.io.Closeable)greeter).close();
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
        String wsdlString2 = "classpath:wsdl/jms_test.wsdl";
        broker.updateWsdl((Bus)ctx.getBean("cxf"), wsdlString2);
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
        ctx.close();
        BusFactory.setDefaultBus(getBus());
        BusFactory.setThreadDefaultBus(getBus());
    }
    
    @Test
    public void testOneWayQueueConnection() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldOneWayQueueService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldOneWayQueuePort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldOneWayQueueService service = new HelloWorldOneWayQueueService(wsdl, serviceName);

        try {
            HelloWorldOneWayPort greeter = service.getPort(portName, HelloWorldOneWayPort.class,
                                                           new AddressingFeature(true, true));
            for (int idx = 0; idx < 5; idx++) {
                greeter.greetMeOneWay("JMS:Queue:Milestone-" + idx);
            }
            //Give some time to complete one-way calls.
            Thread.sleep(100L);
            ((java.io.Closeable)greeter).close();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    @Ignore // FIXME
    public void testQueueDecoupledOneWaysConnection() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", 
                                      "HelloWorldQueueDecoupledOneWaysService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldQueueDecoupledOneWaysPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        broker.updateWsdl(getBus(), "testutils/jms_test.wsdl");

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
            ((java.io.Closeable)greeter).close();
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
    public void testQueueOneWaySpecCompliantConnection() throws Throwable {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldQueueDecoupledOneWaysService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldQueueDecoupledOneWaysPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        assertNotNull(wsdl);
        String wsdlString2 = "testutils/jms_test.wsdl";
        broker.updateWsdl(getBus(), wsdlString2);

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
                    throw requestServant.getException();
                } else {
                    fail("The decoupled one-way reply was sent");
                }
            }
            ((java.io.Closeable)greeter).close();
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (requestEndpoint != null) {
                requestEndpoint.stop();
            }
        }

    }

    @Test
    public void testContextPropogation() throws Exception {
        final String testReturnPropertyName = "Test_Prop";
        final String testIgnoredPropertyName = "Test_Prop_No_Return";
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldService");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldService service = new HelloWorldService(wsdl, serviceName);

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
        JMSMTOMService service = new JMSMTOMService(wsdl, serviceName);

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
    public void testOutMTOM() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/jms_mtom", "JMSMTOMService");
        QName portName = new QName("http://cxf.apache.org/jms_mtom", "MTOMPort");

        URL wsdl = getWSDLURL("/wsdl/jms_test_mtom.wsdl");
        JMSOutMTOMService service = new JMSOutMTOMService(wsdl, serviceName);

        JMSMTOMPortType mtom = service.getPort(portName, JMSMTOMPortType.class);
        URL fileURL = this.getClass().getResource("/org/apache/cxf/systest/jms/JMSClientServerTest.class");
        DataHandler handler1 = new DataHandler(fileURL);
        int size = handler1.getInputStream().available();
        DataHandler ret = mtom.testOutMtom();
        
        byte bytes[] = IOUtils.readBytesFromStream(ret.getInputStream());
        assertEquals("The response file is not same with the original file.", size, bytes.length);
    }
    
    @Test
    public void testSpecJMS() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/jms_greeter", "JMSGreeterService");
        QName portName = new QName("http://cxf.apache.org/jms_greeter", "GreeterPort");
        URL wsdl = getWSDLURL("/wsdl/jms_spec_test.wsdl");
        JMSGreeterService service = new JMSGreeterService(wsdl, serviceName);

        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        JMSGreeterPortType greeter = service.getPort(portName, JMSGreeterPortType.class);
        for (int idx = 0; idx < 5; idx++) {

            greeter.greetMeOneWay("test String");

            String greeting = greeter.greetMe("Milestone-" + idx);
            assertNotNull("no response received from service", greeting);
            String exResponse = response1 + idx;
            assertEquals(exResponse, greeting);

            String reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals(response2, reply);
        }
    }
    
    @Test
    public void testWsdlExtensionSpecJMS() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/jms_greeter", "JMSGreeterService");
        QName portName = new QName("http://cxf.apache.org/jms_greeter", "GreeterPort");
        URL wsdl = getWSDLURL("/wsdl/jms_spec_test.wsdl");
        assertNotNull(wsdl);

        JMSGreeterService service = new JMSGreeterService(wsdl, serviceName);
        assertNotNull(service);

        String response = new String("Bonjour");
        try {
            JMSGreeterPortType greeter = service.getPort(portName, JMSGreeterPortType.class);
            Map<String, Object> requestContext = ((BindingProvider)greeter).getRequestContext();
            JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
            requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);
            
            String reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals(response, reply);
            
            requestContext = ((BindingProvider)greeter).getRequestContext();
            requestHeader = (JMSMessageHeadersType)requestContext
                .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
            assertEquals(requestHeader.getSOAPJMSBindingVersion(), "1.0");
            assertEquals(requestHeader.getSOAPJMSSOAPAction(), "\"test\"");
            assertEquals(requestHeader.getTimeToLive(), 3000);
            assertEquals(requestHeader.getJMSDeliveryMode(), DeliveryMode.PERSISTENT);
            assertEquals(requestHeader.getJMSPriority(), 7);
            
            Map<String, Object> responseContext = ((BindingProvider)greeter).getResponseContext();
            JMSMessageHeadersType responseHeader = (JMSMessageHeadersType)responseContext
                .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
            assertEquals(responseHeader.getSOAPJMSBindingVersion(), "1.0");
            assertEquals(responseHeader.getSOAPJMSSOAPAction(), null);
            assertEquals(responseHeader.getJMSDeliveryMode(), DeliveryMode.PERSISTENT);
            assertEquals(responseHeader.getJMSPriority(), 7);
            
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    public void testWsdlExtensionSpecJMSPortError() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/jms_greeter", "JMSGreeterService2");
        QName portName = new QName("http://cxf.apache.org/jms_greeter", "GreeterPort2");
        URL wsdl = getWSDLURL("/wsdl/jms_spec_test.wsdl");
        assertNotNull(wsdl);

        JMSGreeterService2 service = new JMSGreeterService2(wsdl, serviceName);
        assertNotNull(service);

        String response = new String("Bonjour");
        JMSGreeterPortType greeter = service.getPort(portName, JMSGreeterPortType.class);    
        String reply = greeter.sayHi();
        assertNotNull("no response received from service", reply);
        assertEquals(response, reply); 
    }
    
    @Test 
    public void testSpecNoWsdlService() throws Exception {
        specNoWsdlService(null);
    }
    
    @Test
    public void testSpecNoWsdlServiceWithDifferentMessageType() throws Exception {
        specNoWsdlService("text");
        specNoWsdlService("byte");
        specNoWsdlService("binary");
    }
    
    private void specNoWsdlService(String messageType) throws Exception {
        String address = "jms:jndi:dynamicQueues/test.cxf.jmstransport.queue3"
            + "?jndiInitialContextFactory"
            + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
            + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL=" 
            + broker.getEncodedBrokerURL();
        if (messageType != null) {
            address = address + "&messageType=" + messageType;
        }

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setTransportId(JMSSpecConstants.SOAP_JMS_SPECIFICATION_TRANSPORTID);
        factory.setServiceClass(Hello.class);
        factory.setAddress(address);
        Hello client = (Hello)factory.create();
        String reply = client.sayHi(" HI");
        assertEquals(reply, "get HI");
    }
    
    @Test
    public void testBindingVersionError() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/jms_greeter", "JMSGreeterService");
        QName portName = new QName("http://cxf.apache.org/jms_greeter", "GreeterPort");
        URL wsdl = getWSDLURL("/wsdl/jms_spec_test.wsdl");

        JMSGreeterService service = new JMSGreeterService(wsdl, serviceName);

        JMSGreeterPortType greeter = service.getPort(portName, JMSGreeterPortType.class);
        BindingProvider  bp = (BindingProvider)greeter;
        
        Map<String, Object> requestContext = bp.getRequestContext();
        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestHeader.setSOAPJMSBindingVersion("0.3");
        requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);
 
        try {
            greeter.greetMe("Milestone-");
            fail("Should have thrown a fault");
        } catch (SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("0.3"));
            Map<String, Object> responseContext = bp.getResponseContext();
            JMSMessageHeadersType responseHdr = 
                 (JMSMessageHeadersType)responseContext.get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
            if (responseHdr == null) {
                fail("response Header should not be null");
            }
            assertTrue(responseHdr.isSOAPJMSIsFault());
        }

    }
    
    @Test
    public void testReplyToConfig() throws Exception {
        JMSEndpoint endpoint = new JMSEndpoint();
        endpoint.setJndiInitialContextFactory("org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        endpoint.setJndiURL(broker.getBrokerURL());
        endpoint.setJndiConnectionFactoryName("ConnectionFactory");

        final JMSConfiguration jmsConfig = new JMSConfiguration();        
        JndiTemplate jt = new JndiTemplate();
        jt.setEnvironment(JMSOldConfigHolder.getInitialContextEnv(endpoint));
        
        JNDIConfiguration jndiConfig = new JNDIConfiguration();
        jndiConfig.setJndiConnectionFactoryName(endpoint.getJndiConnectionFactoryName());
        jmsConfig.setJndiTemplate(jt);
        jmsConfig.setJndiConfig(jndiConfig);
        
        TestReceiver receiver = new TestReceiver(jmsConfig.getConnectionFactory(), 
                                                 "dynamicQueues/SoapService7.replyto.queue");
        receiver.setStaticReplyQueue("dynamicQueues/SoapService7.reply.queue");
        receiver.runAsync();
        
        QName serviceName = new QName("http://apache.org/hello_world_doc_lit", "SOAPService7");
        QName portName = new QName("http://apache.org/hello_world_doc_lit", "SoapPort7");
        URL wsdl = getWSDLURL("/wsdl/hello_world_doc_lit.wsdl");
        SOAPService7 service = new SOAPService7(wsdl, serviceName);        
        Greeter greeter = service.getPort(portName, Greeter.class);
        String name = "FooBar";
        String reply = greeter.greetMe(name);
        assertEquals(reply, "Hello " + name);
    }
}
