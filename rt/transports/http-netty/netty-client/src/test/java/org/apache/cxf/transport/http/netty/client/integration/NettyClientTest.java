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
package org.apache.cxf.transport.http.netty.client.integration;

import java.net.URL;
import java.util.concurrent.ExecutionException;

import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;
import org.apache.hello_world_soap_http.types.GreetMeResponse;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NettyClientTest extends AbstractBusClientServerTestBase {

    public static final String PORT = allocatePort(NettyClientTest.class);
    public static final String PORT_INV = allocatePort(NettyClientTest.class, 2);


    static Endpoint ep;

    static Greeter g;

    @BeforeClass
    public static void start() throws Exception {
        Bus b = createStaticBus();
        BusFactory.setThreadDefaultBus(b);
        ep = Endpoint.publish("http://localhost:" + PORT + "/SoapContext/SoapPort",
                new org.apache.hello_world_soap_http.GreeterImpl());

        URL wsdl = NettyClientTest.class.getResource("/wsdl/hello_world.wsdl");
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
    }

    @Test
    public void testInvocation() throws Exception {
        updateAddressPort(g, PORT);
        String response = g.greetMe("test");
        assertEquals("Get a wrong response", "Hello test", response);
    }

    @Test
    public void testInovationWithNettyAddress() throws Exception {
        String address = "netty://http://localhost:" + PORT + "/SoapContext/SoapPort";
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Greeter.class);
        factory.setAddress(address);
        Greeter greeter = factory.create(Greeter.class);
        String response = greeter.greetMe("test");
        assertEquals("Get a wrong response", "Hello test", response);
    }

    @Test
    public void testInvocationWithTransportId() throws Exception {
        String address = "http://localhost:" + PORT + "/SoapContext/SoapPort";
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Greeter.class);
        factory.setAddress(address);
        factory.setTransportId("http://cxf.apache.org/transports/http/netty/client");
        Greeter greeter = factory.create(Greeter.class);
        String response = greeter.greetMe("test");
        assertEquals("Get a wrong response", "Hello test", response);
    }

    @Test
    public void testCallAsync() throws Exception {
        updateAddressPort(g, PORT);
        GreetMeResponse resp = (GreetMeResponse)g.greetMeAsync("asyncTest", new AsyncHandler<GreetMeResponse>() {
            public void handleResponse(Response<GreetMeResponse> res) {
                try {
                    res.get().getResponseType();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }).get();
        assertEquals("Hello asyncTest", resp.getResponseType());

        MyLaterResponseHandler handler = new MyLaterResponseHandler();
        g.greetMeLaterAsync(1000, handler).get();
        // need to check the result here
        assertEquals("Hello, finally!", handler.getResponse().getResponseType());


    }

    private final class MyLaterResponseHandler implements AsyncHandler<GreetMeLaterResponse> {
        GreetMeLaterResponse response;
        @Override
        public void handleResponse(Response<GreetMeLaterResponse> res) {
            try {
                response = res.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        GreetMeLaterResponse getResponse() {
            return response;
        }

    }
}
