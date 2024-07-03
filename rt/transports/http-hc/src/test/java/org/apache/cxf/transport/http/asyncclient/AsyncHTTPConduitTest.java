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

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.asyncclient.AsyncHttpResponseWrapperFactory.AsyncHttpResponseWrapper;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.workqueue.AutomaticWorkQueueImpl;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.apache.http.HttpResponse;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AsyncHTTPConduitTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(AsyncHTTPConduitTest.class);
    public static final String PORT_INV = allocatePort(AsyncHTTPConduitTest.class, 2);
    public static final String FILL_BUFFER = "FillBuffer";

    static Endpoint ep;
    static String request;
    static Greeter g;

    @BeforeClass
    public static void start() throws Exception {
        Bus b = createStaticBus();
        b.setProperty(AsyncHTTPConduit.USE_ASYNC, AsyncHTTPConduitFactory.UseAsyncPolicy.ALWAYS);
        b.setProperty("org.apache.cxf.transport.http.async.MAX_CONNECTIONS", 501);

        BusFactory.setThreadDefaultBus(b);

        AsyncHTTPConduitFactory hcf = (AsyncHTTPConduitFactory)b.getExtension(HTTPConduitFactory.class);
        assertEquals(501, hcf.maxConnections);

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

        URL wsdl = AsyncHTTPConduitTest.class.getResource("/wsdl/hello_world_services.wsdl");
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
        try {
            Response<GreetMeLaterResponse> future = g.greetMeLaterAsync(-5000L);
            future.get();
            fail();
        } catch (Exception ex) {
            //expected!!!
        }
    }

    @Test
    public void testRetransmitAsync() throws Exception {
                
        updateAddressPort(g, PORT);
        HTTPConduit c = (HTTPConduit)ClientProxy.getClient(g).getConduit();
        HTTPClientPolicy cp = new HTTPClientPolicy();
        cp.setMaxRetransmits(2);
        cp.setChunkLength(20);
        c.setClient(cp);
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
    public void testInvocationWithHCAddress() throws Exception {
        String address = "hc://http://localhost:" + PORT + "/SoapContext/SoapPort";
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
        factory.setTransportId("http://cxf.apache.org/transports/http/http-client");
        Greeter greeter = factory.create(Greeter.class);
        String response = greeter.greetMe("test");
        assertEquals("Get a wrong response", "Hello test", response);
    }

    @Test
    public void testCall() throws Exception {
        updateAddressPort(g, PORT);
        assertEquals("Hello " + request, g.greetMe(request));
        HTTPConduit c = (HTTPConduit)ClientProxy.getClient(g).getConduit();
        HTTPClientPolicy cp = new HTTPClientPolicy();
        cp.setAllowChunking(false);
        c.setClient(cp);
        assertEquals("Hello " + request, g.greetMe(request));
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

    @Test
    public void testCallAsyncWithResponseWrapper() throws Exception {
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final AsyncHttpResponseWrapper wrapper = new AsyncHttpResponseWrapper() {
                @Override
                public void responseReceived(HttpResponse response, Consumer<HttpResponse> delegate) {
                    delegate.accept(response);
                    latch.countDown();
                }
            };

            getStaticBus().setExtension(() -> wrapper, AsyncHttpResponseWrapperFactory.class);
    
            final String address = "hc://http://localhost:" + PORT + "/SoapContext/SoapPort";
            final Greeter greeter = new SOAPService().getSoapPort();
            setAddress(greeter, address);

            greeter.greetMeLaterAsync(1000, new AsyncHandler<GreetMeLaterResponse>() {
                public void handleResponse(Response<GreetMeLaterResponse> res) {
                }
            }).get();

            assertThat(latch.await(5, TimeUnit.SECONDS), is(true));
        } finally {
            getStaticBus().setExtension(null, AsyncHttpResponseWrapperFactory.class);
        }
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

    @Test
    public void testCallAsyncWithFullWorkQueue() throws Exception {
        Bus bus = BusFactory.getThreadDefaultBus();
        WorkQueueManager workQueueManager = bus.getExtension(WorkQueueManager.class);
        AutomaticWorkQueueImpl automaticWorkQueue1 = (AutomaticWorkQueueImpl)workQueueManager.getAutomaticWorkQueue();
        updateAddressPort(g, PORT);

        Client client = ClientProxy.getClient(g);
        HTTPConduit http = (HTTPConduit) client.getConduit();

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();

        int asyncExecuteTimeout = 500;
        httpClientPolicy.setAsyncExecuteTimeout(asyncExecuteTimeout);

        http.setClient(httpClientPolicy);

        long repeat = automaticWorkQueue1.getHighWaterMark() + automaticWorkQueue1.getMaxSize() + 1;
        CountDownLatch initialThreadsLatch = new CountDownLatch(automaticWorkQueue1.getHighWaterMark());
        CountDownLatch doneLatch = new CountDownLatch((int) repeat);
        AtomicInteger threadCount = new AtomicInteger();

        for (long i = 0; i < repeat; i++) {
            g.greetMeLaterAsync(-50, res -> {

                try {
                    int myCount = threadCount.getAndIncrement();

                    if (myCount < automaticWorkQueue1.getHighWaterMark()) {
                        // Sleep long enough so that the workqueue will fill up and then
                        // handleResponseOnWorkqueue will fail for the calls from both
                        // responseReceived and consumeContent
                        Thread.sleep(3L * asyncExecuteTimeout);
                        initialThreadsLatch.countDown();
                    } else {
                        Thread.sleep(50);
                    }
                    initialThreadsLatch.await();
                    doneLatch.countDown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        doneLatch.await(30, TimeUnit.SECONDS);

        assertEquals("All responses should be handled eventually", 0, doneLatch.getCount());
    }


    @Test
    @Ignore("peformance test")
    public void testCalls() throws Exception {
        updateAddressPort(g, PORT);

        //warmup
        for (int x = 0; x < 10000; x++) {
            //builder.append('a');
            //long s1 = System.nanoTime();
            //System.out.println("aa1: " + s1);
            String value = g.greetMe(request);
            //long s2 = System.nanoTime();
            //System.out.println("aa2: " + s2 + " " + (s2 - s1));
            assertEquals("Hello " + request, value);
            //System.out.println();
        }

        long start = System.currentTimeMillis();
        for (int x = 0; x < 10000; x++) {
            //builder.append('a');
            //long s1 = System.nanoTime();
            //System.out.println("aa1: " + s1);
            g.greetMe(request);
            //long s2 = System.nanoTime();
            //System.out.println("aa2: " + s2 + " " + (s2 - s1));
            //System.out.println();
        }
        long end = System.currentTimeMillis();
        System.out.println("Total: " + (end - start));
        /*
        updateAddressPort(g, PORT2);
        String value = g.greetMe(builder.toString());
        assertEquals("Hello " + builder.toString(), value);
        */
    }

    @Test
    @Ignore("peformance test")
    public void testCallsAsync() throws Exception {
        updateAddressPort(g, PORT);

        final int warmupIter = 5000;
        final int runIter = 5000;
        final CountDownLatch wlatch = new CountDownLatch(warmupIter);
        final boolean[] wdone = new boolean[warmupIter];

        @SuppressWarnings("unchecked")
        AsyncHandler<GreetMeLaterResponse>[] whandler = new AsyncHandler[warmupIter];
        for (int x = 0; x < warmupIter; x++) {
            final int c = x;
            whandler[x] = new AsyncHandler<GreetMeLaterResponse>() {
                public void handleResponse(Response<GreetMeLaterResponse> res) {
                    try {
                        String s = res.get().getResponseType();
                        s = s.substring(s.lastIndexOf(' ') + 1);
                        if (c != Integer.parseInt(s)) {
                            System.out.println("Problem " + c + " != " + s);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    wdone[c] = true;
                    wlatch.countDown();
                }
            };
        }

        //warmup
        long start = System.currentTimeMillis();
        for (int x = 0; x < warmupIter; x++) {
            //builder.append('a');
            //long s1 = System.nanoTime();
            //System.out.println("aa1: " + s1);
            g.greetMeLaterAsync(x, whandler[x]);
            //long s2 = System.nanoTime();
            //System.out.println("aa2: " + s2 + " " + (s2 - s1));
            //System.out.println();
        }
        wlatch.await(30, TimeUnit.SECONDS);

        long end = System.currentTimeMillis();
        System.out.println("Warmup Total: " + (end - start) + " " + wlatch.getCount());
        for (int x = 0; x < warmupIter; x++) {
            if (!wdone[x]) {
                System.out.println("  " + x);
            }
        }
        if (wlatch.getCount() > 0) {
            Thread.sleep(1000000);
        }

        final CountDownLatch rlatch = new CountDownLatch(runIter);
        AsyncHandler<GreetMeLaterResponse> rhandler = new AsyncHandler<GreetMeLaterResponse>() {
            public void handleResponse(Response<GreetMeLaterResponse> res) {
                rlatch.countDown();
            }
        };

        start = System.currentTimeMillis();
        for (int x = 0; x < runIter; x++) {
            //builder.append('a');
            //long s1 = System.nanoTime();
            //System.out.println("aa1: " + s1);
            g.greetMeLaterAsync(x, rhandler);
            //long s2 = System.nanoTime();
            //System.out.println("aa2: " + s2 + " " + (s2 - s1));
            //System.out.println();
        }
        rlatch.await(30, TimeUnit.SECONDS);
        end = System.currentTimeMillis();

        System.out.println("Total: " + (end - start) + " " + rlatch.getCount());
    }

    @Test
    public void testPathWithQueryParams() throws IOException {
        final String address = "http://localhost:" + PORT + "/SoapContext/SoapPort?param1=value1&param2=value2";
        final Greeter greeter = new SOAPService().getSoapPort();
        setAddress(greeter, address);

        final HTTPConduit c = (HTTPConduit)ClientProxy.getClient(greeter).getConduit();
        final MessageImpl message = new MessageImpl();
        message.put(AsyncHTTPConduit.USE_ASYNC, AsyncHTTPConduitFactory.UseAsyncPolicy.ALWAYS);

        final Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        c.prepare(message);

        final CXFHttpRequest e = message.get(CXFHttpRequest.class);
        assertEquals(address, e.getURI().toString());
    }

    @Test
    public void testEmptyPathWithQueryParams() throws IOException {
        final String address = "http://localhost:" + PORT + "?param1=value1&param2=value2";
        final Greeter greeter = new SOAPService().getSoapPort();
        setAddress(greeter, address);

        final HTTPConduit c = (HTTPConduit)ClientProxy.getClient(greeter).getConduit();
        final MessageImpl message = new MessageImpl();
        message.put(AsyncHTTPConduit.USE_ASYNC, AsyncHTTPConduitFactory.UseAsyncPolicy.ALWAYS);

        final Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        c.prepare(message);

        final CXFHttpRequest e = message.get(CXFHttpRequest.class);
        assertEquals("http://localhost:" + PORT + "/?param1=value1&param2=value2", e.getURI().toString());
    }
}
