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
package org.apache.cxf.systest.jms.tx;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;

import javax.jms.ConnectionFactory;
import javax.xml.namespace.QName;

import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.transport.jms.JMSConfigFeature;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.PingMeFault;
import org.apache.hello_world_doc_lit.SOAPService2;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.connection.JmsTransactionManager;


public class JMSTransactionClientServerTest extends AbstractBusClientServerTestBase {
    static EmbeddedJMSBrokerLauncher broker;

    public static class Server extends AbstractBusTestServerBase {
    
        protected void run()  {
            // create the application context
            ClassPathXmlApplicationContext context = 
                new ClassPathXmlApplicationContext("org/apache/cxf/systest/jms/tx/jms_server_config.xml");
            context.start();
            
            EndpointImpl endpoint = new EndpointImpl(new GreeterImplWithTransaction());
            endpoint.setAddress("jms://");
            JMSConfiguration jmsConfig = new JMSConfiguration();
    
            ConnectionFactory connectionFactory
                = context.getBean("jmsConnectionFactory", ConnectionFactory.class);
            jmsConfig.setConnectionFactory(connectionFactory);
            jmsConfig.setTargetDestination("greeter.queue.noaop");
            jmsConfig.setSessionTransacted(true);
            jmsConfig.setPubSubDomain(false);
            jmsConfig.setUseJms11(true);
            jmsConfig.setTransactionManager(new JmsTransactionManager(connectionFactory));
            jmsConfig.setCacheLevel(3);
    
            JMSConfigFeature jmsConfigFeature = new JMSConfigFeature();
            jmsConfigFeature.setJmsConfig(jmsConfig);
            endpoint.getFeatures().add(jmsConfigFeature);
            endpoint.publish();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        broker = new EmbeddedJMSBrokerLauncher("vm://JMSTransactionClientServerTest");
        System.setProperty("EmbeddedBrokerURL", broker.getBrokerURL());
        launchServer(broker);
        launchServer(new Server());
        createStaticBus();
    }
    @AfterClass
    public static void clearProperty() {
        System.clearProperty("EmbeddedBrokerURL");
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
        String wsdlString = wsdl.toString();
        SOAPService2 service = new SOAPService2(wsdl, serviceName);
        broker.updateWsdl(getBus(), wsdlString);
        assertNotNull(service);

        Greeter greeter = service.getPort(portName, Greeter.class);
        doService(greeter, true);
    }
    @Test
    public void testNonAopTransaction() throws Exception {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Greeter.class);
        factory.setAddress("jms://");

        JMSConfiguration jmsConfig = new JMSConfiguration();
        ConnectionFactory connectionFactory
            = new org.apache.activemq.ActiveMQConnectionFactory(broker.getBrokerURL());
        jmsConfig.setConnectionFactory(connectionFactory);
        jmsConfig.setTargetDestination("greeter.queue.noaop");
        jmsConfig.setPubSubDomain(false);
        jmsConfig.setUseJms11(true);

        JMSConfigFeature jmsConfigFeature = new JMSConfigFeature();
        jmsConfigFeature.setJmsConfig(jmsConfig);
        factory.getFeatures().add(jmsConfigFeature);

        Greeter greeter = (Greeter)factory.create();
        doService(greeter, false);
    }    
    public void doService(Greeter greeter, boolean doEx) throws Exception {

        String response1 = new String("Hello ");
        
        try {
                          
            String greeting = greeter.greetMe("Good guy");
            assertNotNull("No response received from service", greeting);
            String exResponse = response1 + "Good guy";
            assertEquals("Get unexcpeted result", exResponse, greeting);

            greeting = greeter.greetMe("Bad guy");
            assertNotNull("No response received from service", greeting);
            exResponse = response1 + "[Bad guy]";
            assertEquals("Get unexcpeted result", exResponse, greeting);
            
            if (doEx) {
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

}
