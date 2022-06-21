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

package org.apache.cxf.systest.soap_udp;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.BusFactory;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_soap_http.Greeter;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SoapUDPTest extends AbstractBusClientServerTestBase {
    private static final String PORT = Server.PORT;
    private final QName serviceName = new QName("http://apache.org/hello_world_soap_http",
                                            "SOAPService");
    private final QName localPortName = new QName("http://apache.org/hello_world_soap_http",
                                            "UDPPortName");


    @BeforeClass
    public static void startServers() throws Exception {
        staticBus = BusFactory.getDefaultBus();
        new LoggingFeature().initialize(staticBus);
        BusFactory.setThreadDefaultBus(staticBus);
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testSOAPUDP() {
        BusFactory.setThreadDefaultBus(staticBus);
        Service service = Service.create(serviceName);
        service.addPort(localPortName, "http://schemas.xmlsoap.org/soap/",
                        "soap.udp://localhost:" + PORT);
        Greeter greeter = service.getPort(localPortName, Greeter.class);

        String reply = greeter.greetMe("test");
        assertEquals("Hello test", reply);
        reply = greeter.sayHi();
        assertNotNull("no response received from service", reply);
        assertEquals("Bonjour", reply);
    }



}
