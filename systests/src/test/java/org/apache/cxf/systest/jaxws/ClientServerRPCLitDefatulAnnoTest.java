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

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientServerRPCLitDefatulAnnoTest extends AbstractClientServerTestBase {

    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            Object implementor = new HelloImpl();
            String address = "http://localhost:9091/hello";
            Endpoint.publish(address, implementor);
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @Test
    public void testBasicConnection() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/systest/jaxws/", "HelloService");
        HelloService service = new HelloService(getClass().getResource("/wsdl/others/hello.wsdl"), 
                                                serviceName);
        assertNotNull(service);
        Hello hello = service.getHelloPort();
        assertEquals("getSayHi", hello.sayHi("SayHi"));

    }
}
