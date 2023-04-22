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
package org.apache.cxf.transport.http.netty.server.integration;

import java.net.URL;

import jakarta.xml.ws.Endpoint;
import org.apache.hello_world_soap_http.SOAPService;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.junit.Assert.assertNotNull;

/**
 * publish the service with SSL configuraiton with Spring
 */
public class SSLNettySpringServerTest extends SSLNettyServerTest {
    public static final String PORT = allocatePort(SSLNettySpringServerTest.class);

    static {
        System.setProperty("SSLNettySpringServerTest.port", PORT);
    }
    static ConfigurableApplicationContext context;

    @BeforeClass
    public static void start() throws Exception {
        context = new ClassPathXmlApplicationContext(
                "/org/apache/cxf/transport/http/netty/server/integration/ApplicationContext.xml");

        address = "https://localhost:" + PORT + "/SoapContext/SoapPort";
        ep = context.getBean("myEndpoint", Endpoint.class);

        URL wsdl = NettyServerTest.class.getResource("/wsdl/hello_world.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl);
        assertNotNull("Service is null", service);

        g = service.getSoapPort();
        assertNotNull("Port is null", g);
    }

    @AfterClass
    public static void stop() throws Exception {
        if (g != null) {
            ((java.io.Closeable)g).close();
        }
        if (ep != null) {
            ep.stop();
        }
        ep = null;
        if (context != null) {
            context.close();
        }
    }

}
