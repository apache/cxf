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

package org.apache.cxf.transport.http.asyncclient;


import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;
import org.apache.hello_world_soap_http.types.GreetMeResponse;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AsyncHTTPConduitLargeResponseTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(AsyncHTTPConduitLargeResponseTest.class);
    public static final String PORT_INV = allocatePort(AsyncHTTPConduitLargeResponseTest.class, 2);

    static Endpoint ep;
    static String request;
    static Greeter g;

    @BeforeClass
    public static void start() {
        Bus b = createStaticBus();
        b.setProperty(AsyncHTTPConduit.USE_ASYNC, AsyncHTTPConduitFactory.UseAsyncPolicy.ALWAYS);
        b.setProperty("org.apache.cxf.transport.http.async.MAX_CONNECTIONS", 501);

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
                    return "Hello " + me.repeat(500);
                }
            });

        StringBuilder builder = new StringBuilder("NaNaNa");
        for (int x = 0; x < 50; x++) {
            builder.append(" NaNaNa ");
        }
        request = builder.toString();

        URL wsdl = AsyncHTTPConduitLargeResponseTest.class.getResource("/wsdl/hello_world_services.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService();
        assertNotNull("Service is null", service);

        g = service.getSoapPort();
        assertNotNull("Port is null", g);
    }

    @AfterClass
    public static void stop() throws Exception {
        ((java.io.Closeable)g).close();
        ep.stop();
        ep = null;
    }

    @Test
    public void testCall() throws Exception {
        updateAddressPort(g, PORT);
        assertEquals("Hello " + request.repeat(500), g.greetMe(request));
        HTTPConduit c = (HTTPConduit)ClientProxy.getClient(g).getConduit();
        HTTPClientPolicy cp = new HTTPClientPolicy();
        cp.setAllowChunking(false);
        c.setClient(cp);
        assertEquals("Hello " + request.repeat(500), g.greetMe(request));
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
        assertEquals("Hello " + request.repeat(500), resp.getResponseType());

        g.greetMeLaterAsync(1000, new AsyncHandler<GreetMeLaterResponse>() {
            public void handleResponse(Response<GreetMeLaterResponse> res) {
            }
        }).get();
    }

    @Test
    public void testCallAsyncCallbackInvokedOnlyOnce() throws Exception {
        // This test is especially targeted for RHEL 6.8
        updateAddressPort(g, PORT_INV);
        int repeat = 100;
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
        Thread.sleep(1000);
        assertEquals("Callback should be invoked only once per request", repeat, count.intValue());
    }
}