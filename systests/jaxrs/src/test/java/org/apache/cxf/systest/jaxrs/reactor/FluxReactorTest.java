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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.reactor.client.ReactorInvoker;
import org.apache.cxf.jaxrs.reactor.client.ReactorInvokerProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class FluxReactorTest extends AbstractBusClientServerTestBase {
    public static final String PORT = ReactorServer.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(ReactorServer.class, true));
        createStaticBus();
    }
    @Test
    public void testGetHelloWorldJson() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/flux/textJson";
        List<HelloWorldBean> collector = new ArrayList<>();
        ClientBuilder.newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request("application/json")
                .rx(ReactorInvoker.class)
                .get(HelloWorldBean.class)
                .doOnNext(collector::add)
                .subscribe();
        Thread.sleep(500);
        assertEquals(1, collector.size());
        HelloWorldBean bean = collector.get(0);
        assertEquals("Hello", bean.getGreeting());
        assertEquals("World", bean.getAudience());
    }

    @Test
    public void testTextJsonImplicitListAsyncStream() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor/flux/textJsonImplicitListAsyncStream";
        doTestTextJsonImplicitListAsyncStream(address);
    }
    @Test
    public void testTextJsonImplicitListAsyncStream2() throws Exception {
        String address = "http://localhost:" + PORT + "/reactor2/flux/textJsonImplicitListAsyncStream2";
        doTestTextJsonImplicitListAsyncStream(address);
    }
    private void doTestTextJsonImplicitListAsyncStream(String address) throws Exception {
        List<HelloWorldBean> holder = new ArrayList<>();
        ClientBuilder.newClient()
                .register(new JacksonJsonProvider())
                .register(new ReactorInvokerProvider())
                .target(address)
                .request("application/json")
                .rx(ReactorInvoker.class)
                .getFlux(HelloWorldBean.class)
                .doOnNext(holder::add)
                .subscribe();
        Thread.sleep(500);
        assertEquals(2, holder.size());
        assertEquals("Hello", holder.get(0).getGreeting());
        assertEquals("World", holder.get(0).getAudience());
        assertEquals("Ciao", holder.get(1).getGreeting());
    }
}
