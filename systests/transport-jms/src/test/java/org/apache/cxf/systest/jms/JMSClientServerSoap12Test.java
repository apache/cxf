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
import java.util.HashMap;
import java.util.Map;


import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.transport.jms.AddressType;
import org.apache.cxf.transport.jms.JMSNamingPropertyType;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.PingMeFault;
import org.apache.hello_world_doc_lit.SOAPService2;

import org.junit.BeforeClass;
import org.junit.Test;

public class JMSClientServerSoap12Test extends AbstractBusClientServerTestBase {
    static final String JMS_PORT = EmbeddedJMSBrokerLauncher.PORT;
    static final String PORT = Soap12Server.PORT;
    
    private String wsdlString;
    
    @BeforeClass
    public static void startServers() throws Exception {
        Map<String, String> props = new HashMap<String, String>();                
        if (System.getProperty("org.apache.activemq.default.directory.prefix") != null) {
            props.put("org.apache.activemq.default.directory.prefix",
                      System.getProperty("org.apache.activemq.default.directory.prefix"));
        }
        props.put("java.util.logging.config.file", 
                  System.getProperty("java.util.logging.config.file"));
        
        assertTrue("server did not launch correctly", 
                   launchServer(EmbeddedJMSBrokerLauncher.class, props, null));

        assertTrue("server did not launch correctly", 
                   launchServer(Soap12Server.class));
    }
    
    public URL getWSDLURL(String s) throws Exception {
        URL u = getClass().getResource(s);
        wsdlString = u.toString().intern();
        EmbeddedJMSBrokerLauncher.updateWsdlExtensors(getBus(), wsdlString);
        System.gc();
        System.gc();
        return u;
    }
    public QName getServiceName(QName q) {
        return q;
    }
    public QName getPortName(QName q) {
        return q;
    }
    
    @Test
    public void testGzipEncodingWithJms() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        Bus bus = bf.createBus("org/apache/cxf/systest/jms/soap12Bus.xml");
        BusFactory.setDefaultBus(bus);
        QName serviceName = getServiceName(new QName("http://apache.org/hello_world_doc_lit", 
                                 "SOAPService8"));
        QName portName = getPortName(new QName("http://apache.org/hello_world_doc_lit", "SoapPort8"));
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
        bus.shutdown(true);
    }
}
