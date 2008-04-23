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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;


import org.apache.cxf.hello_world_jms.BadRecordLitFault;
import org.apache.cxf.hello_world_jms.HWByteMsgService;
import org.apache.cxf.hello_world_jms.HelloWorldOneWayPort;
import org.apache.cxf.hello_world_jms.HelloWorldOneWayQueueService;
import org.apache.cxf.hello_world_jms.HelloWorldPortType;
import org.apache.cxf.hello_world_jms.HelloWorldPubSubPort;
import org.apache.cxf.hello_world_jms.HelloWorldPubSubService;
import org.apache.cxf.hello_world_jms.HelloWorldService;
import org.apache.cxf.hello_world_jms.NoSuchCodeLitFault;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;
import org.apache.cxf.transport.jms.JMSPropertyType;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.PingMeFault;
import org.apache.hello_world_doc_lit.SOAPService2;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JMSClientServerTest extends AbstractBusClientServerTestBase {
    
    protected static boolean serversStarted;

    @Before
    public void startServers() throws Exception {
        if (serversStarted) {
            return;
        }
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
        serversStarted = true;
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
            for (int idx = 0; idx < 5; idx++) {
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
            Thread.sleep(100L);
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
            InvocationHandler handler  = Proxy.getInvocationHandler(greeter);
            BindingProvider  bp = null;
            
            if (handler instanceof BindingProvider) {
                bp = (BindingProvider)handler;
                //System.out.println(bp.toString());
                Map<String, Object> requestContext = bp.getRequestContext();
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
            } 
 
            String greeting = greeter.greetMe("Milestone-");
            assertNotNull("no response received from service", greeting);

            assertEquals("Hello Milestone-", greeting);

            if (bp != null) {
                Map<String, Object> responseContext = bp.getResponseContext();
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
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
}
