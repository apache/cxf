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

package org.apache.cxf.systest.http_jetty;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;

import org.junit.Before;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests thread pool config.
 */

public class JettyBasicAuthTest extends AbstractClientServerTestBase {
    private static final String ADDRESS = JettyBasicAuthServer.ADDRESS;
    private static final QName SERVICE_NAME =
        new QName("http://apache.org/hello_world_soap_http", "SOAPServiceAddressing");

    private Greeter greeter;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(JettyBasicAuthServer.class, true));
    }

    @Before
    public void setUp() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        greeter = new SOAPService(wsdl, SERVICE_NAME).getPort(Greeter.class);
        BindingProvider bp = (BindingProvider)greeter;
        ClientProxy.getClient(greeter).getInInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(greeter).getOutInterceptors().add(new LoggingOutInterceptor());
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                   ADDRESS);
        bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "ffang");
        bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "pswd");
    }

    @org.junit.Test
    public void testBasicAuth() throws Exception {
        assertEquals("Hello Alice", greeter.greetMe("Alice"));
    }

    @org.junit.Test
    public void testGetWSDL() throws Exception {
        BusFactory bf = BusFactory.newInstance();
        Bus bus = bf.createBus();
        bus.getInInterceptors().add(new LoggingInInterceptor());
        bus.getOutInterceptors().add(new LoggingOutInterceptor());

        MyHTTPConduitConfigurer myHttpConduitConfig = new MyHTTPConduitConfigurer();
        bus.setExtension(myHttpConduitConfig, HTTPConduitConfigurer.class);
        JaxWsDynamicClientFactory factory = JaxWsDynamicClientFactory.newInstance(bus);
        factory.createClient(ADDRESS + "?wsdl");
    }

    private static final class MyHTTPConduitConfigurer implements HTTPConduitConfigurer {
        public void configure(String name, String address, HTTPConduit c) {

            AuthorizationPolicy authorizationPolicy = new AuthorizationPolicy();

            authorizationPolicy.setUserName("ffang");
            authorizationPolicy.setPassword("pswd");
            authorizationPolicy.setAuthorizationType("Basic");
            c.setAuthorization(authorizationPolicy);
        }
    }
}
