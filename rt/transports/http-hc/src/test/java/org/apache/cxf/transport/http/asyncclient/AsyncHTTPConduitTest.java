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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Response;

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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;



public class AsyncHTTPConduitTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(AsyncHTTPConduitTest.class);
    public static final String PORT_INV = allocatePort(AsyncHTTPConduitTest.class, 2);
    
    static Endpoint ep;
    static String request;
    static Greeter g;
    
    @BeforeClass
    public static void start() throws Exception {
        Bus b = createStaticBus();
        b.setProperty(AsyncHTTPConduit.USE_ASYNC, AsyncHTTPConduitFactory.UseAsyncPolicy.ALWAYS);
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
                    return "Hello " + me;
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
    public void testInovationWithHCAddress() throws Exception {
        String address =  "hc://http://localhost:" + PORT + "/SoapContext/SoapPort";
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Greeter.class);
        factory.setAddress(address);
        Greeter greeter = factory.create(Greeter.class);
        String response = greeter.greetMe("test");
        assertEquals("Get a wrong response", "Hello test", response);
    }
    
    @Test
    public void testInvocationWithTransportId() throws Exception {
        String address =  "http://localhost:" + PORT + "/SoapContext/SoapPort";
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
    }
    @Test
    public void testCallAsync() throws Exception {
        updateAddressPort(g, PORT);
        GreetMeResponse resp = (GreetMeResponse)g.greetMeAsync(request, new AsyncHandler<GreetMeResponse>() {
            public void handleResponse(Response<GreetMeResponse> res) {
                try {
                    res.get().getResponseType();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    // TODO Auto-generated catch block
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
    @Ignore("peformance test")
    public void testCalls() throws Exception {
        updateAddressPort(g, PORT);

        //warmup
        for (int x = 0; x < 10000; x++) {
            //builder.append("a");
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
            //builder.append("a");
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
        final boolean wdone[] = new boolean[warmupIter];
        
        @SuppressWarnings("unchecked")
        AsyncHandler<GreetMeLaterResponse> whandler[] = new AsyncHandler[warmupIter]; 
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
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        // TODO Auto-generated catch block
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
            //builder.append("a");
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
            //builder.append("a");
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

}
