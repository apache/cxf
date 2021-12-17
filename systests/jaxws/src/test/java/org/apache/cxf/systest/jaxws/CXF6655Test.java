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

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Endpoint;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CXF6655Test extends AbstractClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final int PROXY_PORT = Integer.parseInt(allocatePort(CXF6655Test.class));
    static HttpProxyServer proxy;

    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            Object implementor = new HelloImpl();
            String address = "http://localhost:" + PORT + "/hello";
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

    @AfterClass
    public static void stopProxy() {
        proxy.stop();
        proxy = null;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        proxy = DefaultHttpProxyServer.bootstrap().withPort(PROXY_PORT).start();
    }

    @Test
    public void testConnection() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/systest/jaxws/", "HelloService");
        HelloService service = new HelloService(null, serviceName);
        assertNotNull(service);
        Hello hello = service.getHelloPort();

        ((BindingProvider)hello).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                         "http://localhost:" + PORT + "/hello");
        assertEquals("getSayHi", hello.sayHi("SayHi"));

    }

    @Test
    public void testConnectionWithProxy() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/systest/jaxws/", "HelloService");
        HelloService service = new HelloService(null, serviceName);
        assertNotNull(service);
        Hello hello = service.getHelloPort();

        Client client = ClientProxy.getClient(hello);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        HTTPConduit http = (HTTPConduit)client.getConduit();
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setAllowChunking(false);
        httpClientPolicy.setReceiveTimeout(0);
        httpClientPolicy.setProxyServerType(ProxyServerType.HTTP);
        httpClientPolicy.setProxyServer("localhost");
        httpClientPolicy.setProxyServerPort(PROXY_PORT);
        http.setClient(httpClientPolicy);

        ((BindingProvider)hello).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                         "http://localhost:" + PORT + "/hello");
        assertEquals("getSayHi", hello.sayHi("SayHi"));

    }
}
