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

package org.apache.cxf.transport.http.netty.client;

import java.net.URL;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;
import org.apache.hello_world_soap_http.types.GreetMeResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class NettyHttpConduitTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(NettyHttpConduitTest.class);
    public static final String PORT_INV = allocatePort(NettyHttpConduitTest.class, 2);
    public static final String FILL_BUFFER = "FillBuffer";

    private Endpoint ep;
    private String request;
    private Greeter g;

    @Before
    public void start() throws Exception {
        Bus b = createStaticBus();
        b.setProperty(NettyHttpConduit.USE_ASYNC, NettyHttpConduitFactory.UseAsyncPolicy.ALWAYS);

        BusFactory.setThreadDefaultBus(b);

        ep = Endpoint.publish("http://localhost:" + PORT + "/SoapContext/SoapPort",
                              new org.apache.hello_world_soap_http.GreeterImpl() {
                public String greetMeLater(long cnt) {
                    //use the continuations so the async client can
                    //have a ton of connections, use less threads
                    //
                    //mimic a slow server by delaying somewhere between
                    //1 and 2 seconds, with a preference of delaying the earlier
                    //requests longer to create a sort of backlog/contention
                    //with the later requests
                    ContinuationProvider p = (ContinuationProvider)
                        getContext().getMessageContext().get(ContinuationProvider.class.getName());
                    Continuation c = p.getContinuation();
                    if (c.isNew()) {
                        if (cnt < 0) {
                            c.suspend(-cnt);
                        } else {
                            c.suspend(2000 - (cnt % 1000));
                        }
                        return null;
                    }
                    return "Hello, finally! " + cnt;
                }
                public String greetMe(String me) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10L));
                    if (me.equals(FILL_BUFFER)) {
                        return String.join("", Collections.nCopies(16093, " "));
                    } else {
                        return "Hello " + me;
                    }
                }
            });

        StringBuilder builder = new StringBuilder("NaNaNa");
        for (int x = 0; x < 50; x++) {
            builder.append(" NaNaNa ");
        }
        request = builder.toString();

        URL wsdl = NettyHttpConduitTest.class.getResource("/wsdl/hello_world_services.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService();
        assertNotNull("Service is null", service);

        g = service.getSoapPort();
        assertNotNull("Port is null", g);
    }

    @After
    public void stop() throws Exception {
        ((java.io.Closeable)g).close();
        ep.stop();
        ep = null;
    }

    @Test
    public void testResponseSameBufferSize() throws Exception {
        updateAddressPort(g, PORT);
        HTTPConduit c = (HTTPConduit)ClientProxy.getClient(g).getConduit();
        c.getClient().setReceiveTimeout(12000);
        try {
            g.greetMe(FILL_BUFFER);
            g.greetMe("Hello");
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void testTimeout() throws Exception {
        updateAddressPort(g, PORT);
        HTTPConduit c = (HTTPConduit)ClientProxy.getClient(g).getConduit();
        c.getClient().setReceiveTimeout(3000);
        try {
            assertEquals("Hello " + request, g.greetMeLater(-5000));
            fail();
        } catch (Exception ex) {
            //expected!!!
        }
    }

    @Test
    public void testTimeoutWithPropertySetting() throws Exception {
        ((jakarta.xml.ws.BindingProvider)g).getRequestContext().put("jakarta.xml.ws.client.receiveTimeout",
            "3000");
        updateAddressPort(g, PORT);

        try {
            assertEquals("Hello " + request, g.greetMeLater(-5000));
            fail();
        } catch (Exception ex) {
            //expected!!!
        }
    }

    @Test
    public void testTimeoutAsync() throws Exception {
        updateAddressPort(g, PORT);
        HTTPConduit c = (HTTPConduit)ClientProxy.getClient(g).getConduit();
        c.getClient().setReceiveTimeout(3000);
        c.getClient().setAsyncExecuteTimeout(3000);
        try {
            Response<GreetMeLaterResponse> future = g.greetMeLaterAsync(-5000L);
            future.get();
            fail();
        } catch (Exception ex) {
            //expected!!!
        }
    }
    
    @Test
    public void testNoTimeoutAsync() throws Exception {
        updateAddressPort(g, PORT);
        HTTPConduit c = (HTTPConduit)ClientProxy.getClient(g).getConduit();
        c.getClient().setReceiveTimeout(2000);
        c.getClient().setAsyncExecuteTimeout(2000);

        Response<GreetMeLaterResponse> future = g.greetMeLaterAsync(-100L);
        future.get();
        
        Response<GreetMeLaterResponse> future2 = g.greetMeLaterAsync(-100L);
        future2.get();
        
        Thread.sleep(3000);
    }

    @Test
    public void testTimeoutAsyncWithPropertySetting() throws Exception {
        updateAddressPort(g, PORT);
        ((jakarta.xml.ws.BindingProvider)g).getRequestContext().put("jakarta.xml.ws.client.receiveTimeout",
            "3000");
        try {
            Response<GreetMeLaterResponse> future = g.greetMeLaterAsync(-5000L);
            future.get();
            fail();
        } catch (Exception ex) {
            //expected!!!
        }
    }

    @Test
    public void testConnectIssue() throws Exception {
        updateAddressPort(g, PORT_INV);
        try {
            g.greetMe(request);
            fail("should have connect exception");
        } catch (Exception ex) {
            //expected
        }
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
        GreetMeResponse resp = (GreetMeResponse)g.greetMeAsync(request, new AsyncHandler<GreetMeResponse>() {
            public void handleResponse(Response<GreetMeResponse> res) {
                try {
                    res.get().getResponseType();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }).get();
        assertEquals("Hello " + request, resp.getResponseType());

        g.greetMeLaterAsync(1000, new AsyncHandler<GreetMeLaterResponse>() {
            public void handleResponse(Response<GreetMeLaterResponse> res) {
            }
        }).get();
    }

    @Test(timeout = 60000)
    public void testCallAsyncCallbackInvokedOnlyOnce() throws Exception {
        updateAddressPort(g, PORT);
        int repeat = 20;
        final AtomicInteger count = new AtomicInteger(0);
        for (int i = 0; i < repeat; i++) {
            try {
                g.greetMeAsync(request, new AsyncHandler<GreetMeResponse>() {
                    public void handleResponse(Response<GreetMeResponse> res) {
                        count.incrementAndGet();
                    }
                }).get();
            } catch (Exception e) {
            }
        }
        assertEquals("Callback should be invoked only once per request", repeat, count.intValue());
    }
}
