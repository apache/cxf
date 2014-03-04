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
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.jms.ConnectionFactoryFeature;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.PingMeFault;
import org.apache.hello_world_doc_lit.SOAPService2;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JMSClientServerSoap12Test {
    private static final String BROKER_URI = "vm://JMSClientServerSoap12Test?broker.persistent=false";
    private static Bus bus;
    private static ConnectionFactoryFeature cff;
    
    @BeforeClass
    public static void startServers() throws Exception {
        bus = BusFactory.getDefaultBus();
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URI);
        PooledConnectionFactory cfp = new PooledConnectionFactory(cf);
        cff = new ConnectionFactoryFeature(cfp);

        EndpointImpl ep = (EndpointImpl)Endpoint.create(new GreeterImplSoap12());
        ep.setBus(bus);
        ep.getFeatures().add(cff);
        ep.publish("jms:queue:routertest.SOAPService2Q.text");
    }
    
    @AfterClass
    public static void stopServers() throws Exception {
        bus.shutdown(false);
    }
    
    public URL getWSDLURL(String s) throws Exception {
        return getClass().getResource(s);
    }
    
    @Test
    public void testGzipEncodingWithJms() throws Exception {
        QName serviceName = new QName("http://apache.org/hello_world_doc_lit", 
                                 "SOAPService8");
        QName portName = new QName("http://apache.org/hello_world_doc_lit", "SoapPort8");
        URL wsdl = getWSDLURL("/wsdl/hello_world_doc_lit.wsdl");
        SOAPService2 service = new SOAPService2(wsdl, serviceName);
        String response1 = new String("Hello Milestone-");
        Greeter greeter = service.getPort(portName, Greeter.class, cff);

        for (int idx = 0; idx < 5; idx++) {

            greeter.greetMeOneWay("test String");

            String greeting = greeter.greetMe("Milestone-" + idx);
            Assert.assertEquals(response1 + idx, greeting);

            String reply = greeter.sayHi();
            Assert.assertEquals("Bonjour", reply);

            try {
                greeter.pingMe();
                Assert.fail("Should have thrown FaultException");
            } catch (PingMeFault ex) {
                Assert.assertNotNull(ex.getFaultInfo());
            }

        }
        ((Closeable)greeter).close();
    }
}
