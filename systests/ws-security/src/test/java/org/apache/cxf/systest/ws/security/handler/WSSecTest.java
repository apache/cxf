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
package org.apache.cxf.systest.ws.security.handler;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WSSecTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("Server failed to launch", launchServer(Server.class));
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @Test
    public void testClientServer() throws Exception {

        Bus bus = new SpringBusFactory().createBus("org/apache/cxf/systest/ws/security/handler/client.xml");

        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        Service service = Service.create(new URL("http://localhost:" + PORT + "/wsse/HelloWorldWS?wsdl"),
                                         new QName("http://cxf.apache.org/wsse/handler/helloworld",
                                                   "HelloWorldImplService"));
        QName portName = new QName("http://cxf.apache.org/wsse/handler/helloworld", "HelloWorldPort");

        HelloWorld port = service.getPort(portName, HelloWorld.class);
        updateAddressPort(port, PORT);
        assertEquals("Hello CXF", port.sayHello("CXF"));

        bus.shutdown(true);
    }

}
