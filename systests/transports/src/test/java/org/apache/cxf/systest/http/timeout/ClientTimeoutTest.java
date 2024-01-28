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
package org.apache.cxf.systest.http.timeout;

import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class ClientTimeoutTest extends AbstractBusClientServerTestBase {
    private static final String PORT = TestUtil.getNewPortNumber(ClientTimeoutTest.class);
    private static Server server;

    @BeforeClass
    public static void setUp() {
        final DelayedServiceImpl delayedImpl = new DelayedServiceImpl();
        final ServerFactoryBean svrFactory = new ServerFactoryBean();
        svrFactory.setServiceClass(DelayedService.class);
        svrFactory.setAddress("http://localhost:" + PORT + "/Hello");
        svrFactory.setServiceBean(delayedImpl);
        server = svrFactory.create();
    }

    @Test
    public void clientTimeoutWithParallelCalls() throws Exception {
        final DelayedService client = buildClient();

        // Start two soap calls in parallel, both will run into a timeout defined by CustomReadTimeoutInterceptor
        final CompletableFuture<String> f = new CompletableFuture<>();
        final Thread thread = new Thread(() -> {
            try {
                f.complete(client.delay(10 * 1000L));
            } catch (Exception ex) {
                f.completeExceptionally(ex);
            }
        });
        thread.start();

        // Wait a bit before scheduling another call
        Thread.sleep(1000);

        // Timeout for second call is 2000 millis.
        final Fault f1 = assertThrows(Fault.class, () -> client.delay(10 * 1000L));
        assertThat(f1.getCause(), instanceOf(SocketTimeoutException.class));
        assertThat(f1.getCause().getMessage(), containsString("2,000 milliseconds timeout"));

        // Timeout for first call is 4000 millis.
        final CompletionException f2 = assertThrows(CompletionException.class, () -> f.join());
        assertThat(f2.getCause(), instanceOf(Fault.class));
        assertThat(f2.getCause().getCause(), instanceOf(SocketTimeoutException.class));
        assertThat(f2.getCause().getCause().getMessage(), containsString("4,000 milliseconds timeout"));
    }

    private static DelayedService buildClient() {
        final Bus bus = BusFactory.getThreadDefaultBus();
        bus.setProperty(AsyncHTTPConduit.USE_ASYNC, true);

        final ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        factory.setAddress("http://localhost:" + PORT + "/Hello");
        factory.getOutInterceptors().add(new CustomReceiveTimeoutInterceptor());

        return factory.create(DelayedService.class);
    }

    @AfterClass
    public static void tearDown() {
        server.destroy();
    }
    
    /**
     * First call: Message.RECEIVE_TIMEOUT is set to 4000 millis
     * Second call: Message.RECEIVE_TIMEOUT is set to 2000 millis
     * ... and so on.
     */
    private static final class CustomReceiveTimeoutInterceptor extends AbstractPhaseInterceptor<Message> {
        private volatile long timeoutMillis = 4000;

        private CustomReceiveTimeoutInterceptor() {
            super(Phase.PREPARE_SEND);
            addBefore(MessageSenderInterceptor.class.getName());
        }

        @Override
        public void handleMessage(Message message) {
            message.put(Message.RECEIVE_TIMEOUT, timeoutMillis);
            timeoutMillis /= 2;
        }
    }
}
