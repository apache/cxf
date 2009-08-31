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

package org.apache.cxf.systest.jaxws;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;





import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.hello_world_soap_http.any.Greeter;
import org.apache.hello_world_soap_http.any.SOAPService;
import org.apache.hello_world_soap_http.any_types.GreeterImpl;
import org.apache.hello_world_soap_http.any_types.SayHi.Port;
import org.junit.BeforeClass;
import org.junit.Test;

public final class AnyClientServerTest extends AbstractBusClientServerTestBase {

    static final Logger LOG = LogUtils.getLogger(AnyClientServerTest.class);
    private final QName serviceName = new QName("http://apache.org/hello_world_soap_http/any", 
                                                "SOAPService");

    public static class MyServer extends AbstractBusTestServerBase {

        protected void run() {
            Object implementor = new GreeterImpl();
            String address = "http://localhost:9000/SoapContext/SoapPort";
            Endpoint.publish(address, implementor);

        }

        public static void main(String[] args) {
            try {
                MyServer s = new MyServer();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                LOG.info("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(MyServer.class));
    }

    @Test
    public void testAny() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/any.wsdl");
        assertNotNull(wsdl);

        SOAPService ss = new SOAPService(wsdl, serviceName);
        Greeter port = ss.getSoapPort();

        List<Port> any = new ArrayList<Port>();
        Port anyPort = new Port();
        Port anyPort1 = new Port();
        JAXBElement<String> ele1 = new JAXBElement<String>(
            new QName("http://apache.org/hello_world_soap_http/other", "port"), 
            String.class, "hello");
        
        anyPort.setAny(ele1);
        JAXBElement<String> ele2 = new JAXBElement<String>(
            new QName("http://apache.org/hello_world_soap_http/other", "port"), 
            String.class, "Bon");
        anyPort1.setAny(ele2);
        
        any.add(anyPort);
        any.add(anyPort1);
        String rep = port.sayHi(any);
        assertEquals(rep, "helloBon");
    }
    
    @Test
    public void testList() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/any.wsdl");
        assertNotNull(wsdl);

        SOAPService ss = new SOAPService(wsdl, serviceName);
        Greeter port = ss.getSoapPort();
        List<org.apache.hello_world_soap_http.any_types.SayHi1.Port> list = 
                new ArrayList<org.apache.hello_world_soap_http.any_types.SayHi1.Port>();
        org.apache.hello_world_soap_http.any_types.SayHi1.Port port1 = 
            new org.apache.hello_world_soap_http.any_types.SayHi1.Port();
        port1.setRequestType("hello");
        org.apache.hello_world_soap_http.any_types.SayHi1.Port port2 = 
            new org.apache.hello_world_soap_http.any_types.SayHi1.Port();
        port2.setRequestType("Bon");
        list.add(port1);
        list.add(port2);
        String rep = port.sayHi1(list);
        assertEquals(rep, "helloBon");
    }
}
