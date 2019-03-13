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

package org.apache.cxf.systest.jaxrs.reactor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.reactor.client.ReactorInvoker;
import org.apache.cxf.jaxrs.reactor.client.ReactorInvokerProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MonoReactorTest extends AbstractBusClientServerTestBase {
    public static final String PORT = ReactorServer.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(ReactorServer.class, true));
        createStaticBus();
    }
    @Test
    public void testGetHelloWorldJson() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/mono/textJson";
        final BlockingQueue<HelloWorldBean> holder = new LinkedBlockingQueue<>();
        ClientBuilder.newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(ReactorInvoker.class)
                .get(HelloWorldBean.class)
                .doOnNext(holder::offer)
                .subscribe();

        HelloWorldBean bean = holder.poll(1L, TimeUnit.SECONDS);
        assertNotNull(bean);
        assertEquals("Hello", bean.getGreeting());
        assertEquals("World", bean.getAudience());
    }

    @Test
    public void testTextJsonImplicitListAsyncStream() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/mono/textJsonImplicitListAsyncStream";
        final BlockingQueue<HelloWorldBean> holder = new LinkedBlockingQueue<>();
        ClientBuilder.newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.APPLICATION_JSON)
                .rx(ReactorInvoker.class)
                .get(HelloWorldBean.class)
                .doOnNext(holder::offer)
                .subscribe();
        HelloWorldBean bean = holder.poll(1L, TimeUnit.SECONDS);
        assertNotNull(bean);
        assertEquals("Hello", bean.getGreeting());
        assertEquals("World", bean.getAudience());
    }

    @Test
    public void testGetString() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/mono/textAsync";
        final BlockingQueue<String> holder = new LinkedBlockingQueue<>();
        ClientBuilder.newClient()
                .register(new ReactorInvokerProvider())
                .target(address)
                .request(MediaType.TEXT_PLAIN)
                .rx(ReactorInvoker.class)
                .get(String.class)
                .doOnNext(holder::offer)
                .subscribe();

        String value = holder.poll(1L, TimeUnit.SECONDS);
        assertNotNull(value);
        assertEquals("Hello, world!", value);
    }
}
