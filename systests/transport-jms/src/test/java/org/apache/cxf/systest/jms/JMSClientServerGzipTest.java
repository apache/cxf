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

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.transport.common.gzip.GZIPFeature;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.PingMeFault;
import org.apache.hello_world_doc_lit.SOAPService2;
import org.junit.BeforeClass;
import org.junit.Test;

public class JMSClientServerGzipTest extends AbstractBusClientServerTestBase {
    private static EmbeddedJMSBrokerLauncher broker;
    private String wsdlString;
    
    
    public static class GzipServer extends AbstractBusTestServerBase {
        EndpointImpl ep;
        protected void run()  {
            Object impleDoc = new GreeterImplDoc();
            Bus bus = BusFactory.getDefaultBus();
            bus.getFeatures().add(new GZIPFeature());
            setBus(bus);
            broker.updateWsdl(bus, "testutils/hello_world_doc_lit.wsdl");
            ep = (EndpointImpl)Endpoint.create(null, impleDoc);
            ep.getFeatures().add(new GZIPFeature());
            ep.publish();
        }
        public void tearDown() {
            ep.stop();
        }
    }
    @BeforeClass
    public static void startServers() throws Exception {
        broker = new EmbeddedJMSBrokerLauncher();
        launchServer(broker);
        assertTrue("server did not launch correctly", 
                   launchServer(GzipServer.class, true));
        
    }
    
    public URL getWSDLURL(String s) throws Exception {
        URL u = getClass().getResource(s);
        wsdlString = u.toString().intern();
        broker.updateWsdl(getBus(), wsdlString);
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
        QName serviceName = getServiceName(new QName("http://apache.org/hello_world_doc_lit", 
                                 "SOAPService2"));
        QName portName = getPortName(new QName("http://apache.org/hello_world_doc_lit", "SoapPort2"));
        URL wsdl = getWSDLURL("/wsdl/hello_world_doc_lit.wsdl");
        assertNotNull(wsdl);
        SOAPService2 service = new SOAPService2(wsdl, serviceName);

        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        try {
            Greeter greeter = service.getPort(portName, Greeter.class, new GZIPFeature());

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
}
