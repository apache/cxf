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
package org.apache.cxf.jms.testsuite.testcases;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jms_greeter.JMSGreeterPortType;
import org.apache.cxf.jms_greeter.JMSGreeterService;
import org.apache.cxf.jms_greeter.JMSGreeterService2;
import org.apache.cxf.systest.jms.Hello;
import org.apache.cxf.systest.jms.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.transport.jms.JMSConfigFactory;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;
import org.apache.cxf.transport.jms.uri.JMSEndpoint;
import org.apache.cxf.transport.jms.util.TestReceiver;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.SOAPService7;
import org.junit.BeforeClass;
import org.junit.Test;

public class SoapJmsSpecTest extends AbstractBusClientServerTestBase {

    private static EmbeddedJMSBrokerLauncher broker;
    private String wsdlString;
    
    public URL getWSDLURL(String s) throws Exception {
        URL u = getClass().getResource(s);
        if (u == null) {
            throw new IllegalArgumentException("WSDL classpath resource not found " + s);
        }
        wsdlString = u.toString().intern();
        broker.updateWsdl(getBus(), wsdlString);
        return u;
    }


    @BeforeClass
    public static void startServers() throws Exception {
        broker = new EmbeddedJMSBrokerLauncher();
        launchServer(broker);
        launchServer(new Server(broker));
        createStaticBus();
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
            + "?jndiInitialContextFactory=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
            + "&jndiConnectionFactoryName=ConnectionFactory&jndiURL=" + broker.getEncodedBrokerURL();
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
        JMSEndpoint endpoint = new JMSEndpoint("");
        endpoint.setJndiInitialContextFactory("org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        endpoint.setJndiURL(broker.getBrokerURL());
        endpoint.setJndiConnectionFactoryName("ConnectionFactory");

        final JMSConfiguration jmsConfig = new JMSConfiguration();        
        jmsConfig.setJndiEnvironment(JMSConfigFactory.getInitialContextEnv(endpoint));
        jmsConfig.setConnectionFactoryName(endpoint.getJndiConnectionFactoryName());
        
        TestReceiver receiver = new TestReceiver(jmsConfig.getConnectionFactory(), 
                                                 "dynamicQueues/SoapService7.replyto.queue", false);
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
