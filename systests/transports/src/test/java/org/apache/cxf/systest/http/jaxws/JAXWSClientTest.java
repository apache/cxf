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

package org.apache.cxf.systest.http.jaxws;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.jws.WebService;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.AbstractGreeterImpl;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JAXWSClientTest  extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    
    public static class Server extends AbstractBusTestServerBase {

        protected void run()  {
            GreeterImpl implementor = new GreeterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";
            jakarta.xml.ws.Endpoint.publish(address, implementor);
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

        @WebService(serviceName = "BasicGreeterService",
                    portName = "GreeterPort",
                    endpointInterface = "org.apache.cxf.greeter_control.Greeter",
                    targetNamespace = "http://cxf.apache.org/greeter_control",
                    wsdlLocation = "testutils/greeter_control.wsdl")
        public class GreeterImpl extends AbstractGreeterImpl {
        }
    }


    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @AfterClass
    public static void stopServers() throws Exception {
        stopAllServers();
    }
    
    @Test
    public void testClientChunkingWithFaultyLogEventSender() throws Exception {
        // setup the feature by using JAXWS front-end API
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress("http://localhost:" + PORT + "/SoapContext/GreeterPort");

        // See please https://issues.apache.org/jira/browse/CXF-9100
        factory.getOutInterceptors().add(new org.apache.cxf.ext.logging.LoggingOutInterceptor(
            new LogEventSender() {
                @Override
                public void send(LogEvent event) {
                    throw new RuntimeException("Unexpected exception");
                }
            }) {
            
        });
        factory.setServiceClass(Greeter.class);
        Greeter proxy = factory.create(Greeter.class);

        Client client = ClientProxy.getClient(proxy);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.getClient().setAllowChunking(true);
        http.getClient().setChunkingThreshold(1000);

        final char[] bytes = new char [32 * 1024];
        final Random random = new Random();
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (char)(random.nextInt(26) + 'a');
        }

        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        final Supplier<Long> captureHttpClientThreads = () ->
            Arrays
                .stream(threadMXBean.getAllThreadIds())
                .mapToObj(id -> threadMXBean.getThreadInfo(id))
                .filter(Objects::nonNull)
                .filter(t -> t.getThreadName().startsWith("HttpClient-"))
                .filter(t -> !t.getThreadName().endsWith("-SelectorManager"))
                .count();

        // Capture the number of client threads at start
        final long expectedHttpClientThreads = captureHttpClientThreads.get();
        final String greeting = new String(bytes);
        for (int  i = 0; i < 100; ++i) {
            assertThrows(SOAPFaultException.class, () -> proxy.greetMe(greeting));
        }

        // HttpClient may keep some small amount worker threads around, capping it to 5
        assertThat(captureHttpClientThreads.get(), lessThanOrEqualTo(expectedHttpClientThreads + 5L));
    }

    @Test
    public void testNoChunkingHighLoad() throws Exception {
        // setup the feature by using JAXWS front-end API
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress("http://localhost:" + PORT + "/SoapContext/GreeterPort");
        factory.setServiceClass(Greeter.class);

        final Greeter proxy = factory.create(Greeter.class);
        Client client = ClientProxy.getClient(proxy);
        HTTPConduit http = (HTTPConduit) client.getConduit();

        final HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(3000);
        httpClientPolicy.setAllowChunking(false);
        httpClientPolicy.setVersion("1.1");
        http.setClient(httpClientPolicy);

        final AuthorizationPolicy authPolicy = new AuthorizationPolicy();
        authPolicy.setUserName("test");
        authPolicy.setPassword("test");
        authPolicy.setAuthorizationType("Basic");
        http.setAuthorization(authPolicy);

        final char[] bytes = new char [32 * 1024];
        final Random random = new Random();
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (char)(random.nextInt(26) + 'a');
        }

        final String greeting = new String(bytes);
        final Collection<Future<String>> futures = new ArrayList<>();
        final ExecutorService executor = Executors.newFixedThreadPool(10);

        try {
            for (int  i = 0; i < 2000; ++i) {
                futures.add(executor.submit(() -> proxy.greetMe(greeting)));
            }

            for (final Future<String> f: futures) {
                assertThat(f.get(10, TimeUnit.SECONDS), equalTo(greeting.toUpperCase()));
            }
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void testUpdateAddress() throws Exception {
        // setup the feature by using JAXWS front-end API
        final JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress("http://localhost:" + PORT + "/SoapContext/GreeterPort");
        factory.setServiceClass(Greeter.class);

        final Greeter proxy = factory.create(Greeter.class);
        final Collection<Future<String>> futures = new ArrayList<>();
        final ExecutorService executor = Executors.newFixedThreadPool(10);

        try {
            for (int  i = 0; i < 100; ++i) {
                futures.add(executor.submit(() -> {
                    final BindingProvider bp = (BindingProvider)proxy;
                    updateAddressPort(bp, PORT);
                    return proxy.greetMe("Hi!");
                }));
            }

            for (final Future<String> f: futures) {
                assertThat(f.get(10, TimeUnit.SECONDS), equalTo("Hi!".toUpperCase()));
            }
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }
}
