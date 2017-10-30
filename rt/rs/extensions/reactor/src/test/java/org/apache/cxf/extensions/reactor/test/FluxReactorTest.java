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
package org.apache.cxf.extensions.reactor.test;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.reactor.client.FluxRxInvoker;
import org.apache.cxf.jaxrs.reactor.client.FluxRxInvokerProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.ws.Holder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FluxReactorTest extends AbstractBusClientServerTestBase {
    public static final String PORT = ReactorServer.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                launchServer(ReactorServer.class, true));
        createStaticBus();
    }
    @Test
    public void testGetHelloWorldJson() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/flux/textJson";
        List<Object> providers = new LinkedList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new FluxRxInvokerProvider());
        WebClient wc = WebClient.create(address, providers);
        List<HelloWorldBean> holder = new ArrayList<>();
        wc.accept("application/json")
                .rx(FluxRxInvoker.class)
                .get(HelloWorldBean.class)
                .doOnNext(holder::add)
                .subscribe();
        Thread.sleep(2000);
        assertEquals(1, holder.size());
        HelloWorldBean bean = holder.get(0);
        assertEquals("Hello", bean.getGreeting());
        assertEquals("World", bean.getAudience());
    }

    @Test
    @Ignore
    //TODO: this doesn't work yet,  No serializer found for class org.apache.cxf.jaxrs.reactor.server.StreamingAsyncSubscriber$StreamingResponseImpl
    public void testTextJsonImplicitListAsyncStream() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/flux/textJsonImplicitListAsyncStream";
        List<Object> providers = new LinkedList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new FluxRxInvokerProvider());
        WebClient wc = WebClient.create(address, providers);
        Holder<HelloWorldBean> holder = new Holder<>();
        wc.accept("application/json")
                .rx(FluxRxInvoker.class)
                .get(HelloWorldBean.class)
                .doOnNext(helloWorldBean -> holder.value = helloWorldBean)
                .subscribe();
        Thread.sleep(2000);
        assertEquals("Hello", holder.value.getGreeting());
        assertEquals("World", holder.value.getAudience());
    }

    @Test
    public void testGetString() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/flux/textAsync";
        List<Object> providers = new LinkedList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new FluxRxInvokerProvider());
        WebClient wc = WebClient.create(address, providers);
        Holder<String> holder = new Holder<>();
        wc.accept("text/plain")
                .rx(FluxRxInvoker.class)
                .get(String.class)
                .doOnNext(msg -> holder.value = msg)
                .subscribe();
        Thread.sleep(2000);
        assertEquals("Hello, world!", holder.value);
    }
}
