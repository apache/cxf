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

package org.apache.cxf.systest.jaxrs.reactive;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.xml.ws.Holder;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.rx2.client.FlowableRxInvoker;
import org.apache.cxf.jaxrs.rx2.client.FlowableRxInvokerProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;

import org.junit.BeforeClass;
import org.junit.Test;


public class JAXRSRxJava2FlowableTest extends AbstractBusClientServerTestBase {
    public static final String PORT = RxJava2FlowableServer.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(RxJava2FlowableServer.class, true));
        createStaticBus();
    }
    @Test
    public void testGetHelloWorldAsyncText() throws Exception {
        String address = "http://localhost:" + PORT + "/rx2/flowable/textAsync";
        WebClient wc = WebClient.create(address);
        String text = wc.accept("text/plain").get(String.class);
        assertEquals("Hello, world!", text);
    }
    
    @Test
    public void testGetHelloWorldJson() throws Exception {
        String address = "http://localhost:" + PORT + "/rx2/flowable/textJson";
        List<Object> providers = new LinkedList<>();
        providers.add(new JacksonJsonProvider());
        providers.add(new FlowableRxInvokerProvider());
        WebClient wc = WebClient.create(address, providers);
        Flowable<HelloWorldBean> obs = wc.accept("application/json")
            .rx(FlowableRxInvoker.class)
            .get(HelloWorldBean.class);
        
        Holder<HelloWorldBean> holder = new Holder<HelloWorldBean>();
        Disposable d = obs.subscribe(v -> {
            holder.value = v;
        });
        if (d == null) {
            throw new IllegalStateException("Subscribe did not return a Disposable");
        }
        Thread.sleep(3000);
        assertEquals("Hello", holder.value.getGreeting());
        assertEquals("World", holder.value.getAudience());
    }
    
    @Test
    public void testGetHelloWorldJsonImplicitListAsync() throws Exception {
        String address = "http://localhost:" + PORT + "/rx2/flowable/textJsonImplicitListAsync";
        doTestGetHelloWorldJsonList(address);
    }
    @Test
    public void testGetHelloWorldJsonImplicitListAsyncStream() throws Exception {
        String address = "http://localhost:" + PORT + "/rx2/flowable/textJsonImplicitListAsyncStream";
        doTestGetHelloWorldJsonList(address);
    }
    @Test
    public void testGetHelloWorldJsonImplicitList() throws Exception {
        String address = "http://localhost:" + PORT + "/rx22/flowable/textJsonImplicitList";
        doTestGetHelloWorldJsonList(address);
    }
    private void doTestGetHelloWorldJsonList(String address) throws Exception {
        WebClient wc = WebClient.create(address,
                                        Collections.singletonList(new JacksonJsonProvider()));
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000);
        GenericType<List<HelloWorldBean>> genericResponseType = new GenericType<List<HelloWorldBean>>() {
        };

        List<HelloWorldBean> beans = wc.accept("application/json").get(genericResponseType);
        assertEquals(2, beans.size());
        assertEquals("Hello", beans.get(0).getGreeting());
        assertEquals("World", beans.get(0).getAudience());
        assertEquals("Ciao", beans.get(1).getGreeting());
        assertEquals("World", beans.get(1).getAudience());
    }
    
    @Test
    public void testGetHelloWorldJsonSingle() throws Exception {
        String address = "http://localhost:" + PORT + "/rx22/flowable/textJsonSingle";
        WebClient wc = WebClient.create(address,
                                        Collections.singletonList(new JacksonJsonProvider()));
    
        HelloWorldBean bean = wc.accept("application/json").get(HelloWorldBean.class);
        assertEquals("Hello", bean.getGreeting());
        assertEquals("World", bean.getAudience());
    }
    
    @Test
    public void testGetHelloWorldAsyncObservable() throws Exception {
        String address = "http://localhost:" + PORT + "/rx2/flowable/textAsync";
        WebClient wc = WebClient.create(address,
                                        Collections.singletonList(new FlowableRxInvokerProvider()));
        Flowable<String> obs = wc.accept("text/plain")
            .rx(FlowableRxInvoker.class)
            .get(String.class);
        
        Thread.sleep(2000);

        Disposable d = obs.map(
            s -> {
                return s + s;
            })
            .subscribe(s -> assertDuplicateResponse(s));
        if (d == null) {
            throw new IllegalStateException("Subscribe did not return a Disposable");
        }
    }
    @Test
    public void testGetHelloWorldAsyncObservable404() throws Exception {
        String address = "http://localhost:" + PORT + "/rx2/flowable/textAsync404";
        Invocation.Builder b = ClientBuilder.newClient().register(new FlowableRxInvokerProvider())
            .target(address).request();
        Disposable d = b.rx(FlowableRxInvoker.class).get(String.class).subscribe(
            s -> {
                fail("Exception expected");
            },
            t -> validateT((ExecutionException)t));
        if (d == null) {
            throw new IllegalStateException("Subscribe did not return a Disposable");
        }
        
    }

    private void validateT(ExecutionException t) {
        assertTrue(t.getCause() instanceof NotFoundException);
    }
    private void assertDuplicateResponse(String s) {
        assertEquals("Hello, world!Hello, world!", s);
    }
}
