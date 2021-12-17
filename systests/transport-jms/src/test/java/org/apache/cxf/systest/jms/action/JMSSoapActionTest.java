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
package org.apache.cxf.systest.jms.action;


import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.BusFactory;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.hello_world_jms.HelloWorldPortType;
import org.apache.cxf.hello_world_jms.HelloWorldService;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * Some tests for sending a SOAP Action with JMS
 */
public class JMSSoapActionTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(JMSSoapActionTest.class);

    private static EmbeddedJMSBrokerLauncher broker;
    private List<String> wsdlStrings = new ArrayList<>();

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
    public void testSayHi() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldServiceSoapAction");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldService service = new HelloWorldService(wsdl, serviceName);

        String response = new String("Bonjour");
        HelloWorldPortType greeter = service.getPort(portName, HelloWorldPortType.class);

        ClientProxy.getClient(greeter).getOutInterceptors().add(new LoggingOutInterceptor());
        ClientProxy.getClient(greeter).getOutInterceptors().add(new LoggingInInterceptor());

        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_1"
        );

        String reply = greeter.sayHi();
        assertNotNull("no response received from service", reply);
        assertEquals(response, reply);

        ((java.io.Closeable)greeter).close();
    }

    
    
    @Test
    public void testSayHi2() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldServiceSoapAction");
        QName portName = new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort");
        URL wsdl = getWSDLURL("/wsdl/jms_test.wsdl");
        HelloWorldService service = new HelloWorldService(wsdl, serviceName);

        HelloWorldPortType greeter = service.getPort(portName, HelloWorldPortType.class);

        ClientProxy.getClient(greeter).getOutInterceptors().add(new LoggingOutInterceptor());
        ClientProxy.getClient(greeter).getOutInterceptors().add(new LoggingInInterceptor());

        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_2"
        );

        try {
            greeter.sayHi();
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }
            
        ((java.io.Closeable)greeter).close();
    }


}
