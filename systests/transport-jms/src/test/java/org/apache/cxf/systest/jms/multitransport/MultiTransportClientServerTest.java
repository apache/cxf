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

package org.apache.cxf.systest.jms.multitransport;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Endpoint;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.junit.EmbeddedActiveMQResource;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.jms.ConnectionFactoryFeature;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.HTTPGreeterImpl;
import org.apache.hello_world_doc_lit.JMSGreeterImpl;
import org.apache.hello_world_doc_lit.MultiTransportService;
import org.apache.hello_world_doc_lit.PingMeFault;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Show that one service can have two ports with different transports
 */
public class MultiTransportClientServerTest {
    @ClassRule public static EmbeddedActiveMQResource server = new EmbeddedActiveMQResource(0);

    private static final String PORT = TestUtil.getNewPortNumber(MultiTransportClientServerTest.class);
    private static QName serviceName = new QName("http://apache.org/hello_world_doc_lit",
                                                "MultiTransportService");

    private static ConnectionFactoryFeature cff;
    private static Bus bus;

    @BeforeClass
    public static void startServers() throws Exception {
        bus = BusFactory.getDefaultBus();
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        cff = new ConnectionFactoryFeature(cf);
        String address = "http://localhost:" + PORT + "/SOAPDocLitService/SoapPort";
        Endpoint.publish(address, new HTTPGreeterImpl());
        EndpointImpl ep1 = (EndpointImpl)Endpoint.create(new JMSGreeterImpl());
        ep1.setBus(bus);
        ep1.getFeatures().add(cff);
        ep1.publish();
    }

    @AfterClass
    public static void stopServers() throws Exception {
        bus.shutdown(false);
    }

    @Test
    public void testMultiTransportInOneService() throws Exception {

        QName portName1 = new QName("http://apache.org/hello_world_doc_lit", "HttpPort");
        QName portName2 = new QName("http://apache.org/hello_world_doc_lit", "JMSPort");
        URL wsdl = getClass().getResource("/wsdl/hello_world_doc_lit.wsdl");
        Assert.assertNotNull(wsdl);
        MultiTransportService service = new MultiTransportService(wsdl, serviceName);

        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        Greeter greeter = service.getPort(portName1, Greeter.class);
        TestUtil.updateAddressPort(greeter, PORT);
        for (int idx = 0; idx < 5; idx++) {
            String greeting = greeter.greetMe("Milestone-" + idx);
            Assert.assertNotNull("no response received from service", greeting);
            String exResponse = response1 + idx;
            Assert.assertEquals(exResponse, greeting);

            String reply = greeter.sayHi();
            Assert.assertNotNull("no response received from service", reply);
            Assert.assertEquals(response2, reply);

            try {
                greeter.pingMe();
                Assert.fail("Should have thrown FaultException");
            } catch (PingMeFault ex) {
                Assert.assertNotNull(ex.getFaultInfo());
            }

        }
        ((java.io.Closeable)greeter).close();
        greeter = null;

        greeter = service.getPort(portName2, Greeter.class, cff);
        for (int idx = 0; idx < 5; idx++) {
            String greeting = greeter.greetMe("Milestone-" + idx);
            Assert.assertNotNull("no response received from service", greeting);
            String exResponse = response1 + idx;
            Assert.assertEquals(exResponse, greeting);

            String reply = greeter.sayHi();
            Assert.assertNotNull("no response received from service", reply);
            Assert.assertEquals(response2, reply);

            try {
                greeter.pingMe();
                Assert.fail("Should have thrown FaultException");
            } catch (PingMeFault ex) {
                Assert.assertNotNull(ex.getFaultInfo());
            }

        }
        ((java.io.Closeable)greeter).close();
    }

}
