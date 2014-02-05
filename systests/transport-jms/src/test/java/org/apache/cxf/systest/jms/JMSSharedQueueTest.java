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

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.hello_world_jms.HelloWorldPortType;
import org.apache.cxf.hello_world_jms.HelloWorldServiceAppCorrelationID;
import org.apache.cxf.hello_world_jms.HelloWorldServiceAppCorrelationIDNoPrefix;
import org.apache.cxf.hello_world_jms.HelloWorldServiceAppCorrelationIDStaticPrefix;
import org.apache.cxf.hello_world_jms.HelloWorldServiceRuntimeCorrelationIDDynamicPrefix;
import org.apache.cxf.hello_world_jms.HelloWorldServiceRuntimeCorrelationIDStaticPrefix;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;
import org.junit.BeforeClass;
import org.junit.Test;

public class JMSSharedQueueTest extends AbstractBusClientServerTestBase {
    private static final String BROKER_URI = "vm://SharedQueueTest" 
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

    private interface CorrelationIDFactory {
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
                for (int idx = 0; idx < 5; idx++) {
                    callGreetMe();
                }
            } catch (Throwable e) {
                ex = e;
            }
        }

        private void callGreetMe() {
            BindingProvider  bp = (BindingProvider)port;
            Map<String, Object> requestContext = bp.getRequestContext();
            JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
            requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);
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
    }
    
    private void executeAsync(ClientRunnable... clients) throws Throwable {
        executeAsync(Arrays.asList(clients));
    }
    
    private void executeAsync(Collection<ClientRunnable> clients) throws Throwable {
        ExecutorService executor = Executors.newCachedThreadPool();
        for (ClientRunnable client : clients) {
            executor.execute(client);
        }
        executor.shutdown();
        for (ClientRunnable client : clients) {
            if (client.getException() != null) {
                throw client.getException();
            }
        }
    }
    
    @Test
    public void testTwoWayQueueAppCorrelationID() throws Throwable {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldServiceAppCorrelationID");
        QName portNameEng = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPortAppCorrelationIDEng");
        QName portNameSales = 
            new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPortAppCorrelationIDSales");

        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
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
     
        executeAsync(engClient, salesClient);
    }
    
    @Test
    public void testTwoWayQueueAppCorrelationIDStaticPrefix() throws Throwable {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldServiceAppCorrelationIDStaticPrefix");
        QName portNameEng = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldPortAppCorrelationIDStaticPrefixEng");
        QName portNameSales = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldPortAppCorrelationIDStaticPrefixSales");

        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldServiceAppCorrelationIDStaticPrefix service = 
            new HelloWorldServiceAppCorrelationIDStaticPrefix(wsdl, serviceName);

        ClientRunnable engClient = 
            new ClientRunnable(service.getPort(portNameEng, HelloWorldPortType.class));
        
        ClientRunnable salesClient = 
             new ClientRunnable(service.getPort(portNameSales, HelloWorldPortType.class));

        executeAsync(engClient, salesClient);
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
    public void testTwoWayQueueAppCorrelationIDNoPrefix() throws Throwable {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldServiceAppCorrelationIDNoPrefix");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldPortAppCorrelationIDNoPrefix");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldServiceAppCorrelationIDNoPrefix service = 
            new HelloWorldServiceAppCorrelationIDNoPrefix(wsdl, serviceName);
        
        HelloWorldPortType port = service.getPort(portName, HelloWorldPortType.class);

        Collection<ClientRunnable> clients = new ArrayList<ClientRunnable>();
        for (int i = 0; i < 1; ++i) {
            clients.add(new ClientRunnable(port));            
        }
        executeAsync(clients);
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
    public void testTwoWayQueueRuntimeCorrelationIDStaticPrefix() throws Throwable {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldServiceRuntimeCorrelationIDStaticPrefix");
        
        QName portNameEng = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldPortRuntimeCorrelationIDStaticPrefixEng");
        QName portNameSales = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldPortRuntimeCorrelationIDStaticPrefixSales");

        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldServiceRuntimeCorrelationIDStaticPrefix service = 
            new HelloWorldServiceRuntimeCorrelationIDStaticPrefix(wsdl, serviceName);
        
        HelloWorldPortType portEng = service.getPort(portNameEng, HelloWorldPortType.class);
        HelloWorldPortType portSales = service.getPort(portNameSales, HelloWorldPortType.class);

        Collection<ClientRunnable> clients = new ArrayList<ClientRunnable>();
        for (int i = 0; i < 10; ++i) {
            clients.add(new ClientRunnable(portEng, "com.mycompany.eng:"));
            clients.add(new ClientRunnable(portSales, "com.mycompany.sales:"));
        }
        executeAsync(clients);
    }



    @Test
    public void testTwoWayQueueRuntimeCorrelationDynamicPrefix() throws Throwable {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldServiceRuntimeCorrelationIDDynamicPrefix");
        
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", 
            "HelloWorldPortRuntimeCorrelationIDDynamicPrefix");
        
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldServiceRuntimeCorrelationIDDynamicPrefix service = 
            new HelloWorldServiceRuntimeCorrelationIDDynamicPrefix(wsdl, serviceName);
        HelloWorldPortType port = service.getPort(portName, HelloWorldPortType.class);

        Collection<ClientRunnable> clients = new ArrayList<ClientRunnable>();        
        for (int i = 0; i < 10; ++i) {
            clients.add(new ClientRunnable(port));
        }
        executeAsync(clients);
    }
    
}
