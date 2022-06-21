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
package org.apache.cxf.systest.jaxrs;

import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.systest.jaxrs.jaxws.HelloWorld;
import org.apache.cxf.systest.jaxrs.jaxws.User;
import org.apache.cxf.systest.jaxrs.jaxws.UserImpl;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JAXRSSoapRestBlueprintTest extends AbstractClientServerTestBase {
    public static final int PORT = BlueprintServer.PORT;
    @BeforeClass
    public static void beforeClass() throws Exception {
        // must be 'in-process' to communicate with inner class in single JVM
        // and to spawn class SpringServer w/o using main() method
        launchServer(BlueprintServer.class, true);
    }

    @Ignore
    public static class BlueprintServer extends AbstractSpringServer {
        public static final int PORT = allocatePortAsInt(BlueprintServer.class);
        public BlueprintServer() {
            super("/jaxrs_soap_blueprint", "/bp", PORT);
        }
    }
    @Test
    public void testHelloRest() throws Exception {
        String address = "http://localhost:" + PORT + "/bp/services/hello-rest";

        HelloWorld service = JAXRSClientFactory.create(address, HelloWorld.class);
        useHelloService(service);
    }

    @Test
    public void testHelloSoap() throws Exception {
        final QName serviceName = new QName("http://hello.com", "HelloWorld");
        final QName portName = new QName("http://hello.com", "HelloWorldPort");
        final String address = "http://localhost:" + PORT + "/bp/services/hello-soap";

        Service service = Service.create(serviceName);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, address);

        HelloWorld hw = service.getPort(HelloWorld.class);

        useHelloService(hw);
    }
    private void useHelloService(HelloWorld service) {
        assertEquals("Hello Barry", service.sayHi("Barry"));
        assertEquals("Hello Fred", service.sayHiToUser(new UserImpl("Fred")));

        Map<Integer, User> users = service.getUsers();
        assertEquals(1, users.size());
        assertEquals("Fred", users.entrySet().iterator().next().getValue().getName());

        users = service.echoUsers(users);
        assertEquals(1, users.size());
        assertEquals("Fred", users.entrySet().iterator().next().getValue().getName());

        assertEquals(1, service.clearUsers());
        assertEquals(0, service.clearUsers());
    }

}
